import React, { useState, useMemo } from 'react';
import type { ValidationError, ValidationResult } from '../types/migration';

interface ValidationResultsProps {
  result: ValidationResult;
  onExportErrors?: (errors: ValidationError[]) => void;
  onRowClick?: (row: number, column: string) => void;
}

type SortField = 'row' | 'column' | 'severity';
type SortDirection = 'asc' | 'desc';
type FilterSeverity = 'ALL' | 'ERROR' | 'WARNING';

export default function ValidationResults({
  result,
  onExportErrors,
  onRowClick,
}: ValidationResultsProps) {
  const [sortField, setSortField] = useState<SortField>('row');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');
  const [filterSeverity, setFilterSeverity] = useState<FilterSeverity>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const allIssues = useMemo(() => [...result.errors, ...result.warnings], [result.errors, result.warnings]);

  const filteredAndSortedIssues = useMemo(() => {
    let issues = [...allIssues];

    // Filter by severity
    if (filterSeverity !== 'ALL') {
      issues = issues.filter((issue) => issue.severity === filterSeverity);
    }

    // Filter by search query
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      issues = issues.filter(
        (issue) =>
          issue.message.toLowerCase().includes(query) ||
          issue.column.toLowerCase().includes(query) ||
          issue.code.toLowerCase().includes(query) ||
          issue.value.toLowerCase().includes(query)
      );
    }

    // Sort
    issues.sort((a, b) => {
      let comparison = 0;
      switch (sortField) {
        case 'row':
          comparison = a.row - b.row;
          break;
        case 'column':
          comparison = a.column.localeCompare(b.column);
          break;
        case 'severity':
          // ERROR comes before WARNING
          comparison = a.severity === 'ERROR' ? -1 : 1;
          break;
      }
      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return issues;
  }, [allIssues, filterSeverity, searchQuery, sortField, sortDirection]);

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortField !== field) {
      return <span className="text-gray-400 ml-1">↕</span>;
    }
    return <span className="text-jira-blue ml-1">{sortDirection === 'asc' ? '↑' : '↓'}</span>;
  };

  return (
    <div className="space-y-4">
      {/* Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg border p-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-500">Total Rows</span>
            <span className="text-lg font-semibold text-gray-900">{result.totalRows}</span>
          </div>
        </div>

        <div className="bg-white rounded-lg border p-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-500">Valid Rows</span>
            <span className="text-lg font-semibold text-green-600">{result.validRows}</span>
          </div>
        </div>

        <div className="bg-white rounded-lg border p-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-500">Errors</span>
            <span className="text-lg font-semibold text-red-600">{result.errors.length}</span>
          </div>
        </div>

        <div className="bg-white rounded-lg border p-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-500">Warnings</span>
            <span className="text-lg font-semibold text-yellow-600">{result.warnings.length}</span>
          </div>
        </div>
      </div>

      {/* Status Banner */}
      {result.errors.length === 0 && result.warnings.length === 0 && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 flex items-center gap-3">
          <span className="text-green-500 text-xl">✓</span>
          <div>
            <p className="text-green-800 font-medium">Validation Passed</p>
            <p className="text-green-600 text-sm">All {result.totalRows} rows passed validation</p>
          </div>
        </div>
      )}

      {result.errors.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-3">
          <span className="text-red-500 text-xl">✕</span>
          <div>
            <p className="text-red-800 font-medium">Validation Failed</p>
            <p className="text-red-600 text-sm">
              Found {result.errors.length} error(s) that must be fixed before import
            </p>
          </div>
        </div>
      )}

      {result.errors.length === 0 && result.warnings.length > 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-center gap-3">
          <span className="text-yellow-500 text-xl">⚠</span>
          <div>
            <p className="text-yellow-800 font-medium">Validation Passed with Warnings</p>
            <p className="text-yellow-600 text-sm">
              Found {result.warnings.length} warning(s). Import can proceed.
            </p>
          </div>
        </div>
      )}

      {/* Issues Table */}
      {(result.errors.length > 0 || result.warnings.length > 0) && (
        <div className="bg-white rounded-lg border overflow-hidden">
          {/* Toolbar */}
          <div className="px-4 py-3 border-b bg-gray-50 flex flex-wrap items-center gap-4">
            <div className="flex-1 min-w-[200px]">
              <input
                type="text"
                placeholder="Search errors..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-jira-blue"
              />
            </div>

            <div className="flex items-center gap-2">
              <label className="text-sm text-gray-500">Filter:</label>
              <select
                value={filterSeverity}
                onChange={(e) => setFilterSeverity(e.target.value as FilterSeverity)}
                className="px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-1 focus:ring-jira-blue"
              >
                <option value="ALL">All Issues</option>
                <option value="ERROR">Errors Only</option>
                <option value="WARNING">Warnings Only</option>
              </select>
            </div>

            <div className="text-sm text-gray-500">
              {filteredAndSortedIssues.length} of {allIssues.length} issues
            </div>

            {onExportErrors && (
              <button
                onClick={() => onExportErrors(allIssues)}
                className="px-3 py-2 text-sm bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
              >
                Export to CSV
              </button>
            )}
          </div>

          {/* Table */}
          <div className="overflow-x-auto max-h-96">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50 sticky top-0">
                <tr>
                  <th
                    className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                    onClick={() => handleSort('row')}
                  >
                    Row
                    <SortIcon field="row" />
                  </th>
                  <th
                    className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                    onClick={() => handleSort('column')}
                  >
                    Column
                    <SortIcon field="column" />
                  </th>
                  <th
                    className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                    onClick={() => handleSort('severity')}
                  >
                    Severity
                    <SortIcon field="severity" />
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Code
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Value
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Message
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {filteredAndSortedIssues.map((issue, index) => (
                  <tr
                    key={index}
                    className={`
                      hover:bg-gray-50 cursor-pointer transition-colors
                      ${issue.severity === 'ERROR' ? 'bg-red-50' : 'bg-yellow-50'}
                    `}
                    onClick={() => onRowClick?.(issue.row, issue.column)}
                  >
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">{issue.row}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{issue.column}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`
                          inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium
                          ${issue.severity === 'ERROR'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-yellow-100 text-yellow-800'
                          }
                        `}
                      >
                        {issue.severity}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm font-mono text-gray-500">{issue.code}</td>
                    <td
                      className="px-4 py-3 text-sm text-gray-600 max-w-[200px] truncate"
                      title={issue.value}
                    >
                      {issue.value || <span className="text-gray-300 italic">empty</span>}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{issue.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {filteredAndSortedIssues.length === 0 && searchQuery && (
            <div className="px-4 py-8 text-center text-gray-500">
              No issues match your search criteria
            </div>
          )}
        </div>
      )}
    </div>
  );
}
