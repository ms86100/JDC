import React, { useState } from 'react';
import { IssueResponse } from '../../../api/issueApi';

interface SearchExportProps {
  issues: IssueResponse[];
  onClose: () => void;
}

type ExportFormat = 'csv' | 'excel' | 'json';
type ExportScope = 'all' | 'selected' | 'current';

export default function SearchExport({ issues, onClose }: SearchExportProps) {
  const [format, setFormat] = useState<ExportFormat>('csv');
  const [scope, setScope] = useState<ExportScope>('all');
  const [includeColumns, setIncludeColumns] = useState({
    key: true,
    summary: true,
    status: true,
    priority: true,
    assignee: true,
    reporter: false,
    created: false,
    updated: false,
    labels: false,
    description: false,
  });

  const handleExport = () => {
    // Filter issues based on scope
    let issuesToExport = issues;

    // Get selected columns
    const selectedColumns = Object.entries(includeColumns)
      .filter(([_, selected]) => selected)
      .map(([col]) => col);

    // Generate export data
    const exportData = issuesToExport.map(issue => {
      const row: Record<string, string> = {};
      selectedColumns.forEach(col => {
        switch (col) {
          case 'key':
            row['Key'] = issue.issueKey;
            break;
          case 'summary':
            row['Summary'] = issue.title;
            break;
          case 'status':
            row['Status'] = issue.status || '';
            break;
          case 'priority':
            row['Priority'] = issue.priority || '';
            break;
          case 'assignee':
            row['Assignee'] = issue.assigneeId ? String(issue.assigneeId) : '';
            break;
          case 'reporter':
            row['Reporter'] = issue.reporterId ? String(issue.reporterId) : '';
            break;
          case 'created':
            row['Created'] = issue.createdAt ? new Date(issue.createdAt).toISOString() : '';
            break;
          case 'updated':
            row['Updated'] = issue.updatedAt ? new Date(issue.updatedAt).toISOString() : '';
            break;
          case 'labels':
            row['Labels'] = (issue as any).labels?.join(', ') || '';
            break;
          case 'description':
            row['Description'] = issue.description || '';
            break;
        }
      });
      return row;
    });

    // Generate file based on format
    let content: string;
    let mimeType: string;
    let extension: string;

    switch (format) {
      case 'csv':
        content = generateCsv(exportData, selectedColumns);
        mimeType = 'text/csv';
        extension = 'csv';
        break;
      case 'excel':
        content = generateCsv(exportData, selectedColumns); // Simplified - would use xlsx library
        mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
        extension = 'xlsx';
        break;
      case 'json':
        content = JSON.stringify(exportData, null, 2);
        mimeType = 'application/json';
        extension = 'json';
        break;
      default:
        return;
    }

    // Download file
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `issues-export.${extension}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    onClose();
  };

  const generateCsv = (data: Record<string, string>[], columns: string[]): string => {
    const headers = columns.map(col => {
      switch (col) {
        case 'key': return 'Key';
        case 'summary': return 'Summary';
        case 'status': return 'Status';
        case 'priority': return 'Priority';
        case 'assignee': return 'Assignee';
        case 'reporter': return 'Reporter';
        case 'created': return 'Created';
        case 'updated': return 'Updated';
        case 'labels': return 'Labels';
        case 'description': return 'Description';
        default: return col;
      }
    });

    const rows = data.map(row =>
      headers.map(h => {
        const value = row[h] || '';
        // Escape quotes and wrap in quotes if contains comma or newline
        if (value.includes(',') || value.includes('\n') || value.includes('"')) {
          return `"${value.replace(/"/g, '""')}"`;
        }
        return value;
      }).join(',')
    );

    return [headers.join(','), ...rows].join('\n');
  };

  const toggleColumn = (column: string) => {
    setIncludeColumns(prev => ({
      ...prev,
      [column]: !prev[column as keyof typeof prev],
    }));
  };

  return (
    <div className="ab-export-overlay" onClick={onClose}>
      <div className="ab-export-modal" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ab-modal-header">
          <h2>Export Issues</h2>
          <button className="ab-close-btn" onClick={onClose}>
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="ab-modal-body">
          {/* Summary */}
          <div className="ab-export-summary">
            <span className="ab-export-count">{issues.length}</span>
            <span>issues will be exported</span>
          </div>

          {/* Format selection */}
          <div className="ab-form-group">
            <label className="ab-label">Export Format</label>
            <div className="ab-format-options">
              <label className={`ab-format-option ${format === 'csv' ? 'selected' : ''}`}>
                <input
                  type="radio"
                  name="format"
                  value="csv"
                  checked={format === 'csv'}
                  onChange={() => setFormat('csv')}
                />
                <span className="ab-format-icon">📊</span>
                <span className="ab-format-name">CSV</span>
                <span className="ab-format-desc">Comma-separated values</span>
              </label>
              <label className={`ab-format-option ${format === 'excel' ? 'selected' : ''}`}>
                <input
                  type="radio"
                  name="format"
                  value="excel"
                  checked={format === 'excel'}
                  onChange={() => setFormat('excel')}
                />
                <span className="ab-format-icon">📗</span>
                <span className="ab-format-name">Excel</span>
                <span className="ab-format-desc">Spreadsheet format</span>
              </label>
              <label className={`ab-format-option ${format === 'json' ? 'selected' : ''}`}>
                <input
                  type="radio"
                  name="format"
                  value="json"
                  checked={format === 'json'}
                  onChange={() => setFormat('json')}
                />
                <span className="ab-format-icon">{ }</span>
                <span className="ab-format-name">JSON</span>
                <span className="ab-format-desc">Data interchange</span>
              </label>
            </div>
          </div>

          {/* Column selection */}
          <div className="ab-form-group">
            <label className="ab-label">Columns to Include</label>
            <div className="ab-columns-grid">
              {Object.entries(includeColumns).map(([col, selected]) => (
                <label key={col} className="ab-column-checkbox">
                  <input
                    type="checkbox"
                    checked={selected}
                    onChange={() => toggleColumn(col)}
                  />
                  <span>{col.charAt(0).toUpperCase() + col.slice(1)}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="ab-modal-footer">
          <button className="ab-btn ab-btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="ab-btn ab-btn-primary" onClick={handleExport}>
            Export {issues.length} Issues
          </button>
        </div>
      </div>

      <style>{`
        .ab-export-overlay {
          position: fixed;
          inset: 0;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }

        .ab-export-modal {
          background: var(--ab-white);
          border-radius: var(--ab-radius-lg);
          width: 480px;
          max-width: 90%;
          max-height: 90%;
          overflow-y: auto;
        }

        .ab-modal-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: var(--ab-spacing-lg);
          border-bottom: 1px solid var(--ab-gray-200);
        }

        .ab-modal-header h2 {
          margin: 0;
          font-size: var(--ab-font-size-lg);
          font-weight: 600;
        }

        .ab-close-btn {
          background: none;
          border: none;
          cursor: pointer;
          color: var(--ab-gray-500);
        }

        .ab-modal-body {
          padding: var(--ab-spacing-lg);
        }

        .ab-export-summary {
          display: flex;
          align-items: baseline;
          gap: var(--ab-spacing-xs);
          margin-bottom: var(--ab-spacing-lg);
          padding: var(--ab-spacing-md);
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-md);
        }

        .ab-export-count {
          font-size: var(--ab-font-size-xl);
          font-weight: 700;
          color: var(--ab-primary-600);
        }

        .ab-form-group {
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-label {
          display: block;
          font-weight: 500;
          margin-bottom: var(--ab-spacing-sm);
        }

        .ab-format-options {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--ab-spacing-sm);
        }

        .ab-format-option {
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: var(--ab-spacing-md);
          border: 2px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          cursor: pointer;
          transition: all var(--ab-transition-fast);
        }

        .ab-format-option:hover {
          border-color: var(--ab-primary-300);
        }

        .ab-format-option.selected {
          border-color: var(--ab-primary-500);
          background: var(--ab-primary-50);
        }

        .ab-format-option input {
          display: none;
        }

        .ab-format-icon {
          font-size: 24px;
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-format-name {
          font-weight: 600;
          color: var(--ab-gray-800);
        }

        .ab-format-desc {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
          text-align: center;
        }

        .ab-columns-grid {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: var(--ab-spacing-xs);
        }

        .ab-column-checkbox {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-xs);
          font-size: var(--ab-font-size-sm);
          cursor: pointer;
        }

        .ab-modal-footer {
          display: flex;
          justify-content: flex-end;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-lg);
          border-top: 1px solid var(--ab-gray-200);
        }
      `}</style>
    </div>
  );
}