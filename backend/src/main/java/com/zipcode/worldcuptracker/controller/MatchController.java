package com.zipcode.worldcuptracker.controller;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zipcode.worldcuptracker.model.Commentary;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.repository.MatchRepository;
import com.zipcode.worldcuptracker.service.CommentaryService;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "http://localhost:5173")
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);

    private final MatchRepository matchRepository;
    private final CommentaryService commentaryService;

    public MatchController(MatchRepository matchRepository, CommentaryService commentaryService) {
        this.matchRepository = matchRepository;
        this.commentaryService = commentaryService;
    }

    @GetMapping
    public List<Match> getMatches() {
        return matchRepository.findAll();
    }

    @GetMapping("/{id}")
    public Match getMatchById(@PathVariable Long id) {
        return matchRepository.findById(id).orElseThrow();
    }

    /**
     * Pre-match AI color commentary, cached per match_id. The first request
     * for a match generates it via Gemini and stores it; every later request
     * just reads the cached row (the Gemini API is never called twice for
     * the same match).
     */
    @GetMapping("/{id}/commentary")
    public ResponseEntity<?> getCommentary(@PathVariable Long id) {
        try {
            Commentary commentary = commentaryService.getOrGenerate(id);
            return ResponseEntity.ok(Map.of(
                    "content", commentary.getContent(),
                    "generatedAt", commentary.getGeneratedAt()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn("Commentary generation failed for match {}: {}", id, e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Commentary unavailable right now."
            ));
        }
    }
}
