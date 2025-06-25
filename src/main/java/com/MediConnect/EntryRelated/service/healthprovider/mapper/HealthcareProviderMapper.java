package com.MediConnect.EntryRelated.service.healthprovider.mapper;

import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

import java.util.Date;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {EducationHistoryMapper.class, WorkExperienceMapper.class}
)
public interface HealthcareProviderMapper {

    @Mapping(target = "role", constant = "HEALTHCARE_PROVIDER")
    @Mapping(target = "registrationDate", source = ".", qualifiedByName = "getCurrentDate")
    @Mapping(target = "id", ignore = true) // Let JPA handle ID generation
    @Mapping(target = "specializations", ignore = true) // Handle separately if needed
    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "medicalRecords", ignore = true)
    @Mapping(target = "educationHistories", source = "educationHistories")
    @Mapping(target = "workExperiences", source = "workExperiences")
    @Mapping(target = "availableDays", source = "availableDays")
    @Mapping(target = "availableTimeStart", source = "availableTimeStart")
    @Mapping(target = "availableTimeEnd", source = "availableTimeEnd")
    @Mapping(target = "educationStartDate", source = "educationStartDate")
    @Mapping(target = "educationEndDate", source = "educationEndDate")
    @Mapping(target = "university", source = "university")
    @Mapping(target = "degree", source = "degree")
    @Mapping(target = "licenseNumber", source = "licenseNumber")
    @Mapping(target = "hospitalName", source = "hospitalName")
    @Mapping(target = "clinicName", source = "clinicName")
    @Mapping(target = "department", source = "department")
    @Mapping(target = "bio", source = "bio")
    @Mapping(target = "yearsOfExperience", source = "yearsOfExperience")
    @Mapping(target = "consultationFee", source = "consultationFee")
    HealthcareProvider signupDtoToProvider(SignupHPRequestDTO dto);

    @Named("getCurrentDate")
    default Date getCurrentDate(SignupHPRequestDTO dto) {
        return new Date();
    }

    // Set bidirectional relationships after mapping
    @AfterMapping
    default void setProviderInRelatedEntities(@MappingTarget HealthcareProvider provider) {
        // Set provider reference in education histories
        if (provider.getEducationHistories() != null) {
            for (EducationHistory educationHistory : provider.getEducationHistories()) {
                educationHistory.setProvider(provider);
            }
        }

        // Set provider reference in work experiences
        if (provider.getWorkExperiences() != null) {
            for (WorkExperience workExperience : provider.getWorkExperiences()) {
                workExperience.setProvider(provider);
            }
        }
    }
}