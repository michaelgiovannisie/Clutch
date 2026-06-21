import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { fetchMatchById, fetchMatchCommentary } from '../lib/api.js';
import { useFavorites } from '../context/FavoritesContext.jsx';
import Flag from '../components/Flag.jsx';
import Crest from '../components/Crest.jsx';
import Pill from '../components/Pill.jsx';

function formatKickoff(iso) {
  return new Date(iso).toLocaleString(undefined, {
    weekday: 'short', month: 'short', day: 'numeric',
    hour: 'numeric', minute: '2-digit',
  });
}

export default function MatchDetail() {
  const { id } = useParams();
  const [match, setMatch] = useState(null);
  const [loading, setLoading] = useState(true);
  const [commentary, setCommentary] = useState(null);
  const [commentaryLoading, setCommentaryLoading] = useState(true);
  const { isStarredMatch, toggleMatch } = useFavorites();

  useEffect(() => {
    setLoading(true);
    fetchMatchById(id).then(m => { setMatch(m); setLoading(false); });
  }, [id]);

  useEffect(() => {
    setCommentaryLoading(true);
    setCommentary(null);
    fetchMatchCommentary(id).then(text => { setCommentary(text); setCommentaryLoading(false); });
  }, [id]);

  if (loading) {
    return (
      <div className="page">
        <Link to="/matches" className="back-link" style={{ display: 'inline-block', marginBottom: 16 }}>← Matches</Link>
        <p style={{ color: 'var(--text-muted)' }}>Loading…</p>
      </div>
    );
  }

  if (!match) {
    return (
      <div className="page">
        <Link to="/matches" className="back-link" style={{ display: 'inline-block', marginBottom: 16 }}>← Matches</Link>
        <h1 className="page-title">Match not found</h1>
      </div>
    );
  }

  const starred = isStarredMatch(match.id);
  const isLive = match.status === 'LIVE';
  const isFinished = match.status === 'FINISHED';

  return (
    <div className="page">
      <Link to="/matches" className="back-link" style={{ display: 'inline-block', marginBottom: 16 }}>← Matches</Link>

      {/* Hero matchup card */}
      <div className="match-detail-hero">
        <div className="match-detail-team">
          <Flag slug={match.home.slug} alt={match.home.name} className="match-detail-flag" />
          <Crest slug={match.home.slug} size="lg" alt={match.home.name} />
          <span className="match-detail-name">{match.home.name}</span>
          <span className="match-detail-abbr">{match.home.abbr}</span>
        </div>

        <div className="match-detail-score-block">
          {isLive && <Pill tone="live">LIVE {match.minute}'</Pill>}
          {isFinished && <Pill tone="neutral">Full Time</Pill>}
          <div className="match-detail-score">
            {isLive || isFinished ? `${match.homeScore} – ${match.awayScore}` : 'vs'}
          </div>
          <span className="match-detail-stage">{match.stage}</span>
        </div>

        <div className="match-detail-team">
          <Flag slug={match.away.slug} alt={match.away.name} className="match-detail-flag" />
          <Crest slug={match.away.slug} size="lg" alt={match.away.name} />
          <span className="match-detail-name">{match.away.name}</span>
          <span className="match-detail-abbr">{match.away.abbr}</span>
        </div>
      </div>

      {/* Info bar */}
      <div className="match-detail-info">
        {match.kickoff && <span>{formatKickoff(match.kickoff)}</span>}
        {match.venueName && <><span className="match-detail-info-sep">·</span><span>{match.venueName}</span></>}
        {match.city && <><span className="match-detail-info-sep">·</span><span>{match.city}</span></>}
        <button
          className={'match-row-star match-detail-star' + (starred ? ' is-starred' : '')}
          aria-label={starred ? 'Remove from watchlist' : 'Add to watchlist'}
          aria-pressed={starred}
          onClick={() => toggleMatch(match.id)}
        >
          {starred ? '★' : '☆'} {starred ? 'Watching' : 'Watch'}
        </button>
      </div>

      {/* AI commentary slot */}
      <div style={{ background: 'var(--surface)', borderRadius: 'var(--radius-card)', border: '1px solid var(--border)', overflow: 'hidden', marginTop: 20 }}>
        <div className="card-header section-title">AI Commentary</div>
        {commentaryLoading && (
          <p style={{ padding: '16px', color: 'var(--text-muted)', margin: 0, fontSize: '0.9rem' }}>
            Generating pre-match analysis…
          </p>
        )}
        {!commentaryLoading && commentary && (
          <p style={{ padding: '16px', color: 'var(--text)', margin: 0, fontSize: '0.95rem', lineHeight: 1.6 }}>
            {commentary}
          </p>
        )}
        {!commentaryLoading && !commentary && (
          <p style={{ padding: '16px', color: 'var(--text-muted)', margin: 0, fontSize: '0.9rem' }}>
            Commentary coming soon — check back later.
          </p>
        )}
      </div>
    </div>
  );
}
