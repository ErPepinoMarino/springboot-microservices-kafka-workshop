package com.workshop.orders.api;

import com.workshop.orders.application.CreateDraftRequest;
import com.workshop.orders.application.CreateOrderRequest;
import com.workshop.orders.application.OrderService;
import com.workshop.orders.domain.Order;
import com.workshop.orders.domain.OrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderController(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @PostMapping("/drafts")
    public ResponseEntity<Order> createDraft(@Valid @RequestBody CreateDraftRequest request) {
        Order order = new Order(
                request.getCustomerEmail(),
                "DRAFT",
                Instant.now()
        );

        Order saved = orderRepository.save(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<Order> replaceDraftLines(
            @PathVariable Long orderId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ResponseEntity.ok(orderService.replaceDraftLines(orderId, request));
    }

    @PostMapping("/{orderId}/submit")
    public ResponseEntity<Order> submitDraft(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.submitDraft(orderId));
    }
}
