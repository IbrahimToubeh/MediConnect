package com.MediConnect.EntryRelated.service.patient.mapper;

import com.MediConnect.EntryRelated.dto.patient.CurrentMedicationDTO;
import com.MediConnect.EntryRelated.entities.Medication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CurrentMedicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    Medication dtoToEntity(CurrentMedicationDTO dto);

    List<Medication> dtosToEntities(List<CurrentMedicationDTO> dtos);
}
