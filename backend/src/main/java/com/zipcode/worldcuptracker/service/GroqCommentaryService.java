package com.zipcode.worldcuptracker.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.zipcode.worldcuptracker.model.Match;

/**
 * Generates pre-match, non-predictive tactical color commentary for a Match
 * using Groq's OpenAI-compatible chat completions API. Called lazily (once
 * per match) — see CommentaryService, which caches the result.
 */
@Service
public class GroqCommentaryService {

    private static final String SYSTEM_PROMPT = """
            You are a football (soccer) broadcast analyst giving pre-match color commentary
            for a FIFA World Cup 2026 match, in the voice of a TV analyst moments before kickoff.

            Write ONE engaging paragraph of about 4-6 sentences. No headings, no bullet points,
            no lists - just the paragraph.

            Strict rules:
            - Use ONLY the facts given in the match data. Do not add statistics, results, dates,
              player names, or any claim that is not provided. If something isn't given, keep it
              general instead of inventing specifics.
            - Do NOT predict or imply an outcome: no winner, no "favorite", no "edge", no scoreline,
              no betting language. Give both teams their due and stay neutral.
            - Focus on the stylistic and tactical contrast between the two sides and what makes the
              matchup compelling to watch.
            - Keep it spoiler-free and present-tense, as if the match has not started.
            - Output only the commentary paragraph - nothing else.
            """;

    // Loose guardrail scan for prediction/leakage language.
    private static final Pattern LEAKAGE_PATTERN = Pattern.compile(
            "(?i)\\b(will win|favou?rite|underdog|likely to (win|beat|lose)|" +
            "edge over|expected to (win|beat)|odds (are|favor)|\\d\\s*-\\s*\\d)\\b"
    );

    private final RestClient restClient;
    private final String model;

    public GroqCommentaryService(
            @Value("${api.groq.base-url}") String baseUrl,
            @Value("${api.groq.key}") String apiKey,
            @Value("${api.groq.model}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.model = model;
    }

    public String generate(Match match) {
        String userPrompt = buildUserPrompt(match);

        String text = callGroq(userPrompt);
        if (text != null && LEAKAGE_PATTERN.matcher(text).find()) {
            // One retry with a stronger reminder, per the commentary spec's guardrail.
            text = callGroq(userPrompt + "\n\nReminder: do not predict a winner, favorite, "
                    + "edge, or scoreline. Stay strictly neutral.");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Groq returned no commentary text");
        }
        return text.trim();
    }

    private String buildUserPrompt(Match match) {
        String homeName = match.getHomeTeam() != null ? match.getHomeTeam().getName() : "Home";
        String awayName = match.getAwayTeam() != null ? match.getAwayTeam().getName() : "Away";
        String group = match.getGroupLabel() != null ? match.getGroupLabel() : "the knockout stage";

        return """
                Match: %s vs %s
                Group: %s

                Write the pre-match color commentary.
                """.formatted(homeName, awayName, group);
    }

    @SuppressWarnings("unchecked")
    private String callGroq(String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.65,
                "max_tokens", 300
        );

        Map<String, Object> response = restClient.post()
                .uri("/openai/v1/chat/completions")
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null) return null;

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return null;

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null) return null;

        Object content = message.get("content");
        return content != null ? content.toString() : null;
    }
}
