package com.zipcode.worldcuptracker.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal liveness endpoint so the API contract is callable from day one and
 * the React app can verify backend connectivity. Feature endpoints are added
 * per the PLAN.md §2.2 contract by each slice owner.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "service", "world-cup-tracker"
        );
    }
}
