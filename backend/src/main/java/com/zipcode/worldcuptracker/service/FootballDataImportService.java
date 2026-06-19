package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.model.DataRefreshLog;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.Player;
import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.model.Venue;
import com.zipcode.worldcuptracker.repository.DataRefreshLogRepository;
import com.zipcode.worldcuptracker.repository.MatchRepository;
import com.zipcode.worldcuptracker.repository.TeamRepository;
import com.zipcode.worldcuptracker.repository.VenueRepository;
import com.zipcode.worldcuptracker.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FootballDataImportService {

    private final ApiFootballService apiFootballService;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final VenueRepository venueRepository;
    private final DataRefreshLogRepository dataRefreshLogRepository;
    private final PlayerRepository playerRepository;

    @Value("${api.cache.static-minutes:10080}")
    private int staticCacheMinutes;

    @Value("${api.cache.results-minutes:15}")
    private int resultsCacheMinutes;

    public FootballDataImportService(
            ApiFootballService apiFootballService,
            TeamRepository teamRepository,
            MatchRepository matchRepository,
            VenueRepository venueRepository,
            DataRefreshLogRepository dataRefreshLogRepository,
            PlayerRepository playerRepository
    ) {
        this.apiFootballService = apiFootballService;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
        this.venueRepository = venueRepository;
        this.dataRefreshLogRepository = dataRefreshLogRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional
    public String refreshStaticData() {
        if (wasRecentlyRefreshed("api-football-static", staticCacheMinutes)) {
            return "Skipped static refresh. Cached recently.";
        }

        importTeams(apiFootballService.getTeams());
        importMatches(apiFootballService.getFixtures());

        saveRefreshLog("api-football-static");

        return "Static tournament data refreshed.";
    }

    @Transactional
    public String refreshResultsOnly() {
        if (wasRecentlyRefreshed("api-football-results", resultsCacheMinutes)) {
            return "Skipped results refresh. Cached recently.";
        }

        importMatches(apiFootballService.getFixtures());

        saveRefreshLog("api-football-results");

        return "Match results refreshed.";
    }

    @Transactional
    public String refreshFromApi() {
        return refreshStaticData();
    }

    @Transactional
    public void clearAllImportedData() {
        matchRepository.deleteAll();
        venueRepository.deleteAll();
        teamRepository.deleteAll();
        dataRefreshLogRepository.deleteAll();
    }

    @Transactional
    public String refreshRosters() {
    if (wasRecentlyRefreshed("api-football-rosters", 10080)) {
        return "Skipped roster refresh. Cached recently.";
    }

    for (Team team : teamRepository.findAll()) {
        importSquad(team);
    }

    saveRefreshLog("api-football-rosters");
    return "Rosters refreshed.";
    }

    @Transactional
    public String refreshGroups() {
    Map<String, Object> response = apiFootballService.getStandings();
    importGroupsFromStandings(response);
    return "Groups refreshed.";
}

    private boolean wasRecentlyRefreshed(String dataType, int cacheMinutes) {
        return dataRefreshLogRepository
                .findTopByDataTypeOrderByRefreshedAtDesc(dataType)
                .map(log -> log.getRefreshedAt()
                        .isAfter(LocalDateTime.now().minusMinutes(cacheMinutes)))
                .orElse(false);
    }

    private void saveRefreshLog(String dataType) {
        dataRefreshLogRepository.save(
                new DataRefreshLog(dataType, LocalDateTime.now())
        );
    }

    private String fetchCoachName(Integer teamApiId) {
    try {
        Map<String, Object> response = apiFootballService.getCoach(teamApiId);
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) response.get("response");

        if (items == null || items.isEmpty()) {
            return "Not available";
        }

        Map<String, Object> coach = items.get(0);
        Object name = coach.get("name");

        return name == null ? "Not available" : name.toString();

    } catch (Exception e) {
        return "Not available";
    }
}

    private void importTeams(Map<String, Object> response) {
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) response.get("response");

        if (items == null) {
            throw new RuntimeException("No teams found in API-Football response: " + response);
        }

        for (Map<String, Object> item : items) {
            Map<String, Object> teamData = (Map<String, Object>) item.get("team");
            Map<String, Object> coachData = (Map<String, Object>) item.get("coach");

            if (teamData == null) {
                continue;
            }

            Integer apiId = toInteger(teamData.get("id"));

            if (apiId == null) {
                continue;
            }

            Team team = teamRepository.findByApiId(apiId)
                    .orElse(new Team());

            team.setApiId(apiId);
            team.setName((String) teamData.get("name"));
            team.setCountry((String) teamData.get("country"));
            team.setFlagUrl((String) teamData.get("logo"));

            String coachName = fetchCoachName(apiId);
            team.setCoach(coachName);

            String fallbackGroup = fallbackGroupForTeam(team.getName());

            if (!fallbackGroup.equals("TBD")) {
                team.setGroupLabel(fallbackGroup);
            } else {
                team.setGroupLabel("TBD");
            }

            teamRepository.save(team);
        }
    }

    private void importMatches(Map<String, Object> response) {
    List<Map<String, Object>> items =
            (List<Map<String, Object>>) response.get("response");

    if (items == null) {
        throw new RuntimeException("No fixtures found in API-Football response: " + response);
    }

    for (Map<String, Object> item : items) {

        Map<String, Object> fixture = (Map<String, Object>) item.get("fixture");
        Map<String, Object> teams = (Map<String, Object>) item.get("teams");
        Map<String, Object> goals = (Map<String, Object>) item.get("goals");
        Map<String, Object> league = (Map<String, Object>) item.get("league");

        if (fixture == null || teams == null) {
            continue;
        }

        Map<String, Object> home = (Map<String, Object>) teams.get("home");
        Map<String, Object> away = (Map<String, Object>) teams.get("away");
        Map<String, Object> status = (Map<String, Object>) fixture.get("status");
        Map<String, Object> venueData = (Map<String, Object>) fixture.get("venue");

        if (home == null || away == null) {
            continue;
        }

        Integer fixtureId = toInteger(fixture.get("id"));
        Integer homeApiId = toInteger(home.get("id"));
        Integer awayApiId = toInteger(away.get("id"));

        if (fixtureId == null || homeApiId == null || awayApiId == null) {
            continue;
        }

        Team homeTeam = findTeamByApiId(homeApiId);
        Team awayTeam = findTeamByApiId(awayApiId);
        Venue venue = importVenue(venueData);

        Match match = matchRepository.findByApiId(fixtureId)
                .orElse(new Match());

        match.setApiId(fixtureId);
        match.setHomeTeam(homeTeam);
        match.setAwayTeam(awayTeam);
        match.setVenue(venue);

        if (status != null && status.get("long") != null) {
            match.setStatus((String) status.get("long"));
        } else {
            match.setStatus("TBD");
        }

        String round = league != null
                ? (String) league.get("round")
                : null;

        String groupLabel = extractGroupLabel(round);

        // API-Football often returns "Group Stage"
        if (groupLabel == null
                || groupLabel.isBlank()
                || groupLabel.equalsIgnoreCase("Group Stage")) {

            String homeFallback = fallbackGroupForTeam(homeTeam.getName());

            if (!homeFallback.equals("TBD")) {
                groupLabel = homeFallback;
            } else {
                String awayFallback = fallbackGroupForTeam(awayTeam.getName());

                if (!awayFallback.equals("TBD")) {
                    groupLabel = awayFallback;
                } else {
                    groupLabel = "TBD";
                }
            }
        }

        match.setGroupLabel(cleanGroupLabel(groupLabel));

        // Force update teams if they still have bad labels
        if (homeTeam.getGroupLabel() == null
                || homeTeam.getGroupLabel().isBlank()
                || homeTeam.getGroupLabel().equalsIgnoreCase("Group Stage")
                || homeTeam.getGroupLabel().equalsIgnoreCase("TBD")) {

            String homeFallback = fallbackGroupForTeam(homeTeam.getName());

            if (!homeFallback.equals("TBD")) {
                homeTeam.setGroupLabel(homeFallback);
            } else {
                homeTeam.setGroupLabel(groupLabel);
            }

            homeTeam.setGroupLabel(cleanGroupLabel(homeTeam.getGroupLabel()));
            teamRepository.save(homeTeam);
        }

        if (awayTeam.getGroupLabel() == null
                || awayTeam.getGroupLabel().isBlank()
                || awayTeam.getGroupLabel().equalsIgnoreCase("Group Stage")
                || awayTeam.getGroupLabel().equalsIgnoreCase("TBD")) {

            String awayFallback = fallbackGroupForTeam(awayTeam.getName());

            if (!awayFallback.equals("TBD")) {
                awayTeam.setGroupLabel(awayFallback);
            } else {
                awayTeam.setGroupLabel(groupLabel);
            }
            
            awayTeam.setGroupLabel(cleanGroupLabel(awayTeam.getGroupLabel()));
            teamRepository.save(awayTeam);
        }

        if (goals != null) {
            match.setHomeScore(toInteger(goals.get("home")));
            match.setAwayScore(toInteger(goals.get("away")));
        }

        String date = (String) fixture.get("date");

        if (date != null) {
            match.setKickoffTime(
                    OffsetDateTime.parse(date).toLocalDateTime()
            );
        }

        matchRepository.save(match);
    }
}

    private Venue importVenue(Map<String, Object> venueData) {
    if (venueData == null) {
        return null;
    }

    Integer venueApiId = toInteger(venueData.get("id"));
    String name = (String) venueData.get("name");
    String city = (String) venueData.get("city");

    if (name == null || name.isBlank()) {
        return null;
    }

    Venue venue;

    if (venueApiId != null) {
        venue = venueRepository.findByApiId(venueApiId).orElse(new Venue());
        venue.setApiId(venueApiId);
    } else {
        venue = venueRepository.findByNameAndCity(name, city).orElse(new Venue());
    }

    venue.setName(name);
    venue.setCity(city != null ? city : "Unknown");

    if (venue.getCountry() == null || venue.getCountry().isBlank()) {
        venue.setCountry("Unknown");
    }

    return venueRepository.save(venue);
}

    private void importSquad(Team team) {
    if (team.getApiId() == null) {
        return;
    }

    Map<String, Object> response = apiFootballService.getSquad(team.getApiId());

    List<Map<String, Object>> items =
            (List<Map<String, Object>>) response.get("response");

    if (items == null || items.isEmpty()) {
        return;
    }

    Map<String, Object> squadData = items.get(0);

    List<Map<String, Object>> players =
            (List<Map<String, Object>>) squadData.get("players");

    if (players == null) {
        return;
    }

    for (Map<String, Object> playerData : players) {
        Integer playerApiId = toInteger(playerData.get("id"));

        if (playerApiId == null) {
            continue;
        }

        Player player = playerRepository.findByApiId(playerApiId)
                .orElse(new Player());

        String shortName = (String) playerData.get("name");

        player.setApiId(playerApiId);
        player.setName(shortName);
        player.setFirstName(null);
        player.setLastName(null);
        player.setAge(toInteger(playerData.get("age")));
        player.setShirtNumber(toInteger(playerData.get("number")));
        player.setPosition((String) playerData.get("position"));
        player.setPhotoUrl((String) playerData.get("photo"));
        player.setTeam(team);

        playerRepository.save(player);
    }
}

    private Team findTeamByApiId(Integer apiId) {
        return teamRepository.findByApiId(apiId)
                .orElseThrow(() ->
                        new RuntimeException("Team not found for apiId: " + apiId));
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return Integer.parseInt(value.toString());
    }

    private void importGroupsFromStandings(Map<String, Object> response) {
    List<Map<String, Object>> topLevel =
            (List<Map<String, Object>>) response.get("response");

    if (topLevel == null || topLevel.isEmpty()) {
        return;
    }

    Map<String, Object> league = (Map<String, Object>) topLevel.get(0).get("league");

    if (league == null) {
        return;
    }

    List<List<Map<String, Object>>> standings =
            (List<List<Map<String, Object>>>) league.get("standings");

    if (standings == null) {
        return;
    }

    for (List<Map<String, Object>> groupTable : standings) {
        for (Map<String, Object> row : groupTable) {
            String groupName = (String) row.get("group");
            Map<String, Object> teamData = (Map<String, Object>) row.get("team");

            if (teamData == null || groupName == null) {
                continue;
            }

            Integer teamApiId = toInteger(teamData.get("id"));

            if (teamApiId == null) {
                continue;
            }

            Team team = teamRepository.findByApiId(teamApiId).orElse(null);

            if (team == null) {
                continue;
            }

            team.setGroupLabel(groupName);
            teamRepository.save(team);
        }
    }
}

