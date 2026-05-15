import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useRef } from 'react';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { commentApi } from '../../../api/commentApi';
import { labelApi } from '../../../api/labelApi';
import EditIssueModal from '../components/EditIssueModal';
import './IssueDetailPage.css';

type TabType = 'details' | 'people' | 'activity' | 'comment' | 'work';

interface FullIssueResponse extends IssueResponse {
  projectName?: string;
  epicId?: string;
  epicName?: string;
  storyPoints?: number;
  sprintId?: string;
  sprintName?: string;
  components?: string[];
  securityLevel?: string;
  parentId?: string;
  parentKey?: string;
  children?: IssueResponse[];
  votes?: number;
  watchers?: string[];
  linkedIssues?: Array<{ type: string; key: string; title: string }>;
  classification?: string;
  labels?: string[];
}

export default function IssueDetailPage() {
  const { issueId } = useParams<{ issueId: string }>();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabType>('details');
  const [showEditModal, setShowEditModal] = useState(false);
  const [showTransitionMenu, setShowTransitionMenu] = useState(false);
  const [showMoreMenu, setShowMoreMenu] = useState(false);
  const [showExportMenu, setShowExportMenu] = useState(false);
  const [newComment, setNewComment] = useState('');

  const { data: issue, isLoading } = useQuery<FullIssueResponse>({
    queryKey: ['issue', issueId],
    queryFn: async () => {
      const response = await issueApi.getById(issueId!);
      return response.data;
    },
    enabled: !!issueId,
  });

  const { data: comments } = useQuery({
    queryKey: ['comments', issueId],
    queryFn: async () => {
      const response = await commentApi.getByIssue(issueId!);
      return response.data;
    },
    enabled: !!issueId,
  });

  const { data: priorities } = useQuery({
    queryKey: ['priorities'],
    queryFn: async () => {
      const response = await issueApi.getPriorities();
      return response.data;
    },
  });

  const { data: statuses } = useQuery({
    queryKey: ['statuses'],
    queryFn: async () => {
      const response = await issueApi.getStatuses();
      return response.data;
    },
  });

  const addCommentMutation = useMutation({
    mutationFn: (content: string) => commentApi.create({ issueId: issueId!, content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comments', issueId] });
      setNewComment('');
    },
  });

  const transitionMutation = useMutation({
    mutationFn: (statusId: string) => issueApi.transitionStatus(issueId!, statusId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
      setShowTransitionMenu(false);
    },
  });

  const getStatusStyle = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'done': case 'resolved': case 'closed':
        return { background: '#dcfce7', color: '#166534' };
      case 'in progress': case 'in_review': case 'in progress':
        return { background: '#dbeafe', color: '#1e40af' };
      case 'blocked':
        return { background: '#fee2e2', color: '#991b1b' };
      default:
        return { background: '#f3f4f6', color: '#374151' };
    }
  };

  const getPriorityIcon = (priority: string) => {
    switch (priority?.toLowerCase()) {
      case 'highest': return '▲';
      case 'high': return '▲';
      case 'medium': return '◆';
      case 'low': return '▼';
      case 'lowest': return '▼';
      default: return '◆';
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority?.toLowerCase()) {
      case 'highest': case 'high': return '#D04437';
      case 'medium': return '#FF8B00';
      default: return '#6B778C';
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type?.toLowerCase()) {
      case 'bug': return '🐛';
      case 'story': return '📖';
      case 'task': return '✓';
      case 'epic': return '⚡';
      default: return '📋';
    }
  };

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
    });
  };

  const formatDateTime = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
      hour: 'numeric', minute: '2-digit',
    });
  };

  const getRelativeTime = (dateStr: string | undefined) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return formatDate(dateStr);
  };

  const formatTime = (seconds: number | undefined) => {
    if (!seconds) return '-';
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    if (hours >= 24) return `${Math.floor(hours / 24)}d ${hours % 24}h`;
    return `${hours}h ${mins}m`;
  };

  if (isLoading) {
    return (
      <div className="idc-loading">
        <div className="idc-spinner" />
      </div>
    );
  }

  if (!issue) {
    return (
      <div className="idc-empty">
        <h3>Issue not found</h3>
        <Link to="/issues" className="idc-btn idc-btn-primary">Back to Issues</Link>
      </div>
    );
  }

  const statusStyle = getStatusStyle(issue.status || '');

  return (
    <div className="idc-issue-view">
      <div className="idc-breadcrumb">
        <Link to="/issues" className="idc-breadcrumb-project">Issues</Link>
        <span className="idc-breadcrumb-sep">/</span>
        {issue.projectId && (
          <>
            <Link to={`/projects/${issue.projectId}`} className="idc-breadcrumb-project">
              {issue.projectName || 'Project'}
            </Link>
            <span className="idc-breadcrumb-sep">/</span>
          </>
        )}
        <span className="idc-breadcrumb-key">{issue.issueKey}</span>
      </div>

      {/* Issue Header */}
          <div className="idc-issue-header">
            <div className="idc-issue-header-top">
              {/* Type + Status */}
              <div className="idc-type-status">
                <span className="idc-type-badge">
                  <span>{getTypeIcon(issue.issueType)}</span>
                  <span>{issue.issueType || 'Story'}</span>
                </span>
                <div
                  className="idc-status-badge"
                  style={{ background: statusStyle.background, color: statusStyle.color }}
                >
                  {issue.status || 'To Do'}
                  <span className="idc-status-caret">▾</span>
                </div>
              </div>

              {/* Right Actions */}
              <div className="idc-issue-actions">
                <button className="idc-action-btn" onClick={() => setShowEditModal(true)}>
                  Edit
                </button>
                <button className="idc-action-btn" onClick={() => {
                  const content = newComment;
                  if (content.trim()) addCommentMutation.mutate(content);
                }}>
                  Add comment
                </button>
                <button className="idc-action-btn">
                  Assign
                </button>
                <div className="idc-dropdown-wrapper">
                  <button
                    className="idc-action-btn"
                    onClick={() => setShowMoreMenu(!showMoreMenu)}
                  >
                    More <span className="idc-dropdown-caret">▾</span>
                  </button>
                  {showMoreMenu && (
                    <div className="idc-dropdown-menu">
                      <button className="idc-dropdown-item">Link issues</button>
                      <button className="idc-dropdown-item">Create subtask</button>
                      <button className="idc-dropdown-item">Clone issue</button>
                      <button className="idc-dropdown-item">Delete</button>
                    </div>
                  )}
                </div>
                <button
                  className="idc-status-transition-btn"
                  onClick={() => setShowTransitionMenu(!showTransitionMenu)}
                >
                  In Progress <span className="idc-dropdown-caret">▾</span>
                </button>
                <button className="idc-action-btn">Admin</button>
                <button className="idc-icon-btn" title="Share">🔗</button>
                <div className="idc-dropdown-wrapper">
                  <button
                    className="idc-action-btn"
                    onClick={() => setShowExportMenu(!showExportMenu)}
                  >
                    Export <span className="idc-dropdown-caret">▾</span>
                  </button>
                  {showExportMenu && (
                    <div className="idc-dropdown-menu idc-dropdown-right">
                      <button className="idc-dropdown-item">Export to Word</button>
                      <button className="idc-dropdown-item">Export to PDF</button>
                      <button className="idc-dropdown-item">Print</button>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Title */}
            <h1 className="idc-issue-title">{issue.title}</h1>

            {/* Metadata */}
            <div className="idc-issue-meta">
              <span className="idc-meta-key">{issue.issueKey}</span>
              <span className="idc-meta-sep">—</span>
              <span className="idc-meta-created">
                Created by {String(issue.reporterId || '').split('-')[0] || 'Unknown'} · {getRelativeTime(issue.createdAt)}
              </span>
            </div>

            {/* Transition Menu */}
            {showTransitionMenu && (
              <div className="idc-transition-menu">
                <div className="idc-transition-header">Status</div>
                {statuses?.map((s) => (
                  <button
                    key={s.id}
                    className="idc-transition-option"
                    onClick={() => {
                      transitionMutation.mutate(s.id);
                      setShowTransitionMenu(false);
                    }}
                  >
                    {s.name}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Two Column Layout */}
          <div className="idc-issue-body">
            {/* ========== LEFT COLUMN ========== */}
            <div className="idc-left-col">
              {/* Details Section */}
              <div className="idc-section">
                <div className="idc-section-header">
                  <h3>Details</h3>
                  <button className="idc-section-btn" onClick={() => setShowEditModal(true)}>
                    Edit
                  </button>
                </div>
                <div className="idc-details-table">
                  <div className="idc-detail-row">
                    <span className="idc-detail-label">Assignee</span>
                    <div className="idc-detail-value">
                      {issue.assigneeId ? (
                        <span className="idc-user">
                          <span className="idc-user-avatar">
                            {String(issue.assigneeId).charAt(0).toUpperCase()}
                          </span>
                          <span className="idc-user-name">
                            {String(issue.assigneeId).split('-')[0]}
                          </span>
                        </span>
                      ) : (
                        <span className="idc-unassigned">Unassigned</span>
                      )}
                    </div>
                  </div>
                  <div className="idc-detail-row">
                    <span className="idc-detail-label">Reporter</span>
                    <div className="idc-detail-value">
                      <span className="idc-user">
                        <span className="idc-user-avatar idc-user-avatar-green">
                          {String(issue.reporterId || 'U').charAt(0).toUpperCase()}
                        </span>
                        <span className="idc-user-name">
                          {String(issue.reporterId || '').split('-')[0] || 'Unknown'}
                        </span>
                      </span>
                    </div>
                  </div>
                  <div className="idc-detail-row">
                    <span className="idc-detail-label">Priority</span>
                    <div className="idc-detail-value">
                      <span style={{ color: getPriorityColor(issue.priority || '') }}>
                        {getPriorityIcon(issue.priority || '')}
                      </span>
                      <span>{issue.priority || 'Medium'}</span>
                    </div>
                  </div>
                  <div className="idc-detail-row">
                    <span className="idc-detail-label">Labels</span>
                    <div className="idc-detail-value">
                      {(issue as any).labels?.length > 0 ? (
                        <div className="idc-labels">
                          {(issue as any).labels.map((l: string) => (
                            <span key={l} className="idc-label">{l}</span>
                          ))}
                        </div>
                      ) : (
                        <span className="idc-no-value">None</span>
                      )}
                    </div>
                  </div>
                  <div className="idc-detail-row">
                    <span className="idc-detail-label">Sprint</span>
                    <div className="idc-detail-value">
                      {issue.sprintName ? (
                        <span className="idc-sprint-tag">{issue.sprintName}</span>
                      ) : (
                        <span className="idc-no-value">Backlog</span>
                      )}
                    </div>
                  </div>
                  <div className="idc-detail-row">
                    <span className="idc-detail-label">Epic Link</span>
                    <div className="idc-detail-value">
                      {issue.epicId ? (
                        <span className="idc-epic-link">⚡ {issue.epicName || 'Epic'}</span>
                      ) : (
                        <span className="idc-no-value">None</span>
                      )}
                    </div>
                  </div>
                  <div className="idc-detail-row">
                    <span className="idc-detail-label">Story Points</span>
                    <div className="idc-detail-value">
                      {issue.storyPoints !== undefined ? (
                        <span className="idc-story-points">{issue.storyPoints}</span>
                      ) : (
                        <span className="idc-no-value">None</span>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Description */}
              <div className="idc-section">
                <div className="idc-section-header">
                  <h3>Description</h3>
                </div>
                <div className="idc-description">
                  {issue.description ? (
                    <p className="idc-description-text">{issue.description}</p>
                  ) : (
                    <span className="idc-description-placeholder">
                      Click to add description...
                    </span>
                  )}
                </div>
              </div>

              {/* Activity Tabs */}
              <div className="idc-tabs">
                <button
                  className={`idc-tab ${activeTab === 'details' ? 'idc-tab-active' : ''}`}
                  onClick={() => setActiveTab('details')}
                >
                  Details
                </button>
                <button
                  className={`idc-tab ${activeTab === 'comment' ? 'idc-tab-active' : ''}`}
                  onClick={() => setActiveTab('comment')}
                >
                  Comment
                </button>
                <button
                  className={`idc-tab ${activeTab === 'activity' ? 'idc-tab-active' : ''}`}
                  onClick={() => setActiveTab('activity')}
                >
                  Activity
                </button>
                <button
                  className={`idc-tab ${activeTab === 'work' ? 'idc-tab-active' : ''}`}
                  onClick={() => setActiveTab('work')}
                >
                  Work Log
                </button>
                <button
                  className={`idc-tab ${activeTab === 'people' ? 'idc-tab-active' : ''}`}
                  onClick={() => setActiveTab('people')}
                >
                  People
                </button>
              </div>

              <div className="idc-tab-content">
                {/* Details Tab */}
                {activeTab === 'details' && (
                  <div className="idc-tab-details">
                    <div className="idc-detail-col">
                      <div className="idc-detail-row-2">
                        <span className="idc-detail-label-2">Type</span>
                        <span>{issue.issueType || 'Story'}</span>
                      </div>
                      <div className="idc-detail-row-2">
                        <span className="idc-detail-label-2">Priority</span>
                        <span style={{ color: getPriorityColor(issue.priority || '') }}>
                          {getPriorityIcon(issue.priority || '')} {issue.priority || 'Medium'}
                        </span>
                      </div>
                      <div className="idc-detail-row-2">
                        <span className="idc-detail-label-2">Status</span>
                        <span style={{ fontWeight: 500 }}>{issue.status || 'To Do'}</span>
                      </div>
                    </div>
                    <div className="idc-detail-col">
                      <div className="idc-detail-row-2">
                        <span className="idc-detail-label-2">Affects Version</span>
                        <span className="idc-no-value">None</span>
                      </div>
                      <div className="idc-detail-row-2">
                        <span className="idc-detail-label-2">Fix Version</span>
                        <span className="idc-no-value">None</span>
                      </div>
                      <div className="idc-detail-row-2">
                        <span className="idc-detail-label-2">Components</span>
                        <span className="idc-no-value">None</span>
                      </div>
                    </div>
                  </div>
                )}

                {/* Comment Tab */}
                {activeTab === 'comment' && (
                  <div className="idc-comment-tab">
                    <div className="idc-compose">
                      <div className="idc-compose-avatar">S</div>
                      <div className="idc-compose-body">
                        <textarea
                          className="idc-compose-input"
                          placeholder="Add a comment..."
                          value={newComment}
                          onChange={(e) => setNewComment(e.target.value)}
                          rows={3}
                        />
                        <div className="idc-compose-toolbar">
                          <button className="idc-compose-tool">B</button>
                          <button className="idc-compose-tool">I</button>
                          <button className="idc-compose-tool">U</button>
                          <button className="idc-compose-tool">🔗</button>
                          <button className="idc-compose-tool">📎</button>
                        </div>
                        <div className="idc-compose-footer">
                          <span className="idc-compose-visibility">All Users</span>
                          <button
                            className="idc-btn idc-btn-primary idc-btn-sm"
                            onClick={() => {
                              if (newComment.trim()) addCommentMutation.mutate(newComment);
                            }}
                            disabled={!newComment.trim() || addCommentMutation.isPending}
                          >
                            Save
                          </button>
                        </div>
                      </div>
                    </div>
                    {comments && comments.length > 0 ? (
                      <div className="idc-comments-list">
                        {comments.map((c: any) => (
                          <div key={c.id} className="idc-comment">
                            <div className="idc-comment-avatar">
                              {String(c.authorId || 'U').charAt(0).toUpperCase()}
                            </div>
                            <div className="idc-comment-body">
                              <div className="idc-comment-header">
                                <span className="idc-comment-author">
                                  {String(c.authorId || '').split('-')[0]}
                                </span>
                                <span className="idc-comment-time">
                                  {getRelativeTime(c.createdAt)}
                                </span>
                              </div>
                              <p className="idc-comment-text">{c.content}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="idc-no-comments">No comments yet.</div>
                    )}
                  </div>
                )}

                {/* Activity Tab */}
                {activeTab === 'activity' && (
                  <div className="idc-activity-tab">
                    <div className="idc-activity-filter">
                      <span className="idc-activity-label">Show:</span>
                      <button className="idc-activity-chosen">All</button>
                      <button className="idc-activity-opt">Comments</button>
                      <button className="idc-activity-opt">History</button>
                    </div>
                    <div className="idc-activity-timeline">
                      <div className="idc-activity-empty">
                        No activity recorded yet.
                      </div>
                    </div>
                  </div>
                )}

                {/* Work Tab */}
                {activeTab === 'work' && (
                  <div className="idc-work-tab">
                    <div className="idc-work-summary">
                      <div className="idc-work-item">
                        <span className="idc-work-label">Remaining Estimate</span>
                        <span className="idc-work-value">-</span>
                      </div>
                      <div className="idc-work-item">
                        <span className="idc-work-label">Time Spent</span>
                        <span className="idc-work-value">-</span>
                      </div>
                    </div>
                    <button className="idc-btn idc-btn-secondary idc-btn-sm">
                      Log Work
                    </button>
                  </div>
                )}

                {/* People Tab */}
                {activeTab === 'people' && (
                  <div className="idc-people-tab">
                    <div className="idc-people-list">
                      <div className="idc-people-row">
                        <span className="idc-detail-label-2">Assignee</span>
                        <span className="idc-user">
                          <span className="idc-user-avatar">
                            {String(issue.assigneeId || 'U').charAt(0).toUpperCase()}
                          </span>
                          <span>{String(issue.assigneeId || '').split('-')[0]}</span>
                        </span>
                      </div>
                      <div className="idc-people-row">
                        <span className="idc-detail-label-2">Reporter</span>
                        <span className="idc-user">
                          <span className="idc-user-avatar idc-user-avatar-green">
                            {String(issue.reporterId || 'U').charAt(0).toUpperCase()}
                          </span>
                          <span>{String(issue.reporterId || '').split('-')[0]}</span>
                        </span>
                      </div>
                      <div className="idc-people-row">
                        <span className="idc-detail-label-2">Watchers</span>
                        <span className="idc-no-value">{issue.watchers?.length || 0} watching</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Attachments */}
              <div className="idc-section">
                <div className="idc-section-header">
                  <h3>Attachments</h3>
                  <button className="idc-section-btn">Add files</button>
                </div>
                <div className="idc-empty-section">No attachments</div>
              </div>
            </div>

            {/* ========== RIGHT COLUMN ========== */}
            <div className="idc-right-col">
              {/* People */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">People</h4>
                <div className="idc-right-rows">
                  <div className="idc-right-row">
                    <span className="idc-right-label">Assignee</span>
                    <span className="idc-right-value">
                      {issue.assigneeId ? (
                        <span className="idc-user">
                          <span className="idc-user-avatar-sm">
                            {String(issue.assigneeId).charAt(0).toUpperCase()}
                          </span>
                          <span className="idc-user-name-sm">
                            {String(issue.assigneeId).split('-')[0]}
                          </span>
                        </span>
                      ) : (
                        <button className="idc-right-action">Assign</button>
                      )}
                    </span>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Reporter</span>
                    <span className="idc-right-value">
                      <span className="idc-user">
                        <span className="idc-user-avatar-sm idc-user-avatar-green">
                          {String(issue.reporterId || 'U').charAt(0).toUpperCase()}
                        </span>
                        <span className="idc-user-name-sm">
                          {String(issue.reporterId || '').split('-')[0] || 'Unknown'}
                        </span>
                      </span>
                    </span>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Watchers</span>
                    <button className="idc-right-action">{issue.watchers?.length || 0}</button>
                  </div>
                </div>
              </div>

              {/* Dates */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">Dates</h4>
                <div className="idc-right-rows">
                  <div className="idc-right-row">
                    <span className="idc-right-label">Created</span>
                    <span className="idc-right-value" title={formatDateTime(issue.createdAt)}>
                      {formatDate(issue.createdAt)}
                    </span>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Updated</span>
                    <span className="idc-right-value" title={formatDateTime(issue.updatedAt)}>
                      {getRelativeTime(issue.updatedAt)}
                    </span>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Due Date</span>
                    <button className="idc-right-action">Set due date</button>
                  </div>
                </div>
              </div>

              {/* Priority & Type */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">Fields</h4>
                <div className="idc-right-rows">
                  <div className="idc-right-row">
                    <span className="idc-right-label">Priority</span>
                    <button className="idc-right-value idc-priority-btn">
                      <span style={{ color: getPriorityColor(issue.priority || '') }}>
                        {getPriorityIcon(issue.priority || '')}
                      </span>
                      {issue.priority || 'Medium'}
                    </button>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Type</span>
                    <button className="idc-right-value idc-type-btn">
                      <span>{getTypeIcon(issue.issueType)}</span>
                      {issue.issueType || 'Story'}
                    </button>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Resolution</span>
                    <span className="idc-right-value idc-no-val">Unresolved</span>
                  </div>
                </div>
              </div>

              {/* Sprint & Epic */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">Sprint & Epic</h4>
                <div className="idc-right-rows">
                  <div className="idc-right-row">
                    <span className="idc-right-label">Sprint</span>
                    <button className="idc-right-action">
                      {issue.sprintName || 'Backlog'}
                    </button>
                  </div>
                  {issue.epicId && (
                    <div className="idc-right-row">
                      <span className="idc-right-label">Epic</span>
                      <span className="idc-epic-link">⚡ {issue.epicName || 'Epic'}</span>
                    </div>
                  )}
                  {issue.storyPoints !== undefined && (
                    <div className="idc-right-row">
                      <span className="idc-right-label">Story Points</span>
                      <span className="idc-story-points-sm">{issue.storyPoints}</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Time Tracking */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">Time Tracking</h4>
                <div className="idc-right-rows">
                  <div className="idc-right-row">
                    <span className="idc-right-label">Original Estimate</span>
                    <span className="idc-right-value">
                      {issue.originalEstimate ? formatTime(issue.originalEstimate) : '-'}
                    </span>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Remaining</span>
                    <span className="idc-right-value">
                      {issue.remainingEstimate ? formatTime(issue.remainingEstimate) : '-'}
                    </span>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Time Spent</span>
                    <span className="idc-right-value">
                      {issue.timeSpent ? formatTime(issue.timeSpent) : '-'}
                    </span>
                  </div>
                  <button className="idc-log-work-btn">⏱ Log Work</button>
                </div>
              </div>

              {/* Components & Versions */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">Components & Versions</h4>
                <div className="idc-right-rows">
                  <div className="idc-right-row">
                    <span className="idc-right-label">Components</span>
                    <button className="idc-right-action">
                      {issue.components?.length ? issue.components.join(', ') : 'None'}
                    </button>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Affects Versions</span>
                    <span className="idc-right-value idc-no-val">-</span>
                  </div>
                  <div className="idc-right-row">
                    <span className="idc-right-label">Fix Versions</span>
                    <span className="idc-right-value idc-no-val">-</span>
                  </div>
                </div>
              </div>

              {/* Labels */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">Labels</h4>
                <div className="idc-right-rows">
                  <div className="idc-labels-row">
                    {(issue as any).labels?.length > 0 ? (
                      <div className="idc-labels">
                        {(issue as any).labels.map((l: string) => (
                          <span key={l} className="idc-label">{l}</span>
                        ))}
                      </div>
                    ) : (
                      <span className="idc-no-val">None</span>
                    )}
                    <button className="idc-right-action">Add</button>
                  </div>
                </div>
              </div>

              {/* Security */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">Security</h4>
                <div className="idc-right-rows">
                  <div className="idc-right-row">
                    <span className="idc-right-label">Security Level</span>
                    <button className="idc-right-action">
                      {issue.securityLevel || 'None'}
                    </button>
                  </div>
                  {issue.parentId && (
                    <div className="idc-right-row">
                      <span className="idc-right-label">Parent</span>
                      <Link to={`/issues/${issue.parentKey}`} className="idc-parent-link">
                        {issue.parentKey || 'Parent'}
                      </Link>
                    </div>
                  )}
                </div>
              </div>

              {/* Actions */}
              <div className="idc-right-section">
                <h4 className="idc-right-title">Actions</h4>
                <div className="idc-actions-list">
                  <button className="idc-action-link">👤 Assign to me</button>
                  <button className="idc-action-link">🔄 Change status</button>
                  <button className="idc-action-link">👁 Watch</button>
                  <button className="idc-action-link">📋 Clone issue</button>
                  <button className="idc-action-link idc-action-link-danger">🗑 Delete</button>
                </div>
              </div>
            </div>
          </div>

      {/* Edit Modal */}
      {showEditModal && (
        <EditIssueModal
          issue={issue}
          onClose={() => setShowEditModal(false)}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
            setShowEditModal(false);
          }}
        />
      )}
    </div>
  );
}