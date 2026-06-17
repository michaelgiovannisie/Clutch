export default function Card({ as: Tag = 'div', className, children, style }) {
  return (
    <Tag className={className} style={{
      background: 'var(--surface)',
      borderRadius: 'var(--radius-card)',
      boxShadow: 'var(--shadow-card)',
      border: '1px solid var(--border)',
      ...style,
    }}>
      {children}
    </Tag>
  );
}
