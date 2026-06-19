package com.zipcode.worldcuptracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * World Cup Tracker — Spring Boot REST API entry point.
 *
 * Architecture skeleton (PLAN.md §1): this app is a pure JSON REST backend
 * consumed by the separate React SPA in ../frontend. No Thymeleaf / server-side views.
 */
@SpringBootApplication
public class WorldCupTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldCupTrackerApplication.class, args);
    }
}
