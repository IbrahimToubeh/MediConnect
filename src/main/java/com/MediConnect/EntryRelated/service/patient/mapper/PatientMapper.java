package com.MediConnect.EntryRelated.service.patient.mapper;

import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.entities.Medication;
import com.MediConnect.EntryRelated.entities.MentalHealthMedication;
import com.MediConnect.EntryRelated.entities.Patient;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {MedicationMapper.class, CurrentMedicationMapper.class}
)
public interface PatientMapper {

    @Mapping(target = "role", constant = "PATIENT")
    @Mapping(target = "id", ignore = true) // Let JPA handle ID generation
    @Mapping(target = "registrationDate", expression = "java(new java.util.Date())")
    @Mapping(target = "medications", source = "medications")
    @Mapping(target = "mentalHealthMedications", source = "mentalHealthMedications")
    Patient signupDtoToPatient(SignupPatientRequestDTO dto);

    @AfterMapping
    default void setPatientInMedications(@MappingTarget Patient patient) {
        // Set patient reference in regular medications
        if (patient.getMedications() != null) {
            for (Medication medication : patient.getMedications()) {
                medication.setPatient(patient);
            }
        }
        
        // Set patient reference in mental health medications
        if (patient.getMentalHealthMedications() != null) {
            for (MentalHealthMedication medication : patient.getMentalHealthMedications()) {
                medication.setPatient(patient);
            }
        }
    }
}