package com.bootstrap.clipper.models.dao;

import com.bootstrap.clipper.configurations.exceptions.type.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Factory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "factory_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer production;

    @Column(nullable = false)
    private Integer stock = 0;

    @Version
    private Long version;

    @Transient
    private String address;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    public Factory(String name, Integer production) {
        if (name != null) this.name = requireName(name);
        if (production != null) this.production = requirePositive(production, "La production doit être positive");
        this.stock = 0;
    }

    public void produce() {
        this.stock += this.production;
    }

    public void produce(int quantity) {
        this.stock += requirePositive(quantity, "La quantité produite doit être positive");
    }

    public void ship(int quantity) {
        requirePositive(quantity, "La quantité expédiée doit être positive");
        if (this.stock < quantity) {
            throw new ConflictException("Stock insuffisant : l'usine '" + this.name
                    + "' a " + this.stock + " trombones, " + quantity + " demandés");
        }
        this.stock -= quantity;
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void changeProduction(int production) {
        this.production = requirePositive(production, "La production doit être positive");
    }

    public void locateAt(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void describeAs(String address) {
        this.address = address;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de l'usine est obligatoire");
        }
        return name;
    }

    private static int requirePositive(int value, String message) {
        if (value <= 0) throw new IllegalArgumentException(message);
        return value;
    }
}
