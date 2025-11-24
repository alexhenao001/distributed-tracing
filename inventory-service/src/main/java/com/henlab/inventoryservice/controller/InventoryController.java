package com.henlab.inventoryservice.controller;

import com.henlab.inventoryservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> checkInventory(@PathVariable String orderId) {
        log.info("Checking inventory for orderId={}", orderId);
        
        Map<String, Object> result = inventoryService.checkInventory(orderId);
        
        log.info("Inventory check completed for orderId={}", orderId);
        return ResponseEntity.ok(result);
    }
}