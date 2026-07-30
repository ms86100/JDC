import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axiosClient from '../../../api/axiosClient';

interface SavedFiltersProps {
  onSelectFilter: (jql: string) => void;
  onClose: () => void;
}

interface SavedFilter {
  id: string;
  name: string;
  jql: string;
  owner: string;
  isShared: boolean;
  favorite: boolean;
  shareType: string;
}

const SYSTEM_FILTERS = [
  { id: 'all-issues', name: 'All Issues', jql: '' },
  { id: 'my-issues', name: 'My Issues', jql: 'assignee = currentUser()' },
  { id: 'reported-me', name: 'Reported by Me', jql: 'reporter = currentUser()' },
  { id: 'recently-viewed', name: 'Recently Viewed', jql: 'ORDER BY lastViewed DESC' },
  { id: 'open-issues', name: 'Open Issues', jql: 'status NOT IN (Done, Closed)' },
];

export default function SavedFilters({ onSelectFilter, onClose }: SavedFiltersProps) {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'my' | 'shared' | 'system'>('my');
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newFilterName, setNewFilterName] = useState('');
  const [newFilterJql, setNewFilterJql] = useState('');

  // Fetch user's saved filters
  const { data: userFilters, isLoading } = useQuery<SavedFilter[]>({
    queryKey: ['saved-filters', activeTab],
    queryFn: async () => {
      // Mock data - in production, this would call the API
      return [
        { id: '1', name: 'Critical Bugs', jql: 'type = Bug AND priority IN (Highest, High)', owner: 'me', isShared: false, favorite: true, shareType: 'PRIVATE' },
        { id: '2', name: 'Current Sprint', jql: 'sprint = "Sprint 1"', owner: 'me', isShared: true, favorite: false, shareType: 'PROJECT' },
        { id: '3', name: 'Unassigned Tasks', jql: 'type = Task AND assignee is empty', owner: 'me', isShared: false, favorite: false, shareType: 'PRIVATE' },
      ];
    },
  });

  // Create filter mutation
  const createMutation = useMutation({
    mutationFn: async (data: { name: string; jql: string; isShared: boolean }) => {
      // Mock API call
      return { id: Date.now().toString(), ...data, owner: 'me', favorite: false, shareType: 'PRIVATE' };
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-filters'] });
      setShowCreateForm(false);
      setNewFilterName('');
      setNewFilterJql('');
    },
  });

  // Delete filter mutation
  const deleteMutation = useMutation({
    mutationFn: async (filterId: string) => {
      // Mock API call
      console.log('Deleting filter:', filterId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-filters'] });
    },
  });

  // Toggle favorite
  const toggleFavoriteMutation = useMutation({
    mutationFn: async (filterId: string) => {
      console.log('Toggling favorite:', filterId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['saved-filters'] });
    },
  });

  const handleCreateFilter = () => {
    if (!newFilterName.trim() || !newFilterJql.trim()) return;
    createMutation.mutate({
      name: newFilterName,
      jql: newFilterJql,
      isShared: false,
    });
  };

  const getFiltersForTab = () => {
    switch (activeTab) {
      case 'my':
        return userFilters?.filter(f => !f.isShared) || [];
      case 'shared':
        return userFilters?.filter(f => f.isShared) || [];
      case 'system':
        return SYSTEM_FILTERS;
      default:
        return [];
    }
  };

  return (
    <div className="ab-saved-filters-overlay" onClick={onClose}>
      <div className="ab-saved-filters-panel" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ab-panel-header">
          <h2>Saved Filters</h2>
          <button className="ab-close-btn" onClick={onClose}>
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
            </svg>
          </button>
        </div>

        {/* Tabs */}
        <div className="ab-panel-tabs">
          <button
            className={`ab-tab ${activeTab === 'my' ? 'active' : ''}`}
            onClick={() => setActiveTab('my')}
          >
            My Filters
          </button>
          <button
            className={`ab-tab ${activeTab === 'shared' ? 'active' : ''}`}
            onClick={() => setActiveTab('shared')}
          >
            Shared with Me
          </button>
          <button
            className={`ab-tab ${activeTab === 'system' ? 'active' : ''}`}
            onClick={() => setActiveTab('system')}
          >
            System
          </button>
        </div>

        {/* Content */}
        <div className="ab-panel-content">
          {/* Create filter button/form */}
          {activeTab !== 'system' && (
            <div className="ab-create-filter">
              {showCreateForm ? (
                <div className="ab-create-form">
                  <input
                    type="text"
                    placeholder="Filter name"
                    value={newFilterName}
                    onChange={(e) => setNewFilterName(e.target.value)}
                    className="ab-input"
                  />
                  <textarea
                    placeholder="JQL query..."
                    value={newFilterJql}
                    onChange={(e) => setNewFilterJql(e.target.value)}
                    className="ab-textarea"
                    rows={2}
                  />
                  <div className="ab-form-actions">
                    <button
                      className="ab-btn ab-btn-ghost"
                      onClick={() => setShowCreateForm(false)}
                    >
                      Cancel
                    </button>
                    <button
                      className="ab-btn ab-btn-primary"
                      onClick={handleCreateFilter}
                      disabled={!newFilterName.trim() || !newFilterJql.trim()}
                    >
                      Save Filter
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  className="ab-btn ab-btn-secondary ab-btn-block"
                  onClick={() => setShowCreateForm(true)}
                >
                  + Create Filter
                </button>
              )}
            </div>
          )}

          {/* Filter list */}
          <div className="ab-filter-list">
            {isLoading ? (
              <div className="ab-loading">Loading...</div>
            ) : (
              getFiltersForTab().map((filter) => (
                <div key={filter.id} className="ab-filter-item">
                  <div
                    className="ab-filter-info"
                    onClick={() => onSelectFilter(filter.jql)}
                  >
                    <div className="ab-filter-header">
                      <span className="ab-filter-name">{filter.name}</span>
                      {activeTab !== 'system' && 'favorite' in filter && (
                        <button
                          className={`ab-favorite-btn ${filter.favorite ? 'active' : ''}`}
                          onClick={(e) => {
                            e.stopPropagation();
                            toggleFavoriteMutation.mutate(filter.id);
                          }}
                        >
                          {filter.favorite ? '⭐' : '☆'}
                        </button>
                      )}
                    </div>
                    <div className="ab-filter-jql">{filter.jql}</div>
                    {activeTab === 'shared' && 'shareType' in filter && (
                      <span className="ab-filter-share-type">
                        {filter.shareType === 'PROJECT' ? '📁 Project' : '🔒 Private'}
                      </span>
                    )}
                  </div>
                  {activeTab !== 'system' && (
                    <button
                      className="ab-delete-btn"
                      onClick={() => deleteMutation.mutate(filter.id)}
                    >
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                        <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
                        <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4L4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
                      </svg>
                    </button>
                  )}
                </div>
              ))
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="ab-panel-footer">
          <button className="ab-btn ab-btn-ghost">
            Manage Filters
          </button>
        </div>
      </div>

      <style>{`
        .ab-saved-filters-overlay {
          position: fixed;
          inset: 0;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          justify-content: flex-end;
          z-index: 1000;
        }

        .ab-saved-filters-panel {
          width: 480px;
          max-width: 100%;
          height: 100%;
          background: var(--ab-white);
          display: flex;
          flex-direction: column;
          animation: slideIn 0.2s ease-out;
        }

        @keyframes slideIn {
          from { transform: translateX(100%); }
          to { transform: translateX(0); }
        }

        .ab-panel-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: var(--ab-spacing-lg);
          border-bottom: 1px solid var(--ab-gray-200);
        }

        .ab-panel-header h2 {
          margin: 0;
          font-size: var(--ab-font-size-lg);
          font-weight: 600;
        }

        .ab-close-btn {
          background: none;
          border: none;
          cursor: pointer;
          color: var(--ab-gray-500);
        }

        .ab-panel-tabs {
          display: flex;
          border-bottom: 1px solid var(--ab-gray-200);
        }

        .ab-tab {
          flex: 1;
          padding: var(--ab-spacing-md);
          background: none;
          border: none;
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-500);
          cursor: pointer;
          border-bottom: 2px solid transparent;
        }

        .ab-tab.active {
          color: var(--ab-primary-600);
          border-bottom-color: var(--ab-primary-500);
        }

        .ab-panel-content {
          flex: 1;
          overflow-y: auto;
          padding: var(--ab-spacing-md);
        }

        .ab-create-filter {
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-create-form {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-md);
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-md);
        }

        .ab-form-actions {
          display: flex;
          justify-content: flex-end;
          gap: var(--ab-spacing-sm);
        }

        .ab-filter-list {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
        }

        .ab-filter-item {
          display: flex;
          align-items: flex-start;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-md);
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          cursor: pointer;
          transition: all var(--ab-transition-fast);
        }

        .ab-filter-item:hover {
          border-color: var(--ab-primary-300);
          background: var(--ab-primary-50);
        }

        .ab-filter-info {
          flex: 1;
        }

        .ab-filter-header {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
        }

        .ab-filter-name {
          font-weight: 500;
          color: var(--ab-gray-800);
        }

        .ab-favorite-btn {
          background: none;
          border: none;
          cursor: pointer;
          font-size: var(--ab-font-size-sm);
        }

        .ab-filter-jql {
          font-family: var(--ab-font-mono);
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
          margin-top: var(--ab-spacing-xs);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .ab-filter-share-type {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-400);
          margin-top: var(--ab-spacing-xs);
        }

        .ab-delete-btn {
          background: none;
          border: none;
          cursor: pointer;
          color: var(--ab-gray-400);
          padding: var(--ab-spacing-xs);
        }

        .ab-delete-btn:hover {
          color: var(--ab-danger-500);
        }

        .ab-panel-footer {
          padding: var(--ab-spacing-md);
          border-top: 1px solid var(--ab-gray-200);
        }
      `}</style>
    </div>
  );
}