import { useState, useEffect, useMemo } from 'react';
import { groups as mockGroups } from '../data/mockStandings.js';
import { annotateGroups, getProjectedQualifiers, getRankedThirds } from '../lib/standings.js';
import { BRACKET_SLOTS, resolveSlot } from '../lib/bracketTemplate.js';
import { fetchStandings } from '../lib/api.js';
import Tabs from '../components/Tabs.jsx';
import Legend from '../components/Legend.jsx';
import GroupTable from '../components/GroupTable.jsx';
import ProjectedQualifiers from '../components/ProjectedQualifiers.jsx';
import ProjectedR32List from '../components/ProjectedR32List.jsx';

const TABS = [
  { id: 'group',   label: 'Group Stage' },
  { id: 'bracket', label: 'Bracket' },
];

export default function Standings() {
  const [tab, setTab] = useState('group');
  const [rawGroups, setRawGroups] = useState(mockGroups);

  useEffect(() => {
    fetchStandings().then(setRawGroups);
  }, []);

  const annotated = useMemo(() => annotateGroups(rawGroups), [rawGroups]);
  const { winners, runnersUp, bestThirds } = useMemo(() => getProjectedQualifiers(rawGroups), [rawGroups]);
  const matchups = useMemo(() => BRACKET_SLOTS.map(slot => ({
    id:   slot.id,
    home: resolveSlot(slot.home, { winners, runnersUp }),
    away: resolveSlot(slot.away, { winners, runnersUp }),
  })), [winners, runnersUp]);

  return (
    <div>
      <h1 className="page-title">Standings</h1>
      <p className="page-subtitle">
        {tab === 'bracket'
          ? 'Projected — based on current group standings.'
          : 'Group stage results — top 2 from each group advance.'}
      </p>
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'group' && (
        <>
          <Legend />
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
