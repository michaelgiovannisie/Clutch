package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.repository.MatchRepository;
import org.springframework.stereotype.Service;

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

    /** Looks up a match by its football-data.org id, used when syncing. */
    public Optional<Match> getMatchByExternalId(Long externalId) {
        return matchRepository.findByExternalId(externalId);
    }

    public Match save(Match match) {
        return matchRepository.save(match);
    }
}
