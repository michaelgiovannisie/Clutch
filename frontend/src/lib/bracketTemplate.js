// FIFA 2026 World Cup — confirmed R32 bracket skeleton (matches 73–88).
// Source: docs/features/bracket-reference.md
//
// Slot notation:
//   'W-{G}' = winner of Group G
//   'R-{G}' = runner-up of Group G
//   'T3:{A,B,...}' = best third-place finisher from one of the listed groups
//                    (resolved by the 495-scenario FIFA allocation table once available)

export const BRACKET_SLOTS = [
  { id: 73, home: 'R-A',           away: 'R-B'           },
  { id: 74, home: 'W-E',           away: 'T3:{A,B,C,D,F}'},
  { id: 75, home: 'W-F',           away: 'R-C'           },
  { id: 76, home: 'W-C',           away: 'R-F'           },
  { id: 77, home: 'W-I',           away: 'T3:{C,D,F,G,H}'},
  { id: 78, home: 'R-E',           away: 'R-I'           },
  { id: 79, home: 'W-A',           away: 'T3:{C,E,F,H,I}'},
  { id: 80, home: 'W-L',           away: 'T3:{E,H,I,J,K}'},
  { id: 81, home: 'W-D',           away: 'T3:{B,E,F,I,J}'},
  { id: 82, home: 'W-G',           away: 'T3:{A,E,H,I,J}'},
  { id: 83, home: 'R-K',           away: 'R-L'           },
  { id: 84, home: 'W-H',           away: 'R-J'           },
  { id: 85, home: 'W-B',           away: 'T3:{E,F,G,I,J}'},
  { id: 86, home: 'W-J',           away: 'R-H'           },
  { id: 87, home: 'W-K',           away: 'T3:{D,E,I,J,L}'},
  { id: 88, home: 'R-D',           away: 'R-G'           },
];

/**
 * Resolves a slot string to a team row, or a placeholder descriptor.
 * Returns null only when the team data is genuinely missing.
 */
export function resolveSlot(slot, { winners, runnersUp }) {
  if (slot.startsWith('W-')) {
    const g = slot.slice(2);
    return winners.find(t => t.group === g) ?? null;
  }
  if (slot.startsWith('R-')) {
    const g = slot.slice(2);
    return runnersUp.find(t => t.group === g) ?? null;
  }
  if (slot.startsWith('T3:')) {
    // Third-place allocation pending official FIFA 495-scenario table.
    const groups = slot.slice(3).replace(/[{}]/g, '');
    return { placeholder: true, label: `3rd of ${groups}` };
  }
  return null;
}
