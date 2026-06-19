# Fix Missing Teams + Player Rosters — Implementation Brief

> **For:** Claude Code. **Owner:** Joe. **Target:** the `api-integration` branch (API_Data backend + NewCombine UI).
> Two related fixes: (A) the 3 missing teams, and (B) surface the player/roster data the API already exposes. This is a spec, not code.

## A. Fix the 3 missing teams

### Root cause
Teams are only created by `importTeams()` (api-football `/teams?league&season`). The fixture importer resolves teams via `FootballDataImportService.findTeamByApiId(apiId)`, which **throws** when a team isn't already saved:
```java
return teamRepository.findByApiId(apiId)
    .orElseThrow(() -> new RuntimeException("Team not found for apiId: " + apiId));
```
When the `/teams` response is short (some 2026 spots came via play-offs the provider hasn't attached to the league/season), those teams are never created, and the fixture importer can't backfill them — it errors instead. Result: ~45 teams.

### Fix (robust)
1. **Find-or-create teams from fixtures.** In the match import path, replace the throwing `findTeamByApiId` with a find-or-create that builds the team from the fixture's `home`/`away` object (which already has `id`, `name`, `logo`):
   - `teamRepository.findByApiId(apiId).orElseGet(() -> create from fixture team data)` — set `apiId`, `name`, `flagUrl` (logo), `groupLabel = fallbackGroupForTeam(name)`, `coach = "Not available"` (a later static refresh fills it). Save before linking to the match.
   - Since every team plays group fixtures, this guarantees all 48 exist after a fixtures refresh, and is resilient to future `/teams` gaps.
2. **Verify `fallbackGroupForTeam` covers all 48 names** — make sure the 3 currently-missing teams' api-football names are mapped to the right group letter (otherwise they import with `groupLabel = "TBD"` and won't slot into standings/group filters).
3. **Diagnose + supplemental seed (only if still short).** After `POST /api/admin/refresh` (static + fixtures), call `GET /api/teams` and confirm it returns **48**. If the provider also lacks fixtures for a missing team (matchup still TBD on their side), add that team to a small supplemental seed (in `SampleDataService` or a seed list): `name`, `country`, `groupLabel`, optional `flagUrl`. Keep it minimal — just enough so all 48 appear.

### Acceptance (A)
- `GET /api/teams` returns 48 distinct teams with correct `groupLabel`s; the Teams page and standings show all 48.
- Re-running a fixtures refresh no longer throws "Team not found for apiId".

## B. Player rosters feature

The backend already exposes squads — this is mostly a **frontend** feature plus running the roster refresh.

### Data / backend (already present)
- `POST /api/admin/refresh/rosters` populates players for every team (api-football `/players/squads`). Run it once after teams exist. Note: that's ~48 squad calls — mind the free-tier rate limit.
- `GET /api/teams/{id}/players` → `Player[]` `{ id, name, position, shirtNumber, age, photoUrl }` (the squads endpoint gives only a short `name`; `firstName`/`lastName` are null — use `name`). `position` values are like `Goalkeeper | Defender | Midfielder | Attacker`.
- `GET /api/teams/{id}` also returns the team with nested `players[]`.

### Frontend (build this)
- Add to `lib/api.js`: `fetchTeamPlayers(id)` (and reuse the teams list to resolve a `slug` → team `id`, since the Team detail route is by slug but the API is by numeric id). Add a `mapPlayer` in `mappers.js` if any shaping is needed.
- **Team detail roster section:** on `TeamDetail`, after resolving the team, fetch its players and render a **squad grouped by position** (Goalkeepers → Defenders → Midfielders → Attackers), each group a labeled `.section-title`; within a group, sort by `shirtNumber`. Each player row/card: shirt number, name, age, and `photoUrl` (with a graceful fallback avatar/initials when the photo is missing).
- **States:** loading skeleton; empty state ("Squad not yet available") when players haven't been refreshed; keep it responsive and token-styled (light/dark).
- Also good: show the team `coach` in the team-detail header (already on the Team object).

### Acceptance (B)
- A team's detail page shows its full squad grouped by position with shirt numbers, ages, and photos; graceful empty/loading states; styled with the design system in both themes.
- Slug→id resolution works (clicking a team anywhere lands on the right roster).
- `npm run build` succeeds.

## Run order (for whoever executes)
1. Apply the backend find-or-create fix; `mvn clean spring-boot:run` (Postgres up, port 8081).
2. `POST /api/admin/refresh` then `POST /api/admin/refresh/rosters`.
3. Confirm `GET /api/teams` = 48 and `GET /api/teams/{id}/players` returns squads.
4. Build the frontend roster UI; verify against the live data.

## Out of scope
- Player detail pages / stats beyond the squad list (the `/players/profiles` endpoint exists if wanted later).
- Replacing api-football player photos with local assets.
