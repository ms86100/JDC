import { useParams, Link, useNavigate, useOutletContext } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect, useRef } from 'react';
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
import { useFieldBehaviors } from '../../../hooks/useFieldBehaviors';
import { scriptApi } from '../../../api/scriptApi';
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

interface FullIssueResponse extends Omit<IssueResponse, 'watchers'> {
  watchers?: string[] | Array<{ id: string; name: string; avatar: string }>;
  projectName?: string;
  issueKey: string;
  issueType: string;
  issueTypeIcon?: string;
  issueTypeColor?: string;
  status: string;
  statusCategory?: string;
  priority: string;
  priorityIcon?: string;
  priorityColor?: string;
  resolutionId?: string;
  resolutionName?: string;
  resolutionDate?: string;
  projectId: string;
  projectKey: string;
  assigneeId?: string;
  assigneeName?: string;
  assigneeAvatar?: string;
  reporterId?: string;
  reporterName?: string;
  reporterAvatar?: string;
  creatorId?: string;
  creatorName?: string;
  createdAt: string;
  updatedAt: string;
  lastViewedAt?: string;
  resolvedAt?: string;
  dueDate?: string;
  title: string;
  description?: string;
  environment?: string;
  affectsVersions?: string[];
  affectsVersionNames?: string[];
  fixVersions?: string[];
  fixVersionNames?: string[];
  components?: string[];
  labels?: string[];
  sprintId?: string;
  sprintName?: string;
  teamId?: string;
  teamName?: string;
  epicId?: string;
  epicName?: string;
  epicColor?: string;
  storyPoints?: number;
  originalStoryPoints?: number;
  parentId?: string;
  parentKey?: string;
  originalEstimate?: number;
  remainingEstimate?: number;
  timeSpent?: number;
  aggregateTimeEstimate?: number;
  aggregateTimeSpent?: number;
  aggregateRemainingEstimate?: number;
  workRatio?: number;
  securityLevelId?: string;
  securityLevelName?: string;
  votes?: number;
  voteCount?: number;
  watcherCount?: number;
  linkedIssues?: Array<{ type: string; key: string; title: string }>;
  subtasks?: IssueResponse[];
  parent?: { id: string; key: string; title: string };
  customFields?: Record<string, any>;
}

export interface IssueDetailPageProps {
  issueIdOverride?: string;
  embedded?: boolean;
  onClose?: () => void;
}

