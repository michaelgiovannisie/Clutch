import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { fetchTeamBySlug, fetchTeamPlayers } from '../lib/api.js';
import Flag from '../components/Flag.jsx';
import Crest from '../components/Crest.jsx';

const POSITION_ORDER = ['Goalkeeper', 'Defender', 'Midfielder', 'Attacker'];
const POSITION_PLURAL = {
  Goalkeeper: 'Goalkeepers',
  Defender: 'Defenders',
  Midfielder: 'Midfielders',
  Attacker: 'Attackers',
};

function PlayerAvatar({ player }) {
  const [imgErr, setImgErr] = useState(false);
  if (player.photoUrl && !imgErr) {
    return (
      <img
        src={player.photoUrl}
        alt={player.name}
        className="player-photo"
        onError={() => setImgErr(true)}
      />
    );
  }
  const initials = player.name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase();
  return <span className="player-avatar">{initials}</span>;
}

function SquadSection({ position, players }) {
  const sorted = [...players].sort((a, b) => (a.shirtNumber ?? 99) - (b.shirtNumber ?? 99));
  return (
    <div className="squad-section">
      <h3 className="squad-section-title">{POSITION_PLURAL[position] ?? position}</h3>
      {sorted.map(p => (
        <div key={p.id} className="player-row">
          <span className="player-number">{p.shirtNumber ?? '—'}</span>
          <PlayerAvatar player={p} />
          <span className="player-name">{p.name}</span>
          {p.age && <span className="player-age">{p.age}</span>}
        </div>
      ))}
    </div>
  );
}

export default function TeamDetail() {
  const { slug } = useParams();
  const [team, setTeam] = useState(null);
  const [players, setPlayers] = useState([]);
  const [loadingTeam, setLoadingTeam] = useState(true);
  const [loadingPlayers, setLoadingPlayers] = useState(false);

  useEffect(() => {
    setLoadingTeam(true);
    fetchTeamBySlug(slug).then(t => {
      setTeam(t);
      setLoadingTeam(false);
      if (t?.id) {
        setLoadingPlayers(true);
        fetchTeamPlayers(t.id).then(ps => {
          setPlayers(ps);
          setLoadingPlayers(false);
        });
      }
    });
  }, [slug]);

  if (loadingTeam) {
    return (
      <div className="page">
        <Link to="/teams" className="back-link">← All Teams</Link>
        <p style={{ color: 'var(--text-muted)', marginTop: 24 }}>Loading…</p>
      </div>
    );
  }

  if (!team) {
    return (
      <div className="page">
        <Link to="/teams" className="back-link">← All Teams</Link>
        <h1>Team not found</h1>
      </div>
    );
  }

  const grouped = POSITION_ORDER.reduce((acc, pos) => {
    const inPos = players.filter(p => p.position === pos);
    if (inPos.length) acc[pos] = inPos;
    return acc;
  }, {});

  return (
    <div className="page team-detail">
      <Link to="/teams" className="back-link">← All Teams</Link>

      <div className="team-detail-banner">
        <Flag slug={team.slug} alt="" className="team-detail-banner-bg" />
        <div className="team-detail-banner-overlay" />
        <div className="team-detail-banner-content">
          <Crest slug={team.slug} size="lg" alt={team.name} />
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.4rem' }}>
              <Flag slug={team.slug} alt={team.name} className="team-card-flag-chip" loading="lazy" />
              <span className="team-detail-group">Group {team.group}</span>
            </div>
            <h1 className="team-detail-name">{team.name}</h1>
            <p className="team-detail-abbr">{team.abbr}</p>
            {team.coach && team.coach !== 'Not available' && (
              <p className="team-detail-coach">Coach: {team.coach}</p>
            )}
          </div>
        </div>
      </div>

      {/* Squad roster */}
      <div style={{ marginTop: 24, background: 'var(--surface)', borderRadius: 'var(--radius-card)', border: '1px solid var(--border)', overflow: 'hidden' }}>
        <div className="card-header section-title">Squad</div>

        {loadingPlayers ? (
          <p style={{ padding: '24px 16px', color: 'var(--text-muted)', margin: 0 }}>Loading squad…</p>
        ) : Object.keys(grouped).length === 0 ? (
          <p style={{ padding: '24px 16px', color: 'var(--text-muted)', margin: 0 }}>
            Squad not yet available — run a roster refresh to populate.
          </p>
        ) : (
          <div style={{ padding: '0 0 8px' }}>
            {POSITION_ORDER.filter(pos => grouped[pos]).map(pos => (
              <SquadSection key={pos} position={pos} players={grouped[pos]} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
