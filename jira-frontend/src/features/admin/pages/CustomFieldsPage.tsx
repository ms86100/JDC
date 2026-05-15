import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import './CustomFieldsPage.css';

interface CustomField {
  id: string;
  name: string;
  type: string;
  description: string;
  contextCount: number;
  isRequired: boolean;
}

const mockCustomFields: CustomField[] = [
  { id: '1', name: 'Story Points', type: 'number', description: 'Estimation in story points', contextCount: 3, isRequired: false },
  { id: '2', name: 'Sprint', type: 'sprint', description: 'Current sprint assignment', contextCount: 5, isRequired: false },
  { id: '3', name: 'Team', type: 'select', description: 'Development team', contextCount: 2, isRequired: true },
  { id: '4', name: 'Release Date', type: 'datepicker', description: 'Target release date', contextCount: 4, isRequired: false },
  { id: '5', name: 'Epic Link', type: 'issuelink', description: 'Parent epic', contextCount: 8, isRequired: false },
  { id: '6', name: 'Department', type: 'select', description: 'Department assignment', contextCount: 1, isRequired: true },
];

const fieldTypes = [
  { key: 'text', label: 'Text Field', icon: 'T' },
  { key: 'number', label: 'Number Field', icon: '#' },
  { key: 'datepicker', label: 'Date Picker', icon: 'D' },
  { key: 'datetime', label: 'Date Time Picker', icon: '⏱' },
  { key: 'select', label: 'Select List', icon: '▼' },
  { key: 'multiselect', label: 'Multi-Select', icon: '⊞' },
  { key: 'checkbox', label: 'Checkboxes', icon: '☑' },
  { key: 'radio', label: 'Radio Buttons', icon: '○' },
  { key: 'textarea', label: 'Text Area', icon: '≡' },
  { key: 'url', label: 'URL', icon: '🔗' },
  { key: 'issuelink', label: 'Issue Link', icon: '⛓' },
  { key: 'sprint', label: 'Sprint', icon: '⚡' },
  { key: 'project', label: 'Project', icon: '📁' },
  { key: 'userpicker', label: 'User Picker', icon: '👤' },
];

export default function CustomFieldsPage() {
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedField, setSelectedField] = useState<CustomField | null>(null);

  const filteredFields = mockCustomFields.filter(field => {
    const matchesSearch = field.name.toLowerCase().includes(search.toLowerCase()) ||
      field.description.toLowerCase().includes(search.toLowerCase());
    const matchesType = !typeFilter || field.type === typeFilter;
    return matchesSearch && matchesType;
  });

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Custom Fields</h1>
          <p className="admin-page-description">
            Create and manage custom fields to capture additional data on issues.
          </p>
        </div>

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Custom Fields</div>
            <div className="admin-stat-value">{mockCustomFields.length}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Field Types Used</div>
            <div className="admin-stat-value">
              {new Set(mockCustomFields.map(f => f.type)).size}
            </div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Required Fields</div>
            <div className="admin-stat-value">
              {mockCustomFields.filter(f => f.isRequired).length}
            </div>
          </div>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search custom fields..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="admin-form-select"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              style={{ width: '160px' }}
            >
              <option value="">All Types</option>
              {fieldTypes.map(type => (
                <option key={type.key} value={type.key}>{type.label}</option>
              ))}
            </select>
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-secondary">Reorder Fields</button>
            <button className="admin-btn-primary" onClick={() => setShowCreateModal(true)}>
              Add Custom Field
            </button>
          </div>
        </div>

        <div className="custom-fields-grid">
          {filteredFields.map((field) => (
            <div key={field.id} className="custom-field-card">
              <div className="field-card-header">
                <div className="field-type-badge">{field.type}</div>
                {field.isRequired && (
                  <span className="required-badge">Required</span>
                )}
              </div>
              <div className="field-card-body">
                <h4 className="field-name">{field.name}</h4>
                <p className="field-description">{field.description}</p>
                <div className="field-meta">
                  <span className="field-contexts">{field.contextCount} contexts</span>
                </div>
              </div>
              <div className="field-card-footer">
                <button className="admin-btn-secondary" onClick={() => setSelectedField(field)}>
                  Configure
                </button>
                <button className="admin-btn-secondary">Screens</button>
              </div>
            </div>
          ))}
        </div>

        {filteredFields.length === 0 && (
          <div className="admin-empty-state">
            <div className="admin-empty-state-icon">🔍</div>
            <div className="admin-empty-state-title">No custom fields found</div>
            <div className="admin-empty-state-description">
              {search || typeFilter
                ? 'Try adjusting your search or filter criteria.'
                : 'Get started by creating your first custom field.'}
            </div>
            {!search && !typeFilter && (
              <button className="admin-btn-primary" onClick={() => setShowCreateModal(true)}>
                Add Custom Field
              </button>
            )}
          </div>
        )}
      </div>

      {/* Create Custom Field Modal */}
      {showCreateModal && (
        <div className="admin-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="admin-modal" style={{ maxWidth: '800px' }} onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2 className="admin-modal-title">Add Custom Field</h2>
              <button className="admin-modal-close" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <div className="admin-modal-body">
              <div className="field-type-selector">
                <h4 className="field-type-title">Select the type of field</h4>
                <div className="field-types-grid">
                  {fieldTypes.map((type) => (
                    <button key={type.key} className="field-type-option">
                      <span className="field-type-icon">{type.icon}</span>
                      <span className="field-type-label">{type.label}</span>
                    </button>
                  ))}
                </div>
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Field Name</label>
                <input type="text" className="admin-form-input" placeholder="e.g., Story Points" />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Description</label>
                <textarea
                  className="admin-form-textarea"
                  placeholder="Describe what this field is used for"
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Field Context</label>
                <select className="admin-form-select">
                  <option value="global">Global (All Issues)</option>
                  <option value="project">Specific Project(s)</option>
                  <option value="issuetype">Specific Issue Type(s)</option>
                </select>
                <span className="admin-form-hint">
                  Choose where this field will be available
                </span>
              </div>
            </div>
            <div className="admin-modal-footer">
              <button className="admin-btn-secondary" onClick={() => setShowCreateModal(false)}>
                Cancel
              </button>
              <button className="admin-btn-primary">Next</button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}