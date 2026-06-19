package com.zipcode.worldcuptracker.repository;

import com.zipcode.worldcuptracker.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStatus(String status);
    List<Match> findByGroupLabel(String groupLabel);
    List<Match> findByVenueId(Long venueId);
    Optional<Match> findByApiId(Integer apiId);
}
