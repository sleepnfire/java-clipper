package com.bootstrap.clipper.services.shipment;

import com.bootstrap.clipper.models.dao.ShipmentStatus;
import com.bootstrap.clipper.repositories.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final ShipmentRepository shipmentRepository;

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void checkDeliveries() {
        var arrivedShipments = shipmentRepository
                .findByStatusAndEstimatedArrivalAtBefore(ShipmentStatus.IN_TRANSIT, LocalDateTime.now());

        for (var shipment : arrivedShipments) {
            shipment.markDelivered(LocalDateTime.now());

            log.info("Livraison #{} terminee : usine '{}' vers magasin '{}', +{} trombones (stock: {})",
                    shipment.getId(),
                    shipment.getFactory().getName(),
                    shipment.getStore().getName(),
                    shipment.getQuantity(),
                    shipment.getStore().getStock());
        }
    }
}
