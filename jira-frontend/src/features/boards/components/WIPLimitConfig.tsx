import React, { useState, useCallback } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import boardApi from '../../../api/boardApi';
import type { BoardColumn } from '../../../api/boardApi';

interface WIPLimitConfigProps {
  boardId: string;
  columns: BoardColumn[];
  onClose: () => void;
}

export default function WIPLimitConfig({ boardId, columns, onClose }: WIPLimitConfigProps) {
  const queryClient = useQueryClient();
  const [localColumns, setLocalColumns] = useState<BoardColumn[]>(columns);

  const updateColumn = useMutation({
    mutationFn: ({ columnId, maxIssues }: { columnId: string; maxIssues?: number }) =>
      boardApi.updateColumn(boardId, columnId, { maxIssues }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board-config', boardId] });
    },
  });

  const handleWipChange = useCallback((columnId: string, value: string) => {
    const maxIssues = value === '' ? undefined : parseInt(value, 10);
    setLocalColumns((cols) =>
      cols.map((c) => (c.id === columnId ? { ...c, maxIssues } : c)),
    );
  }, []);

  const handleSave = useCallback(() => {
    localColumns.forEach((col) => {
      updateColumn.mutate({ columnId: col.id, maxIssues: col.maxIssues });
    });
    onClose();
  }, [localColumns, onClose]);

  return (
    <div className="sa-wip-config">
      <h3>WIP Limits</h3>
      <p className="sa-wip-hint">
        Set work-in-progress limits per column. Leave empty for no limit.
      </p>
      <table className="sa-wip-table">
        <thead>
          <tr>
            <th>Column</th>
            <th>WIP Limit</th>
            <th>Current</th>
          </tr>
        </thead>
        <tbody>
          {localColumns.map((col) => (
            <tr key={col.id}>
              <td>{col.name}</td>
              <td>
                <input
                  type="number"
                  min="1"
                  max="100"
                  placeholder="No limit"
                  value={col.maxIssues ?? ''}
                  onChange={(e) => handleWipChange(col.id, e.target.value)}
                />
              </td>
              <td className="sa-wip-current">{col.currentIssues ?? 0}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="sa-wip-actions">
        <button className="sa-btn sa-btn-secondary" onClick={onClose}>Cancel</button>
        <button className="sa-btn sa-btn-primary" onClick={handleSave}>Save</button>
      </div>
    </div>
  );
}
