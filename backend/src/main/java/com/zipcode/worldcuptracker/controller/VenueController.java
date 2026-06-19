package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.Venue;
import com.zipcode.worldcuptracker.repository.MatchRepository;
import com.zipcode.worldcuptracker.repository.VenueRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
@CrossOrigin(origins = "http://localhost:5173")
public class VenueController {

    private final VenueRepository venueRepository;
    private final MatchRepository matchRepository;

    public VenueController(
            VenueRepository venueRepository,
            MatchRepository matchRepository
    ) {
        this.venueRepository = venueRepository;
        this.matchRepository = matchRepository;
    }

    @GetMapping
    public List<Venue> getVenues() {
        return venueRepository.findAll();
    }

    @GetMapping("/{id}")
    public Venue getVenue(@PathVariable Long id) {
        return venueRepository.findById(id).orElseThrow();
    }

    @GetMapping("/{id}/matches")
    public List<Match> getMatchesAtVenue(@PathVariable Long id) {
        return matchRepository.findByVenueId(id);
    }
}
