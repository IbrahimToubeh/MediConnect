# AI Chatbot Implementation Documentation

## Overview

The MediConnect AI Chatbot is an intelligent patient assistant that helps users find doctors, navigate the platform, and get medical guidance. It uses OpenAI's Chat Completions API (GPT-4o-mini) to provide natural language interactions while leveraging backend logic for doctor matching, fuzzy search, and context management.

**Key Features:**
- Natural language conversation with patients
- Intelligent doctor recommendations based on specialization, insurance, and location
- Fuzzy matching to handle typos and variations (e.g., "nurology" → "NEUROLOGY")
- Context-aware conversations that remember patient preferences
- Intent detection to distinguish between doctor searches, navigation help, and greetings
- Progressive information gathering through follow-up questions
- Navigation tips to guide users through MediConnect features

---

## Architecture

### High-Level Flow

```
Frontend (PatientAssistantChat.tsx)
    ↓ POST /ai/chat
Backend Controller (PatientAssistantController)
    ↓
Service Layer (PatientRecommendationChatService)
    ↓
1. Load all active doctors from database
2. Detect user intent (find doctor, navigation, greeting)
3. Enrich context from user messages (extract insurance, location, specialization)
4. Build and rank doctor catalogue
5. Prepare OpenAI API request with system prompts and doctor catalogue
    ↓
OpenAI Chat Completions API
    ↓
Parse JSON response and merge with backend recommendations
    ↓
Return ChatResponseDTO to frontend
```

### Component Structure

```
MediConnect/src/main/java/com/MediConnect/ai/
├── config/
│   └── AiConfiguration.java          # RestTemplate configuration for OpenAI API
├── dto/
│   ├── ChatRequestDTO.java           # Request payload from frontend
│   ├── ChatResponseDTO.java          # Response payload to frontend
│   ├── ChatMessageDTO.java           # Individual chat message
│   ├── PatientContextDTO.java        # Structured patient information
│   └── DoctorSuggestionDTO.java      # Lightweight doctor profile for chatbot
└── service/
    └── PatientRecommendationChatService.java  # Core chatbot logic

MediConnect/src/main/java/com/MediConnect/EntryRelated/
└── controller/
    └── PatientAssistantController.java        # REST endpoint

meddiconnect/src/components/
└── PatientAssistantChat.tsx                   # Frontend React component
```

---

## Key Components

### 1. PatientRecommendationChatService

**Location:** `MediConnect/src/main/java/com/MediConnect/ai/service/PatientRecommendationChatService.java`

This is the core service that orchestrates the entire chatbot flow.

#### Main Method: `chat(ChatRequestDTO request)`

**Flow:**
1. **Initialize Context**: Creates or uses existing `PatientContextDTO` from request
2. **Load Doctors**: Fetches all active, non-flagged healthcare providers from database
3. **Detect Intent**: Determines if user wants to find a doctor, navigate, or just greeting
4. **Enrich Context**: Extracts insurance, location, and specialization from user messages
5. **Build Catalogue**: Creates ranked list of doctor suggestions based on context
6. **Call OpenAI**: Sends conversation history, context, and doctor catalogue to OpenAI
7. **Parse Response**: Extracts JSON response from OpenAI and merges with backend recommendations
8. **Return Response**: Returns `ChatResponseDTO` with reply, doctors, follow-ups, and tips

#### Key Constants

```java
private static final int MAX_HISTORY_LENGTH = 18;                    // Max conversation messages sent to OpenAI
private static final int MAX_DOCTORS_SHARED_WITH_MODEL = 12;        // Max doctors in OpenAI prompt
private static final int MAX_DOCTORS_RETURNED_TO_PATIENT = 4;       // Max doctors shown to user
private static final double BASE_MATCH_SCORE = 1.0d;                // Base score for all doctors
```

#### Key Private Methods

**Intent Detection:**
- `detectIntent(List<ChatMessageDTO> messages)`: Analyzes user messages to determine intent
  - `FIND_DOCTOR`: User wants to find a doctor
  - `NAVIGATION_ONLY`: User asking about platform features
  - `GREETING_ONLY`: Simple greetings
  - `GENERAL`: Default fallback

