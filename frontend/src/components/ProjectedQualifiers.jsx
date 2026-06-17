import Flag from './Flag.jsx';

function QualifierItem({ team }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 0', borderBottom: '1px solid var(--border)' }}>
      <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '0.7rem', color: 'var(--brand)', width: 16, flexShrink: 0 }}>{team.group}</span>
      <Flag slug={team.slug} alt={team.name} style={{ width: 22, height: 15, objectFit: 'cover', borderRadius: 2, flexShrink: 0 }} />
      <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>{team.name}</span>
    </div>
  );
}

function Column({ title, teams, accent }) {
  return (
    <div style={{ flex: '1 1 220px', background: 'var(--surface)', borderRadius: 'var(--radius-card)', border: '1px solid var(--border)', overflow: 'hidden' }}>
      <div style={{ padding: '10px 14px', background: 'var(--surface-2)', borderBottom: '1px solid var(--border)', fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '0.85rem', letterSpacing: '0.05em', textTransform: 'uppercase', color: accent ?? 'var(--text-muted)' }}>
        {title} <span style={{ fontWeight: 400, marginLeft: 6, fontSize: '0.75rem', color: 'var(--text-muted)' }}>({teams.length})</span>
      </div>
      <div style={{ padding: '4px 14px 8px' }}>
        {teams.map(t => <QualifierItem key={t.slug} team={t} />)}
      </div>
    </div>
  );
}

export default function ProjectedQualifiers({ winners, runnersUp, bestThirds }) {
  return (
    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 32 }}>
      <Column title="Group Winners"  teams={winners}    accent="var(--status-advance)" />
      <Column title="Runners-up"     teams={runnersUp}  accent="var(--text-muted)" />
      <Column title="Best 3rd-place" teams={bestThirds} accent="var(--status-wildcard)" />
    </div>
  );
}
