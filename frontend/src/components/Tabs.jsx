export default function Tabs({ tabs, active, onChange }) {
  return (
    <div style={{ display: 'flex', gap: 4, borderBottom: '1px solid var(--border)', marginBottom: 24 }}>
      {tabs.map(tab => {
        const isActive = tab.id === active;
        return (
          <button key={tab.id} onClick={() => onChange(tab.id)} style={{
            background: 'none', border: 'none',
            borderBottom: isActive ? '2px solid var(--brand)' : '2px solid transparent',
            color: isActive ? 'var(--text)' : 'var(--text-muted)',
            fontFamily: 'var(--font-display)', fontWeight: 700,
            fontSize: '1rem', letterSpacing: '0.04em', textTransform: 'uppercase',
            padding: '10px 20px', cursor: 'pointer', marginBottom: -1,
            transition: 'color 0.15s, border-color 0.15s',
          }}>
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
