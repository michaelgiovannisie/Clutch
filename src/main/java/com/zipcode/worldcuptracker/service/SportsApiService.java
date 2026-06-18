package com.zipcode.worldcuptracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.Player;
import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.model.Venue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Pulls World Cup team and match data from football-data.org (v4) and syncs it
 * into the local database.
 *
 * football-data.org's free tier is rate-limited (roughly 10 requests/minute), so
 * this service is meant to be triggered on demand (e.g. an admin "sync" button or
 * a low-frequency scheduled job) rather than called on every page load. Match and
 * team data already pulled into Postgres is what powers the rest of the app.
 */
@Service
public class SportsApiService {

    private static final Logger log = LoggerFactory.getLogger(SportsApiService.class);

    private static final String BASE_URL = "https://api.football-data.org/v4";
    private static final String COMPETITION_CODE = "WC"; // FIFA World Cup

    private final TeamService teamService;
    private final MatchService matchService;
    private final PlayerService playerService;
    private final VenueService venueService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${api.football.key:}")
    private String apiKey;

    /** Tracks the last "X-Requests-Available-Minute" value football-data.org reported, -1 if unknown. */
    private volatile int lastRequestsAvailable = -1;

    public SportsApiService(TeamService teamService, MatchService matchService, PlayerService playerService,
                             VenueService venueService) {
        this.teamService = teamService;
        this.matchService = matchService;
        this.playerService = playerService;
        this.venueService = venueService;
    }

    /**
     * Runs the sync automatically: once shortly after startup, then every 12 hours.
     * Rosters and groups barely change and match status/scores only update a few
     * times a day, so this keeps the DB fresh without coming close to the free
     * tier's rate limit. Manual syncs via POST /api/sync/football-data still work
     * too (e.g. right after a matchday).
     */
    @Scheduled(initialDelay = 30_000, fixedRate = 12 * 60 * 60 * 1000)
    public void scheduledSync() {
        log.info("Running scheduled football-data.org sync: {}", syncAll());
    }

