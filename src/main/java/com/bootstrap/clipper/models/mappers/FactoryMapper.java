package com.bootstrap.clipper.models.mappers;

import com.bootstrap.clipper.models.dao.Factory;
import com.bootstrap.clipper.models.dto.FactoryRequest;
import com.bootstrap.clipper.models.dto.FactoryResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FactoryMapper {

    Factory toEntity(FactoryRequest request);

    @AfterMapping
    default void fillAddress(FactoryRequest request, @MappingTarget Factory factory) {
        factory.describeAs(request.address());
    }

    FactoryResponse toResponse(Factory factory);

    List<FactoryResponse> toResponseList(List<Factory> factories);
}
