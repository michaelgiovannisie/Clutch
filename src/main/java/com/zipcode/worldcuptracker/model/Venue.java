package com.zipcode.worldcuptracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "venues")
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

    private String flag;

    private Integer capacity;

    private Double lat;

    private Double lng;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "country_color", length = 10)
    private String countryColor;

    public Venue() {}

    public Venue(Long id, String name, String city, String country, String flag,
                 Integer capacity, Double lat, Double lng, String imageUrl, String countryColor) {
        this.id = id; this.name = name; this.city = city; this.country = country;
        this.flag = flag; this.capacity = capacity; this.lat = lat; this.lng = lng;
        this.imageUrl = imageUrl; this.countryColor = countryColor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCountryColor() { return countryColor; }
    public void setCountryColor(String countryColor) { this.countryColor = countryColor; }
}
