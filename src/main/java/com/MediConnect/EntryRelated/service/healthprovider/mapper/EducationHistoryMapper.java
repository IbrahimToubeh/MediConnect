package com.MediConnect.EntryRelated.service.healthprovider.mapper;

import com.MediConnect.EntryRelated.dto.healthprovider.EducationHistoryDTO;
import com.MediConnect.EntryRelated.entities.EducationHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EducationHistoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", ignore = true)
    EducationHistory dtoToEntity(EducationHistoryDTO dto);

    List<EducationHistory> dtosToEntities(List<EducationHistoryDTO> dtos);

    EducationHistoryDTO entityToDto(EducationHistory entity);

    List<EducationHistoryDTO> entitiesToDtos(List<EducationHistory> entities);
}
