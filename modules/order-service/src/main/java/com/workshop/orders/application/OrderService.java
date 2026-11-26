package com.workshop.orders.application;

import com.workshop.orders.domain.Order;
import com.workshop.orders.domain.OrderItem;
import com.workshop.orders.domain.OrderItemRepository;
import com.workshop.orders.domain.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public Order replaceDraftLines(Long orderId, CreateOrderRequest request) {
        Order order = findOrder(orderId);

        if (!"DRAFT".equals(order.getStatus())) {
            return order;
        }

        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (CreateOrderItemRequest line : request.getLines()) {
            if (!seen.add(line.getProductId())) {
                duplicates.add(line.getProductId());
            }
        }
        if (!duplicates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Productos repetidos en el pedido: " + String.join(", ", duplicates));
        }

        orderItemRepository.deleteByIdOrderId(orderId);
        for (CreateOrderItemRequest line : request.getLines()) {
            orderItemRepository.save(new OrderItem(orderId, line.getProductId(), line.getQuantity()));
        }
        return order;
    }

    @Transactional
    public Order submitDraft(Long orderId) {
        Order order = findOrder(orderId);

        if (!"DRAFT".equals(order.getStatus())) {
            return order;
        }

        order.setStatus("PENDING");
        return order;
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + orderId));
    }
}
