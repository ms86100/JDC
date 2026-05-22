import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { ImportHistoryItem } from '../types/migration';
import { migrationApi } from '../../../api/serviceApi';
import { format } from 'date-fns';
import { Skeleton } from '../../../components/ui/Skeleton';
import { EmptyState } from '../../../components/ui/EmptyState';
import { StatusLozenge } from '../../../components/ui/StatusLozenge';
import { jobStatusLozenge } from '../utils/jobStatusLozenge';

interface JobHistoryTableProps {
  onViewDetails?: (jobId: string, jobType?: string) => void;
  onRetryJob?: (jobId: string) => void;
  onRollbackJob?: (jobId: string) => void;
  onDownloadReport?: (jobId: string) => void;
  limit?: number;
  showPagination?: boolean;
}

const STATUS_BADGES: Record<string, { bg: string; text: string; icon: string }> = {
  PENDING: { bg: 'bg-gray-100', text: 'text-gray-600', icon: '⏳' },
  VALIDATING: { bg: 'bg-blue-100', text: 'text-blue-600', icon: '🔍' },
  MAPPING: { bg: 'bg-purple-100', text: 'text-purple-600', icon: '🔗' },
  IMPORTING: { bg: 'bg-blue-100', text: 'text-blue-600', icon: '📥' },
  INDEXING: { bg: 'bg-indigo-100', text: 'text-indigo-600', icon: '🔎' },
  COMPLETED: { bg: 'bg-green-100', text: 'text-green-600', icon: '✅' },
  FAILED: { bg: 'bg-red-100', text: 'text-red-600', icon: '❌' },
  CANCELLED: { bg: 'bg-gray-100', text: 'text-gray-600', icon: '🚫' },
  ROLLED_BACK: { bg: 'bg-amber-100', text: 'text-amber-800', icon: '↩️' },
};

const JOB_TYPE_LABELS: Record<string, string> = {
  CSV: 'CSV Import',
  ISSUE_XML: 'Issue XML (Jira DC)',
  JIRA_DC: 'Systems and Avionics Import',
  WORKFLOW_XML: 'Workflow XML',
  PROJECT_IMPORT: 'Project Import',
  PROJECT_EXPORT: 'Project Export',
};

