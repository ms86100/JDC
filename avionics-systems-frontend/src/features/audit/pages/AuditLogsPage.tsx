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
    <div className="ab-page">
      <div className="ab-page-header">
        <div>
          <h1 className="ab-page-title">Audit Logs</h1>
          <p className="ab-text-muted">Track all system activities and changes</p>
        </div>
        <button
          onClick={() => refetch()}
          className="ab-btn ab-btn-primary"
        >
          Refresh
        </button>
      </div>

      {/* Filters */}
      <div className="ab-card" style={{ marginBottom: 'var(--ab-spacing-lg, 24px)' }}>
        <div className="ab-card-body" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 'var(--ab-spacing-md, 16px)' }}>
          <div className="ab-form-group">
            <label className="ab-label">Entity Type</label>
            <select
              className="ab-input"
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
          <div className="ab-form-group">
            <label className="ab-label">Action</label>
            <select
              className="ab-input"
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
          <div className="ab-form-group">
            <label className="ab-label">From Date</label>
            <input
              type="date"
              className="ab-input"
              value={filters.startDate || ''}
              onChange={(e) => setFilters({ ...filters, startDate: e.target.value || undefined })}
            />
          </div>
          <div className="ab-form-group">
            <label className="ab-label">To Date</label>
            <input
              type="date"
              className="ab-input"
              value={filters.endDate || ''}
              onChange={(e) => setFilters({ ...filters, endDate: e.target.value || undefined })}
            />
          </div>
        </div>
      </div>

      {/* Logs Table */}
      <div className="ab-card" style={{ overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table className="ab-table" style={{ minWidth: '100%' }}>
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>User</th>
                <th>Action</th>
                <th>Entity Type</th>
                <th>Entity</th>
                <th>Service</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
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
                    className="ab-table-row-hover"
                    style={{ cursor: 'pointer' }}
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
                      <button className="text-avisys-blue hover:underline">View</button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="ab-card-footer" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 24px', borderTop: '1px solid var(--ab-gray-200, #e5e7eb)' }}>
          <div className="ab-text-muted" style={{ fontSize: '0.85rem' }}>
            Showing {data?.content?.length || 0} of {data?.totalElements || 0} logs
          </div>
          <div style={{ display: 'flex', gap: 'var(--ab-spacing-sm, 8px)' }}>
            <button
              onClick={() => setFilters(f => ({ ...f, page: f.page ? f.page - 1 : 0 }))}
              disabled={!filters.page || filters.page === 0}
              className="ab-btn ab-btn-secondary ab-btn-sm"
            >
              Previous
            </button>
            <button
              onClick={() => setFilters(f => ({ ...f, page: f.page + 1 }))}
              className="ab-btn ab-btn-secondary ab-btn-sm"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      {/* Detail Modal */}
      {selectedLog && (
        <div className="ab-modal-overlay">
          <div className="ab-modal ab-card" style={{ maxWidth: 640, width: '100%', margin: '0 16px', maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="ab-card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3>Audit Log Details</h3>
              <button
                onClick={() => setSelectedLog(null)}
                className="ab-btn ab-btn-secondary ab-btn-sm"
              >
                ✕
              </button>
            </div>
            <div className="ab-card-body" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--ab-spacing-md, 16px)' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--ab-spacing-md, 16px)' }}>
                <div>
                  <label className="ab-label">Timestamp</label>
                  <p>{format(new Date(selectedLog.createdAt), 'PPpp')}</p>
                </div>
                <div>
                  <label className="ab-label">User</label>
                  <p>{selectedLog.username || selectedLog.userId}</p>
                </div>
                <div>
                  <label className="ab-label">Action</label>
                  <p>{selectedLog.action}</p>
                </div>
                <div>
                  <label className="ab-label">Service</label>
                  <p>{selectedLog.serviceName}</p>
                </div>
                <div>
                  <label className="ab-label">Entity Type</label>
                  <p>{selectedLog.entityType}</p>
                </div>
                <div>
                  <label className="ab-label">Entity ID</label>
                  <p style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{selectedLog.entityId}</p>
                </div>
              </div>
              {selectedLog.ipAddress && (
                <div>
                  <label className="ab-label">IP Address</label>
                  <p style={{ fontFamily: 'monospace' }}>{selectedLog.ipAddress}</p>
                </div>
              )}
              {selectedLog.changes && (
                <div>
                  <label className="ab-label">Changes</label>
                  <div className="ab-code-block" style={{ marginTop: 4, padding: 12, borderRadius: 'var(--ab-radius-md, 6px)' }}>
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