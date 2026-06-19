package com.zipcode.worldcuptracker.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer apiId;

    @ManyToOne
    private Team homeTeam;

    @ManyToOne
    private Team awayTeam;

    @ManyToOne
    @JoinColumn(name = "venue_id")
    private Venue venue;

    private LocalDateTime kickoffTime;
    private Integer homeScore;
    private Integer awayScore;
    private String status;
    private String groupLabel;

    public Match() {}

    public Match(Integer apiId, Team homeTeam, Team awayTeam, LocalDateTime kickoffTime,
                 Integer homeScore, Integer awayScore, String status, String groupLabel) {
        this.apiId = apiId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.kickoffTime = kickoffTime;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = status;
        this.groupLabel = groupLabel;
    }

    public Long getId() { return id; }
    public Integer getApiId() { return apiId; }
    public Team getHomeTeam() { return homeTeam; }
    public Team getAwayTeam() { return awayTeam; }
    public LocalDateTime getKickoffTime() { return kickoffTime; }
    public Integer getHomeScore() { return homeScore; }
    public Integer getAwayScore() { return awayScore; }
    public String getStatus() { return status; }
    public String getGroupLabel() { return groupLabel; }
    public Venue getVenue() { return venue;}

    public void setId(Long id) { this.id = id; }
    public void setApiId(Integer apiId) { this.apiId = apiId; }
    public void setHomeTeam(Team homeTeam) { this.homeTeam = homeTeam; }
    public void setAwayTeam(Team awayTeam) { this.awayTeam = awayTeam; }
    public void setKickoffTime(LocalDateTime kickoffTime) { this.kickoffTime = kickoffTime; }
    public void setHomeScore(Integer homeScore) { this.homeScore = homeScore; }
    public void setAwayScore(Integer awayScore) { this.awayScore = awayScore; }
    public void setStatus(String status) { this.status = status; }
    public void setGroupLabel(String groupLabel) { this.groupLabel = groupLabel; }
    public void setVenue(Venue venue) { this.venue = venue;} 
}