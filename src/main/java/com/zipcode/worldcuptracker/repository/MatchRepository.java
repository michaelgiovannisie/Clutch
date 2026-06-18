package com.zipcode.worldcuptracker.repository;

import com.zipcode.worldcuptracker.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByExternalId(Long externalId);

    List<Match> findAllByOrderByUtcDateAsc();

    List<Match> findByVenueIdOrderByUtcDateAsc(Long venueId);

    List<Match> findByHomeTeamIdOrAwayTeamIdOrderByUtcDateAsc(Long homeTeamId, Long awayTeamId);
}
