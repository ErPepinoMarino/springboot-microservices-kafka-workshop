package com.workshop.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "registro_ordenes")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "tipo_de_movimiento", nullable = false)
    private String movementType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public StockMovement() {
    }

    public StockMovement(String productId, Long orderId, String movementType, int quantity,
                         Instant occurredAt) {
        this.productId = productId;
        this.orderId = orderId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}