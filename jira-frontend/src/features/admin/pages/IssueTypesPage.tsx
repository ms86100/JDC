import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import { issueTypeSchemeApi, schemesForIssueType } from '../../../api/issueAdminApi';
import './IssueTypesPage.css';
import './AdminIssueConfig.css';

interface IssueType {
  id: string;
  name: string;
  description: string;
  issueTypeKey: string;
  isSubtask: boolean;
  icon?: string;
  color?: string;
  createdAt?: string;
}

const ISSUE_TYPE_ICONS: Record<string, { icon: string; color: string }> = {
  bug: { icon: '🐛', color: '#d73a49' },
  story: { icon: '📖', color: '#006644' },
  task: { icon: '✅', color: '#0052cc' },
  epic: { icon: '⚡', color: '#6b2db0' },
  subtask: { icon: '📝', color: '#6554c0' },
};

const issueTypeApi = {
  getIssueTypes: () => apiClient.get<IssueType[]>('/api/admin/issues/issue-types'),
  createIssueType: (data: Partial<IssueType>) => apiClient.post<IssueType>('/api/admin/issues/issue-types', data),
  updateIssueType: (id: string, data: Partial<IssueType>) => apiClient.put<IssueType>(`/api/admin/issues/issue-types/${id}`, data),
  deleteIssueType: (id: string) => apiClient.delete(`/api/admin/issues/issue-types/${id}`),
};

