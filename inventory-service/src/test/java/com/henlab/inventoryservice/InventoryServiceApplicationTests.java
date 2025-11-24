package com.henlab.inventoryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "management.tracing.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans"
})
class InventoryServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}