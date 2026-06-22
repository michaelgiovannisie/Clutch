import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { fetchMatches } from '../lib/api.js';
import Flag from '../components/Flag.jsx';
import Pill from '../components/Pill.jsx';

// ── Tournament phase ─────────────────────────────────────────────────────────

const PHASES = [
  { start: new Date('2026-06-11'), label: 'Group Stage',   next: new Date('2026-06-28'), nextLabel: 'Round of 32 begins' },
  { start: new Date('2026-06-28'), label: 'Round of 32',   next: new Date('2026-07-05'), nextLabel: 'Round of 16 begins' },
  { start: new Date('2026-07-05'), label: 'Round of 16',   next: new Date('2026-07-11'), nextLabel: 'Quarterfinals begin' },
  { start: new Date('2026-07-11'), label: 'Quarterfinals', next: new Date('2026-07-15'), nextLabel: 'Semifinals begin' },
  { start: new Date('2026-07-15'), label: 'Semifinals',    next: new Date('2026-07-19'), nextLabel: 'Final day' },
  { start: new Date('2026-07-19'), label: 'Final',         next: new Date('2026-07-20'), nextLabel: 'Tournament ends' },
];
const KICKOFF = new Date('2026-06-11');
const END     = new Date('2026-07-20');

function getPhase(now) {
  if (now >= END)    return { label: 'Concluded', target: null, targetLabel: null };
  if (now < KICKOFF) return { label: 'Countdown', target: KICKOFF, targetLabel: 'Tournament kicks off' };
  let phase = PHASES[0];
  for (const p of PHASES) { if (now >= p.start) phase = p; }
  return { label: phase.label, target: phase.next, targetLabel: phase.nextLabel };
}

function useNow() {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 30_000);
    return () => clearInterval(id);
  }, []);
  return now;
}

function CountdownNumerals({ target, label }) {
  const now = useNow();
  const diff = Math.max(0, target - now);
  const days = Math.floor(diff / 86400000);
  const hrs  = Math.floor((diff % 86400000) / 3600000);
  const mins = Math.floor((diff % 3600000)  / 60000);
  return (
    <div className="home-countdown">
      <p className="home-countdown-label">{label} in</p>
      <div className="home-countdown-numerals">
        {days > 0 && (
          <><span className="home-countdown-num">{days}</span><span className="home-countdown-unit">d</span></>
        )}
        <span className="home-countdown-num">{String(hrs).padStart(2, '0')}</span>
        <span className="home-countdown-unit">h</span>
        <span className="home-countdown-num">{String(mins).padStart(2, '0')}</span>
        <span className="home-countdown-unit">m</span>
      </div>
    </div>
  );
}

// ── Match strip ──────────────────────────────────────────────────────────────

