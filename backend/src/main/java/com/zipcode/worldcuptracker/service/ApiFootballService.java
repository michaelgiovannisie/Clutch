package com.zipcode.worldcuptracker.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ApiFootballService {

    private final RestClient restClient;

    @Value("${api.football.world-cup-league-id}")
    private Integer leagueId;

    @Value("${api.football.season}")
    private Integer season;

    public ApiFootballService(
            @Value("${api.football.base-url}") String baseUrl,
            @Value("${api.football.key}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-apisports-key", apiKey)
                .build();
    }

    public Map<String, Object> getTeams() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> getFixtures() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> getSquad(Integer teamApiId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/players/squads")
                        .queryParam("team", teamApiId)
                        .build())
                .retrieve()
                .body(Map.class);
    }

    public Map<String, Object> getCoach(Integer teamApiId) {
    return restClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/coachs")
                    .queryParam("team", teamApiId)
                    .build())
            .retrieve()
            .body(Map.class);
}

public Map<String, Object> getPlayerProfile(Integer playerApiId) {
    return restClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/players/profiles")
                    .queryParam("player", playerApiId)
                    .build())
            .retrieve()
            .body(Map.class);
}

public Map<String, Object> getStandings() {
    return restClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/standings")
                    .queryParam("league", leagueId)
                    .queryParam("season", season)
                    .build())
            .retrieve()
            .body(Map.class);
}

}