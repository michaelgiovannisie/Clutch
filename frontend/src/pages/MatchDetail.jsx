import { useParams } from 'react-router-dom';
export default function MatchDetail() {
  const { id } = useParams();
  return <div className="page"><h1>Match {id}</h1><p className="muted">Match detail — coming soon.</p></div>;
}
