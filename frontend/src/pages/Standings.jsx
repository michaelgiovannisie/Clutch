import { useState } from 'react';
import { groups as rawGroups } from '../data/mockStandings.js';
import { annotateGroups, getProjectedQualifiers, getRankedThirds } from '../lib/standings.js';
import { BRACKET_SLOTS, resolveSlot } from '../lib/bracketTemplate.js';
import Tabs from '../components/Tabs.jsx';
import Legend from '../components/Legend.jsx';
import GroupTable from '../components/GroupTable.jsx';
import ThirdPlaceRace from '../components/ThirdPlaceRace.jsx';
import ProjectedQualifiers from '../components/ProjectedQualifiers.jsx';
import ProjectedR32List from '../components/ProjectedR32List.jsx';

const TABS = [
  { id: 'group',   label: 'Group Stage' },
  { id: 'bracket', label: 'Bracket' },
];

export default function Standings() {
  const [tab, setTab] = useState('group');
  const annotated = annotateGroups(rawGroups);
  const thirds     = getRankedThirds(rawGroups);
  const { winners, runnersUp, bestThirds } = getProjectedQualifiers(rawGroups);
  const matchups = BRACKET_SLOTS.map(slot => ({
    id:   slot.id,
    home: resolveSlot(slot.home, { winners, runnersUp }),
    away: resolveSlot(slot.away, { winners, runnersUp }),
  }));

  return (
    <div>
      <h1 style={{ fontFamily: 'var(--font-display)', fontWeight: 700, fontSize: 'clamp(1.4rem, 3vw, 2rem)', letterSpacing: '0.03em', textTransform: 'uppercase', margin: '0 0 4px' }}>
        Standings
      </h1>
      <p style={{ color: 'var(--text-muted)', margin: '0 0 20px', fontSize: '0.9rem' }}>
        Projected — based on standings as of today. Knockouts begin June 28.
      </p>
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'group' && (
        <>
          <Legend />
          <ThirdPlaceRace thirds={thirds} cutoff={8} />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 16 }}>
            {annotated.map(g => <GroupTable key={g.id} group={g} />)}
          </div>
        </>
      )}
      {tab === 'bracket' && (
        <>
          <ProjectedQualifiers winners={winners} runnersUp={runnersUp} bestThirds={bestThirds} />
          <ProjectedR32List matchups={matchups} />
        </>
      )}
    </div>
  );
}