private String extractGroupLabel(String round) {
    if (round == null) {
        return "TBD";
    }

    String upper = round.toUpperCase();

    if (upper.contains("GROUP A")) return "Group A";
    if (upper.contains("GROUP B")) return "Group B";
    if (upper.contains("GROUP C")) return "Group C";
    if (upper.contains("GROUP D")) return "Group D";
    if (upper.contains("GROUP E")) return "Group E";
    if (upper.contains("GROUP F")) return "Group F";
    if (upper.contains("GROUP G")) return "Group G";
    if (upper.contains("GROUP H")) return "Group H";
    if (upper.contains("GROUP I")) return "Group I";
    if (upper.contains("GROUP J")) return "Group J";
    if (upper.contains("GROUP K")) return "Group K";
    if (upper.contains("GROUP L")) return "Group L";

    return round;
}

private String fallbackGroupForTeam(String teamName) {
    if (teamName == null) return "TBD";

    return switch (teamName) {
        case "Mexico", "South Africa", "South Korea", "Czech Republic", "Czechia" -> "Group A";
        case "Canada", "Qatar", "Switzerland", "Bosnia and Herzegovina" -> "Group B";
        case "Brazil", "Morocco", "Haiti", "Scotland" -> "Group C";
        case "United States", "USA", "Paraguay", "Australia", "Turkey" -> "Group D";
        case "Germany", "Curaçao", "Curacao", "Ivory Coast", "Ecuador" -> "Group E";
        case "Netherlands", "Japan", "Tunisia", "Sweden" -> "Group F";
        case "Belgium", "Egypt", "Iran", "New Zealand" -> "Group G";
        case "Spain", "Cape Verde", "Saudi Arabia", "Uruguay" -> "Group H";
        case "France", "Senegal", "Iraq", "Norway" -> "Group I";
        case "Argentina", "Algeria", "Austria", "Jordan" -> "Group J";
        case "Portugal", "DR Congo", "Congo DR", "Uzbekistan", "Colombia" -> "Group K";
        case "England", "Croatia", "Ghana", "Panama" -> "Group L";
        default -> "TBD";
    };
}

private boolean isBadGroupLabel(String groupLabel) {
    return groupLabel == null
            || groupLabel.isBlank()
            || groupLabel.equalsIgnoreCase("TBD")
            || groupLabel.equalsIgnoreCase("Group Stage");
}

private String cleanGroupLabel(String label) {
    if (label == null || label.isBlank() || label.equalsIgnoreCase("Group Stage")) {
        return "TBD";
    }
    return label;
}

}