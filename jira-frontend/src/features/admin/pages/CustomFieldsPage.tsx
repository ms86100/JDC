import React, { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import AdminLayout from '../components/AdminLayout';
import { fieldApi, type CreateCustomFieldRequest } from '../../../api/fieldApi';
import './CustomFieldsPage.css';

interface CustomFieldRow {
  id: string;
  name: string;
  type: string;
  description: string;
  fieldKey: string;
  enabled?: boolean;
}

const fieldTypes = [
  { key: 'text', label: 'Text Field', icon: 'T' },
  { key: 'number', label: 'Number Field', icon: '#' },
  { key: 'datepicker', label: 'Date Picker', icon: 'D' },
  { key: 'datetime', label: 'Date Time Picker', icon: '⏱' },
  { key: 'select', label: 'Select List', icon: '▼' },
  { key: 'multiselect', label: 'Multi-Select', icon: '⊞' },
  { key: 'checkbox', label: 'Checkboxes', icon: '☑' },
  { key: 'textarea', label: 'Text Area', icon: '≡' },
  { key: 'url', label: 'URL', icon: '🔗' },
  { key: 'userpicker', label: 'User Picker', icon: '👤' },
];

export default function CustomFieldsPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newName, setNewName] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newType, setNewType] = useState('text');
  const [error, setError] = useState<string | null>(null);

  const { data: fields = [], isLoading, isError } = useQuery({
    queryKey: ['admin-custom-fields'],
    queryFn: async () => {
      const res = await fieldApi.getCustomFields();
      return (res.data ?? []).map(
        (f): CustomFieldRow => ({
          id: f.id,
          name: f.name,
          type: f.type,
          description: f.description ?? '',
          fieldKey: f.fieldKey,
          enabled: f.enabled,
        })
      );
    },
  });

  const createMutation = useMutation({
    mutationFn: (body: CreateCustomFieldRequest) => fieldApi.createCustomField(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-custom-fields'] });
      queryClient.invalidateQueries({ queryKey: ['migration-target-fields'] });
      setShowCreateModal(false);
      setNewName('');
      setNewDescription('');
      setNewType('text');
      setError(null);
    },
    onError: (e: Error) => setError(e.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => fieldApi.deleteCustomField(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-custom-fields'] });
      queryClient.invalidateQueries({ queryKey: ['migration-target-fields'] });
    },
    onError: (e: Error) => setError(e.message),
  });

  const filteredFields = useMemo(() => {
    return fields.filter((field) => {
      const matchesSearch =
        field.name.toLowerCase().includes(search.toLowerCase()) ||
        field.description.toLowerCase().includes(search.toLowerCase()) ||
        field.fieldKey.toLowerCase().includes(search.toLowerCase());
      const matchesType = !typeFilter || field.type === typeFilter;
      return matchesSearch && matchesType;
    });
  }, [fields, search, typeFilter]);

  const handleCreate = () => {
    if (!newName.trim()) {
      setError('Field name is required');
      return;
    }
    createMutation.mutate({
      name: newName.trim(),
      description: newDescription.trim() || undefined,
      type: newType,
    });
  };

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Custom Fields</h1>
          <p className="admin-page-description">
            Create and manage custom fields (Jira DC parity — backed by migration field registry).
          </p>
        </div>

        {error && (
          <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded p-2 mb-4">{error}</div>
        )}

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Custom Fields</div>
            <div className="admin-stat-value">{fields.length}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Field Types Used</div>
            <div className="admin-stat-value">{new Set(fields.map((f) => f.type)).size}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Enabled</div>
            <div className="admin-stat-value">{fields.filter((f) => f.enabled !== false).length}</div>
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
              {fieldTypes.map((type) => (
                <option key={type.key} value={type.key}>
                  {type.label}
                </option>
              ))}
            </select>
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={() => setShowCreateModal(true)}>
              Add Custom Field
            </button>
          </div>
        </div>

        {isLoading && <p className="text-gray-500 p-4">Loading custom fields…</p>}
        {isError && (
          <p className="text-red-600 p-4">
            Failed to load fields. Ensure migration-service is running on port 8094.
          </p>
        )}

        <div className="custom-fields-grid">
          {filteredFields.map((field) => (
            <div key={field.id} className="custom-field-card">
              <div className="field-card-header">
                <div className="field-type-badge">{field.type}</div>
                <span className="text-xs text-gray-500">{field.fieldKey}</span>
              </div>
              <div className="field-card-body">
                <h4 className="field-name">{field.name}</h4>
                <p className="field-description">{field.description || '—'}</p>
              </div>
              <div className="field-card-footer">
                <button
                  type="button"
                  className="admin-btn-secondary text-red-700"
                  disabled={deleteMutation.isPending}
                  onClick={() => {
                    if (window.confirm(`Disable custom field "${field.name}"?`)) {
                      deleteMutation.mutate(field.id);
                    }
                  }}
                >
                  Disable
                </button>
              </div>
            </div>
          ))}
        </div>

        {!isLoading && filteredFields.length === 0 && (
          <div className="admin-empty-state">
            <div className="admin-empty-state-title">No custom fields found</div>
            <div className="admin-empty-state-description">
              {search || typeFilter
                ? 'Try adjusting your search or filter criteria.'
                : 'Create a custom field or provision fields from the migration wizard.'}
            </div>
          </div>
        )}
      </div>

      {showCreateModal && (
        <div className="admin-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="admin-modal" style={{ maxWidth: '520px' }} onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2 className="admin-modal-title">Add Custom Field</h2>
              <button type="button" className="admin-modal-close" onClick={() => setShowCreateModal(false)}>
                ×
              </button>
            </div>
            <div className="admin-modal-body space-y-4">
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Field Name</label>
                <input
                  type="text"
                  className="admin-form-input"
                  placeholder="e.g., Story Points"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Description</label>
                <textarea
                  className="admin-form-textarea"
                  placeholder="Describe what this field is used for"
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Type</label>
                <select
                  className="admin-form-select"
                  value={newType}
                  onChange={(e) => setNewType(e.target.value)}
                >
                  {fieldTypes.map((t) => (
                    <option key={t.key} value={t.key}>
                      {t.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="admin-modal-footer">
              <button type="button" className="admin-btn-secondary" onClick={() => setShowCreateModal(false)}>
                Cancel
              </button>
              <button
                type="button"
                className="admin-btn-primary"
                disabled={createMutation.isPending}
                onClick={handleCreate}
              >
                {createMutation.isPending ? 'Creating…' : 'Create'}
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
