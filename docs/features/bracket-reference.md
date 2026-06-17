# 2026 World Cup — Round of 32 Bracket Data Reference

> **For:** Claude Code, to populate `BracketTemplate` and `ThirdPlaceAllocation` referenced in `port-standings-to-thymeleaf.md` / `standings-bracket.md`.
> This is verified reference data, not code. Encode it as config; don't fabricate the parts marked "source from FIFA."

## 1. Confirmed format facts

- 48 teams, 12 groups (A–L) of 4. **Top 2 of each group (24) + the 8 best 3rd-placed teams = 32** advance to the Round of 32.
- **Third-place ranking** (to pick 8 of 12), in order: **points → goal difference → goals scored → disciplinary/fair-play points → drawing of lots.**
- The 8 third-placed qualifiers are slotted into the bracket via a **FIFA pre-determined allocation: 495 scenarios** (one per combination of 8 qualifying groups out of 12). The correct scenario is applied automatically when the group stage ends — **there is no secondary draw**.

## 2. Fixed R32 skeleton (authoritative — encode as `BracketTemplate`)

Matches 73–88. `1X` = winner of Group X, `2X` = runner-up of Group X, `3[set]` = a third-placed team from one of the listed groups (resolved by the allocation in §3). This skeleton never changes.

| Match | Home slot | Away slot | Type |
|------|-----------|-----------|------|
| 73 | 2A | 2B | RU v RU |
| 74 | 1E | 3 of {A,B,C,D,F} | W v 3rd |
| 75 | 1F | 2C | W v RU |
| 76 | 1C | 2F | W v RU |
| 77 | 1I | 3 of {C,D,F,G,H} | W v 3rd |
| 78 | 2E | 2I | RU v RU |
| 79 | 1A | 3 of {C,E,F,H,I} | W v 3rd |
| 80 | 1L | 3 of {E,H,I,J,K} | W v 3rd |
| 81 | 1D | 3 of {B,E,F,I,J} | W v 3rd |
| 82 | 1G | 3 of {A,E,H,I,J} | W v 3rd |
| 83 | 2K | 2L | RU v RU |
| 84 | 1H | 2J | W v RU |
| 85 | 1B | 3 of {E,F,G,I,J} | W v 3rd |
| 86 | 1J | 2H | W v RU |
| 87 | 1K | 3 of {D,E,I,J,L} | W v 3rd |
| 88 | 2D | 2G | RU v RU |

Breakdown: **4 runner-up vs runner-up** (M73, 78, 83, 88), **4 winner vs runner-up** — the winners of C/F/H/J (M75, 76, 84, 86), and **8 winner vs 3rd-place** — the winners of A/B/D/E/G/I/K/L (M74, 77, 79, 80, 81, 82, 85, 87). Only those 8 "v 3rd" slots vary by scenario; everything else is fixed.

(Sanity check baked in: each "v 3rd" set excludes the winner's own group.)

## 3. Third-place allocation (`ThirdPlaceAllocation`) — source from FIFA

The 8 "v 3rd" slots (M74, 77, 79, 80, 81, 82, 85, 87) receive the 8 qualifying thirds according to which **combination** of 8 groups produced them. There are **495 combinations** (C(12,8)); each maps the 8 winner-slots to specific third-place groups.

- **Constraint sets per slot** (the allowed third-place groups for each, from §2):
  - M74 (1E): {A,B,C,D,F}
  - M77 (1I): {C,D,F,G,H}
  - M79 (1A): {C,E,F,H,I}
  - M80 (1L): {E,H,I,J,K}
  - M81 (1D): {B,E,F,I,J}
  - M82 (1G): {A,E,H,I,J}
  - M85 (1B): {E,F,G,I,J}
  - M87 (1K): {D,E,I,J,L}

- **Do not hand-transcribe all 495 rows** — that's error-prone. Encode `ThirdPlaceAllocation` as a lookup keyed by the sorted set of the 8 qualifying group letters (e.g. `"ABCDEFGH"`) → a map of `{matchNumber → groupLetter}`. Populate it from the **official FIFA source** (the FIFA Competition Regulations 2026 allocation table, or a maintained machine-readable dataset). Treat it as a data file, not logic.

- **Interim behaviour until the table is loaded:** for the "if it started today" projection, render the fixed slots (winners + runner-up pairings) fully, and for the 8 third-place slots either (a) show `3 of {set}` as a labelled placeholder, or (b) compute a *provisional* assignment by matching the 8 qualifying thirds to the slots' constraint sets (a constraint-satisfaction assignment) and clearly mark it "provisional — pending official allocation." Never present a guessed assignment as official.

## 4. How to encode (structure, not implementation)

- `BracketTemplate`: an ordered list of 16 entries `{ match, homeSlot, awaySlot }` where slots are tokens like `1A`, `2B`, or `3:{A,B,C,D,F}`. This is the §2 table verbatim.
- `ThirdPlaceAllocation`: `Map<String, Map<Integer,String>>` — key = sorted 8-letter combination, value = `{74→"x", 77→"y", ...}`. Populate from FIFA's official table.
- Resolver: given group standings → compute 1X/2X for all groups, rank the 12 thirds, take the top 8, build the combination key, look up the allocation, and fill the 8 third-place slots. Output the 16 resolved matchups for the projected-R32 view.

## 5. Notes

- Round of 16 / quarter-final / semi-final progression is a separate fixed tree (winner of M73 vs winner of M74, etc.) — out of scope for this reference, which covers R32 seeding only.
- The skeleton above is the men's 2026 bracket; verify match numbers/venues against the official FIFA schedule if you also display kickoff/venue.

## Sources

- [Sky Sports — World Cup 2026 fixture schedule (R32 match list)](https://www.skysports.com/football/news/11095/13481245/world-cup-2026-fixture-schedule-and-uk-kick-off-times-day-by-day-breakdown-of-all-104-matches-including-england-scotland)
- [FIFA — Knockout stage match schedule / bracket](https://www.fifa.com/en/tournaments/mens/worldcup/canadamexicousa2026/articles/knockout-stage-match-schedule-bracket)
- [Sportmonks — R32 & knockouts: 495 pre-defined scenarios, no secondary draw](https://www.sportmonks.com/blogs/world-cup-2026-round-of-32-and-knockouts-how-to-build-world-cup-brackets/)
- [FOX Sports — group-stage & third-place tiebreakers](https://www.foxsports.com/stories/soccer/fifa-world-cup-group-stage-third-place-tiebreakers)
- [FIFA — groups, how teams qualify & tie-breakers](https://www.fifa.com/en/tournaments/mens/worldcup/canadamexicousa2026/articles/groups-how-teams-qualify-tie-breakers)
