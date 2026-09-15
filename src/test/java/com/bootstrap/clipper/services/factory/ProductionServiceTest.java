package com.bootstrap.clipper.services.factory;

import com.bootstrap.clipper.models.dao.Factory;
import com.bootstrap.clipper.repositories.FactoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTest {

    @Mock
    private FactoryRepository repository;

    @InjectMocks
    private ProductionService productionService;

    @Test
    void produce_shouldAddProductionToStock() {
        Factory factory = new Factory("Paris", 10);

        factory.produce();

        assertEquals(10, factory.getStock());
    }

    @Test
    void produce_calledThreeTimes_shouldAccumulateStock() {
        Factory factory = new Factory("Lyon", 5);

        factory.produce();
        factory.produce();
        factory.produce();

        assertEquals(15, factory.getStock());
    }

    @Test
    void produce_withExistingStock_shouldAddOnTop() {
        Factory factory = new Factory("Marseille", 7);
        factory.produce(20);

        factory.produce();

        assertEquals(27, factory.getStock());
    }

    // ──── Tests sur ProductionService.produceClips() (avec mock) ────

    @Test
    void produceClips_shouldCallProduceOnEachFactory() {
        Factory paris = new Factory("Paris", 10);
        Factory lyon = new Factory("Lyon", 5);
        when(repository.findAll()).thenReturn(List.of(paris, lyon));

        productionService.produceClips();

        assertEquals(10, paris.getStock());
        assertEquals(5, lyon.getStock());
        verify(repository).saveAll(List.of(paris, lyon));
    }

    @Test
    void produceClips_calledTwice_shouldDoubleProduction() {
        Factory factory = new Factory("Paris", 8);
        when(repository.findAll()).thenReturn(List.of(factory));

        productionService.produceClips();
        productionService.produceClips();

        assertEquals(16, factory.getStock());
        verify(repository, times(2)).saveAll(List.of(factory));
    }

    @Test
    void produceClips_withNoFactory_shouldDoNothing() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        productionService.produceClips();

        verify(repository).saveAll(Collections.emptyList());
    }
}
