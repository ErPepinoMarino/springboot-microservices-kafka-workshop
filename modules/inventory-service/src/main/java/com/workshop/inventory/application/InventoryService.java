package com.workshop.inventory.application;

import com.workshop.inventory.domain.ProcessedOrder;
import com.workshop.inventory.domain.ProcessedOrderRepository;
import com.workshop.inventory.domain.Stock;
import com.workshop.inventory.domain.StockMovement;
import com.workshop.inventory.domain.StockMovementRepository;
import com.workshop.inventory.domain.StockRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProcessedOrderRepository processedOrderRepository;

    public InventoryService(StockRepository stockRepository,
                            StockMovementRepository stockMovementRepository,
                            ProcessedOrderRepository processedOrderRepository) {
        this.stockRepository = stockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.processedOrderRepository = processedOrderRepository;
    }

    @Transactional
    public void reserveOrder(ReserveOrderRequest request) {
        Long orderId = request.orderId();
        if (orderId == null || request.lines() == null || request.lines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido sin líneas: " + orderId);
        }

        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (ReserveOrderRequest.ReserveOrderItemRequest line : request.lines()) {
            if (line.productId() == null || line.productId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU vacío en el pedido: " + orderId);
            }
            if (line.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cantidad no positiva para " + line.productId() + " en el pedido: " + orderId);
            }
            if (!seen.add(line.productId())) {
                duplicates.add(line.productId());
            }
        }
        if (!duplicates.isEmpty()) {
            log.warn("Pedido {} rechazado por SKUs repetidos: {}", orderId, duplicates);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Productos repetidos en el pedido: " + String.join(", ", duplicates));
        }

        if (processedOrderRepository.existsById(orderId)) {
            log.info("Pedido {} ya procesado; se ignora la reentrega", orderId);
            return;
        }

        processedOrderRepository.saveAndFlush(new ProcessedOrder(orderId, Instant.now()));

        for (ReserveOrderRequest.ReserveOrderItemRequest line : request.lines()) {
            Stock stock = stockRepository.findById(line.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "SKU sin stock: " + line.productId()));
            if (stock.getAvailableQuantity() < line.quantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente para " + line.productId() + " en el pedido: " + orderId);
            }
            stock.setAvailableQuantity(stock.getAvailableQuantity() - line.quantity());
            stockMovementRepository.save(new StockMovement(line.productId(), orderId, "salida",
                    line.quantity(), Instant.now()));
        }

        log.info("Stock reservado para el pedido {}: {} líneas", orderId, request.lines().size());
    }
}