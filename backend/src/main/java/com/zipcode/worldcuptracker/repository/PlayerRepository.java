package com.zipcode.worldcuptracker.repository;

import com.zipcode.worldcuptracker.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamId(Long teamId);

    Optional<Player> findByApiId(Integer apiId);
}
