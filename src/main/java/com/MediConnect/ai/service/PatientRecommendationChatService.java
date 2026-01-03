package com.MediConnect.ai.service;

import com.MediConnect.EntryRelated.entities.AccountStatus;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.SpecializationType;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.ai.dto.ChatMessageDTO;
import com.MediConnect.ai.dto.ChatRequestDTO;
import com.MediConnect.ai.dto.ChatResponseDTO;
import com.MediConnect.ai.dto.DoctorSuggestionDTO;
import com.MediConnect.ai.dto.PatientContextDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

/**
 * Coordinates doctor discovery with the OpenAI chat model.
 * <p>
 * The service keeps the orchestration logic on the server so the frontend can remain lightweight.
 * It prepares the doctor catalogue, calls the OpenAI Chat Completions endpoint, and parses the
 * structured response expected from the model.
 */
@Slf4j
@Service
public class PatientRecommendationChatService {

    private static final URI OPENAI_CHAT_URI = URI.create("https://api.openai.com/v1/chat/completions");
    private static final double BASE_MATCH_SCORE = 1.0d;
    private static final int MAX_HISTORY_LENGTH = 8; // Reduced to save tokens
    private static final int MAX_DOCTORS_SHARED_WITH_MODEL = 4; // Reduced to save tokens
    private static final int MAX_DOCTORS_RETURNED_TO_PATIENT = 4;
    private static final Map<SpecializationType, Set<String>> SPECIALISATION_KEYWORDS = buildSpecialisationKeywordMap();
    private static final Map<String, String> SPECIALISATION_SYNONYMS = buildSpecialisationSynonymMap();

    private final HealthcareProviderRepo healthcareProviderRepo;
    private final ObjectMapper objectMapper;
    private final RestTemplate openAiRestTemplate;
    
    @Value("${openai.api-key:}")
    private String openAiApiKey;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    // Explicit constructor injection to properly handle @Qualifier
    public PatientRecommendationChatService(
            HealthcareProviderRepo healthcareProviderRepo,
            ObjectMapper objectMapper,
            @Qualifier("openAiRestTemplate") RestTemplate openAiRestTemplate) {
        this.healthcareProviderRepo = healthcareProviderRepo;
        this.objectMapper = objectMapper;
        this.openAiRestTemplate = openAiRestTemplate;
    }

    public ChatResponseDTO chat(ChatRequestDTO request) {
        PatientContextDTO context = Optional.ofNullable(request.getContext()).orElseGet(PatientContextDTO::new);

        List<HealthcareProvider> providers = healthcareProviderRepo.findAll();
        log.info("Fetched {} providers from database", providers.size());
        if (providers.isEmpty()) {
            log.warn("No providers found in database at all!");
            return buildFallbackResponse(context, List.of(), "No active doctors available.");
        }

        List<ChatMessageDTO> incomingMessages = Optional.ofNullable(request.getMessages()).orElse(List.of());
        InteractionIntent intent = detectIntent(incomingMessages);
        log.info("Detected intent: {} for messages: {}", intent, incomingMessages.stream()
                .map(m -> m.getContent())
                .collect(Collectors.toList()));

        if (intent == InteractionIntent.NAVIGATION_ONLY || intent == InteractionIntent.GREETING_ONLY) {
            resetContextForNavigation(context);
        }

        enrichContextFromMessages(context, incomingMessages, providers);

        List<DoctorSuggestionDTO> doctorCatalogue = loadDoctorCatalogue(context, providers);
        log.info("loadDoctorCatalogue returned {} doctors", doctorCatalogue.size());
        RecommendationOutcome recommendationOutcome = buildRankedRecommendations(context, doctorCatalogue, intent == InteractionIntent.FIND_DOCTOR);
        log.info("buildRankedRecommendations returned payloadCatalogue with {} doctors", recommendationOutcome.payloadCatalogue.size());

        // Ensure we always send doctors to the AI if they exist, even if filtering resulted in empty payloadCatalogue
        // This allows the AI to recommend doctors even if they don't perfectly match all criteria
        List<DoctorSuggestionDTO> doctorsToSend = recommendationOutcome.payloadCatalogue;
        if (doctorsToSend.isEmpty() && !doctorCatalogue.isEmpty() && intent == InteractionIntent.FIND_DOCTOR) {
            // If filtering removed all doctors, send at least the top doctors from the original catalogue
            // Prioritize doctors with the right specialization if available
            log.warn("payloadCatalogue is empty after filtering, but doctorCatalogue has {} doctors. Sending top doctors from original catalogue.", doctorCatalogue.size());
            doctorsToSend = doctorCatalogue.stream()
                    .limit(MAX_DOCTORS_SHARED_WITH_MODEL)
                    .collect(Collectors.toList());
        }

        // Only return early if the original catalogue is completely empty
        if (intent == InteractionIntent.FIND_DOCTOR && doctorCatalogue.isEmpty()) {
            log.warn("Doctor catalogue is completely empty. Returning fallback message.");
            return buildFallbackResponse(context, List.of(), "No active doctors available.");
        }

        // Log what we're sending for debugging
        if (intent == InteractionIntent.FIND_DOCTOR) {
            log.info("Sending {} doctors to AI for recommendation. Original catalogue had {} doctors.", 
                    doctorsToSend.size(), doctorCatalogue.size());
            if (!doctorsToSend.isEmpty()) {
                log.debug("Sample doctor IDs being sent: {}", 
                        doctorsToSend.stream().map(DoctorSuggestionDTO::getId).limit(3).collect(Collectors.toList()));
            } else {
                log.error("ERROR: doctorsToSend is empty but doctorCatalogue has {} doctors! This should not happen.", doctorCatalogue.size());
            }
        }

        // Build messages with the doctors we're actually sending to the AI
        List<Map<String, Object>> payloadMessages = buildMessagesPayload(incomingMessages, context, doctorsToSend);
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", openAiModel);
        payload.put("temperature", 0.2);
        payload.put("messages", payloadMessages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = openAiRestTemplate.exchange(
                    OPENAI_CHAT_URI,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("OpenAI request returned non-success status: {}", response.getStatusCode());
                return buildFallbackResponse(context, doctorsToSend, "Assistant unavailable right now.");
            }

            // Use the doctors we actually sent to the AI for parsing
            ChatResponseDTO chatResponse = parseResponse(response.getBody(), context, doctorsToSend);
            mergeRecommendationGuidance(chatResponse, recommendationOutcome, intent == InteractionIntent.FIND_DOCTOR);
            
            // CRITICAL: Filter recommended doctors to only include those matching the requested specialization
            if (intent == InteractionIntent.FIND_DOCTOR && 
                chatResponse.getRecommendedDoctors() != null && 
                !chatResponse.getRecommendedDoctors().isEmpty() &&
                context != null && !isBlank(context.getPreferredSpecialization())) {
                String requestedSpec = context.getPreferredSpecialization().toUpperCase();
                List<DoctorSuggestionDTO> filteredDoctors = chatResponse.getRecommendedDoctors().stream()
                        .filter(doc -> doc.getSpecializations() != null && 
                                doc.getSpecializations().stream()
                                        .anyMatch(spec -> spec != null && spec.toUpperCase().equals(requestedSpec)))
                        .collect(Collectors.toList());
                
                if (filteredDoctors.size() != chatResponse.getRecommendedDoctors().size()) {
                    log.info("Filtered recommended doctors: {} -> {} (removed non-matching specializations)", 
                            chatResponse.getRecommendedDoctors().size(), filteredDoctors.size());
                }
                chatResponse.setRecommendedDoctors(filteredDoctors);
            }
            
            // Fallback: If AI didn't recommend any doctors but we have doctors in the catalogue and user is looking for a doctor,
            // recommend the top doctors anyway
            if (intent == InteractionIntent.FIND_DOCTOR && 
                (chatResponse.getRecommendedDoctors() == null || chatResponse.getRecommendedDoctors().isEmpty()) &&
                !doctorsToSend.isEmpty()) {
                log.warn("AI did not recommend any doctors, but catalogue has {} doctors. Adding top doctors as fallback.", doctorsToSend.size());
                // Take top 4 doctors from the catalogue, prioritizing by specialization match if context has one
                List<DoctorSuggestionDTO> fallbackDoctors;
                String requestedSpec = (context != null && !isBlank(context.getPreferredSpecialization())) 
                        ? context.getPreferredSpecialization().toUpperCase() 
                        : null;
                if (requestedSpec != null) {
                    // ONLY include doctors with the requested specialization - don't fall back to others
                    List<DoctorSuggestionDTO> matchingSpec = doctorsToSend.stream()
                            .filter(doc -> doc.getSpecializations() != null && 
                                    doc.getSpecializations().stream()
                                            .anyMatch(spec -> spec != null && spec.toUpperCase().equals(requestedSpec)))
                            .limit(MAX_DOCTORS_RETURNED_TO_PATIENT)
                            .collect(Collectors.toList());
                    // Only use matching doctors - don't fall back to non-matching ones
                    fallbackDoctors = matchingSpec;
                    log.info("Fallback: Found {} doctors matching specialization '{}'", matchingSpec.size(), requestedSpec);
                } else {
                    fallbackDoctors = doctorsToSend.stream()
                            .limit(MAX_DOCTORS_RETURNED_TO_PATIENT)
                            .collect(Collectors.toList());
                }
                
                chatResponse.setRecommendedDoctors(fallbackDoctors);
                chatResponse.setInformationComplete(true);
                
                // Update reply to mention we're showing available doctors
                String originalReply = chatResponse.getReply();
                String specializationName = (context != null && !isBlank(context.getPreferredSpecialization())) 
                        ? context.getPreferredSpecialization().toLowerCase().replace("_", " ") 
                        : "";
                if (originalReply != null && (originalReply.toLowerCase().contains("no doctors") || 
                                              originalReply.toLowerCase().contains("unfortunately") ||
                                              originalReply.toLowerCase().contains("don't have"))) {
                    // Replace the "no doctors" message with a positive one
                    if (!isBlank(specializationName)) {
                        chatResponse.setReply("I found " + fallbackDoctors.size() + " " + specializationName + 
                                " doctor(s) for you. Here are the best matches:");
                    } else {
                        chatResponse.setReply("I found " + fallbackDoctors.size() + 
                                " doctor(s) for you. Here are the best matches:");
                    }
                }
            }
            
            // FINAL FILTER: Ensure we never return doctors that don't match the requested specialization
            if (intent == InteractionIntent.FIND_DOCTOR && 
                chatResponse.getRecommendedDoctors() != null && 
                !chatResponse.getRecommendedDoctors().isEmpty() &&
                context != null && !isBlank(context.getPreferredSpecialization())) {
                String requestedSpec = context.getPreferredSpecialization().toUpperCase();
                List<DoctorSuggestionDTO> finalFiltered = chatResponse.getRecommendedDoctors().stream()
                        .filter(doc -> {
                            if (doc.getSpecializations() == null || doc.getSpecializations().isEmpty()) {
                                return false; // Exclude doctors with no specializations if one was requested
                            }
                            return doc.getSpecializations().stream()
                                    .anyMatch(spec -> spec != null && spec.toUpperCase().equals(requestedSpec));
                        })
                        .collect(Collectors.toList());
                
                if (finalFiltered.size() != chatResponse.getRecommendedDoctors().size()) {
                    log.warn("FINAL FILTER: Removed {} non-matching doctors. Returning {} matching doctors.", 
                            chatResponse.getRecommendedDoctors().size() - finalFiltered.size(), finalFiltered.size());
                }
                chatResponse.setRecommendedDoctors(finalFiltered);
            }
            
            if (intent != InteractionIntent.FIND_DOCTOR) {
                chatResponse.setRecommendedDoctors(new ArrayList<>());
                chatResponse.setInformationComplete(false);
            }
            return chatResponse;
        } catch (Exception ex) {
            log.error("OpenAI chat call failed: {}", ex.getMessage(), ex);
            return buildFallbackResponse(context, doctorsToSend, ex.getMessage());
        }
    }

