package com.MediConnect.EntryRelated.service.healthprovider.mapper;

import com.MediConnect.EntryRelated.dto.healthprovider.EducationHistoryDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.SignupHPRequestDTO;
import com.MediConnect.EntryRelated.dto.healthprovider.WorkExperienceDTO;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Mapper(componentModel = "spring")
public interface HealthcareProviderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "medicalRecords", ignore = true)
    @Mapping(target = "registrationDate", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "followList", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "zipcode", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    HealthcareProvider signupDtoToProvider(SignupHPRequestDTO dto);

    List<EducationHistory> mapEducationHistories(List<EducationHistoryDTO> dtos);

    List<WorkExperience> mapWorkExperiences(List<WorkExperienceDTO> dtos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(source = "startDate", target = "startDate", qualifiedByName = "stringToDate")
    @Mapping(source = "endDate", target = "endDate", qualifiedByName = "stringToDate")
    EducationHistory mapEducationHistory(EducationHistoryDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(source = "startDate", target = "startDate", qualifiedByName = "stringToDate")
    @Mapping(source = "endDate", target = "endDate", qualifiedByName = "stringToDate")
    WorkExperience mapWorkExperience(WorkExperienceDTO dto);

    @Named("stringToDate")
    default Date stringToDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            String cleanDate = dateString.trim();
            // If ISO format (contains T), extract just the date part
            if (cleanDate.contains("T")) {
                cleanDate = cleanDate.substring(0, cleanDate.indexOf("T")).trim();
            }
            // Parse the date in simple yyyy-MM-dd format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            return sdf.parse(cleanDate);
        } catch (ParseException e) {
            System.out.println("Error parsing date: '" + dateString + "'");
            e.printStackTrace();
            return null;
        }
    }

    default String dateToString(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

}
