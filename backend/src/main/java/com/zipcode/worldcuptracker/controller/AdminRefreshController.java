package com.zipcode.worldcuptracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zipcode.worldcuptracker.service.FootballDataImportService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminRefreshController {

    private final FootballDataImportService footballDataImportService;

    public AdminRefreshController(FootballDataImportService footballDataImportService) {
        this.footballDataImportService = footballDataImportService;
    }

    @PostMapping("/refresh/static")
    public ResponseEntity<String> refreshStaticData() {
        String message = footballDataImportService.refreshStaticData();
        return ResponseEntity.ok(message);
    }       

    @PostMapping("/refresh/results")
    public ResponseEntity<String> refreshResultsOnly() {
        String message = footballDataImportService.refreshResultsOnly();
        return ResponseEntity.ok(message);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshData() {
        String message = footballDataImportService.refreshStaticData();
        return ResponseEntity.ok(message);
    }

    @PostMapping("/refresh/rosters")
    public ResponseEntity<String> refreshRosters() {
        String message = footballDataImportService.refreshRosters();
        return ResponseEntity.ok(message);
    }

    @PostMapping("/refresh/groups")
    public ResponseEntity<String> refreshGroups() {
    String message = footballDataImportService.refreshGroups();
    return ResponseEntity.ok(message);
    }

    //admin/dev-only
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearData() {
        footballDataImportService.clearAllImportedData();
        return ResponseEntity.ok("Database cleared");
    }
}
