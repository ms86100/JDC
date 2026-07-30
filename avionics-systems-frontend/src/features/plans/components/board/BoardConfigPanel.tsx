import React, { useState } from 'react';
import { useBoard, useUpdateBoard, useAddColumn, useUpdateColumn, useDeleteColumn, useAddSwimlane, useAddCardColor } from '../../hooks/useBoardConfig';
import type { BoardConfigResponse, CreateBoardConfigRequest } from '../../hooks/useBoardConfig';

interface BoardConfigPanelProps {
  boardId: string;
  onClose: () => void;
}

export default function BoardConfigPanel({ boardId, onClose }: BoardConfigPanelProps) {
  const { data: board, isLoading } = useBoard(boardId);
  const updateBoard = useUpdateBoard();
  const addColumn = useAddColumn();
  const updateColumn = useUpdateColumn();
  const deleteColumn = useDeleteColumn();
  const addSwimlane = useAddSwimlane();
  const addCardColor = useAddCardColor();

  const [activeTab, setActiveTab] = useState<'general' | 'columns' | 'swimlanes' | 'colors'>('general');
  const [editingBoard, setEditingBoard] = useState<CreateBoardConfigRequest>({
    name: board?.name || '',
    boardType: board?.boardType as 'SCRUM' | 'KANBAN' | undefined,
  });
  const [newColumnName, setNewColumnName] = useState('');
  const [newSwimlaneField, setNewSwimlaneField] = useState('NONE');
  const [newColorName, setNewColorName] = useState('');
  const [newColor, setNewColor] = useState('#ff0000');

  if (isLoading) {
    return <div className="ab-config-loading">Loading board configuration...</div>;
  }

  if (!board) {
    return <div className="ab-config-error">Board not found</div>;
  }

  const handleSaveGeneral = () => {
    updateBoard.mutate({ boardId, data: editingBoard });
  };

  const handleAddColumn = () => {
    if (!newColumnName.trim()) return;
    addColumn.mutate({
      boardId,
      data: {
        name: newColumnName,
        statusMapping: [],
        sequence: board.columns?.length || 0,
      },
    });
    setNewColumnName('');
  };

  const handleDeleteColumn = (columnId: string) => {
    if (confirm('Are you sure you want to delete this column?')) {
      deleteColumn.mutate(columnId);
    }
  };

  const handleAddSwimlane = () => {
    addSwimlane.mutate({
      boardId,
      data: {
        name: `Swimlane ${(board.swimlanes?.length || 0) + 1}`,
        groupingField: newSwimlaneField,
        sequence: board.swimlanes?.length || 0,
      },
    });
  };

  const handleAddCardColor = () => {
    addCardColor.mutate({
      boardId,
      data: {
        name: newColorName,
        color: newColor,
        conditions: [],
        sequence: board.cardColors?.length || 0,
      },
    });
    setNewColorName('');
    setNewColor('#ff0000');
  };

  return (
    <div className="ab-board-config-panel">
      <div className="ab-config-header">
        <h2>Board Configuration</h2>
        <button className="ab-btn-close" onClick={onClose}>&times;</button>
      </div>

      <div className="ab-config-tabs">
        <button
          className={`ab-tab ${activeTab === 'general' ? 'ab-active' : ''}`}
          onClick={() => setActiveTab('general')}
        >
          General
        </button>
        <button
          className={`ab-tab ${activeTab === 'columns' ? 'ab-active' : ''}`}
          onClick={() => setActiveTab('columns')}
        >
          Columns ({board.columns?.length || 0})
        </button>
        <button
          className={`ab-tab ${activeTab === 'swimlanes' ? 'ab-active' : ''}`}
          onClick={() => setActiveTab('swimlanes')}
        >
          Swimlanes ({board.swimlanes?.length || 0})
        </button>
        <button
          className={`ab-tab ${activeTab === 'colors' ? 'ab-active' : ''}`}
          onClick={() => setActiveTab('colors')}
        >
          Card Colors ({board.cardColors?.length || 0})
        </button>
      </div>

      <div className="ab-config-content">
        {/* General Settings */}
        {activeTab === 'general' && (
          <div className="ab-config-section">
            <div className="ab-form-group">
              <label>Board Name</label>
              <input
                type="text"
                className="ab-input"
                value={editingBoard.name ?? board.name}
                onChange={(e) => setEditingBoard({ ...editingBoard, name: e.target.value })}
              />
            </div>
            <div className="ab-form-group">
              <label>Board Type</label>
              <select
                className="ab-select"
                value={editingBoard.boardType ?? board.boardType}
                onChange={(e) => setEditingBoard({ ...editingBoard, boardType: e.target.value as 'SCRUM' | 'KANBAN' })}
              >
                <option value="SCRUM">Scrum</option>
                <option value="KANBAN">Kanban</option>
              </select>
            </div>
            <div className="ab-form-group">
              <label>Card Layout</label>
              <select
                className="ab-select"
                value={editingBoard.cardLayoutMode ?? board.cardLayoutMode}
                onChange={(e) => setEditingBoard({ ...editingBoard, cardLayoutMode: e.target.value })}
              >
                <option value="COMPACT">Compact</option>
                <option value="FULL">Full</option>
              </select>
            </div>
            <div className="ab-form-group">
              <label>Default Swimlane</label>
              <select
                className="ab-select"
                value={editingBoard.defaultSwimlane ?? board.defaultSwimlane}
                onChange={(e) => setEditingBoard({ ...editingBoard, defaultSwimlane: e.target.value })}
              >
                <option value="NONE">None</option>
                <option value="EPIC">Epic</option>
                <option value="ASSIGNEE">Assignee</option>
                <option value="PROJECT">Project</option>
                <option value="PRIORITY">Priority</option>
              </select>
            </div>
            <button className="ab-btn ab-btn-primary" onClick={handleSaveGeneral}>
              Save Changes
            </button>
          </div>
        )}

        {/* Columns */}
        {activeTab === 'columns' && (
          <div className="ab-config-section">
            <div className="ab-columns-list">
              {board.columns?.map((column, index) => (
                <div key={column.id} className="ab-column-item">
                  <div className="ab-column-drag-handle">&#8942;</div>
                  <div className="ab-column-info">
                    <span className="ab-column-name">{column.name}</span>
                    <span className="ab-column-statuses">
                      {column.statusMapping?.join(', ') || 'No statuses'}
                    </span>
                    {column.maxIssues && (
                      <span className="ab-column-wip">WIP: {column.maxIssues}</span>
                    )}
                  </div>
                  <button
                    className="ab-btn ab-btn-sm ab-btn-danger"
                    onClick={() => handleDeleteColumn(column.id)}
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
            <div className="ab-add-column">
              <input
                type="text"
                className="ab-input"
                placeholder="New column name"
                value={newColumnName}
                onChange={(e) => setNewColumnName(e.target.value)}
              />
              <button className="ab-btn ab-btn-primary" onClick={handleAddColumn}>
                Add Column
              </button>
            </div>
          </div>
        )}

        {/* Swimlanes */}
        {activeTab === 'swimlanes' && (
          <div className="ab-config-section">
            <div className="ab-swimlanes-list">
              {board.swimlanes?.map(swimlane => (
                <div key={swimlane.id} className="ab-swimlane-item">
                  <span className="ab-swimlane-name">{swimlane.name}</span>
                  <span className="ab-swimlane-grouping">{swimlane.groupingField}</span>
                </div>
              ))}
            </div>
            <div className="ab-add-swimlane">
              <select
                className="ab-select"
                value={newSwimlaneField}
                onChange={(e) => setNewSwimlaneField(e.target.value)}
              >
                <option value="NONE">None</option>
                <option value="EPIC">Epic</option>
                <option value="ASSIGNEE">Assignee</option>
                <option value="PROJECT">Project</option>
                <option value="PRIORITY">Priority</option>
                <option value="LABEL">Label</option>
              </select>
              <button className="ab-btn ab-btn-primary" onClick={handleAddSwimlane}>
                Add Swimlane
              </button>
            </div>
          </div>
        )}

        {/* Card Colors */}
        {activeTab === 'colors' && (
          <div className="ab-config-section">
            <div className="ab-colors-list">
              {board.cardColors?.map(color => (
                <div key={color.id} className="ab-color-item">
                  <div className="ab-color-preview" style={{ backgroundColor: color.color }} />
                  <span className="ab-color-name">{color.name}</span>
                </div>
              ))}
            </div>
            <div className="ab-add-color">
              <input
                type="text"
                className="ab-input"
                placeholder="Color name"
                value={newColorName}
                onChange={(e) => setNewColorName(e.target.value)}
              />
              <input
                type="color"
                className="ab-color-picker"
                value={newColor}
                onChange={(e) => setNewColor(e.target.value)}
              />
              <button className="ab-btn ab-btn-primary" onClick={handleAddCardColor}>
                Add Color Rule
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}