**Context Enrichment:**
- `enrichContextFromMessages(...)`: Extracts structured data from natural language
  - Insurance provider detection
  - City/state/country extraction (e.g., "in Amman")
  - Specialization detection from symptoms/keywords

**Fuzzy Matching:**
- `detectSpecialisationFromMessage(String message)`: Handles typos and variations
  - Uses Levenshtein distance algorithm (max 2 character differences)
  - Maps synonyms (e.g., "neurologist" → "NEUROLOGY")
  - Example: "nurology" → "NEUROLOGY", "cardio" → "CARDIOLOGY"

**Insurance Normalization:**
- `normaliseInsuranceName(String value)`: Standardizes insurance names
  - Removes punctuation and parentheses
  - Lowercases and tokenizes
  - Example: "Blue Cross (BCBS)" → "blue cross bcbs"

**Doctor Catalogue Building:**
- `loadDoctorCatalogue(...)`: Creates ranked list of doctors
  - Filters: Active status, not admin-flagged
  - Scores based on: Location (+3.5 city, +1.5 state, +1.0 country), Insurance (+3.0), Specialization (+4.0)
  - Returns top 12 doctors sorted by match score

**Ranking & Filtering:**
- `buildRankedRecommendations(...)`: Applies soft filters
  - If no exact matches, relaxes filters gracefully
  - Prioritizes specialization > insurance > location
  - Returns filtered catalogue for OpenAI

**OpenAI Integration:**
- `buildMessagesPayload(...)`: Constructs OpenAI API request
  - System instruction with JSON schema
  - Doctor catalogue as JSON string
  - Truncated conversation history (last 18 messages)

**Response Parsing:**
- `parseResponse(...)`: Extracts structured data from OpenAI JSON response
  - Parses `reply`, `context`, `recommendedDoctorIds`, `followUpQuestions`, `navigationTips`
  - Maps doctor IDs to full `DoctorSuggestionDTO` objects
  - Merges context updates with previous context

### 2. DTOs (Data Transfer Objects)

#### ChatRequestDTO
**Location:** `MediConnect/src/main/java/com/MediConnect/ai/dto/ChatRequestDTO.java`

```java
{
  "messages": [ChatMessageDTO],      // Conversation history
  "context": PatientContextDTO       // Current patient context (optional)
}
```

#### ChatResponseDTO
**Location:** `MediConnect/src/main/java/com/MediConnect/ai/dto/ChatResponseDTO.java`

```java
{
  "reply": "string",                          // Natural language response
  "context": PatientContextDTO,               // Updated context
  "recommendedDoctors": [DoctorSuggestionDTO], // Top matching doctors
  "followUpQuestions": ["string"],            // Quick reply suggestions
  "navigationTips": ["string"],               // Platform usage tips
  "informationComplete": boolean,             // Whether enough info collected
  "rawModelContent": "string"                 // Debug: raw OpenAI response
}
```

#### PatientContextDTO
**Location:** `MediConnect/src/main/java/com/MediConnect/ai/dto/PatientContextDTO.java`

Structured information collected during conversation:
- `ageRange`, `primaryConcern`, `symptomDuration`, `symptomSeverity`
- `medicalHistory`, `medications`, `allergies`
- `preferredDoctorGender`, `preferredLanguage`
- `insuranceProvider`, `city`, `state`, `country`, `postalCode`
- `preferredSpecialization`, `appointmentPreference`

#### DoctorSuggestionDTO
**Location:** `MediConnect/src/main/java/com/MediConnect/ai/dto/DoctorSuggestionDTO.java`

Lightweight doctor profile for chatbot display:
- `id`, `fullName`, `clinicName`
- `city`, `state`, `country`
- `consultationFee`, `shortBio`, `profilePicture`
- `specializations: List<String>` (e.g., ["NEUROLOGY", "CARDIOLOGY"])
- `insuranceAccepted: List<String>` (e.g., ["Blue Cross", "Aetna"])
- `matchScore: Double` (ranking score)

### 3. Controller

**Location:** `MediConnect/src/main/java/com/MediConnect/EntryRelated/controller/PatientAssistantController.java`

```java
@RestController
@RequestMapping("/ai")
public class PatientAssistantController {
    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request)
}
```

**Endpoint:** `POST /ai/chat`

