package com.zipcode.worldcuptracker.standings;

import java.util.List;
import java.util.Map;

/** Mid-tournament mock data — 48 teams, 12 groups. Swap for real API data later. */
public final class MockStandingsData {

    private MockStandingsData() {}

    private static RawStandingRow r(String slug, String name, String abbr,
                                    int w, int d, int l, int gf, int ga) {
        return new RawStandingRow(slug, name, abbr, w, d, l, gf, ga);
    }

    public static final Map<String, List<RawStandingRow>> GROUPS = Map.ofEntries(
        Map.entry("A", List.of(
            r("united-states", "United States", "USA", 2, 0, 0, 5, 1),
            r("uruguay",       "Uruguay",       "URU", 1, 1, 0, 3, 2),
            r("panama",        "Panama",        "PAN", 0, 1, 1, 1, 3),
            r("curacao",       "Curaçao",       "CUR", 0, 0, 2, 1, 5)
        )),
        Map.entry("B", List.of(
            r("canada",      "Canada",      "CAN", 1, 1, 0, 4, 2),
            r("colombia",    "Colombia",    "COL", 1, 1, 0, 3, 2),
            r("south-korea", "South Korea", "KOR", 0, 1, 1, 2, 3),
            r("ivory-coast", "Ivory Coast", "CIV", 0, 1, 1, 1, 3)
        )),
        Map.entry("C", List.of(
            r("mexico",      "Mexico",      "MEX", 2, 0, 0, 4, 1),
            r("brazil",      "Brazil",      "BRA", 1, 0, 1, 3, 2),
            r("iraq",        "Iraq",        "IRQ", 1, 0, 1, 2, 3),
            r("new-zealand", "New Zealand", "NZL", 0, 0, 2, 0, 4)
        )),
        Map.entry("D", List.of(
            r("argentina", "Argentina", "ARG", 2, 0, 0, 6, 1),
            r("japan",     "Japan",     "JPN", 1, 0, 1, 3, 3),
            r("australia", "Australia", "AUS", 1, 0, 1, 2, 3),
            r("algeria",   "Algeria",   "ALG", 0, 0, 2, 1, 5)
        )),
        Map.entry("E", List.of(
            r("england",      "England",      "ENG", 2, 0, 0, 5, 0),
            r("ecuador",      "Ecuador",      "ECU", 1, 0, 1, 3, 3),
            r("south-africa", "South Africa", "RSA", 1, 0, 1, 2, 3),
            r("saudi-arabia", "Saudi Arabia", "KSA", 0, 0, 2, 0, 4)
        )),
        Map.entry("F", List.of(
            r("france",   "France",   "FRA", 2, 0, 0, 7, 1),
            r("morocco",  "Morocco",  "MAR", 1, 1, 0, 3, 2),
            r("paraguay", "Paraguay", "PAR", 0, 1, 1, 2, 3),
            r("ghana",    "Ghana",    "GHA", 0, 0, 2, 1, 8)
        )),
        Map.entry("G", List.of(
            r("germany",    "Germany",    "GER", 2, 0, 0, 5, 1),
            r("egypt",      "Egypt",      "EGY", 1, 0, 1, 2, 3),
            r("uzbekistan", "Uzbekistan", "UZB", 1, 0, 1, 2, 3),
            r("cape-verde", "Cape Verde", "CPV", 0, 0, 2, 1, 4)
        )),
        Map.entry("H", List.of(
            r("spain",    "Spain",    "ESP", 2, 0, 0, 5, 0),
            r("dr-congo", "DR Congo", "COD", 1, 0, 1, 2, 2),
            r("tunisia",  "Tunisia",  "TUN", 1, 0, 1, 2, 3),
            r("haiti",    "Haiti",    "HAI", 0, 0, 2, 0, 4)
        )),
        Map.entry("I", List.of(
            r("portugal", "Portugal", "POR", 2, 0, 0, 6, 1),
            r("senegal",  "Senegal",  "SEN", 1, 0, 1, 3, 3),
            r("norway",   "Norway",   "NOR", 1, 0, 1, 2, 3),
            r("jordan",   "Jordan",   "JOR", 0, 0, 2, 0, 4)
        )),
        Map.entry("J", List.of(
            r("netherlands", "Netherlands", "NED", 1, 1, 0, 4, 2),
            r("belgium",     "Belgium",     "BEL", 1, 1, 0, 3, 2),
            r("iran",        "Iran",        "IRN", 0, 1, 1, 2, 3),
            r("qatar",       "Qatar",       "QAT", 0, 1, 1, 1, 3)
        )),
        Map.entry("K", List.of(
            r("croatia",  "Croatia",  "CRO", 1, 1, 0, 3, 2),
            r("scotland", "Scotland", "SCO", 1, 0, 1, 3, 3),
            r("sweden",   "Sweden",   "SWE", 1, 0, 1, 2, 2),
            r("austria",  "Austria",  "AUT", 0, 1, 1, 2, 3)
        )),
        Map.entry("L", List.of(
            r("switzerland",       "Switzerland",    "SUI", 1, 1, 0, 3, 2),
            r("turkiye",           "Türkiye",        "TUR", 1, 1, 0, 2, 1),
            r("czechia",           "Czech Republic", "CZE", 0, 1, 1, 1, 2),
            r("bosnia-herzegovina","Bosnia & Herz.", "BIH", 0, 1, 1, 1, 2)
        ))
    );

    public static final List<String> GROUP_ORDER =
        List.of("A","B","C","D","E","F","G","H","I","J","K","L");
}
