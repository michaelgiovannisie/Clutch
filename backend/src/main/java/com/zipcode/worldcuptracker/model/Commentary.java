package com.zipcode.worldcuptracker.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * Cached AI-generated pre-match color commentary for a single Match.
 * Generated lazily on first request and never regenerated for the same matchId.
 */
@Entity
public class Commentary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long matchId;

    @Lob
    @Column(nullable = false)
    private String content;

    private LocalDateTime generatedAt;

    public Commentary() {}

    public Commentary(Long matchId, String content, LocalDateTime generatedAt) {
        this.matchId = matchId;
        this.content = content;
        this.generatedAt = generatedAt;
    }

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public String getContent() { return content; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public void setContent(String content) { this.content = content; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
