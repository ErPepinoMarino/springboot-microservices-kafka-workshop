package com.workshop.orders.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class CreateOrderItemRequest {

    @NotBlank(message = "El producto es obligatorio")
    private String productId;

    @Positive(message = "La cantidad debe ser mayor que cero")
    private int quantity;

    public CreateOrderItemRequest() {
    }

    public CreateOrderItemRequest(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
