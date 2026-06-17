import TeamHero from '../components/TeamHero.jsx';
import MatchCard from '../components/MatchCard.jsx';
import { team, matches } from '../data/mockTeam.js';

export default function DesignReference() {
  return (
    <div style={{ '--team-accent': team.accent, '--team-accent-2': team.accentEnd }}>
      <TeamHero team={team} />
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
        {matches.map((m) => <MatchCard key={m.id} match={m} />)}
      </div>
    </div>
  );
}
