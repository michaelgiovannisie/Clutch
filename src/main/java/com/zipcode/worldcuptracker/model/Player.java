package com.zipcode.worldcuptracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID from football-data.org, used to avoid duplicate syncs. */
    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(nullable = false)
    private String name;

    /** e.g. "Goalkeeper", "Defence", "Midfield", "Offence" */
    private String position;

    private String nationality;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "shirt_number")
    private Integer shirtNumber;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}
