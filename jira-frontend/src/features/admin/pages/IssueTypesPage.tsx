import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import AdminLayout from '../components/AdminLayout';
import './IssueAdministrationPage.css';
import './IssueTypesPage.css';

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

// API functions
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

  const { data: issueTypes, isLoading, isError, error } = useQuery({
    queryKey: ['admin', 'issueTypes'],
    queryFn: () => issueTypeApi.getIssueTypes().then(res => res.data),
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
      alert('Failed to create issue type: ' + err.message);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<IssueType> }) =>
      issueTypeApi.updateIssueType(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypes'] });
      setEditMode(false);
      setSelectedIssueType(null);
    },
    onError: (err: Error) => {
      console.error('Failed to update issue type:', err.message);
      alert('Failed to update issue type: ' + err.message);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => issueTypeApi.deleteIssueType(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'issueTypes'] });
    },
    onError: (err: Error) => {
      console.error('Failed to delete issue type:', err.message);
      alert('Failed to delete issue type: ' + err.message);
    },
  });

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    issueTypeKey: '',
    isSubtask: false,
  });

  const filteredIssueTypes = issueTypes?.filter(it =>
    it.name.toLowerCase().includes(search.toLowerCase()) ||
    it.issueTypeKey.toLowerCase().includes(search.toLowerCase())
  ) || [];

  const handleCreate = () => {
    if (!formData.name || !formData.issueTypeKey) {
      alert('Name and Issue Type Key are required');
      return;
    }
    createMutation.mutate(formData);
  };

  const openEditModal = (issueType: IssueType) => {
    setSelectedIssueType({ ...issueType });
    setEditMode(true);
  };

  const handleSaveEdit = () => {
    if (!selectedIssueType) return;
    updateMutation.mutate({ id: selectedIssueType.id, data: selectedIssueType });
  };

  const handleDelete = (issueType: IssueType) => {
    if (confirm(`Are you sure you want to delete "${issueType.name}"?`)) {
      deleteMutation.mutate(issueType.id);
    }
  };

  const getIssueTypeColor = (issueType: IssueType): string => {
    if (issueType.color) return issueType.color;
    const colors: Record<string, string> = {
      bug: '#d73a49',
      story: '#006644',
      task: '#0052cc',
      epic: '#6b2db0',
    };
    return colors[issueType.issueTypeKey?.toLowerCase()] || '#0052cc';
  };

  const getIssueTypeIcon = (issueType: IssueType): string => {
    const icons: Record<string, string> = {
      bug: '🐛',
      story: '📖',
      task: '✅',
      epic: '⚡',
    };
    return icons[issueType.issueTypeKey?.toLowerCase()] || issueType.name.charAt(0).toUpperCase();
  };

  return (
    <AdminLayout>
    <div className="admin-page">
      <div className="admin-page-header">
        <h1 className="admin-page-title">Issue Types</h1>
        <p className="admin-page-description">
          Configure the types of issues available in your Jira instance.
        </p>
        </div>

        <div className="admin-toolbar-modern">
          <div className="toolbar-left">
            <div className="search-input-wrapper">
              <span className="search-icon">🔍</span>
              <input
                type="text"
                placeholder="Search issue types..."
                className="search-input"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          </div>
          <div className="toolbar-right">
            <button className="btn-create-project" onClick={() => setShowCreateModal(true)}>
              <span>+</span> Add Issue Type
            </button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Issue Type</th>
                <th>Key</th>
                <th>Description</th>
                <th>Type</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '24px' }}>
                    <div className="loading-spinner">Loading issue types...</div>
                  </td>
                </tr>
              ) : isError ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '24px', color: '#de350b' }}>
                    Error loading issue types. Please check if the server is running.
                    <br />
                    <small>{(error as Error)?.message}</small>
                  </td>
                </tr>
              ) : filteredIssueTypes.length === 0 ? (
                <tr>
                  <td colSpan={5}>
                    <div className="admin-empty-state">
                      <div className="admin-empty-state-icon">📋</div>
                      <div className="admin-empty-state-title">No issue types found</div>
                      <div className="admin-empty-state-description">
                        {search ? 'No issue types match your search criteria.' : 'Get started by creating your first issue type.'}
                      </div>
                      {!search && (
                        <button className="admin-btn-primary" onClick={() => setShowCreateModal(true)}>
                          + Add Issue Type
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                filteredIssueTypes.map((issueType) => (
                  <tr key={issueType.id}>
                    <td>
                      <div className="issue-type-cell">
                        <span className="issue-type-icon" style={{ background: getIssueTypeColor(issueType) }}>
                          {getIssueTypeIcon(issueType)}
                        </span>
                        <span className="issue-type-name">{issueType.name}</span>
                      </div>
                    </td>
                    <td>
                      <code className="issue-type-key">{issueType.issueTypeKey}</code>
                    </td>
                    <td className="description-cell">{issueType.description || 'No description'}</td>
                    <td>
                      <span className={`admin-status ${issueType.isSubtask ? 'admin-status-pending' : 'admin-status-active'}`}>
                        {issueType.isSubtask ? 'Subtask' : 'Standard'}
                      </span>
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary" onClick={() => openEditModal(issueType)}>
                          Edit
                        </button>
                        <button
                          className="admin-btn-secondary admin-btn-danger-text"
                          onClick={() => handleDelete(issueType)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Modal */}
      {showCreateModal && (
        <div className="admin-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2 className="admin-modal-title">Add Issue Type</h2>
              <button className="admin-modal-close" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <div className="admin-modal-body">
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Name</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g., Bug, Story, Task"
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Issue Type Key</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.issueTypeKey}
                  onChange={(e) => setFormData({ ...formData, issueTypeKey: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '-') })}
                  placeholder="e.g., bug, story, task"
                />
                <span className="admin-form-hint">Lowercase letters and numbers only</span>
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Description</label>
                <textarea
                  className="admin-form-textarea"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Describe what this issue type represents"
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.isSubtask}
                    onChange={(e) => setFormData({ ...formData, isSubtask: e.target.checked })}
                  />
                  <span>This is a subtask type</span>
                </label>
                <span className="admin-form-hint">Subtasks belong to a parent issue</span>
              </div>
            </div>
            <div className="admin-modal-footer">
              <button className="admin-btn-secondary" onClick={() => setShowCreateModal(false)}>
                Cancel
              </button>
              <button
                className="admin-btn-primary"
                onClick={handleCreate}
                disabled={createMutation.isPending}
              >
                {createMutation.isPending ? 'Creating...' : 'Add'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {editMode && selectedIssueType && (
        <div className="admin-modal-overlay" onClick={() => setEditMode(false)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2 className="admin-modal-title">Edit Issue Type</h2>
              <button className="admin-modal-close" onClick={() => setEditMode(false)}>×</button>
            </div>
            <div className="admin-modal-body">
              <div className="admin-form-group">
                <label className="admin-form-label">Name</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={selectedIssueType.name}
                  onChange={(e) => setSelectedIssueType({ ...selectedIssueType, name: e.target.value })}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Description</label>
                <textarea
                  className="admin-form-textarea"
                  value={selectedIssueType.description}
                  onChange={(e) => setSelectedIssueType({ ...selectedIssueType, description: e.target.value })}
                />
              </div>
            </div>
            <div className="admin-modal-footer">
              <button className="admin-btn-secondary" onClick={() => setEditMode(false)}>
                Cancel
              </button>
              <button
                className="admin-btn-primary"
                onClick={handleSaveEdit}
                disabled={updateMutation.isPending}
              >
                {updateMutation.isPending ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}