import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { matches, getUniqueDates, getMatchesByDate } from '../lib/matches.js';
import { useFavorites } from '../context/FavoritesContext.jsx';
import Flag from '../components/Flag.jsx';
import Pill from '../components/Pill.jsx';

const TODAY = new Date().toISOString().slice(0, 10);

function formatTime(iso) {
  return new Date(iso).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
}

function MatchRow({ match }) {
  const { isStarredMatch, toggleMatch } = useFavorites();
  const starred = isStarredMatch(match.id);
  const isLive = match.status === 'LIVE';
  const isFinished = match.status === 'FINISHED';

  return (
    <Link to={`/matches/${match.id}`} className="match-row">
      <div className="match-row-status">
        {isLive && (
          <>
            <Pill tone="live">LIVE</Pill>
            {match.minute && <span className="match-row-minute">{match.minute}'</span>}
          </>
        )}
        {isFinished && <Pill tone="neutral">FT</Pill>}
        {!isLive && !isFinished && (
          <span className="match-row-time">{formatTime(match.kickoff)}</span>
        )}
      </div>

      <div className="match-row-teams">
        <div className="match-row-team match-row-home">
          <Flag slug={match.home.slug} alt={match.home.name} className="match-row-flag" />
          <span className="match-row-abbr">{match.home.abbr}</span>
        </div>
        <span className="match-row-score">
          {isLive || isFinished ? `${match.homeScore}–${match.awayScore}` : 'vs'}
        </span>
        <div className="match-row-team match-row-away">
          <span className="match-row-abbr">{match.away.abbr}</span>
          <Flag slug={match.away.slug} alt={match.away.name} className="match-row-flag" />
        </div>
      </div>

      <span className="match-row-venue">{match.city}</span>

      <button
        className={'match-row-star' + (starred ? ' is-starred' : '')}
        aria-label={starred ? 'Remove from watchlist' : 'Add to watchlist'}
        aria-pressed={starred}
        onClick={e => { e.preventDefault(); e.stopPropagation(); toggleMatch(match.id); }}
      >
        {starred ? '★' : '☆'}
      </button>
    </Link>
  );
}

export default function Matches() {
  const dates = useMemo(() => getUniqueDates(), []);
  const clampedToday = dates.includes(TODAY) ? TODAY : dates[0];
  const [selectedDate, setSelectedDate] = useState(clampedToday);
  const [filter, setFilter] = useState('all');
  const { favoriteTeams, starredMatches } = useFavorites();

  const dayMatches = useMemo(() => getMatchesByDate(selectedDate), [selectedDate]);

  const visible = useMemo(() => {
    if (filter === 'starred') return dayMatches.filter(m => starredMatches.has(m.id));
    if (filter === 'myteams') return dayMatches.filter(m =>
      favoriteTeams.has(m.home.slug) || favoriteTeams.has(m.away.slug)
    );
    return dayMatches;
  }, [dayMatches, filter, starredMatches, favoriteTeams]);

  const subtitleDate = new Date(selectedDate + 'T12:00:00').toLocaleDateString(undefined, {
    weekday: 'long', month: 'long', day: 'numeric',
  });
  const subtitle = selectedDate === TODAY ? "Today's matches" : `Matches on ${subtitleDate}`;

  return (
    <div className="page">
      <h1 className="page-title">Matches</h1>
      <p className="page-subtitle">{subtitle} · times in your local timezone</p>

      {/* Date navigator */}
      <div className="date-nav">
        <button className="group-pill date-nav-today" onClick={() => setSelectedDate(clampedToday)}>
          Today
        </button>
        {dates.map(d => {
          const dt = new Date(d + 'T12:00:00');
          const hasStarred = matches.filter(m => m.date === d).some(m => starredMatches.has(m.id));
          return (
            <button
              key={d}
              className={'date-nav-day' + (d === selectedDate ? ' is-selected' : '')}
              onClick={() => setSelectedDate(d)}
            >
              <span className="day-num">{dt.getDate()}</span>
              <span className="day-mon">{dt.toLocaleDateString(undefined, { month: 'short' })}</span>
              {hasStarred && <span className="day-star" />}
            </button>
          );
        })}
      </div>

      {/* Filter chips */}
      <div className="group-filter" style={{ marginBottom: 12 }}>
        {[['all', 'All'], ['myteams', 'My teams'], ['starred', 'Starred']].map(([id, label]) => (
          <button
            key={id}
            className={'group-pill' + (filter === id ? ' is-active' : '')}
            onClick={() => setFilter(id)}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Match list */}
      <div style={{ background: 'var(--surface)', borderRadius: 'var(--radius-card)', border: '1px solid var(--border)', overflow: 'hidden' }}>
        {visible.length === 0 ? (
          <p style={{ padding: '24px 16px', color: 'var(--text-muted)', textAlign: 'center', margin: 0 }}>
            {filter === 'starred'
              ? 'No starred matches yet — tap ☆ on a match to add it.'
              : filter === 'myteams'
              ? 'No matches for your favorite teams on this day.'
              : 'No matches on this day.'}
          </p>
        ) : (
          visible.map(m => <MatchRow key={m.id} match={m} />)
        )}
      </div>
    </div>
  );
}
