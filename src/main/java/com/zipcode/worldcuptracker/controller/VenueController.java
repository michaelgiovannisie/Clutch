package com.zipcode.worldcuptracker.controller;

import com.zipcode.worldcuptracker.model.Venue;
import com.zipcode.worldcuptracker.service.VenueService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class VenueController {

    private final VenueService venueService;
    private final ResourceLoader resourceLoader;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VenueController(VenueService venueService, ResourceLoader resourceLoader) {
        this.venueService = venueService;
        this.resourceLoader = resourceLoader;
    }

    // ── Thymeleaf page ────────────────────────────────────────────────────────

    @GetMapping("/venues")
    public String venueExplorer(Model model) {
        model.addAttribute("venueCount", venueService.getAllVenues().size());
        return "venues/venue-explorer";
    }

    @GetMapping("/venues/{id}")
    public String venueDetail(@PathVariable Long id, Model model) {
        return venueService.getVenueById(id)
                .map(venue -> {
                    model.addAttribute("venue", venue);
                    return "venues/venue-detail";
                })
                .orElse("redirect:/venues");
    }

    // ── REST endpoints ────────────────────────────────────────────────────────

    @GetMapping("/api/venues")
    @ResponseBody
    public ResponseEntity<List<Venue>> apiVenues(
            @RequestParam(required = false) String country) {

        List<Venue> venues = (country != null && !country.isBlank())
                ? venueService.getVenuesByCountry(country)
                : venueService.getAllVenues();

        return ResponseEntity.ok(venues);
    }

    @GetMapping("/api/venues/{id}")
    @ResponseBody
    public ResponseEntity<Venue> apiVenueById(@PathVariable Long id) {
        return venueService.getVenueById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/venues/{id}/images")
    @ResponseBody
    public ResponseEntity<List<String>> apiVenueImages(@PathVariable Long id) {
        return venueService.getVenueById(id).map(venue -> {
            String folder = venue.getImageUrl();
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

    @GetMapping("/api/venues/{id}/landmark-images")
    @ResponseBody
    public ResponseEntity<List<String>> apiLandmarkImages(@PathVariable Long id) {
        String folder = "/images/landmarks/" + id;
        try {
            Resource resource = resourceLoader.getResource("classpath:static" + folder);
            if (!resource.exists()) return ResponseEntity.ok(Collections.emptyList());
            File dir = resource.getFile();
            File[] files = dir.listFiles(f -> !f.getName().startsWith("."));
            if (files == null) return ResponseEntity.ok(Collections.emptyList());
            Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            List<String> urls = Arrays.stream(files)
                    .map(f -> folder + "/" + f.getName())
                    .collect(Collectors.toList());
            return ResponseEntity.ok(urls);
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/api/venues/{id}/attraction-images")
    @ResponseBody
    public ResponseEntity<List<List<String>>> apiAttractionImages(@PathVariable Long id) {
        String base = "/images/attractions/" + id;
        try {
            Resource resource = resourceLoader.getResource("classpath:static" + base);
            if (!resource.exists()) return ResponseEntity.ok(Collections.emptyList());

            File dir = resource.getFile();
            File[] subDirs = dir.listFiles(f -> f.isDirectory() && !f.getName().startsWith("."));
            if (subDirs == null) return ResponseEntity.ok(Collections.emptyList());

            Arrays.sort(subDirs, (a, b) -> {
                try {
                    return Integer.compare(Integer.parseInt(a.getName()), Integer.parseInt(b.getName()));
                } catch (NumberFormatException e) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });

            List<List<String>> result = new ArrayList<>();
            for (File sub : subDirs) {
                File[] files = sub.listFiles(f -> !f.getName().startsWith("."));
                List<String> urls = new ArrayList<>();
                if (files != null) {
                    Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    for (File f : files) urls.add(base + "/" + sub.getName() + "/" + f.getName());
                }
                result.add(urls);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/api/venues/{id}/weather")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiVenueWeather(@PathVariable Long id) {
        return venueService.getVenueById(id).map(venue -> {
            try {
                String url = String.format(
                        "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
                                "&current=temperature_2m,weather_code,wind_speed_10m" +
                                "&temperature_unit=fahrenheit&wind_speed_unit=mph",
                        venue.getLat(), venue.getLng());

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return ResponseEntity.ok(Collections.<String, Object>emptyMap());
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode current = root.path("current");
                int code = current.path("weather_code").asInt(-1);

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("tempF", current.path("temperature_2m").asDouble());
                result.put("windMph", current.path("wind_speed_10m").asDouble());
                result.put("code", code);
                result.put("label", weatherLabel(code));
                result.put("icon", weatherIcon(code));
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                return ResponseEntity.ok(Collections.<String, Object>emptyMap());
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    private static String weatherLabel(int code) {
        if (code == 0) return "Clear sky";
        if (code <= 2) return "Partly cloudy";
        if (code == 3) return "Overcast";
        if (code == 45 || code == 48) return "Foggy";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 80 && code <= 82) return "Rain showers";
        if (code >= 85 && code <= 86) return "Snow showers";
        if (code >= 95 && code <= 99) return "Thunderstorm";
        return "Unknown";
    }

    private static String weatherIcon(int code) {
        if (code == 0) return "☀️";
        if (code <= 2) return "🌤️";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if (code >= 51 && code <= 57) return "🌦️";
        if (code >= 61 && code <= 67) return "🌧️";
        if (code >= 71 && code <= 77) return "❄️";
        if (code >= 80 && code <= 82) return "🌧️";
        if (code >= 85 && code <= 86) return "🌨️";
        if (code >= 95 && code <= 99) return "⛈️";
        return "🌡️";
    }
}
