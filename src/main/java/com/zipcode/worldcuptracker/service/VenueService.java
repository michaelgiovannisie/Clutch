package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.model.Venue;
import com.zipcode.worldcuptracker.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    /** All venues sorted by capacity descending. */
    public List<Venue> getAllVenues() {
        return venueRepository.findAllByOrderByCapacityDesc();
    }

    /** Venues filtered by host country (case-insensitive). */
    public List<Venue> getVenuesByCountry(String country) {
        return venueRepository.findByCountryIgnoreCase(country);
    }

    /** Single venue by id. */
    public Optional<Venue> getVenueById(Long id) {
        return venueRepository.findById(id);
    }
}
