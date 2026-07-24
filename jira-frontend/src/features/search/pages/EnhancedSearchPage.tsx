import React, { useState, useCallback, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import JQLAutocomplete from '../components/JQLAutocomplete';
import SavedFilters from '../components/SavedFilters';
import SearchExport from '../components/SearchExport';
import FilterSubscriptions from '../components/FilterSubscriptions';
import { chartColors } from '../../../utils/chartColors';

const QUICK_FILTERS = [
  { id: 'my-issues', label: 'My Issues', jql: 'assignee = currentUser()' },
  { id: 'recent', label: 'Recently Updated', jql: 'updated >= -1d' },
  { id: 'unassigned', label: 'Unassigned', jql: 'assignee is empty' },
  { id: 'high-priority', label: 'High Priority', jql: 'priority in (High, Highest)' },
  { id: 'has-due-date', label: 'Has Due Date', jql: 'duedate is not empty' },
  { id: 'blocked', label: 'Blocked', jql: 'status = Blocked' },
];

const VIEW_MODES = ['list', 'board', 'detail'] as const;
const SORT_OPTIONS = [
  { value: 'created', label: 'Created' },
  { value: 'updated', label: 'Updated' },
  { value: 'priority', label: 'Priority' },
  { value: 'summary', label: 'Summary' },
  { value: 'status', label: 'Status' },
];

const COLUMNS = [
  { id: 'type', label: 'Type', default: true },
  { id: 'key', label: 'Key', default: true },
  { id: 'summary', label: 'Summary', default: true },
  { id: 'status', label: 'Status', default: true },
  { id: 'priority', label: 'Priority', default: true },
  { id: 'assignee', label: 'Assignee', default: true },
  { id: 'reporter', label: 'Reporter', default: false },
  { id: 'created', label: 'Created', default: false },
  { id: 'updated', label: 'Updated', default: false },
  { id: 'duedate', label: 'Due Date', default: false },
  { id: 'labels', label: 'Labels', default: false },
  { id: 'sprint', label: 'Sprint', default: false },
];

export default function EnhancedSearchPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Search state
  const [jqlQuery, setJqlQuery] = useState('');
  const [isJqlMode, setIsJqlMode] = useState(false);
  const [viewMode, setViewMode] = useState<typeof VIEW_MODES[number]>('list');
  const [sortField, setSortField] = useState('updated');
  const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('DESC');
  const [selectedColumns, setSelectedColumns] = useState(COLUMNS.filter(c => c.default).map(c => c.id));
  const [isExportOpen, setIsExportOpen] = useState(false);

  // Filter panel state
  const [showFilterPanel, setShowFilterPanel] = useState(false);
  const [showSavedFilters, setShowSavedFilters] = useState(false);
  const [showSubscriptions, setShowSubscriptions] = useState(false);

  // Query validation
  const [queryError, setQueryError] = useState<string | null>(null);

  // Basic filters (when not in JQL mode)
  const [basicFilters, setBasicFilters] = useState({
    projectId: '',
    status: '',
    priority: '',
    assignee: '',
    type: '',
    text: '',
  });

  // Build JQL from basic filters
  const buildJqlFromBasicFilters = useCallback(() => {
    const clauses: string[] = [];

    if (basicFilters.projectId) {
      clauses.push(`project = ${basicFilters.projectId}`);
    }
    if (basicFilters.status) {
      clauses.push(`status = "${basicFilters.status}"`);
    }
    if (basicFilters.priority) {
      clauses.push(`priority = "${basicFilters.priority}"`);
    }
    if (basicFilters.assignee) {
      clauses.push(`assignee = "${basicFilters.assignee}"`);
    }
    if (basicFilters.type) {
      clauses.push(`type = "${basicFilters.type}"`);
    }
    if (basicFilters.text) {
      clauses.push(`text ~ "${basicFilters.text}"`);
    }

    let jql = clauses.join(' AND ');
    if (sortField) {
      jql += ` ORDER BY ${sortField} ${sortDirection}`;
    }
    return jql;
  }, [basicFilters, sortField, sortDirection]);

  // Effective JQL
  const effectiveJql = isJqlMode ? jqlQuery : buildJqlFromBasicFilters();

  // Query execution
  const { data: issues = [], isLoading, error, refetch } = useQuery<IssueResponse[]>({
    queryKey: ['search-issues', effectiveJql],
    queryFn: async () => {
      const response = await issueApi.getAll({ jql: effectiveJql });
      const data = response.data;
      if (data && 'content' in data) {
        return data.content || [];
      }
      return [];
    },
    enabled: effectiveJql.length > 0,
  });

  // Projects for filter dropdowns
  const { data: projects = [] } = useQuery<ProjectResponse[]>({
    queryKey: ['projects'],
    queryFn: async () => {
      return await projectApi.getAll();
    },
  });

  // Handle JQL change with validation
  const handleJqlChange = useCallback((value: string) => {
    setJqlQuery(value);
    // Validation would happen here
    if (value.includes('ERROR')) {
      setQueryError('Invalid JQL syntax');
    } else {
      setQueryError(null);
    }
  }, []);

  // Handle quick filter selection
  const handleQuickFilter = useCallback((jql: string) => {
    setJqlQuery(jql);
    setIsJqlMode(true);
  }, []);

  // Toggle column
  const toggleColumn = useCallback((columnId: string) => {
    setSelectedColumns(prev =>
      prev.includes(columnId)
        ? prev.filter(id => id !== columnId)
        : [...prev, columnId]
    );
  }, []);

  // Get status styling
  const getStatusVariant = (status: string | undefined) => {
    switch (status?.toLowerCase()) {
      case 'done': case 'closed': case 'resolved': return 'success';
      case 'in progress': case 'in review': return 'primary';
      case 'blocked': return 'danger';
      case 'to do': return 'secondary';
      default: return 'secondary';
    }
  };

  // Get priority icon
  const getPriorityIcon = (priority: string | undefined) => {
    switch (priority?.toLowerCase()) {
      case 'highest': case 'critical': return { icon: '🔴', color: chartColors.danger };
      case 'high': return { icon: '🟠', color: chartColors.orange };
      case 'medium': return { icon: '🟡', color: chartColors.warning };
      case 'low': case 'lowest': return { icon: '🟢', color: chartColors.success };
      default: return { icon: '⚪', color: chartColors.neutral600 };
    }
  };

  // Get issue type icon
  const getTypeIcon = (type: string | undefined) => {
    switch (type?.toLowerCase()) {
      case 'bug': return '🐛';
      case 'story': return '📖';
      case 'task': return '✓';
      case 'epic': return '⚡';
      case 'sub-task': return '↳';
      default: return '📋';
    }
  };

  return (
    <div className="ab-search-page ab-enhanced-search">
      {/* Header */}
      <div className="ab-search-header">
        <div className="ab-search-title">
          <h1>Search Issues</h1>
          <p>Find issues using JQL or basic filters</p>
        </div>
        <div className="ab-search-actions">
          <button
            className="ab-btn ab-btn-secondary"
            onClick={() => setShowSavedFilters(!showSavedFilters)}
          >
            📑 Saved Filters
          </button>
          <button
            className="ab-btn ab-btn-secondary"
            onClick={() => setShowSubscriptions(true)}
          >
            🔔 Subscribe
          </button>
          {issues && issues.length > 0 && (
            <button
              className="ab-btn ab-btn-secondary"
              onClick={() => setIsExportOpen(true)}
            >
              📥 Export
            </button>
          )}
        </div>
      </div>

      {/* Search Mode Toggle */}
      <div className="ab-search-mode-toggle">
        <button
          className={`ab-mode-btn ${!isJqlMode ? 'active' : ''}`}
          onClick={() => setIsJqlMode(false)}
        >
          Basic Search
        </button>
        <button
          className={`ab-mode-btn ${isJqlMode ? 'active' : ''}`}
          onClick={() => setIsJqlMode(true)}
        >
          JQL Search
        </button>
      </div>

      {/* Search Input */}
      <div className="ab-search-container">
        {isJqlMode ? (
          <JQLAutocomplete
            value={jqlQuery}
            onChange={handleJqlChange}
            placeholder="project = JRA AND issuetype = Bug AND assignee = currentUser() ORDER BY priority DESC"
            error={queryError}
          />
        ) : (
          <div className="ab-basic-search-bar">
            <div className="ab-search-input-wrapper">
              <span className="ab-search-icon">🔍</span>
              <input
                type="text"
                className="ab-search-input"
                placeholder="Search by issue key, title, or description..."
                value={basicFilters.text}
                onChange={(e) => setBasicFilters({ ...basicFilters, text: e.target.value })}
              />
              {basicFilters.text && (
                <button
                  className="ab-search-clear"
                  onClick={() => setBasicFilters({ ...basicFilters, text: '' })}
                >
                  ×
                </button>
              )}
            </div>
            <button
              className={`ab-btn ab-btn-secondary ${showFilterPanel ? 'active' : ''}`}
              onClick={() => setShowFilterPanel(!showFilterPanel)}
            >
              🔽 Filters
            </button>
          </div>
        )}

        {/* Search button */}
        <button
          className="ab-btn ab-btn-primary ab-search-btn"
          onClick={() => refetch()}
        >
          Search
        </button>
      </div>

      {/* Quick Filters */}
      <div className="ab-quick-filters">
        <span className="ab-quick-filters-label">Quick filters:</span>
        {QUICK_FILTERS.map((filter) => (
          <button
            key={filter.id}
            className={`ab-quick-filter ${isJqlMode && jqlQuery === filter.jql ? 'active' : ''}`}
            onClick={() => handleQuickFilter(filter.jql)}
          >
            {filter.label}
          </button>
        ))}
      </div>

      {/* Filter Panel */}
      {showFilterPanel && !isJqlMode && (
        <div className="ab-filters-panel">
          <div className="ab-filters-grid">
            <div className="ab-form-group">
              <label className="ab-label">Project</label>
              <select
                className="ab-select"
                value={basicFilters.projectId}
                onChange={(e) => setBasicFilters({ ...basicFilters, projectId: e.target.value })}
              >
                <option value="">Any Project</option>
                {projects?.map((project) => (
                  <option key={project.id} value={project.projectKey || project.name}>
                    {project.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Status</label>
              <select
                className="ab-select"
                value={basicFilters.status}
                onChange={(e) => setBasicFilters({ ...basicFilters, status: e.target.value })}
              >
                <option value="">Any Status</option>
                <option value="To Do">To Do</option>
                <option value="In Progress">In Progress</option>
                <option value="In Review">In Review</option>
                <option value="Done">Done</option>
                <option value="Blocked">Blocked</option>
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Priority</label>
              <select
                className="ab-select"
                value={basicFilters.priority}
                onChange={(e) => setBasicFilters({ ...basicFilters, priority: e.target.value })}
              >
                <option value="">Any Priority</option>
                <option value="Highest">Highest</option>
                <option value="High">High</option>
                <option value="Medium">Medium</option>
                <option value="Low">Low</option>
                <option value="Lowest">Lowest</option>
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Type</label>
              <select
                className="ab-select"
                value={basicFilters.type}
                onChange={(e) => setBasicFilters({ ...basicFilters, type: e.target.value })}
              >
                <option value="">Any Type</option>
                <option value="Bug">Bug</option>
                <option value="Story">Story</option>
                <option value="Task">Task</option>
                <option value="Epic">Epic</option>
                <option value="Sub-task">Sub-task</option>
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Sort By</label>
              <select
                className="ab-select"
                value={sortField}
                onChange={(e) => setSortField(e.target.value)}
              >
                {SORT_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Sort Direction</label>
              <select
                className="ab-select"
                value={sortDirection}
                onChange={(e) => setSortDirection(e.target.value as 'ASC' | 'DESC')}
              >
                <option value="DESC">Descending</option>
                <option value="ASC">Ascending</option>
              </select>
            </div>
          </div>

          <div className="ab-filters-actions">
            <button
              className="ab-btn ab-btn-ghost"
              onClick={() => setBasicFilters({
                projectId: '', status: '', priority: '', assignee: '', type: '', text: ''
              })}
            >
              Clear All
            </button>
          </div>
        </div>
      )}

      {/* Results Header */}
      <div className="ab-results-header">
        <div className="ab-results-info">
          <span className="ab-results-count">
            {isLoading ? 'Searching...' : `${issues?.length || 0} issue${(issues?.length || 0) !== 1 ? 's' : ''} found`}
          </span>
          {effectiveJql && (
            <span className="ab-query-preview" title={effectiveJql}>
              Query: {effectiveJql.substring(0, 100)}{effectiveJql.length > 100 ? '...' : ''}
            </span>
          )}
        </div>

        <div className="ab-results-controls">
          {/* Column selector */}
          <div className="ab-column-selector">
            <button className="ab-btn ab-btn-ghost ab-btn-sm">
              Columns ▾
            </button>
          </div>

          {/* View mode toggle */}
          <div className="ab-view-toggle">
            {VIEW_MODES.map(mode => (
              <button
                key={mode}
                className={`ab-view-btn ${viewMode === mode ? 'active' : ''}`}
                onClick={() => setViewMode(mode)}
              >
                {mode === 'list' && '📋'}
                {mode === 'board' && '📊'}
                {mode === 'detail' && '📄'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Results */}
      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
          <p>Searching issues...</p>
        </div>
      ) : error ? (
        <div className="ab-error-state">
          <span className="ab-error-icon">⚠️</span>
          <h3>Search Error</h3>
          <p>{error instanceof Error ? error.message : 'Failed to execute search'}</p>
        </div>
      ) : issues && issues.length > 0 ? (
        <div className={`ab-results-${viewMode}`}>
          {viewMode === 'list' && (
            <div className="ab-card">
              <table className="ab-table ab-results-table">
                <thead>
                  <tr>
                    {selectedColumns.includes('type') && <th>Type</th>}
                    {selectedColumns.includes('key') && <th>Key</th>}
                    {selectedColumns.includes('summary') && <th>Summary</th>}
                    {selectedColumns.includes('status') && <th>Status</th>}
                    {selectedColumns.includes('priority') && <th>Priority</th>}
                    {selectedColumns.includes('assignee') && <th>Assignee</th>}
                    {selectedColumns.includes('reporter') && <th>Reporter</th>}
                    {selectedColumns.includes('created') && <th>Created</th>}
                    {selectedColumns.includes('updated') && <th>Updated</th>}
                    {selectedColumns.includes('duedate') && <th>Due Date</th>}
                    {selectedColumns.includes('labels') && <th>Labels</th>}
                    {selectedColumns.includes('sprint') && <th>Sprint</th>}
                  </tr>
                </thead>
                <tbody>
                  {issues.map((issue) => (
                    <tr
                      key={issue.id}
                      onClick={() => navigate(`/issues/${issue.id}`)}
                      className="ab-clickable-row"
                    >
                      {selectedColumns.includes('type') && (
                        <td>
                          <span title={issue.issueType}>
                            {getTypeIcon(issue.issueType)}
                          </span>
                        </td>
                      )}
                      {selectedColumns.includes('key') && (
                        <td className="ab-font-mono">{issue.issueKey}</td>
                      )}
                      {selectedColumns.includes('summary') && (
                        <td className="ab-summary-cell">{issue.title}</td>
                      )}
                      {selectedColumns.includes('status') && (
                        <td>
                          <span className={`ab-badge ab-badge-${getStatusVariant(issue.status)}`}>
                            {issue.status || 'To Do'}
                          </span>
                        </td>
                      )}
                      {selectedColumns.includes('priority') && (
                        <td>{getPriorityIcon(issue.priority).icon}</td>
                      )}
                      {selectedColumns.includes('assignee') && (
                        <td>
                          {issue.assigneeId ? (
                            <span className="ab-user-cell">
                              <span className="ab-avatar-xs">
                                {(issue.assigneeId as string).charAt(0).toUpperCase()}
                              </span>
                              <span className="ab-username">
                                {String(issue.assigneeId).split('-')[0]}
                              </span>
                            </span>
                          ) : '-'}
                        </td>
                      )}
                      {selectedColumns.includes('reporter') && (
                        <td>
                          {issue.reporterId ? String(issue.reporterId).split('-')[0] : '-'}
                        </td>
                      )}
                      {selectedColumns.includes('created') && (
                        <td className="ab-date-cell">
                          {issue.createdAt ? new Date(issue.createdAt).toLocaleDateString() : '-'}
                        </td>
                      )}
                      {selectedColumns.includes('updated') && (
                        <td className="ab-date-cell">
                          {issue.updatedAt ? new Date(issue.updatedAt).toLocaleDateString() : '-'}
                        </td>
                      )}
                      {selectedColumns.includes('duedate') && (
                        <td>{issue.dueDate || '-'}</td>
                      )}
                      {selectedColumns.includes('labels') && (
                        <td>{(issue as any).labels?.join(', ') || '-'}</td>
                      )}
                      {selectedColumns.includes('sprint') && (
                        <td>{(issue as any).sprintName || '-'}</td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {viewMode === 'board' && (
            <div className="ab-results-board">
              {['To Do', 'In Progress', 'In Review', 'Done'].map((status) => {
                const statusIssues = issues.filter(i =>
                  i.status?.toLowerCase().replace(' ', '') === status.toLowerCase().replace(' ', '')
                );
                if (statusIssues.length === 0) return null;

                return (
                  <div key={status} className="ab-board-column">
                    <div className="ab-column-header">
                      <span>{status}</span>
                      <span className="ab-column-count">{statusIssues.length}</span>
                    </div>
                    <div className="ab-column-cards">
                      {statusIssues.map((issue) => (
                        <div
                          key={issue.id}
                          className="ab-board-card"
                          onClick={() => navigate(`/issues/${issue.id}`)}
                        >
                          <div className="ab-card-header">
                            <span>{getTypeIcon(issue.issueType)}</span>
                            <span className="ab-card-key">{issue.issueKey}</span>
                            <span>{getPriorityIcon(issue.priority).icon}</span>
                          </div>
                          <div className="ab-card-title">{issue.title}</div>
                          {issue.assigneeId && (
                            <div className="ab-card-footer">
                              <span className="ab-avatar-xs">
                                {String(issue.assigneeId).charAt(0).toUpperCase()}
                              </span>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {viewMode === 'detail' && (
            <div className="ab-results-detail">
              {issues.map((issue) => (
                <div
                  key={issue.id}
                  className="ab-detail-card"
                  onClick={() => navigate(`/issues/${issue.id}`)}
                >
                  <div className="ab-detail-header">
                    <span className="ab-detail-type">{getTypeIcon(issue.issueType)}</span>
                    <span className="ab-detail-key">{issue.issueKey}</span>
                    <span className={`ab-badge ab-badge-${getStatusVariant(issue.status)}`}>
                      {issue.status}
                    </span>
                    <span>{getPriorityIcon(issue.priority).icon}</span>
                  </div>
                  <h3 className="ab-detail-summary">{issue.title}</h3>
                  <div className="ab-detail-meta">
                    <span>Assignee: {issue.assigneeId ? String(issue.assigneeId).split('-')[0] : 'Unassigned'}</span>
                    <span>Created: {issue.createdAt ? new Date(issue.createdAt).toLocaleDateString() : '-'}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className="ab-card">
          <div className="ab-empty-state">
            <div className="ab-empty-icon">🔍</div>
            <h3>No issues found</h3>
            <p>Try adjusting your search criteria or filters</p>
            <button
              className="ab-btn ab-btn-primary"
              onClick={() => {
                setJqlQuery('');
                setBasicFilters({ projectId: '', status: '', priority: '', assignee: '', type: '', text: '' });
              }}
            >
              Clear Search
            </button>
          </div>
        </div>
      )}

      {/* Saved Filters Panel */}
      {showSavedFilters && (
        <SavedFilters
          onSelectFilter={(jql) => {
            setJqlQuery(jql);
            setIsJqlMode(true);
            setShowSavedFilters(false);
          }}
          onClose={() => setShowSavedFilters(false)}
        />
      )}

      {/* Export Modal */}
      {isExportOpen && (
        <SearchExport
          issues={issues || []}
          onClose={() => setIsExportOpen(false)}
        />
      )}

      {/* Subscriptions Panel */}
      {showSubscriptions && (
        <FilterSubscriptions
          filterName="Custom Filter"
          jql={effectiveJql}
          onClose={() => setShowSubscriptions(false)}
        />
      )}

      <style>{`
        .ab-enhanced-search {
          padding: var(--ab-spacing-lg);
          max-width: 1600px;
          margin: 0 auto;
        }

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

        .ab-search-actions {
          display: flex;
          gap: var(--ab-spacing-sm);
        }

        .ab-search-mode-toggle {
          display: flex;
          background: var(--ab-gray-100);
          border-radius: var(--ab-radius-md);
          padding: 2px;
          width: fit-content;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-mode-btn {
          padding: var(--ab-spacing-xs) var(--ab-spacing-lg);
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          background: transparent;
          border: none;
          border-radius: var(--ab-radius-sm);
          cursor: pointer;
          color: var(--ab-gray-600);
          transition: all var(--ab-transition-fast);
        }

        .ab-mode-btn.active {
          background: var(--ab-white);
          color: var(--ab-primary-600);
          box-shadow: var(--ab-shadow-sm);
        }

        .ab-search-container {
          display: flex;
          gap: var(--ab-spacing-sm);
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-search-container > *:first-child {
          flex: 1;
        }

        .ab-search-btn {
          flex-shrink: 0;
        }

        .ab-basic-search-bar {
          flex: 1;
          display: flex;
          gap: var(--ab-spacing-sm);
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

        .ab-quick-filters {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          margin-bottom: var(--ab-spacing-lg);
          flex-wrap: wrap;
        }

        .ab-quick-filters-label {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
          font-weight: 500;
        }

        .ab-quick-filter {
          display: inline-flex;
          align-items: center;
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

        .ab-quick-filter.active {
          background: var(--ab-primary-100);
          border-color: var(--ab-primary-300);
          color: var(--ab-primary-700);
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
          grid-template-columns: repeat(6, 1fr);
          gap: var(--ab-spacing-md);
        }

        @media (max-width: 1200px) {
          .ab-filters-grid { grid-template-columns: repeat(3, 1fr); }
        }

        @media (max-width: 768px) {
          .ab-filters-grid { grid-template-columns: repeat(2, 1fr); }
        }

        .ab-results-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-results-info {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-xs);
        }

        .ab-results-count {
          font-size: var(--ab-font-size-base);
          font-weight: 600;
          color: var(--ab-gray-800);
        }

        .ab-query-preview {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
          font-family: var(--ab-font-mono);
        }

        .ab-results-controls {
          display: flex;
          gap: var(--ab-spacing-md);
          align-items: center;
        }

        .ab-view-toggle {
          display: flex;
          background: var(--ab-gray-100);
          border-radius: var(--ab-radius-md);
          padding: 2px;
        }

        .ab-view-btn {
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          font-size: var(--ab-font-size-base);
          background: transparent;
          border: none;
          border-radius: var(--ab-radius-sm);
          cursor: pointer;
          transition: all var(--ab-transition-fast);
        }

        .ab-view-btn.active {
          background: var(--ab-white);
          box-shadow: var(--ab-shadow-sm);
        }

        .ab-results-table {
          width: 100%;
        }

        .ab-results-table th {
          text-align: left;
          font-weight: 600;
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-600);
          padding: var(--ab-spacing-sm);
          border-bottom: 2px solid var(--ab-gray-200);
        }

        .ab-results-table td {
          padding: var(--ab-spacing-sm);
          border-bottom: 1px solid var(--ab-gray-100);
          font-size: var(--ab-font-size-sm);
        }

        .ab-clickable-row {
          cursor: pointer;
        }

        .ab-clickable-row:hover {
          background: var(--ab-gray-50);
        }

        .ab-summary-cell {
          max-width: 300px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .ab-user-cell {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
        }

        .ab-date-cell {
          color: var(--ab-gray-500);
          font-size: var(--ab-font-size-xs);
        }

        .ab-results-board {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: var(--ab-spacing-md);
        }

        @media (max-width: 1200px) {
          .ab-results-board { grid-template-columns: repeat(2, 1fr); }
        }

        @media (max-width: 768px) {
          .ab-results-board { grid-template-columns: 1fr; }
        }

        .ab-results-detail {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
        }

        .ab-detail-card {
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-md);
          cursor: pointer;
          transition: all var(--ab-transition-fast);
        }

        .ab-detail-card:hover {
          box-shadow: var(--ab-shadow-md);
          border-color: var(--ab-gray-300);
        }

        .ab-detail-header {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-detail-key {
          font-family: var(--ab-font-mono);
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
        }

        .ab-detail-summary {
          font-size: var(--ab-font-size-base);
          font-weight: 500;
          margin: 0 0 var(--ab-spacing-xs);
        }

        .ab-detail-meta {
          display: flex;
          gap: var(--ab-spacing-lg);
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
        }

        .ab-loading {
          text-align: center;
          padding: var(--ab-spacing-xl);
        }

        .ab-error-state {
          text-align: center;
          padding: var(--ab-spacing-xl);
        }

        .ab-error-icon {
          font-size: 48px;
          display: block;
          margin-bottom: var(--ab-spacing-md);
        }
      `}</style>
    </div>
  );
}