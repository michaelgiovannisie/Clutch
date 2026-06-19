import raw from '../data/mockMatches.json';

export const matches = raw;

export function getMatchById(id) {
  return matches.find(m => m.id === Number(id)) ?? null;
}

export function getUniqueDates() {
  return [...new Set(matches.map(m => m.date))].sort();
}

export function getMatchesByDate(date) {
  return matches.filter(m => m.date === date);
}
