import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import VENUE_ATTRACTIONS from '../data/venueAttractions.js';

function Gallery({ images, label }) {
  const [idx, setIdx] = useState(0);

  if (!images || images.length === 0) {
    return (
      <div className="venue-gallery-empty">
        <span style={{ fontSize: '2rem', opacity: 0.3 }}>🏟️</span>
        <p>No {label} photos available</p>
      </div>
    );
  }

  const prev = () => setIdx(i => (i - 1 + images.length) % images.length);
  const next = () => setIdx(i => (i + 1) % images.length);

  return (
    <div className="venue-gallery">
      <div className="venue-gallery-hero">
        <img
          key={images[idx]}
          src={images[idx]}
          alt={`${label} photo ${idx + 1}`}
          className="venue-gallery-img"
        />
        {images.length > 1 && (
          <>
            <button className="venue-gallery-btn venue-gallery-btn--prev" onClick={prev}>‹</button>
            <button className="venue-gallery-btn venue-gallery-btn--next" onClick={next}>›</button>
            <div className="venue-gallery-count">{idx + 1} / {images.length}</div>
          </>
        )}
      </div>
      {images.length > 1 && (
        <div className="venue-gallery-dots">
          {images.map((_, i) => (
            <button
              key={i}
              className={`venue-gallery-dot${i === idx ? ' is-active' : ''}`}
              onClick={() => setIdx(i)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function WeatherWidget({ venueId }) {
  const [weather, setWeather] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch(`/api/venues/${venueId}/weather`)
      .then(r => r.json())
      .then(data => { setWeather(data); setLoading(false); })
      .catch(() => setLoading(false));
  }, [venueId]);

  if (loading) return <div className="venue-weather-loading">Loading weather…</div>;
  if (!weather || Object.keys(weather).length === 0) {
    return <div className="venue-weather-error">Weather data unavailable</div>;
  }

  return (
    <div className="venue-weather">
      <span className="venue-weather-icon">{weather.icon}</span>
      <div className="venue-weather-info">
        <div className="venue-weather-temp">{Math.round(weather.tempF)}°F</div>
        <div className="venue-weather-label">{weather.label}</div>
        <div className="venue-weather-wind">Wind: {Math.round(weather.windMph)} mph</div>
      </div>
    </div>
  );
}

function AttractionsSection({ venueId }) {
  const [photoSets, setPhotoSets] = useState([]);
  const [activeIdx, setActiveIdx] = useState(0);
  const attractions = VENUE_ATTRACTIONS[Number(venueId)] || [];

  useEffect(() => {
    if (!attractions.length) return;
    fetch(`/api/venues/${venueId}/attraction-images`)
      .then(r => r.json())
      .then(setPhotoSets)
      .catch(() => {});
  }, [venueId]);

  if (!attractions.length) return <p className="muted">No attractions listed for this venue.</p>;

  return (
    <div className="venue-attractions">
      <div className="venue-attractions-pills">
        {attractions.map((a, i) => (
          <button
            key={i}
            className={`group-pill${i === activeIdx ? ' is-active' : ''}`}
            onClick={() => setActiveIdx(i)}
          >
            {a.icon} {a.name}
          </button>
        ))}
      </div>

      <div className="venue-attraction-detail">
        <div className="venue-attraction-meta">
          <span className="venue-attraction-icon">{attractions[activeIdx].icon}</span>
          <div>
            <div className="venue-attraction-name">{attractions[activeIdx].name}</div>
            <div className="venue-attraction-type">
              {attractions[activeIdx].type} · {attractions[activeIdx].dist}
            </div>
          </div>
        </div>
        <Gallery images={photoSets[activeIdx] || []} label={attractions[activeIdx].name} />
      </div>
    </div>
  );
}

export default function VenueDetail() {
  const { id } = useParams();
  const [venue, setVenue] = useState(null);
  const [stadiumImages, setStadiumImages] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetch(`/api/venues/${id}`).then(r => r.ok ? r.json() : null),
      fetch(`/api/venues/${id}/images`).then(r => r.json()).catch(() => []),
    ]).then(([v, imgs]) => {
      setVenue(v);
      setStadiumImages(imgs);
      setLoading(false);
    });
  }, [id]);

  if (loading) return <div className="page"><p className="muted">Loading…</p></div>;
  if (!venue) return <div className="page"><p className="muted">Venue not found.</p></div>;

  return (
    <div className="venue-detail-page">
      <Link to="/venues" className="back-link">← Venues</Link>

      <div className="venue-detail-header">
        <span className="venue-detail-flag">{venue.flag}</span>
        <div>
          <h1 className="venue-detail-name">{venue.name}</h1>
          <div className="venue-detail-meta">
            <span>{venue.city}</span>
            <span className="venue-detail-sep">·</span>
            <span>{venue.country}</span>
            <span className="venue-detail-sep">·</span>
            <span>{venue.capacity.toLocaleString()} seats</span>
          </div>
        </div>
      </div>

      <div className="venue-detail-grid">
        <div className="venue-detail-main">
          <section className="venue-section">
            <h2 className="venue-section-title">Stadium Gallery</h2>
            <Gallery images={stadiumImages} label={venue.name} />
          </section>

          <section className="venue-section">
            <h2 className="venue-section-title">Nearby Attractions</h2>
            <AttractionsSection venueId={id} />
          </section>
        </div>

        <aside className="venue-detail-sidebar">
          <div className="venue-info-card">
            <div className="venue-section-title" style={{ marginBottom: '1rem' }}>Venue Info</div>
            <div className="venue-stat">
              <span className="venue-stat-label">City</span>
              <span className="venue-stat-value">{venue.city}</span>
            </div>
            <div className="venue-stat">
              <span className="venue-stat-label">Country</span>
              <span className="venue-stat-value">{venue.flag} {venue.country}</span>
            </div>
            <div className="venue-stat">
              <span className="venue-stat-label">Capacity</span>
              <span className="venue-stat-value">{venue.capacity.toLocaleString()}</span>
            </div>
            <div className="venue-stat">
              <span className="venue-stat-label">Coordinates</span>
              <span className="venue-stat-value" style={{ fontFamily: 'monospace', fontSize: '0.78rem' }}>
                {venue.lat}°, {venue.lng}°
              </span>
            </div>
          </div>

          <div className="venue-info-card">
            <div className="venue-section-title" style={{ marginBottom: '1rem' }}>Live Weather</div>
            <WeatherWidget venueId={id} />
          </div>
        </aside>
      </div>
    </div>
  );
}
