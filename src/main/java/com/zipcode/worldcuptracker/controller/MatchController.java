package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.model.Commentary;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.service.ApiFootballService;
import com.zipcode.worldcuptracker.service.CommentaryService;
import com.zipcode.worldcuptracker.service.EspnService;
import com.zipcode.worldcuptracker.service.HighlightlyService;
import com.zipcode.worldcuptracker.service.MatchService;
import com.zipcode.worldcuptracker.service.SportsApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class MatchController {

    private final MatchService matchService;
    private final SportsApiService sportsApiService;
    private final ApiFootballService apiFootballService;
    private final EspnService espnService;
    private final HighlightlyService highlightlyService;
    private final CommentaryService commentaryService;

    public MatchController(MatchService matchService, SportsApiService sportsApiService,
                            ApiFootballService apiFootballService, EspnService espnService,
                            HighlightlyService highlightlyService, CommentaryService commentaryService) {
        this.matchService = matchService;
        this.sportsApiService = sportsApiService;
        this.apiFootballService = apiFootballService;
        this.espnService = espnService;
        this.highlightlyService = highlightlyService;
        this.commentaryService = commentaryService;
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

    /**
     * Re-syncs just the matches list (teams + venues + scores/status), skipping
     * the slow per-team squad sync. Returns in a couple seconds instead of
     * several minutes — use this to verify a matches-only fix (e.g. venue
     * resolution) without waiting on the full sync.
     * POST /api/sync/football-data/matches
     */
    @PostMapping("/api/sync/football-data/matches")
    @ResponseBody
    public ResponseEntity<String> syncFootballDataMatchesOnly() {
        return ResponseEntity.ok(sportsApiService.syncMatchesOnly());
    }

    /**
     * Manually triggers an ESPN sync (timeline + statistics for live/finished
     * matches missing that data). ESPN's hidden API is free with no documented
     * quota, but this is still manual rather than purely automatic so it can be
     * run on demand right after a match finishes.
     * POST /api/sync/espn
     */
    @PostMapping("/api/sync/espn")
    @ResponseBody
    public ResponseEntity<String> syncEspn() {
        return ResponseEntity.ok(espnService.syncAll());
    }

    /**
     * GET /api/matches/{id}/lineups — raw JSON array from api-football.com's
     * /fixtures/lineups, or "[]" if not synced yet.
     */
    @GetMapping(value = "/api/matches/{id}/lineups", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> apiMatchLineups(@PathVariable Long id) {
        return matchService.getMatchById(id)
                .map(m -> ResponseEntity.ok(m.getLineupsJson() != null ? m.getLineupsJson() : "[]"))
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/matches/{id}/statistics — raw JSON array from api-football.com's /fixtures/statistics. */
    @GetMapping(value = "/api/matches/{id}/statistics", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> apiMatchStatistics(@PathVariable Long id) {
        return matchService.getMatchById(id)
                .map(m -> ResponseEntity.ok(m.getStatisticsJson() != null ? m.getStatisticsJson() : "[]"))
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/matches/{id}/events — raw JSON array from api-football.com's /fixtures/events (the timeline). */
    @GetMapping(value = "/api/matches/{id}/events", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> apiMatchEvents(@PathVariable Long id) {
        return matchService.getMatchById(id)
                .map(m -> ResponseEntity.ok(m.getEventsJson() != null ? m.getEventsJson() : "[]"))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Manually triggers an api-football.com sync (fixture mapping + lineups/stats/events
     * for a small batch of finished matches). Free tier is capped at 100 requests/day,
     * so this is meant to be called sparingly.
     * POST /api/sync/api-football
     */
    @PostMapping("/api/sync/api-football")
    @ResponseBody
    public ResponseEntity<String> syncApiFootball() {
        return ResponseEntity.ok(apiFootballService.syncAll());
    }

    /**
     * Manually triggers a Highlightly sync (match-id mapping + starting
     * lineups/formations). Free tier is capped at 100 requests/day, so this
     * is meant to be called sparingly.
     * POST /api/sync/highlightly
     * POST /api/sync/highlightly?force=true — re-fetches lineups even for
     * matches that already have lineupsJson stored, needed after a
     * normalization fix (e.g. the coach field) so previously-cached matches
     * pick up the fix instead of being skipped forever.
     */
    @PostMapping("/api/sync/highlightly")
    @ResponseBody
    public ResponseEntity<String> syncHighlightly(@RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(highlightlyService.syncAll(force));
    }

    /**
     * GET /api/matches/{id}/commentary — returns cached AI tactical commentary
     * for this match, generating it on first request (via Gemini) and caching
     * it from then on. Generation happens lazily/synchronously here rather
     * than via a scheduled sync, since it only needs to happen once per match
     * and there's no benefit to pre-generating it for matches nobody's viewing.
     */
    @GetMapping(value = "/api/matches/{id}/commentary", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiMatchCommentary(
            @PathVariable Long id, @RequestParam(defaultValue = "false") boolean regenerate) {
        return matchService.getMatchById(id)
                .map(match -> {
                    try {
                        Commentary commentary = commentaryService.getOrGenerate(match, regenerate);
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "content", commentary.getContent(),
                                "generatedAt", commentary.getGeneratedAt()));
                    } catch (IllegalStateException e) {
                        return ResponseEntity.ok(Map.<String, Object>of("error", e.getMessage()));
                    } catch (RuntimeException e) {
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "error", "Commentary isn't available right now — try again shortly."));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
