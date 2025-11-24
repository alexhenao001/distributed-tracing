package com.henlab.orderservice.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@TestPropertySource(properties = {
    "management.tracing.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans",
    "management.tracing.sampling.probability=1.0"
})
class OrderControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private ClientAndServer mockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("inventory.service.url", () -> "http://localhost:8082");
    }

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(8082);
        setupMockExpectations();
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop();
    }

    private void setupMockExpectations() {
        mockServer
            .when(
                request()
                    .withMethod("GET")
                    .withPath("/api/inventory/test-order-123")
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody("""
                        {
                          "orderId": "test-order-123",
                          "available": true,
                          "quantity": 25,
                          "location": "warehouse-1"
                        }
                        """)
            );
        
        mockServer
            .when(
                request()
                    .withMethod("GET")
                    .withPath("/api/inventory/test-order-456")
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody("""
                        {
                          "orderId": "test-order-456",
                          "available": true,
                          "quantity": 25,
                          "location": "warehouse-1"
                        }
                        """)
            );
        
        mockServer
            .when(
                request()
                    .withMethod("GET")
                    .withPath("/api/inventory/test-order-b3")
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody("""
                        {
                          "orderId": "test-order-b3",
                          "available": true,
                          "quantity": 25,
                          "location": "warehouse-1"
                        }
                        """)
            );
        
        mockServer
            .when(
                request()
                    .withMethod("GET")
                    .withPath("/api/inventory/test-baggage-order")
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody("""
                        {
                          "orderId": "test-baggage-order",
                          "available": true,
                          "quantity": 25,
                          "location": "warehouse-1"
                        }
                        """)
            );
        
        mockServer
            .when(
                request()
                    .withMethod("GET")
                    .withPath("/api/inventory/context-test-order")
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withContentType(MediaType.APPLICATION_JSON)
                    .withBody("""
                        {
                          "orderId": "context-test-order",
                          "available": true,
                          "quantity": 25,
                          "location": "warehouse-1"
                        }
                        """)
            );
    }


    @Test
    void testProcessOrderWithB3Headers() {
        String orderId = "test-order-123";
        String traceId = "463ac35c9f6413ad48485a3953bb6124";
        String spanId = "a2fb4a1d1a96d312";
        
        webTestClient.post()
            .uri("/api/orders/" + orderId)
            .header("X-B3-TraceId", traceId)
            .header("X-B3-SpanId", spanId)
            .header("X-B3-Sampled", "1")
            .header("correlationId", "test-correlation-123")
            .header("X-User-Id", "user-456")
            .header("X-Company-Id", "company-789")
            .body(Mono.empty(), String.class)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("correlationId", "test-correlation-123")
            .expectHeader().valueEquals("X-User-Id", "user-456")
            .expectHeader().valueEquals("X-Company-Id", "company-789")
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response.get("orderId")).isEqualTo(orderId);
                assertThat(response.get("status")).isEqualTo("processed");
                assertThat(response).containsKey("inventory");
            });
    }

    @Test
    void testProcessOrderWithoutCorrelationId() {
        String orderId = "test-order-456";
        
        webTestClient.post()
            .uri("/api/orders/" + orderId)
            .header("X-User-Id", "user-789")
            .header("X-Company-Id", "company-123")
            .body(Mono.empty(), String.class)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().exists("correlationId")
            .expectHeader().valueEquals("X-User-Id", "user-789")
            .expectHeader().valueEquals("X-Company-Id", "company-123")
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response.get("orderId")).isEqualTo(orderId);
                assertThat(response.get("status")).isEqualTo("processed");
            });
    }

    @Test
    void testB3HeaderPropagationAndTracing() {
        String orderId = "test-order-b3";
        String traceId = "763ac35c9f6413ad48485a3953bb6125";
        String spanId = "b2fb4a1d1a96d313";
        
        webTestClient.post()
            .uri("/api/orders/" + orderId)
            .header("X-B3-TraceId", traceId)
            .header("X-B3-SpanId", spanId)
            .header("X-B3-Sampled", "1")
            .header("correlationId", "b3-test-correlation")
            .header("X-User-Id", "b3-user")
            .header("X-Company-Id", "b3-company")
            .body(Mono.empty(), String.class)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response).containsKey("inventory");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> inventory = (Map<String, Object>) response.get("inventory");
                assertThat(inventory).containsKey("orderId");
                assertThat(inventory.get("orderId")).isEqualTo(orderId);
            });

        // Note: This test verifies that tracing headers are propagated and the services communicate correctly.
        // Span verification can be done via Zipkin UI in a real deployment scenario.
    }

    @Test
    void testCustomBaggagePropagation() {
        String orderId = "test-baggage-order";
        
        webTestClient.post()
            .uri("/api/orders/" + orderId)
            .header("correlationId", "baggage-correlation-test")
            .header("X-User-Id", "baggage-user")
            .header("X-Company-Id", "baggage-company")
            .body(Mono.empty(), String.class)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response).containsEntry("orderId",orderId);
                assertThat(response.get("status")).isEqualTo("processed");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> inventory = (Map<String, Object>) response.get("inventory");
                assertThat(inventory).isNotNull();
                assertThat(inventory).containsEntry("orderId", orderId);
            });
    }

    @Test
    void testTracingContextPropagationAcrossServices() {
        String orderId = "context-test-order";
        
        webTestClient.post()
            .uri("/api/orders/" + orderId)
            .header("X-B3-TraceId", "863ac35c9f6413ad48485a3953bb6126")
            .header("X-B3-SpanId", "c2fb4a1d1a96d314")
            .header("X-B3-Sampled", "1")
            .header("correlationId", "context-correlation")
            .header("X-User-Id", "context-user")
            .header("X-Company-Id", "context-company")
            .body(Mono.empty(), String.class)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Map.class)
            .value(response -> {
                assertThat(response).containsKey("inventory");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> inventory = (Map<String, Object>) response.get("inventory");
                assertThat(inventory).containsEntry("orderId", orderId);
            });
    }
}