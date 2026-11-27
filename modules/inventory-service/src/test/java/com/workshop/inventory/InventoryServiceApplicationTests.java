package com.workshop.inventory;

import com.workshop.inventory.application.InventoryService;
import com.workshop.inventory.application.ReserveOrderRequest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class InventoryServiceApplicationTests {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("workshop_inventory")
            .withUsername("workshop")
            .withPassword("workshop");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void seedStock() {
        jdbcTemplate.update("INSERT INTO stock_disponible (product_id, available_quantity) VALUES (?, ?)",
                "P1", 10);
        jdbcTemplate.update("INSERT INTO stock_disponible (product_id, available_quantity) VALUES (?, ?)",
                "P2", 5);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM registro_ordenes");
        jdbcTemplate.update("DELETE FROM processed_order");
        jdbcTemplate.update("DELETE FROM stock_disponible");
    }

    @Test
    void shouldApplyInventoryMigrations() {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables " +
                        "WHERE table_name IN ('stock_disponible', 'registro_ordenes')",
                Integer.class
        );

        assertThat(tables).isEqualTo(2);
    }

    @Test
    void shouldReserveAllLines() {
        inventoryService.reserveOrder(request(1L, item("P1", 2), item("P2", 3)));

        assertThat(stock("P1")).isEqualTo(8);
        assertThat(stock("P2")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM registro_ordenes WHERE order_id = ?", 1L)).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM processed_order WHERE order_id = ?", 1L)).isEqualTo(1);
    }

    @Test
    void shouldIgnoreRedelivery() {
        ReserveOrderRequest request = request(2L, item("P1", 4));

        inventoryService.reserveOrder(request);
        inventoryService.reserveOrder(request);

        assertThat(stock("P1")).isEqualTo(6);
        assertThat(count("SELECT count(*) FROM registro_ordenes WHERE order_id = ?", 2L)).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM processed_order WHERE order_id = ?", 2L)).isEqualTo(1);
    }

    @Test
    void shouldRollbackEverythingWhenStockInsufficient() {
        assertThatThrownBy(() -> inventoryService.reserveOrder(request(3L, item("P1", 2), item("P2", 99))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(stock("P1")).isEqualTo(10);
        assertThat(stock("P2")).isEqualTo(5);
        assertThat(count("SELECT count(*) FROM registro_ordenes WHERE order_id = ?", 3L)).isZero();
        assertThat(count("SELECT count(*) FROM processed_order WHERE order_id = ?", 3L)).isZero();
    }

    @Test
    void shouldRejectDuplicateSku() {
        assertThatThrownBy(() -> inventoryService.reserveOrder(request(4L, item("P1", 1), item("P1", 2))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Productos repetidos")
                .hasMessageContaining("P1")
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(stock("P1")).isEqualTo(10);
        assertThat(count("SELECT count(*) FROM registro_ordenes WHERE order_id = ?", 4L)).isZero();
        assertThat(count("SELECT count(*) FROM processed_order WHERE order_id = ?", 4L)).isZero();
    }

    private static ReserveOrderRequest request(Long orderId,
                                               ReserveOrderRequest.ReserveOrderItemRequest... lines) {
        return new ReserveOrderRequest(orderId, List.of(lines));
    }

    private static ReserveOrderRequest.ReserveOrderItemRequest item(String productId, int quantity) {
        return new ReserveOrderRequest.ReserveOrderItemRequest(productId, quantity);
    }

    private Integer stock(String productId) {
        return jdbcTemplate.queryForObject(
                "SELECT available_quantity FROM stock_disponible WHERE product_id = ?",
                Integer.class, productId);
    }

    private int count(String sql, Object... args) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }
}
