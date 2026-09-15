package com.bootstrap.clipper.models.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factory_id")
    private Factory factory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(nullable = false)
    private Integer quantity;

    @Column
    private Double distanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column
    private LocalDateTime departedAt;

    @Column
    private LocalDateTime estimatedArrivalAt;

    @Column
    private LocalDateTime deliveredAt;

    public Shipment(Factory factory, Store store, int quantity, double distanceKm,
                    LocalDateTime departedAt, LocalDateTime estimatedArrivalAt) {
        this.factory = factory;
        this.store = store;
        this.quantity = quantity;
        this.distanceKm = distanceKm;
        this.departedAt = departedAt;
        this.estimatedArrivalAt = estimatedArrivalAt;
        this.status = ShipmentStatus.IN_TRANSIT;
    }

    public void markDelivered(LocalDateTime deliveredAt) {
        if (this.status == ShipmentStatus.DELIVERED) {
            throw new IllegalStateException("L'expédition #" + this.id + " est déjà livrée");
        }
        this.status = ShipmentStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
        this.store.receive(this.quantity);
    }

    public boolean hasArrived(LocalDateTime now) {
        return this.status == ShipmentStatus.IN_TRANSIT
                && this.estimatedArrivalAt != null
                && this.estimatedArrivalAt.isBefore(now);
    }
}
