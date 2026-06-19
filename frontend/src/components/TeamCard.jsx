import { Link } from 'react-router-dom';
import Flag from './Flag.jsx';
import Crest from './Crest.jsx';
import { useFavorites } from '../context/FavoritesContext.jsx';

export default function TeamCard({ team }) {
  const { isFavoriteTeam, toggleTeam } = useFavorites();
  const fav = isFavoriteTeam(team.slug);

  return (
    <Link
      to={`/teams/${team.slug}`}
      className="team-card"
      aria-label={team.name}
      style={{ '--card-accent': team.accent ?? 'var(--orange)' }}
    >
      <div className="team-card-accent" />
      <div className="team-card-crest">
        <Crest slug={team.slug} size="md" alt={team.name} />
        <button
          className={'team-card-fav' + (fav ? ' is-fav' : '')}
          aria-label={fav ? `Unfavorite ${team.name}` : `Favorite ${team.name}`}
          aria-pressed={fav}
          onClick={e => { e.preventDefault(); e.stopPropagation(); toggleTeam(team.slug); }}
        >
          {fav ? '♥' : '♡'}
        </button>
      </div>
      <div className="team-card-body">
        <div className="team-card-row">
          <Flag slug={team.slug} alt={team.name} className="team-card-flag-chip" loading="lazy" />
          <div className="team-card-meta">
            <span className="team-card-name">{team.name}</span>
            <span className="team-card-group">Group {team.group}</span>
          </div>
        </div>
      </div>
    </Link>
  );
}