const sanitizeHtml = (html: string | undefined | null): string => {
  if (!html) return '';
  return html
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/\bon\w+\s*=\s*(?:"[^"]*"|'[^']*'|[^\s>]*)/gi, '')
    .replace(/javascript\s*:/gi, '');
};

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
  const [drawerOpen, setDrawerOpen] = useState(true);

  const moreMenuRef = useRef<HTMLDivElement>(null);
  const transitionMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (moreMenuRef.current && !moreMenuRef.current.contains(e.target as Node)) {
        setShowMoreMenu(false);
      }
      if (transitionMenuRef.current && !transitionMenuRef.current.contains(e.target as Node)) {
        setShowTransitionMenu(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const { data: issue, isLoading } = useQuery({
    queryKey: ['issue', issueId],
    queryFn: async () => {
      const response = await issueApi.getById(issueId!);
      return response.data as FullIssueResponse;
    },
    enabled: !!issueId,
  });

  const resolvedIssueUuid = issue?.id ?? (issueId?.match(/^[0-9a-f-]{36}$/i) ? issueId : undefined);

  const { isFieldVisible, getFieldWarning, getFieldLabel } = useFieldBehaviors({
    screenContext: 'VIEW',
    projectId: issue?.projectId,
    issueTypeId: issue?.issueTypeId,
    issueData: issue as unknown as Record<string, unknown>,
    enabled: !!issue,
  });

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

  const getStatusClass = (status: string) => {
    switch (status?.toLowerCase()) {
      case 'done': case 'resolved': case 'closed':
        return 'idm-status--done';
      case 'in progress': case 'in_review':
        return 'idm-status--inprogress';
      case 'blocked':
        return 'idm-status--blocked';
      default:
        return 'idm-status--todo';
    }
  };

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

  const getPriorityClass = (priority: string) => {
    switch (priority?.toLowerCase()) {
      case 'highest': case 'critical': return 'idm-priority--critical';
      case 'high': return 'idm-priority--high';
      case 'medium': return 'idm-priority--medium';
      case 'low': return 'idm-priority--low';
      case 'lowest': return 'idm-priority--lowest';
      default: return 'idm-priority--medium';
    }
  };

  const getPriorityIcon = (priority: string) => {
    switch (priority?.toLowerCase()) {
      case 'highest': case 'critical': return '↑↑';
      case 'high': return '↑';
      case 'medium': return '↔';
      case 'low': return '↓';
      case 'lowest': return '↓↓';
      default: return '↔';
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

  const timeProgress = (() => {
    if (!issue?.originalEstimate) return 0;
    const spent = issue.timeSpent ?? 0;
    return Math.min(100, Math.round((spent / issue.originalEstimate) * 100));
  })();

  if (isLoading) {
    return (
      <div className="idm-loading">
        <div className="idm-loading-pulse">
          <div className="idm-pulse-ring" />
          <div className="idm-pulse-dot" />
        </div>
        <span className="idm-loading-text">Loading issue...</span>
      </div>
    );
  }

  if (!issue) {
    return (
      <div className="idm-empty-state">
        <div className="idm-empty-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M9 12h6M12 9v6M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h3>Issue not found</h3>
        <p>The issue you're looking for doesn't exist or has been moved.</p>
        <Link to="/issues" className="idm-btn idm-btn--primary">Back to Issues</Link>
      </div>
    );
  }

  const tabItems: { key: TabType; label: string; icon: string; badge?: number }[] = [
    { key: 'comment', label: 'Comments', icon: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z' },
    { key: 'activity', label: 'Activity', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
    { key: 'work', label: 'Work Log', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
    { key: 'links', label: 'Links', icon: 'M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1' },
    { key: 'labels', label: 'Labels', icon: 'M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z' },
    { key: 'attachments', label: 'Attachments', icon: 'M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13' },
    { key: 'details', label: 'Details', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2', badge: customFieldsWithValues > 0 ? customFieldsWithValues : undefined },
  ];

  return (
    <div className={`idm-root ${embedded ? 'idm-root--embedded' : ''}`}>
      {/* ── HEADER BAR ── */}
      <header className="idm-header">
        <div className="idm-header-top">
          {/* Breadcrumb + Key */}
          <div className="idm-header-left">
            {!embedded && (
              <nav className="idm-breadcrumb" aria-label="Breadcrumb">
                <Link to="/issues" className="idm-breadcrumb-link">Issues</Link>
                <svg className="idm-breadcrumb-sep" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 5l7 7-7 7" /></svg>
                <Link to={`/projects/${issue.projectId}`} className="idm-breadcrumb-link">
                  {issue.projectName || issue.projectKey || 'Project'}
                </Link>
                <svg className="idm-breadcrumb-sep" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 5l7 7-7 7" /></svg>
                <span className="idm-breadcrumb-current">{issue.issueKey}</span>
              </nav>
            )}
            {embedded && (
              <span className="idm-key-badge">{issue.issueKey}</span>
            )}
          </div>

          {/* Action Toolbar */}
          <div className="idm-header-actions">
            <button className="idm-tool-btn" onClick={() => setShowEditModal(true)} title="Edit issue">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>
              <span>Edit</span>
            </button>

            <IssueAdminMenu projectId={issue?.projectId} issueKey={issue?.issueKey} />

            <div className="idm-dropdown-wrapper" ref={moreMenuRef}>
              <button className="idm-tool-btn" onClick={() => setShowMoreMenu(!showMoreMenu)}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /><circle cx="5" cy="12" r="1" /></svg>
              </button>
              {showMoreMenu && (
                <div className="idm-dropdown">
                  <div className="idm-dropdown-section">
                    <button type="button" className="idm-dropdown-item"
                      disabled={isWatching ? unwatchMutation.isPending : watchMutation.isPending}
                      onClick={() => { if (isWatching) unwatchMutation.mutate(); else watchMutation.mutate(); setShowMoreMenu(false); }}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" /></svg>
                      {isWatching ? 'Stop watching' : 'Watch issue'}
                    </button>
                    <button type="button" className="idm-dropdown-item"
                      disabled={hasVoted ? unvoteMutation.isPending : voteMutation.isPending}
                      onClick={() => { if (hasVoted) unvoteMutation.mutate(); else voteMutation.mutate(); setShowMoreMenu(false); }}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14z" /><path d="M7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3" /></svg>
                      {hasVoted ? 'Remove vote' : 'Vote for issue'}
                    </button>
                  </div>
                  <div className="idm-dropdown-divider" />
                  <div className="idm-dropdown-section">
                    <button type="button" className="idm-dropdown-item" disabled={rankMutation.isPending}
                      onClick={() => rankMutation.mutate(rankForTop())}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 19V5M5 12l7-7 7 7" /></svg>
                      Rank to top
                    </button>
                    <button type="button" className="idm-dropdown-item" disabled={rankMutation.isPending}
                      onClick={() => rankMutation.mutate(rankForBottom())}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 5v14M19 12l-7 7-7-7" /></svg>
                      Rank to bottom
                    </button>
                  </div>
                  <div className="idm-dropdown-divider" />
                  <div className="idm-dropdown-section">
                    <button type="button" className="idm-dropdown-item"
                      onClick={() => { setActiveTab('work'); setShowMoreMenu(false); }}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                      Log work
                    </button>
                    <button type="button" className="idm-dropdown-item"
                      onClick={() => { setActiveTab('links'); setShowMoreMenu(false); }}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101" /></svg>
                      Link issues
                    </button>
                    <button type="button" className="idm-dropdown-item"
                      onClick={() => { setShowCreateSubtask(true); setShowMoreMenu(false); }}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 4v16m8-8H4" /></svg>
                      Create subtask
                    </button>
                    <button type="button" className="idm-dropdown-item" disabled={cloneMutation.isPending}
                      onClick={() => cloneMutation.mutate()}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2" /><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1" /></svg>
                      {cloneMutation.isPending ? 'Cloning...' : 'Clone issue'}
                    </button>
                    <button type="button" className="idm-dropdown-item"
                      onClick={() => { setShowMoveModal(true); setShowMoreMenu(false); }}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
                      Move
                    </button>
                    <button type="button" className="idm-dropdown-item"
                      onClick={() => { void navigator.clipboard?.writeText(window.location.href); setShowMoreMenu(false); }}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13" /></svg>
                      Share (copy link)
                    </button>
                    <button type="button" className="idm-dropdown-item"
                      onClick={() => { window.open(`/api/issues/${issueId}`, '_blank', 'noopener'); setShowMoreMenu(false); }}
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6M15 3h6v6M10 14L21 3" /></svg>
                      Export (API view)
                    </button>
                    {issue?.projectId && (
                      <button type="button" className="idm-dropdown-item"
                        onClick={() => { setShowMoreMenu(false); navigate(`/projects/${issue.projectId}/board/active?issueId=${issueId}`); }}
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" /></svg>
                        Find on board
                      </button>
                    )}
                    {embedded && (
                      <button type="button" className="idm-dropdown-item"
                        onClick={() => { setShowMoreMenu(false); navigate(`/issues/${issueId}`); }}
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7" /></svg>
                        Open in full view
                      </button>
                    )}
                  </div>
                  <div className="idm-dropdown-divider" />
                  <button type="button" className="idm-dropdown-item idm-dropdown-item--danger"
                    disabled={deleteMutation.isPending}
                    onClick={() => { if (window.confirm('Delete this issue permanently?')) deleteMutation.mutate(); }}
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                    Delete
                  </button>
                </div>
              )}
            </div>

            <div className="idm-action-divider" />

            {/* Transition Button */}
            <div className="idm-dropdown-wrapper" ref={transitionMenuRef}>
              <button className="idm-transition-btn" onClick={() => setShowTransitionMenu(!showTransitionMenu)}>
                <span>{issue.status || 'Transition'}</span>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 9l6 6 6-6" /></svg>
              </button>
              {showTransitionMenu && (
                <div className="idm-dropdown idm-dropdown--transition">
                  <div className="idm-dropdown-header">Workflow Transitions</div>
                  {(availableTransitions?.transitions ?? []).length === 0 ? (
                    <div className="idm-dropdown-empty">No transitions available</div>
                  ) : (
                    availableTransitions!.transitions.map((t) => (
                      <button key={t.id} type="button" className="idm-dropdown-item"
                        onClick={() => {
                          setPendingTransition({
                            id: t.id, name: t.name, description: t.description,
                            toStatusId: t.toStatusId, hasScreen: t.hasScreen, screenFields: t.screenFields,
                          });
                          setScreenInput({}); setTransitionComment('');
                        }}
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
                        {t.name}{t.hasScreen ? ' ...' : ''}
                      </button>
                    ))
                  )}
                  {transitionError && (
                    <p className="idm-dropdown-error" role="alert">{transitionError}</p>
                  )}
                  {pendingTransition && (
                    <TransitionScreenForm
                      transition={pendingTransition}
                      comment={transitionComment}
                      onCommentChange={setTransitionComment}
                      screenInput={screenInput}
                      onScreenInputChange={setScreenInput}
                      onConfirm={confirmTransition}
                      onCancel={() => { setPendingTransition(null); setScreenInput({}); setTransitionError(null); }}
                      isSubmitting={transitionMutation.isPending}
                    />
                  )}
                </div>
              )}
            </div>

            <div className="idm-action-divider" />

            {/* Drawer Toggle */}
            <button className={`idm-tool-btn idm-tool-btn--drawer ${drawerOpen ? 'idm-tool-btn--active' : ''}`}
              onClick={() => setDrawerOpen(!drawerOpen)} title={drawerOpen ? 'Hide details' : 'Show details'}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="3" width="18" height="18" rx="2" />
                <path d="M15 3v18" />
              </svg>
            </button>
          </div>
        </div>

        {/* Title + Meta */}
        <div className="idm-header-body">
          <div className="idm-badges">
            <span className="idm-type-badge">
              <span className="idm-type-icon">{getTypeIcon(issue.issueType)}</span>
              {issue.issueType || 'Story'}
            </span>
            <span className={`idm-status-lozenge ${getStatusClass(issue.status)}`}>
              {issue.status || 'To Do'}
            </span>
            <span className={`idm-priority-badge ${getPriorityClass(issue.priority)}`}>
              <span className="idm-priority-arrow">{getPriorityIcon(issue.priority)}</span>
              {issue.priority}
            </span>
          </div>
          <h1 className="idm-title">{issue.title}</h1>
          <div className="idm-meta">
            <span className="idm-meta-reporter">
              <span className="idm-meta-avatar">{issue.reporterName?.charAt(0) || 'U'}</span>
              {issue.reporterName || 'Unknown'}
            </span>
            <span className="idm-meta-dot" />
            <span className="idm-meta-time" title={formatDateTime(issue.createdAt)}>
              Created {getRelativeTime(issue.createdAt)}
            </span>
            {issue.updatedAt && issue.updatedAt !== issue.createdAt && (
              <>
                <span className="idm-meta-dot" />
                <span className="idm-meta-time" title={formatDateTime(issue.updatedAt)}>
                  Updated {getRelativeTime(issue.updatedAt)}
                </span>
              </>
            )}
          </div>
        </div>
      </header>

      {/* ── MAIN CONTENT AREA ── */}
      <div className={`idm-body ${drawerOpen ? 'idm-body--drawer-open' : ''}`}>

        {/* ─── LEFT: PRIMARY CONTENT ─── */}
        <main className="idm-main">

          {/* Quick-Glance Bar */}
          <div className="idm-glance-bar">
            <div className="idm-glance-item">
              <span className="idm-glance-label">Assignee</span>
              <span className="idm-glance-value">
                {issue.assigneeId ? (
                  <span className="idm-avatar-chip">
                    <span className="idm-avatar-sm">{issue.assigneeName?.charAt(0) || 'U'}</span>
                    {issue.assigneeName}
                  </span>
                ) : (
                  <span className="idm-no-value">Unassigned</span>
                )}
              </span>
            </div>
            <div className="idm-glance-sep" />
            <div className="idm-glance-item">
              <span className="idm-glance-label">Sprint</span>
              <span className="idm-glance-value">
                {issue.sprintName || <span className="idm-no-value">Backlog</span>}
              </span>
            </div>
            <div className="idm-glance-sep" />
            <div className="idm-glance-item">
              <span className="idm-glance-label">Story Points</span>
              <span className="idm-glance-value">
                {issue.storyPoints !== undefined ? (
                  <span className="idm-sp-badge">{issue.storyPoints}</span>
                ) : (
                  <span className="idm-no-value">-</span>
                )}
              </span>
            </div>
            {issue.dueDate && (
              <>
                <div className="idm-glance-sep" />
                <div className="idm-glance-item">
                  <span className="idm-glance-label">Due Date</span>
                  <span className="idm-glance-value">{formatDate(issue.dueDate)}</span>
                </div>
              </>
            )}
          </div>

          {/* Description Card */}
          <section className="idm-card">
            <div className="idm-card-header">
              <h3 className="idm-card-title">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" /></svg>
                Description
              </h3>
              {!editingDescription && (
                <button className="idm-card-action" onClick={() => { setDescriptionDraft(issue.description || ''); setEditingDescription(true); }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" /></svg>
                  Edit
                </button>
              )}
            </div>
            <div className="idm-card-body">
              {editingDescription ? (
                <div className="idm-description-editor">
                  <textarea
                    className="idm-textarea"
                    value={descriptionDraft}
                    onChange={(e) => setDescriptionDraft(e.target.value)}
                    rows={6}
                    autoFocus
                    placeholder="Add a description..."
                  />
                  <div className="idm-editor-actions">
                    <button className="idm-btn idm-btn--primary" disabled={saveDescriptionMutation.isPending}
                      onClick={() => saveDescriptionMutation.mutate(descriptionDraft)}
                    >
                      {saveDescriptionMutation.isPending ? 'Saving...' : 'Save'}
                    </button>
                    <button className="idm-btn idm-btn--ghost" onClick={() => setEditingDescription(false)}>Cancel</button>
                  </div>
                </div>
              ) : (
                <div className="idm-description-content"
                  onClick={() => { setDescriptionDraft(issue.description || ''); setEditingDescription(true); }}
                >
                  {issue.description ? (
                    <div className="idm-description-text" dangerouslySetInnerHTML={{ __html: sanitizeHtml(issue.description) }} />
                  ) : (
                    <div className="idm-description-placeholder">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 4v16m8-8H4" /></svg>
                      Click to add description...
                    </div>
                  )}
                </div>
              )}
            </div>
          </section>

          {/* Environment */}
          {issue.environment && (
            <section className="idm-card">
              <div className="idm-card-header">
                <h3 className="idm-card-title">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" /></svg>
                  Environment
                </h3>
              </div>
              <div className="idm-card-body"><p className="idm-env-text">{issue.environment}</p></div>
            </section>
          )}

          {/* Activity Tabs */}
          <section className="idm-card idm-card--tabs">
            <div className="idm-tabs" role="tablist" aria-label="Issue panels">
              {tabItems.map((t) => (
                <button key={t.key} type="button" role="tab"
                  aria-selected={activeTab === t.key}
                  className={`idm-tab ${activeTab === t.key ? 'idm-tab--active' : ''}`}
                  onClick={() => setActiveTab(t.key)}
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d={t.icon} /></svg>
                  {t.label}
                  {t.badge !== undefined && (
                    <span className="idm-tab-badge">{t.badge}</span>
                  )}
                </button>
              ))}
            </div>

            <div className="idm-tab-panel" role="tabpanel">
              {activeTab === 'comment' && (
                <div className="idm-comments">
                  <div className="idm-comment-compose">
                    {commentError && <p className="idm-error-msg">{commentError}</p>}
                    <div className="idm-compose-row">
                      <span className="idm-avatar-sm idm-avatar--brand">{user?.username?.charAt(0) || 'U'}</span>
                      <textarea className="idm-textarea idm-textarea--comment" value={newComment}
                        onChange={(e) => setNewComment(e.target.value)} placeholder="Add a comment..."
                      />
                    </div>
                    <div className="idm-compose-footer">
                      <button className="idm-btn idm-btn--primary idm-btn--sm"
                        onClick={() => { if (newComment.trim()) addCommentMutation.mutate(newComment); }}
                        disabled={addCommentMutation.isPending || !newComment.trim()}
                      >
                        {addCommentMutation.isPending ? 'Saving...' : 'Save'}
                      </button>
                    </div>
                  </div>
                  <div className="idm-comment-list">
                    {comments?.map((c: any) => (
                      <div key={c.id} className="idm-comment-item">
                        <span className="idm-avatar-sm">{c.authorName?.charAt(0) || 'U'}</span>
                        <div className="idm-comment-body">
                          <div className="idm-comment-meta">
                            <span className="idm-comment-author">{c.authorName}</span>
                            <span className="idm-comment-time">{getRelativeTime(c.createdAt)}</span>
                          </div>
                          <p className="idm-comment-text">{c.content}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {activeTab === 'activity' && issueId && <ActivityTab issueId={issueId} />}
              {activeTab === 'work' && issueId && <WorklogsTab issueId={issueId} originalEstimate={issue?.originalEstimate} remainingEstimate={issue?.remainingEstimate} timeSpent={issue?.timeSpent} />}
              {activeTab === 'links' && issueId && <IssueLinksTab issueId={issueId} />}
              {activeTab === 'labels' && issueId && <LabelsTab issueId={issueId} />}
              {activeTab === 'attachments' && issueId && <AttachmentsTab issueId={issueId} />}

              {activeTab === 'details' && (
                <div className="idm-details-panel">
                  {resolvedIssueUuid && (
                    <div className="idm-custom-fields-block">
                      <h4 className="idm-detail-section-title">Custom Fields</h4>
                      <IssueCustomFieldsPanel issueId={resolvedIssueUuid} issueKey={issue?.issueKey}
                        projectId={issue?.projectId} issueTypeId={issue?.issueTypeId} variant="inline"
                      />
                    </div>
                  )}
                  <div className="idm-details-grid">
                    {[
                      { label: 'Type', value: <><span className="idm-type-icon-sm">{getTypeIcon(issue.issueType)}</span>{issue.issueType}</> },
                      { label: 'Priority', value: <span className={getPriorityClass(issue.priority)}>{issue.priority}</span> },
                      { label: 'Status', value: issue.status },
                      { label: 'Resolution', value: issue.resolutionName || '-' },
                      { label: 'Affects Version', value: (issue.affectsVersionNames ?? issue.affectsVersions)?.join(', ') || '-' },
                      { label: 'Fix Version', value: (issue.fixVersionNames ?? issue.fixVersions)?.join(', ') || '-' },
                      { label: 'Components', value: issue.components?.join(', ') || '-' },
                    ].map((d) => (
                      <div key={d.label} className="idm-detail-row">
                        <span className="idm-detail-label">{d.label}</span>
                        <span className="idm-detail-value">{d.value}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </section>

          {/* Subtasks */}
          {issue.subtasks && issue.subtasks.length > 0 && (
            <section className="idm-card">
              <div className="idm-card-header">
                <h3 className="idm-card-title">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" /></svg>
                  Subtasks
                  <span className="idm-count-badge">{issue.subtasks.length}</span>
                </h3>
              </div>
              <div className="idm-subtask-list">
                {issue.subtasks.map((subtask: any) => (
                  <Link key={subtask.id} to={`/issues/${subtask.id}`} className="idm-subtask-row">
                    <span className={`idm-status-dot ${getStatusClass(subtask.status)}`} />
                    <span className="idm-subtask-key">{subtask.issueKey}</span>
                    <span className="idm-subtask-title">{subtask.title}</span>
                    <span className={`idm-subtask-status ${getStatusClass(subtask.status)}`}>{subtask.status}</span>
                  </Link>
                ))}
              </div>
            </section>
          )}
        </main>

        {/* ─── RIGHT: DRAWER SIDEBAR ─── */}
        <aside className={`idm-drawer ${drawerOpen ? 'idm-drawer--open' : ''}`} aria-label="Issue details drawer">

          {/* People */}
          <div className="idm-drawer-card">
            <h4 className="idm-drawer-title">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" /></svg>
              People
            </h4>
            <div className="idm-drawer-rows">
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Assignee</span>
                <div className="idm-drawer-value">
                  {issue.assigneeId ? (
                    <span className="idm-avatar-chip"><span className="idm-avatar-sm">{issue.assigneeName?.charAt(0) || 'U'}</span>{issue.assigneeName}</span>
                  ) : (
                    <span className="idm-no-value">Unassigned</span>
                  )}
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Reporter</span>
                <div className="idm-drawer-value">
                  <span className="idm-avatar-chip"><span className="idm-avatar-sm idm-avatar--green">{issue.reporterName?.charAt(0) || 'U'}</span>{issue.reporterName}</span>
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Votes</span>
                <div className="idm-drawer-value idm-drawer-value--actions">
                  <span className="idm-stat">{issue.voteCount || 0}</span>
                  <button type="button" className="idm-micro-btn" onClick={() => voteMutation.mutate()} disabled={voteMutation.isPending}>
                    {voteMutation.isPending ? '...' : 'Vote'}
                  </button>
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Watchers</span>
                <div className="idm-drawer-value idm-drawer-value--actions">
                  <span className="idm-stat">{issue.watcherCount || 0}</span>
                  <button type="button" className="idm-micro-btn" onClick={() => { if (isWatching) unwatchMutation.mutate(); else watchMutation.mutate(); }}
                    disabled={watchMutation.isPending || unwatchMutation.isPending}
                  >
                    {isWatching ? 'Unwatch' : 'Watch'}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Details */}
          <div className="idm-drawer-card">
            <h4 className="idm-drawer-title">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><path d="M12 16v-4M12 8h.01" /></svg>
              Details
            </h4>
            <div className="idm-drawer-rows">
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Priority</span>
                <div className="idm-drawer-value">
                  <span className={`idm-priority-badge idm-priority-badge--sm ${getPriorityClass(issue.priority)}`}>
                    <span className="idm-priority-arrow">{getPriorityIcon(issue.priority)}</span>
                    {issue.priority}
                  </span>
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Resolution</span>
                <div className="idm-drawer-value">{issue.resolutionName || <span className="idm-no-value">Unresolved</span>}</div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Components</span>
                <div className="idm-drawer-value">
                  {issue.components?.length ? (
                    <div className="idm-tag-list">{issue.components.map(c => <span key={c} className="idm-tag">{c}</span>)}</div>
                  ) : (
                    <span className="idm-no-value">None</span>
                  )}
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Labels</span>
                <div className="idm-drawer-value">
                  {issue.labels?.length ? (
                    <div className="idm-tag-list">{issue.labels.map(l => <span key={l} className="idm-tag idm-tag--label">{l}</span>)}</div>
                  ) : (
                    <span className="idm-no-value">None</span>
                  )}
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Security</span>
                <div className="idm-drawer-value">{issue.securityLevelName || <span className="idm-no-value">None</span>}</div>
              </div>
            </div>
          </div>

          {/* Custom Fields */}
          {resolvedIssueUuid && (
            <div className="idm-drawer-card">
              <h4 className="idm-drawer-title">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 3v18M3 12h18" /></svg>
                Custom Fields
                {customFieldsWithValues > 0 && <span className="idm-count-badge">{customFieldsWithValues}</span>}
              </h4>
              <IssueCustomFieldsPanel issueId={resolvedIssueUuid} issueKey={issue.issueKey}
                projectId={issue.projectId} issueTypeId={issue.issueTypeId} variant="sidebar"
              />
              <button type="button" className="idm-view-all-btn" onClick={() => setActiveTab('details')}>
                View all in Details tab
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M5 12h14M12 5l7 7-7 7" /></svg>
              </button>
            </div>
          )}

          {/* Time Tracking */}
          <div className="idm-drawer-card">
            <h4 className="idm-drawer-title">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" /></svg>
              Time Tracking
            </h4>
            {(issue.originalEstimate || issue.timeSpent) && (() => {
              const est = issue.originalEstimate ?? 0;
              const spent = issue.timeSpent ?? 0;
              const rem = issue.remainingEstimate ?? 0;
              const total = Math.max(est, spent + rem, 1);
              const spentPct = Math.min(100, (spent / total) * 100);
              const remPct = Math.min(100 - spentPct, (rem / total) * 100);
              const overBudget = est > 0 && spent > est;
              return (
                <div className="idm-time-bar">
                  <div className="idm-time-bar-track" style={{ display: 'flex', height: '8px', background: 'var(--ab-gray-100, #f3f4f6)', borderRadius: '4px', overflow: 'hidden' }}>
                    <div style={{ width: `${spentPct}%`, background: overBudget ? '#dc2626' : '#2563eb', transition: 'width 0.3s' }} />
                    <div style={{ width: `${remPct}%`, background: '#93c5fd', transition: 'width 0.3s' }} />
                  </div>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '4px', fontSize: '10px', color: 'var(--ab-gray-500)' }}>
                    <span><span style={{ display: 'inline-block', width: '6px', height: '6px', borderRadius: '50%', background: '#2563eb', marginRight: '3px' }} />Logged</span>
                    <span><span style={{ display: 'inline-block', width: '6px', height: '6px', borderRadius: '50%', background: '#93c5fd', marginRight: '3px' }} />Remaining</span>
                  </div>
                </div>
              );
            })()}
            <div className="idm-drawer-rows">
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Original Estimate</span>
                <div className="idm-drawer-value idm-mono">{formatTimeWithDays(issue.originalEstimate)}</div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Remaining</span>
                <div className="idm-drawer-value idm-mono">{formatTimeWithDays(issue.remainingEstimate)}</div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Time Spent</span>
                <div className="idm-drawer-value idm-mono">{formatTimeWithDays(issue.timeSpent)}</div>
              </div>
              {issue.workRatio !== undefined && (
                <div className="idm-drawer-row">
                  <span className="idm-drawer-label">Work Ratio</span>
                  <div className="idm-drawer-value idm-mono">{issue.workRatio}%</div>
                </div>
              )}
              {(issue.aggregateTimeEstimate != null || issue.aggregateTimeSpent != null) && (
                <>
                  <div className="idm-drawer-row" style={{ borderTop: '1px solid var(--ab-gray-100)', paddingTop: '6px', marginTop: '4px' }}>
                    <span className="idm-drawer-label">Aggregate Estimate</span>
                    <div className="idm-drawer-value idm-mono">{formatTimeWithDays(issue.aggregateTimeEstimate)}</div>
                  </div>
                  <div className="idm-drawer-row">
                    <span className="idm-drawer-label">Aggregate Spent</span>
                    <div className="idm-drawer-value idm-mono">{formatTimeWithDays(issue.aggregateTimeSpent)}</div>
                  </div>
                  <div className="idm-drawer-row">
                    <span className="idm-drawer-label">Aggregate Remaining</span>
                    <div className="idm-drawer-value idm-mono">{formatTimeWithDays(issue.aggregateRemainingEstimate)}</div>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Agile */}
          <div className="idm-drawer-card">
            <h4 className="idm-drawer-title">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" /></svg>
              Agile
            </h4>
            <div className="idm-drawer-rows">
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Sprint</span>
                <div className="idm-drawer-value">
                  {issue.sprintName ? <span className="idm-tag idm-tag--sprint">{issue.sprintName}</span> : <span className="idm-no-value">Backlog</span>}
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Story Points</span>
                <div className="idm-drawer-value">
                  {issue.storyPoints !== undefined ? <span className="idm-sp-badge">{issue.storyPoints}</span> : <span className="idm-no-value">None</span>}
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Epic Link</span>
                <div className="idm-drawer-value">
                  {issue.epicId ? (
                    <Link to={`/epics/${issue.epicId}`} className="idm-tag idm-tag--epic">
                      <span style={{ color: issue.epicColor }}>&#x26A1;</span> {issue.epicName}
                    </Link>
                  ) : (
                    <span className="idm-no-value">None</span>
                  )}
                </div>
              </div>
              {issue.teamName && (
                <div className="idm-drawer-row">
                  <span className="idm-drawer-label">Team</span>
                  <div className="idm-drawer-value">{issue.teamName}</div>
                </div>
              )}
            </div>
          </div>

          {/* Dates */}
          <div className="idm-drawer-card">
            <h4 className="idm-drawer-title">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
              Dates
            </h4>
            <div className="idm-drawer-rows">
              <div className="idm-drawer-row"><span className="idm-drawer-label">Created</span><div className="idm-drawer-value">{formatDateTime(issue.createdAt)}</div></div>
              <div className="idm-drawer-row"><span className="idm-drawer-label">Updated</span><div className="idm-drawer-value">{formatDateTime(issue.updatedAt)}</div></div>
              <div className="idm-drawer-row"><span className="idm-drawer-label">Resolved</span><div className="idm-drawer-value">{issue.resolvedAt ? formatDateTime(issue.resolvedAt) : '-'}</div></div>
              <div className="idm-drawer-row"><span className="idm-drawer-label">Due Date</span><div className="idm-drawer-value">{issue.dueDate ? formatDate(issue.dueDate) : '-'}</div></div>
              <div className="idm-drawer-row"><span className="idm-drawer-label">Last Viewed</span><div className="idm-drawer-value">{issue.lastViewedAt ? formatDateTime(issue.lastViewedAt) : '-'}</div></div>
            </div>
          </div>

          {/* Versions */}
          <div className="idm-drawer-card">
            <h4 className="idm-drawer-title">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" /></svg>
              Versions
            </h4>
            <div className="idm-drawer-rows">
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Affects</span>
                <div className="idm-drawer-value">
                  {(issue.affectsVersionNames ?? issue.affectsVersions)?.length ? (
                    <div className="idm-tag-list">{(issue.affectsVersionNames ?? issue.affectsVersions ?? []).map((v, i) => <span key={i} className="idm-tag idm-tag--version">{v}</span>)}</div>
                  ) : (
                    <span className="idm-no-value">None</span>
                  )}
                </div>
              </div>
              <div className="idm-drawer-row">
                <span className="idm-drawer-label">Fix Version</span>
                <div className="idm-drawer-value">
                  {(issue.fixVersionNames ?? issue.fixVersions)?.length ? (
                    <div className="idm-tag-list">{(issue.fixVersionNames ?? issue.fixVersions ?? []).map((v, i) => <span key={i} className="idm-tag idm-tag--version">{v}</span>)}</div>
                  ) : (
                    <span className="idm-no-value">None</span>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Linked Issues */}
          {issue.linkedIssues && issue.linkedIssues.length > 0 && (
            <div className="idm-drawer-card">
              <h4 className="idm-drawer-title">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101" /><path d="M10.172 13.828a4 4 0 005.656 0l4-4a4 4 0 10-5.656-5.656l-1.1 1.1" /></svg>
                Linked Issues
                <span className="idm-count-badge">{issue.linkedIssues.length}</span>
              </h4>
              <div className="idm-linked-list">
                {issue.linkedIssues.map((link, idx) => (
                  <div key={idx} className="idm-linked-row">
                    <span className="idm-linked-type">{link.type}</span>
                    <Link to={`/issues/${link.key}`} className="idm-linked-key">{link.key}</Link>
                    <span className="idm-linked-title">{link.title}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </aside>
      </div>

      {/* ── MODALS ── */}
      {showEditModal && (
        <EditIssueModal
          issue={issue}
          onClose={() => setShowEditModal(false)}
          onSuccess={() => { queryClient.invalidateQueries({ queryKey: ['issue', issueId] }); setShowEditModal(false); }}
        />
      )}
      {showCreateSubtask && issue && (
        <CreateIssueModal
          projectId={issue.projectId} projectKey={issue.projectKey}
          parentIssueId={issueId} defaultTitle={`Subtask of ${issue.issueKey}`}
          onClose={() => setShowCreateSubtask(false)}
          onSuccess={() => { queryClient.invalidateQueries({ queryKey: ['issue', issueId] }); setShowCreateSubtask(false); }}
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
