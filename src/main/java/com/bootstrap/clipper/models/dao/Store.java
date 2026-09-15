package com.bootstrap.clipper.models.dao;

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
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @Column(nullable = false)
    private String name;

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

    public Store(String name) {
        if (name != null) this.name = requireName(name);
        this.stock = 0;
    }

    public void receive(int quantity) {
        this.stock += requirePositive(quantity, "La quantité reçue doit être positive");
    }

    public void rename(String name) {
        this.name = requireName(name);
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
            throw new IllegalArgumentException("Le nom du magasin est obligatoire");
        }
        return name;
    }

    private static int requirePositive(int value, String message) {
        if (value <= 0) throw new IllegalArgumentException(message);
        return value;
    }
}
