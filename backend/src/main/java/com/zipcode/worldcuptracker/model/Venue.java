package com.zipcode.worldcuptracker.model;

import jakarta.persistence.*;

@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Integer apiId;

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

    public Long getId() { return id; }
    public Integer getApiId() { return apiId; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public String getFlag() { return flag; }
    public Integer getCapacity() { return capacity; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public String getImageUrl() { return imageUrl; }
    public String getCountryColor() { return countryColor; }

    public void setId(Long id) { this.id = id; }
    public void setApiId(Integer apiId) { this.apiId = apiId; }
    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setCountry(String country) { this.country = country; }
    public void setFlag(String flag) { this.flag = flag; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public void setLat(Double lat) { this.lat = lat; }
    public void setLng(Double lng) { this.lng = lng; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCountryColor(String countryColor) { this.countryColor = countryColor; }
}
