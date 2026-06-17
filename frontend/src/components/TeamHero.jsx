import Flag from './Flag.jsx';
import Crest from './Crest.jsx';

export default function TeamHero({ team }) {
  return (
    <div style={{ '--team-accent': team.accent, '--team-accent-2': team.accentEnd }}>
      <div style={{ position: 'relative', width: '100%', height: 260, overflow: 'hidden', borderRadius: 'var(--radius-card)', marginBottom: 24 }}>
        <Flag slug={team.slug} alt="" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', opacity: 0.35 }} />
        <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(160deg, rgba(10,14,22,0.55) 0%, var(--team-accent-2) 100%)' }} />
        <div style={{ position: 'relative', height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 12, padding: '24px 16px' }}>
          <Crest slug={team.slug} crestUrl={team.crestUrl} alt={team.name} size="lg" />
          <h1 style={{ margin: 0, fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: 'clamp(1.6rem, 4vw, 2.4rem)', letterSpacing: '0.04em', textTransform: 'uppercase', color: 'var(--text)', textAlign: 'center' }}>
            {team.name}
          </h1>
        </div>
      </div>
    </div>
  );
}
