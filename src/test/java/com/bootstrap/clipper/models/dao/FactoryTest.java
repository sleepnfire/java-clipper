package com.bootstrap.clipper.models.dao;

import com.bootstrap.clipper.configurations.exceptions.type.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FactoryTest {

    @Test
    @DisplayName("Une usine nait avec un stock vide")
    void newFactory_startsEmpty() {
        Factory factory = new Factory("Paris", 10);

        assertEquals(0, factory.getStock());
        assertEquals(10, factory.getProduction());
    }

    @Test
    @DisplayName("Une usine sans nom est refusee des la construction")
    void newFactory_blankName_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> new Factory("  ", 10));
    }

    @Test
    @DisplayName("Une production nulle ou negative est refusee des la construction")
    void newFactory_nonPositiveProduction_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> new Factory("Paris", 0));
        assertThrows(IllegalArgumentException.class, () -> new Factory("Paris", -5));
    }

    @Test
    @DisplayName("Expedier plus que le stock echoue et ne modifie rien")
    void ship_moreThanStock_shouldThrowAndLeaveStockUntouched() {
        Factory factory = new Factory("Paris", 10);
        factory.produce();

        assertThrows(ConflictException.class, () -> factory.ship(50));
        assertEquals(10, factory.getStock(), "le stock doit rester intact apres un echec");
    }

    @Test
    @DisplayName("Expedier retire exactement la quantite demandee")
    void ship_shouldDecreaseStock() {
        Factory factory = new Factory("Paris", 100);
        factory.produce();

        factory.ship(30);

        assertEquals(70, factory.getStock());
    }

    @Test
    @DisplayName("Expedier une quantite negative est refuse")
    void ship_negativeQuantity_shouldThrow() {
        Factory factory = new Factory("Paris", 100);
        factory.produce();

        assertThrows(IllegalArgumentException.class, () -> factory.ship(-10));
    }
}
