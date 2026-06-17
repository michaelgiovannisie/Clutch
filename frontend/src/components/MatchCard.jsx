import Pill from './Pill.jsx';

function TeamRow({ abbr, score }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: 8 }}>
      <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '1.15rem', letterSpacing: '0.04em', textTransform: 'uppercase', color: 'var(--text)' }}>
        {abbr}
      </span>
      {score !== undefined && (
        <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '1.15rem', color: 'var(--text)' }}>
          {score}
        </span>
      )}
    </div>
  );
}

export default function MatchCard({ match }) {
  const isLive = match.status === 'LIVE';
  return (
    <div style={{
      background: 'var(--team-gradient)',
      borderRadius: 'var(--radius-card)',
      boxShadow: 'var(--shadow-card)',
      border: '1px solid var(--border)',
      padding: '16px',
      display: 'flex',
      gap: 12,
      alignItems: 'center',
      minWidth: 200,
      flex: '1 1 200px',
    }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 4, alignItems: 'flex-start', minWidth: 52 }}>
        {isLive ? (
          <>
            <Pill tone="live">Live</Pill>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>{match.clock}</span>
          </>
        ) : (
          <>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem', fontWeight: 600 }}>{match.date}</span>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>{match.time}</span>
          </>
        )}
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
        <TeamRow abbr={match.home.abbr} score={isLive ? match.home.score : undefined} />
        <TeamRow abbr={match.away.abbr} score={isLive ? match.away.score : undefined} />
      </div>
      {isLive && (
        <button style={{
          width: 28, height: 28, borderRadius: '50%',
          border: '1.5px solid rgba(255,255,255,0.4)',
          background: 'rgba(255,255,255,0.1)',
          color: 'var(--text)', fontWeight: 700, fontSize: '0.8rem',
          cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
        }}>i</button>
      )}
    </div>
  );
}
