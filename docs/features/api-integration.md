# API Integration — wiring API_Data's backend into the polished UI — Implementation Brief

> **For:** Claude Code. **Owner:** Joe.
> **Goal:** Combine the real backend from `API_Data` with the polished frontend from `NewCombine` (side menu, warm theme, standings, teams cards, venues map, favorites), replacing the frontend's mock data with live API data — without changing the UI components. This is a spec, not code.

## 0. DO NOT `git merge` — do this instead

A straight `git merge API_Data` into NewCombine will **conflict and risk losing UI work**: both branches edit the same frontend files (`API_Data` has crude `Teams.jsx`/`Matches.jsx` that fetch raw JSON; NewCombine has the polished, mock-driven versions). Don't merge the branches.

Instead, bring over **only the backend** and keep NewCombine's frontend untouched:

```bash
# from the repo root, with a clean working tree
git checkout NewCombine
git checkout -b api-integration                 # integration branch off NewCombine (keeps its UI)
git checkout API_Data -- backend/               # take ONLY the backend from API_Data
# (do NOT git checkout API_Data -- frontend/ — that would clobber the polished UI)
git status                                       # should show only backend/ changes staged
git commit -m "Bring in API_Data backend onto the NewCombine UI"
```

Then do the frontend wiring (§3–§5) as normal commits. The result: NewCombine's UI, fed by API_Data's backend. Everything below assumes this approach.

### Run it (after wiring)
```bash
# backend (needs Postgres running; port 8081)
cd backend && mvn clean spring-boot:run
# populate the DB once (empty until you do): run the admin refresh or SampleDataService — see §2
# frontend (separate terminal)
cd frontend && npm install && npm run dev        # http://localhost:5173, proxies /api -> :8081
```

## 1. Branch / structure plan

The two branches hold complementary halves:
- `API_Data` → the canonical REST backend (`backend/`, Spring Boot, api-football, Postgres, port 8081).
- `NewCombine` → the canonical frontend (`frontend/`, the design system + all feature pages).

Plan:
1. Create an integration branch from **NewCombine** (it has the UI we keep).
2. Bring in `API_Data`'s backend: `git checkout API_Data -- backend/` (this replaces NewCombine's older/legacy backend). The repo has accumulated multiple backends (a legacy root `src/` Thymeleaf app and `backend/`) — **consolidate on `API_Data`'s `backend/`** and stop using the legacy root `src/` app to avoid two servers.
3. Add the frontend data-access layer (§4) and point the frontend at the backend (§3).

## 2. Backend: how data gets populated (run order)

- Requires **PostgreSQL** (`worldcup` DB; the committed `application.properties` uses user `colin`/empty password and the api-football key — move real secrets to a gitignored `application-local.properties`).
- Backend runs on **port 8081** (`server.port=8081`), CORS already allows `http://localhost:5173`.
- The DB starts empty → GET endpoints return `[]` until populated. Populate via the admin refresh endpoints (`POST /api/admin/refresh`, `/refresh/rosters`, `/refresh/groups`) which pull from api-football. (There's also a `SampleDataService` — confirm whether it seeds without hitting the API; if so it's the fastest path for local dev.)
- api-football free tier is rate-limited; the config already has `min-refresh-minutes`/cache settings. Don't hammer refresh.

## 3. Point the frontend at the API

- Standardize the base URL: add a Vite dev proxy so `/api` → `http://localhost:8081`, and use **relative** `/api/...` everywhere (your polished Venues page already does). Replace any absolute `http://localhost:8081`/`:8080` fetches with the proxied relative path (and a `VITE_API_BASE_URL` for prod).
- Note the port changed to **8081** — update the venue/react-port proxy note (which assumed 8080) accordingly.

## 4. Frontend data-access layer (the core of the integration)

Add `frontend/src/lib/api.js` (fetchers) + `frontend/src/lib/mappers.js` (shape adapters). Every page imports from here instead of the mock modules. The mappers convert API responses into the **exact shapes the existing components already consume**, so `TeamCard`, `GroupTable`, `MatchCard`, `Venues`, etc. don't change.

