package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.dto.StandingDto;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.repository.MatchRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/standings")
@CrossOrigin(origins = "http://localhost:5173")
public class StandingsController {

    private final MatchRepository matchRepository;

    public StandingsController(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @GetMapping
    public List<StandingDto> getStandings(@RequestParam(required = false) String group) {
        Map<Long, StandingDto> table = new HashMap<>();

        for (Match match : matchRepository.findAll()) {
            if (match.getHomeTeam() == null || match.getAwayTeam() == null) continue;
            if (match.getHomeScore() == null || match.getAwayScore() == null) continue;

            if (group != null && !group.equalsIgnoreCase(match.getGroupLabel())) continue;

            applyMatch(table, match.getHomeTeam(), match.getGroupLabel(), match.getHomeScore(), match.getAwayScore());
            applyMatch(table, match.getAwayTeam(), match.getGroupLabel(), match.getAwayScore(), match.getHomeScore());
        }

        return table.values().stream()
                .sorted(
                        Comparator.comparingInt((StandingDto s) -> s.points).reversed()
                                .thenComparing(Comparator.comparingInt((StandingDto s) -> s.goalDifference).reversed())
                                .thenComparing(Comparator.comparingInt((StandingDto s) -> s.goalsFor).reversed())
                )
                .toList();
    }

    private void applyMatch(Map<Long, StandingDto> table, Team team, String groupLabel, int goalsFor, int goalsAgainst) {
        StandingDto row = table.computeIfAbsent(team.getId(), id -> {
            StandingDto s = new StandingDto();
            s.teamId = team.getId();
            s.teamName = team.getName();
            s.groupLabel = groupLabel;
            return s;
        });

        row.played++;
        row.goalsFor += goalsFor;
        row.goalsAgainst += goalsAgainst;
        row.goalDifference = row.goalsFor - row.goalsAgainst;

        if (goalsFor > goalsAgainst) {
            row.won++;
            row.points += 3;
        } else if (goalsFor == goalsAgainst) {
            row.drawn++;
            row.points += 1;
        } else {
            row.lost++;
        }
    }
}
