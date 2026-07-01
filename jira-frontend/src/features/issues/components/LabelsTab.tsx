import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { labelApi, LabelResponse } from '../../../api/labelApi';

interface LabelsTabProps {
  issueId: string;
}

const LABEL_COLORS = [
  '#0066ff', '#28a745', '#dc3545', '#ffc107',
  '#17a2b8', '#6f42c1', '#fd7e14', '#e83e8c',
];

function getColorForLabel(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return LABEL_COLORS[Math.abs(hash) % LABEL_COLORS.length];
}

export default function LabelsTab({ issueId }: LabelsTabProps) {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [newLabel, setNewLabel] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const { data: labels, isLoading } = useQuery<LabelResponse[]>({
    queryKey: ['labels', issueId],
    queryFn: async () => {
      const response = await labelApi.getAll(issueId);
      return response.data;
    },
    enabled: !!issueId,
  });

  const { data: suggestions } = useQuery<LabelResponse[]>({
    queryKey: ['label-suggestions', issueId, searchQuery],
    queryFn: async () => {
      const response = await labelApi.search(issueId, searchQuery);
      return response.data;
    },
    enabled: !!issueId && searchQuery.length > 1,
  });

  const addMutation = useMutation({
    mutationFn: (name: string) => labelApi.add(issueId, name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['labels', issueId] });
      setNewLabel('');
      setSearchQuery('');
      setShowForm(false);
    },
  });

  const removeMutation = useMutation({
    mutationFn: (labelName: string) => labelApi.remove(issueId, labelName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['labels', issueId] });
    },
  });

  const handleAddLabel = (name: string) => {
    const trimmedName = name.trim().toLowerCase();
    if (trimmedName && !labels?.some(l => l.name === trimmedName)) {
      addMutation.mutate(trimmedName);
    }
  };

  const filteredSuggestions = suggestions?.filter(
    s => !labels?.some(l => l.name === s.name)
  );

  return (
    <div className="ab-labels-tab">
      <div className="ab-section-header">
        <div className="ab-section-info">
          <h3>Labels</h3>
          {labels && labels.length > 0 && (
            <span className="ab-label-count">{labels.length} label{labels.length !== 1 ? 's' : ''}</span>
          )}
        </div>
        <button
          className="ab-btn ab-btn-primary ab-btn-sm"
          onClick={() => setShowForm(!showForm)}
        >
          {showForm ? 'Cancel' : 'Add Label'}
        </button>
      </div>

      {showForm && (
        <div className="ab-label-form ab-card">
          <div className="ab-card-body">
            <div className="ab-form-group">
              <label className="ab-label">Label Name</label>
              <div className="ab-label-input-group">
                <input
                  type="text"
                  className="ab-input"
                  placeholder="Enter label name"
                  value={newLabel}
                  onChange={(e) => {
                    setNewLabel(e.target.value);
                    setSearchQuery(e.target.value);
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      handleAddLabel(newLabel);
                    }
                  }}
                />
                <button
                  className="ab-btn ab-btn-primary"
                  onClick={() => handleAddLabel(newLabel)}
                  disabled={!newLabel.trim() || addMutation.isPending}
                >
                  Add
                </button>
              </div>
              {filteredSuggestions && filteredSuggestions.length > 0 && (
                <div className="ab-label-suggestions">
                  <span className="ab-suggestions-label">Existing labels:</span>
                  {filteredSuggestions.slice(0, 5).map((suggestion) => (
                    <button
                      key={suggestion.id}
                      className="ab-chip"
                      onClick={() => handleAddLabel(suggestion.name)}
                      type="button"
                    >
                      {suggestion.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
        </div>
      ) : labels && labels.length > 0 ? (
        <div className="ab-labels-list">
          {labels.map((label) => (
            <span
              key={label.id}
              className="ab-label-chip"
              style={{ '--label-color': getColorForLabel(label.name) } as React.CSSProperties}
            >
              {label.name}
              <button
                className="ab-chip-remove"
                onClick={() => {
                  if (confirm(`Remove label "${label.name}"?`)) {
                    removeMutation.mutate(label.name);
                  }
                }}
                title="Remove label"
              >
                ×
              </button>
            </span>
          ))}
        </div>
      ) : (
        <div className="ab-empty-state">
          <div className="ab-empty-state-icon">🏷️</div>
          <p className="ab-empty-state-description">No labels assigned</p>
        </div>
      )}

      <style>{`
        .ab-labels-tab {
          padding: var(--ab-spacing-md) 0;
        }

        .ab-section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-section-info {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
        }

        .ab-section-info h3 {
          font-size: var(--ab-font-size-base);
          font-weight: 600;
          margin: 0;
        }

        .ab-label-count {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
        }

        .ab-label-form {
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-label-input-group {
          display: flex;
          gap: var(--ab-spacing-sm);
        }

        .ab-label-input-group .ab-input {
          flex: 1;
        }

        .ab-label-suggestions {
          display: flex;
          flex-wrap: wrap;
          align-items: center;
          gap: var(--ab-spacing-sm);
          margin-top: var(--ab-spacing-sm);
        }

        .ab-suggestions-label {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
        }

        .ab-labels-list {
          display: flex;
          flex-wrap: wrap;
          gap: var(--ab-spacing-sm);
        }

        .ab-label-chip {
          display: inline-flex;
          align-items: center;
          gap: 0.25rem;
          padding: 0.25rem 0.5rem;
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          background: color-mix(in srgb, var(--label-color) 15%, transparent);
          color: var(--label-color);
          border-radius: var(--ab-radius-full);
          border: 1px solid color-mix(in srgb, var(--label-color) 30%, transparent);
        }
      `}</style>
    </div>
  );
}