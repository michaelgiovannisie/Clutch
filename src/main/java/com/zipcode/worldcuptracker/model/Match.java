package com.zipcode.worldcuptracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID from football-data.org, used to avoid duplicate syncs. */
    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(name = "utc_date")
    private OffsetDateTime utcDate;

    /** football-data.org status: SCHEDULED, TIMED, IN_PLAY, PAUSED, FINISHED, SUSPENDED, POSTPONED, CANCELLED, AWARDED */
    private String status;

    private Integer matchday;

    /** football-data.org stage, e.g. GROUP_STAGE, LAST_16, QUARTER_FINALS, SEMI_FINALS, FINAL */
    private String stage;

    /** World Cup group letter, e.g. "A" — null once knockout stage begins */
    @Column(name = "group_name")
    private String groupName;

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    private Integer homeScore;

    private Integer awayScore;

    /** "HOME_TEAM", "AWAY_TEAM", "DRAW", or null if not finished */
    private String winner;

    /**
     * Fixture id from api-football.com (v3, league=1/season=2026 for the World
     * Cup), used to pull lineups/stats/events that football-data.org's free
     * tier doesn't expose. Null until matched by date + team names.
     */
    @Column(name = "api_football_fixture_id")
    private Long apiFootballFixtureId;

    /**
     * Match id from the Highlightly Football API (soccer.highlightly.net),
     * used to pull starting lineups/formations — the one data point
     * api-football.com's free tier can't provide for World Cup 2026 (season
     * restricted) and ESPN's hidden scoreboard endpoint doesn't expose at all.
     * Null until matched by date + team names.
     */
    @Column(name = "highlightly_match_id")
    private Long highlightlyMatchId;

    /**
     * Raw JSON array, normalized into the same shape api-football.com used
     * ([{team:{name}, formation, startXI:[{player:{name,number}}], coach}, ...])
     * regardless of which provider (api-football.com or Highlightly) actually
     * supplied it, so match-detail.html's rendering JS needs no changes.
     */
    @Column(name = "lineups_json", columnDefinition = "TEXT")
    private String lineupsJson;

    /** Raw JSON array from api-football.com's /fixtures/statistics. */
    @Column(name = "statistics_json", columnDefinition = "TEXT")
    private String statisticsJson;

    /** Raw JSON array from api-football.com's /fixtures/events (the match timeline). */
    @Column(name = "events_json", columnDefinition = "TEXT")
    private String eventsJson;
}
