package com.bootstrap.clipper.services.factory;

import com.bootstrap.clipper.clients.GeocodingClient;
import com.bootstrap.clipper.configurations.exceptions.type.NotFoundException;
import com.bootstrap.clipper.models.dao.Factory;
import com.bootstrap.clipper.models.dto.GeocodingResult;
import com.bootstrap.clipper.repositories.FactoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FactoryServiceImp implements FactoryService {

    private final FactoryRepository repository;
    private final GeocodingClient geocodingClient;

    public Factory saveFactory(Factory request) {
        applyGeocoding(request);
        return repository.save(request);
    }

    public Factory updateFactory(Long factoryId, Factory request) {
        var factory = repository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException("L'usine avec l'id " + factoryId + " n'existe pas"));

        factory.rename(request.getName());
        factory.changeProduction(request.getProduction());
        factory.describeAs(request.getAddress());
        applyGeocoding(factory);

        return repository.save(factory);
    }

    public Factory patchFactory(Long factoryId, Factory request) {
        var factory = repository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException("L'usine avec l'id " + factoryId + " n'existe pas"));

        if (request.getName() != null) factory.rename(request.getName());
        if (request.getProduction() != null) factory.changeProduction(request.getProduction());
        if (request.getAddress() != null) {
            factory.describeAs(request.getAddress());
            applyGeocoding(factory);
        }

        return repository.save(factory);
    }

    public Factory getFactory(Long factoryId) {
        return repository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException("L'usine n'existe pas"));
    }

    public List<Factory> getAllFactory() {
        return repository.findAll();
    }

    public void deleteFactory(Long factoryId) {
        Optional<Factory> factoryOptional = repository.findById(factoryId);
        if (factoryOptional.isEmpty()) {
            throw new NotFoundException("l'usine n'existe pas");
        }
        repository.deleteById(factoryId);
    }

    public Factory produceFactory(Long factoryId, int quantity) {
        var factory = repository.findById(factoryId)
                .orElseThrow(() -> new NotFoundException("L'usine avec l'id " + factoryId + " n'existe pas"));
        factory.produce(quantity);
        return repository.save(factory);
    }

    private void applyGeocoding(Factory factory) {
        if (factory.getAddress() == null) return;
        GeocodingResult result = geocodingClient.geocode(factory.getAddress());
        factory.locateAt(result.latitude(), result.longitude());
    }
}
