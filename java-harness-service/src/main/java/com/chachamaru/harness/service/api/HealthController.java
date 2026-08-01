package com.chachamaru.harness.service.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "harness-spring-service");
        return health;
    }

    @GetMapping("/ready")
    public Map<String, Object> ready() {
        Map<String, Object> status = new HashMap<>();
        status.put("ready", true);
        status.put("timestamp", LocalDateTime.now());
        return status;
    }
}