function MatchStripCard({ match }) {
  const isLive     = match.status === 'LIVE';
  const isFinished = match.status === 'FINISHED';
  const hasScore   = match.homeScore !== null && match.awayScore !== null;

  return (
    <Link to={`/matches/${match.id}`} className="home-match-card">
      <div className="home-match-stage">{match.stage}</div>
      <div className="home-match-teams">
        <div className="home-match-team">
          <Flag slug={match.home.slug} alt={match.home.name} className="home-match-flag" />
          <span className="home-match-abbr">{match.home.abbr}</span>
        </div>

        <div className="home-match-centre">
          {hasScore ? (
            <span className="home-match-scoreline">{match.homeScore} – {match.awayScore}</span>
          ) : (
            <span className="home-match-vs">vs</span>
          )}
          {isLive && <Pill tone="live">{match.minute ? `${match.minute}'` : 'Live'}</Pill>}
          {isFinished && <span className="home-match-ft">FT</span>}
          {!isLive && !isFinished && match.kickoff && (
            <span className="home-match-time">
              {new Date(match.kickoff).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
            </span>
          )}
        </div>

        <div className="home-match-team home-match-team--away">
          <span className="home-match-abbr">{match.away.abbr}</span>
          <Flag slug={match.away.slug} alt={match.away.name} className="home-match-flag" />
        </div>
      </div>
    </Link>
  );
}

function MatchStrip({ matches }) {
  if (matches === null) {
    return (
      <div className="home-match-strip">
        {[0, 1, 2].map(i => <div key={i} className="home-match-card home-match-card--skeleton" />)}
      </div>
    );
  }
  if (matches.length === 0) {
    return <p className="home-strip-empty">No matches scheduled — check back soon</p>;
  }
  return (
    <div className="home-match-strip">
      {matches.map(m => <MatchStripCard key={m.id} match={m} />)}
    </div>
  );
}

// ── Nav cards ────────────────────────────────────────────────────────────────

const NAV_CARDS = [
  { to: '/teams',     title: 'Teams',     icon: <IconTeams />,     hook: d => `${d.teamCount} nations` },
  { to: '/standings', title: 'Standings', icon: <IconStandings />, hook: d => d.phase },
  { to: '/matches',   title: 'Matches',   icon: <IconMatches />,   hook: d => d.matchesToday > 0 ? `${d.matchesToday} today` : 'View schedule' },
  { to: '/venues',    title: 'Venues',    icon: <IconVenues />,    hook: ()  => '16 host stadiums' },
];

// ── Home ─────────────────────────────────────────────────────────────────────

export default function Home() {
  const now = useNow();
  const { label: phase, target, targetLabel } = getPhase(now);
  const [matches, setMatches] = useState(null);

  useEffect(() => {
    let cancelled = false;
    fetchMatches().then(all => { if (!cancelled) setMatches(all); });
    return () => { cancelled = true; };
  }, []);

  const todayStr     = now.toISOString().slice(0, 10);
  const todayMatches = matches?.filter(m => m.date === todayStr) ?? null;
  const liveMatches  = matches?.filter(m => m.status === 'LIVE') ?? [];
  const stripLabel   = todayMatches?.length > 0 ? "Today's Matches" : "Up Next";
  const stripMatches = matches === null ? null
    : todayMatches?.length > 0 ? todayMatches
    : matches.filter(m => m.status === 'SCHEDULED').slice(0, 4);

  const dynamicData = {
    teamCount:    48,
    phase,
    matchesToday: todayMatches?.length ?? 0,
  };

  const isConcluded = phase === 'Concluded';

  return (
    <div className="home-page">

      {/* ── Hero ── */}
      <div className="home-hero surface-card">
        <div className="home-hero-phase-row">
          <span className={`home-phase-badge${phase !== 'Countdown' && !isConcluded ? ' home-phase-badge--live' : ''}`}>
            {phase}
            {liveMatches.length > 0 && <span className="home-phase-live-dot" aria-hidden="true" />}
          </span>
        </div>

        <h1 className="page-title" style={{ marginBottom: 0 }}>Ballon d'Or</h1>
        <p className="page-subtitle">Your guide to the FIFA World Cup 2026™</p>

        {!isConcluded && target && <CountdownNumerals target={target} label={targetLabel} />}
        {isConcluded && <p className="home-concluded">FIFA World Cup 2026™ has concluded</p>}
      </div>

      {/* ── Today / Up next ── */}
      <div className="home-strip-section">
        <div className="home-strip-header">
          <span className="section-title">{stripLabel}</span>
          <Link to="/matches" className="home-strip-more">All matches →</Link>
        </div>
        <MatchStrip matches={stripMatches} />
      </div>

      {/* ── Quick-nav cards ── */}
      <div className="home-nav-grid">
        {NAV_CARDS.map(card => (
          <Link key={card.to} to={card.to} className="home-nav-card">
            <span className="home-nav-icon" aria-hidden="true">{card.icon}</span>
            <strong className="home-nav-title">{card.title}</strong>
            <p className="home-nav-hook">{card.hook(dynamicData)}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}

function IconTeams() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="7" r="4"/><circle cx="17" cy="9" r="3"/>
      <path d="M1 21v-2a6 6 0 0 1 11.78-1.6"/><path d="M16 21v-2a4 4 0 0 1 4.78-3.9"/>
    </svg>
  );
}
function IconStandings() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/>
      <line x1="8" y1="18" x2="21" y2="18"/>
      <line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/>
      <line x1="3" y1="18" x2="3.01" y2="18"/>
    </svg>
  );
}
function IconMatches() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/>
      <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
      <line x1="2" y1="12" x2="22" y2="12"/>
    </svg>
  );
}
function IconVenues() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 0 1 18 0z"/>
      <circle cx="12" cy="10" r="3"/>
    </svg>
  );
}
