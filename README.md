# Distributed Tracing Demo with B3 Propagation

A Spring Boot demonstration of distributed tracing using Micrometer Tracing with Brave and B3 propagation format. This project shows how to propagate B3 headers and custom baggage fields across microservices using RestTemplate.

## Project Structure

```
distributed-tracing-demo/
├── order-service/          # Service A (port 8080)
├── inventory-service/      # Service B (port 8081)
├── docker-compose.yml      # Zipkin setup
└── README.md
```

## Technologies

- **Spring Boot 3.5.8** with Spring Web and Actuator
- **Micrometer Tracing** with Brave bridge
- **B3 Propagation** (default with Brave)
- **Zipkin** for trace visualization
- **RestTemplate** for inter-service communication
- **WebTestClient** for integration testing

## Key Features

### B3 Propagation
- Automatically propagates B3 headers: `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-Sampled`
- Uses Brave's default B3 propagation format
- RestTemplate automatically adds tracing headers

### Custom Baggage Propagation
- **correlationId**: Generated if not present, propagated across services
- **X-User-Id**: Custom user identifier
- **X-Company-Id**: Custom company/tenant identifier

### Logging with Trace Context
- MDC logging shows traceId, spanId, and custom fields
- Log pattern includes all trace context information

## Quick Start

### 1. Start Zipkin
```bash
docker-compose up -d
```
Zipkin UI: http://localhost:9411

### 2. Build the Project
```bash
./mvnw clean compile
```

### 3. Run the Services

**Terminal 1 - Order Service:**
```bash
./mvnw spring-boot:run -pl order-service
```

**Terminal 2 - Inventory Service:**
```bash
./mvnw spring-boot:run -pl inventory-service
```

### 4. Test the Services

**Basic request (correlationId auto-generated):**
```bash
curl -X POST http://localhost:8080/api/orders/order-123 \
  -H "X-User-Id: user-456" \
  -H "X-Company-Id: company-789"
```

**Request with B3 headers:**
```bash
curl -X POST http://localhost:8080/api/orders/order-456 \
  -H "X-B3-TraceId: 463ac35c9f6413ad48485a3953bb6124" \
  -H "X-B3-SpanId: a2fb4a1d1a96d312" \
  -H "X-B3-Sampled: 1" \
  -H "correlationId: custom-correlation-123" \
  -H "X-User-Id: user-789" \
  -H "X-Company-Id: company-456"
```

**Request with custom correlationId:**
```bash
curl -X POST http://localhost:8080/api/orders/order-789 \
  -H "correlationId: my-custom-id" \
  -H "X-User-Id: user-123" \
  -H "X-Company-Id: company-abc"
```

## Running Tests

### Run All Tests
```bash
./mvnw test
```

### Run Specific Service Tests
```bash
# Order service tests
./mvnw test -pl order-service

# Inventory service tests  
./mvnw test -pl inventory-service
```

### Integration Tests Features
- Uses **WebTestClient** with **@AutoConfigureObservability**
- Verifies B3 header propagation
- Tests custom baggage propagation
- Asserts span creation and naming
- Validates trace context across service calls

## Key Implementation Details

### B3 Propagation Configuration
```properties
# application.properties (both services)
management.tracing.propagation.type=B3
management.tracing.sampling.probability=1.0

# Baggage propagation for custom headers
management.tracing.baggage.remote-fields[0]=correlationId
management.tracing.baggage.remote-fields[1]=X-User-Id
management.tracing.baggage.remote-fields[2]=X-Company-Id
```

### RestTemplate Configuration
RestTemplate is automatically configured with tracing interceptors by Spring Boot's auto-configuration:

```java
@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
}
```

### Servlet Filter for Custom Headers
Both services include a `TracingFilter` that:
- Extracts custom headers from requests
- Generates correlationId if missing
- Adds trace context to MDC for logging
- Propagates headers in responses

### Log Output Example
```
14:30:15.123 [http-nio-8080-exec-1] INFO  [463ac35c9f6413ad48485a3953bb6124,a2fb4a1d1a96d312] [correlationId=custom-correlation-123] [userId=user-789] [companyId=company-456] c.h.orderservice.controller.OrderController - Processing order request for orderId=order-456
14:30:15.145 [http-nio-8080-exec-1] INFO  [463ac35c9f6413ad48485a3953bb6124,b3fb4a1d1a96d313] [correlationId=custom-correlation-123] [userId=user-789] [companyId=company-456] c.h.orderservice.service.OrderService - Calling inventory service at URL: http://localhost:8081/api/inventory/order-456
```

## Endpoints

### Order Service (Port 8080)
- `POST /api/orders/{orderId}` - Process order, calls inventory service

### Inventory Service (Port 8081)  
- `GET /api/inventory/{orderId}` - Check inventory status

### Actuator Endpoints (Both Services)
- `/actuator/health` - Health check
- `/actuator/tracing` - Trace information
- `/actuator/metrics` - Metrics

## Testing B3 Headers

The integration tests verify:

1. **B3 Header Propagation**: X-B3-TraceId, X-B3-SpanId, X-B3-Sampled headers are correctly propagated
2. **Custom Baggage**: correlationId, X-User-Id, X-Company-Id are propagated via baggage
3. **Span Creation**: Proper spans are created for HTTP requests and inter-service calls
4. **Trace Context**: MDC logging includes all trace context fields

### Example Test Assertions
```java
@Test
void testProcessOrderWithB3Headers() {
    webTestClient.post()
        .uri("/api/orders/test-order-123")
        .header("X-B3-TraceId", "463ac35c9f6413ad48485a3953bb6124")
        .header("X-B3-SpanId", "a2fb4a1d1a96d312")
        .header("X-B3-Sampled", "1")
        .header("correlationId", "test-correlation-123")
        .header("X-User-Id", "user-456")
        .header("X-Company-Id", "company-789")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().valueEquals("correlationId", "test-correlation-123");
}
```

## Viewing Traces

1. Navigate to Zipkin UI: http://localhost:9411
2. Click "Find Traces" 
3. View distributed traces showing:
   - Service dependencies
   - Span timing and relationships
   - B3 trace and span IDs
   - Custom baggage fields

## Architecture Notes

- **Service A (order-service)** receives requests and calls Service B using RestTemplate
- **Service B (inventory-service)** processes inventory checks
- **RestTemplate** automatically propagates B3 headers and custom baggage
- **Servlet filters** handle custom header extraction and MDC population
- **B3 propagation** is the default format used by Brave
- **Baggage fields** carry custom business context across service boundaries