    /**
     * Pulls teams, then matches, from football-data.org and upserts them into
     * the database. Returns a short summary of how many of each were synced.
     */
    public String syncAll() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_SPORTS_API_KEY")) {
            return "Skipped sync: no api.football.key configured in application.properties.";
        }
        int teamsSynced = syncTeams();
        int matchesSynced = syncMatches();
        int playersSynced = syncSquads();
        return "Synced " + teamsSynced + " teams, " + matchesSynced + " matches, and " + playersSynced
                + " players from football-data.org."
                + (lastRequestsAvailable >= 0 ? " (" + lastRequestsAvailable + " requests/min remaining)" : "");
    }

    /**
     * Re-syncs just the matches list (2 requests: this method plus the underlying
     * call) — skips the teams and squads steps, which is what makes the full
     * syncAll() take several minutes (one request per team, 6s apart, to respect
     * the free-tier rate limit). Useful for quickly picking up a matches-only
     * change like a venue-resolution fix without waiting on the full cycle.
     */
    public String syncMatchesOnly() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_SPORTS_API_KEY")) {
            return "Skipped sync: no api.football.key configured in application.properties.";
        }
        int matchesSynced = syncMatches();
        return "Synced " + matchesSynced + " matches from football-data.org."
                + (lastRequestsAvailable >= 0 ? " (" + lastRequestsAvailable + " requests/min remaining)" : "");
    }

    private int syncTeams() {
        try {
            JsonNode root = get("/competitions/" + COMPETITION_CODE + "/teams");
            JsonNode teams = root.path("teams");
            int count = 0;
            for (JsonNode t : teams) {
                Team team = Team.builder()
                        .externalId(t.path("id").asLong())
                        .name(t.path("name").asText(null))
                        .tla(t.path("tla").asText(null))
                        .crestUrl(t.path("crest").asText(null))
                        .flag(t.path("area").path("flag").asText(null))
                        .build();
                teamService.upsertTeam(team);
                count++;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private int syncMatches() {
        try {
            JsonNode root = get("/competitions/" + COMPETITION_CODE + "/matches");
            JsonNode matches = root.path("matches");
            int count = 0;
            for (JsonNode m : matches) {
                long externalId = m.path("id").asLong();

                Optional<Team> home = teamService.getTeamByExternalId(m.path("homeTeam").path("id").asLong());
                Optional<Team> away = teamService.getTeamByExternalId(m.path("awayTeam").path("id").asLong());

                Match match = matchService.getMatchByExternalId(externalId).orElseGet(Match::new);
                match.setExternalId(externalId);
                match.setUtcDate(parseDate(m.path("utcDate").asText(null)));
                match.setStatus(m.path("status").asText(null));
                match.setMatchday(m.path("matchday").isInt() ? m.path("matchday").asInt() : null);
                match.setStage(m.path("stage").asText(null));
                match.setGroupName(extractGroupLetter(m.path("group").asText(null)));
                match.setHomeTeam(home.orElse(null));
                match.setAwayTeam(away.orElse(null));
                // football-data.org's free tier sends no venue for World Cup matches, so
                // don't clobber a venue that ApiFootballService already resolved and set.
                Venue resolvedVenue = resolveVenue(m.path("venue").asText(null));
                if (resolvedVenue != null) {
                    match.setVenue(resolvedVenue);
                }

                JsonNode fullTime = m.path("score").path("fullTime");
                match.setHomeScore(fullTime.path("home").isInt() ? fullTime.path("home").asInt() : null);
                match.setAwayScore(fullTime.path("away").isInt() ? fullTime.path("away").asInt() : null);
                match.setWinner(m.path("score").path("winner").asText(null));

                matchService.save(match);
                count++;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Pulls each team's squad from football-data.org (only available per-team, not
     * via the competition/teams list) and upserts it as Player rows. This makes one
     * request per team, so we space calls ~6 seconds apart to stay well under the
     * free tier's 10 requests/minute limit even though syncTeams()/syncMatches()
     * already used a couple of requests in this same run.
     */
    private int syncSquads() {
        int count = 0;
        List<Team> teams = teamService.getAllTeams();
        for (Team team : teams) {
            if (team.getExternalId() == null) {
                continue;
            }
            try {
                JsonNode root = get("/teams/" + team.getExternalId());
                JsonNode squad = root.path("squad");
                for (JsonNode p : squad) {
                    Player player = Player.builder()
                            .externalId(p.path("id").asLong())
                            .name(p.path("name").asText(null))
                            .position(p.path("position").asText(null))
                            .nationality(p.path("nationality").asText(null))
                            .dateOfBirth(parseLocalDate(p.path("dateOfBirth").asText(null)))
                            .shirtNumber(p.path("shirtNumber").isInt() ? p.path("shirtNumber").asInt() : null)
                            .team(team)
                            .build();
                    playerService.upsertPlayer(player);
                    count++;
                }
            } catch (Exception e) {
                log.warn("Failed to sync squad for team {}: {}", team.getName(), e.getMessage());
            }

            try {
                Thread.sleep(6000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return count;
    }

    /** football-data.org returns groups like "GROUP_A" — we just want "A". */
    private String extractGroupLetter(String group) {
        if (group == null || group.isBlank()) return null;
        int idx = group.lastIndexOf('_');
        return idx >= 0 ? group.substring(idx + 1) : group;
    }

    /**
     * football-data.org's free tier sends a null/blank venue for World Cup
     * matches (confirmed directly against the raw API response — their docs
     * imply it's always a populated plain string, but in practice it isn't
     * for this competition/tier). When it does send a name, delegate to the
     * shared VenueService matcher (handles FIFA's debranded stadium names).
     * api-football.com (ApiFootballService) is the provider that actually
     * supplies venue data for us; this is just a defensive fallback.
     */
    private Venue resolveVenue(String venueName) {
        if (venueName == null || venueName.isBlank()) {
            return null;
        }
        return venueService.resolveByName(venueName).orElse(null);
    }

    private OffsetDateTime parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseLocalDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return LocalDate.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calls football-data.org, honoring its throttling headers as recommended in
     * their onboarding docs: X-Requests-Available-Minute (requests left in the
     * current window) and X-RequestCounter-Reset (seconds until that window
     * resets). If we're down to our last request, or the server replies 429, we
     * wait out the reset window and retry once rather than hammering the limiter.
     */
    private JsonNode get(String path) throws Exception {
        // Proactively pause if the previous call told us we're nearly out of budget.
        if (lastRequestsAvailable == 0) {
            Thread.sleep(6000);
        }

        HttpResponse<String> response = sendRequest(path);

        if (response.statusCode() == 429) {
            long waitMs = retryAfterMillis(response);
            Thread.sleep(waitMs);
            response = sendRequest(path);
        }

        recordRateLimitHeaders(response);

        if (response.statusCode() != 200) {
            throw new RuntimeException("football-data.org returned HTTP " + response.statusCode() + " for " + path);
        }
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> sendRequest(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(8))
                .header("X-Auth-Token", apiKey)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void recordRateLimitHeaders(HttpResponse<String> response) {
        response.headers().firstValue("X-Requests-Available-Minute")
                .map(this::parseIntSafely)
                .ifPresent(v -> lastRequestsAvailable = v);
    }

    private long retryAfterMillis(HttpResponse<String> response) {
        // football-data.org's own header takes priority; fall back to standard Retry-After.
        return response.headers().firstValue("X-RequestCounter-Reset")
                .map(this::parseIntSafely)
                .filter(s -> s > 0)
                .map(s -> s * 1000L)
                .orElseGet(() -> response.headers().firstValue("Retry-After")
                        .map(this::parseIntSafely)
                        .filter(s -> s > 0)
                        .map(s -> s * 1000L)
                        .orElse(10_000L));
    }

    private int parseIntSafely(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
