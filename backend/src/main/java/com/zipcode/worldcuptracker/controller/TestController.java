package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.service.ApiFootballService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final ApiFootballService apiFootballService;

    public TestController(ApiFootballService apiFootballService) {
        this.apiFootballService = apiFootballService;
    }

    @GetMapping("/teams")
    public Object teams() {
        return apiFootballService.getTeams();
    }

    @GetMapping("/fixtures")
    public Object fixtures() {
        return apiFootballService.getFixtures();
    }
}
