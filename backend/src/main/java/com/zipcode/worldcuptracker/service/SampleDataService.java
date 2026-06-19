package com.zipcode.worldcuptracker.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.Player;
import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.repository.MatchRepository;
import com.zipcode.worldcuptracker.repository.PlayerRepository;
import com.zipcode.worldcuptracker.repository.TeamRepository;

@Service
public class SampleDataService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    public SampleDataService(
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            MatchRepository matchRepository
    ) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
    }

    public void refreshSampleData() {
        matchRepository.deleteAll();
        playerRepository.deleteAll();
        teamRepository.deleteAll();

        Team usa = teamRepository.save(
                new Team(1, "United States", "USA", "", "D", "Mauricio Pochettino")
        );

        Team england = teamRepository.save(
                new Team(2, "England", "England", "", "L", "Thomas Tuchel")
        );

        playerRepository.save(new Player("Christian Pulisic", "Forward", 10, usa));
        playerRepository.save(new Player("Weston McKennie", "Midfielder", 8, usa));
        playerRepository.save(new Player("Harry Kane", "Forward", 9, england));
        playerRepository.save(new Player("Jude Bellingham", "Midfielder", 10, england));

        matchRepository.save(
                new Match(
                        101,
                        usa,
                        england,
                        LocalDateTime.now().plusDays(2),
                        null,
                        null,
                        "SCHEDULED",
                        "Friendly"
                )
        );

        matchRepository.save(
                new Match(
                        102,
                        usa,
                        england,
                        LocalDateTime.now().minusDays(1),
                        2,
                        1,
                        "FINISHED",
                        "Friendly"
                )
        );
    }
}
