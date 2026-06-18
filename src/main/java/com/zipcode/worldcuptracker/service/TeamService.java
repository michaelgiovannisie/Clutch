package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /** All teams sorted alphabetically. */
    public List<Team> getAllTeams() {
        return teamRepository.findAllByOrderByNameAsc();
    }

    /** Teams in a given World Cup group (e.g. "A"). */
    public List<Team> getTeamsByGroup(String groupName) {
        return teamRepository.findByGroupNameIgnoreCase(groupName);
    }

    /** Single team by id. */
    public Optional<Team> getTeamById(Long id) {
        return teamRepository.findById(id);
    }

    /** Looks up a team by its football-data.org id, used when syncing match data. */
    public Optional<Team> getTeamByExternalId(Long externalId) {
        return teamRepository.findByExternalId(externalId);
    }

    /** Inserts a team if it doesn't already exist (matched by externalId), otherwise updates it. */
    public Team upsertTeam(Team incoming) {
        return teamRepository.findByExternalId(incoming.getExternalId())
                .map(existing -> {
                    existing.setName(incoming.getName());
                    existing.setTla(incoming.getTla());
                    existing.setCrestUrl(incoming.getCrestUrl());
                    existing.setFlag(incoming.getFlag());
                    existing.setGroupName(incoming.getGroupName());
                    existing.setCoach(incoming.getCoach());
                    return teamRepository.save(existing);
                })
                .orElseGet(() -> teamRepository.save(incoming));
    }
}
