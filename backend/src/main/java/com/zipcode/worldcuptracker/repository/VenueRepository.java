package com.zipcode.worldcuptracker.repository;

import com.zipcode.worldcuptracker.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    Optional<Venue> findByApiId(Integer apiId);
    Optional<Venue> findByNameAndCity(String name, String city);
}