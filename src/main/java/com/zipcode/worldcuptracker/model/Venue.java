package com.zipcode.worldcuptracker.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    /** Emoji flag, e.g. 🇺🇸 */
    private String flag;

    private Integer capacity;

    /** Decimal latitude */
    private Double lat;

    /** Decimal longitude */
    private Double lng;

    /** URL to a stadium photo */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /** Hex colour used on the map marker dot, e.g. #B22234 */
    @Column(name = "country_color", length = 10)
    private String countryColor;
}
