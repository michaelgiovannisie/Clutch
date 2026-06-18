package com.zipcode.worldcuptracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Cached AI-generated pre-match tactical commentary for a single match.
 * Generated once (on first request for a given match) and stored here so
 * repeat page loads don't re-call the AI provider — see CommentaryService.
 */
@Entity
@Table(name = "commentary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commentary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, unique = true)
    private Long matchId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;
}
