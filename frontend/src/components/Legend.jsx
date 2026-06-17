const items = [
  { color: 'var(--status-advance)',  label: 'Advancing (1st / 2nd)' },
  { color: 'var(--status-wildcard)', label: 'Wild-card in (best 3rd)' },
  { color: 'var(--text-muted)',      label: 'Outside cutoff (group stage not complete)' },
];

export default function Legend() {
  return (
    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 20 }}>
      {items.map(item => (
        <div key={item.label} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 10, height: 10, borderRadius: '50%', background: item.color, flexShrink: 0 }} />
          <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{item.label}</span>
        </div>
      ))}
    </div>
  );
}
