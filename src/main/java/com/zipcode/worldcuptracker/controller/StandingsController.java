package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.model.TeamStanding;
import com.zipcode.worldcuptracker.service.StandingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    /** GET /standings — group tables for every group with teams assigned. */
    @GetMapping("/standings")
    public String standings(Model model) {
        Map<String, List<TeamStanding>> byGroup = new LinkedHashMap<>();
        for (String group : standingsService.getGroups()) {
            byGroup.put(group, standingsService.getGroupStandings(group));
        }
        model.addAttribute("standingsByGroup", byGroup);
        return "standings/standings";
    }

    /** GET /api/standings/{group} */
    @GetMapping("/api/standings/{group}")
    @ResponseBody
    public ResponseEntity<List<TeamStanding>> apiGroupStandings(@PathVariable String group) {
        return ResponseEntity.ok(standingsService.getGroupStandings(group));
    }
}
