package com.workshop.orders.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateOrderRequest {

    @NotEmpty(message = "El pedido debe tener al menos una línea")
    private List<@Valid CreateOrderItemRequest> lines;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(List<CreateOrderItemRequest> lines) {
        this.lines = lines;
    }

    public List<CreateOrderItemRequest> getLines() {
        return lines;
    }

    public void setLines(List<CreateOrderItemRequest> lines) {
        this.lines = lines;
    }
}
