export default function Flag({ slug, alt, className, style }) {
  return (
    <img
      src={`/flags/${slug}.jpg`}
      alt={alt ?? slug}
      className={className}
      style={style}
      onError={(e) => { e.currentTarget.style.display = 'none'; }}
    />
  );
}
