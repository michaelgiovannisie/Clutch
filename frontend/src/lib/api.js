import { mapTeam, mapMatch, mapStandings } from './mappers.js';
import mockMatchesRaw from '../data/mockMatches.json';
import { groups as mockGroups } from '../data/mockStandings.js';
import { TEAMS as mockTeams } from '../data/mockTeams.js';

const BASE = import.meta.env.VITE_API_BASE_URL ?? '';

async function get(path) {
  const res = await fetch(BASE + path);
  if (!res.ok) throw new Error(`${res.status} ${path}`);
  return res.json();
}

export async function fetchTeams() {
  try {
    const data = await get('/api/teams');
    return data.map(mapTeam);
  } catch {
    return mockTeams;
  }
}

export async function fetchMatches() {
  try {
    const data = await get('/api/matches');
    return data.map(mapMatch);
  } catch {
    return mockMatchesRaw;
  }
}

export async function fetchMatchById(id) {
  try {
    const data = await get(`/api/matches/${id}`);
    return mapMatch(data);
  } catch {
    // fallback: search mock data
    return mockMatchesRaw.find(m => m.id === Number(id)) ?? null;
  }
}

export async function fetchStandings() {
  try {
    const data = await get('/api/standings');
    return mapStandings(data);
  } catch {
    return mockGroups;
  }
}
