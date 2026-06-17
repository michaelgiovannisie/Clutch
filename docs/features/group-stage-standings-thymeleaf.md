# Group Stage Standings (Thymeleaf) — Implementation Brief

> **For:** Claude Code. **Owner:** Joe. **Target branch:** `combinebranch`.
> **Scope:** The **Group Stage** tab of the `/standings` page, ported from Joe2's React version into combinebranch's Thymeleaf app. Companion brief: `bracket-thymeleaf.md` (Bracket tab). Behavior reference: `standings-bracket.md` (the React original).
> This is a spec, not code.

## 1. Goal

A `/standings` page in combinebranch (Spring Boot + Thymeleaf) whose **Group Stage** tab shows all 12 groups with live "if it ended today" advancement, matching combinebranch's existing UI. This brief also builds the **shared page shell** (controller, template, tab switcher, nav) that the Bracket tab plugs into.

## 2. Target stack & conventions (match these)

combinebranch is Spring Boot + **Thymeleaf** + Spring Data JPA + Lombok, Java 21, package `com.zipcode.worldcuptracker`. Mirror the existing `VenueController`/`VenueService`/`VenueRepository` pattern; templates in `src/main/resources/templates/`, static assets in `src/main/resources/static/`.

**UI style — reuse combinebranch's look** (from `templates/venues/venue-explorer.html`): inline `<style>`, warm gradient + glassmorphism, tokens `--orange:#FF6B35; --peach:#FFAA70; --pink:#E8407A; --gold:#FFB830; --dark:#2D1200; --panel:rgba(255,255,255,0.93); --radius:14px;`, glass header with `backdrop-filter: blur`, **filter pills** with an `.active` state (white bg / orange text), `'Segoe UI'`, bold uppercase titles. Do **not** use Joe2's dark React theme.

## 3. Page shell (built here, shared with the Bracket tab)

- `StandingsController` → `@GetMapping("/standings")` returning view `standings/standings`, populating model attributes (group-stage ones listed in §5; bracket ones added by the companion brief).
- Template `templates/standings/standings.html` with two **pill tabs**: "Group Stage" (default active) and "Bracket". Toggle the two sections with a small inline JS class switch (same pattern as the venue filter pills) — no SPA router.
- Add a nav link/pill to `/standings` in the venue pages' header (and back), so it lives inside combinebranch's UI.

## 4. Group Stage tab

- Responsive grid of **all 12 groups (A–L)**, each a glass-panel mini standings table.
- Columns: **Pos · Team (flag + abbr; full name on wide screens) · P · W · D · L · GF · GA · GD · Pts** (Pts emphasized; GF/GA may hide on narrow widths).
- **Advancement coloring** per row (left rail / Pos cell):
  - advancing (rank 1–2) — green
  - wild-card in (rank 3 within the top-8 best-thirds cutoff) — gold/amber (fits the theme)
  - out (rank 3 outside cutoff, or rank 4) — muted red
- A **Legend** explaining the three states.
- A **Third-place race** panel: all 12 third-placed teams ranked, with a **cutoff line after 8** (the MLB wild-card board); same colors.

## 5. Server-side logic (Java)

- `StandingsService` (pure, testable):
  - rank a group by **Points → Goal difference → Goals scored → head-to-head (refinement) → alphabetical**.
  - rank the 12 third-placed teams by **Points → GD → Goals scored → fair-play → drawing of lots**; top 8 = "wild-card in".
- **View models** (Lombok): `GroupStandingView`, `StandingRowView` (slug, name, abbr, played, won, drawn, lost, gf, ga, gd, points, state), `ThirdPlaceRow`.
- **Data source (now = mock):** a seed component or `data.sql` with the 12 groups' current P/W/D/L/GF/GA. Keep `StandingsService` independent of the source so it can later compute from real `Match` results with no UI change.
- Model attributes for the template: `groups` (12 `GroupStandingView`), `thirdPlaceRace` (ranked 12 with cutoff flag).

> The qualifiers/thirds computed here are also consumed by the Bracket tab — expose them from `StandingsService` so `bracket-thymeleaf.md` can reuse them.

## 6. Format rules to encode

12 groups of 4; **top 2 per group + 8 best thirds = 32** advance. (The 8-best-thirds ranking is what drives both the wild-card coloring here and the projected bracket in the companion brief.)

## 7. Assets

Copy `assets/flags/*.jpg` → `src/main/resources/static/flags/` and `assets/crests/*.png` → `src/main/resources/static/crests/`. Reference by slug, e.g. `<img th:src="@{'/flags/' + ${row.slug} + '.jpg'}">`. `slug` matches filenames (`united-states`, `dr-congo`, `curacao`, …); crest falls back to placeholder if missing.

## 8. Acceptance criteria

- `mvn spring-boot:run` works; `GET /standings` renders inside combinebranch's UI (orange/pink glass, pills), reachable from the venue pages.
- Group Stage tab: all 12 groups correctly ranked, advancement colors + Legend, third-place race board with cutoff after 8.
- Standings math isolated in `StandingsService` (unit-testable), independent of the data source.
- Visually consistent with `venue-explorer.html`. No React, no dark theme.

## 9. Out of scope

- The **Bracket tab** content → `bracket-thymeleaf.md`.
- Real `Match`-derived standings — mock/seed now.
