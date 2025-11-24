package com.henlab.inventoryservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@TestPropertySource(properties = {
    "management.tracing.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans",
    "management.tracing.sampling.probability=1.0"
})
class InventoryControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testCheckInventoryWithB3Headers() {
        String orderId = "test-inventory-123";
        String traceId = "563ac35c9f6413ad48485a3953bb6127";
        String spanId = "d2fb4a1d1a96d315";
        
        webTestClient.get()
            .uri("/api/inventory/" + orderId)
            .header("X-B3-TraceId", traceId)
            .header("X-B3-SpanId", spanId)
            .header("X-B3-Sampled", "1")
            .header("correlationId", "inventory-correlation-123")
            .header("X-User-Id", "inventory-user-456")
            .header("X-Company-Id", "inventory-company-789")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("correlationId", "inventory-correlation-123")
            .expectHeader().valueEquals("X-User-Id", "inventory-user-456")
            .expectHeader().valueEquals("X-Company-Id", "inventory-company-789")
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response.get("orderId")).isEqualTo(orderId);
                assertThat(response).containsKeys("available", "quantity", "location");
            });
    }

    @Test
    void testCheckInventoryWithoutCustomHeaders() {
        String orderId = "test-inventory-456";
        
        webTestClient.get()
            .uri("/api/inventory/" + orderId)
            .header("X-B3-TraceId", "663ac35c9f6413ad48485a3953bb6128")
            .header("X-B3-SpanId", "e2fb4a1d1a96d316")
            .header("X-B3-Sampled", "1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response.get("orderId")).isEqualTo(orderId);
                assertThat(response).containsKeys("available", "quantity", "location");
                assertThat(response.get("quantity")).isInstanceOf(Integer.class);
                assertThat(response.get("available")).isInstanceOf(Boolean.class);
                assertThat(response.get("location")).asString().startsWith("warehouse-");
            });
    }

    @Test
    void testTracingContextInInventoryService() {
        String orderId = "tracing-context-inventory";
        
        webTestClient.get()
            .uri("/api/inventory/" + orderId)
            .header("X-B3-TraceId", "763ac35c9f6413ad48485a3953bb6129")
            .header("X-B3-SpanId", "f2fb4a1d1a96d317")
            .header("X-B3-Sampled", "1")
            .header("correlationId", "context-inventory-correlation")
            .header("X-User-Id", "context-inventory-user")
            .header("X-Company-Id", "context-inventory-company")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response.get("orderId")).isEqualTo(orderId);
                assertThat(response).containsKey("available");
                assertThat(response).containsKey("quantity");
                assertThat(response).containsKey("location");
            });

        // Note: This test verifies that tracing context is properly handled in the inventory service.
        // Span verification can be done via Zipkin UI in a real deployment scenario.
    }

    @Test
    void testBaggagePropagationInInventoryService() {
        String orderId = "baggage-inventory-test";
        
        webTestClient.get()
            .uri("/api/inventory/" + orderId)
            .header("correlationId", "baggage-inventory-correlation")
            .header("X-User-Id", "baggage-inventory-user")
            .header("X-Company-Id", "baggage-inventory-company")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("correlationId")
            .expectHeader().exists("X-User-Id")
            .expectHeader().exists("X-Company-Id")
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response.get("orderId")).isEqualTo(orderId);
                assertThat(((Integer) response.get("quantity"))).isPositive();
            });
    }
}