package com.zipcode.worldcuptracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // API-Football player id
    @Column(unique = true)
    private Integer apiId;

    // Full display name
    private String name;

    // Store separately as well
    private String firstName;
    private String lastName;

    private String position;
    private Integer shirtNumber;
    private Integer age;

    @Column(length = 512)
    private String photoUrl;

    @ManyToOne
    @JoinColumn(name = "team_id")
    @JsonIgnore
    private Team team;

    public Player() {}

    public Player(String name, String position, Integer shirtNumber, Team team) {
        this.name = name;
        this.position = position;
        this.shirtNumber = shirtNumber;
        this.team = team;
    }

    public Long getId() {
        return id;
    }

    public Integer getApiId() {
        return apiId;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPosition() {
        return position;
    }

    public Integer getShirtNumber() {
        return shirtNumber;
    }

    public Integer getAge() {
        return age;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public Team getTeam() {
        return team;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setApiId(Integer apiId) {
        this.apiId = apiId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setShirtNumber(Integer shirtNumber) {
        this.shirtNumber = shirtNumber;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
