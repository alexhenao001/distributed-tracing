package com.henlab.orderservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final RestTemplate restTemplate;
    
    @Value("${inventory.service.url:http://localhost:8081}")
    private String inventoryServiceUrl;

    public OrderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> processOrder(String orderId) {
        log.info("Starting order processing for orderId={}", orderId);

        Map<String, Object> inventoryResponse = checkInventory(orderId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("status", "processed");
        result.put("inventory", inventoryResponse);
        
        log.info("Order processing completed for orderId={}", orderId);
        return result;
    }

    private Map<String, Object> checkInventory(String orderId) {
        log.info("Checking inventory for orderId={}", orderId);
        
        HttpHeaders headers = new HttpHeaders();
        String correlationId = MDC.get("correlationId");
        String userId = MDC.get("userId");
        String companyId = MDC.get("companyId");
        
        if (correlationId != null) {
            headers.set("correlationId", correlationId);
        }
        if (userId != null) {
            headers.set("X-User-Id", userId);
        }
        if (companyId != null) {
            headers.set("X-Company-Id", companyId);
        }
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = inventoryServiceUrl + "/api/inventory/" + orderId;
        
        log.info("Calling inventory service at URL: {}", url);
        
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            
            log.info("Received response from inventory service for orderId={}", orderId);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling inventory service for orderId={}: {}", orderId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unable to check inventory");
            errorResponse.put("message", e.getMessage());
            return errorResponse;
        }
    }
}