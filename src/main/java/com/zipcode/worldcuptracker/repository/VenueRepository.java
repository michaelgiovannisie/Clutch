package com.zipcode.worldcuptracker.repository;

import com.zipcode.worldcuptracker.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    List<Venue> findByCountryIgnoreCase(String country);

    List<Venue> findAllByOrderByCapacityDesc();
}
