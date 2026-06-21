import StandingsRow from './StandingsRow.jsx';

const headers = ['', '#', 'Team', 'P', 'W', 'D', 'L', 'GF', 'GA', 'GD', 'Pts'];

export default function GroupTable({ group }) {
  return (
    <div style={{ background: 'var(--surface)', borderRadius: 'var(--radius-card)', border: '1px solid var(--border)', overflow: 'hidden' }}>
      <div className="card-header section-title group-table-header" data-group={group.id}>
        Group {group.id}
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            {headers.map((h, i) => (
              <th key={i} style={{ padding: '4px 6px', fontSize: '0.7rem', fontWeight: 600, color: 'var(--text-muted)', textAlign: i <= 2 ? 'left' : 'center', textTransform: 'uppercase', letterSpacing: '0.05em', borderBottom: '1px solid var(--border)' }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {group.teams.map((row, i) => (
            <StandingsRow key={row.slug} row={row} rank={row.rank ?? i + 1} state={row.state} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
