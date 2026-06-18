package com.zipcode.worldcuptracker.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for "do these two team names refer to the same team"
 * across providers (football-data.org, ESPN, Highlightly, API-Football), each
 * of which formats/names countries differently.
 *
 * Previously this comparison was duplicated in MatchService, EspnService, and
 * HighlightlyService as: normalize punctuation away, then check substring
 * containment either direction (handles e.g. "Bosnia-Herzegovina" vs "Bosnia
 * and Herzegovina" vs "Bosnia & Herzegovina" - all share "bosnia herzegovina").
 *
 * That approach silently fails for true synonym pairs that share no common
 * substring at all - e.g. "USA" vs "United States", or "Czechia" vs "Czech
 * Republic", or "South Korea" vs "Korea Republic". Those aren't punctuation
 * differences, they're different words for the same country, so substring
 * matching can never bridge them. This was the actual cause of lineups (and
 * potentially events/stats) never syncing for matches involving those teams:
 * the mapping step that links a Highlightly/ESPN match id to our Match row
 * requires both team names to match, and silently found no match, leaving
 * the match's provider-id field permanently null with no error logged.
 *
 * Fix: canonicalize known synonym groups to one token before comparing, on
 * top of the existing substring fallback for everything else.
 */
final class TeamNameMatcher {

    private TeamNameMatcher() {}

    // Each inner array is a group of names/aliases that all refer to the same
    // team; normalized (lowercased, punctuation-stripped) before comparison.
    private static final String[][] SYNONYM_GROUPS = {
            {"usa", "united states", "united states of america"},
            {"south korea", "korea republic", "republic of korea"},
            {"north korea", "korea dpr", "democratic peoples republic of korea"},
            {"czechia", "czech republic"},
            {"ivory coast", "cote d ivoire", "cote divoire"},
            {"iran", "ir iran", "islamic republic of iran"},
            {"netherlands", "holland"},
            {"england", "three lions"}, // defensive; unlikely to appear but harmless
            {"turkey", "turkiye"}, // ESPN sends the native spelling "Türkiye"
    };

    private static final Map<String, String> CANONICAL_BY_ALIAS = buildAliasMap();

    private static Map<String, String> buildAliasMap() {
        Map<String, String> map = new java.util.HashMap<>();
        for (String[] group : SYNONYM_GROUPS) {
            String canonical = group[0];
            for (String alias : group) {
                map.put(alias, canonical);
            }
        }
        return Map.copyOf(map);
    }

    /**
     * Lowercased, diacritics stripped (so "Türkiye" reads as "turkiye" rather
     * than being blanked out to "t rkiye" by the old non-ASCII-to-space pass —
     * that gap is what let ESPN's "Türkiye" silently fail to match "Turkey"
     * for the Australia vs Turkey match), punctuation stripped down to
     * letters/digits/spaces, trimmed.
     */
    static String normalize(String s) {
        if (s == null) return "";
        String deAccented = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return deAccented.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /**
     * True if two raw team names (in any provider's formatting) refer to the
     * same team: either after synonym canonicalization, or via substring
     * containment (for punctuation/wording variants of the same name).
     */
    static boolean matches(String a, String b) {
        if (a == null || b == null) return false;
        String na = normalize(a);
        String nb = normalize(b);
        if (na.isBlank() || nb.isBlank()) return false;

        String ca = CANONICAL_BY_ALIAS.getOrDefault(na, na);
        String cb = CANONICAL_BY_ALIAS.getOrDefault(nb, nb);
        if (ca.equals(cb)) return true;

        return na.equals(nb) || na.contains(nb) || nb.contains(na);
    }
}
