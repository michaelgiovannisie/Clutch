package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.service.BracketService;
import com.zipcode.worldcuptracker.service.StandingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StandingsController {

    private final StandingsService standingsService;
    private final BracketService bracketService;

    public StandingsController(StandingsService standingsService, BracketService bracketService) {
        this.standingsService = standingsService;
        this.bracketService = bracketService;
    }

    @GetMapping("/standings")
    public String standings(Model model) {
        model.addAttribute("groups",             standingsService.getRankedGroups());
        model.addAttribute("thirdPlaceRace",     standingsService.getRankedThirds());
        model.addAttribute("projectedWinners",   standingsService.getGroupWinners());
        model.addAttribute("projectedRunnersUp", standingsService.getGroupRunnersUp());
        model.addAttribute("projectedBestThirds",standingsService.getBestThirds(8));
        model.addAttribute("projectedR32",       bracketService.getProjectedR32());
        return "standings/standings";
    }
}
