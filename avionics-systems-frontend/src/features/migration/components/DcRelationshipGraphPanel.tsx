import React from 'react';
import MigrationPanel from './MigrationPanel';

interface Edge {
  from: string;
  to: string;
  type: string;
}

interface Props {
  edges: Edge[];
}

/** Issue/parent/epic/link edges from DC validate — visible counterpart to workflow graph UI. */
export default function DcRelationshipGraphPanel({ edges }: Props) {
  if (!edges.length) {
    return (
      <div className="rounded-lg border bg-gray-50 p-4 text-sm text-gray-500" data-testid="dc-relationship-graph">
        No relationship edges detected in this export.
      </div>
    );
  }

  const byType = edges.reduce<Record<string, number>>((acc, e) => {
    acc[e.type] = (acc[e.type] ?? 0) + 1;
    return acc;
  }, {});

  return (
    <MigrationPanel
      title={`Issue relationship graph (${edges.length} edges)`}
      data-testid="dc-relationship-graph"
    >
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 12 }}>
        {Object.entries(byType).map(([type, n]) => (
          <span
            key={type}
            style={{
              fontSize: 'var(--sa-fs-xs)',
              padding: '2px 8px',
              borderRadius: 'var(--sa-radius-sm)',
              background: 'var(--sa-brand-50, var(--sa-n100))',
              border: '1px solid var(--sa-n200)',
              color: 'var(--sa-n800)',
            }}
          >
            {type}: {n}
          </span>
        ))}
      </div>
      <ul
        style={{
          margin: 0,
          padding: 0,
          listStyle: 'none',
          fontSize: 'var(--sa-fs-xs)',
          fontFamily: 'monospace',
          color: 'var(--sa-n700)',
          maxHeight: 192,
          overflowY: 'auto',
        }}
      >
        {edges.map((e, i) => (
          <li key={`${e.from}-${e.to}-${i}`} style={{ padding: '2px 0' }}>
            {e.from} —[{e.type}]→ {e.to}
          </li>
        ))}
      </ul>
    </MigrationPanel>
  );
}
