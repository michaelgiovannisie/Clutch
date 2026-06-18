package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.service.MatchService;
import com.zipcode.worldcuptracker.service.SportsApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class MatchController {

    private final MatchService matchService;
    private final SportsApiService sportsApiService;

    public MatchController(MatchService matchService, SportsApiService sportsApiService) {
        this.matchService = matchService;
        this.sportsApiService = sportsApiService;
    }

    // ── Thymeleaf pages ───────────────────────────────────────────────────────

    /** GET /matches */
    @GetMapping("/matches")
    public String matchList(Model model) {
        model.addAttribute("matches", matchService.getAllMatches());
        return "matches/match-list";
    }

    /** GET /matches/{id} */
    @GetMapping("/matches/{id}")
    public String matchDetail(@PathVariable Long id, Model model) {
        return matchService.getMatchById(id)
                .map(match -> {
                    model.addAttribute("match", match);
                    return "matches/match-detail";
                })
                .orElse("redirect:/matches");
    }

    // ── REST endpoints ────────────────────────────────────────────────────────

    /** GET /api/matches */
    @GetMapping("/api/matches")
    @ResponseBody
    public ResponseEntity<List<Match>> apiMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    /** GET /api/matches/{id} */
    @GetMapping("/api/matches/{id}")
    @ResponseBody
    public ResponseEntity<Match> apiMatchById(@PathVariable Long id) {
        return matchService.getMatchById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/venues/{venueId}/matches — used by the venue detail page's Match Schedule card */
    @GetMapping("/api/venues/{venueId}/matches")
    @ResponseBody
    public ResponseEntity<List<Match>> apiMatchesByVenue(@PathVariable Long venueId) {
        return ResponseEntity.ok(matchService.getMatchesByVenue(venueId));
    }

    /**
     * Manually triggers a pull of teams + matches from football-data.org.
     * Deliberately not automatic/scheduled — the free tier is rate-limited, so this
     * is meant to be called sparingly (e.g. once a day) rather than on every request.
     * POST /api/sync/football-data
     */
    @PostMapping("/api/sync/football-data")
    @ResponseBody
    public ResponseEntity<String> syncFootballData() {
        return ResponseEntity.ok(sportsApiService.syncAll());
    }
}
