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
}
