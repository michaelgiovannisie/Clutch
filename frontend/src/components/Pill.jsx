const toneStyles = {
  live:    { background: 'var(--live)',      color: '#fff' },
  brand:   { background: 'var(--brand)',     color: '#000' },
  neutral: { background: 'var(--surface-2)', color: 'var(--text-muted)', border: '1px solid var(--border)' },
};

export default function Pill({ tone = 'neutral', children }) {
  return (
    <span style={{
      display: 'inline-block',
      padding: '0.15rem 0.55rem',
      borderRadius: 'var(--radius-pill)',
      fontWeight: 700,
      fontSize: '0.7rem',
      letterSpacing: '0.06em',
      textTransform: 'uppercase',
      ...toneStyles[tone],
    }}>
      {children}
    </span>
  );
}
