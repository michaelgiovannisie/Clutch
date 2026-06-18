package com.zipcode.worldcuptracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zipcode.worldcuptracker.model.Match;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Pulls match lineups, statistics, and timeline events from api-football.com
 * (v3.football.api-sports.io). Currently dormant in practice: confirmed
 * directly against the live API that the free plan blocks season 2026 outright
 * ("Free plans do not have access to this season, try from 2022 to 2024"), so
 * every fixture/detail/venue request below returns zero results until/unless
 * the account is upgraded. ESPN's free hidden scoreboard endpoint
 * (EspnService) and Highlightly (HighlightlyService) cover what this would
 * have provided — timeline/statistics/venue and lineups respectively — so
 * this class is left in place rather than removed in case a paid plan is
 * added later, but isn't load-bearing for any current feature.
 *
 * The free plan is capped at 100 requests/day total (much tighter than
 * football-data.org's per-minute limit), so this service is deliberately
 * conservative: it maps fixtures to our Match rows once per run (1 request),
 * then pulls full detail (3 requests: lineups + statistics + events) for only
 * a small batch of newly finished matches per run.
 */
@Service
public class ApiFootballService {

    private static final Logger log = LoggerFactory.getLogger(ApiFootballService.class);

    private static final String BASE_URL = "https://v3.football.api-sports.io";
    private static final int LEAGUE_ID = 1; // FIFA World Cup
    private static final int SEASON = 2026;
    private static final int MAX_DETAIL_SYNCS_PER_RUN = 5;

    private final MatchService matchService;
    private final VenueService venueService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${api.apifootball.key:}")
    private String apiKey;

    /** Daily requests remaining as last reported by api-football.com, -1 if unknown. */
    private volatile int lastDailyRemaining = -1;

    public ApiFootballService(MatchService matchService, VenueService venueService) {
        this.matchService = matchService;
        this.venueService = venueService;
    }

    /**
     * Runs once shortly after startup, then every 6 hours. Cheap by design: the
     * mapping call is 1 request, and detail sync is capped at
     * MAX_DETAIL_SYNCS_PER_RUN * 3 requests per run, well within the 100/day quota.
     */
    @Scheduled(initialDelay = 60_000, fixedRate = 6 * 60 * 60 * 1000)
    public void scheduledSync() {
        log.info("Running scheduled api-football.com sync: {}", syncAll());
    }

    public String syncAll() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_APIFOOTBALL_KEY")) {
            return "Skipped sync: no api.apifootball.key configured in application.properties.";
        }
        int mapped = syncFixtureMapping();
        int detailed = syncMatchDetails();
        return "Mapped " + mapped + " fixtures and pulled details for " + detailed
                + " match(es) from api-football.com."
                + (lastDailyRemaining >= 0 ? " (" + lastDailyRemaining + " requests/day remaining)" : "");
    }

    /**
     * Pulls the full World Cup schedule (1 request) and links each fixture to an
     * existing Match row by kickoff date + team names, since the two providers
     * use unrelated ids. Also resolves venue here — football-data.org's free
     * tier sends no venue at all for World Cup matches, but api-football.com's
     * fixture object includes fixture.venue.name/city, so we piggyback on this
     * same request (no extra quota cost) to fill in match.venue when missing.
     */
    private int syncFixtureMapping() {
        try {
            JsonNode root = get("/fixtures?league=" + LEAGUE_ID + "&season=" + SEASON);
            JsonNode response = root.path("response");
            int count = 0;
            int venuesResolved = 0;
            for (JsonNode f : response) {
                long fixtureId = f.path("fixture").path("id").asLong();
                String homeName = f.path("teams").path("home").path("name").asText(null);
                String awayName = f.path("teams").path("away").path("name").asText(null);
                OffsetDateTime kickoff = parseDate(f.path("fixture").path("date").asText(null));
                if (homeName == null || awayName == null || kickoff == null) continue;

                Optional<Match> matchOpt = matchService.findByTeamsAndDate(homeName, awayName, kickoff);
                if (matchOpt.isEmpty()) continue;
                Match match = matchOpt.get();
                boolean changed = false;

                if (match.getApiFootballFixtureId() == null) {
                    match.setApiFootballFixtureId(fixtureId);
                    changed = true;
                    count++;
                }

                if (match.getVenue() == null) {
                    String venueName = f.path("fixture").path("venue").path("name").asText(null);
                    Optional<com.zipcode.worldcuptracker.model.Venue> venue = venueService.resolveByName(venueName);
                    if (venue.isPresent()) {
                        match.setVenue(venue.get());
                        changed = true;
                        venuesResolved++;
                    } else if (venueName != null && !venueName.isBlank()) {
                        log.warn("api-football.com sent venue \"{}\" for fixture {} but it didn't match any seeded venue",
                                venueName, fixtureId);
                    }
                }

                if (changed) {
                    matchService.save(match);
                }
            }
            log.info("api-football.com fixture sync: mapped {} fixture id(s), resolved {} venue(s)", count, venuesResolved);
            return count;
        } catch (Exception e) {
            log.warn("Failed to sync api-football.com fixture mapping: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * For a small batch of finished matches that already have a mapped fixture
     * id but no detail data yet, pulls lineups, statistics, and events and
     * stores the raw JSON arrays for the match-detail page to render client-side.
     */
    private int syncMatchDetails() {
        List<Match> pending = matchService.getFinishedMatchesNeedingApiFootballDetail(MAX_DETAIL_SYNCS_PER_RUN);
        int count = 0;
        for (Match match : pending) {
            try {
                Long fixtureId = match.getApiFootballFixtureId();
                JsonNode lineups = get("/fixtures/lineups?fixture=" + fixtureId).path("response");
                sleep();
                JsonNode statistics = get("/fixtures/statistics?fixture=" + fixtureId).path("response");
                sleep();
                JsonNode events = get("/fixtures/events?fixture=" + fixtureId).path("response");

                match.setLineupsJson(lineups.toString());
                match.setStatisticsJson(statistics.toString());
                match.setEventsJson(events.toString());
                matchService.save(match);
                count++;
            } catch (Exception e) {
                log.warn("Failed to sync api-football.com details for match {}: {}", match.getId(), e.getMessage());
            }
            sleep();
        }
        return count;
    }

    private void sleep() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private OffsetDateTime parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calls api-football.com, honoring its daily-quota header
     * (x-ratelimit-requests-remaining). Once that hits 0 we stop making calls
     * for the rest of the run rather than risk burning next month's reputation
     * with the provider on 429s.
     */
    private JsonNode get(String path) throws Exception {
        if (lastDailyRemaining == 0) {
            throw new RuntimeException("api-football.com daily request quota exhausted");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(8))
                .header("x-apisports-key", apiKey)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            Thread.sleep(5000);
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        response.headers().firstValue("x-ratelimit-requests-remaining")
                .map(this::parseIntSafely)
                .ifPresent(v -> lastDailyRemaining = v);

        if (response.statusCode() != 200) {
            throw new RuntimeException("api-football.com returned HTTP " + response.statusCode() + " for " + path);
        }
        return objectMapper.readTree(response.body());
    }

    private int parseIntSafely(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
