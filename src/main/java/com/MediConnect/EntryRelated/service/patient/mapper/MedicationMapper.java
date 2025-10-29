package com.MediConnect.EntryRelated.service.patient.mapper;

import com.MediConnect.EntryRelated.dto.patient.MentalHealthMedicationDTO;
import com.MediConnect.EntryRelated.entities.MentalHealthMedication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MedicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    MentalHealthMedication dtoToEntity(MentalHealthMedicationDTO dto);

    List<MentalHealthMedication> dtosToEntities(List<MentalHealthMedicationDTO> dtos);
}