package com.zipcode.worldcuptracker.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer apiId;
    private String name;
    private String country;
    private String flagUrl;
    private String groupLabel;
    private String coach;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<Player> players = new ArrayList<>();

    public Team() {}

    public Team(Integer apiId, String name, String country, String flagUrl, String groupLabel, String coach) {
        this.apiId = apiId;
        this.name = name;
        this.country = country;
        this.flagUrl = flagUrl;
        this.groupLabel = groupLabel;
        this.coach = coach;
    }

    public Long getId() { return id; }
    public Integer getApiId() { return apiId; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getFlagUrl() { return flagUrl; }
    public String getGroupLabel() { return groupLabel; }
    public String getCoach() { return coach; }
    public List<Player> getPlayers() { return players; }

    public void setId(Long id) { this.id = id; }
    public void setApiId(Integer apiId) { this.apiId = apiId; }
    public void setName(String name) { this.name = name; }
    public void setCountry(String country) { this.country = country; }
    public void setFlagUrl(String flagUrl) { this.flagUrl = flagUrl; }
    public void setGroupLabel(String groupLabel) { this.groupLabel = groupLabel; }
    public void setCoach(String coach) { this.coach = coach; }
    public void setPlayers(List<Player> players) { this.players = players; }
}
