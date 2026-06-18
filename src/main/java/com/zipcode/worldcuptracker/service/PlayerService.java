package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.model.Player;
import com.zipcode.worldcuptracker.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    /** Roster for a team, sorted by shirt number. */
    public List<Player> getPlayersByTeam(Long teamId) {
        return playerRepository.findByTeamIdOrderByShirtNumberAsc(teamId);
    }

    public Optional<Player> getPlayerByExternalId(Long externalId) {
        return playerRepository.findByExternalId(externalId);
    }

    /** Inserts a player if it doesn't already exist (matched by externalId), otherwise updates it. */
    public Player upsertPlayer(Player incoming) {
        return playerRepository.findByExternalId(incoming.getExternalId())
                .map(existing -> {
                    existing.setName(incoming.getName());
                    existing.setPosition(incoming.getPosition());
                    existing.setNationality(incoming.getNationality());
                    existing.setDateOfBirth(incoming.getDateOfBirth());
                    existing.setShirtNumber(incoming.getShirtNumber());
                    existing.setTeam(incoming.getTeam());
                    return playerRepository.save(existing);
                })
                .orElseGet(() -> playerRepository.save(incoming));
    }

    /**
     * football-data.org doesn't supply shirtNumber for national-team squads
     * (confirmed null across the board, unlike club squads where it's
     * reliably populated), so this fills the gap from a different source:
     * Highlightly's per-match starting-lineup data, which does include
     * player numbers. Only ever sets a number that's currently null — never
     * overwrites — and only for the player matched by name within that team
     * (via the same normalize/substring logic TeamNameMatcher already uses
     * for team names, reused here for player names since it's just generic
     * accent/punctuation-insensitive string matching).
     */
    public void backfillShirtNumber(Long teamId, String playerName, int shirtNumber) {
        if (teamId == null || playerName == null || playerName.isBlank()) return;
        for (Player p : playerRepository.findByTeamIdOrderByShirtNumberAsc(teamId)) {
            if (p.getShirtNumber() == null && TeamNameMatcher.matches(p.getName(), playerName)) {
                p.setShirtNumber(shirtNumber);
                playerRepository.save(p);
                return;
            }
        }
    }
}
