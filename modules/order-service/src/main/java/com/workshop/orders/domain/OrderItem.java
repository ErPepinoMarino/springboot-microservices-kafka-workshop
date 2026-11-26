package com.workshop.orders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @EmbeddedId
    private OrderItemId id;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public OrderItem() {
    }

    public OrderItem(Long orderId, String productId, int quantity) {
        this.id = new OrderItemId(orderId, productId);
        this.quantity = quantity;
    }

    public OrderItemId getId() {
        return id;
    }

    public int getQuantity() {
        return quantity;
    }
}
