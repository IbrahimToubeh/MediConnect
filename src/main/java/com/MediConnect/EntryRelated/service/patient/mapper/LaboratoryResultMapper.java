package com.MediConnect.EntryRelated.service.patient.mapper;


import com.MediConnect.EntryRelated.dto.patient.LabResultResponseDTO;
import com.MediConnect.EntryRelated.entities.LaboratoryResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface LaboratoryResultMapper {

    @Mapping(target = "hasImage", source = "image", qualifiedByName = "calculateHasImage")
    @Mapping(target = "imageSize", source = "image", qualifiedByName = "calculateImageSize")
    LabResultResponseDTO toDTO(LaboratoryResult labResult);

    @Named("calculateHasImage")
    default boolean calculateHasImage(byte[] image) {
        return image != null && image.length > 0;
    }

    @Named("calculateImageSize")
    default int calculateImageSize(byte[] image) {
        return image != null ? image.length : 0;
    }
}