export default function JobHistoryTable({
  onViewDetails,
  onRetryJob,
  onRollbackJob,
  onDownloadReport,
  limit = 10,
  showPagination = true,
}: JobHistoryTableProps) {
  const [page, setPage] = useState(0);
  const [sortField, setSortField] = useState<'initiatedAt' | 'jobType' | 'status'>('initiatedAt');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [filterType, setFilterType] = useState<string>('ALL');

  // Fetch job history
  const { data, isLoading, error } = useQuery({
    queryKey: ['migration-job-history', page, filterStatus, filterType],
    queryFn: async () => {
      const response = await migrationApi.listJobs({
        page,
        size: limit * 5,
        status: filterStatus !== 'ALL' ? filterStatus : undefined,
        type: filterType !== 'ALL' ? filterType : undefined,
        sortBy: 'initiatedAt',
        sortDir: 'DESC',
      });
      const content = response.data.content || [];
      const jobs: ImportHistoryItem[] = content.map((job) => ({
        id: job.id,
        jobType: job.importSource || job.jobType,
        status: job.jobStatus,
        totalEntities: job.totalEntities ?? 0,
        successCount: Math.max(0, job.processedEntities ?? 0),
        failedCount: job.failedEntities ?? 0,
        initiatedAt: job.initiatedAt || new Date().toISOString(),
        completedAt: job.completedAt,
        initiatedBy: job.initiatedBy || '—',
      }));
      return {
        jobs,
        totalElements: response.data.totalElements ?? jobs.length,
        totalPages: response.data.totalPages ?? 1,
      };
    },
    staleTime: 30 * 1000,
  });

  const jobs = data?.jobs || [];
  const totalElements = data?.totalElements || 0;

  // Filter and sort jobs
  const processedJobs = useMemo(() => {
    let filtered = [...jobs];

    // Apply status filter
    if (filterStatus !== 'ALL') {
      filtered = filtered.filter((job) => job.status === filterStatus);
    }

    // Apply type filter
    if (filterType !== 'ALL') {
      filtered = filtered.filter((job) => job.jobType === filterType);
    }

    // Sort
    filtered.sort((a, b) => {
      let comparison = 0;
      switch (sortField) {
        case 'initiatedAt':
          comparison = new Date(a.initiatedAt).getTime() - new Date(b.initiatedAt).getTime();
          break;
        case 'jobType':
          comparison = a.jobType.localeCompare(b.jobType);
          break;
        case 'status':
          comparison = a.status.localeCompare(b.status);
          break;
      }
      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return filtered;
  }, [jobs, filterStatus, filterType, sortField, sortDirection]);

  // Pagination
  const paginatedJobs = useMemo(() => {
    if (!showPagination) return processedJobs.slice(0, limit);
    const start = page * limit;
    return processedJobs.slice(start, start + limit);
  }, [processedJobs, page, limit, showPagination]);

  const totalPages = Math.ceil(processedJobs.length / limit);

  const handleSort = (field: 'initiatedAt' | 'jobType' | 'status') => {
    if (sortField === field) {
      setSortDirection((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const SortIcon = ({ field }: { field: 'initiatedAt' | 'jobType' | 'status' }) => {
    if (sortField !== field) {
      return <span className="text-gray-400 ml-1">↕</span>;
    }
    return <span className="text-jira-blue ml-1">{sortDirection === 'asc' ? '↑' : '↓'}</span>;
  };

  const formatDate = (dateString: string): string => {
    try {
      return format(new Date(dateString), 'MMM d, yyyy HH:mm');
    } catch {
      return dateString;
    }
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-lg border overflow-hidden" data-testid="job-history-loading">
        <div className="px-4 py-3 border-b" style={{ background: 'var(--sa-n50)' }}>
          <Skeleton width={120} height={16} />
        </div>
        <div className="p-4 space-y-3">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="flex gap-4">
              <Skeleton width="25%" height={14} />
              <Skeleton width="20%" height={14} />
              <Skeleton width="15%" height={14} />
              <Skeleton width="30%" height={14} />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div
        className="rounded-lg border p-6"
        role="alert"
        style={{ borderColor: 'var(--sa-status-blocked-bg)', background: 'var(--sa-status-blocked-bg)' }}
      >
        <p style={{ margin: 0, fontWeight: 600, color: 'var(--sa-status-blocked-fg)' }}>Failed to load job history</p>
        <p style={{ margin: '8px 0 0', fontSize: 'var(--sa-fs-sm)', color: 'var(--sa-n700)' }}>
          Ensure jira-migration-service is running on port 8094.
        </p>
      </div>
    );
  }

  if (jobs.length === 0) {
    return (
      <div className="bg-white rounded-lg border" data-testid="job-history-empty">
        <EmptyState
          title="No migration jobs yet"
          description="Start an import from the Import wizard tab. Completed and failed jobs appear here with retry, rollback, and report download."
        />
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg border overflow-hidden">
      {/* Toolbar */}
      <div className="px-4 py-3 border-b bg-gray-50 flex flex-wrap items-center gap-4">
        <h3 className="text-sm font-medium text-gray-700">Job History</h3>

        <div className="flex-1" />

        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-500">Status:</label>
          <select
            value={filterStatus}
            onChange={(e) => {
              setFilterStatus(e.target.value);
              setPage(0);
            }}
            className="px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-jira-blue"
          >
            <option value="ALL">All Statuses</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-500">Type:</label>
          <select
            value={filterType}
            onChange={(e) => {
              setFilterType(e.target.value);
              setPage(0);
            }}
            className="px-2 py-1 text-sm border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-jira-blue"
          >
            <option value="ALL">All Types</option>
            <option value="CSV">CSV Import</option>
            <option value="ISSUE_XML">Issue XML (Jira DC)</option>
            <option value="JIRA_DC">Systems and Avionics Import</option>
            <option value="PROJECT_IMPORT">Project Import</option>
            <option value="WORKFLOW_XML">Workflow XML</option>
            <option value="PROJECT_EXPORT">Project Export</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50 sticky top-0 z-10">
            <tr>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                onClick={() => handleSort('initiatedAt')}
              >
                Date
                <SortIcon field="initiatedAt" />
              </th>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                onClick={() => handleSort('jobType')}
              >
                Type
                <SortIcon field="jobType" />
              </th>
              <th
                className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                onClick={() => handleSort('status')}
              >
                Status
                <SortIcon field="status" />
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Entities
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Success
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Failed
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Initiated By
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {paginatedJobs.map((job) => {
              const lozenge = jobStatusLozenge(job.status);

              return (
                <tr key={job.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatDate(job.initiatedAt)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {JOB_TYPE_LABELS[job.jobType] || job.jobType}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <StatusLozenge status={lozenge.status}>{lozenge.label}</StatusLozenge>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {job.totalEntities.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-green-600">
                    {job.successCount.toLocaleString()}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-red-600">
                    {job.failedCount > 0 ? job.failedCount.toLocaleString() : '-'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {job.initiatedBy}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm">
                    <div className="flex items-center justify-end gap-2">
                      {onViewDetails && (
                        <button
                          onClick={() => onViewDetails(job.id, job.jobType)}
                          className="text-jira-blue hover:text-blue-700"
                          title="View Details"
                        >
                          View
                        </button>
                      )}
                      {job.status === 'FAILED' && onRetryJob && (
                        <button
                          onClick={() => onRetryJob(job.id)}
                          className="text-purple-600 hover:text-purple-700"
                          title="Retry Job"
                        >
                          Retry
                        </button>
                      )}
                      {job.status === 'COMPLETED' && onRollbackJob && (
                        <button
                          onClick={() => onRollbackJob(job.id)}
                          className="text-amber-700 hover:text-amber-900"
                          title="Rollback import"
                        >
                          Rollback
                        </button>
                      )}
                      {job.status === 'COMPLETED' && onDownloadReport && (
                        <button
                          onClick={() => onDownloadReport(job.id)}
                          className="text-green-600 hover:text-green-700"
                          title="Download Report"
                        >
                          Report
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {showPagination && totalPages > 1 && (
        <div className="px-6 py-3 border-t bg-gray-50 flex items-center justify-between">
          <div className="text-sm text-gray-500">
            Showing {page * limit + 1} to {Math.min((page + 1) * limit, processedJobs.length)} of {processedJobs.length} jobs
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1 text-sm border border-gray-300 rounded hover:bg-white disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
              let pageNum = i;
              if (totalPages > 5 && page > 2) {
                pageNum = page - 2 + i;
              }
              if (pageNum >= totalPages) return null;

              return (
                <button
                  key={pageNum}
                  onClick={() => setPage(pageNum)}
                  className={`px-3 py-1 text-sm border rounded ${
                    page === pageNum
                      ? 'bg-jira-blue text-white border-jira-blue'
                      : 'border-gray-300 hover:bg-white'
                  }`}
                >
                  {pageNum + 1}
                </button>
              );
            })}
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1 text-sm border border-gray-300 rounded hover:bg-white disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
