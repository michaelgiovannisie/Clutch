package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.model.Player;
import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.service.PlayerService;
import com.zipcode.worldcuptracker.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class TeamController {

    private final TeamService teamService;
    private final PlayerService playerService;

    public TeamController(TeamService teamService, PlayerService playerService) {
        this.teamService = teamService;
        this.playerService = playerService;
    }

    // ── Thymeleaf pages ───────────────────────────────────────────────────────

    /** GET /teams */
    @GetMapping("/teams")
    public String teamList(Model model) {
        model.addAttribute("teams", teamService.getAllTeams());
        return "teams/team-list";
    }

    /** GET /teams/{id} */
    @GetMapping("/teams/{id}")
    public String teamDetail(@PathVariable Long id, Model model) {
        return teamService.getTeamById(id)
                .map(team -> {
                    model.addAttribute("team", team);
                    return "teams/team-detail";
                })
                .orElse("redirect:/teams");
    }

    // ── REST endpoints ────────────────────────────────────────────────────────

    /** GET /api/teams?group=A */
    @GetMapping("/api/teams")
    @ResponseBody
    public ResponseEntity<List<Team>> apiTeams(@RequestParam(required = false) String group) {
        List<Team> teams = (group != null && !group.isBlank())
                ? teamService.getTeamsByGroup(group)
                : teamService.getAllTeams();
        return ResponseEntity.ok(teams);
    }

    /** GET /api/teams/{id} */
    @GetMapping("/api/teams/{id}")
    @ResponseBody
    public ResponseEntity<Team> apiTeamById(@PathVariable Long id) {
        return teamService.getTeamById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/teams/{id}/players */
    @GetMapping("/api/teams/{id}/players")
    @ResponseBody
    public ResponseEntity<List<Player>> apiTeamPlayers(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getPlayersByTeam(id));
    }
}
