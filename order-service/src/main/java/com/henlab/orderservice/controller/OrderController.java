package com.henlab.orderservice.controller;

import com.henlab.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> processOrder(@PathVariable String orderId) {
        log.info("Processing order request for orderId={}", orderId);
        
        Map<String, Object> result = orderService.processOrder(orderId);
        
        log.info("Order processing completed for orderId={}", orderId);
        return ResponseEntity.ok(result);
    }
}