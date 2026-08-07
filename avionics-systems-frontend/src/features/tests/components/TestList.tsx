import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import combinedApi, { TestResponse } from '../../../api/testApi';
import { TestStatusBadge, TestTypeBadge } from './TestComponents';
import { Trash2, X, AlertTriangle, Loader2, Copy, Download, FolderInput, Tags, UserPlus } from 'lucide-react';

interface TestListProps {
  projectId: string;
  folderId?: string;
  filter?: {
    testType?: string;
    testStatus?: string;
    search?: string;
  };
}

export const TestList: React.FC<TestListProps> = ({ projectId, folderId, filter }) => {
  const queryClient = useQueryClient();
  const [tests, setTests] = useState<TestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedTests, setSelectedTests] = useState<Set<string>>(new Set());
  const [deleteConfirm, setDeleteConfirm] = useState<{ open: boolean; testId: string | null; testName?: string }>({ open: false, testId: null });

  useEffect(() => {
    loadTests();
  }, [projectId, folderId, filter]);

  const loadTests = async () => {
    setLoading(true);
    try {
      const response = await combinedApi.searchTests({
        projectId,
        folderId,
        testType: filter?.testType,
        testStatus: filter?.testStatus,
        search: filter?.search,
      });
      setTests(response);
    } catch (error) {
      console.error('Failed to load tests:', error);
    } finally {
      setLoading(false);
    }
  };

  const toggleSelectTest = (testId: string) => {
    const newSelected = new Set(selectedTests);
    if (newSelected.has(testId)) {
      newSelected.delete(testId);
    } else {
      newSelected.add(testId);
    }
    setSelectedTests(newSelected);
  };

  const toggleSelectAll = () => {
    if (selectedTests.size === tests.length) {
      setSelectedTests(new Set());
    } else {
      setSelectedTests(new Set(tests.map(t => t.id)));
    }
  };

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (testId: string) => combinedApi.deleteTest(testId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tests'] });
      loadTests();
      setDeleteConfirm({ open: false, testId: null });
    },
  });

  // Bulk delete mutation
  const bulkDeleteMutation = useMutation({
    mutationFn: (testIds: string[]) => Promise.all(testIds.map(id => combinedApi.deleteTest(id))),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tests'] });
      loadTests();
      setSelectedTests(new Set());
    },
  });

  // Clone mutation
  const cloneMutation = useMutation({
    mutationFn: (testId: string) => combinedApi.cloneTest(testId, projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tests'] });
      loadTests();
    },
  });

  // Bulk status update
  const bulkStatusMutation = useMutation({
    mutationFn: (status: string) => combinedApi.bulkUpdateStatus({ testIds: Array.from(selectedTests), status }, projectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tests'] });
      loadTests();
      setSelectedTests(new Set());
    },
  });

  // Export tests as CSV
  const handleExport = () => {
    const testsToExport = selectedTests.size > 0 ? tests.filter(t => selectedTests.has(t.id)) : tests;
    const headers = ['Key', 'Name', 'Type', 'Status', 'Priority', 'Labels', 'Description'];
    const rows = testsToExport.map(t => [
      t.issueKey || '',
      t.name,
      t.testType,
      t.testStatus,
      t.priority || '',
      (t.labels || []).join('; '),
      (t.description || '').replace(/"/g, '""'),
    ]);
    const csv = [headers, ...rows].map(r => r.map(f => `"${f}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `tests-export-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const handleDeleteClick = (testId: string, testName?: string) => {
    setDeleteConfirm({ open: true, testId, testName });
  };

  const handleDeleteConfirm = () => {
    if (deleteConfirm.testId) {
      deleteMutation.mutate(deleteConfirm.testId);
    }
  };

  const handleBulkDelete = () => {
    if (selectedTests.size > 0 && confirm(`Delete ${selectedTests.size} selected tests?`)) {
      bulkDeleteMutation.mutate(Array.from(selectedTests));
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (tests.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p className="text-lg">No tests found</p>
        <p className="text-sm mt-2">Create a new test to get started</p>
      </div>
    );
  }

  return (
    <div className="test-list">
      {/* Toolbar */}
      <div className="mb-4 flex items-center justify-between">
        <div className="text-sm text-gray-500">{tests.length} test(s)</div>
        <button onClick={handleExport} className="flex items-center gap-1 px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">
          <Download className="w-4 h-4" /> Export CSV
        </button>
      </div>

      {selectedTests.size > 0 && (
        <div className="bulk-actions mb-4 p-3 bg-blue-50 rounded-lg flex items-center justify-between">
          <span className="font-medium">{selectedTests.size} test(s) selected</span>
          <div className="flex gap-2 flex-wrap">
            <select
              onChange={(e) => { if (e.target.value) bulkStatusMutation.mutate(e.target.value); e.target.value = ''; }}
              className="text-sm border border-gray-300 rounded px-2 py-1"
              defaultValue=""
            >
              <option value="" disabled>Update Status...</option>
              <option value="DRAFT">Draft</option>
              <option value="READY">Ready</option>
              <option value="APPROVED">Approved</option>
              <option value="DEPRECATED">Deprecated</option>
            </select>
            <button className="px-3 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 flex items-center gap-1">
              <FolderInput className="w-3 h-3" /> Move to Folder
            </button>
            <button className="px-3 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 flex items-center gap-1">
              <Tags className="w-3 h-3" /> Add Labels
            </button>
            <button className="px-3 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 flex items-center gap-1">
              <UserPlus className="w-3 h-3" /> Assign
            </button>
            <button onClick={handleBulkDelete} disabled={bulkDeleteMutation.isPending} className="px-3 py-1 text-sm bg-red-50 text-red-700 border border-red-200 rounded hover:bg-red-100 flex items-center gap-1">
              {bulkDeleteMutation.isPending && <Loader2 className="w-3 h-3 animate-spin" />}
              <Trash2 className="w-3 h-3" /> Delete
            </button>
            <button onClick={() => setSelectedTests(new Set())} className="px-3 py-1 text-sm text-gray-500 hover:text-gray-700">
              <X className="w-3 h-3" />
            </button>
          </div>
        </div>
      )}

      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-4 py-3 text-left">
                <input
                  type="checkbox"
                  checked={selectedTests.size === tests.length && tests.length > 0}
                  onChange={toggleSelectAll}
                  className="rounded"
                  aria-label="Select all tests"
                />
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Key</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Summary</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Type</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Priority</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Labels</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {tests.map((test) => (
              <tr key={test.id} className="hover:bg-gray-50">
                <td className="px-4 py-3">
                  <input
                    type="checkbox"
                    checked={selectedTests.has(test.id)}
                    onChange={() => toggleSelectTest(test.id)}
                    className="rounded"
                    aria-label={`Select test ${test.issueKey}`}
                  />
                </td>
                <td className="px-4 py-3">
                  <Link
                    to={`/tests/${test.id}`}
                    className="text-blue-600 hover:text-blue-800 font-mono text-sm"
                  >
                    {test.issueKey}
                  </Link>
                </td>
                <td className="px-4 py-3">
                  <Link
                    to={`/tests/${test.id}`}
                    className="text-gray-900 hover:text-blue-600"
                  >
                    {test.name}
                  </Link>
                </td>
                <td className="px-4 py-3">
                  <TestTypeBadge type={test.testType} />
                </td>
                <td className="px-4 py-3">
                  <TestStatusBadge status={test.testStatus} />
                </td>
                <td className="px-4 py-3 text-sm text-gray-600">
                  {test.testPriority || test.priority || '-'}
                </td>
                <td className="px-4 py-3">
                  <div className="flex flex-wrap gap-1">
                    {(Array.isArray(test.labels) ? test.labels : []).slice(0, 3).map((label, i) => (
                      <span key={i} className="px-2 py-0.5 bg-gray-100 rounded text-xs">
                        {label}
                      </span>
                    ))}
                    {Array.isArray(test.labels) && test.labels.length > 3 && (
                      <span className="px-2 py-0.5 bg-gray-100 rounded text-xs">
                        +{test.labels.length - 3}
                      </span>
                    )}
                  </div>
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <Link
                      to={`/tests/${test.id}`}
                      className="text-blue-600 hover:text-blue-800 text-sm"
                    >
                      View
                    </Link>
                    <Link
                      to={`/tests/${test.id}/history`}
                      className="text-green-600 hover:text-green-800 text-sm"
                    >
                      Execute
                    </Link>
                    <button
                      onClick={() => cloneMutation.mutate(test.id)}
                      disabled={cloneMutation.isPending}
                      className="text-purple-600 hover:text-purple-800 text-sm flex items-center gap-1"
                      aria-label="Clone test"
                    >
                      <Copy className="w-3 h-3" /> Clone
                    </button>
                    <button
                      onClick={() => handleDeleteClick(test.id, test.name)}
                      className="text-red-600 hover:text-red-800 text-sm"
                      aria-label="Delete test"
                    >
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Delete Confirmation Modal */}
      {deleteConfirm.open && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4">
            <div className="fixed inset-0 bg-black bg-opacity-50" onClick={() => setDeleteConfirm({ open: false, testId: null })}></div>
            <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="p-3 bg-red-100 rounded-full">
                  <AlertTriangle className="w-6 h-6 text-red-600" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold">Delete Test</h3>
                  <p className="text-sm text-gray-500">This action cannot be undone</p>
                </div>
              </div>
              <p className="mb-6">
                Are you sure you want to delete test <strong>{deleteConfirm.testName || deleteConfirm.testId}</strong>?
                This will also remove it from any test sets or plans.
              </p>
              <div className="flex justify-end gap-3">
                <button
                  onClick={() => setDeleteConfirm({ open: false, testId: null })}
                  className="btn btn-secondary flex items-center gap-1"
                >
                  <X className="w-4 h-4" /> Cancel
                </button>
                <button
                  onClick={handleDeleteConfirm}
                  disabled={deleteMutation.isPending}
                  className="btn bg-red-600 hover:bg-red-700 text-white flex items-center gap-1"
                >
                  {deleteMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                  <Trash2 className="w-4 h-4" /> Delete
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TestList;