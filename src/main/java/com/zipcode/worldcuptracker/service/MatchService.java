package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.repository.MatchRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    /** All matches sorted by kickoff time ascending. */
    public List<Match> getAllMatches() {
        return matchRepository.findAllByOrderByUtcDateAsc();
    }

    /** Single match by id. */
    public Optional<Match> getMatchById(Long id) {
        return matchRepository.findById(id);
    }

    /** Matches scheduled to be played at a given venue. */
    public List<Match> getMatchesByVenue(Long venueId) {
        return matchRepository.findByVenueIdOrderByUtcDateAsc(venueId);
    }

    /** Matches involving a given team, home or away. */
    public List<Match> getMatchesByTeam(Long teamId) {
        return matchRepository.findByHomeTeamIdOrAwayTeamIdOrderByUtcDateAsc(teamId, teamId);
    }

    /** Finished group-stage matches for a given group, used to compute standings. */
    public List<Match> getFinishedMatchesByGroup(String groupName) {
        return matchRepository.findByGroupNameAndStatus(groupName, "FINISHED");
    }

    /** All group-stage matches (any status) for a given group, used to derive
     * group membership — team.groupName is never populated (football-data.org's
     * /teams endpoint doesn't return group info), so standings derive both the
     * group list and each group's teams from match data instead. */
    public List<Match> getMatchesByGroup(String groupName) {
        return matchRepository.findByGroupName(groupName);
    }

    /** Distinct group letters that currently have at least one match assigned. */
    public List<String> getDistinctGroupNames() {
        return matchRepository.findDistinctGroupNames();
    }

    /** Looks up a match by its football-data.org id, used when syncing. */
    public Optional<Match> getMatchByExternalId(Long externalId) {
        return matchRepository.findByExternalId(externalId);
    }

    public Match save(Match match) {
        return matchRepository.save(match);
    }

    /**
     * Finds a Match by team names and kickoff date, used to link api-football.com
     * fixtures to our football-data.org-sourced rows — the two providers use
     * unrelated ids, so we match on what's stable across both: same calendar day,
     * and team names that are equal or one contains the other (handles minor
     * naming differences like "Korea Republic" vs "South Korea").
     */
    public Optional<Match> findByTeamsAndDate(String homeName, String awayName, OffsetDateTime kickoff) {
        return matchRepository.findAll().stream()
                .filter(m -> m.getHomeTeam() != null && m.getAwayTeam() != null && m.getUtcDate() != null)
                .filter(m -> namesMatch(m.getHomeTeam().getName(), homeName)
                        && namesMatch(m.getAwayTeam().getName(), awayName))
                .filter(m -> m.getUtcDate().toLocalDate().equals(kickoff.toLocalDate()))
                .findFirst();
    }

    /**
     * Finished matches that already have an api-football.com fixture id mapped
     * but haven't had lineups/stats/events pulled yet, capped to a small batch
     * since each one costs 3 requests against api-football.com's 100/day free quota.
     */
    public List<Match> getFinishedMatchesNeedingApiFootballDetail(int limit) {
        return matchRepository.findAll().stream()
                .filter(m -> "FINISHED".equals(m.getStatus()))
                .filter(m -> m.getApiFootballFixtureId() != null)
                .filter(m -> m.getEventsJson() == null)
                .limit(limit)
                .toList();
    }

    /**
     * Matches that are live or finished but still missing ESPN-sourced timeline/
     * statistics data, capped to a small batch per sync run.
     */
    public List<Match> getMatchesNeedingEspnDetail(int limit) {
        return matchRepository.findAll().stream()
                .filter(m -> "FINISHED".equals(m.getStatus()) || "IN_PLAY".equals(m.getStatus()) || "PAUSED".equals(m.getStatus()))
                .filter(m -> m.getEventsJson() == null || m.getStatisticsJson() == null)
                .limit(limit)
                .toList();
    }

    /**
     * Finished matches that already have a Highlightly match id mapped but
     * haven't had lineups pulled yet, capped to a small batch per sync run.
     * Restricted to FINISHED (rather than also IN_PLAY/PAUSED, as the ESPN
     * sync does) because lineups for not-yet-finished/not-yet-started matches
     * usually aren't published yet — attempting those first would burn the
     * batch on empty results while real, available data for finished matches
     * waits. Sorted oldest-first so a backlog clears in chronological order
     * across repeated sync calls instead of in arbitrary id order.
     */
    public List<Match> getMatchesNeedingHighlightlyLineup(int limit) {
        return matchRepository.findAll().stream()
                .filter(m -> "FINISHED".equals(m.getStatus()))
                .filter(m -> m.getHighlightlyMatchId() != null)
                .filter(m -> m.getLineupsJson() == null)
                .sorted(java.util.Comparator.comparing(Match::getUtcDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    /**
     * Same as getMatchesNeedingHighlightlyLineup but ignores whether lineupsJson
     * is already populated — used for a forced re-sync after a normalization
     * fix (e.g. the coach field) so already-cached matches get refreshed
     * instead of being permanently skipped.
     */
    public List<Match> getMatchesWithHighlightlyLineupMapped(int limit) {
        return matchRepository.findAll().stream()
                .filter(m -> "FINISHED".equals(m.getStatus()))
                .filter(m -> m.getHighlightlyMatchId() != null)
                .sorted(java.util.Comparator.comparing(Match::getUtcDate,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    /**
     * Matches missing venue, capped to a small batch per sync run. Used by
     * EspnService — neither football-data.org's free tier (sends no venue at
     * all for World Cup matches) nor api-football.com's free tier (blocks
     * season 2026 entirely) can supply this, but ESPN's hidden scoreboard
     * endpoint includes it for free. Requires a kickoff date since matches are
     * looked up via the dated scoreboard endpoint.
     */
    public List<Match> getMatchesNeedingVenue(int limit) {
        return matchRepository.findAll().stream()
                .filter(m -> m.getVenue() == null)
                .filter(m -> m.getUtcDate() != null)
                .sorted(java.util.Comparator.comparing(Match::getUtcDate))
                .limit(limit)
                .toList();
    }

    /**
     * Compares team names across providers. See TeamNameMatcher for details —
     * handles both punctuation/wording variants (e.g. football-data.org's
     * "Bosnia-Herzegovina" vs Highlightly's "Bosnia & Herzegovina") and true
     * synonym pairs that share no substring at all (e.g. "USA" vs "United
     * States", "Czechia" vs "Czech Republic", "South Korea" vs "Korea Republic").
     */
    private boolean namesMatch(String a, String b) {
        return TeamNameMatcher.matches(a, b);
    }
}
