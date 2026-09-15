package com.bootstrap.clipper.controllers;

import com.bootstrap.clipper.clients.GeocodingClient;
import com.bootstrap.clipper.models.dao.Factory;
import com.bootstrap.clipper.models.dto.FactoryRequest;
import com.bootstrap.clipper.models.dto.FactoryResponse;
import com.bootstrap.clipper.models.mappers.FactoryMapper;
import com.bootstrap.clipper.services.factory.FactoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService service;
    private final FactoryMapper mapper;
    private final GeocodingClient geocodingClient;

    @PostMapping
    public ResponseEntity<FactoryResponse> saveFactory(@Valid @RequestBody FactoryRequest request) {
        Factory saved = service.saveFactory(mapper.toEntity(request));
        saved.describeAs(request.address()); // transient : on connaît déjà l'adresse
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<FactoryResponse>> getAllFactories() {
        List<Factory> factories = service.getAllFactory();
        factories.forEach(this::resolveAddress);
        return ResponseEntity.ok(mapper.toResponseList(factories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactoryResponse> getFactory(@PathVariable Long id) {
        Factory factory = service.getFactory(id);
        resolveAddress(factory);
        return ResponseEntity.ok(mapper.toResponse(factory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FactoryResponse> updateFactory(@PathVariable Long id,
                                                         @Valid @RequestBody FactoryRequest request) {
        Factory updated = service.updateFactory(id, mapper.toEntity(request));
        updated.describeAs(request.address());
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FactoryResponse> patchFactory(@PathVariable Long id,
                                                        @RequestBody FactoryRequest request) {
        Factory patched = service.patchFactory(id, mapper.toEntity(request));
        resolveAddress(patched);
        return ResponseEntity.ok(mapper.toResponse(patched));
    }

    @PostMapping("/{id}/produce")
    public ResponseEntity<FactoryResponse> produceFactory(@PathVariable Long id,
                                                          @RequestParam int quantity) {
        Factory factory = service.produceFactory(id, quantity);
        resolveAddress(factory);
        return ResponseEntity.ok(mapper.toResponse(factory));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFactory(@PathVariable Long id) {
        service.deleteFactory(id);
        return ResponseEntity.noContent().build();
    }

    private void resolveAddress(Factory factory) {
        if (factory.getLatitude() != null && factory.getLongitude() != null) {
            factory.describeAs(geocodingClient.reverseGeocode(factory.getLatitude(), factory.getLongitude())
                    .address());
        }
    }
}
