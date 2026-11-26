package com.workshop.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_disponible")
public class Stock {

    @Id
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    public Stock() {
    }

    public Stock(String productId, int availableQuantity) {
        this.productId = productId;
        this.availableQuantity = availableQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}
