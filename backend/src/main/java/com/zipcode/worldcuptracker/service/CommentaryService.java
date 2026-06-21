package com.zipcode.worldcuptracker.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.zipcode.worldcuptracker.model.Commentary;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.repository.CommentaryRepository;
import com.zipcode.worldcuptracker.repository.MatchRepository;

/**
 * Serves cached AI pre-match commentary, generating and persisting it once
 * per match on first request. The Groq API is never called again for a
 * match_id that already has a stored Commentary row.
 */
@Service
public class CommentaryService {

    private final CommentaryRepository commentaryRepository;
    private final MatchRepository matchRepository;
    private final GroqCommentaryService groqCommentaryService;

    public CommentaryService(
            CommentaryRepository commentaryRepository,
            MatchRepository matchRepository,
            GroqCommentaryService groqCommentaryService
    ) {
        this.commentaryRepository = commentaryRepository;
        this.matchRepository = matchRepository;
        this.groqCommentaryService = groqCommentaryService;
    }

    public Commentary getOrGenerate(Long matchId) {
        return commentaryRepository.findByMatchId(matchId)
                .orElseGet(() -> generateAndSave(matchId));
    }

    private Commentary generateAndSave(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Match not found: " + matchId));

        String content = groqCommentaryService.generate(match);
        Commentary commentary = new Commentary(matchId, content, LocalDateTime.now());
        return commentaryRepository.save(commentary);
    }
}
