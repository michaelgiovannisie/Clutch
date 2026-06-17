import { useState } from 'react';

const sizePx = { sm: 40, md: 72, lg: 120 };

export default function Crest({ slug, crestUrl, alt, size = 'md' }) {
  const [src, setSrc] = useState(crestUrl || `/crests/${slug}.jpg`);
  const px = sizePx[size] ?? sizePx.md;

  return (
    <img
      src={src}
      alt={alt ?? slug}
      width={px}
      height={px}
      style={{ objectFit: 'contain' }}
      onError={() => {
        if (src !== `/crests/${slug}.jpg`) setSrc(`/crests/${slug}.jpg`);
      }}
    />
  );
}
