import React, { useState } from 'react';
import { BoardColumn, QuickFilter } from '../../../api/boardApi';

type CardLayout = 'FULL' | 'COMPACT' | 'MINI';
type SwimlanField = 'none' | 'epic' | 'assignee' | 'priority' | 'labels' | 'sprint';

interface BoardConfigPanelProps {
  columns: BoardColumn[];
  boardType: 'SCRUM' | 'KANBAN' | 'BADGE';
  swimlaneField: SwimlanField;
  cardColorField: 'none' | 'priority' | 'type' | 'labels' | 'epic';
  showWorkVsCapacity: boolean;
  quickFilters: QuickFilter[];
  onClose: () => void;
  onColumnConfigChange: (columnId: string, config: Partial<BoardColumn>) => void;
  onBoardTypeChange: (type: 'SCRUM' | 'KANBAN' | 'BADGE') => void;
  onSwimlaneFieldChange: (field: SwimlanField) => void;
  onCardColorFieldChange: (field: 'none' | 'priority' | 'type' | 'labels' | 'epic') => void;
  onShowWorkVsCapacityChange: (show: boolean) => void;
  onQuickFiltersChange: (filters: QuickFilter[]) => void;
}

export default function BoardConfigPanel({
  columns,
  boardType,
  swimlaneField,
  cardColorField,
  showWorkVsCapacity,
  quickFilters,
  onClose,
  onColumnConfigChange,
  onBoardTypeChange,
  onSwimlaneFieldChange,
  onCardColorFieldChange,
  onShowWorkVsCapacityChange,
  onQuickFiltersChange,
}: BoardConfigPanelProps) {
  const [activeTab, setActiveTab] = useState<'general' | 'columns' | 'swimlanes' | 'filters'>('general');
  const [editingColumn, setEditingColumn] = useState<BoardColumn | null>(null);
  const [editedColumnName, setEditedColumnName] = useState('');
  const [editedWipLimit, setEditedWipLimit] = useState<string>('');

  const handleEditColumn = (column: BoardColumn) => {
    setEditingColumn(column);
    setEditedColumnName(column.name);
    setEditedWipLimit(column.maxIssues?.toString() || '');
  };

  const handleSaveColumn = () => {
    if (editingColumn) {
      onColumnConfigChange(editingColumn.id, {
        name: editedColumnName,
        maxIssues: editedWipLimit ? parseInt(editedWipLimit) : undefined,
      });
      setEditingColumn(null);
    }
  };

  const handleAddQuickFilter = () => {
    const newFilter: QuickFilter = {
      id: `qf-custom-${Date.now()}`,
      name: 'New Filter',
      jql: '',
    };
    onQuickFiltersChange([...quickFilters, newFilter]);
  };

  const handleRemoveQuickFilter = (filterId: string) => {
    onQuickFiltersChange(quickFilters.filter(f => f.id !== filterId));
  };

  const handleUpdateQuickFilter = (filterId: string, updates: Partial<QuickFilter>) => {
    onQuickFiltersChange(
      quickFilters.map(f => f.id === filterId ? { ...f, ...updates } : f)
    );
  };

  const COLORS = [
    '#dc3545', '#fd7e14', '#ffc107', '#28a745', '#20c997',
    '#0066ff', '#6f42c1', '#6c757d', '#343a40', '#e83e8c'
  ];

  return (
    <div className="ab-config-panel-overlay" onClick={onClose}>
      <div className="ab-config-panel" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ab-config-header">
          <h2>Board Configuration</h2>
          <button className="ab-close-btn" onClick={onClose}>
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
            </svg>
          </button>
        </div>

        {/* Tabs */}
        <div className="ab-config-tabs">
          <button
            className={`ab-config-tab ${activeTab === 'general' ? 'active' : ''}`}
            onClick={() => setActiveTab('general')}
          >
            General
          </button>
          <button
            className={`ab-config-tab ${activeTab === 'columns' ? 'active' : ''}`}
            onClick={() => setActiveTab('columns')}
          >
            Columns
          </button>
          <button
            className={`ab-config-tab ${activeTab === 'swimlanes' ? 'active' : ''}`}
            onClick={() => setActiveTab('swimlanes')}
          >
            Swimlanes
          </button>
          <button
            className={`ab-config-tab ${activeTab === 'filters' ? 'active' : ''}`}
            onClick={() => setActiveTab('filters')}
          >
            Quick Filters
          </button>
        </div>

        {/* Content */}
        <div className="ab-config-content">
          {/* General Tab */}
          {activeTab === 'general' && (
            <div className="ab-config-section">
              {/* Board Type */}
              <div className="ab-form-group">
                <label className="ab-label">Board Type</label>
                <select
                  value={boardType}
                  onChange={(e) => onBoardTypeChange(e.target.value as 'SCRUM' | 'KANBAN' | 'BADGE')}
                  className="ab-select"
                >
                  <option value="SCRUM">Scrum</option>
                  <option value="KANBAN">Kanban</option>
                  <option value="BADGE">Badge</option>
                </select>
                <p className="ab-help-text">
                  Scrum boards support sprints, backlog, and velocity tracking. Kanban boards focus on workflow visualization.
                </p>
              </div>

              {/* Card Layout */}
              <div className="ab-form-group">
                <label className="ab-label">Card Layout</label>
                <select
                  value={cardColorField === 'none' ? 'none' : cardColorField}
                  onChange={(e) => onCardColorFieldChange(e.target.value as any)}
                  className="ab-select"
                >
                  <option value="none">No color</option>
                  <option value="priority">Color by Priority</option>
                  <option value="type">Color by Issue Type</option>
                  <option value="epic">Color by Epic</option>
                </select>
              </div>

              {/* Work vs Capacity */}
              <div className="ab-form-group">
                <label className="ab-checkbox-label">
                  <input
                    type="checkbox"
                    checked={showWorkVsCapacity}
                    onChange={(e) => onShowWorkVsCapacityChange(e.target.checked)}
                    className="ab-checkbox"
                  />
                  <span>Show work vs capacity bars</span>
                </label>
                <p className="ab-help-text">
                  Display capacity bars on columns to visualize work load against WIP limits.
                </p>
              </div>
            </div>
          )}

          {/* Columns Tab */}
          {activeTab === 'columns' && (
            <div className="ab-config-section">
              <div className="ab-column-list">
                {columns.map((column) => (
                  <div key={column.id} className="ab-column-config-item">
                    <div className="ab-column-color" style={{ backgroundColor: column.color }} />
                    <div className="ab-column-info">
                      <span className="ab-column-name">{column.name}</span>
                      {column.maxIssues && (
                        <span className="ab-wip-info">WIP: {column.maxIssues}</span>
                      )}
                    </div>
                    <button
                      className="ab-btn ab-btn-sm ab-btn-secondary"
                      onClick={() => handleEditColumn(column)}
                    >
                      Edit
                    </button>
                  </div>
                ))}
              </div>

              {/* Edit Column Modal */}
              {editingColumn && (
                <div className="ab-modal-overlay">
                  <div className="ab-modal">
                    <div className="ab-modal-header">
                      <h3>Edit Column</h3>
                    </div>
                    <div className="ab-modal-body">
                      <div className="ab-form-group">
                        <label className="ab-label">Column Name</label>
                        <input
                          type="text"
                          value={editedColumnName}
                          onChange={(e) => setEditedColumnName(e.target.value)}
                          className="ab-input"
                        />
                      </div>
                      <div className="ab-form-group">
                        <label className="ab-label">WIP Limit</label>
                        <input
                          type="number"
                          value={editedWipLimit}
                          onChange={(e) => setEditedWipLimit(e.target.value)}
                          className="ab-input"
                          placeholder="No limit"
                          min="0"
                        />
                        <p className="ab-help-text">Leave empty for no WIP limit</p>
                      </div>
                      <div className="ab-form-group">
                        <label className="ab-label">Column Color</label>
                        <div className="ab-color-picker">
                          {COLORS.map((color) => (
                            <button
                              key={color}
                              className={`ab-color-option ${editingColumn.color === color ? 'selected' : ''}`}
                              style={{ backgroundColor: color }}
                              onClick={() => onColumnConfigChange(editingColumn.id, { color })}
                            />
                          ))}
                        </div>
                      </div>
                    </div>
                    <div className="ab-modal-footer">
                      <button className="ab-btn ab-btn-secondary" onClick={() => setEditingColumn(null)}>
                        Cancel
                      </button>
                      <button className="ab-btn ab-btn-primary" onClick={handleSaveColumn}>
                        Save
                      </button>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Swimlanes Tab */}
          {activeTab === 'swimlanes' && (
            <div className="ab-config-section">
              <div className="ab-form-group">
                <label className="ab-label">Group Issues By</label>
                <select
                  value={swimlaneField}
                  onChange={(e) => onSwimlaneFieldChange(e.target.value as SwimlanField)}
                  className="ab-select"
                >
                  <option value="none">No Swimlanes</option>
                  <option value="epic">Epic</option>
                  <option value="assignee">Assignee</option>
                  <option value="priority">Priority</option>
                  <option value="labels">Labels</option>
                  <option value="sprint">Sprint</option>
                </select>
                <p className="ab-help-text">
                  Swimlanes group issues horizontally across columns. Click a swimlane header to collapse/expand it.
                </p>
              </div>

              {swimlaneField !== 'none' && (
                <div className="ab-swimlane-preview">
                  <h4>Preview</h4>
                  <div className="ab-preview-labels">
                    <span className="ab-preview-label">
                      {swimlaneField === 'epic' && '⚡ Epic'}
                      {swimlaneField === 'assignee' && '👤 Assignee'}
                      {swimlaneField === 'priority' && '🔺 Priority'}
                      {swimlaneField === 'labels' && '🏷️ Labels'}
                      {swimlaneField === 'sprint' && '🏃 Sprint'}
                    </span>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Quick Filters Tab */}
          {activeTab === 'filters' && (
            <div className="ab-config-section">
              <div className="ab-filters-list">
                {quickFilters.map((filter) => (
                  <div key={filter.id} className="ab-filter-item">
                    <input
                      type="text"
                      value={filter.name}
                      onChange={(e) => handleUpdateQuickFilter(filter.id, { name: e.target.value })}
                      className="ab-input"
                      placeholder="Filter name"
                    />
                    <input
                      type="text"
                      value={filter.jql}
                      onChange={(e) => handleUpdateQuickFilter(filter.id, { jql: e.target.value })}
                      className="ab-input"
                      placeholder="JQL query"
                    />
                    <button
                      className="ab-btn ab-btn-icon"
                      onClick={() => handleRemoveQuickFilter(filter.id)}
                      title="Remove filter"
                    >
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                        <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
                        <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4L4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
              <button className="ab-btn ab-btn-secondary" onClick={handleAddQuickFilter}>
                + Add Quick Filter
              </button>
              <p className="ab-help-text">
                Quick filters appear as one-click buttons above the board. JQL queries filter visible issues.
              </p>
            </div>
          )}
        </div>

        <style>{`
          .ab-config-panel-overlay {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            align-items: flex-start;
            justify-content: flex-end;
            z-index: 1000;
          }

          .ab-config-panel {
            width: 480px;
            height: 100%;
            background: var(--ab-white);
            box-shadow: -4px 0 20px rgba(0, 0, 0, 0.15);
            display: flex;
            flex-direction: column;
            animation: slideIn 0.2s ease-out;
          }

          @keyframes slideIn {
            from { transform: translateX(100%); }
            to { transform: translateX(0); }
          }

          .ab-config-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: var(--ab-spacing-lg);
            border-bottom: 1px solid var(--ab-gray-200);
          }

          .ab-config-header h2 {
            margin: 0;
            font-size: var(--ab-font-size-lg);
            font-weight: 600;
          }

          .ab-close-btn {
            background: none;
            border: none;
            cursor: pointer;
            color: var(--ab-gray-500);
            padding: var(--ab-spacing-xs);
          }

          .ab-close-btn:hover {
            color: var(--ab-gray-700);
          }

          .ab-config-tabs {
            display: flex;
            border-bottom: 1px solid var(--ab-gray-200);
          }

          .ab-config-tab {
            flex: 1;
            padding: var(--ab-spacing-md);
            background: none;
            border: none;
            font-size: var(--ab-font-size-sm);
            font-weight: 500;
            color: var(--ab-gray-500);
            cursor: pointer;
            border-bottom: 2px solid transparent;
            transition: all var(--ab-transition-fast);
          }

          .ab-config-tab:hover {
            color: var(--ab-gray-700);
          }

          .ab-config-tab.active {
            color: var(--ab-primary-600);
            border-bottom-color: var(--ab-primary-500);
          }

          .ab-config-content {
            flex: 1;
            overflow-y: auto;
            padding: var(--ab-spacing-lg);
          }

          .ab-config-section {
            display: flex;
            flex-direction: column;
            gap: var(--ab-spacing-lg);
          }

          .ab-form-group {
            display: flex;
            flex-direction: column;
            gap: var(--ab-spacing-xs);
          }

          .ab-label {
            font-size: var(--ab-font-size-sm);
            font-weight: 500;
            color: var(--ab-gray-700);
          }

          .ab-select,
          .ab-input {
            padding: var(--ab-spacing-sm);
            border: 1px solid var(--ab-gray-300);
            border-radius: var(--ab-radius-md);
            font-size: var(--ab-font-size-sm);
          }

          .ab-select:focus,
          .ab-input:focus {
            outline: none;
            border-color: var(--ab-primary-500);
            box-shadow: 0 0 0 2px var(--ab-primary-100);
          }

          .ab-help-text {
            font-size: var(--ab-font-size-xs);
            color: var(--ab-gray-500);
            margin: 0;
          }

          .ab-checkbox-label {
            display: flex;
            align-items: center;
            gap: var(--ab-spacing-sm);
            cursor: pointer;
          }

          .ab-checkbox {
            width: 16px;
            height: 16px;
          }

          .ab-column-list {
            display: flex;
            flex-direction: column;
            gap: var(--ab-spacing-sm);
          }

          .ab-column-config-item {
            display: flex;
            align-items: center;
            gap: var(--ab-spacing-md);
            padding: var(--ab-spacing-sm);
            background: var(--ab-gray-50);
            border-radius: var(--ab-radius-md);
          }

          .ab-column-color {
            width: 16px;
            height: 16px;
            border-radius: 50%;
          }

          .ab-column-info {
            flex: 1;
            display: flex;
            flex-direction: column;
          }

          .ab-column-name {
            font-weight: 500;
          }

          .ab-wip-info {
            font-size: var(--ab-font-size-xs);
            color: var(--ab-gray-500);
          }

          .ab-modal-overlay {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1001;
          }

          .ab-modal {
            background: var(--ab-white);
            border-radius: var(--ab-radius-lg);
            width: 400px;
            max-width: 90%;
          }

          .ab-modal-header {
            padding: var(--ab-spacing-lg);
            border-bottom: 1px solid var(--ab-gray-200);
          }

          .ab-modal-header h3 {
            margin: 0;
            font-size: var(--ab-font-size-lg);
          }

          .ab-modal-body {
            padding: var(--ab-spacing-lg);
            display: flex;
            flex-direction: column;
            gap: var(--ab-spacing-md);
          }

          .ab-modal-footer {
            padding: var(--ab-spacing-lg);
            border-top: 1px solid var(--ab-gray-200);
            display: flex;
            justify-content: flex-end;
            gap: var(--ab-spacing-sm);
          }

          .ab-color-picker {
            display: flex;
            gap: var(--ab-spacing-xs);
            flex-wrap: wrap;
          }

          .ab-color-option {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            border: 2px solid transparent;
            cursor: pointer;
          }

          .ab-color-option.selected {
            border-color: var(--ab-gray-800);
          }

          .ab-filters-list {
            display: flex;
            flex-direction: column;
            gap: var(--ab-spacing-sm);
          }

          .ab-filter-item {
            display: flex;
            gap: var(--ab-spacing-sm);
            align-items: center;
          }

          .ab-filter-item .ab-input:first-child {
            width: 150px;
          }

          .ab-filter-item .ab-input:nth-child(2) {
            flex: 1;
          }

          .ab-swimlane-preview {
            padding: var(--ab-spacing-md);
            background: var(--ab-gray-50);
            border-radius: var(--ab-radius-md);
          }

          .ab-swimlane-preview h4 {
            margin: 0 0 var(--ab-spacing-sm);
            font-size: var(--ab-font-size-sm);
            color: var(--ab-gray-600);
          }

          .ab-preview-labels {
            display: flex;
            gap: var(--ab-spacing-sm);
          }

          .ab-preview-label {
            font-size: var(--ab-font-size-sm);
            padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
            background: var(--ab-gray-200);
            border-radius: var(--ab-radius-sm);
          }

          .ab-btn-sm {
            padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
            font-size: var(--ab-font-size-sm);
          }
        `}</style>
      </div>
    </div>
  );
}