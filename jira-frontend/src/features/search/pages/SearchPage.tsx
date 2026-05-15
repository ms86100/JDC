import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { projectApi, ProjectResponse } from '../../../api/projectApi';

interface SearchFilters {
  text: string;
  projectId: string;
  status: string;
  priority: string;
  assignee: string;
  type: string;
}

const QUICK_FILTERS = [
  { id: 'my-issues', label: 'My Issues', icon: '👤' },
  { id: 'recent', label: 'Recently Updated', icon: '🕐' },
  { id: 'unassigned', label: 'Unassigned', icon: '❓' },
  { id: 'high-priority', label: 'High Priority', icon: '⚡' },
  { id: 'blocked', label: 'Blocked', icon: '⛔' },
];

const STATUS_OPTIONS = ['', 'To Do', 'In Progress', 'In Review', 'Done', 'Blocked'];
const PRIORITY_OPTIONS = ['', 'Highest', 'High', 'Medium', 'Low', 'Lowest'];
const TYPE_OPTIONS = ['', 'Bug', 'Story', 'Task', 'Epic'];

export default function SearchPage() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<SearchFilters>({
    text: '',
    projectId: '',
    status: '',
    priority: '',
    assignee: '',
    type: '',
  });
  const [showFilters, setShowFilters] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'board'>('list');

  const { data: projects = [] } = useQuery<ProjectResponse[]>({
    queryKey: ['projects'],
    queryFn: async () => {
      return await projectApi.getAll();
    },
  });

  const { data: issues = [], isLoading } = useQuery<IssueResponse[]>({
    queryKey: ['search-issues', filters],
    queryFn: async () => {
      const params: Record<string, string> = {};
      if (filters.text) params['search'] = filters.text;
      if (filters.projectId) params['projectId'] = filters.projectId;
      if (filters.status) params['status'] = filters.status;
      if (filters.priority) params['priority'] = filters.priority;
      if (filters.assignee) params['assigneeId'] = filters.assignee;
      if (filters.type) params['type'] = filters.type;

      const response = await issueApi.getAll(params);
      const data = response.data;
      if (data && 'content' in data) {
        return data.content || [];
      }
      return [];
    },
  });

  const handleQuickFilter = (filterId: string) => {
    switch (filterId) {
      case 'my-issues':
        setFilters({ ...filters, assignee: 'me' });
        break;
      case 'high-priority':
        setFilters({ ...filters, priority: 'High' });
        break;
      default:
        break;
    }
  };

  const clearFilters = () => {
    setFilters({
      text: '',
      projectId: '',
      status: '',
      priority: '',
      assignee: '',
      type: '',
    });
  };

  const hasActiveFilters = Object.values(filters).some(v => v !== '');

  const getStatusVariant = (status: string | undefined) => {
    switch (status?.toLowerCase()) {
      case 'done': return 'success';
      case 'in progress': return 'primary';
      case 'blocked': return 'danger';
      default: return 'secondary';
    }
  };

  const getPriorityIcon = (priority: string | undefined) => {
    switch (priority?.toLowerCase()) {
      case 'highest': case 'critical': return '🔴';
      case 'high': return '🟠';
      case 'medium': return '🟡';
      case 'low': case 'lowest': return '🟢';
      default: return '⚪';
    }
  };

  return (
    <div className="ab-search-page">
      <div className="ab-search-header">
        <div className="ab-search-title">
          <h1>Search Issues</h1>
          <p>Find issues across all projects</p>
        </div>
        <div className="ab-view-toggle">
          <button
            className={`ab-view-btn ${viewMode === 'list' ? 'active' : ''}`}
            onClick={() => setViewMode('list')}
          >
            📋 List
          </button>
          <button
            className={`ab-view-btn ${viewMode === 'board' ? 'active' : ''}`}
            onClick={() => setViewMode('board')}
          >
            📊 Board
          </button>
        </div>
      </div>

      <div className="ab-search-bar">
        <div className="ab-search-input-wrapper">
          <span className="ab-search-icon">🔍</span>
          <input
            type="text"
            className="ab-search-input"
            placeholder="Search by issue key, title, or description..."
            value={filters.text}
            onChange={(e) => setFilters({ ...filters, text: e.target.value })}
          />
          {filters.text && (
            <button
              className="ab-search-clear"
              onClick={() => setFilters({ ...filters, text: '' })}
            >
              ×
            </button>
          )}
        </div>
        <button
          className={`ab-btn ab-btn-secondary ${showFilters ? 'active' : ''}`}
          onClick={() => setShowFilters(!showFilters)}
        >
          🔽 Filters {hasActiveFilters && `(${Object.values(filters).filter(v => v).length})`}
        </button>
      </div>

      <div className="ab-quick-filters">
        {QUICK_FILTERS.map((filter) => (
          <button
            key={filter.id}
            className="ab-quick-filter"
            onClick={() => handleQuickFilter(filter.id)}
          >
            <span>{filter.icon}</span>
            {filter.label}
          </button>
        ))}
      </div>

      {showFilters && (
        <div className="ab-filters-panel">
          <div className="ab-filters-grid">
            <div className="ab-form-group">
              <label className="ab-label">Project</label>
              <select
                className="ab-select"
                value={filters.projectId}
                onChange={(e) => setFilters({ ...filters, projectId: e.target.value })}
              >
                <option value="">All Projects</option>
                {projects?.map((project) => (
                  <option key={project.id} value={project.id}>{project.name}</option>
                ))}
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Status</label>
              <select
                className="ab-select"
                value={filters.status}
                onChange={(e) => setFilters({ ...filters, status: e.target.value })}
              >
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>{status || 'Any Status'}</option>
                ))}
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Priority</label>
              <select
                className="ab-select"
                value={filters.priority}
                onChange={(e) => setFilters({ ...filters, priority: e.target.value })}
              >
                {PRIORITY_OPTIONS.map((priority) => (
                  <option key={priority} value={priority}>{priority || 'Any Priority'}</option>
                ))}
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Type</label>
              <select
                className="ab-select"
                value={filters.type}
                onChange={(e) => setFilters({ ...filters, type: e.target.value })}
              >
                {TYPE_OPTIONS.map((type) => (
                  <option key={type} value={type}>{type || 'Any Type'}</option>
                ))}
              </select>
            </div>
          </div>

          <div className="ab-filters-actions">
            <button className="ab-btn ab-btn-ghost" onClick={clearFilters}>
              Clear All
            </button>
          </div>
        </div>
      )}

      <div className="ab-search-results">
        <div className="ab-results-header">
          <span className="ab-results-count">
            {issues?.length || 0} issue{(issues?.length || 0) !== 1 ? 's' : ''} found
          </span>
        </div>

        {isLoading ? (
          <div className="ab-loading">
            <div className="ab-spinner"></div>
          </div>
        ) : issues && issues.length > 0 ? (
          <div className={`ab-results-${viewMode}`}>
            {viewMode === 'list' ? (
              <div className="ab-card">
                <table className="ab-table">
                  <thead>
                    <tr>
                      <th>Type</th>
                      <th>Key</th>
                      <th>Summary</th>
                      <th>Status</th>
                      <th>Priority</th>
                      <th>Assignee</th>
                    </tr>
                  </thead>
                  <tbody>
                    {issues.map((issue) => (
                      <tr
                        key={issue.id}
                        onClick={() => navigate(`/issues/${issue.id}`)}
                        style={{ cursor: 'pointer' }}
                      >
                        <td>
                          <span className="ab-issue-type">
                            {issue.issueType === 'Bug' ? '🐛' :
                             issue.issueType === 'Story' ? '📖' :
                             issue.issueType === 'Epic' ? '⚡' : '✓'}
                          </span>
                        </td>
                        <td className="ab-text-muted ab-font-mono">{issue.issueKey}</td>
                        <td className="ab-issue-summary">{issue.title}</td>
                        <td>
                          <span className={`ab-badge ab-badge-${getStatusVariant(issue.status)}`}>
                            {issue.status || 'To Do'}
                          </span>
                        </td>
                        <td>
                          <span title={issue.priority}>
                            {getPriorityIcon(issue.priority)}
                          </span>
                        </td>
                        <td>
                          {issue.assigneeId ? (
                            <span className="ab-avatar-sm">
                              {(issue.assigneeId as string).charAt(0).toUpperCase()}
                            </span>
                          ) : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="ab-results-board">
                {['To Do', 'In Progress', 'Done'].map((status) => (
                  <div key={status} className="ab-board-column">
                    <div className="ab-column-header">
                      <span>{status}</span>
                      <span className="ab-column-count">
                        {issues.filter(i => i.status === status).length}
                      </span>
                    </div>
                    <div className="ab-column-cards">
                      {issues
                        .filter(i => i.status === status)
                        .map((issue) => (
                          <div
                            key={issue.id}
                            className="ab-board-card"
                            onClick={() => navigate(`/issues/${issue.id}`)}
                          >
                            <div className="ab-card-header">
                              <span className="ab-card-key">{issue.issueKey}</span>
                              <span>{getPriorityIcon(issue.priority)}</span>
                            </div>
                            <div className="ab-card-title">{issue.title}</div>
                          </div>
                        ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="ab-card">
            <div className="ab-empty-state">
              <div className="ab-empty-state-icon">🔍</div>
              <h3 className="ab-empty-state-title">No issues found</h3>
              <p className="ab-empty-state-description">
                Try adjusting your search criteria or filters
              </p>
              {hasActiveFilters && (
                <button className="ab-btn ab-btn-primary" onClick={clearFilters}>
                  Clear Filters
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      <style>{`
        .ab-search-page { padding: var(--ab-spacing-lg); }
        .ab-search-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: var(--ab-spacing-lg);
        }
        .ab-search-title h1 {
          font-size: var(--ab-font-size-2xl);
          font-weight: 700;
          margin: 0 0 var(--ab-spacing-xs);
        }
        .ab-search-title p {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
          margin: 0;
        }
        .ab-view-toggle {
          display: flex;
          background: var(--ab-gray-100);
          border-radius: var(--ab-radius-md);
          padding: 2px;
        }
        .ab-view-btn {
          padding: var(--ab-spacing-xs) var(--ab-spacing-md);
          font-size: var(--ab-font-size-sm);
          background: transparent;
          border: none;
          border-radius: var(--ab-radius-sm);
          cursor: pointer;
          color: var(--ab-gray-600);
          transition: all var(--ab-transition-fast);
        }
        .ab-view-btn.active {
          background: var(--ab-white);
          color: var(--ab-gray-800);
          box-shadow: var(--ab-shadow-sm);
        }
        .ab-search-bar {
          display: flex;
          gap: var(--ab-spacing-sm);
          margin-bottom: var(--ab-spacing-md);
        }
        .ab-search-input-wrapper {
          flex: 1;
          position: relative;
          display: flex;
          align-items: center;
        }
        .ab-search-icon {
          position: absolute;
          left: var(--ab-spacing-md);
          font-size: var(--ab-font-size-lg);
        }
        .ab-search-input {
          width: 100%;
          padding: var(--ab-spacing-sm) var(--ab-spacing-md);
          padding-left: 2.5rem;
          font-size: var(--ab-font-size-base);
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-md);
          background: var(--ab-white);
        }
        .ab-search-input:focus {
          outline: none;
          border-color: var(--ab-primary-500);
          box-shadow: 0 0 0 3px rgba(0, 102, 255, 0.15);
        }
        .ab-search-clear {
          position: absolute;
          right: var(--ab-spacing-md);
          background: none;
          border: none;
          font-size: var(--ab-font-size-lg);
          color: var(--ab-gray-400);
          cursor: pointer;
        }
        .ab-quick-filters {
          display: flex;
          gap: var(--ab-spacing-sm);
          margin-bottom: var(--ab-spacing-md);
          flex-wrap: wrap;
        }
        .ab-quick-filter {
          display: inline-flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          padding: var(--ab-spacing-xs) var(--ab-spacing-md);
          font-size: var(--ab-font-size-sm);
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-full);
          cursor: pointer;
          transition: all var(--ab-transition-fast);
        }
        .ab-quick-filter:hover {
          border-color: var(--ab-primary-400);
          background: var(--ab-primary-50);
        }
        .ab-filters-panel {
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-lg);
          padding: var(--ab-spacing-lg);
          margin-bottom: var(--ab-spacing-lg);
        }
        .ab-filters-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: var(--ab-spacing-md);
        }
        @media (max-width: 1024px) {
          .ab-filters-grid { grid-template-columns: repeat(2, 1fr); }
        }
        .ab-filters-actions {
          display: flex;
          justify-content: flex-end;
          margin-top: var(--ab-spacing-md);
          padding-top: var(--ab-spacing-md);
          border-top: 1px solid var(--ab-gray-100);
        }
        .ab-results-header { margin-bottom: var(--ab-spacing-md); }
        .ab-results-count { font-size: var(--ab-font-size-sm); color: var(--ab-gray-500); }
        .ab-results-board {
          display: grid;
          grid-template-columns: repeat(3, 1fr);
          gap: var(--ab-spacing-md);
        }
        .ab-board-column {
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-lg);
          padding: var(--ab-spacing-sm);
        }
        .ab-board-column .ab-column-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: var(--ab-spacing-sm);
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
        }
        .ab-board-column .ab-column-count {
          background: var(--ab-gray-200);
          padding: 2px 8px;
          border-radius: var(--ab-radius-full);
          font-size: var(--ab-font-size-xs);
        }
        .ab-column-cards {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
        }
        .ab-board-card {
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-sm);
          cursor: pointer;
          transition: box-shadow var(--ab-transition-fast);
        }
        .ab-board-card:hover { box-shadow: var(--ab-shadow-md); }
        .ab-board-card .ab-card-header {
          display: flex;
          justify-content: space-between;
          margin-bottom: var(--ab-spacing-xs);
        }
        .ab-board-card .ab-card-key {
          font-family: var(--ab-font-mono);
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
        }
        .ab-board-card .ab-card-title {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-800);
          line-height: 1.4;
        }
      `}</style>
    </div>
  );
}