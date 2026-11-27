package com.workshop.inventory.application;

import java.util.List;

public record ReserveOrderRequest(Long orderId, List<ReserveOrderItemRequest> lines) {

    public ReserveOrderRequest {
        lines = List.copyOf(lines);
    }

    public record ReserveOrderItemRequest(String productId, int quantity) {
    }
}