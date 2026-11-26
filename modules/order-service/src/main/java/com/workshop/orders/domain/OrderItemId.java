package com.workshop.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

//Solucion de java para crear claves compuestas en JPA, rarete pero es lo que hay.
@Embeddable
public class OrderItemId implements Serializable {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    public OrderItemId() {
    }

    public OrderItemId(Long orderId, String productId) {
        this.orderId = orderId;
        this.productId = productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderItemId that)) {
            return false;
        }
        return Objects.equals(orderId, that.orderId)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, productId);
    }
}
