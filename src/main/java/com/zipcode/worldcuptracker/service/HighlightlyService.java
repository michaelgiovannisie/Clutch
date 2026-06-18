package com.zipcode.worldcuptracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.Team;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Pulls starting lineups/formations from the Highlightly Football API
 * (soccer.highlightly.net) — the one piece of match detail neither
 * api-football.com (free tier excludes season 2026) nor ESPN's hidden
 * scoreboard endpoint (doesn't expose starting XIs at all) can supply for
 * FIFA World Cup 2026. Free "Basic" plan: 100 requests/day, no card required,
 * lineups explicitly included. FIFA World Cup is league id 1635 there, with
 * season 2026 listed and 72 real, current matches confirmed live.
 *
 * Output is normalized into the same shape api-football.com used
 * ([{team:{name}, formation, startXI:[{player:{name,number}}]}, ...])
 * so match-detail.html's existing lineup-rendering JS needs no changes.
 * Note: Highlightly's /lineups response has no coach field (confirmed via
 * their official API docs), so this is the one piece api-football.com's
 * shape included that we simply can't supply from any current data source.
 */
@Service
public class HighlightlyService {

    private static final Logger log = LoggerFactory.getLogger(HighlightlyService.class);

    private static final String BASE_URL = "https://soccer.highlightly.net";
    private static final String RAPIDAPI_HOST = "football-highlights-api.p.rapidapi.com";
    private static final int LEAGUE_ID = 1635; // FIFA World Cup
    private static final int SEASON = 2026;
    private static final int MAX_DETAIL_SYNCS_PER_RUN = 20;

    private final MatchService matchService;
    private final PlayerService playerService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${api.highlightly.key:}")
    private String apiKey;

    public HighlightlyService(MatchService matchService, PlayerService playerService) {
        this.matchService = matchService;
        this.playerService = playerService;
    }

    /**
     * Runs once shortly after startup, then every 6 hours. Cheap by design:
     * the mapping call is 1 request, and detail sync is capped at
     * MAX_DETAIL_SYNCS_PER_RUN requests per run, well within the 100/day quota.
     */
    @Scheduled(initialDelay = 75_000, fixedRate = 6 * 60 * 60 * 1000)
    public void scheduledSync() {
        log.info("Running scheduled Highlightly sync: {}", syncAll());
    }

    public String syncAll() {
        return syncAll(false);
    }

