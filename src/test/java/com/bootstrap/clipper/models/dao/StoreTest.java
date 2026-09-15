package com.bootstrap.clipper.models.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoreTest {

    @Test
    @DisplayName("Un magasin nait avec un stock vide")
    void newStore_startsEmpty() {
        assertEquals(0, new Store("Paris").getStock());
    }

    @Test
    @DisplayName("Un magasin sans nom est refuse des la construction")
    void newStore_blankName_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> new Store(""));
    }

    @Test
    @DisplayName("Recevoir une quantite negative est refuse")
    void receive_negativeQuantity_shouldThrow() {
        Store store = new Store("Paris");

        assertThrows(IllegalArgumentException.class, () -> store.receive(-5));
        assertEquals(0, store.getStock());
    }

    @Test
    @DisplayName("Recevoir cumule les livraisons")
    void receive_shouldAccumulate() {
        Store store = new Store("Paris");

        store.receive(20);
        store.receive(12);

        assertEquals(32, store.getStock());
    }

    @Test
    @DisplayName("Une position se pose en un seul geste")
    void locateAt_setsBothCoordinates() {
        Store store = new Store("Paris");

        store.locateAt(48.8566, 2.3522);

        assertEquals(48.8566, store.getLatitude());
        assertEquals(2.3522, store.getLongitude());
    }
}
