import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { issueLinkApi, IssueLinkResponse } from '../../../api/issueLinkApi';
import { issueApi } from '../../../api/issueApi';

interface IssueLinksTabProps {
  issueId: string;
}

const LINK_TYPES = [
  { value: 'blocks', label: 'Blocks', icon: '⛔' },
  { value: 'relates to', label: 'Relates to', icon: '🔗' },
  { value: 'duplicates', label: 'Duplicates', icon: '📋' },
  { value: 'is cloned by', label: 'Clones', icon: '📎' },
  { value: 'is parent of', label: 'Parent', icon: '📁' },
  { value: 'causes', label: 'Causes', icon: '⚠️' },
];

export default function IssueLinksTab({ issueId }: IssueLinksTabProps) {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [targetIssueKey, setTargetIssueKey] = useState('');
  const [linkType, setLinkType] = useState('relates to');

  const { data: outwardLinks, isLoading: loadingOutward } = useQuery<IssueLinkResponse[]>({
    queryKey: ['issue-links-outward', issueId],
    queryFn: async () => {
      const response = await issueLinkApi.getOutward(issueId);
      return response.data;
    },
    enabled: !!issueId,
  });

  const { data: inwardLinks, isLoading: loadingInward } = useQuery<IssueLinkResponse[]>({
    queryKey: ['issue-links-inward', issueId],
    queryFn: async () => {
      const response = await issueLinkApi.getInward(issueId);
      return response.data;
    },
    enabled: !!issueId,
  });

  const createMutation = useMutation({
    mutationFn: (data: { destinationIssueId: string; linkType: string }) =>
      issueLinkApi.create(issueId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issue-links-outward', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issue-links-inward', issueId] });
      setShowForm(false);
      setTargetIssueKey('');
      setLinkType('relates to');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (linkId: string) => issueLinkApi.delete(issueId, linkId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issue-links-outward', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issue-links-inward', issueId] });
    },
  });

  const handleCreateLink = async () => {
    if (!targetIssueKey.trim()) return;

    // First, find the issue by key
    try {
      const response = await issueApi.getAll({ projectId: '', search: targetIssueKey });
      const foundIssue = response.data?.find(
        (i: any) => i.issueKey?.toLowerCase() === targetIssueKey.toLowerCase()
      );

      if (foundIssue) {
        createMutation.mutate({
          destinationIssueId: foundIssue.id,
          linkType,
        });
      } else {
        alert('Issue not found. Please enter a valid issue key.');
      }
    } catch (error) {
      alert('Could not find the issue. Please check the issue key.');
    }
  };

  const getLinkIcon = (type: string) => {
    return LINK_TYPES.find(t => t.value === type)?.icon || '🔗';
  };

  const getLinkLabel = (type: string) => {
    return LINK_TYPES.find(t => t.value === type)?.label || type;
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="ab-links-tab">
      <div className="ab-section-header">
        <h3>Issue Links</h3>
        <button
          className="ab-btn ab-btn-primary ab-btn-sm"
          onClick={() => setShowForm(!showForm)}
        >
          {showForm ? 'Cancel' : 'Link Issue'}
        </button>
      </div>

      {showForm && (
        <div className="ab-link-form ab-card">
          <div className="ab-card-body">
            <div className="ab-form-row">
              <div className="ab-form-group">
                <label className="ab-label">Link Type</label>
                <select
                  className="ab-select"
                  value={linkType}
                  onChange={(e) => setLinkType(e.target.value)}
                >
                  {LINK_TYPES.map((type) => (
                    <option key={type.value} value={type.value}>
                      {type.icon} {type.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="ab-form-group">
                <label className="ab-label">Issue Key</label>
                <input
                  type="text"
                  className="ab-input"
                  placeholder="e.g., PROJ-123"
                  value={targetIssueKey}
                  onChange={(e) => setTargetIssueKey(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleCreateLink();
                    }
                  }}
                />
              </div>
            </div>
            <div className="ab-form-actions">
              <button
                className="ab-btn ab-btn-secondary"
                onClick={() => setShowForm(false)}
              >
                Cancel
              </button>
              <button
                className="ab-btn ab-btn-primary"
                onClick={handleCreateLink}
                disabled={!targetIssueKey.trim() || createMutation.isPending}
              >
                {createMutation.isPending ? 'Creating...' : 'Create Link'}
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="ab-links-content">
        {loadingOutward || loadingInward ? (
          <div className="ab-loading">
            <div className="ab-spinner"></div>
          </div>
        ) : (
          <>
            {/* Outward Links */}
            {outwardLinks && outwardLinks.length > 0 && (
              <div className="ab-links-section">
                <h4 className="ab-links-section-title">Outward Links</h4>
                <div className="ab-links-list">
                  {outwardLinks.map((link) => (
                    <div key={link.id} className="ab-link-item">
                      <span className="ab-link-icon">{getLinkIcon(link.linkType)}</span>
                      <span className="ab-link-type">{getLinkLabel(link.linkType)}</span>
                      <Link to={`/issues/${link.destinationIssueId}`} className="ab-link-issue">
                        {link.destinationIssueKey || link.destinationIssueId}
                      </Link>
                      <span className="ab-link-date">{formatDate(link.createdAt)}</span>
                      <button
                        className="ab-btn-icon"
                        onClick={() => {
                          if (confirm('Delete this link?')) {
                            deleteMutation.mutate(link.id);
                          }
                        }}
                        title="Delete"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Inward Links */}
            {inwardLinks && inwardLinks.length > 0 && (
              <div className="ab-links-section">
                <h4 className="ab-links-section-title">Inward Links</h4>
                <div className="ab-links-list">
                  {inwardLinks.map((link) => (
                    <div key={link.id} className="ab-link-item">
                      <span className="ab-link-icon">↩️</span>
                      <span className="ab-link-type">{getLinkLabel(link.linkType)} by</span>
                      <Link to={`/issues/${link.sourceIssueId}`} className="ab-link-issue">
                        {link.sourceIssueKey || link.sourceIssueId}
                      </Link>
                      <span className="ab-link-date">{formatDate(link.createdAt)}</span>
                      <button
                        className="ab-btn-icon"
                        onClick={() => {
                          if (confirm('Delete this link?')) {
                            deleteMutation.mutate(link.id);
                          }
                        }}
                        title="Delete"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {(!outwardLinks?.length && !inwardLinks?.length) && (
              <div className="ab-empty-state">
                <div className="ab-empty-state-icon">🔗</div>
                <p className="ab-empty-state-description">No issue links yet</p>
              </div>
            )}
          </>
        )}
      </div>

      <style>{`
        .ab-links-tab {
          padding: var(--ab-spacing-md) 0;
        }

        .ab-section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-section-header h3 {
          font-size: var(--ab-font-size-base);
          font-weight: 600;
          margin: 0;
        }

        .ab-link-form {
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-form-row {
          display: grid;
          grid-template-columns: 1fr 2fr;
          gap: var(--ab-spacing-md);
        }

        @media (max-width: 600px) {
          .ab-form-row {
            grid-template-columns: 1fr;
          }
        }

        .ab-form-actions {
          display: flex;
          justify-content: flex-end;
          gap: var(--ab-spacing-sm);
          margin-top: var(--ab-spacing-md);
        }

        .ab-links-section {
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-links-section-title {
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
          color: var(--ab-gray-500);
          text-transform: uppercase;
          letter-spacing: 0.5px;
          margin-bottom: var(--ab-spacing-sm);
        }

        .ab-links-list {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-xs);
        }

        .ab-link-item {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-sm) var(--ab-spacing-md);
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
        }

        .ab-link-icon {
          font-size: var(--ab-font-size-base);
        }

        .ab-link-type {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-600);
          min-width: 100px;
        }

        .ab-link-issue {
          flex: 1;
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-primary-500);
        }

        .ab-link-date {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-400);
        }
      `}</style>
    </div>
  );
}
