package com.bootstrap.clipper.models.mappers;

import com.bootstrap.clipper.models.dao.Store;
import com.bootstrap.clipper.models.dto.StoreRequest;
import com.bootstrap.clipper.models.dto.StoreResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    Store toEntity(StoreRequest request);

    @AfterMapping
    default void fillAddress(StoreRequest request, @MappingTarget Store store) {
        store.describeAs(request.address());
    }

    StoreResponse toResponse(Store store);

    List<StoreResponse> toResponseList(List<Store> stores);
}
