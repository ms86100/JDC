import React, { useState, useEffect } from 'react';
import combinedApi, { TraceabilityMatrixResponse } from '../../../api/testApi';

interface TraceabilityMatrixProps {
  projectId: string;
}

export const TraceabilityMatrix: React.FC<TraceabilityMatrixProps> = ({ projectId }) => {
  const [matrix, setMatrix] = useState<TraceabilityMatrixResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const [filter, setFilter] = useState<'ALL' | 'COVERED' | 'PARTIALLY_COVERED' | 'NOT_COVERED'>('ALL');

  useEffect(() => {
    loadMatrix();
  }, [projectId]);

  const loadMatrix = async () => {
    setLoading(true);
    try {
      const response = await combinedApi.getTraceabilityMatrix(projectId);
      setMatrix(response);
    } catch (error) {
      console.error('Failed to load traceability matrix:', error);
    } finally {
      setLoading(false);
    }
  };

  const toggleRow = (requirementKey: string) => {
    const newExpanded = new Set(expandedRows);
    if (newExpanded.has(requirementKey)) {
      newExpanded.delete(requirementKey);
    } else {
      newExpanded.add(requirementKey);
    }
    setExpandedRows(newExpanded);
  };

  const getCoverageIcon = (coverageStatus: string) => {
    switch (coverageStatus) {
      case 'COVERED':
        return '✅';
      case 'PARTIALLY_COVERED':
        return '⚠️';
      case 'NOT_COVERED':
        return '❌';
      default:
        return '❓';
    }
  };

  const getCoverageColor = (coverageStatus: string) => {
    switch (coverageStatus) {
      case 'COVERED':
        return 'bg-green-100 text-green-800';
      case 'PARTIALLY_COVERED':
        return 'bg-yellow-100 text-yellow-800';
      case 'NOT_COVERED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const filteredMatrix = matrix.filter((row) => {
    if (filter === 'ALL') return true;
    return row.coverageStatus === filter;
  });

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="traceability-matrix">
      <div className="matrix-header mb-4 flex items-center justify-between">
        <h3 className="text-lg font-semibold">Requirements Traceability Matrix</h3>
        <div className="filter-controls flex gap-2">
          <button
            onClick={() => setFilter('ALL')}
            className={`btn btn-sm ${filter === 'ALL' ? 'btn-primary' : 'btn-secondary'}`}
          >
            All ({matrix.length})
          </button>
          <button
            onClick={() => setFilter('COVERED')}
            className={`btn btn-sm ${filter === 'COVERED' ? 'btn-primary' : 'btn-secondary'}`}
          >
            ✅ Covered ({matrix.filter(m => m.coverageStatus === 'COVERED').length})
          </button>
          <button
            onClick={() => setFilter('PARTIALLY_COVERED')}
            className={`btn btn-sm ${filter === 'PARTIALLY_COVERED' ? 'btn-primary' : 'btn-secondary'}`}
          >
            ⚠️ Partial ({matrix.filter(m => m.coverageStatus === 'PARTIALLY_COVERED').length})
          </button>
          <button
            onClick={() => setFilter('NOT_COVERED')}
            className={`btn btn-sm ${filter === 'NOT_COVERED' ? 'btn-primary' : 'btn-secondary'}`}
          >
            ❌ Not Covered ({matrix.filter(m => m.coverageStatus === 'NOT_COVERED').length})
          </button>
        </div>
      </div>

      {filteredMatrix.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          <p className="text-lg">No requirements found</p>
          <p className="text-sm mt-2">
            Link tests to requirements to see the traceability matrix
          </p>
        </div>
      ) : (
        <div className="matrix-table">
          <table className="min-w-full divide-y divide-gray-200 border">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase w-1/4">
                  Requirement
                </th>
                <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">
                  Test Count
                </th>
                <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">
                  Coverage
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  Tests
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredMatrix.map((row) => (
                <React.Fragment key={row.requirementKey}>
                  <tr
                    className="hover:bg-gray-50 cursor-pointer"
                    onClick={() => toggleRow(row.requirementKey)}
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center">
                        <span className="mr-2">
                          {expandedRows.has(row.requirementKey) ? '▼' : '▶'}
                        </span>
                        <span className="font-mono font-medium">{row.requirementKey}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span className="font-semibold">{row.testCount}</span>
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span
                        className={`px-2 py-1 rounded-full text-xs font-medium ${getCoverageColor(
                          row.coverageStatus
                        )}`}
                      >
                        {getCoverageIcon(row.coverageStatus)} {row.coverageStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {row.tests.slice(0, 3).map((t) => t.issueKey).join(', ')}
                      {row.tests.length > 3 && ` +${row.tests.length - 3} more`}
                    </td>
                  </tr>
                  {expandedRows.has(row.requirementKey) && (
                    <tr className="bg-gray-50">
                      <td colSpan={4} className="px-4 py-3">
                        <div className="tests-detail pl-6 space-y-2">
                          <h5 className="font-medium text-sm mb-2">Linked Tests:</h5>
                          {row.tests.map((test) => (
                            <div
                              key={test.testId}
                              className="flex items-center justify-between p-2 bg-white rounded border"
                            >
                              <div>
                                <span className="font-mono text-blue-600">{test.issueKey}</span>
                                <span className="ml-2 text-gray-600">{test.name}</span>
                              </div>
                              <div className="flex items-center gap-3">
                                {test.lastExecutionStatus && (
                                  <span
                                    className={`px-2 py-0.5 rounded text-xs ${
                                      test.lastExecutionStatus === 'PASSED'
                                        ? 'bg-green-100 text-green-800'
                                        : test.lastExecutionStatus === 'FAILED'
                                        ? 'bg-red-100 text-red-800'
                                        : 'bg-gray-100 text-gray-800'
                                    }`}
                                  >
                                    {test.lastExecutionStatus}
                                  </span>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default TraceabilityMatrix;