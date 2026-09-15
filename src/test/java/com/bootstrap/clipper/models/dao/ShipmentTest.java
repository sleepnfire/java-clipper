package com.bootstrap.clipper.models.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShipmentTest {

    private Shipment inTransit(Store store) {
        LocalDateTime now = LocalDateTime.now();
        return new Shipment(new Factory("Paris", 10), store, 30, 12.5, now, now.plusMinutes(1));
    }

    @Test
    @DisplayName("Une expedition nait TOUJOURS en transit : le status n'est pas un parametre")
    void newShipment_isAlwaysInTransit() {
        Shipment shipment = inTransit(new Store("Lyon"));

        assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
        assertEquals(null, shipment.getDeliveredAt());
    }

    @Test
    @DisplayName("La livraison est atomique : status, date et stock bougent ensemble")
    void markDelivered_isAtomic() {
        Store store = new Store("Lyon");
        Shipment shipment = inTransit(store);

        shipment.markDelivered(LocalDateTime.now());

        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
        assertNotNull(shipment.getDeliveredAt(), "DELIVERED sans deliveredAt est inexprimable");
        assertEquals(30, store.getStock(), "la livraison EST l'entree en stock");
    }

    @Test
    @DisplayName("Livrer deux fois est refuse : pas de double credit de stock")
    void markDelivered_twice_shouldThrow() {
        Store store = new Store("Lyon");
        Shipment shipment = inTransit(store);
        shipment.markDelivered(LocalDateTime.now());

        assertThrows(IllegalStateException.class,
                () -> shipment.markDelivered(LocalDateTime.now()));
        assertEquals(30, store.getStock(), "le stock ne doit pas avoir ete credite deux fois");
    }

    @Test
    @DisplayName("hasArrived depend de l'heure et du status")
    void hasArrived() {
        LocalDateTime now = LocalDateTime.now();
        Shipment shipment = new Shipment(new Factory("Paris", 10), new Store("Lyon"),
                30, 12.5, now.minusHours(2), now.minusHours(1));

        org.junit.jupiter.api.Assertions.assertTrue(shipment.hasArrived(now));

        shipment.markDelivered(now);
        org.junit.jupiter.api.Assertions.assertFalse(shipment.hasArrived(now));
    }
}
