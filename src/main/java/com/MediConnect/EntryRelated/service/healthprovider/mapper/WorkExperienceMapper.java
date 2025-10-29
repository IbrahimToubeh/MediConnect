package com.MediConnect.EntryRelated.service.healthprovider.mapper;

import com.MediConnect.EntryRelated.dto.healthprovider.WorkExperienceDTO;
import com.MediConnect.EntryRelated.entities.WorkExperience;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WorkExperienceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", ignore = true)
    WorkExperience dtoToEntity(WorkExperienceDTO dto);

    List<WorkExperience> dtosToEntities(List<WorkExperienceDTO> dtos);

    WorkExperienceDTO entityToDto(WorkExperience entity);

    List<WorkExperienceDTO> entitiesToDtos(List<WorkExperience> entities);
}
