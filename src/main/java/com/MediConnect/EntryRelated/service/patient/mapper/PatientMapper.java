package com.MediConnect.EntryRelated.service.patient.mapper;

import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.entities.Patient;
import com.MediConnect.EntryRelated.entities.MentalHealthMedication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

import java.util.Date;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {MedicationMapper.class}
)
public interface PatientMapper {

    @Mapping(target = "role", constant = "PATIENT")
    @Mapping(target = "registrationDate", source = ".", qualifiedByName = "getCurrentDate")
    @Mapping(target = "id", ignore = true) // Let JPA handle ID generation
    @Mapping(target = "emergencyContactName", source = "emergencyContactName")
    @Mapping(target = "emergencyContactPhone", source = "emergencyContactPhone")
    @Mapping(target = "emergencyContactRelation", source = "emergencyContactRelation")
    @Mapping(target = "insuranceProvider", source = "insuranceProvider")
    @Mapping(target = "insuranceNumber", source = "insuranceNumber")
    @Mapping(target = "medications", source = "medications")
    Patient signupDtoToPatient(SignupPatientRequestDTO dto);

    @Named("getCurrentDate")
    default Date getCurrentDate(SignupPatientRequestDTO dto) {
        return new Date();
    }

    @AfterMapping
    default void setPatientInMedications(@MappingTarget Patient patient) {
        if (patient.getMedications() != null) {
            for (MentalHealthMedication medication : patient.getMedications()) {
                medication.setPatient(patient);
            }
        }
    }
}