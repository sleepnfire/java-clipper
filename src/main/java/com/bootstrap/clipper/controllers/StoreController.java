package com.bootstrap.clipper.controllers;

import com.bootstrap.clipper.clients.GeocodingClient;
import com.bootstrap.clipper.models.dao.Shipment;
import com.bootstrap.clipper.models.dao.Store;
import com.bootstrap.clipper.models.dto.FactoryAvailabilityResponse;
import com.bootstrap.clipper.models.dto.ShipmentResponse;
import com.bootstrap.clipper.models.dto.StoreRequest;
import com.bootstrap.clipper.models.dto.StoreResponse;
import com.bootstrap.clipper.models.dto.SupplyRequest;
import com.bootstrap.clipper.models.mappers.ShipmentMapper;
import com.bootstrap.clipper.models.mappers.StoreMapper;
import com.bootstrap.clipper.services.store.StoreService;
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
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService service;
    private final StoreMapper mapper;
    private final ShipmentMapper shipmentMapper;
    private final GeocodingClient geocodingClient;

    @PostMapping
    public ResponseEntity<StoreResponse> saveStore(@Valid @RequestBody StoreRequest request) {
        Store saved = service.saveStore(mapper.toEntity(request));
        saved.describeAs(request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getAllStores() {
        List<Store> stores = service.getAllStores();
        stores.forEach(this::resolveAddress);
        return ResponseEntity.ok(mapper.toResponseList(stores));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long id) {
        Store store = service.getStore(id);
        resolveAddress(store);
        return ResponseEntity.ok(mapper.toResponse(store));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreResponse> updateStore(@PathVariable Long id,
                                                     @Valid @RequestBody StoreRequest request) {
        Store updated = service.updateStore(id, mapper.toEntity(request));
        updated.describeAs(request.address());
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StoreResponse> patchStore(@PathVariable Long id,
                                                    @RequestBody StoreRequest request) {
        Store patched = service.patchStore(id, mapper.toEntity(request));
        resolveAddress(patched);
        return ResponseEntity.ok(mapper.toResponse(patched));
    }

    @GetMapping("/{id}/factories")
    public ResponseEntity<List<FactoryAvailabilityResponse>> getAvailableFactories(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAvailableFactories(id));
    }

    @GetMapping("/{id}/shipments")
    public ResponseEntity<List<ShipmentResponse>> getShipments(
            @PathVariable Long id,
            @RequestParam(required = false) String status) {
        List<Shipment> shipments = service.getShipments(id, status);
        shipments.forEach(this::resolveShipmentAddresses);
        return ResponseEntity.ok(shipmentMapper.toResponseList(shipments));
    }

    @PostMapping("/{id}/supply")
    public ResponseEntity<ShipmentResponse> supplyStore(@PathVariable Long id,
                                                        @Valid @RequestBody SupplyRequest request) {
        Shipment shipment = service.supplyStore(id, request.factoryId(), request.quantity());
        resolveShipmentAddresses(shipment);
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentMapper.toResponse(shipment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable Long id) {
        service.deleteStore(id);
        return ResponseEntity.noContent().build();
    }

    private void resolveAddress(Store store) {
        if (store.getLatitude() != null && store.getLongitude() != null) {
            store.describeAs(geocodingClient.reverseGeocode(store.getLatitude(), store.getLongitude())
                    .address());
        }
    }

    private void resolveShipmentAddresses(Shipment shipment) {
        var factory = shipment.getFactory();
        if (factory.getLatitude() != null && factory.getLongitude() != null) {
            factory.describeAs(geocodingClient.reverseGeocode(factory.getLatitude(), factory.getLongitude())
                    .address());
        }
        var store = shipment.getStore();
        if (store.getLatitude() != null && store.getLongitude() != null) {
            store.describeAs(geocodingClient.reverseGeocode(store.getLatitude(), store.getLongitude())
                    .address());
        }
    }
}
