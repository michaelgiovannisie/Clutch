import { useEffect, useState } from 'react';

export default function Home() {
  const [status, setStatus] = useState('checking…');
  useEffect(() => {
    fetch('/api/venues')
      .then(() => setStatus('online'))
      .catch(() => setStatus('offline — start the backend on :8080'));
  }, []);
  return (
    <div className="page">
      <h1>World Cup Tracker</h1>
      <p className="lede">React SPA + Spring Boot REST API — architecture skeleton.</p>
      <p>Backend status: <strong>{status}</strong></p>
      <p className="muted">Feature pages and API endpoints arrive in later phases — see PLAN.md.</p>
    </div>
  );
}
