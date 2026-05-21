import React from 'react';
import MigrationPanel from './MigrationPanel';

export type ExportFormat = 'xml' | 'json' | 'csv';

interface ProjectOption {
  id: string;
  name: string;
  projectKey: string;
}

interface ProjectExportPanelProps {
  projects: ProjectOption[];
  projectId: string;
  format: ExportFormat;
  onProjectChange: (id: string) => void;
  onFormatChange: (format: ExportFormat) => void;
}

/** Project export wizard — selects source project and archive format. */
export default function ProjectExportPanel({
  projects,
  projectId,
  format,
  onProjectChange,
  onFormatChange,
}: ProjectExportPanelProps) {
  return (
    <MigrationPanel
      title="Project export"
      subtitle="Creates a downloadable archive job (XML, JSON, or CSV)"
      data-testid="project-export-panel"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--sa-space-4)' }}>
        <label style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span style={{ fontSize: 'var(--sa-fs-sm)', fontWeight: 600, color: 'var(--sa-n800)' }}>
            Project to export
          </span>
          <select
            data-testid="project-export-project-select"
            value={projectId}
            onChange={(e) => onProjectChange(e.target.value)}
            style={{
              padding: 'var(--sa-space-2)',
              borderRadius: 'var(--sa-radius-sm)',
              border: '1px solid var(--sa-n200)',
              fontFamily: 'var(--sa-font-sans)',
            }}
          >
            <option value="">Select a project…</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.projectKey} — {p.name}
              </option>
            ))}
          </select>
        </label>

        <fieldset style={{ border: 'none', margin: 0, padding: 0 }}>
          <legend
            style={{
              fontSize: 'var(--sa-fs-sm)',
              fontWeight: 600,
              color: 'var(--sa-n800)',
              marginBottom: 8,
            }}
          >
            Export format
          </legend>
          <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
            {(['xml', 'json', 'csv'] as ExportFormat[]).map((f) => (
              <label
                key={f}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  fontSize: 'var(--sa-fs-sm)',
                  cursor: 'pointer',
                }}
              >
                <input
                  type="radio"
                  name="export-format"
                  data-testid={`project-export-format-${f}`}
                  checked={format === f}
                  onChange={() => onFormatChange(f)}
                />
                {f.toUpperCase()}
              </label>
            ))}
          </div>
        </fieldset>

        <p style={{ margin: 0, fontSize: 'var(--sa-fs-xs)', color: 'var(--sa-n600)' }}>
          Continue to review, then start export. Track progress in Job history and download the report when
          complete.
        </p>
      </div>
    </MigrationPanel>
  );
}