**Request Body:**
```json
{
  "messages": [
    {"role": "user", "content": "I need a neurologist in Amman"},
    {"role": "assistant", "content": "I can help you find a neurologist..."}
  ],
  "context": {
    "preferredSpecialization": "NEUROLOGY",
    "city": "Amman"
  }
}
```

**Response:**
```json
{
  "reply": "I found 3 neurologists in Amman...",
  "context": {...},
  "recommendedDoctors": [...],
  "followUpQuestions": ["Do you have insurance?", "What's your primary concern?"],
  "navigationTips": ["You can browse all doctors using the Find Doctors page..."],
  "informationComplete": false
}
```

### 4. Configuration

**Location:** `MediConnect/src/main/java/com/MediConnect/ai/config/AiConfiguration.java`

Configures `RestTemplate` for OpenAI API calls:
- Connection timeout: 10 seconds
- Read timeout: 45 seconds
- Named bean: `openAiRestTemplate`

**Application Properties:**
**Location:** `MediConnect/src/main/resources/application.properties`

```properties
openai.api-key=sk-proj-...
openai.model=gpt-4o-mini
```

---

## Key Features Explained

### 1. Fuzzy Matching for Specializations

**Problem:** Users may type "nurology" instead of "neurology", or "cardio" instead of "cardiology".

**Solution:** 
- **Levenshtein Distance Algorithm**: Calculates character differences between user input and known specializations
- **Synonym Mapping**: Maps common terms to enum values (e.g., "neurologist" → "NEUROLOGY")
- **Keyword Matching**: Matches symptoms to specializations (e.g., "headache" → "NEUROLOGY")

**Code Location:** `detectSpecialisationFromMessage()` and `buildSpecialisationSynonymMap()`

**Example:**
```java
// User types: "nurology doctor"
// Levenshtein distance: "nurology" vs "neurology" = 1 character difference
// Result: Matches "NEUROLOGY" specialization
```

### 2. Insurance Normalization

**Problem:** Insurance names may have variations: "Blue Cross", "Blue Cross (BCBS)", "blue cross", etc.

**Solution:**
- Removes punctuation and parentheses
- Lowercases and tokenizes into words
- Matches using token containment (all tokens must match)

**Code Location:** `normaliseInsuranceName()` and `findInsuranceReference()`

**Example:**
```java
// User: "I have blue cross"
// Database: "Blue Cross (BCBS)"
// Normalized user: "blue cross"
// Normalized DB: "blue cross bcbs"
// Match: true (tokens "blue" and "cross" are contained)
```

### 3. Intent Detection

**Problem:** Chatbot should not show doctor cards for navigation questions like "how do I become a doctor?"

**Solution:**
- Analyzes last user message for keywords
- Resets context for navigation/greeting intents
- Only shows doctors for `FIND_DOCTOR` intent

**Code Location:** `detectIntent()` and `resetContextForNavigation()`

**Intents:**
- `FIND_DOCTOR`: "find a doctor", "need a doctor", "recommend a doctor"
- `NAVIGATION_ONLY`: "become a doctor", "register as a doctor", "apply as a doctor"
- `GREETING_ONLY`: "hi", "hello", "hey"
- `GENERAL`: Default fallback

### 4. Soft Filtering (Constraint-Aware)

**Problem:** Strict filtering may return zero results even when partial matches exist.

**Solution:**
- Applies filters in priority order: specialization → insurance → location
- If no exact matches, relaxes filters gracefully
- Shows best available matches with helpful messages

**Code Location:** `buildRankedRecommendations()`

**Example:**
```java
// User wants: Neurology + Blue Cross + Amman
// If no exact match:
// 1. Try: Neurology + Blue Cross (any location)
// 2. Try: Neurology (any insurance, any location)
// 3. Show top neurologists with helpful message
```

### 5. Context Enrichment

**Problem:** Users may mention insurance/location in natural language without structured format.

**Solution:**
- Extracts insurance from message by matching against database values
- Extracts location using patterns like "in <city>"
- Detects specialization from symptoms/keywords

**Code Location:** `enrichContextFromMessages()`

**Example:**
```java
// User: "I need a neurologist in Amman with Blue Cross"
// Extracted:
//   - preferredSpecialization: "NEUROLOGY"
//   - city: "Amman"
//   - insuranceProvider: "Blue Cross"
```

