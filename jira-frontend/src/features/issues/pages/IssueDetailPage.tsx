import { useParams, Link, useNavigate, useOutletContext } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { commentApi } from '../../../api/commentApi';
import { fieldApi } from '../../../api/fieldApi';
import EditIssueModal from '../components/EditIssueModal';
import CreateIssueModal from '../components/CreateIssueModal';
import TransitionScreenForm, { type AvailableTransition } from '../components/TransitionScreenForm';
import ActivityTab from '../components/ActivityTab';
import WorklogsTab from '../components/WorklogsTab';
import IssueLinksTab from '../components/IssueLinksTab';
import LabelsTab from '../components/LabelsTab';
import AttachmentsTab from '../components/AttachmentsTab';
import IssueCustomFieldsPanel from '../components/IssueCustomFieldsPanel';
import IssueMoveModal from '../components/IssueMoveModal';
import IssueAdminMenu from '../components/IssueAdminMenu';
import { useAuth } from '../../auth/context/AuthContext';
import { rankForBottom, rankForTop } from '../utils/issueRank';
import './IssueDetailPage.css';
import '../styles/issues-layout.css';
import '../components/IssueCustomFieldsPanel.css';

type TabType =
  | 'comment'
  | 'activity'
  | 'work'
  | 'links'
  | 'labels'
  | 'attachments'
  | 'details';

/**
 * Full Issue Response - All Systems and Avionics Mandatory Fields
 */
interface FullIssueResponse extends Omit<IssueResponse, 'watchers'> {
  // Override watchers to accept both formats
  watchers?: string[] | Array<{ id: string; name: string; avatar: string }>;

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
  affectsVersionNames?: string[];
  fixVersions?: string[];
  fixVersionNames?: string[];

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
  linkedIssues?: Array<{ type: string; key: string; title: string }>;
  subtasks?: IssueResponse[];
  parent?: { id: string; key: string; title: string };

  // Custom Fields (dynamic)
  customFields?: Record<string, any>;
}

export interface IssueDetailPageProps {
  issueIdOverride?: string;
  embedded?: boolean;
  onClose?: () => void;
}

