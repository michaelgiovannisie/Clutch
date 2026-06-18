# Venue Explorer → React (port from dupebranch1) — Implementation Brief

> **For:** Claude Code. **Target branch:** `NewCombine` (React side-menu app + Spring backend).
> **Source of the working feature:** `dupebranch1` (Thymeleaf). Read its files as the behavior reference.
> Goal: rebuild the fully-updated venue map + detail as **React pages inside the side-menu app**, consuming the existing venue REST endpoints. This is a spec, not code.

## 1. Goal & integration

`dupebranch1` has the finished venue feature as Thymeleaf (Leaflet map explorer + a rich detail page with image galleries and live weather). NewCombine's UI is the React side-menu app, where `frontend/src/pages/Venues.jsx` is just a placeholder.

Rebuild the feature in React so it lives inside the side-menu shell:
- `Venues.jsx` → interactive Leaflet map + venue list, with a country filter.
- New `VenueDetail.jsx` at route `/venues/:id` → galleries (stadium photos + nearby attractions) and a live weather widget.
- Styled with the existing design-system tokens (`theme.css`); the side menu stays around it.

The backend already exposes JSON, so the React side only fetches + renders.

## 2. Backend port (bring dupebranch1's venue backend into NewCombine)

NewCombine's `VenueController` only has `/api/venues`, `/api/venues/{id}`, `/api/venues/{id}/images`. dupebranch1 adds three more and richer logic. The `Venue` entity is **identical** on both branches — no schema change.

Bring over from `dupebranch1`:
- `src/main/java/.../controller/VenueController.java` (adds `landmark-images`, `attraction-images`, `weather`) — replace NewCombine's version.
- `src/main/java/.../service/VenueService.java` (e.g. `getVenuesByCountry`) — replace/merge.
- `src/main/resources/data.sql` venue seed (14 venues).
- Static image trees under `src/main/resources/static/images/`: the stadium folders (`/images/USA/...`, `/images/Canada/...`, `/images/Mexico/...`), plus `/images/landmarks/{id}/` and `/images/attractions/{id}/{n}/`.

Keep the DEV H2 setup (or Postgres) so it boots; and **run with `mvn clean`** so stale classes from other branches don't get scanned (this exact trap bit us before).

## 3. Venue REST API (the contract the React app consumes)

| Method | Path | Returns |
|--------|------|---------|
| GET | `/api/venues` (opt `?country=USA`) | `Venue[]` |
| GET | `/api/venues/{id}` | `Venue` |
| GET | `/api/venues/{id}/images` | `string[]` — stadium photo URLs (from the venue's folder) |
| GET | `/api/venues/{id}/landmark-images` | `string[]` — `/images/landmarks/{id}/...` |
| GET | `/api/venues/{id}/attraction-images` | `string[][]` — one photo-set per nearby attraction, in `VENUE_ATTRACTIONS` order |
| GET | `/api/venues/{id}/weather` | `{ tempF, windMph, code, label, icon }` (live via Open-Meteo, no API key) |

`Venue` shape: `{ id, name, city, country, flag (emoji), capacity, lat, lng, imageUrl, countryColor }`.

> Note: `attraction-images` returns photo sets **indexed to a frontend-defined list** of attractions (`VENUE_ATTRACTIONS`) that currently lives in dupebranch1's `venue-detail.html` JS — see §5.

## 4. Frontend (React) build

### Dependencies
- Add `leaflet` + `react-leaflet` and import `leaflet/dist/leaflet.css`.
- Fix the well-known react-leaflet default-marker-icon issue (set marker icon image URLs explicitly, or use custom `divIcon` markers colored by `countryColor`).

### Dev wiring (important)
- The React dev server (:5173) must reach the Spring backend (:8080) for both **`/api`** and **`/images`** (stadium/attraction/landmark photos are served from Spring's static dir). Add a Vite dev proxy so `/api` and `/images` proxy to `http://localhost:8080`. (Avoids CORS and makes image URLs from the API resolve in dev.)
- `assets/` (flags/crests/logos) stays served by Vite's `publicDir` as today.

### `Venues.jsx` — map explorer
- Full-width Leaflet map filling `app-main`; markers for all venues (`GET /api/venues`), marker color from `countryColor`.
- Marker click → popup: venue name, city, capacity, flag, and a link to `/venues/{id}`.
- Country filter (All / USA / Canada / Mexico) — pills styled like the existing `.group-pill`; refetch with `?country=` or filter client-side.
- Optional companion list/grid of venue cards beside/below the map (reuse `Card` styling).
- Match dupebranch1's explorer behavior (`venues/venue-explorer.html`) for marker styling and filtering.

### `VenueDetail.jsx` — `/venues/:id`
- Header: venue name, city, flag, capacity (from `GET /api/venues/{id}`).
- **Stadium gallery:** `GET /api/venues/{id}/images`.
- **Nearby attractions:** render the `VENUE_ATTRACTIONS` entries for this venue (name + blurb) each with its photo set from `GET /api/venues/{id}/attraction-images` (same index order).
- **Live weather widget:** `GET /api/venues/{id}/weather` → show `icon`, `tempF`, `windMph`, `label`. Handle the empty-object response (API/network failure) gracefully.
- Mirror dupebranch1's `venues/venue-detail.html` layout/sections.

### Routing
- Add `<Route path="venues/:id" element={<VenueDetail />} />` in `App.jsx`. "Venues" is already in the side menu.

## 5. Data to carry over from the Thymeleaf source

dupebranch1's `venue-detail.html` (and/or `venue-explorer.html`) contains a hardcoded **`VENUE_ATTRACTIONS`** structure (per-venue list of nearby attraction names/descriptions) that the `attraction-images` endpoint is indexed against. Extract it into a React data module (e.g. `frontend/src/data/venueAttractions.js`) keyed by venue id, preserving order so it lines up with the `string[][]` from the API. Carry over any landmark metadata similarly.

## 6. Reference files on `dupebranch1` (read these)

- `src/main/resources/templates/venues/venue-explorer.html` — Leaflet init, marker styling, country filter, theme.
- `src/main/resources/templates/venues/venue-detail.html` — gallery + weather layout, and the `VENUE_ATTRACTIONS` data.
- `src/main/java/.../controller/VenueController.java`, `service/VenueService.java` — endpoint logic.
- `src/main/resources/data.sql` + `static/images/**` — seed + photos.

## 7. Acceptance criteria

- Backend (NewCombine) builds and serves all six venue endpoints; `mvn clean spring-boot:run` boots (H2 or Postgres) on :8080.
- React `/venues` shows an interactive map of all 14 venues with working country filter and marker → detail navigation, inside the side-menu layout.
- `/venues/:id` shows stadium gallery, nearby-attraction galleries (correctly matched to `VENUE_ATTRACTIONS`), and a live weather widget.
- Dev: `npm run dev` (:5173) reaches `/api` and `/images` via the Vite proxy; `npm run build` succeeds.
- Visually consistent with the design system + side menu (no leftover Thymeleaf orange theme).

## 8. Gotchas

- **`mvn clean`** before running (stale `.class` files from other branches caused a startup crash before).
- Vite dev **proxy for `/api` and `/images`** — without it, API calls and stadium/attraction photos 404 in dev.
- **react-leaflet marker icons**: set icon URLs explicitly or markers won't render.
- **Open-Meteo** needs outbound network; the weather endpoint returns `{}` on failure — render a graceful fallback.

## 9. Out of scope

- Changing the `Venue` schema (identical across branches).
- The Thymeleaf venue pages — React is canonical on NewCombine; the old `venues/*.html` can be removed or left unused.
