package com.henlab.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "inventory.service.url=http://localhost:8081",
    "management.tracing.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans"
})
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}