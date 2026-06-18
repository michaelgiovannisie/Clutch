package com.zipcode.worldcuptracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zipcode.worldcuptracker.model.Commentary;
import com.zipcode.worldcuptracker.model.Match;
import com.zipcode.worldcuptracker.model.Team;
import com.zipcode.worldcuptracker.repository.CommentaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Generates pre-match tactical "color commentary" via the Google Gemini API
 * (chosen over Anthropic/OpenAI because its free tier — 1,500 requests/day,
 * no credit card — comfortably covers this app's needs: one short generation
 * per match, cached forever after).
 *
 * Follows the same java.net.http.HttpClient + Jackson pattern as
 * EspnService/HighlightlyService/ApiFootballService for consistency, rather
 * than pulling in a dedicated SDK.
 *
 * Per the README's design intent, the prompt is explicitly engineered to be
 * non-predictive: tactical/stylistic analysis only, no winner or scoreline
 * guesses. Generated text is cached in the `commentary` table keyed by
 * match_id so each match is only ever sent to Gemini once.
 */
@Service
public class CommentaryService {

    private static final Logger log = LoggerFactory.getLogger(CommentaryService.class);

    private static final String MODEL = "gemini-2.5-flash";
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

    private final CommentaryRepository commentaryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${api.gemini.key:}")
    private String apiKey;

    public CommentaryService(CommentaryRepository commentaryRepository) {
        this.commentaryRepository = commentaryRepository;
    }

    /**
     * Returns the cached commentary for this match if it exists, otherwise
     * generates it via Gemini, caches it, and returns the new row. Throws
     * IllegalStateException if no real API key is configured, or
     * RuntimeException if the Gemini call itself fails — callers should
     * surface these as a friendly "not available yet" message rather than
     * a 500, since this is a nice-to-have feature, not core data.
     */
    public Commentary getOrGenerate(Match match) {
        return getOrGenerate(match, false);
    }

    /**
     * @param regenerate if true, discards any cached commentary for this
     *                    match and generates fresh — used to pick up prompt
     *                    changes for matches whose commentary was already
     *                    cached under the old prompt.
     */
    public Commentary getOrGenerate(Match match, boolean regenerate) {
        Optional<Commentary> existing = commentaryRepository.findByMatchId(match.getId());
        if (existing.isPresent() && !regenerate) {
            return existing.get();
        }
        existing.ifPresent(commentaryRepository::delete);

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GEMINI_KEY")) {
            throw new IllegalStateException("No api.gemini.key configured in application.properties.");
        }
        if (match.getHomeTeam() == null || match.getAwayTeam() == null) {
            throw new IllegalStateException("Commentary needs both teams to be known; this fixture's teams aren't determined yet.");
        }

        String text = callGemini(buildPrompt(match));

        Commentary commentary = Commentary.builder()
                .matchId(match.getId())
                .content(text)
                .generatedAt(OffsetDateTime.now())
                .build();
        return commentaryRepository.save(commentary);
    }

    /**
     * Builds the prompt per the README's "AI Commentary — Design Decisions"
     * pattern: tactical/stylistic analysis only, explicitly forbidding a
     * winner prediction, broadcast-analyst tone.
     */
    private String buildPrompt(Match match) {
        Team home = match.getHomeTeam();
        Team away = match.getAwayTeam();

        StringBuilder sb = new StringBuilder();
        sb.append("You are a football analyst providing pre-match color commentary.\n");
        sb.append("Analyze how ").append(home.getName()).append(" and ").append(away.getName())
                .append(" might match up tactically.\n");
        if (match.getGroupName() != null && !match.getGroupName().isBlank()) {
            sb.append("This is a FIFA World Cup 2026 group stage match, Group ").append(match.getGroupName()).append(".\n");
        } else if (match.getStage() != null) {
            sb.append("This is a FIFA World Cup 2026 ").append(match.getStage().replace('_', ' ').toLowerCase()).append(" match.\n");
        }
        sb.append("Do NOT predict a winner, a scoreline, or suggest one team is more likely to win.\n");
        sb.append("Keep the tone engaging, like a broadcast analyst before kickoff.\n");
        sb.append("Write exactly 3 short paragraphs, separated by a single blank line, plain text with no markdown ")
                .append("formatting (no asterisks, no headers, no bullet points):\n");
        sb.append("1) 2-3 sentences contrasting the two teams' overall playing styles/identities.\n");
        sb.append("2) 2-3 sentences on the specific tactical matchups to watch (e.g. one team's strength against the other's weakness).\n");
        sb.append("3) 2-3 sentences on each team's biggest vulnerability or open question heading into the match.\n");
        sb.append("Keep each paragraph punchy and concrete rather than a wall of text.");
        return sb.toString();
    }

    private String callGemini(String prompt) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode contentNode = contents.addObject();
        ArrayNode parts = contentNode.putArray("parts");
        parts.addObject().put("text", prompt);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "?key=" + apiKey))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String text = json.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) {
                throw new RuntimeException("Gemini returned an empty response: " + response.body());
            }
            return text.trim();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to call Gemini for commentary: {}", e.getMessage());
            throw new RuntimeException("Failed to call Gemini: " + e.getMessage(), e);
        }
    }
}
