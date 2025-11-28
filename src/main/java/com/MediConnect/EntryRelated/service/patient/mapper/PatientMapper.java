package com.MediConnect.EntryRelated.service.patient.mapper;

import com.MediConnect.EntryRelated.dto.patient.PatientProfileResponseDTO;
import com.MediConnect.EntryRelated.dto.patient.SignupPatientRequestDTO;
import com.MediConnect.EntryRelated.entities.Medication;
import com.MediConnect.EntryRelated.entities.MentalHealthMedication;
import com.MediConnect.EntryRelated.entities.Patient;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {MedicationMapper.class, CurrentMedicationMapper.class, LaboratoryResultMapper.class}
)
public interface PatientMapper {

    // 1️⃣ Map from signup DTO → Patient entity
    @Mapping(target = "role", constant = "PATIENT")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "registrationDate", expression = "java(new java.util.Date())")
    @Mapping(target = "medications", source = "medications")
    @Mapping(target = "mentalHealthMedications", source = "mentalHealthMedications")
    @Mapping(target = "laboratoryResults", source = "laboratoryResults")
    Patient signupDtoToPatient(SignupPatientRequestDTO dto);

    // 2️⃣ Map from Patient entity → Profile DTO
    @Mapping(target = "fullName", expression = "java(patient.getFirstName() + ' ' + patient.getLastName())")
    @Mapping(target = "labResults", source = "laboratoryResults") // Map lab results using LaboratoryResultMapper
    PatientProfileResponseDTO patientToProfileDto(Patient patient);

    // 3️⃣ AfterMapping hook to set bidirectional relationships
    @AfterMapping
    default void setPatientInCollections(@MappingTarget Patient patient) {
        if (patient.getMedications() != null) {
            for (Medication medication : patient.getMedications()) {
                medication.setPatient(patient);
            }
        }

        if (patient.getMentalHealthMedications() != null) {
            for (MentalHealthMedication medication : patient.getMentalHealthMedications()) {
                medication.setPatient(patient);
            }
        }

        if (patient.getLaboratoryResults() != null) {
            for (com.MediConnect.EntryRelated.entities.LaboratoryResult result : patient.getLaboratoryResults()) {
                result.setPatient(patient);
            }
        }
    }
}
