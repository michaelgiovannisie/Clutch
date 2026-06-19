import { TEAMS } from '../data/mockTeams.js';

export const TEAM_META = Object.fromEntries(TEAMS.map(t => [t.slug, t]));

const NAME_TO_SLUG = Object.fromEntries(TEAMS.map(t => [t.name.toLowerCase(), t.slug]));

const QUIRKS = {
  'korea republic': 'south-korea',
  'ir iran': 'iran',
  "côte d'ivoire": 'ivory-coast',
  "cote d'ivoire": 'ivory-coast',
  'côte divoire': 'ivory-coast',
  'turkey': 'turkiye',
  'cabo verde': 'cape-verde',
  'curacao': 'curacao',
  'congo dr': 'dr-congo',
  'dr congo': 'dr-congo',
  'democratic republic of congo': 'dr-congo',
  'bosnia and herzegovina': 'bosnia-herzegovina',
  'usa': 'united-states',
  'czech republic': 'czechia',
  'saudi arabia': 'saudi-arabia',
  'new zealand': 'new-zealand',
  'south korea': 'south-korea',
  'ivory coast': 'ivory-coast',
  'cape verde': 'cape-verde',
};

function slugify(name) {
  return name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
}

export function nameToSlug(name) {
  if (!name) return 'unknown';
  const lower = name.toLowerCase();
  return QUIRKS[lower] ?? NAME_TO_SLUG[lower] ?? slugify(name);
}

function stripGroup(label) {
  return label?.replace(/^Group\s+/i, '').trim() ?? '';
}

export function mapTeam(apiTeam) {
  const slug = nameToSlug(apiTeam.name);
  const meta = TEAM_META[slug] ?? {};
  return {
    id: apiTeam.id,
    slug,
    name: apiTeam.name,
    country: apiTeam.country,
    abbr: meta.abbr ?? apiTeam.name.slice(0, 3).toUpperCase(),
    group: stripGroup(apiTeam.groupLabel),
    accent: meta.accent ?? 'var(--orange)',
    coach: apiTeam.coach ?? null,
    players: apiTeam.players ?? [],
  };
}

const STATUS_MAP = {
  NS: 'SCHEDULED', TBD: 'SCHEDULED', SCHEDULED: 'SCHEDULED',
  '1H': 'LIVE', '2H': 'LIVE', HT: 'LIVE', ET: 'LIVE', BT: 'LIVE', LIVE: 'LIVE', P: 'LIVE',
  FT: 'FINISHED', AET: 'FINISHED', PEN: 'FINISHED', FINISHED: 'FINISHED',
  AWD: 'FINISHED', WO: 'FINISHED',
};

export function mapMatch(apiMatch) {
  const homeSlug = nameToSlug(apiMatch.homeTeam?.name ?? '');
  const awaySlug = nameToSlug(apiMatch.awayTeam?.name ?? '');
  const homeMeta = TEAM_META[homeSlug] ?? {};
  const awayMeta = TEAM_META[awaySlug] ?? {};

  // LocalDateTime from Spring has no trailing Z — treat as UTC
  const kickoff = apiMatch.kickoffTime ? apiMatch.kickoffTime + 'Z' : null;
  const date = kickoff ? kickoff.slice(0, 10) : null;

  return {
    id: apiMatch.id,
    date,
    kickoff,
    stage: apiMatch.groupLabel ?? '',
    home: {
      slug: homeSlug,
      name: apiMatch.homeTeam?.name ?? '',
      abbr: homeMeta.abbr ?? (apiMatch.homeTeam?.name ?? '???').slice(0, 3).toUpperCase(),
    },
    away: {
      slug: awaySlug,
      name: apiMatch.awayTeam?.name ?? '',
      abbr: awayMeta.abbr ?? (apiMatch.awayTeam?.name ?? '???').slice(0, 3).toUpperCase(),
    },
    venueId: apiMatch.venue?.id ?? null,
    venueName: apiMatch.venue?.name ?? '',
    city: apiMatch.venue?.city ?? '',
    status: STATUS_MAP[apiMatch.status?.toUpperCase()] ?? 'SCHEDULED',
    homeScore: apiMatch.homeScore ?? null,
    awayScore: apiMatch.awayScore ?? null,
    minute: apiMatch.minute ?? null,
  };
}

export function mapStandings(apiRows) {
  const groupMap = {};
  for (const row of apiRows) {
    const letter = stripGroup(row.groupLabel);
    if (!groupMap[letter]) groupMap[letter] = [];
    const slug = nameToSlug(row.teamName);
    const meta = TEAM_META[slug] ?? {};
    groupMap[letter].push({
      slug,
      name: row.teamName,
      abbr: meta.abbr ?? row.teamName.slice(0, 3).toUpperCase(),
      played: row.played,
      won: row.won,
      drawn: row.drawn,
      lost: row.lost,
      gf: row.goalsFor,
      ga: row.goalsAgainst,
      gd: row.goalDifference,
      points: row.points,
    });
  }
  return Object.entries(groupMap)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([id, teams]) => ({ id, teams }));
}
