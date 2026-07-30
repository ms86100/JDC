import { DiffEditor } from '@monaco-editor/react';

interface ScriptDiffViewerProps {
  original: string;
  modified: string;
  originalLabel?: string;
  modifiedLabel?: string;
  height?: string;
  onClose?: () => void;
}

export default function ScriptDiffViewer({
  original,
  modified,
  originalLabel = 'Previous Version',
  modifiedLabel = 'Current Version',
  height = '500px',
  onClose,
}: ScriptDiffViewerProps) {
  return (
    <div style={{ border: '1px solid #d1d5db', borderRadius: '8px', overflow: 'hidden' }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: '#f3f4f6',
          padding: '8px 16px',
          borderBottom: '1px solid #d1d5db',
        }}
      >
        <div style={{ display: 'flex', gap: '24px' }}>
          <span style={{ fontSize: '0.8rem', color: '#dc2626', fontWeight: 500 }}>{originalLabel}</span>
          <span style={{ fontSize: '0.8rem', color: '#16a34a', fontWeight: 500 }}>{modifiedLabel}</span>
        </div>
        {onClose && (
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: '1px solid #d1d5db',
              borderRadius: '4px',
              padding: '4px 12px',
              cursor: 'pointer',
              fontSize: '0.8rem',
            }}
          >
            Close Diff
          </button>
        )}
      </div>
      <DiffEditor
        height={height}
        language="javascript"
        theme="vs-dark"
        original={original}
        modified={modified}
        options={{
          readOnly: true,
          minimap: { enabled: false },
          renderSideBySide: true,
          scrollBeyondLastLine: false,
          fontSize: 13,
          wordWrap: 'on',
        }}
      />
    </div>
  );
}
