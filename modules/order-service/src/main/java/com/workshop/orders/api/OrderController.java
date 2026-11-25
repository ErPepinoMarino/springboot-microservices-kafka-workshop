package com.workshop.orders.api;

import com.workshop.orders.application.CreateOrderRequest;
import com.workshop.orders.domain.Order;
import com.workshop.orders.domain.OrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;

    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = new Order(
                request.getCustomerEmail(),
                "PENDING",
                Instant.now()
        );

        Order saved = orderRepository.save(order);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}