# Bracket — Projected R32 (Thymeleaf) — Implementation Brief

> **For:** Claude Code. **Owner:** Joe. **Target branch:** `combinebranch`.
> **Scope:** The **Bracket** tab of the `/standings` page, ported into combinebranch's Thymeleaf app. Depends on `group-stage-standings-thymeleaf.md` (page shell + standings computation) and `bracket-reference.md` (the R32 data). Behavior reference: `standings-bracket.md`.
> This is a spec, not code.

## 1. Goal

The **Bracket** tab on the `/standings` page shows a **projected Round of 32** computed from current standings — a list now, not a visual tree. "If the tournament started today."

## 2. Depends on

- **Page shell + tabs + nav:** built in `group-stage-standings-thymeleaf.md` (the "Bracket" pill toggles this section).
- **Standings computation:** reuse `StandingsService` (group ranks, the 8 best thirds) from that brief — do not recompute.
- **Bracket data:** `bracket-reference.md` provides the fixed 16-match R32 skeleton, the per-slot third-place constraint sets, and how to source the FIFA 495-scenario allocation.
- **Stack/UI conventions:** same as the group-stage brief (combinebranch Thymeleaf, orange/pink glass theme, pills). Don't restate — follow it.

## 3. Bracket tab content

- **Projected to qualify** — three labeled columns: Group Winners (12), Runners-up (12), Best Thirds (8, with the cutoff line). Each entry = flag + name + group.
- **Projected Round of 32** — the 16 matchups as a list (each line: both teams' flags + names). Built from the §2 skeleton.
- Header note: "Projected — based on standings as of today. Knockouts begin June 28."

## 4. Server-side logic (Java)

- `BracketService` (pure, testable) that, given `StandingsService` output:
  1. computes the 12 group winners (`1X`), 12 runners-up (`2X`), and the 8 best thirds;
  2. fills the **fixed R32 skeleton** (`BracketTemplate`, matches 73–88 — see `bracket-reference.md`);
  3. resolves the 8 "vs 3rd place" slots via the **third-place allocation** (`ThirdPlaceAllocation`, keyed by the sorted 8-group combination).
- **View models** (Lombok): `ProjectedMatchup` (match no., home, away, both resolvable to flag/name or a placeholder label), plus the three qualifier lists.
- Model attributes for the template: `projectedWinners`, `projectedRunnersUp`, `projectedBestThirds`, `projectedR32`.

## 5. Bracket data handling (critical)

- Encode `BracketTemplate` (the 16 fixed matchups) verbatim from `bracket-reference.md` §2.
- Encode `ThirdPlaceAllocation` from FIFA's official 495-scenario table — **do not fabricate** the mapping.
- **Until the allocation table is populated:** render the fixed slots (winners + runner-up pairings) fully, and for the 8 third-place slots either show the `3 of {set}` label or a **provisional** constraint-satisfaction assignment clearly marked "provisional — pending official allocation." Never present a guess as official.

## 6. Acceptance criteria

- Bracket tab renders inside the shared `/standings` page in combinebranch's UI.
- Projected qualifiers show 12 winners / 12 runners-up / 8 best thirds (with cutoff), consistent with the Group Stage tab's wild-card coloring.
- Projected R32 list shows all 16 matchups from the fixed skeleton; third-place slots use the official allocation or are clearly marked "(allocation TBD / provisional)".
- `BracketService` is isolated and unit-testable; reuses `StandingsService` (no duplicated standings math).

## 7. Out of scope

- Visual bracket **tree** (later phase) — projected *list* only.
- Round of 16 → Final progression.
- Real `Match`-derived standings — mock/seed for now (same source as the group-stage brief).
