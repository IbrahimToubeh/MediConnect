package com.MediConnect.EntryRelated.service.patient.mapper;

import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.entities.MentalHealthMedication;
import com.MediConnect.EntryRelated.entities.Patient;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {MedicationMapper.class}
)
public interface PatientMapper {

    @Mapping(target = "role", constant = "PATIENT")
    @Mapping(target = "id", ignore = true) // Let JPA handle ID generation
    @Mapping(target = "registrationDate", expression = "java(new java.util.Date())")
    @Mapping(target = "medications", source = "medications")
    Patient signupDtoToPatient(SignupPatientRequestDTO dto);

    @AfterMapping
    default void setPatientInMedications(@MappingTarget Patient patient) {
        if (patient.getMentalHealthMedications() != null) {
            for (MentalHealthMedication medication : patient.getMentalHealthMedications()) {
                medication.setPatient(patient);
            }
        }
    }
}