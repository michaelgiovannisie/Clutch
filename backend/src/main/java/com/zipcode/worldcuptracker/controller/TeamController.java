package com.zipcode.worldcuptracker.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zipcode.worldcuptracker.model.Player;
import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.repository.PlayerRepository;
import com.zipcode.worldcuptracker.repository.TeamRepository;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "http://localhost:5173")
public class TeamController {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public TeamController(TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    @GetMapping
    public List<Team> getTeams() {
        return teamRepository.findAll();
    }

    @GetMapping("/{id}")
    public Team getTeamById(@PathVariable Long id) {
        return teamRepository.findById(id).orElseThrow();
    }

    @GetMapping("/{id}/players")
    public List<Player> getPlayersByTeam(@PathVariable Long id) {
        return playerRepository.findByTeamId(id);
    }
}
