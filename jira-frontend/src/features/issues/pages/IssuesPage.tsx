import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import CreateIssueModal from '../components/CreateIssueModal';
import BulkOperationsModal from '../components/BulkOperationsModal';
import './IssuesPage.css';

export default function IssuesPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [showCreate, setShowCreate] = useState(false);
  const [showBulkOps, setShowBulkOps] = useState(false);
  const [filter, setFilter] = useState<'all' | 'my'>('all');
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  const { data: issuesResponse, isLoading } = useQuery<{ content: IssueResponse[]; totalElements: number }>({
    queryKey: ['issues', filter],
    queryFn: async () => {
      const response = await issueApi.getAll();
      return response.data;
    },
  });

  const issues = issuesResponse?.content ?? [];

  const handleIssueCreated = () => {
    queryClient.invalidateQueries({ queryKey: ['issues'] });
    setShowCreate(false);
  };

  const toggleSelect = (id: string) => {
    const newSet = new Set(selectedIds);
    if (newSet.has(id)) {
      newSet.delete(id);
    } else {
      newSet.add(id);
    }
    setSelectedIds(newSet);
  };

  const toggleSelectAll = () => {
    if (!issues) return;
    if (selectedIds.size === issues.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(issues.map(i => i.id)));
    }
  };

  const selectedIssues = issues?.filter(i => selectedIds.has(i.id)) || [];

  return (
    <div className="ab-issues-page">
      <div className="ab-page-header">
        <div>
          <h1 className="ab-page-title">Issues</h1>
          <p className="ab-page-subtitle">Track and manage all your issues</p>
        </div>
        <div className="ab-header-actions">
          {selectedIds.size > 0 && (
            <button
              className="ab-btn ab-btn-secondary"
              onClick={() => setShowBulkOps(true)}
            >
              Bulk Operations ({selectedIds.size})
            </button>
          )}
          <button
            className="ab-btn ab-btn-secondary"
            onClick={() => navigate('/issues/batch')}
          >
            Batch lookup
          </button>
          <button className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
            <span className="ab-icon-plus"></span>
            Create Issue
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="ab-filters">
        <div className="ab-tabs">
          <button
            className={`ab-tab ${filter === 'all' ? 'active' : ''}`}
            onClick={() => setFilter('all')}
          >
            All Issues
          </button>
          <button
            className={`ab-tab ${filter === 'my' ? 'active' : ''}`}
            onClick={() => setFilter('my')}
          >
            My Issues
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
        </div>
      ) : issues && issues.length > 0 ? (
        <div className="ab-card">
          <table className="ab-table">
            <thead>
              <tr>
                <th style={{ width: '40px' }}>
                  <input
                    type="checkbox"
                    checked={selectedIds.size === issues.length && issues.length > 0}
                    onChange={toggleSelectAll}
                  />
                </th>
                <th>Type</th>
                <th>Key</th>
                <th>Summary</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Assignee</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {issues.map((issue) => (
                <tr key={issue.id} className={selectedIds.has(issue.id) ? 'selected' : ''}>
                  <td onClick={(e) => e.stopPropagation()}>
                    <input
                      type="checkbox"
                      checked={selectedIds.has(issue.id)}
                      onChange={() => toggleSelect(issue.id)}
                    />
                  </td>
                  <td onClick={() => navigate(`/issues/${issue.id}`)} style={{ cursor: 'pointer' }}>
                    <span className="ab-issue-type" title={issue.issueType}>
                      {issue.issueType === 'Bug' ? '🐛' : issue.issueType === 'Story' ? '📖' : issue.issueType === 'Task' ? '✓' : '⚡'}
                    </span>
                  </td>
                  <td onClick={() => navigate(`/issues/${issue.id}`)} style={{ cursor: 'pointer' }}>
                    <span className="ab-issue-key-link">{issue.issueKey}</span>
                  </td>
                  <td onClick={() => navigate(`/issues/${issue.id}`)} style={{ cursor: 'pointer' }} className="ab-issue-summary">{issue.title}</td>
                  <td onClick={() => navigate(`/issues/${issue.id}`)} style={{ cursor: 'pointer' }}>
                    <span className={`ab-badge ${issue.status?.includes('Done') ? 'ab-badge-success' : 'ab-badge-primary'}`}>
                      {issue.status}
                    </span>
                  </td>
                  <td onClick={() => navigate(`/issues/${issue.id}`)} style={{ cursor: 'pointer' }} className={issue.priority ? `priority-${issue.priority.toLowerCase().replace(/\s+/g, '-')}` : ''}>
                    {issue.priority || '-'}
                  </td>
                  <td onClick={() => navigate(`/issues/${issue.id}`)} style={{ cursor: 'pointer' }}>{issue.assigneeId ? 'Assigned' : 'Unassigned'}</td>
                  <td onClick={() => navigate(`/issues/${issue.id}`)} style={{ cursor: 'pointer' }} className="ab-text-muted">
                    {issue.updatedAt ? new Date(issue.updatedAt).toLocaleDateString() : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="ab-card">
          <div className="ab-empty-state">
            <div className="ab-empty-state-icon">📋</div>
            <h3 className="ab-empty-state-title">No issues found</h3>
            <p className="ab-empty-state-description">
              Create your first issue to start tracking work.
            </p>
            <button className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
              Create Issue
            </button>
          </div>
        </div>
      )}

      {/* Create Issue Modal */}
      {showCreate && (
        <CreateIssueModal
          onClose={() => setShowCreate(false)}
          onSuccess={handleIssueCreated}
        />
      )}

      {/* Bulk Operations Modal */}
      {showBulkOps && selectedIssues.length > 0 && (
        <BulkOperationsModal
          issues={selectedIssues}
          onClose={() => {
            setShowBulkOps(false);
            setSelectedIds(new Set());
            queryClient.invalidateQueries({ queryKey: ['issues'] });
          }}
        />
      )}

    </div>
  );
}
