import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { auditApi, AuditLogResponse, AuditSearchParams } from '../../../api/serviceApi';
import { format } from 'date-fns';

const AuditLogsPage: React.FC = () => {
  const [filters, setFilters] = useState<AuditSearchParams>({
    page: 0,
    size: 50,
  });
  const [selectedLog, setSelectedLog] = useState<AuditLogResponse | null>(null);

  const { data, isLoading, refetch } = useQuery<{ content: AuditLogResponse[]; totalElements: number }>({
    queryKey: ['audit-logs', filters],
    queryFn: async () => {
      const response = await auditApi.getLogs(filters);
      return response.data;
    },
  });

  const actionColors: Record<string, string> = {
    CREATE: 'bg-green-100 text-green-800',
    UPDATE: 'bg-blue-100 text-blue-800',
    DELETE: 'bg-red-100 text-red-800',
    LOGIN: 'bg-purple-100 text-purple-800',
    LOGOUT: 'bg-gray-100 text-gray-800',
    IMPORT: 'bg-yellow-100 text-yellow-800',
    EXPORT: 'bg-indigo-100 text-indigo-800',
  };

  const formatChanges = (changes?: Record<string, any>) => {
    if (!changes) return null;
    return Object.entries(changes).map(([key, value]) => (
      <div key={key} className="text-sm">
        <span className="font-medium text-gray-500">{key}:</span>{' '}
        <span className="text-gray-900">
          {typeof value === 'object' ? JSON.stringify(value) : String(value)}
        </span>
      </div>
    ));
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Audit Logs</h1>
          <p className="text-gray-500 mt-1">Track all system activities and changes</p>
        </div>
        <button
          onClick={() => refetch()}
          className="px-4 py-2 bg-jira-blue text-white rounded hover:bg-blue-600"
        >
          Refresh
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-4 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Entity Type</label>
            <select
              className="w-full border rounded px-3 py-2"
              value={filters.entityType || ''}
              onChange={(e) => setFilters({ ...filters, entityType: e.target.value || undefined })}
            >
              <option value="">All Entities</option>
              <option value="PROJECT">Project</option>
              <option value="ISSUE">Issue</option>
              <option value="WORKFLOW">Workflow</option>
              <option value="USER">User</option>
              <option value="COMMENT">Comment</option>
              <option value="ATTACHMENT">Attachment</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Action</label>
            <select
              className="w-full border rounded px-3 py-2"
              value={filters.action || ''}
              onChange={(e) => setFilters({ ...filters, action: e.target.value || undefined })}
            >
              <option value="">All Actions</option>
              <option value="CREATE">Create</option>
              <option value="UPDATE">Update</option>
              <option value="DELETE">Delete</option>
              <option value="IMPORT">Import</option>
              <option value="EXPORT">Export</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">From Date</label>
            <input
              type="date"
              className="w-full border rounded px-3 py-2"
              value={filters.startDate || ''}
              onChange={(e) => setFilters({ ...filters, startDate: e.target.value || undefined })}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">To Date</label>
            <input
              type="date"
              className="w-full border rounded px-3 py-2"
              value={filters.endDate || ''}
              onChange={(e) => setFilters({ ...filters, endDate: e.target.value || undefined })}
            />
          </div>
        </div>
      </div>

      {/* Logs Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Timestamp</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Action</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Entity Type</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Entity</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Service</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Details</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {isLoading ? (
                <>
                  {[...Array(8)].map((_, i) => (
                    <tr key={i}>
                      <td className="px-6 py-4"><div className="ab-skeleton" style={{ height: 16, width: '80%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td className="px-6 py-4"><div className="ab-skeleton" style={{ height: 16, width: '60%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td className="px-6 py-4"><div className="ab-skeleton" style={{ height: 22, width: 60, borderRadius: 12 }} /></td>
                      <td className="px-6 py-4"><div className="ab-skeleton" style={{ height: 16, width: '50%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td className="px-6 py-4"><div className="ab-skeleton" style={{ height: 16, width: 70, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td className="px-6 py-4"><div className="ab-skeleton" style={{ height: 16, width: '40%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td className="px-6 py-4"><div className="ab-skeleton" style={{ height: 16, width: 40, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    </tr>
                  ))}
                </>
              ) : data?.content?.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                    No audit logs found matching your criteria.
                  </td>
                </tr>
              ) : (
                data?.content?.map((log) => (
                  <tr
                    key={log.id}
                    className="hover:bg-gray-50 cursor-pointer"
                    onClick={() => setSelectedLog(log)}
                  >
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {format(new Date(log.createdAt), 'MMM d, yyyy HH:mm:ss')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {log.username || log.userId}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded-full ${
                          actionColors[log.action] || 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {log.action}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {log.entityType}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-mono text-gray-600">
                      {log.entityId.substring(0, 8)}...
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {log.serviceName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                      <button className="text-jira-blue hover:underline">View</button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="px-6 py-4 flex items-center justify-between border-t">
          <div className="text-sm text-gray-500">
            Showing {data?.content?.length || 0} of {data?.totalElements || 0} logs
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setFilters(f => ({ ...f, page: f.page ? f.page - 1 : 0 }))}
              disabled={!filters.page || filters.page === 0}
              className="px-4 py-2 bg-jira-blue text-white rounded disabled:opacity-50"
            >
              Previous
            </button>
            <button
              onClick={() => setFilters(f => ({ ...f, page: f.page + 1 }))}
              className="px-4 py-2 bg-jira-blue text-white rounded"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Detail Modal */}
      {selectedLog && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
            <div className="px-6 py-4 border-b flex justify-between items-center">
              <h3 className="text-lg font-semibold">Audit Log Details</h3>
              <button
                onClick={() => setSelectedLog(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-gray-500">Timestamp</label>
                  <p className="text-gray-900">
                    {format(new Date(selectedLog.createdAt), 'PPpp')}
                  </p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-500">User</label>
                  <p className="text-gray-900">{selectedLog.username || selectedLog.userId}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-500">Action</label>
                  <p className="text-gray-900">{selectedLog.action}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-500">Service</label>
                  <p className="text-gray-900">{selectedLog.serviceName}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-500">Entity Type</label>
                  <p className="text-gray-900">{selectedLog.entityType}</p>
                </div>
                <div>
                  <label className="text-sm font-medium text-gray-500">Entity ID</label>
                  <p className="text-gray-900 font-mono text-sm">{selectedLog.entityId}</p>
                </div>
              </div>
              {selectedLog.ipAddress && (
                <div>
                  <label className="text-sm font-medium text-gray-500">IP Address</label>
                  <p className="text-gray-900 font-mono">{selectedLog.ipAddress}</p>
                </div>
              )}
              {selectedLog.changes && (
                <div>
                  <label className="text-sm font-medium text-gray-500">Changes</label>
                  <div className="bg-gray-50 rounded p-3 mt-1">
                    {formatChanges(selectedLog.changes)}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AuditLogsPage;