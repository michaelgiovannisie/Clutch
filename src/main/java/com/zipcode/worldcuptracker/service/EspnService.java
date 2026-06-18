package com.zipcode.worldcuptracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zipcode.worldcuptracker.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls match timeline events, statistics, and venue from ESPN's public
 * (unofficial, undocumented) scoreboard endpoint — no API key, no account, no
 * plan restriction, and it's actively serving real 2026 World Cup data. Used
 * in place of api-football.com for this data because api-football.com's free
 * tier only covers seasons 2022-2024 (confirmed directly: querying league=1,
 * season=2026 returns zero fixtures with an explicit "Free plans do not have
 * access to this season" error), and football-data.org's free tier sends
 * neither player-level data (lineups/cards/subs) nor, it turns out, a venue
 * at all for World Cup matches.
 *
 * ESPN does not expose starting lineups/formations on this free endpoint, so
 * lineups are left to the Highlightly pipeline.
 *
 * Output is normalized into the same shape api-football.com used
 * ({time:{elapsed,extra}, type, detail, player:{name}, team:{name}} for
 * events; [{team:{name}, statistics:[{type,value}]}, ...] for stats) so the
 * existing match-detail.html rendering JS needs no changes.
 */
@Service
public class EspnService {

    private static final Logger log = LoggerFactory.getLogger(EspnService.class);

    private static final String BASE_URL = "https://site.api.espn.com/apis/site/v2/sports/soccer/fifa.world";
    // ESPN's "dates" param buckets events by US Eastern game day, not UTC calendar
    // day (confirmed: a 22:00 ET kickoff, which is 02:00 UTC the next day, is filed
    // by ESPN under the EARLIER date). Our matches are stored as UTC instants, so a
    // naive UTC-based date conversion silently queries the wrong day for any match
    // that crosses the UTC midnight boundary relative to Eastern time.
    private static final ZoneId ESPN_ZONE = ZoneId.of("America/New_York");
    private static final int MAX_DETAIL_SYNCS_PER_RUN = 20;
    // ESPN's hidden endpoint has no documented quota (unlike api-football.com's 100/day,
    // which this constant was originally sized for), so venue backfill can run uncapped
    // in practice — 150 comfortably covers the full 104-match World Cup schedule in one go.
    private static final int MAX_VENUE_SYNCS_PER_RUN = 150;
    private static final Pattern CLOCK_PATTERN = Pattern.compile("(\\d+)'(?:\\+(\\d+)')?");

    private static final java.util.Map<String, String> STAT_LABELS = java.util.Map.ofEntries(
            java.util.Map.entry("possessionPct", "Ball Possession"),
            java.util.Map.entry("totalShots", "Total Shots"),
            java.util.Map.entry("shotsOnTarget", "Shots on Goal"),
            java.util.Map.entry("wonCorners", "Corner Kicks"),
            java.util.Map.entry("foulsCommitted", "Fouls"),
            java.util.Map.entry("goalAssists", "Assists"),
            java.util.Map.entry("shotAssists", "Shot Assists"),
            java.util.Map.entry("totalGoals", "Goals")
    );

    private final MatchService matchService;
    private final VenueService venueService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public EspnService(MatchService matchService, VenueService venueService) {
        this.matchService = matchService;
        this.venueService = venueService;
    }

    /** Runs shortly after startup, then hourly — ESPN's hidden API has no documented quota, but stay polite. */
    @Scheduled(initialDelay = 90_000, fixedRate = 60 * 60 * 1000)
    public void scheduledSync() {
        log.info("Running scheduled ESPN sync: {}", syncAll());
    }

    public String syncAll() {
        var detailPending = matchService.getMatchesNeedingEspnDetail(MAX_DETAIL_SYNCS_PER_RUN);
        var venuePending = matchService.getMatchesNeedingVenue(MAX_VENUE_SYNCS_PER_RUN);
        if (detailPending.isEmpty() && venuePending.isEmpty()) {
            return "Nothing to sync: no matches are missing ESPN timeline/statistics/venue data.";
        }

        // Query both the UTC-bucketed and Eastern-bucketed date for each match, since
        // we don't know in advance which convention ESPN will use for it (see
        // ESPN_ZONE comment above) — querying one extra, mostly-empty date is cheap.
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (Match m : detailPending) {
            addCandidateDates(dates, m);
        }
        for (Match m : venuePending) {
            addCandidateDates(dates, m);
        }

        int updated = 0;
        int venuesResolved = 0;
        int scoreboardCalls = 0;
        for (LocalDate date : dates) {
            try {
                JsonNode events = getScoreboard(date).path("events");
                scoreboardCalls++;

                // No date-equality filter here: findMatchingEvent identifies the right
                // event by team names, not date, so it's safe (and necessary, given the
                // bucketing ambiguity above) to try every still-pending match against
                // every queried date's events rather than pre-filtering by a date we
                // can't fully trust.
                for (Match m : detailPending) {
                    if (m.getEventsJson() != null && m.getStatisticsJson() != null) continue;

                    JsonNode matchEvent = findMatchingEvent(events, m);
                    if (matchEvent == null) continue;

                    if (applyDetail(m, matchEvent)) {
                        matchService.save(m);
                        updated++;
                    }
                }

                for (Match m : venuePending) {
                    if (m.getVenue() != null) continue;

                    JsonNode matchEvent = findMatchingEvent(events, m);
                    if (matchEvent == null) continue;

                    if (applyVenue(m, matchEvent)) {
                        matchService.save(m);
                        venuesResolved++;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to sync ESPN data for {}: {}", date, e.getMessage());
            }
        }
        return "Updated " + updated + " match(es) and resolved " + venuesResolved
                + " venue(s) from ESPN across " + scoreboardCalls + " scoreboard call(s).";
    }

    /** Adds both the UTC-calendar-day and Eastern-calendar-day date for a match's
     * kickoff instant to the set of dates to query — see ESPN_ZONE comment. */
    private void addCandidateDates(Set<LocalDate> dates, Match m) {
        if (m.getUtcDate() == null) return;
        dates.add(m.getUtcDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate());
        dates.add(m.getUtcDate().atZoneSameInstant(ESPN_ZONE).toLocalDate());
    }

    /** Resolves and sets match.venue from ESPN's competition.venue.fullName, if present and matchable. */
    private boolean applyVenue(Match match, JsonNode event) {
        JsonNode competition = event.path("competitions").path(0);
        String venueName = competition.path("venue").path("fullName").asText(null);
        if (venueName == null || venueName.isBlank()) return false;

        var venue = venueService.resolveByName(venueName);
        if (venue.isEmpty()) {
            log.warn("ESPN sent venue \"{}\" for a match but it didn't match any seeded venue", venueName);
            return false;
        }
        match.setVenue(venue.get());
        return true;
    }

    private JsonNode findMatchingEvent(JsonNode events, Match match) {
        String homeName = match.getHomeTeam() != null ? match.getHomeTeam().getName() : null;
        String awayName = match.getAwayTeam() != null ? match.getAwayTeam().getName() : null;
        if (homeName == null || awayName == null) return null;

        for (JsonNode event : events) {
            JsonNode competitors = event.path("competitions").path(0).path("competitors");
            if (!competitors.isArray() || competitors.size() < 2) continue;

            String teamA = competitors.get(0).path("team").path("displayName").asText("");
            String teamB = competitors.get(1).path("team").path("displayName").asText("");

            boolean matches = (namesMatch(teamA, homeName) && namesMatch(teamB, awayName))
                    || (namesMatch(teamA, awayName) && namesMatch(teamB, homeName));
            if (matches) return event;
        }
        return null;
    }

    /** Returns true if either events or statistics were newly populated. */
    private boolean applyDetail(Match match, JsonNode event) {
        JsonNode competition = event.path("competitions").path(0);
        JsonNode competitors = competition.path("competitors");
        if (!competitors.isArray() || competitors.size() < 2) return false;

        // ESPN's own feed sometimes still reports a match as "pre"/not completed
        // even after its real-world kickoff has passed (a data-lag issue on
        // ESPN's end, not ours). If we don't gate on this, the "always set
        // eventsJson, even empty" caching below would permanently lock in an
        // empty timeline the first time we see such a match — before ESPN ever
        // publishes the real events/stats. Skip entirely so it stays eligible
        // for retry on a later sync once ESPN catches up.
        boolean espnCompleted = competition.path("status").path("type").path("completed").asBoolean(false);
        if (!espnCompleted) return false;

        boolean changed = false;

        if (match.getStatisticsJson() == null) {
            ArrayNode statsArray = objectMapper.createArrayNode();
            for (JsonNode competitor : competitors) {
                ObjectNode teamStats = objectMapper.createObjectNode();
                ObjectNode teamNode = objectMapper.createObjectNode();
                teamNode.put("name", competitor.path("team").path("displayName").asText(""));
                teamStats.set("team", teamNode);

                ArrayNode statsList = objectMapper.createArrayNode();
                for (JsonNode stat : competitor.path("statistics")) {
                    String name = stat.path("name").asText("");
                    if ("appearances".equals(name)) continue; // not a real match stat, always 0 — ESPN box-score artifact
                    ObjectNode statNode = objectMapper.createObjectNode();
                    statNode.put("type", STAT_LABELS.getOrDefault(name, prettify(name)));
                    String value = stat.path("displayValue").asText("");
                    if ("possessionPct".equals(name) && !value.isBlank() && !value.endsWith("%")) {
                        value = value + "%";
                    }
                    statNode.put("value", value);
                    statsList.add(statNode);
                }
                teamStats.set("statistics", statsList);
                statsArray.add(teamStats);
            }
            if (statsArray.size() >= 2 && statsArray.get(0).path("statistics").size() > 0) {
                match.setStatisticsJson(statsArray.toString());
                changed = true;
            }
        }

        if (match.getEventsJson() == null) {
            ArrayNode eventsArray = objectMapper.createArrayNode();
            // Map ESPN competitor team id -> display name so events (which only carry team id) can be labeled.
            java.util.Map<String, String> teamNamesById = new java.util.HashMap<>();
            for (JsonNode competitor : competitors) {
                teamNamesById.put(competitor.path("team").path("id").asText(""), competitor.path("team").path("displayName").asText(""));
            }

            for (JsonNode detail : competition.path("details")) {
                ObjectNode normalized = normalizeEvent(detail, teamNamesById);
                if (normalized != null) eventsArray.add(normalized);
            }
            // Always set, even if empty, so we don't re-fetch finished matches with zero events every run.
            match.setEventsJson(eventsArray.toString());
            changed = true;
        }

        return changed;
    }

    private ObjectNode normalizeEvent(JsonNode detail, java.util.Map<String, String> teamNamesById) {
        String typeText = detail.path("type").path("text").asText("");
        if (typeText.isBlank()) return null;

        String type;
        String detailText;
        if (typeText.toLowerCase(java.util.Locale.ROOT).contains("card")) {
            type = "Card";
            detailText = typeText;
        } else if (typeText.toLowerCase(java.util.Locale.ROOT).contains("substitution")) {
            type = "subst";
            detailText = "Substitution";
        } else if (typeText.toLowerCase(java.util.Locale.ROOT).contains("goal")) {
            type = "Goal";
            detailText = typeText.replaceFirst("(?i)^goal\\s*-?\\s*", "");
            if (detailText.isBlank()) detailText = "Normal Goal";
        } else {
            type = typeText;
            detailText = typeText;
        }

        String displayClock = detail.path("clock").path("displayValue").asText("");
        int elapsed = 0;
        Integer extra = null;
        Matcher matcher = CLOCK_PATTERN.matcher(displayClock);
        if (matcher.find()) {
            elapsed = Integer.parseInt(matcher.group(1));
            if (matcher.group(2) != null) extra = Integer.parseInt(matcher.group(2));
        }

        String teamId = detail.path("team").path("id").asText("");
        String teamName = teamNamesById.getOrDefault(teamId, "");

        String playerName = "";
        JsonNode athletes = detail.path("athletesInvolved");
        if (athletes.isArray() && athletes.size() > 0) {
            playerName = athletes.get(0).path("displayName").asText("");
        }

        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode time = objectMapper.createObjectNode();
        time.put("elapsed", elapsed);
        if (extra != null) time.put("extra", extra); else time.putNull("extra");
        node.set("time", time);
        node.put("type", type);
        node.put("detail", detailText);
        ObjectNode player = objectMapper.createObjectNode();
        player.put("name", playerName);
        node.set("player", player);
        ObjectNode team = objectMapper.createObjectNode();
        team.put("name", teamName);
        node.set("team", team);
        return node;
    }

    private String prettify(String camelCase) {
        if (camelCase.isBlank()) return camelCase;
        String spaced = camelCase.replaceAll("([a-z])([A-Z])", "$1 $2");
        return spaced.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + spaced.substring(1);
    }

    /** See TeamNameMatcher — shared name comparison, since ESPN's displayName
     * formatting (e.g. "Bosnia and Herzegovina", "United States", "Czechia")
     * differs from football-data.org's and Highlightly's formatting. */
    private boolean namesMatch(String a, String b) {
        return TeamNameMatcher.matches(a, b);
    }

    private JsonNode getScoreboard(LocalDate date) throws Exception {
        String dateParam = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/scoreboard?dates=" + dateParam))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("ESPN returned HTTP " + response.statusCode() + " for " + dateParam);
        }
        return objectMapper.readTree(response.body());
    }
}