export default function IssueTypesPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedIssueType, setSelectedIssueType] = useState<IssueType | null>(null);
  const [editMode, setEditMode] = useState(false);

  const { data: issueTypes, isLoading, isError } = useQuery({
    queryKey: ['admin', 'issueTypes'],
    queryFn: () => issueTypeApi.getIssueTypes().then(res => res.data),
  });

  const { data: schemes } = useQuery({
    queryKey: ['admin', 'issueTypeSchemes'],
    queryFn: () => issueTypeSchemeApi.list().then((r) => r.data),
  });

  const createMutation = useMutation({
    mutationFn: (data: Partial<IssueType>) => issueTypeApi.createIssueType(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypes'] });
      setShowCreateModal(false);
      setFormData({ name: '', description: '', issueTypeKey: '', isSubtask: false });
    },
    onError: (err: Error) => {
      console.error('Failed to create issue type:', err.message);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<IssueType> }) =>
      issueTypeApi.updateIssueType(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypes'] });
      setEditMode(false);
      setSelectedIssueType(null);
      setActionError(null);
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Failed to update issue type');
      setActionError(msg);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => issueTypeApi.deleteIssueType(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypes'] });
      setActionError(null);
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Failed to delete issue type');
      setActionError(msg);
    },
  });

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    issueTypeKey: '',
    isSubtask: false,
  });

  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const filteredIssueTypes = issueTypes?.filter(it =>
    it.name.toLowerCase().includes(search.toLowerCase()) ||
    it.issueTypeKey.toLowerCase().includes(search.toLowerCase())
  ) || [];

  const handleCreate = () => {
    if (!formData.name || !formData.issueTypeKey) return;
    createMutation.mutate(formData);
  };

  const openEditModal = (issueType: IssueType) => {
    setSelectedIssueType({ ...issueType });
    setEditMode(true);
  };

  const handleSaveEdit = () => {
    if (!selectedIssueType) return;
    updateMutation.mutate({
      id: selectedIssueType.id,
      data: {
        name: selectedIssueType.name,
        description: selectedIssueType.description,
        isSubtask: selectedIssueType.isSubtask,
      },
    });
  };

  const handleDelete = (issueType: IssueType) => {
    setActionError(null);
    deleteMutation.mutate(issueType.id, {
      onSettled: () => setDeleteConfirm(null),
    });
  };

  const getIssueTypeStyle = (issueType: IssueType) => {
    const key = issueType.issueTypeKey?.toLowerCase();
    if (issueType.isSubtask) return ISSUE_TYPE_ICONS.subtask;
    if (key && ISSUE_TYPE_ICONS[key]) return ISSUE_TYPE_ICONS[key];
    return { icon: issueType.name.charAt(0).toUpperCase(), color: '#0052cc' };
  };

  const sortedIssueTypes = useMemo(
    () =>
      [...filteredIssueTypes].sort((a, b) => {
        if (a.isSubtask !== b.isSubtask) return a.isSubtask ? 1 : -1;
        return a.name.localeCompare(b.name);
      }),
    [filteredIssueTypes]
  );

  const canDeleteType = (issueTypes?.length ?? 0) > 1;

  return (
    <div className="it-page dc-page">
      <div className="it-header">
        <div className="it-header-content">
          <h1 className="it-title">Issue Types</h1>
          <p className="it-subtitle">
            Configure issue types for your instance. Assign them to projects via{' '}
            <Link to="/admin/issue-type-schemes" style={{ color: '#0052cc', fontWeight: 500 }}>
              Issue type schemes
            </Link>
            .
          </p>
        </div>
        <button className="it-btn it-btn-primary" onClick={() => setShowCreateModal(true)}>
          <span className="it-btn-icon">+</span>
          Add Issue Type
        </button>
      </div>

      {actionError && (
        <div className="it-action-error" role="alert">
          {actionError}
          <button type="button" onClick={() => setActionError(null)} aria-label="Dismiss">
            ×
          </button>
        </div>
      )}

      <div className="it-toolbar">
        <div className="it-search-wrapper">
          <svg className="it-search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
          <input
            type="text"
            className="it-search-input"
            placeholder="Search issue types..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          {search && (
            <button className="it-search-clear" onClick={() => setSearch('')}>×</button>
          )}
        </div>
      </div>

      {isLoading && (
        <div className="it-loading">
          <div className="it-spinner"></div>
          <span>Loading issue types...</span>
        </div>
      )}

      {isError && (
        <div className="it-error-state">
          <div className="it-error-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4M12 16h.01"/>
            </svg>
          </div>
          <h3>Error loading issue types</h3>
          <p>Please check if the server is running</p>
          <button className="it-btn it-btn-secondary" onClick={() => queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypes'] })}>
            Retry
          </button>
        </div>
      )}

      {!isLoading && !isError && filteredIssueTypes.length === 0 && (
        <div className="it-empty-state">
          <div className="it-empty-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M9 9h6M9 12h6M9 15h4"/>
            </svg>
          </div>
          <h3>{search ? 'No results found' : 'No issue types yet'}</h3>
          <p>{search ? 'Try adjusting your search terms' : 'Get started by creating your first issue type'}</p>
          {!search && (
            <button className="it-btn it-btn-primary" onClick={() => setShowCreateModal(true)}>
              + Add Issue Type
            </button>
          )}
        </div>
      )}

      {!isLoading && !isError && filteredIssueTypes.length > 0 && (
        <div className="ab-recent-table-wrap ab-dc-issue-types-table">
          <table className="ab-recent-table ab-issue-config-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Related schemes</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {sortedIssueTypes.map((issueType) => {
                const style = getIssueTypeStyle(issueType);
                const related = schemesForIssueType(schemes, issueType.id);
                return (
                  <tr key={issueType.id}>
                    <td>
                      <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                        <span className="ab-it-icon" style={{ backgroundColor: style.color }}>
                          {style.icon}
                        </span>
                        <div className="ab-it-name-cell">
                          <strong>{issueType.name}</strong>
                          <code style={{ fontSize: 11, color: '#5e6c84' }}>{issueType.issueTypeKey}</code>
                          {issueType.description && (
                            <div className="ab-it-desc">{issueType.description}</div>
                          )}
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className={`ab-dc-type-pill${issueType.isSubtask ? ' subtask' : ''}`}>
                        {issueType.isSubtask ? 'Sub-task' : 'Standard'}
                      </span>
                    </td>
                    <td>
                      {related.length > 0 ? (
                        <ul className="ab-scheme-links">
                          {related.map((s) => (
                            <li key={s.id}>
                              <Link to={`/admin/issue-type-schemes?schemeId=${s.id}`}>{s.name}</Link>
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <span style={{ fontSize: 13, color: '#97a0af' }}>No associated schemes</span>
                      )}
                    </td>
                    <td>
                      <div className="ab-ops-list">
                        <button type="button" onClick={() => openEditModal(issueType)}>
                          Edit
                        </button>
                        {canDeleteType && (
                          <button
                            type="button"
                            className="danger"
                            onClick={() => setDeleteConfirm(issueType.id)}
                          >
                            Delete
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
      )}

      {/* Delete Confirmation */}
      {deleteConfirm && (
        <div className="it-modal-overlay" onClick={() => setDeleteConfirm(null)}>
          <div className="it-modal it-modal-confirm" onClick={(e) => e.stopPropagation()}>
            <div className="it-modal-icon it-modal-icon-warning">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                <line x1="12" y1="9" x2="12" y2="13"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </div>
            <h3>Delete Issue Type</h3>
            <p>Are you sure you want to delete this issue type? This action cannot be undone.</p>
            <div className="it-modal-actions">
              <button className="it-btn it-btn-secondary" onClick={() => setDeleteConfirm(null)}>
                Cancel
              </button>
              <button className="it-btn it-btn-danger" onClick={() => handleDelete(issueTypes!.find(it => it.id === deleteConfirm)!)}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Create Modal */}
      {showCreateModal && (
        <div className="it-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="it-modal" onClick={(e) => e.stopPropagation()}>
            <div className="it-modal-header">
              <h2>Add Issue Type</h2>
              <button className="it-modal-close" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <div className="it-modal-body">
              <div className="it-form-group">
                <label>Name <span className="it-required">*</span></label>
                <input
                  type="text"
                  className="it-input"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g., Bug, Story, Task"
                />
              </div>
              <div className="it-form-group">
                <label>Issue Type Key <span className="it-required">*</span></label>
                <input
                  type="text"
                  className="it-input it-input-key"
                  value={formData.issueTypeKey}
                  onChange={(e) => setFormData({ ...formData, issueTypeKey: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '-') })}
                  placeholder="e.g., bug, story, task"
                />
                <span className="it-form-hint">Lowercase letters and numbers only</span>
              </div>
              <div className="it-form-group">
                <label>Description</label>
                <textarea
                  className="it-textarea"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Describe what this issue type represents"
                  rows={3}
                />
              </div>
              <div className="it-form-group">
                <label className="it-checkbox-wrapper">
                  <input
                    type="checkbox"
                    checked={formData.isSubtask}
                    onChange={(e) => setFormData({ ...formData, isSubtask: e.target.checked })}
                  />
                  <span className="it-checkbox-custom"></span>
                  <span>This is a subtask type</span>
                </label>
                <span className="it-form-hint">Subtasks belong to a parent issue</span>
              </div>
            </div>
            <div className="it-modal-footer">
              <button className="it-btn it-btn-secondary" onClick={() => setShowCreateModal(false)}>
                Cancel
              </button>
              <button
                className="it-btn it-btn-primary"
                onClick={handleCreate}
                disabled={!formData.name || !formData.issueTypeKey || createMutation.isPending}
              >
                {createMutation.isPending ? 'Adding...' : 'Add Issue Type'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {editMode && selectedIssueType && (
        <div className="it-modal-overlay" onClick={() => setEditMode(false)}>
          <div className="it-modal" onClick={(e) => e.stopPropagation()}>
            <div className="it-modal-header">
              <h2>Edit Issue Type</h2>
              <button className="it-modal-close" onClick={() => setEditMode(false)}>×</button>
            </div>
            <div className="it-modal-body">
              <div className="it-form-group">
                <label>Issue type key</label>
                <input
                  type="text"
                  className="it-input it-input-key"
                  value={selectedIssueType.issueTypeKey}
                  readOnly
                  aria-readonly="true"
                />
                <span className="it-form-hint">Key cannot be changed after creation (Jira DC behaviour)</span>
              </div>
              <div className="it-form-group">
                <label>Name</label>
                <input
                  type="text"
                  className="it-input"
                  value={selectedIssueType.name}
                  onChange={(e) => setSelectedIssueType({ ...selectedIssueType, name: e.target.value })}
                />
              </div>
              <div className="it-form-group">
                <label className="it-checkbox-wrapper">
                  <input
                    type="checkbox"
                    checked={selectedIssueType.isSubtask}
                    onChange={(e) =>
                      setSelectedIssueType({ ...selectedIssueType, isSubtask: e.target.checked })
                    }
                  />
                  <span className="it-checkbox-custom" />
                  <span>This is a subtask type</span>
                </label>
              </div>
              <div className="it-form-group">
                <label>Description</label>
                <textarea
                  className="it-textarea"
                  value={selectedIssueType.description}
                  onChange={(e) => setSelectedIssueType({ ...selectedIssueType, description: e.target.value })}
                  rows={3}
                />
              </div>
            </div>
            <div className="it-modal-footer">
              <button className="it-btn it-btn-secondary" onClick={() => setEditMode(false)}>
                Cancel
              </button>
              <button
                className="it-btn it-btn-primary"
                onClick={handleSaveEdit}
                disabled={updateMutation.isPending}
              >
                {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}