### 6. Progressive Information Gathering

**Problem:** Users may not provide all information upfront.

**Solution:**
- Maintains `PatientContextDTO` across conversation
- Generates follow-up questions for missing information
- Sets `informationComplete: false` until enough data collected

**Code Location:** `generateFollowUpPrompts()`

**Example:**
```java
// Missing insurance → "Do you have a preferred insurance provider?"
// Missing location → "Which city or country would you like to find a doctor in?"
// Missing specialization → "Do you know which specialization you prefer?"
```

### 7. System Prompt Engineering

**Location:** `buildSystemInstruction()`

The system prompt instructs OpenAI to:
- Act as MediConnect's AI Care Navigator
- Respond in strict JSON format
- Only recommend doctors from the provided catalogue
- Ask minimum questions needed
- Provide navigation tips for platform features
- Set `informationComplete` flag appropriately

**JSON Schema Sent to OpenAI:**
```json
{
  "reply": "string",
  "context": {...},
  "recommendedDoctorIds": [1, 2, 3],
  "followUpQuestions": ["string"],
  "informationComplete": boolean,
  "navigationTips": ["string"]
}
```

---

## Frontend Integration

**Location:** `meddiconnect/src/components/PatientAssistantChat.tsx`

### Key Features:
- Floating chat button (FAB) that opens/closes chat window
- Message history with user/assistant messages
- Doctor cards displayed inline with chat
- Follow-up question buttons for quick replies
- Navigation tips displayed as bullet points
- Auto-scroll to latest message
- Loading states and error handling

### API Call:
```typescript
const response = await fetch(`${baseApiUrl}/ai/chat`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': authHeader,  // Optional: Bearer token
  },
  body: JSON.stringify({
    messages: chatHistory.map(msg => ({
      role: msg.role,
      content: msg.content,
    })),
    context: patientContext,
  }),
});
```

### State Management:
- `messages`: Array of chat messages
- `patientContext`: Current patient context (echoed back to backend)
- `isSubmitting`: Loading state
- `error`: Error message state

---

## Database Dependencies

The chatbot relies on the following database tables:

### HealthcareProvider Entity
**Location:** `MediConnect/src/main/java/com/MediConnect/EntryRelated/entities/HealthcareProvider.java`

**Required Fields:**
- `id`, `firstName`, `lastName`
- `accountStatus` (must be `ACTIVE`)
- `adminFlagged` (must be `false`)
- `specializations: Set<SpecializationType>` (enum)
- `insuranceAccepted: List<String>`
- `city`, `state`, `country`
- `consultationFee`, `bio`, `profilePicture`
- `clinicName`

**Repository:**
**Location:** `MediConnect/src/main/java/com/MediConnect/EntryRelated/repository/HealthcareProviderRepo.java`

```java
List<HealthcareProvider> findAll();
```

---

## Error Handling

### OpenAI API Failures
- Returns fallback response with top 3 doctors from catalogue
- Logs error for debugging
- User sees: "I'm sorry, I'm having trouble connecting right now..."

### Invalid JSON Response
- If OpenAI returns non-JSON, uses raw text as reply
- Still shows doctor recommendations from catalogue
- Logs warning

### Empty Doctor Catalogue
- Returns message: "No active doctors available."
- Sets `informationComplete: false`
- Provides navigation tips

### Database Errors
- Logged and handled gracefully
- Returns fallback response

---

## Performance Considerations

### Optimizations:
1. **Message History Truncation**: Only sends last 18 messages to OpenAI (reduces token usage)
2. **Doctor Catalogue Limiting**: Only sends top 12 doctors to OpenAI (reduces prompt size)
3. **Batch Loading**: Loads all doctors once per request (no N+1 queries)
4. **In-Memory Filtering**: Filters and ranks doctors in memory (fast)

### Potential Improvements:
- Cache doctor catalogue (refresh every 5-10 minutes)
- Use database-level filtering for large doctor sets
- Implement conversation session management (Redis)

---

## Testing Recommendations

