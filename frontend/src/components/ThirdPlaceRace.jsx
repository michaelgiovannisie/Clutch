import Flag from './Flag.jsx';

export default function ThirdPlaceRace({ thirds, cutoff = 8 }) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 'var(--radius-card)', border: '1px solid var(--border)', overflow: 'hidden', marginBottom: 32 }}>
      <div style={{ padding: '10px 16px', background: 'var(--surface-2)', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '0.9rem', letterSpacing: '0.05em', textTransform: 'uppercase' }}>Wild-card Race — Best Third-Place Teams</span>
        <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Top {cutoff} qualify</span>
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            {['#', 'Grp', 'Team', 'P', 'W', 'D', 'L', 'GD', 'Pts'].map((h, i) => (
              <th key={i} style={{ padding: '4px 8px', fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: i <= 2 ? 'left' : 'center', textTransform: 'uppercase', letterSpacing: '0.05em', borderBottom: '1px solid var(--border)' }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {thirds.map((t, i) => {
            const isIn = i < cutoff;
            const isCutoff = i === cutoff;
            const rail = isIn ? 'var(--status-wildcard)' : 'transparent';
            return (
              <>
                {isCutoff && (
                  <tr key="cutoff">
                    <td colSpan={9} style={{ padding: '4px 12px', fontSize: '0.7rem', color: 'var(--status-out)', fontWeight: 700, letterSpacing: '0.05em', textTransform: 'uppercase', borderTop: '2px dashed var(--status-out)', background: 'rgba(255,92,108,0.05)' }}>
                      ▼ Out of qualification
                    </td>
                  </tr>
                )}
                <tr key={t.slug} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ width: 4, padding: 0, background: rail }} />
                  <td style={{ padding: '6px 8px', color: 'var(--text-muted)', fontSize: '0.8rem', textAlign: 'center', width: 24 }}>{i + 1}</td>
                  <td style={{ padding: '6px 8px', textAlign: 'center', width: 32 }}>
                    <span style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: '0.75rem', color: 'var(--brand)' }}>{t.group}</span>
                  </td>
                  <td style={{ padding: '6px 8px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <Flag slug={t.slug} alt={t.name} style={{ width: 20, height: 14, objectFit: 'cover', borderRadius: 2, flexShrink: 0 }} />
                      <span style={{ fontWeight: 600, fontSize: '0.85rem' }}>{t.name}</span>
                    </div>
                  </td>
                  {[t.played, t.won, t.drawn, t.lost, t.gd > 0 ? `+${t.gd}` : t.gd, t.points].map((v, j) => (
                    <td key={j} style={{ padding: '6px 8px', textAlign: 'center', fontSize: '0.8rem', color: j === 5 ? 'var(--text)' : 'var(--text-muted)', fontWeight: j === 5 ? 700 : 400 }}>{v}</td>
                  ))}
                </tr>
              </>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
