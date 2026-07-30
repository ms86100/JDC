import React, { useState } from 'react';
import type { BoardColumn } from '../../../api/boardApi';

interface WorkflowStatus {
  id: string;
  name: string;
  category: 'TODO' | 'IN_PROGRESS' | 'DONE';
}

interface StatusWorkflowConfigProps {
  columns: BoardColumn[];
  onClose: () => void;
  onSave: (columns: BoardColumn[]) => void;
}

export default function StatusWorkflowConfig({ columns, onClose, onSave }: StatusWorkflowConfigProps) {
  const [localColumns, setLocalColumns] = useState<BoardColumn[]>(columns);

  const availableStatuses: WorkflowStatus[] = [
    { id: 'todo', name: 'To Do', category: 'TODO' },
    { id: 'in-progress', name: 'In Progress', category: 'IN_PROGRESS' },
    { id: 'in-review', name: 'In Review', category: 'IN_PROGRESS' },
    { id: 'blocked', name: 'Blocked', category: 'IN_PROGRESS' },
    { id: 'done', name: 'Done', category: 'DONE' },
    { id: 'closed', name: 'Closed', category: 'DONE' },
    { id: 'backlog', name: 'Backlog', category: 'TODO' },
  ];

  const handleColumnStatusChange = (columnId: string, statusId: string) => {
    const status = availableStatuses.find((s) => s.id === statusId);
    setLocalColumns((cols) =>
      cols.map((c) =>
        c.id === columnId
          ? {
              ...c,
              status: status?.name ?? statusId,
              statusCategory: status?.category ?? 'TODO',
            }
          : c,
      ),
    );
  };

  const handleColorChange = (columnId: string, color: string) => {
    setLocalColumns((cols) =>
      cols.map((c) => (c.id === columnId ? { ...c, color } : c)),
    );
  };

  return (
    <div className="sa-workflow-config">
      <h3>Status & Workflow</h3>
      <p>Configure column statuses and workflow categories.</p>

      <table className="sa-workflow-table">
        <thead>
          <tr>
            <th>Column</th>
            <th>Status</th>
            <th>Category</th>
            <th>Color</th>
          </tr>
        </thead>
        <tbody>
          {localColumns.map((col) => (
            <tr key={col.id}>
              <td>
                <strong>{col.name}</strong>
              </td>
              <td>
                <select
                  value={col.status || 'todo'}
                  onChange={(e) => handleColumnStatusChange(col.id, e.target.value)}
                >
                  {availableStatuses.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </td>
              <td>
                <select
                  value={col.statusCategory || 'TODO'}
                  onChange={(e) => {
                    setLocalColumns((cols) =>
                      cols.map((c) =>
                        c.id === col.id
                          ? { ...c, statusCategory: e.target.value as 'TODO' | 'IN_PROGRESS' | 'DONE' }
                          : c,
                      ),
                    );
                  }}
                >
                  <option value="TODO">To Do</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="DONE">Done</option>
                </select>
              </td>
              <td>
                <input
                  type="color"
                  value={col.color || '#0065ff'}
                  onChange={(e) => handleColorChange(col.id, e.target.value)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="sa-workflow-info">
        <h4>Workflow Categories</h4>
        <ul>
          <li><strong>To Do:</strong> Issues ready to be worked on</li>
          <li><strong>In Progress:</strong> Issues currently being worked on</li>
          <li><strong>Done:</strong> Completed issues</li>
        </ul>
      </div>

      <div className="sa-workflow-actions">
        <button className="sa-btn sa-btn-secondary" onClick={onClose}>Cancel</button>
        <button className="sa-btn sa-btn-primary" onClick={() => onSave(localColumns)}>Save</button>
      </div>
    </div>
  );
}