    private List<Map<String, Object>> buildMessagesPayload(List<ChatMessageDTO> incomingMessages,
                                                           PatientContextDTO context,
                                                           List<DoctorSuggestionDTO> catalogue) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", buildSystemInstruction(context, catalogue)
        ));

        // Serialize doctor catalogue and add it to the system message for better visibility
        // Create a minimal version to reduce token usage
        List<Map<String, Object>> minimalCatalogue = catalogue.stream()
                .map(doc -> {
                    Map<String, Object> minimal = new HashMap<>();
                    minimal.put("id", doc.getId());
                    minimal.put("fullName", doc.getFullName());
                    minimal.put("specializations", doc.getSpecializations());
                    minimal.put("city", doc.getCity());
                    minimal.put("country", doc.getCountry());
                    // Truncate bio to max 100 characters
                    if (doc.getShortBio() != null && !doc.getShortBio().isEmpty()) {
                        minimal.put("shortBio", doc.getShortBio().length() > 100 
                                ? doc.getShortBio().substring(0, 100) + "..." 
                                : doc.getShortBio());
                    }
                    // Limit insurance to first 5
                    if (doc.getInsuranceAccepted() != null && !doc.getInsuranceAccepted().isEmpty()) {
                        minimal.put("insuranceAccepted", doc.getInsuranceAccepted().stream()
                                .limit(5)
                                .collect(Collectors.toList()));
                    }
                    // Only include consultation fee if not null
                    if (doc.getConsultationFee() != null) {
                        minimal.put("consultationFee", doc.getConsultationFee());
                    }
                    // Don't include profilePicture - it's too large
                    return minimal;
                })
                .collect(Collectors.toList());
        
        String catalogueJson;
        try {
            catalogueJson = objectMapper.writeValueAsString(minimalCatalogue);
            // Log catalogue size for debugging
            log.info("Sending {} doctors to AI in catalogue (minimal version, JSON length: {} chars)", 
                    catalogue.size(), catalogueJson.length());
            if (catalogue.isEmpty()) {
                log.warn("WARNING: Doctor catalogue is empty! No doctors will be available for recommendation.");
            }
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise doctor catalogue: {}", ex.getMessage());
            catalogueJson = "[]";
        }
        
        // Add catalogue as a separate user message - keep it SHORT to save tokens
        String catalogueMessage;
        if (catalogue.isEmpty()) {
            catalogueMessage = "DOCTOR_CATALOGUE_JSON: []\n\nNo doctors available.";
        } else {
            // Shortened message to save tokens
            catalogueMessage = "DOCTOR_CATALOGUE_JSON: " + catalogueJson + 
                    "\n\nIMPORTANT: This catalogue has " + catalogue.size() + " doctor(s). " +
                    "You MUST recommend 1-4 doctors from this catalogue. " +
                    "If patient asks for a specific type (e.g., 'psychiatry'), find matching doctors and recommend them immediately. " +
                    "NEVER say 'no doctors available' if catalogue is not empty.";
        }
        messages.add(Map.of(
                "role", "user",
                "content", catalogueMessage
        ));

        List<ChatMessageDTO> truncatedHistory = truncateHistory(incomingMessages);
        for (ChatMessageDTO message : truncatedHistory) {
            String role = message.getNormalisedRole();
            if (!"user".equals(role) && !"assistant".equals(role) && !"system".equals(role)) {
                role = "user";
            }
            messages.add(Map.of(
                    "role", role,
                    "content", Optional.ofNullable(message.getContent()).orElse("").trim()
            ));
        }
        return messages;
    }

    private void enrichContextFromMessages(PatientContextDTO context,
                                           List<ChatMessageDTO> messages,
                                           List<HealthcareProvider> providers) {
        if (context == null || CollectionUtils.isEmpty(messages)) {
            return;
        }

        ChatMessageDTO lastUserMessage = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessageDTO message = messages.get(i);
            if ("user".equals(message.getNormalisedRole())) {
                lastUserMessage = message;
                break;
            }
        }

        if (lastUserMessage == null || isBlank(lastUserMessage.getContent())) {
            return;
        }

        String content = lastUserMessage.getContent().toLowerCase(Locale.ROOT);

        // Extract insurance provider by matching against the insurance catalogue from our database.
        if (isBlank(context.getInsuranceProvider())) {
            String detectedInsurance = findInsuranceReference(content, providers);
            if (detectedInsurance != null) {
                context.setInsuranceProvider(detectedInsurance);
                log.debug("Detected insurance provider from patient message: {}", detectedInsurance);
            }
        }

        // Simple city extraction when patients explicitly mention "in <city>".
        if (isBlank(context.getCity())) {
            int inIndex = content.indexOf(" in ");
            if (inIndex >= 0) {
                String afterIn = content.substring(inIndex + 4).trim();
                // Stop at punctuation if present.
                int punctuation = afterIn.indexOf('.');
                if (punctuation > 0) {
                    afterIn = afterIn.substring(0, punctuation);
                }
                if (!afterIn.isBlank() && afterIn.length() <= 50) {
                    context.setCity(capitalise(afterIn));
                    log.debug("Detected city from patient message: {}", afterIn);
                }
            }
        }

        // If user references a state or country directly, capture it.
        if (isBlank(context.getState())) {
            String detectedState = findStateReference(content, providers);
            if (detectedState != null) {
                context.setState(detectedState);
            }
        }

        if (isBlank(context.getCountry())) {
            String detectedCountry = findCountryReference(content, providers);
            if (detectedCountry != null) {
                context.setCountry(detectedCountry);
            }
        }

        if (isBlank(context.getPreferredSpecialization())) {
            String detectedSpecialisation = detectSpecialisationFromMessage(content);
            if (detectedSpecialisation != null) {
                context.setPreferredSpecialization(detectedSpecialisation);
                log.debug("Detected specialization from patient message: {}", detectedSpecialisation);
            }
        }
    }

    private String findInsuranceReference(String message, List<HealthcareProvider> providers) {
        if (isBlank(message)) {
            return null;
        }

        String normalisedMessage = normaliseInsuranceName(message);
        Set<String> messageTokens = tokenise(normalisedMessage);

        // Build a list of distinct insurance providers from the supplied doctors to match against patient text.
        return providers.stream()
                .filter(Objects::nonNull)
                .map(HealthcareProvider::getInsuranceAccepted)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .filter(provider -> {
                    String normalisedProvider = normaliseInsuranceName(provider);
                    Set<String> providerTokens = tokenise(normalisedProvider);
                    return !providerTokens.isEmpty() && messageTokens.containsAll(providerTokens)
                            || normalisedMessage.contains(normalisedProvider)
                            || normalisedProvider.contains(normalisedMessage);
                })
                .findFirst()
                .orElse(null);
    }

    private String capitalise(String value) {
        if (isBlank(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 1) {
            return trimmed.toUpperCase(Locale.ROOT);
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private String normaliseInsuranceName(String value) {
        if (value == null) {
            return "";
        }
        String noParentheses = value.replaceAll("\\(.*?\\)", " ");
        return noParentheses.replaceAll("[^a-zA-Z0-9 ]", " ")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokenise(String input) {
        Set<String> tokens = new HashSet<>();
        if (input == null) {
            return tokens;
        }
        String[] parts = input.toLowerCase(Locale.ROOT).split("\\s+");
        for (String part : parts) {
            if (part.length() > 2) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private String findStateReference(String message, List<HealthcareProvider> providers) {
        if (isBlank(message) || CollectionUtils.isEmpty(providers)) {
            return null;
        }

        String lowerCaseMessage = message.toLowerCase(Locale.ROOT);

        return providers.stream()
                .map(HealthcareProvider::getState)
                .filter(this::isNotBlank)
                .map(String::trim)
                .distinct()
                .filter(state -> lowerCaseMessage.contains(state.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    private String findCountryReference(String message, List<HealthcareProvider> providers) {
        if (isBlank(message) || CollectionUtils.isEmpty(providers)) {
            return null;
        }

        String lowerCaseMessage = message.toLowerCase(Locale.ROOT);

        return providers.stream()
                .map(HealthcareProvider::getCountry)
                .filter(this::isNotBlank)
                .map(String::trim)
                .distinct()
                .filter(country -> lowerCaseMessage.contains(country.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private List<ChatMessageDTO> truncateHistory(List<ChatMessageDTO> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return List.of();
        }
        int size = messages.size();
        if (size <= MAX_HISTORY_LENGTH) {
            return messages;
        }
        return messages.subList(size - MAX_HISTORY_LENGTH, size);
    }

    private ChatResponseDTO parseResponse(String rawBody,
                                          PatientContextDTO previousContext,
                                          List<DoctorSuggestionDTO> catalogue) {
        try {
            JsonNode body = objectMapper.readTree(rawBody);
            JsonNode choices = body.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return buildFallbackResponse(previousContext, catalogue, "Assistant did not return a reply.");
            }

            JsonNode firstChoice = choices.get(0);
            String content = firstChoice.path("message").path("content").asText();

            ChatResponseDTO response = new ChatResponseDTO();
            response.setRawModelContent(content);

            if (content == null || content.isBlank()) {
                response.setReply("I'm here to help, but I couldn't understand the last response. Could you try again?");
                response.setContext(previousContext);
                response.setRecommendedDoctors(new ArrayList<>(
                        catalogue.subList(0, Math.min(2, catalogue.size()))
                ));
                return response;
            }

            JsonNode parsedContent;
            try {
                parsedContent = objectMapper.readTree(content);
            } catch (JsonProcessingException parsingError) {
                log.warn("Model response was not valid JSON. Returning graceful fallback.");
                response.setReply(content);
                response.setContext(previousContext);
                response.setRecommendedDoctors(new ArrayList<>(
                        catalogue.subList(0, Math.min(2, catalogue.size()))
                ));
                return response;
            }

            String reply = parsedContent.path("reply").asText(content);
            response.setReply(reply);

            PatientContextDTO updatedContext = Optional.ofNullable(
                    objectMapper.convertValue(parsedContent.path("context"), PatientContextDTO.class)
            ).orElse(new PatientContextDTO());
            mergeContexts(previousContext, updatedContext);
            response.setContext(updatedContext);

            List<Long> doctorIds = new ArrayList<>();
            JsonNode doctorNode = parsedContent.path("recommendedDoctorIds");
            if (doctorNode.isArray()) {
                doctorNode.forEach(idNode -> {
                    if (idNode.canConvertToLong()) {
                        doctorIds.add(idNode.longValue());
                    }
                });
            }

            // Filter to only include doctors that match the requested specialization (if specified)
            List<DoctorSuggestionDTO> recommended = catalogue.stream()
                    .filter(dto -> doctorIds.contains(dto.getId()))
                    .filter(dto -> {
                        // If a specialization is requested, only include doctors with that specialization
                        if (previousContext != null && !isBlank(previousContext.getPreferredSpecialization())) {
                            String requestedSpec = previousContext.getPreferredSpecialization().toUpperCase();
                            if (dto.getSpecializations() != null && !dto.getSpecializations().isEmpty()) {
                                return dto.getSpecializations().stream()
                                        .anyMatch(spec -> spec != null && spec.toUpperCase().equals(requestedSpec));
                            }
                            return false; // No specializations listed, exclude if specialization was requested
                        }
                        return true; // No specialization requested, include all
                    })
                    .limit(MAX_DOCTORS_RETURNED_TO_PATIENT)
                    .collect(Collectors.toCollection(ArrayList::new));
            
            log.info("AI recommended {} doctor IDs: {}. After specialization filtering: {} doctors match.", 
                    doctorIds.size(), doctorIds, recommended.size());

            // Log what the AI recommended
            log.info("AI recommended {} doctor IDs: {}. Catalogue had {} doctors.", 
                    doctorIds.size(), doctorIds, catalogue.size());
            
            // If AI didn't recommend any doctors but catalogue has doctors, log a warning
            if (recommended.isEmpty() && !catalogue.isEmpty()) {
                log.warn("AI did not recommend any doctors from catalogue of {} doctors. Doctor IDs in catalogue: {}", 
                        catalogue.size(), 
                        catalogue.stream().map(DoctorSuggestionDTO::getId).limit(5).collect(Collectors.toList()));
            }

            if (recommended.isEmpty()) {
                response.setNavigationTips(buildNavigationTips(previousContext));
            }
            response.setRecommendedDoctors(recommended);

            List<String> followUps = new ArrayList<>();
            JsonNode followUpsNode = parsedContent.path("followUpQuestions");
            if (followUpsNode.isArray()) {
                followUpsNode.forEach(node -> {
                    if (node.isTextual()) {
                        followUps.add(node.asText());
                    }
                });
            }
            if (followUps.isEmpty()) {
                followUps.addAll(generateFollowUpPrompts(updatedContext));
            } else {
                followUps.addAll(generateFollowUpPrompts(updatedContext).stream()
                        .filter(prompt -> followUps.stream().noneMatch(existing -> existing.equalsIgnoreCase(prompt)))
                        .collect(Collectors.toList()));
            }
            response.setFollowUpQuestions(followUps);

            response.setInformationComplete(parsedContent.path("informationComplete").asBoolean(false));
            JsonNode navigationNode = parsedContent.path("navigationTips");
            if (navigationNode.isArray()) {
                List<String> tips = new ArrayList<>();
                navigationNode.forEach(node -> {
                    if (node.isTextual()) {
                        tips.add(node.asText());
                    }
                });
                response.setNavigationTips(tips);
            }
            return response;
        } catch (Exception ex) {
            log.error("Failed to parse OpenAI response: {}", ex.getMessage(), ex);
            return buildFallbackResponse(previousContext, catalogue, "Sorry, something went wrong while parsing the assistant response.");
        }
    }

    private void mergeContexts(PatientContextDTO base, PatientContextDTO updates) {
        if (base == null || updates == null) {
            return;
        }
        if (isBlank(updates.getAgeRange())) {
            updates.setAgeRange(base.getAgeRange());
        }
        if (isBlank(updates.getPrimaryConcern())) {
            updates.setPrimaryConcern(base.getPrimaryConcern());
        }
        if (isBlank(updates.getSymptomDuration())) {
            updates.setSymptomDuration(base.getSymptomDuration());
        }
        if (isBlank(updates.getSymptomSeverity())) {
            updates.setSymptomSeverity(base.getSymptomSeverity());
        }
        if (isBlank(updates.getAdditionalSymptoms())) {
            updates.setAdditionalSymptoms(base.getAdditionalSymptoms());
        }
        if (isBlank(updates.getMedicalHistory())) {
            updates.setMedicalHistory(base.getMedicalHistory());
        }
        if (isBlank(updates.getMedications())) {
            updates.setMedications(base.getMedications());
        }
        if (isBlank(updates.getAllergies())) {
            updates.setAllergies(base.getAllergies());
        }
        if (isBlank(updates.getPreferredDoctorGender())) {
            updates.setPreferredDoctorGender(base.getPreferredDoctorGender());
        }
        if (isBlank(updates.getPreferredLanguage())) {
            updates.setPreferredLanguage(base.getPreferredLanguage());
        }
        if (isBlank(updates.getInsuranceProvider())) {
            updates.setInsuranceProvider(base.getInsuranceProvider());
        }
        if (isBlank(updates.getCity())) {
            updates.setCity(base.getCity());
        }
        if (isBlank(updates.getState())) {
            updates.setState(base.getState());
        }
        if (isBlank(updates.getCountry())) {
            updates.setCountry(base.getCountry());
        }
        if (isBlank(updates.getPostalCode())) {
            updates.setPostalCode(base.getPostalCode());
        }
        if (isBlank(updates.getPreferredSpecialization())) {
            updates.setPreferredSpecialization(base.getPreferredSpecialization());
        }
        if (isBlank(updates.getAppointmentPreference())) {
            updates.setAppointmentPreference(base.getAppointmentPreference());
        }
    }

    private ChatResponseDTO buildFallbackResponse(PatientContextDTO context,
                                                  List<DoctorSuggestionDTO> doctorCatalogue,
                                                  String reason) {
        ChatResponseDTO response = new ChatResponseDTO();
        response.setReply("I'm sorry, I'm having trouble connecting right now. "
                + "Here are some doctors you can review while I get back online.");
        response.setContext(context);

        List<DoctorSuggestionDTO> safeList = new ArrayList<>(doctorCatalogue.subList(
                0, Math.min(3, doctorCatalogue.size())
        ));
        response.setRecommendedDoctors(safeList);
        response.setInformationComplete(!safeList.isEmpty());
        response.setNavigationTips(buildNavigationTips(context));
        response.setRawModelContent(reason);
        return response;
    }

    private ChatResponseDTO buildNoMatchResponse(PatientContextDTO context,
                                                 String reasonDescription) {
        ChatResponseDTO response = new ChatResponseDTO();
        String message = reasonDescription.isBlank()
                ? "I couldn’t find doctors that match all of your requirements just yet."
                : "I couldn’t find doctors currently matching %s. Let’s adjust your filters or explore other options together.".formatted(reasonDescription);
        response.setReply(message);
        response.setContext(context);

        response.setRecommendedDoctors(new ArrayList<>());
        response.setInformationComplete(false);

        List<String> tips = buildNavigationTips(context);
        tips.add("Tip: Try broadening your insurance or location filters to see additional doctors.");
        response.setNavigationTips(tips);
        return response;
    }

    private String buildSystemInstruction(PatientContextDTO context,
                                          List<DoctorSuggestionDTO> catalogue) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("reply", "string");
        schema.put("context", schemaForContext());
        schema.put("recommendedDoctorIds", "array<number>");
        schema.put("followUpQuestions", "array<string>");
        schema.put("informationComplete", "boolean");
        schema.put("navigationTips", "array<string>");

        String schemaJson;
        String contextJson;
        try {
            schemaJson = objectMapper.writeValueAsString(schema);
            contextJson = objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise schema/context: {}", ex.getMessage());
            schemaJson = "{ \"reply\": \"string\" }";
            contextJson = "{}";
        }

        // Build a more explicit system instruction
        String catalogueInfo = catalogue.isEmpty() 
            ? "WARNING: The doctor catalogue is currently empty. You should inform the patient that no doctors are available at this time."
            : "The doctor catalogue contains " + catalogue.size() + " doctor(s). You MUST recommend doctors from this catalogue.";
        
        // SHORTENED system prompt to save tokens (reduced from ~2000 to ~300 tokens)
        return """
                You are MediConnect's AI Care Navigator. Recommend doctors from DOCTOR_CATALOGUE_JSON.

                RULES:
                1. %s
                2. If DOCTOR_CATALOGUE_JSON has doctors, you MUST recommend 1-4 doctor IDs in recommendedDoctorIds.
                3. If patient asks for a type (e.g., "psychiatry"), find doctors with that specialization and recommend immediately.
                4. NEVER say "no doctors available" if catalogue has doctors.
                5. Recommend first, ask for details (city/insurance) after.

                JSON schema: %s
                Context: %s
                """.formatted(catalogueInfo, schemaJson, contextJson).trim();
    }

    private Map<String, Object> schemaForContext() {
        Map<String, Object> contextSchema = new LinkedHashMap<>();
        contextSchema.put("ageRange", "string|null");
        contextSchema.put("primaryConcern", "string|null");
        contextSchema.put("symptomDuration", "string|null");
        contextSchema.put("symptomSeverity", "string|null");
        contextSchema.put("additionalSymptoms", "string|null");
        contextSchema.put("medicalHistory", "string|null");
        contextSchema.put("medications", "string|null");
        contextSchema.put("allergies", "string|null");
        contextSchema.put("preferredDoctorGender", "string|null");
        contextSchema.put("preferredLanguage", "string|null");
        contextSchema.put("insuranceProvider", "string|null");
        contextSchema.put("city", "string|null");
        contextSchema.put("state", "string|null");
        contextSchema.put("country", "string|null");
        contextSchema.put("postalCode", "string|null");
        contextSchema.put("preferredSpecialization", "string|null");
        contextSchema.put("appointmentPreference", "string|null");
        return contextSchema;
    }

    private List<DoctorSuggestionDTO> loadDoctorCatalogue(PatientContextDTO context,
                                                          List<HealthcareProvider> providers) {
        log.info("Loading doctor catalogue: Found {} total providers from database", providers.size());
        
        // Log account status distribution
        Map<AccountStatus, Long> statusCount = providers.stream()
                .collect(Collectors.groupingBy(
                        provider -> provider.getAccountStatus() != null ? provider.getAccountStatus() : AccountStatus.PENDING,
                        Collectors.counting()));
        log.info("Provider account status distribution: {}", statusCount);
        
        // Log admin-flagged count
        long flaggedCount = providers.stream()
                .filter(provider -> Boolean.TRUE.equals(provider.getAdminFlagged()))
                .count();
        log.info("Admin-flagged providers: {}", flaggedCount);
        
        List<HealthcareProvider> activeProviders = providers.stream()
                .filter(provider -> provider.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(provider -> !Boolean.TRUE.equals(provider.getAdminFlagged()))
                .toList();
        
        log.info("After filtering (ACTIVE and not admin-flagged): {} providers remain", activeProviders.size());
        
        // TEMPORARY FIX: If no active providers, use all non-flagged providers regardless of status
        // This helps diagnose the issue and ensures doctors are shown
        if (activeProviders.isEmpty() && !providers.isEmpty()) {
            log.warn("WARNING: No active providers found! Total providers: {}, Statuses: {}, Flagged: {}", 
                    providers.size(), statusCount, flaggedCount);
            // If no active providers, let's see what statuses we have
            providers.stream()
                    .limit(5)
                    .forEach(p -> log.warn("Sample provider - ID: {}, Status: {}, Flagged: {}, Username: {}", 
                            p.getId(), p.getAccountStatus(), p.getAdminFlagged(), p.getUsername()));
            
            // Fallback: Use all non-flagged providers regardless of status
            log.warn("FALLBACK: Using all non-flagged providers regardless of account status");
            activeProviders = providers.stream()
                    .filter(provider -> !Boolean.TRUE.equals(provider.getAdminFlagged()))
                    .toList();
            log.info("After fallback filtering (not admin-flagged): {} providers remain", activeProviders.size());
        }

        Map<Long, Set<String>> providerInsuranceMap = activeProviders.stream()
                .collect(Collectors.toMap(
                        HealthcareProvider::getId,
                        provider -> Optional.ofNullable(provider.getInsuranceAccepted()).orElse(List.of()).stream()
                                .map(this::normaliseInsuranceName)
                                .collect(Collectors.toCollection(HashSet::new))
                ));

        Map<Long, Set<SpecializationType>> providerSpecialityMap = activeProviders.stream()
                .collect(Collectors.toMap(
                        HealthcareProvider::getId,
                        provider -> provider.getSpecializations() == null
                                ? Set.of()
                                : new HashSet<>(provider.getSpecializations())
                ));

        String inferredSpecialisation = inferPreferredSpecialisation(context, providerSpecialityMap);
        log.info("Inferred specialization: {}", inferredSpecialisation);

        List<DoctorSuggestionDTO> catalogue = activeProviders.stream()
                .map(provider -> {
                    try {
                        return toSuggestion(provider, context, inferredSpecialisation,
                                providerInsuranceMap.getOrDefault(provider.getId(), Set.of()),
                                providerSpecialityMap.getOrDefault(provider.getId(), Set.of()));
                    } catch (Exception e) {
                        log.error("Error converting provider {} to DTO: {}", provider.getId(), e.getMessage(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(
                        (DoctorSuggestionDTO dto) -> dto.getMatchScore() != null ? dto.getMatchScore() : 0.0d
                ).reversed())
                .limit(MAX_DOCTORS_SHARED_WITH_MODEL)
                .collect(Collectors.toList());
        
        log.info("Created doctor catalogue with {} doctors", catalogue.size());
        if (!catalogue.isEmpty()) {
            log.debug("Sample doctor IDs in catalogue: {}", 
                    catalogue.stream().map(DoctorSuggestionDTO::getId).limit(3).collect(Collectors.toList()));
        }
        
        return catalogue;
    }

    private DoctorSuggestionDTO toSuggestion(HealthcareProvider provider,
                                             PatientContextDTO context,
                                             String inferredSpecialisation,
                                             Set<String> insuranceSet,
                                             Set<SpecializationType> specializations) {
        double score = BASE_MATCH_SCORE
                + matchLocationScore(provider, context)
                + matchInsuranceScore(context, insuranceSet)
                + matchSpecialisationScore(context, inferredSpecialisation, specializations);

        List<String> specialisationNames = specializations == null
                ? List.of()
                : specializations.stream().map(Enum::name).collect(Collectors.toList());

        List<String> insuranceList = insuranceSet == null
                ? List.of()
                : insuranceSet.stream().filter(Objects::nonNull).map(String::trim).filter(val -> !val.isEmpty()).collect(Collectors.toList());

        return DoctorSuggestionDTO.builder()
                .id(provider.getId())
                .fullName(buildDoctorName(provider))
                .clinicName(trimToNull(provider.getClinicName()))
                .city(trimToNull(provider.getCity()))
                .state(trimToNull(provider.getState()))
                .country(trimToNull(provider.getCountry()))
                .consultationFee(provider.getConsultationFee())
                .shortBio(trimToNull(provider.getBio()))
                .profilePicture(trimToNull(provider.getProfilePicture()))
                .specializations(specialisationNames)
                .insuranceAccepted(insuranceList)
                .matchScore(score)
                .build();
    }

    private double matchLocationScore(HealthcareProvider provider, PatientContextDTO context) {
        double score = 0;
        if (!isBlank(context.getCity()) && equalsIgnoreCase(context.getCity(), provider.getCity())) {
            score += 3.5;
        }
        if (!isBlank(context.getState()) && equalsIgnoreCase(context.getState(), provider.getState())) {
            score += 1.5;
        }
        if (!isBlank(context.getCountry()) && equalsIgnoreCase(context.getCountry(), provider.getCountry())) {
            score += 1.0;
        }
        return score;
    }

    private double matchInsuranceScore(PatientContextDTO context, Set<String> acceptedInsurances) {
        if (isBlank(context.getInsuranceProvider()) || acceptedInsurances == null || acceptedInsurances.isEmpty()) {
            return 0;
        }
        Set<String> desiredTokens = tokenise(normaliseInsuranceName(context.getInsuranceProvider()));
        if (desiredTokens.isEmpty()) {
            return 0;
        }
        return acceptedInsurances.stream()
                .map(this::normaliseInsuranceName)
                .map(this::tokenise)
                .anyMatch(tokens -> !tokens.isEmpty() && tokens.containsAll(desiredTokens)) ? 3.0 : 0.0;
    }

    private double matchSpecialisationScore(PatientContextDTO context,
                                            String inferredSpecialisation,
                                            Set<SpecializationType> providerSpecialisations) {
        if (providerSpecialisations == null || providerSpecialisations.isEmpty()) {
            return 0;
        }

        String preference = !isBlank(context.getPreferredSpecialization())
                ? context.getPreferredSpecialization()
                : inferredSpecialisation;

        if (isBlank(preference)) {
            return 0;
        }

        String normalisedPreference = preference.trim().toUpperCase(Locale.ROOT);
        return providerSpecialisations.stream()
                .map(Enum::name)
                .anyMatch(spec -> spec.equalsIgnoreCase(normalisedPreference))
                ? 4.0
                : 0.0;
    }

    private String inferPreferredSpecialisation(PatientContextDTO context,
                                                Map<Long, Set<SpecializationType>> providerSpecialityMap) {
        if (context == null) {
            return null;
        }
        if (!isBlank(context.getPreferredSpecialization())) {
            return context.getPreferredSpecialization();
        }

        String combinedText = StreamBuilder.start()
                .append(context.getPrimaryConcern())
                .append(context.getAdditionalSymptoms())
                .append(context.getInsuranceProvider())
                .append(context.getAppointmentPreference())
                .build()
                .toLowerCase(Locale.ROOT);

        if (combinedText.isBlank()) {
            return null;
        }

        // Attempt explicit keyword match first.
        Optional<String> keywordMatch = SPECIALISATION_KEYWORDS.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(combinedText::contains))
                .map(entry -> entry.getKey().name())
                .findFirst();
        if (keywordMatch.isPresent()) {
            return keywordMatch.get();
        }

        // Fall back to selecting the most common specialization among doctors when symptoms are vague.
        return providerSpecialityMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.groupingBy(Enum::name, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static Map<SpecializationType, Set<String>> buildSpecialisationKeywordMap() {
        Map<SpecializationType, Set<String>> map = new EnumMap<>(SpecializationType.class);
        map.put(SpecializationType.CARDIOLOGY, Set.of("heart", "chest pain", "palpitations", "cardio"));
        map.put(SpecializationType.DERMATOLOGY, Set.of("skin", "rash", "acne", "eczema", "psoriasis"));
        map.put(SpecializationType.NEUROLOGY, Set.of("migraine", "headache", "numbness", "seizure", "stroke"));
        map.put(SpecializationType.PEDIATRICS, Set.of("child", "kid", "infant", "pediatric"));
        map.put(SpecializationType.FAMILY_MEDICINE, Set.of("annual checkup", "family doctor", "general doctor"));
        map.put(SpecializationType.PSYCHIATRY, Set.of("anxiety", "depression", "mental health", "stress"));
        map.put(SpecializationType.ORTHOPEDIC_SURGERY, Set.of("joint", "bone", "knee", "shoulder", "fracture"));
        map.put(SpecializationType.OBSTETRICS_GYNECOLOGY, Set.of("pregnant", "pregnancy", "obgyn", "gyneco", "women's health"));
        map.put(SpecializationType.UROLOGY, Set.of("urinary", "bladder", "kidney stone", "prostate"));
        map.put(SpecializationType.NEUROSURGERY, Set.of("brain surgery", "spine surgery"));
        map.put(SpecializationType.INTERNAL_MEDICINE, Set.of("diabetes", "blood pressure", "cholesterol", "fatigue"));
        map.put(SpecializationType.OPHTHALMOLOGY, Set.of("vision", "eye", "blurry", "optometrist"));
        return map;
    }

    private static Map<String, String> buildSpecialisationSynonymMap() {
        Map<String, String> map = new HashMap<>();
        map.put("neurology", SpecializationType.NEUROLOGY.name());
        map.put("neurologist", SpecializationType.NEUROLOGY.name());
        map.put("brain doctor", SpecializationType.NEUROLOGY.name());
        map.put("cardiology", SpecializationType.CARDIOLOGY.name());
        map.put("cardiologist", SpecializationType.CARDIOLOGY.name());
        map.put("heart doctor", SpecializationType.CARDIOLOGY.name());
        map.put("dermatology", SpecializationType.DERMATOLOGY.name());
        map.put("dermatologist", SpecializationType.DERMATOLOGY.name());
        map.put("skin doctor", SpecializationType.DERMATOLOGY.name());
        map.put("pediatrics", SpecializationType.PEDIATRICS.name());
        map.put("pediatrician", SpecializationType.PEDIATRICS.name());
        map.put("child doctor", SpecializationType.PEDIATRICS.name());
        map.put("psychiatry", SpecializationType.PSYCHIATRY.name());
        map.put("psychiatrist", SpecializationType.PSYCHIATRY.name());
        map.put("mental health doctor", SpecializationType.PSYCHIATRY.name());
        map.put("orthopedic", SpecializationType.ORTHOPEDIC_SURGERY.name());
        map.put("orthopedic surgeon", SpecializationType.ORTHOPEDIC_SURGERY.name());
        map.put("bone doctor", SpecializationType.ORTHOPEDIC_SURGERY.name());
        map.put("gynecology", SpecializationType.OBSTETRICS_GYNECOLOGY.name());
        map.put("gynecologist", SpecializationType.OBSTETRICS_GYNECOLOGY.name());
        map.put("obgyn", SpecializationType.OBSTETRICS_GYNECOLOGY.name());
        map.put("women's health", SpecializationType.OBSTETRICS_GYNECOLOGY.name());
        map.put("urology", SpecializationType.UROLOGY.name());
        map.put("urologist", SpecializationType.UROLOGY.name());
        map.put("kidney doctor", SpecializationType.UROLOGY.name());
        map.put("internal medicine", SpecializationType.INTERNAL_MEDICINE.name());
        map.put("internist", SpecializationType.INTERNAL_MEDICINE.name());
        map.put("ophthalmology", SpecializationType.OPHTHALMOLOGY.name());
        map.put("ophthalmologist", SpecializationType.OPHTHALMOLOGY.name());
        map.put("eye doctor", SpecializationType.OPHTHALMOLOGY.name());
        map.put("family doctor", SpecializationType.FAMILY_MEDICINE.name());
        map.put("family medicine", SpecializationType.FAMILY_MEDICINE.name());
        return map;
    }

    private String buildDoctorName(HealthcareProvider provider) {
        String firstName = trimToNull(provider.getFirstName());
        String lastName = trimToNull(provider.getLastName());
        if (firstName == null && lastName == null) {
            return "Doctor";
        }
        if (lastName == null) {
            return "Dr. " + firstName;
        }
        if (firstName == null) {
            return "Dr. " + lastName;
        }
        return "Dr. " + firstName + " " + lastName;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int levenshteinDistance(String left, String right) {
        if (left == null || right == null) {
            return Integer.MAX_VALUE;
        }
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[left.length()][right.length()];
    }

    /**
     * Small helper to concatenate optional strings without sprinkling null checks everywhere.
     */
    private static class StreamBuilder {
        private final StringBuilder builder = new StringBuilder();

        static StreamBuilder start() {
            return new StreamBuilder();
        }

        StreamBuilder append(String value) {
            if (value != null && !value.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(value.trim());
            }
            return this;
        }

        String build() {
            return builder.toString();
        }
    }

    private List<String> buildNavigationTips(PatientContextDTO context) {
        List<String> tips = new ArrayList<>();
        tips.add("You can browse all doctors using the Find Doctors page and apply filters like insurance, city, and specialization.");
        tips.add("Once you select a doctor, you can request an appointment and choose whether to share your medical records.");
        if (!isBlank(context != null ? context.getInsuranceProvider() : null)) {
            tips.add("Tip: When viewing doctor profiles, look at the Insurance section to confirm they accept %s.".formatted(context.getInsuranceProvider()));
        }
        return tips;
    }

    private List<String> generateFollowUpPrompts(PatientContextDTO context) {
        List<String> prompts = new ArrayList<>();
        if (context == null || isBlank(context.getPrimaryConcern())) {
            prompts.add("What is your primary concern or symptom?");
        }
        if (context == null || isBlank(context.getInsuranceProvider())) {
            prompts.add("Do you have a preferred insurance provider?");
        }
        if (context == null || (isBlank(context.getCity()) && isBlank(context.getCountry()))) {
            prompts.add("Which city or country would you like to find a doctor in?");
        }
        if (context == null || isBlank(context.getPreferredSpecialization())) {
            prompts.add("Do you know which specialization you prefer, such as Neurology or Cardiology?");
        }
        if (context == null || isBlank(context.getAppointmentPreference())) {
            prompts.add("Would you like an in-person visit or an online consultation?");
        }
        return prompts;
    }

    private void resetContextForNavigation(PatientContextDTO context) {
        if (context == null) {
            return;
        }
        context.setInsuranceProvider(null);
        context.setPreferredSpecialization(null);
        context.setCity(null);
        context.setState(null);
        context.setCountry(null);
        context.setAppointmentPreference(null);
    }

    private RecommendationOutcome buildRankedRecommendations(PatientContextDTO context,
                                                             List<DoctorSuggestionDTO> catalogue,
                                                             boolean includeDoctorScoring) {
        log.info("buildRankedRecommendations CALLED: catalogue size={}, includeDoctorScoring={}", 
                catalogue != null ? catalogue.size() : 0, includeDoctorScoring);
        
        RecommendationOutcome outcome = new RecommendationOutcome();
        if (catalogue == null || catalogue.isEmpty()) {
            log.warn("buildRankedRecommendations: catalogue is null or empty");
            return outcome;
        }

        if (!includeDoctorScoring) {
            // Navigation-only questions: keep catalogue empty so the model focuses on guidance.
            log.warn("buildRankedRecommendations: includeDoctorScoring is false, returning empty catalogue");
            outcome.payloadCatalogue = List.of();
            return outcome;
        }
        
        // CRITICAL: Always ensure we return doctors if catalogue is not empty
        // This is a safety net - if anything goes wrong, we still return doctors
        outcome.payloadCatalogue = new ArrayList<>(catalogue.stream()
                .limit(MAX_DOCTORS_SHARED_WITH_MODEL)
                .collect(Collectors.toList()));
        
        log.info("buildRankedRecommendations: Initial payloadCatalogue set to {} doctors (safety net)", outcome.payloadCatalogue.size());

        Double topSpecialisationScore = null;
        Double topInsuranceScore = null;
        Double topLocationScore = null;

        log.info("Starting to score {} doctors from catalogue", catalogue.size());
        for (DoctorSuggestionDTO doctor : catalogue) {
            double specialityScore = matchSpecialisationScore(context, doctor.getSpecializations());
            double insuranceScore = matchInsuranceScore(context, new HashSet<>(Optional.ofNullable(doctor.getInsuranceAccepted()).orElse(List.of()).stream()
                    .map(this::normaliseInsuranceName)
                    .collect(Collectors.toList())));
            double locationScore = matchLocationScore(doctor, context);
            double totalScore = specialityScore + insuranceScore + locationScore;

            RankedDoctor ranked = new RankedDoctor(doctor, totalScore, specialityScore, insuranceScore, locationScore);
            outcome.ranked.add(ranked);

            topSpecialisationScore = topSpecialisationScore == null ? specialityScore : Math.max(topSpecialisationScore, specialityScore);
            topInsuranceScore = topInsuranceScore == null ? insuranceScore : Math.max(topInsuranceScore, insuranceScore);
            topLocationScore = topLocationScore == null ? locationScore : Math.max(topLocationScore, locationScore);
        }
        log.info("Scored {} doctors, outcome.ranked size: {}", catalogue.size(), outcome.ranked.size());

        // Set outcome scores before filtering (they're used in filtering logic)
        outcome.topSpecialisationScore = Optional.ofNullable(topSpecialisationScore).orElse(0.0);
        outcome.topInsuranceScore = Optional.ofNullable(topInsuranceScore).orElse(0.0);
        outcome.topLocationScore = Optional.ofNullable(topLocationScore).orElse(0.0);
        
        log.info("Scoring complete - topSpecialisationScore: {}, topInsuranceScore: {}, topLocationScore: {}, ranked doctors: {}", 
                outcome.topSpecialisationScore, outcome.topInsuranceScore, outcome.topLocationScore, outcome.ranked.size());
        
        // Sort by total score (highest first)
        List<RankedDoctor> sorted = outcome.ranked.stream()
                .sorted(Comparator.comparingDouble(RankedDoctor::totalScore).reversed())
                .collect(Collectors.toList());
        
        log.info("Sorted {} ranked doctors from {} in outcome.ranked", sorted.size(), outcome.ranked.size());
        
        // CRITICAL FIX: If outcome.ranked is empty, use catalogue directly
        // This can happen if there's an issue with the scoring loop
        if (sorted.isEmpty() && !catalogue.isEmpty()) {
            log.warn("outcome.ranked is empty but catalogue has {} doctors! Using catalogue directly.", catalogue.size());
            outcome.payloadCatalogue = catalogue.stream()
                    .limit(MAX_DOCTORS_SHARED_WITH_MODEL)
                    .collect(Collectors.toList());
            log.info("EMERGENCY: Using catalogue directly, payloadCatalogue now has {} doctors", outcome.payloadCatalogue.size());
            return outcome;
        }
        
        // SIMPLIFIED APPROACH: Always include all doctors, just sorted by score
        // Don't filter them out - let the AI decide which ones to recommend
        // This ensures doctors are always available for recommendation
        List<RankedDoctor> filtered = sorted;
        
        log.info("Using all {} sorted doctors (no filtering applied)", filtered.size());
        
        // Only overwrite payloadCatalogue if we actually have doctors from the mapping
        List<DoctorSuggestionDTO> mappedDoctors = filtered.stream()
                .map(RankedDoctor::doctor)
                .limit(MAX_DOCTORS_SHARED_WITH_MODEL)
                .collect(Collectors.toList());
        
        // Only update if we got doctors, otherwise keep the safety net value
        if (!mappedDoctors.isEmpty()) {
            outcome.payloadCatalogue = mappedDoctors;
        }
        
        log.info("buildRankedRecommendations: {} doctors in payloadCatalogue (filtered size: {}, sorted size: {}, catalogue size: {}, mapped size: {})", 
                outcome.payloadCatalogue.size(), filtered.size(), sorted.size(), catalogue.size(), mappedDoctors.size());
        
        // Final safety check: if payloadCatalogue is still empty but we have doctors, use catalogue directly
        if (outcome.payloadCatalogue.isEmpty() && !catalogue.isEmpty()) {
            log.error("EMERGENCY FALLBACK: payloadCatalogue is empty, using catalogue directly");
            outcome.payloadCatalogue = new ArrayList<>(catalogue.stream()
                    .limit(MAX_DOCTORS_SHARED_WITH_MODEL)
                    .collect(Collectors.toList()));
        }
        
        // ABSOLUTE FINAL CHECK: If still empty, something is very wrong - force it
        if (outcome.payloadCatalogue == null || outcome.payloadCatalogue.isEmpty()) {
            if (!catalogue.isEmpty()) {
                log.error("CRITICAL: payloadCatalogue is STILL empty/null! Forcing catalogue. This should never happen.");
                outcome.payloadCatalogue = new ArrayList<>(catalogue);
            }
        }
        
        log.info("buildRankedRecommendations FINAL RETURN: payloadCatalogue size = {}", 
                outcome.payloadCatalogue != null ? outcome.payloadCatalogue.size() : 0);

        return outcome;
    }

    private void mergeRecommendationGuidance(ChatResponseDTO response,
                                             RecommendationOutcome outcome,
                                             boolean highlightDoctorMatches) {
        if (response == null || outcome == null) {
            return;
        }

        List<String> tips = new ArrayList<>(Optional.ofNullable(response.getNavigationTips()).orElse(List.of()));

        if (highlightDoctorMatches && outcome.topSpecialisationScore <= 0.0 && !isBlank(response.getContext() != null ? response.getContext().getPreferredSpecialization() : null)) {
            tips.add("No doctors exactly matched specialization %s. Showing the closest matches available.".formatted(response.getContext().getPreferredSpecialization()));
        }
        if (highlightDoctorMatches && outcome.topInsuranceScore <= 0.0 && !isBlank(response.getContext() != null ? response.getContext().getInsuranceProvider() : null)) {
            tips.add("No doctors currently accept %s. Browse available doctors and check their insurance list manually.".formatted(response.getContext().getInsuranceProvider()));
        }
        if (highlightDoctorMatches && outcome.topLocationScore <= 0.0 && (!isBlank(response.getContext() != null ? response.getContext().getCity() : null) || !isBlank(response.getContext() != null ? response.getContext().getCountry() : null))) {
            tips.add("We didn’t find doctors in your specified location yet. You can still book a doctor and ask about remote consultations.");
        }

        if (tips.isEmpty()) {
            tips.add("Tap a doctor to view their profile, accepted insurance, and book an appointment in minutes.");
        }

        response.setNavigationTips(tips);
    }

    private double matchSpecialisationScore(PatientContextDTO context, List<String> doctorSpecialisations) {
        if (context == null || doctorSpecialisations == null || doctorSpecialisations.isEmpty()) {
            return 0;
        }
        if (!isBlank(context.getPreferredSpecialization())) {
            final String desired = context.getPreferredSpecialization().trim().toUpperCase(Locale.ROOT);
            // Check if any doctor specialization matches (case-insensitive)
            boolean matches = doctorSpecialisations.stream()
                    .anyMatch(spec -> spec != null && spec.trim().toUpperCase(Locale.ROOT).equals(desired));
            if (matches) {
                log.debug("Specialization match found: desired='{}', doctor specializations={}", desired, doctorSpecialisations);
                return 4.0;
            }
            log.debug("No specialization match: desired='{}', doctor specializations={}", desired, doctorSpecialisations);
            return 0;
        }
        return 2.0;
    }

    private double matchLocationScore(DoctorSuggestionDTO doctor, PatientContextDTO context) {
        if (doctor == null) {
            return 0;
        }
        double score = 0;
        if (!isBlank(context != null ? context.getCity() : null) && equalsIgnoreCase(context.getCity(), doctor.getCity())) {
            score += 3.0;
        } else if (isBlank(context != null ? context.getCity() : null) && !isBlank(doctor.getCity())) {
            score += 0.5;
        }
        if (!isBlank(context != null ? context.getState() : null) && equalsIgnoreCase(context.getState(), doctor.getState())) {
            score += 1.5;
        }
        if (!isBlank(context != null ? context.getCountry() : null) && equalsIgnoreCase(context.getCountry(), doctor.getCountry())) {
            score += 1.0;
        } else if (isBlank(context != null ? context.getCountry() : null) && !isBlank(doctor.getCountry())) {
            score += 0.5;
        }
        return score;
    }

    private record RankedDoctor(DoctorSuggestionDTO doctor,
                                double totalScore,
                                double specialisationScore,
                                double insuranceScore,
                                double locationScore) {
    }

    private static class RecommendationOutcome {
        List<RankedDoctor> ranked = new ArrayList<>();
        List<DoctorSuggestionDTO> payloadCatalogue = new ArrayList<>();
        double topSpecialisationScore;
        double topInsuranceScore;
        double topLocationScore;
    }

    private InteractionIntent detectIntent(List<ChatMessageDTO> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return InteractionIntent.GENERAL;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessageDTO message = messages.get(i);
            if (!"user".equals(message.getNormalisedRole())) {
                continue;
            }
            String content = Optional.ofNullable(message.getContent()).orElse("").toLowerCase(Locale.ROOT);
            
            // Check for greetings first
            if (content.matches("^(hi|hello|hey|good morning|good evening|good afternoon)[.!?]*$")) {
                return InteractionIntent.GREETING_ONLY;
            }
            
            // Check for navigation (doctor registration)
            if (content.contains("become a doctor")
                    || content.contains("apply as a doctor")
                    || content.contains("register as a doctor")
                    || content.contains("doctor application")
                    || content.contains("join as a doctor")) {
                return InteractionIntent.NAVIGATION_ONLY;
            }
            
            // IMPROVED: Check for doctor finding - be more flexible
            // Check for any mention of needing/finding/looking for a doctor or specific specialty
            if (content.contains("need") && (content.contains("doctor") || content.contains("psychiatry") || content.contains("psychiatrist") 
                    || content.contains("cardiologist") || content.contains("neurologist") || content.contains("specialist")
                    || content.contains("physician") || content.contains("surgeon"))) {
                return InteractionIntent.FIND_DOCTOR;
            }
            if (content.contains("find") && (content.contains("doctor") || content.contains("psychiatry") || content.contains("psychiatrist")
                    || content.contains("cardiologist") || content.contains("neurologist") || content.contains("specialist")
                    || content.contains("physician") || content.contains("surgeon"))) {
                return InteractionIntent.FIND_DOCTOR;
            }
            if (content.contains("recommend") && (content.contains("doctor") || content.contains("psychiatry") || content.contains("psychiatrist")
                    || content.contains("cardiologist") || content.contains("neurologist") || content.contains("specialist")
                    || content.contains("physician") || content.contains("surgeon"))) {
                return InteractionIntent.FIND_DOCTOR;
            }
            if (content.contains("looking for") && (content.contains("doctor") || content.contains("psychiatry") || content.contains("psychiatrist")
                    || content.contains("cardiologist") || content.contains("neurologist") || content.contains("specialist")
                    || content.contains("physician") || content.contains("surgeon"))) {
                return InteractionIntent.FIND_DOCTOR;
            }
            // Also check for specialty names alone (e.g., "psychiatry doctor", "cardiologist")
            if (content.contains("psychiatry") || content.contains("psychiatrist") 
                    || content.contains("cardiologist") || content.contains("neurologist")
                    || content.contains("dermatologist") || content.contains("pediatrician")) {
                return InteractionIntent.FIND_DOCTOR;
            }
        }
        return InteractionIntent.GENERAL;
    }

    private enum InteractionIntent {
        GENERAL,
        FIND_DOCTOR,
        NAVIGATION_ONLY,
        GREETING_ONLY
    }

    private String detectSpecialisationFromMessage(String message) {
        if (isBlank(message)) {
            return null;
        }
        String normalised = message.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\s]", " ");

        // Direct keyword lookup.
        for (Map.Entry<String, String> entry : SPECIALISATION_SYNONYMS.entrySet()) {
            if (normalised.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Fuzzy matching for individual words.
        String[] tokens = normalised.split("\\s+");
        for (String token : tokens) {
            if (token.length() < 4) {
                continue;
            }
            for (String candidate : SPECIALISATION_SYNONYMS.keySet()) {
                if (levenshteinDistance(token, candidate) <= 2) {
                    return SPECIALISATION_SYNONYMS.get(candidate);
                }
            }
        }
        return null;
    }
}

