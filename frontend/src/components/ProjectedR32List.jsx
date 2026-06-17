import Flag from './Flag.jsx';

function TeamChip({ team }) {
  if (!team) return <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem', fontStyle: 'italic' }}>(TBD)</span>;
  if (team.placeholder) return <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem', fontStyle: 'italic' }}>{team.label}</span>;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <Flag slug={team.slug} alt={team.name} style={{ width: 22, height: 15, objectFit: 'cover', borderRadius: 2, flexShrink: 0 }} />
      <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>{team.name}</span>
      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>({team.group})</span>
    </div>
  );
}

export default function ProjectedR32List({ matchups }) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 'var(--radius-card)', border: '1px solid var(--border)', overflow: 'hidden' }}>
      <div style={{ padding: '10px 16px', background: 'var(--surface-2)', borderBottom: '1px solid var(--border)', fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '0.9rem', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
        Projected Round of 32 Matchups
      </div>
      <div style={{ padding: '8px 0' }}>
        {matchups.map((m, i) => (
          <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 16px', borderBottom: i < matchups.length - 1 ? '1px solid var(--border)' : 'none' }}>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem', width: 36, flexShrink: 0 }}>M{m.id}</span>
            <div style={{ flex: 1 }}><TeamChip team={m.home} /></div>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem', fontWeight: 700 }}>vs</span>
            <div style={{ flex: 1 }}><TeamChip team={m.away} /></div>
          </div>
        ))}
      </div>
    </div>
  );
}
