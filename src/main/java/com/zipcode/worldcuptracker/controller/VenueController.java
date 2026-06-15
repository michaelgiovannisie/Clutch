package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.model.Venue;
import com.zipcode.worldcuptracker.service.VenueService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class VenueController {

    private final VenueService venueService;
    private final ResourceLoader resourceLoader;

    public VenueController(VenueService venueService, ResourceLoader resourceLoader) {
        this.venueService = venueService;
        this.resourceLoader = resourceLoader;
    }

    // ── Thymeleaf page ────────────────────────────────────────────────────────

    /** Redirect root to the venue explorer. */
    @GetMapping("/")
    public String root() {
        return "redirect:/venues";
    }

    /**
     * Renders the interactive Venue Explorer page.
     * GET /venues
     */
    @GetMapping("/venues")
    public String venueExplorer(Model model) {
        model.addAttribute("venueCount", venueService.getAllVenues().size());
        return "venues/venue-explorer";
    }

    /**
     * Renders the Venue Detail page.
     * GET /venues/{id}
     */
    @GetMapping("/venues/{id}")
    public String venueDetail(@PathVariable Long id, Model model) {
        return venueService.getVenueById(id)
                .map(venue -> {
                    model.addAttribute("venue", venue);
                    return "venues/venue-detail";
                })
                .orElse("redirect:/venues");
    }

    // ── REST endpoints (consumed by Leaflet JS) ───────────────────────────────

    /**
     * Returns all venues as JSON.
     * GET /api/venues
     *
     * Optional query param: ?country=USA
     */
    @GetMapping("/api/venues")
    @ResponseBody
    public ResponseEntity<List<Venue>> apiVenues(
            @RequestParam(required = false) String country) {

        List<Venue> venues = (country != null && !country.isBlank())
                ? venueService.getVenuesByCountry(country)
                : venueService.getAllVenues();

        return ResponseEntity.ok(venues);
    }

    /**
     * Returns a single venue by id.
     * GET /api/venues/{id}
     */
    @GetMapping("/api/venues/{id}")
    @ResponseBody
    public ResponseEntity<Venue> apiVenueById(@PathVariable Long id) {
        return venueService.getVenueById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists image URLs for a venue by scanning its static folder.
     * GET /api/venues/{id}/images
     * Returns e.g. ["/images/USA/MetLife/1.jpg", "/images/USA/MetLife/2.jpeg", ...]
     */
    @GetMapping("/api/venues/{id}/images")
    @ResponseBody
    public ResponseEntity<List<String>> apiVenueImages(@PathVariable Long id) {
        return venueService.getVenueById(id).map(venue -> {
            String folder = venue.getImageUrl(); // e.g. "/images/USA/MetLife"
            if (folder == null || folder.isBlank()) return ResponseEntity.ok(Collections.<String>emptyList());

            try {
                Resource resource = resourceLoader.getResource("classpath:static" + folder);
                if (!resource.exists()) return ResponseEntity.ok(Collections.<String>emptyList());

                File dir = resource.getFile();
                File[] files = dir.listFiles(f -> !f.getName().startsWith("."));
                if (files == null) return ResponseEntity.ok(Collections.<String>emptyList());

                Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                List<String> urls = Arrays.stream(files)
                        .map(f -> folder + "/" + f.getName())
                        .collect(Collectors.toList());

                return ResponseEntity.ok(urls);
            } catch (Exception e) {
                return ResponseEntity.ok(Collections.<String>emptyList());
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
