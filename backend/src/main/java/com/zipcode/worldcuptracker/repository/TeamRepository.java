package com.zipcode.worldcuptracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zipcode.worldcuptracker.model.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByGroupLabel(String groupLabel);

    Optional<Team> findByApiId(Integer apiId);
}
