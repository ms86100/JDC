import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, Edit2, Lock, Settings, X } from 'lucide-react';
import AdminLayout from '../components/AdminLayout';
import apiClient from '../../../api/axiosClient';
import '../styles/admin-shared.css';
import './SystemConfigPage.css';

// ==================== Types ====================

interface SystemConfig {
  id: string;
  configKey: string;
  configValue: string;
  valueType: 'STRING' | 'INTEGER' | 'BOOLEAN' | 'JSON';
  category: string;
  description: string;
  isEditable: boolean;
  updatedAt: string | null;
  updatedBy: string | null;
}

// ==================== Constants ====================

const ALL_CATEGORIES = [
  'ALL',
  'ISSUE',
  'PROJECT',
  'BOARD',
  'SPRINT',
  'TEST',
  'QUALITY',
  'USER',
  'LDAP',
  'WORKFLOW',
  'RELEASE',
  'GOAL',
  'ATTACHMENT',
] as const;

type CategoryFilter = (typeof ALL_CATEGORIES)[number];

// ==================== API ====================

const configApi = {
  getAll: () => apiClient.get<SystemConfig[]>('/api/admin/config'),
  getByCategory: (category: string) =>
    apiClient.get<SystemConfig[]>(`/api/admin/config/category/${category}`),
  update: (key: string, value: string) =>
    apiClient.put<SystemConfig>(`/api/admin/config/${key}`, { value }),
};

// ==================== Hooks ====================

function useSystemConfigs() {
  return useQuery({
    queryKey: ['admin', 'systemConfigs'],
    queryFn: () => configApi.getAll(),
    select: (res) => (Array.isArray(res.data) ? res.data : []),
  });
}

function useUpdateSystemConfig() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ key, value }: { key: string; value: string }) =>
      configApi.update(key, value),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'systemConfigs'] });
    },
  });
}

// ==================== Helpers ====================

function isRecentlyModified(updatedAt: string | null): boolean {
  if (!updatedAt) return false;
  const updated = new Date(updatedAt).getTime();
  const now = Date.now();
  // Consider "modified" if updated within the last 24 hours
  return now - updated < 24 * 60 * 60 * 1000;
}

function formatValue(config: SystemConfig): React.ReactNode {
  if (config.valueType === 'BOOLEAN') {
    const isTrue =
      config.configValue === 'true' || config.configValue === '1';
    return (
      <span
        className={`sysconfig-value sysconfig-value-boolean ${
          isTrue ? 'sysconfig-value-true' : 'sysconfig-value-false'
        }`}
      >
        {isTrue ? 'true' : 'false'}
      </span>
    );
  }
  if (config.valueType === 'JSON') {
    // Truncate long JSON
    const display =
      config.configValue.length > 60
        ? config.configValue.substring(0, 60) + '...'
        : config.configValue;
    return <span className="sysconfig-value">{display}</span>;
  }
  return <span className="sysconfig-value">{config.configValue}</span>;
}

// ==================== Component ====================

