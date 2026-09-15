package com.bootstrap.clipper.services.store;

import com.bootstrap.clipper.clients.GeocodingClient;
import com.bootstrap.clipper.configurations.exceptions.type.ConflictException;
import com.bootstrap.clipper.configurations.exceptions.type.NotFoundException;
import com.bootstrap.clipper.models.dao.Factory;
import com.bootstrap.clipper.models.dao.Shipment;
import com.bootstrap.clipper.models.dao.ShipmentStatus;
import com.bootstrap.clipper.models.dao.Store;
import com.bootstrap.clipper.models.dto.FactoryAvailabilityResponse;
import com.bootstrap.clipper.models.dto.GeocodingResult;
import com.bootstrap.clipper.repositories.FactoryRepository;
import com.bootstrap.clipper.repositories.ShipmentRepository;
import com.bootstrap.clipper.repositories.StoreRepository;
import com.bootstrap.clipper.services.GeoService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreServiceImp implements StoreService {

    private final StoreRepository repository;
    private final FactoryRepository factoryRepository;
    private final ShipmentRepository shipmentRepository;
    private final TransactionTemplate transactionTemplate;
    private final GeocodingClient geocodingClient;
    private final GeoService geoService;

    private static final int MAX_RETRIES = 3;
    private static final double SPEED_KMH = 1235.0; // vitesse du son

    public Store saveStore(Store store) {
        applyGeocoding(store);
        return repository.save(store);
    }

    public Store updateStore(Long storeId, Store request) {
        var store = getStoreOrThrow(storeId);

        store.rename(request.getName());
        store.describeAs(request.getAddress());
        applyGeocoding(store);

        return repository.save(store);
    }

    public Store patchStore(Long storeId, Store request) {
        var store = getStoreOrThrow(storeId);

        if (request.getName() != null) store.rename(request.getName());
        if (request.getAddress() != null) {
            store.describeAs(request.getAddress());
            applyGeocoding(store);
        }

        return repository.save(store);
    }

    public Store getStore(Long storeId) {
        return getStoreOrThrow(storeId);
    }

    public List<Store> getAllStores() {
        return repository.findAll();
    }

    public void deleteStore(Long storeId) {
        if (!repository.existsById(storeId)) {
            throw new NotFoundException("Le magasin avec l'id " + storeId + " n'existe pas");
        }
        repository.deleteById(storeId);
    }

    public List<FactoryAvailabilityResponse> getAvailableFactories(Long storeId) {
        Store store = getStoreOrThrow(storeId);

        return factoryRepository.findAll().stream()
                .map(factory -> {
                    double distanceKm = geoService.calculateDistance(
                            store.getLatitude(), store.getLongitude(),
                            factory.getLatitude(), factory.getLongitude()
                    );
                    return new FactoryAvailabilityResponse(
                            factory.getId(),
                            factory.getName(),
                            factory.getProduction(),
                            factory.getStock(),
                            Math.round(distanceKm * 100.0) / 100.0
                    );
                })
                .toList();
    }

    public Shipment supplyStore(Long storeId, Long factoryId, int quantity) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return transactionTemplate.execute(status -> {
                    Store store = getStoreOrThrow(storeId);

                    Factory factory = factoryRepository.findById(factoryId)
                            .orElseThrow(() -> new NotFoundException("L'usine avec l'id " + factoryId + " n'existe pas"));

                    // Distance réelle via Haversine
                    double distanceKm = geoService.calculateDistance(
                            factory.getLatitude(), factory.getLongitude(),
                            store.getLatitude(), store.getLongitude()
                    );
                    double deliverySeconds = (distanceKm / SPEED_KMH) * 3600;

                    factory.ship(quantity);

                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime arrival = now.plusSeconds((long) deliverySeconds);

                    Shipment shipment = new Shipment(
                            factory,
                            store,
                            quantity,
                            Math.round(distanceKm * 100.0) / 100.0,
                            now,
                            arrival
                    );

                    log.info("Expedition de {} trombones : usine '{}' vers magasin '{}', {} km, arrivee dans {}s",
                            quantity, factory.getName(), store.getName(),
                            Math.round(distanceKm * 10.0) / 10.0,
                            Math.round(deliverySeconds * 10.0) / 10.0);

                    factoryRepository.save(factory);
                    return shipmentRepository.save(shipment);
                });
            } catch (OptimisticLockException e) {
                log.warn("Tentative {}/{} echouee, conflit de concurrence", attempt, MAX_RETRIES);
            }
        }
        throw new ConflictException("Conflit de concurrence apres "
                + MAX_RETRIES + " tentatives, reessayez plus tard");
    }

    public List<Shipment> getShipments(Long storeId, String status) {
        if (!repository.existsById(storeId)) {
            throw new NotFoundException("Le magasin avec l'id " + storeId + " n'existe pas");
        }

        ShipmentStatus shipmentStatus = status != null
                ? ShipmentStatus.valueOf(status.toUpperCase())
                : ShipmentStatus.IN_TRANSIT;

        return shipmentRepository.findByStoreIdAndStatus(storeId, shipmentStatus);
    }

    private Store getStoreOrThrow(Long storeId) {
        return repository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Le magasin avec l'id " + storeId + " n'existe pas"));
    }

    private void applyGeocoding(Store store) {
        if (store.getAddress() == null) return;
        GeocodingResult result = geocodingClient.geocode(store.getAddress());
        store.locateAt(result.latitude(), result.longitude());
    }
}
