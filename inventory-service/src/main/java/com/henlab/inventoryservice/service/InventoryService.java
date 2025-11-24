package com.henlab.inventoryservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final Random random = new Random();

    public Map<String, Object> checkInventory(String orderId) {
        log.info("Processing inventory check for orderId={}", orderId);

        try {
            Thread.sleep(100L + random.nextInt(200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("available", random.nextBoolean());
        result.put("quantity", random.nextInt(100) + 1);
        result.put("location", "warehouse-" + (random.nextInt(3) + 1));
        
        log.info("Inventory check completed for orderId={} - available: {}", 
                orderId, result.get("available"));
        
        return result;
    }
}