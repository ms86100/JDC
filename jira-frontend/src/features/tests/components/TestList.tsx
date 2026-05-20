import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import testApi, { TestResponse } from '../../../api/testApi';
import { TestStatusBadge, TestTypeBadge } from './TestComponents';

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
  const [tests, setTests] = useState<TestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedTests, setSelectedTests] = useState<Set<string>>(new Set());

  useEffect(() => {
    loadTests();
  }, [projectId, folderId, filter]);

  const loadTests = async () => {
    setLoading(true);
    try {
      const response = await testApi.searchTests({
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
      {selectedTests.size > 0 && (
        <div className="bulk-actions mb-4 p-3 bg-blue-50 rounded-lg flex items-center justify-between">
          <span>{selectedTests.size} test(s) selected</span>
          <div className="flex gap-2">
            <button className="btn btn-sm btn-secondary">Add to Test Set</button>
            <button className="btn btn-sm btn-secondary">Add to Test Plan</button>
            <button className="btn btn-sm btn-danger">Delete Selected</button>
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
                    {test.labels?.slice(0, 3).map((label, i) => (
                      <span key={i} className="px-2 py-0.5 bg-gray-100 rounded text-xs">
                        {label}
                      </span>
                    ))}
                    {test.labels?.length > 3 && (
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
                      to={`/tests/${test.id}/execute`}
                      className="text-green-600 hover:text-green-800 text-sm"
                    >
                      Execute
                    </Link>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default TestList;