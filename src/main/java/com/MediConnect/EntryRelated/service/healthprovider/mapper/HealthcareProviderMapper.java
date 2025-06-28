package com.MediConnect.EntryRelated.service.healthprovider.mapper;

import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.EducationHistoryDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SpecialtyDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.WorkExperienceDTO;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HealthcareProviderMapper {

    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "medicalRecords", ignore = true)
    HealthcareProvider signupDtoToProvider(SignupHPRequestDTO dto);

    List<EducationHistory> mapEducationHistories(List<EducationHistoryDTO> dtos);

    List<WorkExperience> mapWorkExperiences(List<WorkExperienceDTO> dtos);

    EducationHistory mapEducationHistory(EducationHistoryDTO dto);

    WorkExperience mapWorkExperience(WorkExperienceDTO dto);




}
