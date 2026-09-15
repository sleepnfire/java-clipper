package com.bootstrap.clipper.services.factory;

import com.bootstrap.clipper.clients.GeocodingClient;
import com.bootstrap.clipper.configurations.exceptions.type.NotFoundException;
import com.bootstrap.clipper.models.dao.Factory;
import com.bootstrap.clipper.models.dto.GeocodingResult;
import com.bootstrap.clipper.repositories.FactoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactoryServiceTest {

    @Mock
    private FactoryRepository repository;

    @Mock
    private GeocodingClient geocodingClient;

    @InjectMocks
    private FactoryServiceImp factoryService;

    // ──── Tests sur saveFactory ────

    @Test
    void saveFactory_withAddress_shouldGeocodeAndSave() {
        Factory factory = new Factory("Usine Paris", 100);
        factory.describeAs("10 rue de Rivoli, Paris");

        GeocodingResult geocodingResult = new GeocodingResult(
                "10 Rue de Rivoli 75004 Paris", 48.8555, 2.3604, 0.95);

        when(geocodingClient.geocode("10 rue de Rivoli, Paris")).thenReturn(geocodingResult);
        when(repository.save(factory)).thenReturn(factory);

        Factory result = factoryService.saveFactory(factory);

        assertEquals(48.8555, result.getLatitude());
        assertEquals(2.3604, result.getLongitude());
        assertEquals("Usine Paris", result.getName());
        assertEquals(0, result.getStock(), "une usine nait toujours avec un stock vide");
        verify(geocodingClient).geocode("10 rue de Rivoli, Paris");
        verify(repository).save(factory);
    }

    @Test
    void saveFactory_withoutAddress_shouldSkipGeocodeAndSave() {
        Factory factory = new Factory("Usine Anonyme", 50);

        when(repository.save(factory)).thenReturn(factory);

        Factory result = factoryService.saveFactory(factory);

        assertEquals("Usine Anonyme", result.getName());
        assertEquals(50, result.getProduction());
        verify(geocodingClient, never()).geocode(any());
        verify(repository).save(factory);
    }

    // ──── Tests sur getFactory ────

    @Test
    void getFactory_existing_shouldReturnFactory() {
        Factory factory = new Factory("Usine Lyon", 200);

        when(repository.findById(1L)).thenReturn(Optional.of(factory));

        Factory result = factoryService.getFactory(1L);

        assertNotNull(result);
        assertEquals("Usine Lyon", result.getName());
        assertEquals(200, result.getProduction());
    }

    @Test
    void getFactory_notFound_shouldThrowNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> factoryService.getFactory(999L)
        );

        assertEquals("L'usine n'existe pas", exception.getMessage());
    }

    // ──── Tests sur getAllFactory ────

    @Test
    void getAllFactory_shouldReturnAllFactories() {
        Factory paris = new Factory("Paris", 10);
        Factory lyon = new Factory("Lyon", 20);

        when(repository.findAll()).thenReturn(List.of(paris, lyon));

        List<Factory> result = factoryService.getAllFactory();

        assertEquals(2, result.size());
        assertEquals("Paris", result.get(0).getName());
        assertEquals("Lyon", result.get(1).getName());
    }

    @Test
    void getAllFactory_empty_shouldReturnEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<Factory> result = factoryService.getAllFactory();

        assertTrue(result.isEmpty());
    }

    // ──── Tests sur deleteFactory ────

    @Test
    void deleteFactory_existing_shouldDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(new Factory("Usine Paris", 10)));

        factoryService.deleteFactory(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteFactory_notFound_shouldThrowNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> factoryService.deleteFactory(999L)
        );

        verify(repository, never()).deleteById(any());
    }

    // ──── Tests sur produceFactory ────

    @Test
    void produceFactory_shouldAddToStock() {
        Factory factory = new Factory("Paris", 10);
        when(repository.findById(1L)).thenReturn(Optional.of(factory));
        when(repository.save(factory)).thenReturn(factory);

        Factory result = factoryService.produceFactory(1L, 40);

        assertEquals(40, result.getStock());
    }
}