- Keep a static `TEAM_META` table (from `mockTeams.js`): `slug → { abbr, accent }`, plus a `NAME_TO_SLUG` normalizer for api-football naming quirks.
- Keep local flag/crest assets keyed by `slug` (`/flags/{slug}.jpg`, `/crests/{slug}.jpg` placeholder) — ignore the API's `flagUrl` for design consistency (or use it as a fallback).
- Optional: keep the mock modules as an offline fallback if a fetch fails (nice for dev/demo).

### Slug normalization (critical)
api-football names won't all match your slugs. Build/verify `NAME_TO_SLUG` against the real responses (see §6 probe). Known watch-list: `Korea Republic`→`south-korea`, `IR Iran`→`iran`, `Côte d'Ivoire`→`ivory-coast`, `Turkey`/`Türkiye`→`turkiye`, `Cabo Verde`→`cape-verde`, `Curacao`→`curacao`, `Congo DR`/`DR Congo`→`dr-congo`, `Bosnia and Herzegovina`→`bosnia-herzegovina`, `USA`/`United States`→`united-states`, `Czechia`. Fallback: a generic slugify(name). If a team has no matching local asset, the crest placeholder still covers it.

## 5. Per-domain mapping

**Teams** (`GET /api/teams` → UI team): `name→name`, `country→country`, `groupLabel→group` (strip any "Group " prefix to the letter), `coach→coach`, `players→squad` (now available for the team-detail roster — a bonus the mock lacked). Derive `slug = NAME_TO_SLUG(name)`, `abbr = TEAM_META[slug].abbr`, `accent = TEAM_META[slug].accent`.

**Matches** (`GET /api/matches` → UI match shape): `home = { slug: norm(homeTeam.name), name: homeTeam.name, abbr: meta }`, same for `away`; `venueId = venue.id, venueName = venue.name, city = venue.city`; `kickoff = kickoffTime`, `date = kickoffTime`'s date part; `stage = groupLabel`; `homeScore/awayScore` direct; `status →` normalize to `SCHEDULED | LIVE | FINISHED` (map whatever api-football status strings appear — e.g. `NS`→SCHEDULED; `1H/2H/HT/ET/LIVE`→LIVE; `FT/AET/PEN`→FINISHED); `minute` from elapsed if present, else null.

**Standings** (`GET /api/standings` → UI groups): the API returns a flat, globally-sorted list. Group rows by `groupLabel` into the 12 groups `{ id: letter, teams: [...] }`; per row map `teamName→name` (+ `slug`/`abbr` via the table), `goalsFor→gf`, `goalsAgainst→ga`, `goalDifference→gd`, `played/won/drawn/lost/points` direct. Your existing `lib/standings.js annotateGroups` then re-ranks per group and sets advancement state. (You can also call `?group=` per group.)

**Venues** (`GET /api/venues` → UI): **direct** — the shape already matches the polished Venues page. Just hit port 8081. Gap: `VenueDetail`'s `/images`, `/landmark-images`, `/attraction-images`, `/weather` endpoints exist on `dupebranch1`'s VenueController but **not** in `API_Data`'s backend — either port those methods over (recommended, they're self-contained) or have `VenueDetail` degrade gracefully (hide galleries/weather) until then.

## 6. Probe before finalizing (do this first)

Run the backend, populate it (refresh or SampleDataService), then call `GET /api/teams`, `/api/matches`, `/api/standings`, `/api/venues` once and inspect the **actual** field values — especially team `name` strings and match `status` codes — and finalize `NAME_TO_SLUG` and the status map against reality, not assumptions.

## 7. Acceptance criteria

- One backend (`API_Data`'s `backend/`, port 8081) + the NewCombine frontend; legacy root `src/` app no longer used.
- Teams, Matches, Standings, and Venues pages render from live API data via the `lib/api.js` + `lib/mappers.js` layer; **no UI component changed** its props/markup.
- Flags/crests and team/venue links still resolve (slug normalization verified against real API names; placeholder fallback for any miss).
- Match statuses/dates display correctly; standings group + rank correctly.
- Frontend talks to the API via the `/api` proxy (8081); secrets are not committed.
- `npm run build` succeeds; backend boots and serves populated data.

## 8. Out of scope / follow-ups

- Porting the venue detail image/weather endpoints into `API_Data`'s backend (recommended follow-up).
- Replacing local flag/crest assets with API imagery (kept local by choice).
- The AI commentary endpoints (separate work).
