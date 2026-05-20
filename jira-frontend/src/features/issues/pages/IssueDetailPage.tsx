import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { commentApi } from '../../../api/commentApi';
import { labelApi } from '../../../api/labelApi';
import EditIssueModal from '../components/EditIssueModal';
import TransitionScreenForm, { type AvailableTransition } from '../components/TransitionScreenForm';
import './IssueDetailPage.css';

type TabType = 'details' | 'people' | 'activity' | 'comment' | 'work';

/**
 * Full Issue Response - All Systems and Avionics Mandatory Fields
 */
interface FullIssueResponse extends IssueResponse {
  // Core Metadata
  projectName?: string;
  issueKey: string;

  // Issue Type & Category
  issueType: string;
  issueTypeIcon?: string;
  issueTypeColor?: string;

  // Status & Priority
  status: string;
  statusCategory?: string;
  priority: string;
  priorityIcon?: string;
  priorityColor?: string;

  // Resolution
  resolutionId?: string;
  resolutionName?: string;
  resolutionDate?: string;

  // Project Information
  projectId: string;
  projectKey: string;

  // User Relationships
  assigneeId?: string;
  assigneeName?: string;
  assigneeAvatar?: string;
  reporterId?: string;
  reporterName?: string;
  reporterAvatar?: string;
  creatorId?: string;
  creatorName?: string;

  // Timestamps
  createdAt: string;
  updatedAt: string;
  lastViewedAt?: string;
  resolvedAt?: string;
  dueDate?: string;

  // Title & Description
  title: string;
  description?: string;
  environment?: string;

  // Versioning
  affectsVersions?: string[];
  fixVersions?: string[];

  // Organization
  components?: string[];
  labels?: string[];
  sprintId?: string;
  sprintName?: string;
  teamId?: string;
  teamName?: string;

  // Agile Fields
  epicId?: string;
  epicName?: string;
  epicColor?: string;
  storyPoints?: number;
  originalStoryPoints?: number;
  parentId?: string;
  parentKey?: string;

  // Time Tracking
  originalEstimate?: number;  // seconds
  remainingEstimate?: number;  // seconds
  timeSpent?: number;  // seconds
  aggregateTimeEstimate?: number;
  aggregateTimeSpent?: number;
  workRatio?: number;

  // Security
  securityLevelId?: string;
  securityLevelName?: string;

  // Social
  votes?: number;
  voteCount?: number;
  watcherCount?: number;
  watchers?: Array<{ id: string; name: string; avatar: string }>;
  linkedIssues?: Array<{ type: string; key: string; title: string }>;
  subtasks?: IssueResponse[];
  parent?: { id: string; key: string; title: string };

  // Custom Fields (dynamic)
  customFields?: Record<string, any>;
}

