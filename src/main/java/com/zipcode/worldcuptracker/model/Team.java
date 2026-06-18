package com.zipcode.worldcuptracker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID from football-data.org, used to avoid duplicate syncs. Nullable for manually-added teams. */
    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(nullable = false)
    private String name;

    /** Three-letter code, e.g. "BRA" */
    private String tla;

    /** URL to the country's flag image (SVG), from football-data.org's area.flag */
    @Column(name = "flag", length = 512)
    private String flag;

    /** URL to the team's crest/badge image */
    @Column(name = "crest_url", length = 512)
    private String crestUrl;

    /** World Cup group letter, e.g. "A" */
    private String groupName;

    private String coach;
}