### Unit Tests:
1. **Fuzzy Matching**: Test "nurology" → "NEUROLOGY", "cardio" → "CARDIOLOGY"
2. **Insurance Normalization**: Test "Blue Cross (BCBS)" matching "blue cross"
3. **Intent Detection**: Test "find a doctor" vs "become a doctor"
4. **Context Enrichment**: Test extraction of city, insurance, specialization
5. **Ranking Logic**: Test match scores for location, insurance, specialization

### Integration Tests:
1. **End-to-End Chat Flow**: Send messages, verify responses
2. **OpenAI Mock**: Mock OpenAI API responses for consistent testing
3. **Database Integration**: Test with real healthcare provider data

### Manual Testing Scenarios:
1. **Typo Handling**: "nurology", "nurologist", "brain doctor"
2. **Insurance Variations**: "Blue Cross", "blue cross", "BCBS"
3. **Location Extraction**: "in Amman", "Amman, Jordan"
4. **Progressive Conversation**: Start with greeting, gradually provide info
5. **Navigation Questions**: "how do I become a doctor?" (should not show doctors)
6. **Empty Results**: Request non-existent specialization/location combination

---

## Configuration & Deployment

### Required Environment Variables:
```properties
openai.api-key=sk-proj-...          # OpenAI API key
openai.model=gpt-4o-mini            # Model name (default: gpt-4o-mini)
```

### Dependencies (pom.xml):
- Spring Boot Web
- Spring Boot Validation
- Jackson (JSON processing)
- Lombok
- SLF4J (logging)

### API Rate Limits:
- OpenAI API has rate limits based on tier
- Consider implementing rate limiting for `/ai/chat` endpoint
- Monitor token usage (each request includes doctor catalogue JSON)

---

## Troubleshooting

### Issue: Chatbot not finding doctors
**Check:**
1. Are doctors in database with `accountStatus = ACTIVE`?
2. Are doctors not `adminFlagged = true`?
3. Check logs for OpenAI API errors
4. Verify specialization names match enum values

### Issue: Typos not being handled
**Check:**
1. Verify `SPECIALISATION_SYNONYMS` map includes common variations
2. Check Levenshtein distance threshold (currently 2)
3. Test with actual typos: "nurology", "cardio", etc.

### Issue: Insurance not matching
**Check:**
1. Verify insurance names in database (check for extra spaces, punctuation)
2. Test normalization: `normaliseInsuranceName()` output
3. Check token matching logic in `findInsuranceReference()`

### Issue: OpenAI API errors
**Check:**
1. Verify API key in `application.properties`
2. Check network connectivity
3. Verify OpenAI account has credits
4. Check request/response logs

---

## Future Enhancements

### Potential Improvements:
1. **Conversation Sessions**: Store conversation history in Redis/database
2. **Multi-language Support**: Detect and respond in Arabic/English
3. **Voice Input**: Integrate speech-to-text
4. **Appointment Booking**: Allow booking directly from chatbot
5. **Medical Records Integration**: Suggest doctors based on medical history
6. **Sentiment Analysis**: Detect patient urgency/emotion
7. **A/B Testing**: Test different system prompts for better responses
8. **Analytics**: Track common queries, successful matches, user satisfaction

---

## File Locations Summary

### Backend Files:
```
MediConnect/src/main/java/com/MediConnect/
├── ai/
│   ├── config/
│   │   └── AiConfiguration.java
│   ├── dto/
│   │   ├── ChatRequestDTO.java
│   │   ├── ChatResponseDTO.java
│   │   ├── ChatMessageDTO.java
│   │   ├── PatientContextDTO.java
│   │   └── DoctorSuggestionDTO.java
│   └── service/
│       └── PatientRecommendationChatService.java
└── EntryRelated/
    ├── controller/
    │   └── PatientAssistantController.java
    ├── entities/
    │   └── HealthcareProvider.java
    └── repository/
        └── HealthcareProviderRepo.java
```

### Frontend Files:
```
meddiconnect/src/components/
└── PatientAssistantChat.tsx
```

### Configuration:
```
MediConnect/src/main/resources/
└── application.properties (openai.api-key, openai.model)
```

---

## Contact & Support

For questions or issues related to the AI chatbot implementation, refer to:
- This documentation
- Code comments in `PatientRecommendationChatService.java`
- OpenAI API documentation: https://platform.openai.com/docs/api-reference/chat

---

**Last Updated:** 2025-01-14
**Version:** 1.0