export default function SystemConfigPage() {
  const { data: configs, isLoading, isError } = useSystemConfigs();
  const updateMutation = useUpdateSystemConfig();

  const [activeCategory, setActiveCategory] = useState<CategoryFilter>('ALL');
  const [search, setSearch] = useState('');
  const [editingConfig, setEditingConfig] = useState<SystemConfig | null>(null);
  const [editValue, setEditValue] = useState('');
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Count entries per category
  const categoryCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    (configs || []).forEach((c) => {
      counts[c.category] = (counts[c.category] || 0) + 1;
    });
    return counts;
  }, [configs]);

  // Filter configs by category and search
  const filteredConfigs = useMemo(() => {
    let list = configs || [];
    if (activeCategory !== 'ALL') {
      list = list.filter((c) => c.category === activeCategory);
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (c) =>
          c.configKey.toLowerCase().includes(q) ||
          (c.description && c.description.toLowerCase().includes(q)) ||
          c.configValue.toLowerCase().includes(q)
      );
    }
    // Sort by category then key
    return [...list].sort((a, b) => {
      const catCmp = a.category.localeCompare(b.category);
      if (catCmp !== 0) return catCmp;
      return a.configKey.localeCompare(b.configKey);
    });
  }, [configs, activeCategory, search]);

  // Group by category for display when showing ALL
  const groupedConfigs = useMemo(() => {
    if (activeCategory !== 'ALL') {
      return { [activeCategory]: filteredConfigs };
    }
    const groups: Record<string, SystemConfig[]> = {};
    filteredConfigs.forEach((c) => {
      if (!groups[c.category]) groups[c.category] = [];
      groups[c.category].push(c);
    });
    return groups;
  }, [filteredConfigs, activeCategory]);

  const totalCount = configs?.length ?? 0;
  const editableCount = configs?.filter((c) => c.isEditable).length ?? 0;
  const categoriesInUse = Object.keys(categoryCounts).length;

  // --- Modal handlers ---

  const openEditModal = (config: SystemConfig) => {
    if (!config.isEditable) return;
    setEditingConfig(config);
    setEditValue(config.configValue);
    setActionError(null);
  };

  const closeEditModal = () => {
    setEditingConfig(null);
    setEditValue('');
    setActionError(null);
  };

  const handleSave = () => {
    if (!editingConfig) return;

    // Validate based on type
    if (editingConfig.valueType === 'INTEGER') {
      const parsed = Number(editValue);
      if (isNaN(parsed) || !Number.isInteger(parsed)) {
        setActionError('Value must be a valid integer.');
        return;
      }
    }
    if (editingConfig.valueType === 'BOOLEAN') {
      if (editValue !== 'true' && editValue !== 'false') {
        setActionError('Value must be "true" or "false".');
        return;
      }
    }
    if (editingConfig.valueType === 'JSON') {
      try {
        JSON.parse(editValue);
      } catch {
        setActionError('Value must be valid JSON.');
        return;
      }
    }

    updateMutation.mutate(
      { key: editingConfig.configKey, value: editValue },
      {
        onSuccess: () => {
          setActionSuccess(
            `Configuration "${editingConfig.configKey}" updated successfully.`
          );
          closeEditModal();
          setTimeout(() => setActionSuccess(null), 4000);
        },
        onError: (err: unknown) => {
          const msg =
            (err as { response?: { data?: { message?: string } } })?.response
              ?.data?.message ||
            (err instanceof Error ? err.message : 'Failed to update configuration');
          setActionError(msg);
        },
      }
    );
  };

  // --- Render edit input based on type ---

  const renderEditInput = () => {
    if (!editingConfig) return null;

    switch (editingConfig.valueType) {
      case 'BOOLEAN':
        return (
          <div className="admin-form-group">
            <label className="admin-form-label">Value</label>
            <div className="sysconfig-toggle-row">
              <label className="toggle-switch">
                <input
                  type="checkbox"
                  checked={editValue === 'true'}
                  onChange={(e) =>
                    setEditValue(e.target.checked ? 'true' : 'false')
                  }
                />
                <span className="toggle-slider"></span>
              </label>
              <span className="sysconfig-toggle-label">
                {editValue === 'true' ? 'Enabled (true)' : 'Disabled (false)'}
              </span>
            </div>
          </div>
        );

      case 'INTEGER':
        return (
          <div className="admin-form-group">
            <label className="admin-form-label">Value</label>
            <input
              className="admin-form-input"
              type="number"
              value={editValue}
              onChange={(e) => setEditValue(e.target.value)}
              step="1"
            />
          </div>
        );

      case 'JSON':
        return (
          <div className="admin-form-group">
            <label className="admin-form-label">Value (JSON)</label>
            <textarea
              className="admin-form-input sysconfig-json-input"
              value={editValue}
              onChange={(e) => setEditValue(e.target.value)}
              rows={8}
              spellCheck={false}
            />
          </div>
        );

      default: // STRING
        return (
          <div className="admin-form-group">
            <label className="admin-form-label">Value</label>
            <input
              className="admin-form-input"
              type="text"
              value={editValue}
              onChange={(e) => setEditValue(e.target.value)}
            />
          </div>
        );
    }
  };

  // --- Render table for a group of configs ---

  const renderConfigTable = (items: SystemConfig[]) => (
    <div className="admin-table-container">
      <table className="admin-table">
        <thead>
          <tr>
            <th style={{ minWidth: 260 }}>Configuration Key</th>
            <th style={{ minWidth: 180 }}>Value</th>
            <th style={{ width: 90 }}>Type</th>
            <th style={{ width: 80 }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map((config) => (
            <tr
              key={config.id}
              className={!config.isEditable ? 'sysconfig-row-locked' : ''}
            >
              <td>
                <div className="sysconfig-key-cell">
                  <div>
                    {!config.isEditable && (
                      <span className="sysconfig-lock-icon">
                        <Lock size={12} />
                      </span>
                    )}
                    <span className="sysconfig-key">{config.configKey}</span>
                    {isRecentlyModified(config.updatedAt) && (
                      <span className="sysconfig-modified-badge">Modified</span>
                    )}
                  </div>
                  {config.description && (
                    <span className="sysconfig-description">
                      {config.description}
                    </span>
                  )}
                  {config.updatedBy && config.updatedAt && (
                    <span className="sysconfig-meta">
                      Last updated by {config.updatedBy}
                    </span>
                  )}
                </div>
              </td>
              <td>{formatValue(config)}</td>
              <td>
                <span
                  className={`sysconfig-type-badge type-${config.valueType}`}
                >
                  {config.valueType}
                </span>
              </td>
              <td>
                <div className="sysconfig-actions">
                  <button
                    className="sysconfig-edit-btn"
                    onClick={() => openEditModal(config)}
                    disabled={!config.isEditable}
                    title={
                      config.isEditable
                        ? `Edit ${config.configKey}`
                        : 'This setting is system-managed and cannot be edited'
                    }
                  >
                    {config.isEditable ? (
                      <Edit2 size={14} />
                    ) : (
                      <Lock size={14} />
                    )}
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">System Configuration</h1>
          <p className="admin-page-description">
            Manage dynamic system configuration settings. These key-value pairs
            control default values, thresholds, and behavior across the
            application.
          </p>
        </div>

        {/* Stats */}
        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-value">{totalCount}</div>
            <div className="admin-stat-label">Total Settings</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{editableCount}</div>
            <div className="admin-stat-label">Editable</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{totalCount - editableCount}</div>
            <div className="admin-stat-label">System-managed</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{categoriesInUse}</div>
            <div className="admin-stat-label">Categories</div>
          </div>
        </div>

        {/* Alerts */}
        {actionSuccess && (
          <div className="admin-alert admin-alert-success">
            {actionSuccess}
            <button
              type="button"
              className="admin-alert-dismiss"
              onClick={() => setActionSuccess(null)}
            >
              &times;
            </button>
          </div>
        )}
        {actionError && !editingConfig && (
          <div className="admin-alert admin-alert-error">
            {actionError}
            <button
              type="button"
              className="admin-alert-dismiss"
              onClick={() => setActionError(null)}
            >
              &times;
            </button>
          </div>
        )}

        {/* Toolbar with search */}
        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search by key, description, or value..."
              className="admin-search-input"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="admin-toolbar-right">
            <span style={{ fontSize: 13, color: 'var(--sa-n500)' }}>
              {filteredConfigs.length} of {totalCount} entries
            </span>
          </div>
        </div>

        {/* Category Tabs */}
        <div className="sysconfig-category-tabs">
          {ALL_CATEGORIES.map((cat) => {
            const count = cat === 'ALL' ? totalCount : categoryCounts[cat] || 0;
            // Only show categories that have entries (or ALL)
            if (cat !== 'ALL' && count === 0) return null;
            return (
              <button
                key={cat}
                className={`sysconfig-category-tab ${
                  activeCategory === cat ? 'active' : ''
                }`}
                onClick={() => setActiveCategory(cat)}
              >
                {cat === 'ALL' ? 'All' : cat}
                <span className="sysconfig-tab-count">{count}</span>
              </button>
            );
          })}
        </div>

        {/* Config Tables */}
        {isLoading ? (
          <div className="admin-table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Configuration Key</th>
                  <th>Value</th>
                  <th>Type</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {[...Array(8)].map((_, i) => (
                  <tr key={i}>
                    <td style={{ padding: '12px 16px' }}>
                      <div
                        className="ab-skeleton"
                        style={{
                          height: 16,
                          width: '70%',
                          borderRadius: 'var(--sa-radius-sm)',
                        }}
                      />
                      <div
                        className="ab-skeleton"
                        style={{
                          height: 12,
                          width: '50%',
                          borderRadius: 'var(--sa-radius-sm)',
                          marginTop: 6,
                        }}
                      />
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <div
                        className="ab-skeleton"
                        style={{
                          height: 16,
                          width: '60%',
                          borderRadius: 'var(--sa-radius-sm)',
                        }}
                      />
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <div
                        className="ab-skeleton"
                        style={{
                          height: 16,
                          width: 60,
                          borderRadius: 'var(--sa-radius-sm)',
                        }}
                      />
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <div
                        className="ab-skeleton"
                        style={{
                          height: 16,
                          width: 32,
                          borderRadius: 'var(--sa-radius-sm)',
                        }}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : isError ? (
          <div className="admin-alert admin-alert-error">
            Failed to load system configuration. Please check if the admin
            service is running.
          </div>
        ) : filteredConfigs.length === 0 ? (
          <div className="sysconfig-empty">
            <div className="sysconfig-empty-icon">
              <Settings size={48} strokeWidth={1} />
            </div>
            <div className="sysconfig-empty-text">
              {search
                ? 'No configuration entries match your search.'
                : 'No configuration entries found for this category.'}
            </div>
          </div>
        ) : activeCategory === 'ALL' ? (
          // Grouped display when viewing all
          Object.entries(groupedConfigs)
            .sort(([a], [b]) => a.localeCompare(b))
            .map(([category, items]) => (
              <div key={category} style={{ marginBottom: 24 }}>
                <div className="sysconfig-section-header">
                  <span className="sysconfig-section-title">{category}</span>
                  <span className="sysconfig-section-count">
                    ({items.length}{' '}
                    {items.length === 1 ? 'entry' : 'entries'})
                  </span>
                </div>
                {renderConfigTable(items)}
              </div>
            ))
        ) : (
          renderConfigTable(filteredConfigs)
        )}

        {/* Edit Modal */}
        {editingConfig && (
          <div className="admin-modal-overlay" onClick={closeEditModal}>
            <div
              className="admin-modal"
              onClick={(e) => e.stopPropagation()}
              style={{ maxWidth: 560 }}
            >
              <div className="admin-modal-header">
                <h3>Edit Configuration</h3>
                <button onClick={closeEditModal}>
                  <X size={18} />
                </button>
              </div>
              <div className="admin-modal-body">
                {actionError && (
                  <div
                    className="admin-alert admin-alert-error"
                    style={{ marginBottom: 16 }}
                  >
                    {actionError}
                  </div>
                )}

                {/* Key (read-only) */}
                <div className="admin-form-group">
                  <label className="admin-form-label">Key</label>
                  <div
                    style={{
                      fontFamily:
                        "'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace",
                      fontSize: 13,
                      padding: '8px 12px',
                      background: 'var(--sa-n50, #fafbfc)',
                      border: '1px solid var(--sa-n200)',
                      borderRadius: 3,
                      color: 'var(--sa-n700)',
                      wordBreak: 'break-all',
                    }}
                  >
                    {editingConfig.configKey}
                  </div>
                </div>

                {/* Description */}
                {editingConfig.description && (
                  <div className="admin-form-group">
                    <label className="admin-form-label">Description</label>
                    <p
                      style={{
                        margin: 0,
                        fontSize: 13,
                        color: 'var(--sa-n600)',
                        lineHeight: 1.5,
                      }}
                    >
                      {editingConfig.description}
                    </p>
                  </div>
                )}

                {/* Current value */}
                <div style={{ marginBottom: 16 }}>
                  <div className="sysconfig-current-value-label">
                    Current Value
                  </div>
                  <div className="sysconfig-current-value">
                    {editingConfig.configValue || '(empty)'}
                  </div>
                </div>

                {/* Type badge */}
                <div className="admin-form-group">
                  <label className="admin-form-label">Type</label>
                  <span
                    className={`sysconfig-type-badge type-${editingConfig.valueType}`}
                  >
                    {editingConfig.valueType}
                  </span>
                </div>

                {/* Value input */}
                {renderEditInput()}
              </div>
              <div className="admin-modal-footer">
                <button
                  className="admin-btn-secondary"
                  onClick={closeEditModal}
                >
                  Cancel
                </button>
                <button
                  className="admin-btn-primary"
                  onClick={handleSave}
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending ? 'Saving...' : 'Save'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