    /**
     * @param force if true, re-fetches lineups for every mapped, finished match
     *              even if lineupsJson is already stored — needed to pick up
     *              normalization fixes (e.g. the coach field) for matches whose
     *              lineup was already cached under the old, incomplete shape.
     *              Regular scheduled/manual syncs should leave this false so we
     *              don't burn the 100/day quota re-fetching unchanged data.
     */
    public String syncAll(boolean force) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_HIGHLIGHTLY_KEY")) {
            return "Skipped sync: no api.highlightly.key configured in application.properties.";
        }
        int mapped = syncMatchMapping();
        int detailed = syncLineups(force);
        return "Mapped " + mapped + " match(es) and pulled lineups for " + detailed
                + " match(es) from Highlightly.";
    }

    /**
     * Pulls the full World Cup schedule (1 request) and links each match to an
     * existing Match row by kickoff date + team names, since Highlightly and
     * football-data.org use unrelated ids.
     */
    private int syncMatchMapping() {
        try {
            JsonNode data = get("/matches?leagueId=" + LEAGUE_ID + "&season=" + SEASON).path("data");
            int count = 0;
            for (JsonNode m : data) {
                long highlightlyId = m.path("id").asLong();
                String homeName = m.path("homeTeam").path("name").asText(null);
                String awayName = m.path("awayTeam").path("name").asText(null);
                OffsetDateTime kickoff = parseDate(m.path("date").asText(null));
                if (homeName == null || awayName == null || kickoff == null) continue;

                Optional<Match> match = matchService.findByTeamsAndDate(homeName, awayName, kickoff);
                if (match.isPresent() && match.get().getHighlightlyMatchId() == null) {
                    match.get().setHighlightlyMatchId(highlightlyId);
                    matchService.save(match.get());
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.warn("Failed to sync Highlightly match mapping: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * For a small batch of matches that already have a mapped Highlightly id
     * but no lineup data yet, pulls /lineups/{id} and normalizes it into the
     * same shape api-football.com used for the match-detail page to render.
     *
     * The MAX_DETAIL_SYNCS_PER_RUN cap only applies to the regular
     * (non-force) scheduled path, since that one runs automatically every 6
     * hours forever and needs to stay well under the 100/day quota with
     * margin to spare. A force resync is a one-off the user triggers
     * deliberately, capped only by the actual number of finished, mapped
     * matches (at most ~104 for the whole tournament) — comfortably within
     * a single day's quota on its own.
     */
    private int syncLineups(boolean force) {
        List<Match> pending = force
                ? matchService.getMatchesWithHighlightlyLineupMapped(Integer.MAX_VALUE)
                : matchService.getMatchesNeedingHighlightlyLineup(MAX_DETAIL_SYNCS_PER_RUN);
        int count = 0;
        for (Match match : pending) {
            try {
                JsonNode lineups = get("/lineups/" + match.getHighlightlyMatchId());
                ArrayNode normalized = normalize(lineups, match);
                if (normalized.size() < 2) continue; // not yet announced (e.g. pre-kickoff)

                match.setLineupsJson(normalized.toString());
                matchService.save(match);
                backfillShirtNumbers(match, normalized);
                count++;
            } catch (Exception e) {
                log.warn("Failed to sync Highlightly lineups for match {}: {}", match.getId(), e.getMessage());
            }
            sleep();
        }
        return count;
    }

    /**
     * Converts Highlightly's {homeTeam:{formation, initialLineup:[[...rows]],
     * name}, awayTeam:{...}} shape into api-football.com's
     * [{team:{name}, formation, startXI:[{player:{name,number}}]}, ...] shape,
     * ordered to match our Match row's home/away rather than Highlightly's.
     */
    private ArrayNode normalize(JsonNode lineups, Match match) {
        ArrayNode result = objectMapper.createArrayNode();
        JsonNode highlightlyHome = lineups.path("homeTeam");
        JsonNode highlightlyAway = lineups.path("awayTeam");
        if (highlightlyHome.isMissingNode() || highlightlyAway.isMissingNode()) return result;

        String ourHomeName = match.getHomeTeam() != null ? match.getHomeTeam().getName() : null;
        boolean swapped = ourHomeName != null
                && !namesMatch(highlightlyHome.path("name").asText(""), ourHomeName)
                && namesMatch(highlightlyAway.path("name").asText(""), ourHomeName);

        JsonNode firstSide = swapped ? highlightlyAway : highlightlyHome;
        JsonNode secondSide = swapped ? highlightlyHome : highlightlyAway;

        result.add(toTeamLineup(firstSide));
        result.add(toTeamLineup(secondSide));
        return result;
    }

    private ObjectNode toTeamLineup(JsonNode side) {
        ObjectNode team = objectMapper.createObjectNode();
        ObjectNode teamName = objectMapper.createObjectNode();
        teamName.put("name", side.path("name").asText(""));
        team.set("team", teamName);
        team.put("formation", side.path("formation").asText(null));

        ArrayNode startXI = objectMapper.createArrayNode();
        for (JsonNode row : side.path("initialLineup")) {
            for (JsonNode p : row) {
                ObjectNode entry = objectMapper.createObjectNode();
                ObjectNode player = objectMapper.createObjectNode();
                player.put("name", p.path("name").asText(""));
                if (p.has("number") && !p.path("number").isNull()) {
                    player.put("number", p.path("number").asInt());
                } else {
                    player.putNull("number");
                }
                entry.set("player", player);
                startXI.add(entry);
            }
        }
        team.set("startXI", startXI);
        return team;
    }

    /**
     * Backfills Player.shirtNumber from this lineup's startXI data —
     * football-data.org never supplies it for national squads, so this is
     * the only source we have. normalized.get(0)/get(1) are already ordered
     * to match.getHomeTeam()/getAwayTeam() (see normalize()'s swap logic),
     * so no further matching is needed at the team level, only at the
     * player-name level (handled by PlayerService.backfillShirtNumber).
     */
    private void backfillShirtNumbers(Match match, ArrayNode normalized) {
        backfillTeamShirtNumbers(match.getHomeTeam(), normalized.get(0));
        backfillTeamShirtNumbers(match.getAwayTeam(), normalized.get(1));
    }

    private void backfillTeamShirtNumbers(Team team, JsonNode teamLineup) {
        if (team == null) return;
        for (JsonNode entry : teamLineup.path("startXI")) {
            JsonNode player = entry.path("player");
            String name = player.path("name").asText(null);
            JsonNode numberNode = player.path("number");
            if (name == null || name.isBlank() || !numberNode.isInt()) continue;
            playerService.backfillShirtNumber(team.getId(), name, numberNode.asInt());
        }
    }

    /** See TeamNameMatcher — shared name comparison (used here both for the
     * mapping step and for home/away swap detection). */
    private boolean namesMatch(String a, String b) {
        return TeamNameMatcher.matches(a, b);
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
            return Instant.parse(iso).atOffset(ZoneOffset.UTC);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(8))
                .header("x-rapidapi-key", apiKey)
                .header("x-rapidapi-host", RAPIDAPI_HOST)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            Thread.sleep(5000);
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }

        if (response.statusCode() != 200) {
            throw new RuntimeException("Highlightly returned HTTP " + response.statusCode() + " for " + path);
        }
        return objectMapper.readTree(response.body());
    }
}
