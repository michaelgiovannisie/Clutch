package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.TeamStanding;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Computes World Cup group standings on the fly from synced Match data — no
 * separate table to keep in sync, since it's fully derivable from finished
 * group-stage matches.
 *
 * Group membership is derived from match.groupName rather than
 * team.groupName: football-data.org's /teams endpoint never returns group
 * info, so team.groupName is always null even after a full sync, while
 * match.groupName is populated correctly during syncMatches().
 */
@Service
public class StandingsService {

    private final MatchService matchService;

    public StandingsService(MatchService matchService) {
        this.matchService = matchService;
    }

    /** All group letters that currently have matches assigned, sorted (A, B, C...). */
    public List<String> getGroups() {
        return new TreeSet<>(matchService.getDistinctGroupNames()).stream().toList();
    }

    /**
     * Standings table for one group, sorted by points, then goal difference,
     * then goals scored, then name — the standard FIFA tiebreak order (minus
     * head-to-head/fair-play, which we don't track).
     */
    public List<TeamStanding> getGroupStandings(String groupName) {
        Map<Long, TeamStanding> standingsByTeamId = new LinkedHashMap<>();
        for (Match match : matchService.getMatchesByGroup(groupName)) {
            if (match.getHomeTeam() != null) {
                standingsByTeamId.putIfAbsent(match.getHomeTeam().getId(), new TeamStanding(match.getHomeTeam()));
            }
            if (match.getAwayTeam() != null) {
                standingsByTeamId.putIfAbsent(match.getAwayTeam().getId(), new TeamStanding(match.getAwayTeam()));
            }
        }

        for (Match match : matchService.getFinishedMatchesByGroup(groupName)) {
            if (match.getHomeTeam() == null || match.getAwayTeam() == null
                    || match.getHomeScore() == null || match.getAwayScore() == null) {
                continue;
            }
            TeamStanding home = standingsByTeamId.get(match.getHomeTeam().getId());
            TeamStanding away = standingsByTeamId.get(match.getAwayTeam().getId());
            if (home != null) home.recordResult(match.getHomeScore(), match.getAwayScore());
            if (away != null) away.recordResult(match.getAwayScore(), match.getHomeScore());
        }

        return standingsByTeamId.values().stream()
                .sorted(Comparator
                        .comparingInt(TeamStanding::getPoints).reversed()
                        .thenComparing(Comparator.comparingInt(TeamStanding::getGoalDifference).reversed())
                        .thenComparing(Comparator.comparingInt(TeamStanding::getGoalsFor).reversed())
                        .thenComparing(s -> s.getTeam().getName()))
                .toList();
    }
}
