import React, { useState } from 'react';
import { useIssueTypes, IssueType } from '../hooks/useAdminApi';
import AdminLayout from '../components/AdminLayout';
import './IssueAdministrationPage.css';

export default function IssueTypesPage() {
  const [search, setSearch] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedIssueType, setSelectedIssueType] = useState<IssueType | null>(null);
  const [editMode, setEditMode] = useState(false);

  const { data: issueTypes, isLoading, refetch } = useIssueTypes();

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

  const handleCreate = async () => {
    // API call would go here
    setShowCreateModal(false);
    setFormData({ name: '', description: '', issueTypeKey: '', isSubtask: false });
  };

  const openEditModal = (issueType: IssueType) => {
    setSelectedIssueType({ ...issueType });
    setEditMode(true);
  };

  const handleSaveEdit = () => {
    // API call would go here
    setEditMode(false);
    setSelectedIssueType(null);
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

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search issue types..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={() => setShowCreateModal(true)}>
              Add Issue Type
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
                  <td colSpan={5} style={{ textAlign: 'center', padding: '24px' }}>Loading...</td>
                </tr>
              ) : filteredIssueTypes.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '24px' }}>No issue types found</td>
                </tr>
              ) : (
                filteredIssueTypes.map((issueType) => (
                  <tr key={issueType.id}>
                    <td>
                      <div className="issue-type-cell">
                        <span className="issue-type-icon">{issueType.name.charAt(0)}</span>
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
                        {!issueType.isSubtask && (
                          <button className="admin-btn-secondary">Schemes</button>
                        )}
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
                  onChange={(e) => setFormData({ ...formData, issueTypeKey: e.target.value.toLowerCase() })}
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
              <button className="admin-btn-primary" onClick={handleCreate}>
                Add
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
              <button className="admin-btn-primary" onClick={handleSaveEdit}>
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}