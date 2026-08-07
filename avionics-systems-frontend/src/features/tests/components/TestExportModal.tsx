import React, { useState } from 'react';
import { TestResponse } from '../../../api/testApi';
import { X, Download, FileSpreadsheet, FileJson } from 'lucide-react';

type ExportFormat = 'csv' | 'json';

interface ColumnOption {
  key: string;
  label: string;
  defaultChecked: boolean;
}

const COLUMN_OPTIONS: ColumnOption[] = [
  { key: 'name', label: 'Name', defaultChecked: true },
  { key: 'issueKey', label: 'Issue Key', defaultChecked: true },
  { key: 'testType', label: 'Test Type', defaultChecked: true },
  { key: 'status', label: 'Status', defaultChecked: true },
  { key: 'priority', label: 'Priority', defaultChecked: true },
  { key: 'labels', label: 'Labels', defaultChecked: false },
  { key: 'description', label: 'Description', defaultChecked: false },
  { key: 'precondition', label: 'Precondition', defaultChecked: false },
  { key: 'requirementKeys', label: 'Requirement Keys', defaultChecked: false },
];

interface TestExportModalProps {
  tests: TestResponse[];
  isOpen: boolean;
  onClose: () => void;
}

function escapeCsvField(value: string): string {
  if (value.includes(',') || value.includes('\n') || value.includes('"')) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}

function getFieldValue(test: TestResponse, column: string): string {
  switch (column) {
    case 'name':
      return test.name || '';
    case 'issueKey':
      return test.issueKey || '';
    case 'testType':
      return test.testType || '';
    case 'status':
      return test.testStatus || test.status || '';
    case 'priority':
      return test.testPriority || test.priority || '';
    case 'labels':
      return Array.isArray(test.labels) ? test.labels.join(', ') : '';
    case 'description':
      return test.description || '';
    case 'precondition':
      return (test as any).precondition || '';
    case 'requirementKeys':
      return Array.isArray(test.requirementKeys) ? test.requirementKeys.join(', ') : '';
    default:
      return '';
  }
}

function generateCsv(tests: TestResponse[], selectedColumns: string[]): string {
  const columnDefs = COLUMN_OPTIONS.filter(c => selectedColumns.includes(c.key));
  const headers = columnDefs.map(c => escapeCsvField(c.label));
  const rows = tests.map(test =>
    columnDefs.map(c => escapeCsvField(getFieldValue(test, c.key))).join(',')
  );
  return [headers.join(','), ...rows].join('\n');
}

function generateJson(tests: TestResponse[], selectedColumns: string[]): string {
  const data = tests.map(test => {
    const row: Record<string, string> = {};
    selectedColumns.forEach(col => {
      const colDef = COLUMN_OPTIONS.find(c => c.key === col);
      if (colDef) {
        row[colDef.label] = getFieldValue(test, col);
      }
    });
    return row;
  });
  return JSON.stringify(data, null, 2);
}

export const TestExportModal: React.FC<TestExportModalProps> = ({ tests, isOpen, onClose }) => {
  const [format, setFormat] = useState<ExportFormat>('csv');
  const [selectedColumns, setSelectedColumns] = useState<Set<string>>(
    new Set(COLUMN_OPTIONS.filter(c => c.defaultChecked).map(c => c.key))
  );

  if (!isOpen) return null;

  const toggleColumn = (key: string) => {
    setSelectedColumns(prev => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const handleExport = () => {
    const cols = Array.from(selectedColumns);
    if (cols.length === 0) return;

    let content: string;
    let mimeType: string;
    let extension: string;

    if (format === 'csv') {
      content = generateCsv(tests, cols);
      mimeType = 'text/csv';
      extension = 'csv';
    } else {
      content = generateJson(tests, cols);
      mimeType = 'application/json';
      extension = 'json';
    }

    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `tests-export.${extension}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full">
          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Export Tests</h2>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Body */}
          <div className="px-6 py-4 space-y-5">
            {/* Summary */}
            <div className="flex items-baseline gap-2 p-3 bg-gray-50 rounded-lg">
              <span className="text-xl font-bold text-blue-600">{tests.length}</span>
              <span className="text-sm text-gray-600">tests will be exported</span>
            </div>

            {/* Format Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Export Format</label>
              <div className="grid grid-cols-2 gap-3">
                <label
                  className={`flex flex-col items-center p-3 border-2 rounded-lg cursor-pointer transition-colors ${
                    format === 'csv' ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="format"
                    value="csv"
                    checked={format === 'csv'}
                    onChange={() => setFormat('csv')}
                    className="sr-only"
                  />
                  <FileSpreadsheet className="w-6 h-6 text-gray-600 mb-1" />
                  <span className="text-sm font-medium">CSV</span>
                  <span className="text-xs text-gray-500">Comma-separated</span>
                </label>
                <label
                  className={`flex flex-col items-center p-3 border-2 rounded-lg cursor-pointer transition-colors ${
                    format === 'json' ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300'
                  }`}
                >
                  <input
                    type="radio"
                    name="format"
                    value="json"
                    checked={format === 'json'}
                    onChange={() => setFormat('json')}
                    className="sr-only"
                  />
                  <FileJson className="w-6 h-6 text-gray-600 mb-1" />
                  <span className="text-sm font-medium">JSON</span>
                  <span className="text-xs text-gray-500">Data interchange</span>
                </label>
              </div>
            </div>

            {/* Column Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Columns to Include</label>
              <div className="grid grid-cols-2 gap-2">
                {COLUMN_OPTIONS.map(col => (
                  <label key={col.key} className="flex items-center gap-2 text-sm cursor-pointer py-1">
                    <input
                      type="checkbox"
                      checked={selectedColumns.has(col.key)}
                      onChange={() => toggleColumn(col.key)}
                      className="w-4 h-4 rounded border-gray-300"
                    />
                    <span className="text-gray-700">{col.label}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-200">
            <button onClick={onClose} className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 text-sm">
              Cancel
            </button>
            <button
              onClick={handleExport}
              disabled={selectedColumns.size === 0}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm disabled:opacity-50"
            >
              <Download className="w-4 h-4" />
              Export {tests.length} Tests
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TestExportModal;
