package com.zipcode.worldcuptracker.service;

import com.zipcode.worldcuptracker.model.Venue;
import com.zipcode.worldcuptracker.repository.VenueRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
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

    /**
     * Some providers report a stadium under a name that shares no substring with
     * our seeded Venue table's name, so plain containment matching always misses.
     * Covers two distinct cases: (1) FIFA's host-stadium "debranding" policy,
     * which renames stadiums to a neutral, sponsor-free name for the tournament
     * (e.g. MetLife Stadium -> "New York New Jersey Stadium"), and (2) a stadium
     * that's been renamed for an unrelated sponsorship deal since we seeded it
     * (e.g. Estadio Azteca -> "Estadio Banorte", per ESPN's live feed). Maps each
     * alternate name (lowercase) to the name we seeded, checked before falling
     * back to containment.
     */
    private static final java.util.Map<String, String> DEBRANDED_VENUE_NAMES = java.util.Map.ofEntries(
            java.util.Map.entry("new york new jersey stadium", "metlife stadium"),
            java.util.Map.entry("los angeles stadium", "sofi stadium"),
            java.util.Map.entry("dallas stadium", "at&t stadium"),
            java.util.Map.entry("san francisco bay area stadium", "levi's stadium"),
            java.util.Map.entry("miami stadium", "hard rock stadium"),
            java.util.Map.entry("boston stadium", "gillette stadium"),
            java.util.Map.entry("philadelphia stadium", "lincoln financial field"),
            java.util.Map.entry("kansas city stadium", "arrowhead stadium"),
            java.util.Map.entry("seattle stadium", "lumen field"),
            java.util.Map.entry("toronto stadium", "bmo field"),
            java.util.Map.entry("estadio guadalajara", "estadio akron"),
            java.util.Map.entry("estadio monterrey", "estadio bbva"),
            java.util.Map.entry("estadio banorte", "estadio azteca")
    );

    /**
     * Matches a free-text stadium name (from any provider) against our seeded
     * Venue table. Checks the FIFA debranding alias map first, then falls back
     * to loose name containment (handles cases like "BC Place Vancouver" /
     * "Estadio Azteca Mexico City" that keep their original name but with a
     * city suffix appended). Shared across providers — football-data.org's
     * free tier sends no venue at all for World Cup matches, so api-football.com
     * is the provider that actually exercises this for now.
     */
    public Optional<Venue> resolveByName(String venueName) {
        if (venueName == null || venueName.isBlank()) {
            return Optional.empty();
        }
        String needle = venueName.toLowerCase(Locale.ROOT);
        String aliased = DEBRANDED_VENUE_NAMES.get(needle);
        List<Venue> all = venueRepository.findAll();
        if (aliased != null) {
            for (Venue v : all) {
                if (v.getName().toLowerCase(Locale.ROOT).equals(aliased)) {
                    return Optional.of(v);
                }
            }
        }
        for (Venue v : all) {
            String name = v.getName().toLowerCase(Locale.ROOT);
            if (name.contains(needle) || needle.contains(name)) {
                return Optional.of(v);
            }
        }
        return Optional.empty();
    }
}
