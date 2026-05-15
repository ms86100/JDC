import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import type { ImportHistoryItem } from '../types/migration';
import { migrationApi } from '../../../api/serviceApi';
import { format } from 'date-fns';

interface JobHistoryTableProps {
  onViewDetails?: (jobId: string) => void;
  onRetryJob?: (jobId: string) => void;
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
};

const JOB_TYPE_LABELS: Record<string, string> = {
  CSV: 'CSV Import',
  JIRA_DC: 'Jira DC Import',
  PROJECT_IMPORT: 'Project Import',
  PROJECT_EXPORT: 'Project Export',
};

export default function JobHistoryTable({
  onViewDetails,
  onRetryJob,
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
    queryKey: ['migration-job-history'],
    queryFn: async () => {
      // For now, we'll use a placeholder since there's no dedicated history endpoint
      // In production, you'd call: migrationApi.getJobHistory({ page, size: limit * 5 })
      return {
        jobs: [] as ImportHistoryItem[],
        totalElements: 0,
        totalPages: 0,
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

  // Loading state
  if (isLoading) {
    return (
      <div className="bg-white rounded-lg border p-8 text-center">
        <div className="w-12 h-12 border-4 border-jira-blue border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-gray-500">Loading job history...</p>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
        <span className="text-red-500 text-2xl">⚠</span>
        <p className="text-red-800 font-medium mt-2">Failed to load job history</p>
        <p className="text-red-600 text-sm mt-1">Please try again later</p>
      </div>
    );
  }

  // Empty state
  if (jobs.length === 0) {
    return (
      <div className="bg-white rounded-lg border p-8 text-center">
        <span className="text-4xl">📋</span>
        <p className="text-gray-900 font-medium mt-4">No migration jobs yet</p>
        <p className="text-gray-500 text-sm mt-1">
          Start an import or export to see the history here
        </p>
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
            <option value="JIRA_DC">Jira DC Import</option>
            <option value="PROJECT_IMPORT">Project Import</option>
            <option value="PROJECT_EXPORT">Project Export</option>
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
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
              const badge = STATUS_BADGES[job.status] || STATUS_BADGES.PENDING;

              return (
                <tr key={job.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatDate(job.initiatedAt)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {JOB_TYPE_LABELS[job.jobType] || job.jobType}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${badge.bg} ${badge.text}`}>
                      <span className="mr-1">{badge.icon}</span>
                      {job.status}
                    </span>
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
                          onClick={() => onViewDetails(job.id)}
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
