import Flag from './Flag.jsx';

const stateColor = {
  advance:  'var(--status-advance)',
  wildcard: 'var(--status-wildcard)',
  out:      'transparent',
  none:     'transparent',
};

export default function StandingsRow({ row, rank, state = 'none' }) {
  const rail = stateColor[state] ?? 'transparent';
  return (
    <tr style={{ borderBottom: '1px solid var(--border)', height: 40 }}>
      <td style={{ width: 4, padding: 0, background: rail, borderRadius: '2px 0 0 2px' }} />
      <td style={{ padding: '0 8px', color: 'var(--text-muted)', fontSize: '0.8rem', textAlign: 'center', width: 24 }}>{rank}</td>
      <td style={{ padding: '0 8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, overflow: 'hidden' }}>
          <Flag slug={row.slug} alt={row.name} style={{ width: 20, height: 14, objectFit: 'cover', borderRadius: 2, flexShrink: 0 }} />
          <span style={{ fontWeight: 600, fontSize: '0.85rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{row.name}</span>
          <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem', flexShrink: 0 }}>{row.abbr}</span>
        </div>
      </td>
      {[row.played, row.won, row.drawn, row.lost, row.gf, row.ga, row.gd > 0 ? `+${row.gd}` : row.gd].map((v, i) => (
        <td key={i} style={{ padding: '0 6px', textAlign: 'center', fontSize: '0.8rem', color: 'var(--text-muted)' }}>{v}</td>
      ))}
      <td style={{ padding: '0 8px', textAlign: 'center', fontWeight: 700, fontSize: '0.85rem', color: 'var(--text)' }}>{row.points}</td>
    </tr>
  );
}
