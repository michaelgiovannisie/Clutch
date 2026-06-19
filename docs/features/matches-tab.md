# Matches Tab — Implementation Brief

> **For:** Claude Code. **Owner:** Joe. **Target:** React app (`frontend/`), current working branch.
> Build out the Matches tab: a day-by-day schedule, a star/watchlist for matches, and team favoriting. This is a spec, not code. Mock data now; swappable for a real API later.

## 1. Scope

Replace the `Matches.jsx` placeholder with a real schedule, and add a favorites system used across the app. Three pillars:
1. **Day-by-day schedule** of the tournament.
2. **Star a match** → a personal watchlist.
3. **Favorite a team** → highlight + filter their matches.

Reuse existing components/tokens: `MatchCard`, `Pill`, `Flag`, `Crest`, `Tabs`, and the `.page-title`/`.page-subtitle`/`.section-title` system. Persist favorites in `localStorage`, mirroring the `SettingsContext` pattern.

## 2. Data (sample already provided)

The sample dataset already exists at **`frontend/src/data/mockMatches.json`** (30 matches, June 11–20, all 12 groups, with FINISHED/LIVE/SCHEDULED examples). Import it directly (`import matches from '../data/mockMatches.json'`) — no need to create it. Its `slug`s already match the Teams data and its `venueId`s match the venue seed, so flags/crests and team/venue links resolve. A real API will replace this later; keep all match access behind one module/function so the swap is trivial. Each match has this shape:

```
{
  id,                       // stable unique id
  date,                     // "2026-06-14" (ISO date)
  kickoff,                  // ISO datetime (UTC) for local-time display
  stage,                    // "Group A".."Group L" | "Round of 32" | ...
  home: { slug, name, abbr },
  away: { slug, name, abbr },
  venueId, venueName, city,
  status,                   // "SCHEDULED" | "LIVE" | "FINISHED"
  homeScore, awayScore,     // null until played
  minute                    // optional, for LIVE
}
```

- The provided sample is partial (30 of 104 matches) but spans every group and all three statuses — enough to build the full UI against. Expanding it isn't required for this work.
- `home.slug`/`away.slug` match the Teams slugs (`mockTeams.js`); `venueId` matches the venue seed. Keep it that way if you add more.
- Wrap match access in one helper/module so swapping `mockMatches.json` for a real `/api/matches` later needs no UI changes.

## 3. Favorites system (shared)

Add `frontend/src/context/FavoritesContext.jsx` (provider near the app root, like `SettingsContext`):
- State: `favoriteTeams` (Set of team slugs), `starredMatches` (Set of match ids).
- Actions: `toggleTeam(slug)`, `toggleMatch(id)`, plus `isFavoriteTeam` / `isStarredMatch` helpers.
- Persist both to `localStorage`; load on init.

## 4. Matches page (day-by-day schedule)

- `.page-title` "Matches" + `.page-subtitle`.
- **Date navigator:** a horizontal, scrollable date strip (June 11 → July 19) showing each matchday; selecting one shows that day's matches. Include a "Today" button that jumps to the current date (clamped to the tournament window). Highlight the selected day and days that have starred matches.
- **Filters (chips, reuse `.group-pill`):** `All` · `My teams` (matches involving a favorited team) · `Starred` (watchlist). Optionally a stage/group filter too.
- **Match list:** for the selected day, list matches as schedule rows (reuse/adapt `MatchCard`): kickoff time **in the user's local timezone** (derive from `kickoff`), home/away flag + abbr + score, venue/city, a status `Pill` (LIVE / FT / upcoming time), and a **star toggle**. Whole row links to `/matches/:id` (the star button stops propagation).
- **Empty states:** "No matches on this day" / "No starred matches yet — tap the star on a match to add it."

## 5. Team favoriting

- Add a **star/heart toggle** to `TeamCard` (and the `TeamDetail` hero) wired to `toggleTeam`. Show a filled state when favorited; the button must not trigger the card's navigation (stop propagation, has `aria-label`/`aria-pressed`).
- Add a **"Favorites" filter** to the Teams page (alongside the group filter) to show only favorited teams.
- The Matches "My teams" filter reads the same `favoriteTeams`.

## 6. Match detail (light buildout)

- Flesh out `MatchDetail.jsx` from placeholder to: both teams (flag/crest, name), score/status, stage, venue + city, date/kickoff (local time), and a **star toggle**. Leave a clearly-marked **slot for the AI commentary panel** (Joe's separate feature) — don't build commentary here.

## 7. Suggestions (open ideas — build the ones you like)

- **Local-time kickoffs** with a small "times shown in your timezone" note (recommended — included above).
- **Surface on Home:** a "Your starred matches" / "next up for your teams" strip on the Home page (cross-feature win).
- **Stage filter** (Group / Round of 32 / …) once knockout fixtures exist.
- **"Add to calendar" (.ics)** export for starred matches (stretch).
- **Live affordances:** if any mock match is `LIVE`, show the minute + a subtle pulse on the status pill.

## 8. Acceptance criteria

- Matches page shows a working day-by-day schedule with a date navigator + Today; selecting a day lists its matches with local kickoff times, status pills, scores for finished games.
- Starring a match persists across reloads and powers the `Starred` filter; favoriting a team persists and powers `My teams` (Matches) and the Teams `Favorites` filter.
- Team and match rows link to their detail pages; slugs resolve to real team pages.
- Match detail shows core info + a commentary slot; favorites buttons are keyboard-accessible (`aria-pressed`).
- Uses the shared type/color system; works in light + dark; responsive; reduced-motion respected. `npm run build` succeeds.

## 9. Out of scope

- Real-time/live data and push notifications.
- Account-based sync (localStorage only — favorites are per-device).
- AI commentary internals (separate brief; just leave the slot).
