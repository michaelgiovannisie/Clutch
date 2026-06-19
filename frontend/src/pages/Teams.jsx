import { useState, useEffect } from 'react';
import { GROUPS } from '../data/mockTeams.js';
import TeamCard from '../components/TeamCard.jsx';
import { useFavorites } from '../context/FavoritesContext.jsx';
import { fetchTeams } from '../lib/api.js';

export default function Teams() {
  const [teams, setTeams] = useState([]);
  const [activeGroup, setActiveGroup] = useState('ALL');
  const [showFavs, setShowFavs] = useState(false);
  const { isFavoriteTeam } = useFavorites();

  useEffect(() => {
    fetchTeams().then(setTeams);
  }, []);

  const visible = teams
    .filter(t => activeGroup === 'ALL' || t.group === activeGroup)
    .filter(t => !showFavs || isFavoriteTeam(t.slug));

  return (
    <div className="page teams-page">
      <h1 className="page-title">Teams</h1>
      <p className="page-subtitle">All 48 nations competing at FIFA World Cup 2026™</p>

      <div className="group-filter">
        <button
          className={'group-pill' + (!showFavs && activeGroup === 'ALL' ? ' is-active' : '')}
          onClick={() => { setActiveGroup('ALL'); setShowFavs(false); }}
        >
          All
        </button>
        {GROUPS.map(g => (
          <button
            key={g}
            className={'group-pill' + (!showFavs && activeGroup === g ? ' is-active' : '')}
            onClick={() => { setActiveGroup(g); setShowFavs(false); }}
          >
            {g}
          </button>
        ))}
        <button
          className={'group-pill' + (showFavs ? ' is-active' : '')}
          onClick={() => setShowFavs(v => !v)}
        >
          ♥ Favorites
        </button>
      </div>

      {teams.length === 0 ? (
        <p style={{ color: 'var(--text-muted)', marginTop: 32 }}>Loading teams…</p>
      ) : visible.length === 0 ? (
        <p style={{ color: 'var(--text-muted)', marginTop: 32, textAlign: 'center' }}>
          No favorite teams yet — tap ♡ on a card to add one.
        </p>
      ) : (
        <div className="teams-grid">
          {visible.map(team => <TeamCard key={team.slug} team={team} />)}
        </div>
      )}
    </div>
  );
}
