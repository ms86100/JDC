import { useState } from 'react';
import { useBacklog, useAddItemToBacklog, useRemoveItemFromBacklog } from '../../hooks/useBacklog';
import { CreatePlanItemRequest } from '../../../../api/planApi';

interface BacklogViewProps {
  planId: string;
}

export default function BacklogView({ planId }: BacklogViewProps) {
  const [showAddItem, setShowAddItem] = useState(false);
  const [issueId, setIssueId] = useState('');
  const [issueType, setIssueType] = useState<'EPIC' | 'STORY' | 'SUBTASK'>('STORY');
  const [parentId, setParentId] = useState('');

  const { data: backlog, isLoading } = useBacklog(planId);
  const addItemMutation = useAddItemToBacklog();
  const removeItemMutation = useRemoveItemFromBacklog();

  const handleAddItem = (e: React.FormEvent) => {
    e.preventDefault();
    const request: CreatePlanItemRequest = {
      issueId,
      issueType,
      parentId: parentId || undefined,
    };
    addItemMutation.mutate(
      { planId, data: request },
      {
        onSuccess: () => {
          setShowAddItem(false);
          setIssueId('');
          setIssueType('STORY');
          setParentId('');
        },
      }
    );
  };

  const handleRemoveItem = (itemId: string) => {
    if (confirm('Remove this item from backlog?')) {
      removeItemMutation.mutate({ planId, itemId });
    }
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  if (!backlog) {
    return <div className="ab-empty-state"><p>No backlog data</p></div>;
  }

  const epics = backlog.items.filter(item => item.issueType === 'EPIC');
  const stories = backlog.items.filter(item => item.issueType === 'STORY');
  const subtasks = backlog.items.filter(item => item.issueType === 'SUBTASK');

  return (
    <div className="ab-backlog-view">
      <div className="ab-backlog-toolbar">
        <div className="ab-backlog-stats">
          <span className="ab-stat">
            <span className="ab-stat-label">Total:</span>
            <span className="ab-stat-value">{backlog.totalItems}</span>
          </span>
          <span className="ab-stat">
            <span className="ab-stat-label">Epics:</span>
            <span className="ab-stat-value">{backlog.epicCount}</span>
          </span>
          <span className="ab-stat">
            <span className="ab-stat-label">Stories:</span>
            <span className="ab-stat-value">{backlog.storyCount}</span>
          </span>
          <span className="ab-stat">
            <span className="ab-stat-label">Subtasks:</span>
            <span className="ab-stat-value">{backlog.subtaskCount}</span>
          </span>
        </div>
        <button className="ab-btn ab-btn-primary" onClick={() => setShowAddItem(true)}>
          <span className="ab-icon-plus"></span>
          Add Issue
        </button>
      </div>

      <div className="ab-backlog-tree">
        {backlog.items.length === 0 ? (
          <div className="ab-empty-state">
            <div className="ab-empty-state-icon">📋</div>
            <h3 className="ab-empty-state-title">No items in backlog</h3>
            <p className="ab-empty-state-description">Add issues to start building your backlog</p>
          </div>
        ) : (
          <>
            {epics.length > 0 && (
              <div className="ab-backlog-section">
                <div className="ab-backlog-section-header">
                  <span className="ab-badge ab-badge-epic">EPIC</span>
                  <span className="ab-backlog-section-title">Epics ({epics.length})</span>
                </div>
                <div className="ab-backlog-items">
                  {epics.map((item) => (
                    <div key={item.id} className="ab-backlog-item ab-backlog-item-epic">
                      <div className="ab-backlog-item-icon">🔵</div>
                      <div className="ab-backlog-item-content">
                        <div className="ab-backlog-item-key">{item.issueKey || 'No key'}</div>
                        <div className="ab-backlog-item-summary">{item.summary || 'No summary'}</div>
                      </div>
                      <div className="ab-backlog-item-meta">
                        {item.targetDate && (
                          <span className="ab-backlog-item-date">{new Date(item.targetDate).toLocaleDateString()}</span>
                        )}
                        <button className="ab-btn-icon-sm" onClick={() => handleRemoveItem(item.id)}>×</button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {stories.length > 0 && (
              <div className="ab-backlog-section">
                <div className="ab-backlog-section-header">
                  <span className="ab-badge ab-badge-story">STORY</span>
                  <span className="ab-backlog-section-title">Stories ({stories.length})</span>
                </div>
                <div className="ab-backlog-items">
                  {stories.map((item) => (
                    <div key={item.id} className="ab-backlog-item ab-backlog-item-story">
                      <div className="ab-backlog-item-icon">📄</div>
                      <div className="ab-backlog-item-content">
                        <div className="ab-backlog-item-key">{item.issueKey || 'No key'}</div>
                        <div className="ab-backlog-item-summary">{item.summary || 'No summary'}</div>
                      </div>
                      <div className="ab-backlog-item-meta">
                        {item.targetDate && (
                          <span className="ab-backlog-item-date">{new Date(item.targetDate).toLocaleDateString()}</span>
                        )}
                        <button className="ab-btn-icon-sm" onClick={() => handleRemoveItem(item.id)}>×</button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {subtasks.length > 0 && (
              <div className="ab-backlog-section">
                <div className="ab-backlog-section-header">
                  <span className="ab-badge ab-badge-subtask">SUBTASK</span>
                  <span className="ab-backlog-section-title">Subtasks ({subtasks.length})</span>
                </div>
                <div className="ab-backlog-items">
                  {subtasks.map((item) => (
                    <div key={item.id} className="ab-backlog-item ab-backlog-item-subtask">
                      <div className="ab-backlog-item-icon">📝</div>
                      <div className="ab-backlog-item-content">
                        <div className="ab-backlog-item-key">{item.issueKey || 'No key'}</div>
                        <div className="ab-backlog-item-summary">{item.summary || 'No summary'}</div>
                      </div>
                      <div className="ab-backlog-item-meta">
                        <button className="ab-btn-icon-sm" onClick={() => handleRemoveItem(item.id)}>×</button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {showAddItem && (
        <div className="ab-modal-overlay" onClick={() => setShowAddItem(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Add Issue to Backlog</h2>
              <button className="ab-btn-icon" onClick={() => setShowAddItem(false)}>
                <span className="ab-icon-close"></span>
              </button>
            </div>
            <form onSubmit={handleAddItem}>
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Issue ID *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={issueId}
                    onChange={(e) => setIssueId(e.target.value)}
                    placeholder="Enter issue UUID"
                    required
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Issue Type *</label>
                  <select
                    className="ab-select"
                    value={issueType}
                    onChange={(e) => setIssueType(e.target.value as 'EPIC' | 'STORY' | 'SUBTASK')}
                  >
                    <option value="EPIC">Epic</option>
                    <option value="STORY">Story</option>
                    <option value="SUBTASK">Subtask</option>
                  </select>
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Parent Issue ID (optional)</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={parentId}
                    onChange={(e) => setParentId(e.target.value)}
                    placeholder="For Stories/Subtasks, enter parent Epic/Story ID"
                  />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowAddItem(false)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={addItemMutation.isPending}>
                  {addItemMutation.isPending ? 'Adding...' : 'Add Issue'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
