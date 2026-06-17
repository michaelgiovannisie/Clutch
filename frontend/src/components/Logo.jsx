export default function Logo({ size = 'md', withWordmark = false }) {
  const px = size === 'sm' ? 28 : 40;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <img
        src="/logos/app-icon.png"
        alt="World Cup Tracker logo"
        width={px}
        height={px}
        style={{ objectFit: 'contain' }}
        onError={(e) => { e.currentTarget.style.display = 'none'; }}
      />
      {withWordmark && (
        <span style={{
          fontFamily: 'var(--font-display)',
          fontWeight: 700,
          fontSize: size === 'sm' ? '0.95rem' : '1.15rem',
          letterSpacing: '0.02em',
          textTransform: 'uppercase',
          background: 'var(--brand-gradient)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
        }}>
          World Cup Tracker
        </span>
      )}
    </div>
  );
}