export default function IssueDetailPage(props?: IssueDetailPageProps) {
  const outletContext = useOutletContext<{ embedded?: boolean }>() ?? {};
  const embedded = props?.embedded ?? outletContext?.embedded ?? false;
  const { issueId: routeIssueId } = useParams<{ issueId: string }>();
  const issueId = props?.issueIdOverride ?? routeIssueId;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuth();

  const [activeTab, setActiveTab] = useState<TabType>('comment');
  const [showEditModal, setShowEditModal] = useState(false);
  const [showCreateSubtask, setShowCreateSubtask] = useState(false);
  const [showMoveModal, setShowMoveModal] = useState(false);
  const [showTransitionMenu, setShowTransitionMenu] = useState(false);
  const [showMoreMenu, setShowMoreMenu] = useState(false);
  const [newComment, setNewComment] = useState('');
  const [editingDescription, setEditingDescription] = useState(false);
  const [descriptionDraft, setDescriptionDraft] = useState('');

  const { data: issue, isLoading } = useQuery({
    queryKey: ['issue', issueId],
    queryFn: async () => {
      const response = await issueApi.getById(issueId!);
      return response.data as FullIssueResponse;
    },
    enabled: !!issueId,
  });

  const resolvedIssueUuid = issue?.id ?? (issueId?.match(/^[0-9a-f-]{36}$/i) ? issueId : undefined);

  const { data: visibleCustomFields } = useQuery({
    queryKey: ['issue-visible-fields-count', resolvedIssueUuid, issue?.projectId, issue?.issueTypeId],
    queryFn: () =>
      fieldApi
        .getVisibleIssueFields(resolvedIssueUuid!, {
          screen: 'VIEW',
          projectId: issue?.projectId,
          issueTypeId: issue?.issueTypeId,
        })
        .then((r) => r.data),
    enabled: !!resolvedIssueUuid,
  });

  const customFieldsWithValues =
    visibleCustomFields?.fields?.filter(
      (f) => f.value != null && String(f.value).trim() !== '',
    ).length ?? 0;

  const { data: comments } = useQuery({
    queryKey: ['comments', issueId],
    queryFn: async () => {
      const response = await commentApi.getByIssue(issueId!);
      return Array.isArray(response.data) ? response.data : [];
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
  const [transitionError, setTransitionError] = useState<string | null>(null);

  const [commentError, setCommentError] = useState<string | null>(null);

  const saveDescriptionMutation = useMutation({
    mutationFn: (description: string) => issueApi.update(issueId!, { description }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
      setEditingDescription(false);
    },
  });

  const addCommentMutation = useMutation({
    mutationFn: (content: string) => commentApi.create({ issueId: issueId!, content }),
    onSuccess: () => {
      setCommentError(null);
      queryClient.invalidateQueries({ queryKey: ['comments', issueId] });
      setNewComment('');
    },
    onError: (err: unknown) => {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      if (status === 401) {
        setCommentError('Not signed in. Log in again so comments can be saved.');
      } else {
        setCommentError(msg || 'Failed to add comment. Is the comment service running?');
      }
    },
  });

  const isWatching = Boolean(
    user?.userId && issue?.watchers?.includes(user.userId),
  );
  const hasVoted = (issue?.votes ?? 0) > 0;

  const voteMutation = useMutation({
    mutationFn: () => issueApi.vote(issueId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['issue', issueId] }),
  });

  const unvoteMutation = useMutation({
    mutationFn: () => issueApi.unvote(issueId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['issue', issueId] }),
  });

  const watchMutation = useMutation({
    mutationFn: () => issueApi.watch(issueId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['issue', issueId] }),
  });

  const unwatchMutation = useMutation({
    mutationFn: () => issueApi.unwatch(issueId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['issue', issueId] }),
  });

  const rankMutation = useMutation({
    mutationFn: (rank: string) => issueApi.update(issueId!, { rank }),
    onSuccess: () => {
      setShowMoreMenu(false);
      queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
    },
  });

  const cloneMutation = useMutation({
    mutationFn: () => issueApi.clone(issueId!, { projectId: issue?.projectId }),
    onSuccess: (res) => {
      setShowMoreMenu(false);
      if (embedded && props?.onClose) {
        window.location.assign(`/issues/${res.data.id}`);
      } else {
        navigate(`/issues/${res.data.id}`);
      }
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => issueApi.delete(issueId!),
    onSuccess: () => {
      setShowMoreMenu(false);
      if (embedded && props?.onClose) {
        props.onClose();
      } else {
        navigate('/issues');
      }
    },
  });

  const moveMutation = useMutation({
    mutationFn: (targetProjectId: string) => issueApi.move(issueId!, { projectId: targetProjectId }),
    onSuccess: (res) => {
      setShowMoveModal(false);
      setShowMoreMenu(false);
      queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
      navigate(`/issues/${res.data.id}`);
    },
  });

  const transitionMutation = useMutation({
    mutationFn: async (payload: {
      transitionId: string;
      toStatusId?: string;
      comment?: string;
      resolutionId?: string;
      screenInput?: Record<string, unknown>;
    }) => {
      try {
        const res = await issueApi.executeTransition({
          issueId: issueId!,
          projectId: issue!.projectId,
          transitionId: payload.transitionId,
          comment: payload.comment,
          resolutionId: payload.resolutionId,
          screenInput: payload.screenInput,
        });
        if (res.data && (res.data as { success?: boolean }).success !== false) {
          return res;
        }
      } catch {
        // workflow engine unavailable — fall through to direct PATCH
      }
      if (payload.toStatusId) {
        return issueApi.transitionStatus(issueId!, issue!.projectId, {
          statusId: payload.toStatusId,
          transitionId: payload.transitionId,
          comment: payload.comment,
          resolutionId: payload.resolutionId,
        });
      }
      throw new Error('Transition failed');
    },
    onSuccess: () => {
      setTransitionError(null);
      queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issue-transitions', issueId] });
      setShowTransitionMenu(false);
      setPendingTransition(null);
      setTransitionComment('');
      setScreenInput({});
    },
    onError: (err: {
      response?: { status?: number; data?: { message?: string; validationErrors?: Record<string, string> } };
    }) => {
      if (err?.response?.status === 409) {
        setTransitionError('This issue was updated elsewhere. Refresh the page and try again.');
        queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
        queryClient.invalidateQueries({ queryKey: ['issue-transitions', issueId] });
        return;
      }
      const fieldErrors = err?.response?.data?.validationErrors;
      const parts: string[] = [];
      if (err?.response?.data?.message) parts.push(err.response.data.message);
      if (fieldErrors && typeof fieldErrors === 'object') {
        parts.push(...Object.values(fieldErrors));
      }
      setTransitionError(parts.length > 0 ? parts.join(' ') : 'Transition failed');
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
      case 'in progress': case 'in_review':
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
    <div className={`idc-issue-view ${embedded ? 'idc-issue-view-embedded' : ''}`}>
      {!embedded && (
      <div className="idc-breadcrumb">
        <Link to="/issues" className="idc-breadcrumb-project">Issues</Link>
        <span className="idc-breadcrumb-sep">/</span>
        <Link to={`/projects/${issue.projectId}`} className="idc-breadcrumb-project">
          {issue.projectName || issue.projectKey || 'Project'}
        </Link>
        <span className="idc-breadcrumb-sep">/</span>
        <span className="idc-breadcrumb-key">{issue.issueKey}</span>
      </div>
      )}

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
            <IssueAdminMenu projectId={issue?.projectId} issueKey={issue?.issueKey} />
            <div className="idc-dropdown-wrapper">
              <button className="idc-action-btn" onClick={() => setShowMoreMenu(!showMoreMenu)}>
                More <span className="idc-dropdown-caret">▾</span>
              </button>
              {showMoreMenu && (
                <div className="idc-dropdown-menu">
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    disabled={isWatching ? unwatchMutation.isPending : watchMutation.isPending}
                    onClick={() => {
                      if (isWatching) unwatchMutation.mutate();
                      else watchMutation.mutate();
                      setShowMoreMenu(false);
                    }}
                  >
                    {isWatching ? 'Stop watching' : 'Watch issue'}
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    disabled={hasVoted ? unvoteMutation.isPending : voteMutation.isPending}
                    onClick={() => {
                      if (hasVoted) unvoteMutation.mutate();
                      else voteMutation.mutate();
                      setShowMoreMenu(false);
                    }}
                  >
                    {hasVoted ? 'Remove vote' : 'Vote for issue'}
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    disabled={rankMutation.isPending}
                    onClick={() => rankMutation.mutate(rankForTop())}
                  >
                    Rank to top
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    disabled={rankMutation.isPending}
                    onClick={() => rankMutation.mutate(rankForBottom())}
                  >
                    Rank to bottom
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    onClick={() => {
                      setActiveTab('work');
                      setShowMoreMenu(false);
                    }}
                  >
                    Log work
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    onClick={() => {
                      setActiveTab('links');
                      setShowMoreMenu(false);
                    }}
                  >
                    Link issues
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    onClick={() => {
                      setShowCreateSubtask(true);
                      setShowMoreMenu(false);
                    }}
                  >
                    Create subtask
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    disabled={cloneMutation.isPending}
                    onClick={() => cloneMutation.mutate()}
                  >
                    {cloneMutation.isPending ? 'Cloning…' : 'Clone issue'}
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    onClick={() => {
                      setShowMoveModal(true);
                      setShowMoreMenu(false);
                    }}
                  >
                    Move
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    onClick={() => {
                      const url = window.location.href;
                      void navigator.clipboard?.writeText(url);
                      setShowMoreMenu(false);
                    }}
                  >
                    Share issue (copy link)
                  </button>
                  <button
                    type="button"
                    className="idc-dropdown-item"
                    onClick={() => {
                      window.open(`/api/issues/${issueId}`, '_blank', 'noopener');
                      setShowMoreMenu(false);
                    }}
                  >
                    Export (API view)
                  </button>
                  {issue?.projectId && (
                    <button
                      type="button"
                      className="idc-dropdown-item"
                      onClick={() => {
                        setShowMoreMenu(false);
                        navigate(
                          `/projects/${issue.projectId}/board/active?issueId=${issueId}`,
                        );
                      }}
                    >
                      Find on board
                    </button>
                  )}
                  {embedded && (
                    <button
                      type="button"
                      className="idc-dropdown-item"
                      onClick={() => {
                        setShowMoreMenu(false);
                        navigate(`/issues/${issueId}`);
                      }}
                    >
                      Open in full view
                    </button>
                  )}
                  <div className="idc-dropdown-divider"></div>
                  <button
                    type="button"
                    className="idc-dropdown-item idc-dropdown-danger"
                    disabled={deleteMutation.isPending}
                    onClick={() => {
                      if (window.confirm('Delete this issue permanently?')) {
                        deleteMutation.mutate();
                      }
                    }}
                  >
                    Delete
                  </button>
                </div>
              )}
            </div>
            <button
              className="idc-status-transition-btn"
              onClick={() => setShowTransitionMenu(!showTransitionMenu)}
            >
              <span>{issue.status || 'Transition'}</span>
              <span className="idc-dropdown-caret">▾</span>
            </button>
          </div>
        </div>

        {/* Title */}
        <h1 className="idc-issue-title">{issue.title}</h1>

        {/* Meta info */}
        <div className="idc-issue-meta">
          <span className="idc-meta-key">{issue.issueKey}</span>
          <span className="idc-meta-sep"> · </span>
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
            {transitionError && (
              <p className="idc-transition-error" role="alert" style={{ color: '#de350b', padding: '8px 12px', fontSize: 13 }}>
                {transitionError}
              </p>
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
                  setTransitionError(null);
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
              {!editingDescription && (
                <button
                  className="idc-section-btn"
                  onClick={() => {
                    setDescriptionDraft(issue.description || '');
                    setEditingDescription(true);
                  }}
                >
                  Edit
                </button>
              )}
            </div>
            <div className="idc-description">
              {editingDescription ? (
                <div>
                  <textarea
                    className="idc-description-textarea"
                    value={descriptionDraft}
                    onChange={(e) => setDescriptionDraft(e.target.value)}
                    rows={6}
                    autoFocus
                    placeholder="Add a description..."
                    style={{ width: '100%', padding: '8px 12px', fontSize: '14px', border: '2px solid #0052cc', borderRadius: '4px', resize: 'vertical', fontFamily: 'inherit' }}
                  />
                  <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                    <button
                      className="idc-section-btn"
                      style={{ background: '#0052cc', color: '#fff', padding: '6px 16px', borderRadius: '4px', border: 'none', cursor: 'pointer' }}
                      disabled={saveDescriptionMutation.isPending}
                      onClick={() => saveDescriptionMutation.mutate(descriptionDraft)}
                    >
                      {saveDescriptionMutation.isPending ? 'Saving...' : 'Save'}
                    </button>
                    <button
                      className="idc-section-btn"
                      style={{ padding: '6px 16px', borderRadius: '4px', border: '1px solid #ddd', background: '#fff', cursor: 'pointer' }}
                      onClick={() => setEditingDescription(false)}
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <div
                  onClick={() => {
                    setDescriptionDraft(issue.description || '');
                    setEditingDescription(true);
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  {issue.description ? (
                    <div className="idc-description-text" dangerouslySetInnerHTML={{ __html: issue.description }} />
                  ) : (
                    <span className="idc-description-placeholder">
                      Click to add description...
                    </span>
                  )}
                </div>
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

          {/* Activity Tabs — single panel, no nested borders */}
          <div className="idc-tabs-panel">
          <div className="idc-tabs" role="tablist" aria-label="Issue panels">
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'comment'}
              className={`idc-tab ${activeTab === 'comment' ? 'idc-tab-active' : ''}`}
              onClick={() => setActiveTab('comment')}
            >
              Comment
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'activity'}
              className={`idc-tab ${activeTab === 'activity' ? 'idc-tab-active' : ''}`}
              onClick={() => setActiveTab('activity')}
            >
              Activity
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'work'}
              className={`idc-tab ${activeTab === 'work' ? 'idc-tab-active' : ''}`}
              onClick={() => setActiveTab('work')}
            >
              Work log
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'links'}
              className={`idc-tab ${activeTab === 'links' ? 'idc-tab-active' : ''}`}
              onClick={() => setActiveTab('links')}
            >
              Links
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'labels'}
              className={`idc-tab ${activeTab === 'labels' ? 'idc-tab-active' : ''}`}
              onClick={() => setActiveTab('labels')}
            >
              Labels
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'attachments'}
              className={`idc-tab ${activeTab === 'attachments' ? 'idc-tab-active' : ''}`}
              onClick={() => setActiveTab('attachments')}
            >
              Attachments
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={activeTab === 'details'}
              className={`idc-tab ${activeTab === 'details' ? 'idc-tab-active' : ''}`}
              onClick={() => setActiveTab('details')}
            >
              Details
              {customFieldsWithValues > 0 && (
                <span className="icf-tab-badge" title="Custom fields with values">
                  {customFieldsWithValues}
                </span>
              )}
            </button>
          </div>

          <div className="idc-tab-content" role="tabpanel">
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
                  {commentError && (
                    <p className="idc-comment-error" style={{ color: '#de350b', marginBottom: 8 }}>
                      {commentError}
                    </p>
                  )}
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

            {/* Activity Tab (Change History + Transitions) */}
            {activeTab === 'activity' && issueId && (
              <div className="idc-activity-section">
                <ActivityTab issueId={issueId} />
              </div>
            )}

            {/* Work Log Tab */}
            {activeTab === 'work' && issueId && (
              <div className="idc-work-section">
                <WorklogsTab issueId={issueId} />
              </div>
            )}

            {activeTab === 'links' && issueId && (
              <div className="idc-links-section">
                <IssueLinksTab issueId={issueId} />
              </div>
            )}

            {activeTab === 'labels' && issueId && (
              <div className="idc-labels-section">
                <LabelsTab issueId={issueId} />
              </div>
            )}

            {activeTab === 'attachments' && issueId && (
              <div className="idc-attachments-section">
                <AttachmentsTab issueId={issueId} />
              </div>
            )}

            {/* Details Tab - Field Mappings */}
            {activeTab === 'details' && (
              <div className="idc-details-section">
                {resolvedIssueUuid && (
                  <div className="mb-6 idc-custom-fields-block">
                    <h4 className="text-sm font-semibold text-gray-700 mb-2">Custom fields</h4>
                    <p className="icf-hint mb-2">
                      Migrated and admin-defined fields (Epic Name, Parent Link, Target dates, etc.)
                    </p>
                    <IssueCustomFieldsPanel
                      issueId={resolvedIssueUuid}
                      issueKey={issue?.issueKey}
                      projectId={issue?.projectId}
                      issueTypeId={issue?.issueTypeId}
                      variant="inline"
                    />
                  </div>
                )}
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
                      {issue.affectsVersionNames?.length ? issue.affectsVersionNames.join(', ') : issue.affectsVersions?.length ? issue.affectsVersions.join(', ') : '-'}
                    </span>
                  </div>
                  <div className="idc-detail-item">
                    <span className="idc-detail-label">Fix Version</span>
                    <span className="idc-detail-value">
                      {issue.fixVersionNames?.length ? issue.fixVersionNames.join(', ') : issue.fixVersions?.length ? issue.fixVersions.join(', ') : '-'}
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
                  <button
                    type="button"
                    className="idc-vote-btn"
                    onClick={() => voteMutation.mutate()}
                    disabled={voteMutation.isPending}
                  >
                    {voteMutation.isPending ? 'Voting…' : 'Vote for this issue'}
                  </button>
                </span>
              </div>
            </div>
            <div className="idc-sidebar-item">
              <span className="idc-sidebar-label">Watchers</span>
              <div className="idc-sidebar-value">
                <span>{issue.watcherCount || 0}</span>
                <button
                  type="button"
                  className="idc-vote-btn"
                  style={{ marginLeft: 8 }}
                  onClick={() => watchMutation.mutate()}
                  disabled={watchMutation.isPending || unwatchMutation.isPending}
                >
                  Watch
                </button>
                <button
                  type="button"
                  className="idc-vote-btn"
                  style={{ marginLeft: 4 }}
                  onClick={() => unwatchMutation.mutate()}
                  disabled={watchMutation.isPending || unwatchMutation.isPending}
                >
                  Unwatch
                </button>
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

          {resolvedIssueUuid && (
            <div className="idc-sidebar-section">
              <h4 className="idc-sidebar-section-title">
                Custom fields
                {customFieldsWithValues > 0 && (
                  <span className="icf-tab-badge" style={{ marginLeft: 6 }}>
                    {customFieldsWithValues}
                  </span>
                )}
              </h4>
              <IssueCustomFieldsPanel
                issueId={resolvedIssueUuid}
                issueKey={issue.issueKey}
                projectId={issue.projectId}
                issueTypeId={issue.issueTypeId}
                variant="sidebar"
              />
              <button
                type="button"
                className="icf-view-all-btn"
                onClick={() => setActiveTab('details')}
              >
                View all in Details tab →
              </button>
            </div>
          )}

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
                  <Link to={`/epics/${issue.epicId}`} className="idc-epic-link">
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
                {(issue.affectsVersionNames ?? issue.affectsVersions)?.length ? (
                  <div className="idc-version-list">
                    {(issue.affectsVersionNames ?? issue.affectsVersions ?? []).map((v, i) => (
                      <span key={i} className="idc-version-tag">{v}</span>
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
                {(issue.fixVersionNames ?? issue.fixVersions)?.length ? (
                  <div className="idc-version-list">
                    {(issue.fixVersionNames ?? issue.fixVersions ?? []).map((v, i) => (
                      <span key={i} className="idc-version-tag">{v}</span>
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
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
            setShowEditModal(false);
          }}
        />
      )}

      {showCreateSubtask && issue && (
        <CreateIssueModal
          projectId={issue.projectId}
          projectKey={issue.projectKey}
          parentIssueId={issueId}
          defaultTitle={`Subtask of ${issue.issueKey}`}
          onClose={() => setShowCreateSubtask(false)}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
            setShowCreateSubtask(false);
          }}
        />
      )}

      {showMoveModal && issue && (
        <IssueMoveModal
          currentProjectId={issue.projectId}
          onClose={() => setShowMoveModal(false)}
          onMove={(targetProjectId) => moveMutation.mutate(targetProjectId)}
          isPending={moveMutation.isPending}
        />
      )}
    </div>
  );
}