export default function IssueDetailPage() {
  const { issueId } = useParams<{ issueId: string }>();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabType>('details');
  const [showEditModal, setShowEditModal] = useState(false);
  const [showTransitionMenu, setShowTransitionMenu] = useState(false);
  const [showMoreMenu, setShowMoreMenu] = useState(false);
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

  const { data: availableTransitions } = useQuery({
    queryKey: ['issue-transitions', issueId, issue?.projectId],
    queryFn: async () => {
      if (!issueId || !issue?.projectId) return null;
      const response = await issueApi.getAvailableTransitions(issueId, issue.projectId);
      return response.data;
    },
    enabled: !!issueId && !!issue?.projectId,
  });

  const [transitionComment, setTransitionComment] = useState('');
  const [screenInput, setScreenInput] = useState<Record<string, unknown>>({});
  const [pendingTransition, setPendingTransition] = useState<AvailableTransition | null>(null);

  const addCommentMutation = useMutation({
    mutationFn: (content: string) => commentApi.create({ issueId: issueId!, content }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['comments', issueId] });
      setNewComment('');
    },
  });

  const transitionMutation = useMutation({
    mutationFn: (payload: {
      transitionId: string;
      toStatusId: string;
      comment?: string;
      resolutionId?: string;
      screenInput?: Record<string, unknown>;
    }) =>
      issueApi.transitionStatus(issueId!, issue!.projectId, {
        transitionId: payload.transitionId,
        statusId: payload.toStatusId,
        comment: payload.comment,
        resolutionId: payload.resolutionId,
        screenInput: payload.screenInput,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issue-transitions', issueId] });
      setShowTransitionMenu(false);
      setPendingTransition(null);
      setTransitionComment('');
      setScreenInput({});
    },
  });

  const confirmTransition = () => {
    if (!pendingTransition) return;
    const resolutionId =
      (screenInput.resolutionId as string) ||
      (screenInput.resolution as string) ||
      undefined;
    transitionMutation.mutate({
      transitionId: pendingTransition.id,
      toStatusId: pendingTransition.toStatusId,
      comment: transitionComment || (screenInput.comment as string) || undefined,
      resolutionId,
      screenInput: Object.keys(screenInput).length > 0 ? screenInput : undefined,
    });
  };

  // Helper functions
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
      case 'sub-task': return '↳';
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

  const formatTime = (seconds: number | undefined) => {
    if (!seconds) return '-';
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    if (hours >= 24) {
      const days = Math.floor(hours / 24);
      const remainingHours = hours % 24;
      return `${days}d ${remainingHours}h`;
    }
    return `${hours}h ${mins}m`;
  };

  const formatTimeWithDays = (seconds: number | undefined) => {
    if (!seconds) return '-';
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    if (hours >= 24) {
      const days = Math.floor(hours / 24);
      const remainingHours = hours % 24;
      return `${days}d ${remainingHours}h ${mins}m`;
    }
    return `${hours}h ${mins}m`;
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
      {/* Breadcrumb */}
      <div className="idc-breadcrumb">
        <Link to="/issues" className="idc-breadcrumb-project">Issues</Link>
        <span className="idc-breadcrumb-sep">/</span>
        <Link to={`/projects/${issue.projectId}`} className="idc-breadcrumb-project">
          {issue.projectName || issue.projectKey || 'Project'}
        </Link>
        <span className="idc-breadcrumb-sep">/</span>
        <span className="idc-breadcrumb-key">{issue.issueKey}</span>
      </div>

      {/* Issue Header - Systems and Avionics Style */}
      <div className="idc-issue-header">
        <div className="idc-issue-header-top">
          {/* Type + Status badges */}
          <div className="idc-type-status">
            <span className="idc-type-badge">
              <span className="idc-type-icon">{getTypeIcon(issue.issueType)}</span>
              <span className="idc-type-name">{issue.issueType || 'Story'}</span>
            </span>
            <div
              className="idc-status-badge"
              style={{ background: statusStyle.background, color: statusStyle.color }}
            >
              {issue.status || 'To Do'}
            </div>
          </div>

          {/* Actions */}
          <div className="idc-issue-actions">
            <button className="idc-action-btn" onClick={() => setShowEditModal(true)}>Edit</button>
            <div className="idc-dropdown-wrapper">
              <button className="idc-action-btn" onClick={() => setShowMoreMenu(!showMoreMenu)}>
                More <span className="idc-dropdown-caret">▾</span>
              </button>
              {showMoreMenu && (
                <div className="idc-dropdown-menu">
                  <button className="idc-dropdown-item">Link issues</button>
                  <button className="idc-dropdown-item">Create subtask</button>
                  <button className="idc-dropdown-item">Clone issue</button>
                  <button className="idc-dropdown-item">Move</button>
                  <div className="idc-dropdown-divider"></div>
                  <button className="idc-dropdown-item idc-dropdown-danger">Delete</button>
                </div>
              )}
            </div>
            <button
              className="idc-status-transition-btn"
              onClick={() => setShowTransitionMenu(!showTransitionMenu)}
            >
              <span>To Do</span>
              <span className="idc-dropdown-caret">▾</span>
            </button>
          </div>
        </div>

        {/* Title */}
        <h1 className="idc-issue-title">{issue.title}</h1>

        {/* Meta info */}
        <div className="idc-issue-meta">
          <span className="idc-meta-key">{issue.issueKey}</span>
          <span className="idc-meta-sep">—</span>
          <span className="idc-meta-created">
            Created by {issue.reporterName || 'Unknown'} · {getRelativeTime(issue.createdAt)}
          </span>
        </div>

        {/* Transition Menu */}
        {showTransitionMenu && (
          <div className="idc-transition-menu">
            <div className="idc-transition-header">Workflow transitions</div>
            {(availableTransitions?.transitions ?? []).length === 0 ? (
              <div className="idc-transition-option" style={{ cursor: 'default', color: '#5e6c84' }}>
                No transitions available
              </div>
            ) : (
              availableTransitions!.transitions.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  className="idc-transition-option"
                  onClick={() => {
                    setPendingTransition({
                      id: t.id,
                      name: t.name,
                      description: t.description,
                      toStatusId: t.toStatusId,
                      hasScreen: t.hasScreen,
                      screenFields: t.screenFields,
                    });
                    setScreenInput({});
                    setTransitionComment('');
                  }}
                >
                  {t.name}
                  {t.hasScreen ? ' …' : ''}
                </button>
              ))
            )}
            {pendingTransition && (
              <TransitionScreenForm
                transition={pendingTransition}
                comment={transitionComment}
                onCommentChange={setTransitionComment}
                screenInput={screenInput}
                onScreenInputChange={setScreenInput}
                onConfirm={confirmTransition}
                onCancel={() => {
                  setPendingTransition(null);
                  setScreenInput({});
                }}
                isSubmitting={transitionMutation.isPending}
              />
            )}
          </div>
        )}
      </div>

      {/* Two Column Layout - Systems and Avionics Style */}
      <div className="idc-issue-body">
        {/* ========== LEFT PRIMARY CONTENT COLUMN ========== */}
        <div className="idc-left-col">

          {/* Description */}
          <div className="idc-section">
            <div className="idc-section-header">
              <h3>Description</h3>
              <button className="idc-section-btn">Edit</button>
            </div>
            <div className="idc-description">
              {issue.description ? (
                <div className="idc-description-text" dangerouslySetInnerHTML={{ __html: issue.description }} />
              ) : (
                <span className="idc-description-placeholder">
                  Click to add description...
                </span>
              )}
            </div>
          </div>

          {/* Environment */}
          {issue.environment && (
            <div className="idc-section">
              <div className="idc-section-header">
                <h3>Environment</h3>
              </div>
              <div className="idc-environment">
                <p>{issue.environment}</p>
              </div>
            </div>
          )}

          {/* Activity Tabs */}
          <div className="idc-tabs">
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
              className={`idc-tab ${activeTab === 'details' ? 'idc-tab-active' : ''}`}
              onClick={() => setActiveTab('details')}
            >
              Details
            </button>
          </div>

          <div className="idc-tab-content">
            {/* Comments Tab */}
            {activeTab === 'comment' && (
              <div className="idc-comment-section">
                <div className="idc-comment-list">
                  {comments?.map((c: any) => (
                    <div key={c.id} className="idc-comment">
                      <div className="idc-comment-avatar">
                        {c.authorName?.charAt(0) || 'U'}
                      </div>
                      <div className="idc-comment-body">
                        <div className="idc-comment-header">
                          <span className="idc-comment-author">{c.authorName}</span>
                          <span className="idc-comment-time">{getRelativeTime(c.createdAt)}</span>
                        </div>
                        <p className="idc-comment-text">{c.content}</p>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="idc-comment-input">
                  <textarea
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    placeholder="Add a comment..."
                    className="idc-comment-textarea"
                  />
                  <button
                    className="idc-btn idc-btn-primary"
                    onClick={() => {
                      if (newComment.trim()) {
                        addCommentMutation.mutate(newComment);
                      }
                    }}
                    disabled={addCommentMutation.isPending}
                  >
                    Save
                  </button>
                </div>
              </div>
            )}

            {/* Activity Tab (Change History) */}
            {activeTab === 'activity' && (
              <div className="idc-activity-section">
                <p className="idc-no-content">No activity recorded yet.</p>
              </div>
            )}

            {/* Work Log Tab */}
            {activeTab === 'work' && (
              <div className="idc-work-section">
                <div className="idc-worklog-list">
                  <div className="idc-worklog-summary">
                    <span>Remaining Estimate: {formatTimeWithDays(issue.remainingEstimate)}</span>
                    <span>Time Spent: {formatTimeWithDays(issue.timeSpent)}</span>
                  </div>
                </div>
                <button className="idc-btn idc-btn-secondary">Log Work</button>
              </div>
            )}

            {/* Details Tab - Field Mappings */}
            {activeTab === 'details' && (
              <div className="idc-details-section">
                <div className="idc-details-grid">
                  <div className="idc-detail-item">
                    <span className="idc-detail-label">Type</span>
                    <span className="idc-detail-value">
                      <span className="idc-type-icon-sm">{getTypeIcon(issue.issueType)}</span>
                      {issue.issueType}
                    </span>
                  </div>
                  <div className="idc-detail-item">
                    <span className="idc-detail-label">Priority</span>
                    <span className="idc-detail-value" style={{ color: getPriorityColor(issue.priority) }}>
                      {issue.priority}
                    </span>
                  </div>
                  <div className="idc-detail-item">
                    <span className="idc-detail-label">Status</span>
                    <span className="idc-detail-value">{issue.status}</span>
                  </div>
                  <div className="idc-detail-item">
                    <span className="idc-detail-label">Resolution</span>
                    <span className="idc-detail-value">{issue.resolutionName || '-'}</span>
                  </div>
                  <div className="idc-detail-item">
                    <span className="idc-detail-label">Affects Version</span>
                    <span className="idc-detail-value">
                      {issue.affectsVersions?.length ? issue.affectsVersions.join(', ') : '-'}
                    </span>
                  </div>
                  <div className="idc-detail-item">
                    <span className="idc-detail-label">Fix Version</span>
                    <span className="idc-detail-value">
                      {issue.fixVersions?.length ? issue.fixVersions.join(', ') : '-'}
                    </span>
                  </div>
                  <div className="idc-detail-item">
                    <span className="idc-detail-label">Components</span>
                    <span className="idc-detail-value">
                      {issue.components?.length ? issue.components.join(', ') : '-'}
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Subtasks Section */}
          {issue.subtasks && issue.subtasks.length > 0 && (
            <div className="idc-section">
              <div className="idc-section-header">
                <h3>Subtasks ({issue.subtasks.length})</h3>
              </div>
              <div className="idc-subtask-list">
                {issue.subtasks.map((subtask: any) => (
                  <Link key={subtask.id} to={`/issues/${subtask.id}`} className="idc-subtask-item">
                    <span className="idc-subtask-status" style={getStatusStyle(subtask.status)}>
                      {subtask.status}
                    </span>
                    <span className="idc-subtask-key">{subtask.issueKey}</span>
                    <span className="idc-subtask-title">{subtask.title}</span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* ========== RIGHT METADATA SIDEBAR ========== */}
        <div className="idc-right-col">

          {/* PEOPLE Section */}
          <div className="idc-sidebar-section">
            <h4 className="idc-sidebar-section-title">People</h4>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Assignee</span>
              <div className="idc-sidebar-value">
                {issue.assigneeId ? (
                  <div className="idc-user-chip">
                    <span className="idc-user-avatar-sm">{issue.assigneeName?.charAt(0) || 'U'}</span>
                    <span>{issue.assigneeName}</span>
                  </div>
                ) : (
                  <span className="idc-no-value">Unassigned</span>
                )}
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Reporter</span>
              <div className="idc-sidebar-value">
                <div className="idc-user-chip">
                  <span className="idc-user-avatar-sm idc-avatar-green">{issue.reporterName?.charAt(0) || 'U'}</span>
                  <span>{issue.reporterName}</span>
                </div>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Votes</span>
              <div className="idc-sidebar-value">
                <span className="idc-vote-count">
                  {issue.voteCount || 0}
                  <button className="idc-vote-btn">Vote for this issue</button>
                </span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Watchers</span>
              <div className="idc-sidebar-value">
                <span>{issue.watcherCount || 0}</span>
              </div>
            </div>
          </div>

          {/* DETAILS Section */}
          <div className="idc-sidebar-section">
            <h4 className="idc-sidebar-section-title">Details</h4>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Priority</span>
              <div className="idc-sidebar-value">
                <span style={{ color: getPriorityColor(issue.priority) }}>{issue.priority}</span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Resolution</span>
              <div className="idc-sidebar-value">
                {issue.resolutionName ? (
                  <span>{issue.resolutionName}</span>
                ) : (
                  <span className="idc-no-value">Unresolved</span>
                )}
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Components</span>
              <div className="idc-sidebar-value">
                {issue.components?.length ? (
                  <div className="idc-components">
                    {issue.components.map(c => (
                      <span key={c} className="idc-component-tag">{c}</span>
                    ))}
                  </div>
                ) : (
                  <span className="idc-no-value">None</span>
                )}
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Labels</span>
              <div className="idc-sidebar-value">
                {issue.labels?.length ? (
                  <div className="idc-labels">
                    {issue.labels.map(l => (
                      <span key={l} className="idc-label">{l}</span>
                    ))}
                  </div>
                ) : (
                  <span className="idc-no-value">None</span>
                )}
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Security</span>
              <div className="idc-sidebar-value">
                {issue.securityLevelName ? (
                  <span>{issue.securityLevelName}</span>
                ) : (
                  <span className="idc-no-value">None</span>
                )}
              </div>
            </div>
          </div>

          {/* TIME TRACKING Section */}
          <div className="idc-sidebar-section">
            <h4 className="idc-sidebar-section-title">Time Tracking</h4>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Original Estimate</span>
              <div className="idc-sidebar-value">
                <span>{formatTimeWithDays(issue.originalEstimate)}</span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Remaining Estimate</span>
              <div className="idc-sidebar-value">
                <span>{formatTimeWithDays(issue.remainingEstimate)}</span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Time Spent</span>
              <div className="idc-sidebar-value">
                <span>{formatTimeWithDays(issue.timeSpent)}</span>
              </div>
            </div>
            {issue.workRatio !== undefined && (
              <div className="idc-sidebar-item">
                <span className="idc-sidebar-label">Work Ratio</span>
                <div className="idc-sidebar-value">
                  <span>{issue.workRatio}%</span>
                </div>
              </div>
            )}
          </div>

          {/* AGILE Section */}
          <div className="idc-sidebar-section">
            <h4 className="idc-sidebar-section-title">Agile</h4>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Sprint</span>
              <div className="idc-sidebar-value">
                {issue.sprintName ? (
                  <span className="idc-sprint-tag">{issue.sprintName}</span>
                ) : (
                  <span className="idc-no-value">Backlog</span>
                )}
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Story Points</span>
              <div className="idc-sidebar-value">
                {issue.storyPoints !== undefined ? (
                  <span className="idc-story-points">{issue.storyPoints}</span>
                ) : (
                  <span className="idc-no-value">None</span>
                )}
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Epic Link</span>
              <div className="idc-sidebar-value">
                {issue.epicId ? (
                  <Link to={`/issues/${issue.epicId}`} className="idc-epic-link">
                    <span className="idc-epic-icon" style={{ color: issue.epicColor }}>⚡</span>
                    <span>{issue.epicName}</span>
                  </Link>
                ) : (
                  <span className="idc-no-value">None</span>
                )}
              </div>
            </div>
            {issue.teamName && (
              <div className="idc-sidebar-item">
                <span className="idc-sidebar-label">Team</span>
                <div className="idc-sidebar-value">
                  <span>{issue.teamName}</span>
                </div>
              </div>
            )}
          </div>

          {/* DATES Section */}
          <div className="idc-sidebar-section">
            <h4 className="idc-sidebar-section-title">Dates</h4>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Created</span>
              <div className="idc-sidebar-value">
                <span>{formatDateTime(issue.createdAt)}</span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Updated</span>
              <div className="idc-sidebar-value">
                <span>{formatDateTime(issue.updatedAt)}</span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Resolved</span>
              <div className="idc-sidebar-value">
                <span>{issue.resolvedAt ? formatDateTime(issue.resolvedAt) : '-'}</span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Due Date</span>
              <div className="idc-sidebar-value">
                <span>{issue.dueDate ? formatDate(issue.dueDate) : '-'}</span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Last Viewed</span>
              <div className="idc-sidebar-value">
                <span>{issue.lastViewedAt ? formatDateTime(issue.lastViewedAt) : '-'}</span>
              </div>
            </div>
          </div>

          {/* VERSIONS Section */}
          <div className="idc-sidebar-section">
            <h4 className="idc-sidebar-section-title">Versions</h4>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Affects Version/s</span>
              <div className="idc-sidebar-value">
                {issue.affectsVersions?.length ? (
                  <div className="idc-version-list">
                    {issue.affectsVersions.map(v => (
                      <span key={v} className="idc-version-tag">{v}</span>
                    ))}
                  </div>
                ) : (
                  <span className="idc-no-value">None</span>
                )}
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Fix Version/s</span>
              <div className="idc-sidebar-value">
                {issue.fixVersions?.length ? (
                  <div className="idc-version-list">
                    {issue.fixVersions.map(v => (
                      <span key={v} className="idc-version-tag">{v}</span>
                    ))}
                  </div>
                ) : (
                  <span className="idc-no-value">None</span>
                )}
              </div>
            </div>
          </div>

          {/* Linked Issues */}
          {issue.linkedIssues && issue.linkedIssues.length > 0 && (
            <div className="idc-sidebar-section">
              <h4 className="idc-sidebar-section-title">Linked Issues</h4>
              <div className="idc-linked-list">
                {issue.linkedIssues.map((link, idx) => (
                  <div key={idx} className="idc-linked-item">
                    <span className="idc-linked-type">{link.type}</span>
                    <Link to={`/issues/${link.key}`} className="idc-linked-key">{link.key}</Link>
                    <span className="idc-linked-title">{link.title}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Edit Modal */}
      {showEditModal && (
        <EditIssueModal
          issue={issue}
          onClose={() => setShowEditModal(false)}
          onSave={() => {
            queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
            setShowEditModal(false);
          }}
        />
      )}
    </div>
  );
}