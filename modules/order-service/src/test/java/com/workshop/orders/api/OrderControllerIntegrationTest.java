package com.workshop.orders.api;

import com.workshop.orders.application.CreateDraftRequest;
import com.workshop.orders.application.CreateOrderItemRequest;
import com.workshop.orders.application.CreateOrderRequest;
import com.workshop.orders.domain.Order;
import com.workshop.orders.domain.OrderItem;
import com.workshop.orders.domain.OrderItemRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestRestTemplate
class OrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("workshop")
            .withUsername("workshop")
            .withPassword("workshop");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void shouldAddModifyAndDeleteLinesWithPut() {
        Long orderId = createDraft("cliente@ejemplo.com");

        ResponseEntity<String> first = replaceLines(orderId, new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("patatas", 1),
                new CreateOrderItemRequest("queso", 2)
        )));

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody()).contains("DRAFT");
        assertThat(itemsOf(orderId)).extracting(item -> item.getId().getProductId())
                .containsExactlyInAnyOrder("patatas", "queso");

        ResponseEntity<String> second = replaceLines(orderId, new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("patatas", 3),
                new CreateOrderItemRequest("pan", 1)
        )));

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody()).contains("DRAFT");

        List<OrderItem> items = itemsOf(orderId);
        assertThat(items).extracting(item -> item.getId().getProductId())
                .containsExactlyInAnyOrder("patatas", "pan");
        assertThat(items).extracting(OrderItem::getQuantity)
                .containsExactlyInAnyOrder(3, 1);
    }

    @Test
    void shouldRejectRepeatedProducts() {
        Long orderId = createDraft("cliente@ejemplo.com");

        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("patatas", 1),
                new CreateOrderItemRequest("queso", 2),
                new CreateOrderItemRequest("patatas", 2)
        ));

        ResponseEntity<String> response = replaceLines(orderId, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Productos repetidos en el pedido: patatas");
        assertThat(itemsOf(orderId)).isEmpty();
    }

    @Test
    void shouldSubmitDraftAndIgnoreResubmission() {
        Long orderId = createDraft("cliente@ejemplo.com");
        replaceLines(orderId, new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("patatas", 1)
        )));

        ResponseEntity<String> submitted = submit(orderId);
        ResponseEntity<String> retried = submit(orderId);

        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submitted.getBody()).contains("PENDING");
        assertThat(retried.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retried.getBody()).contains("PENDING");
    }

    @Test
    void shouldIgnoreEditsAfterSubmit() {
        Long orderId = createDraft("cliente@ejemplo.com");
        replaceLines(orderId, new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("patatas", 1)
        )));
        submit(orderId);

        ResponseEntity<String> response = replaceLines(orderId, new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("otro-producto", 9)
        )));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("PENDING");
        assertThat(itemsOf(orderId)).extracting(item -> item.getId().getProductId())
                .containsExactly("patatas");
        assertThat(itemsOf(orderId)).extracting(OrderItem::getQuantity)
                .containsExactly(1);
    }

    @Test
    void shouldRejectInvalidEmail() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("user", "password")
                .postForEntity("/orders/drafts", new CreateDraftRequest(""), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnNotFoundForUnknownOrder() {
        CreateOrderRequest request = new CreateOrderRequest(List.of(
                new CreateOrderItemRequest("patatas", 1)
        ));

        assertThat(replaceLines(Long.MAX_VALUE, request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(submit(Long.MAX_VALUE).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/orders/drafts",
                new CreateDraftRequest("cliente@ejemplo.com"),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Long createDraft(String email) {
        ResponseEntity<Order> response = restTemplate
                .withBasicAuth("user", "password")
                .postForEntity("/orders/drafts", new CreateDraftRequest(email), Order.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("DRAFT");
        return response.getBody().getId();
    }

    private ResponseEntity<String> replaceLines(Long orderId, CreateOrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate
                .withBasicAuth("user", "password")
                .exchange(
                        "/orders/" + orderId,
                        HttpMethod.PUT,
                        new HttpEntity<>(request, headers),
                        String.class
                );
    }

    private ResponseEntity<String> submit(Long orderId) {
        return restTemplate
                .withBasicAuth("user", "password")
                .postForEntity("/orders/" + orderId + "/submit", null, String.class);
    }

    private List<OrderItem> itemsOf(Long orderId) {
        return orderItemRepository.findAll().stream()
                .filter(item -> orderId.equals(item.getId().getOrderId()))
                .toList();
    }
}
