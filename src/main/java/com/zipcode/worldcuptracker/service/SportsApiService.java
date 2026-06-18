package com.zipcode.worldcuptracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.model.Venue;
import com.zipcode.worldcuptracker.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
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

    private static final String BASE_URL = "https://api.football-data.org/v4";
    private static final String COMPETITION_CODE = "WC"; // FIFA World Cup

    private final TeamService teamService;
    private final MatchService matchService;
    private final VenueRepository venueRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${api.football.key:}")
    private String apiKey;

    public SportsApiService(TeamService teamService, MatchService matchService, VenueRepository venueRepository) {
        this.teamService = teamService;
        this.matchService = matchService;
        this.venueRepository = venueRepository;
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
        return "Synced " + teamsSynced + " teams and " + matchesSynced + " matches from football-data.org.";
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
                match.setVenue(resolveVenue(m.path("venue").asText(null)));

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

    /** football-data.org returns groups like "GROUP_A" — we just want "A". */
    private String extractGroupLetter(String group) {
        if (group == null || group.isBlank()) return null;
        int idx = group.lastIndexOf('_');
        return idx >= 0 ? group.substring(idx + 1) : group;
    }

    /**
     * football-data.org gives the venue as a plain stadium-name string, not an id,
     * so we match it against our seeded Venue table by loose name containment.
     */
    private Venue resolveVenue(String venueName) {
        if (venueName == null || venueName.isBlank()) return null;
        String needle = venueName.toLowerCase(Locale.ROOT);
        List<Venue> all = venueRepository.findAll();
        for (Venue v : all) {
            String name = v.getName().toLowerCase(Locale.ROOT);
            if (name.contains(needle) || needle.contains(name)) {
                return v;
            }
        }
        return null;
    }

    private OffsetDateTime parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(8))
                .header("X-Auth-Token", apiKey)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("football-data.org returned HTTP " + response.statusCode() + " for " + path);
        }
        return objectMapper.readTree(response.body());
    }
}
