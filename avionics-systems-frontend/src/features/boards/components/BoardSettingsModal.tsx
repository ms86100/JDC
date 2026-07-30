import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import boardApi from '../../../api/boardApi';
import type { AgileBoard } from '../../../api/boardApi';

interface BoardSettingsModalProps {
  board: AgileBoard;
  onClose: () => void;
  onSaved?: (board: AgileBoard) => void;
}

export default function BoardSettingsModal({ board, onClose, onSaved }: BoardSettingsModalProps) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(board.name || '');
  const [description, setDescription] = useState(board.description || '');
  const [cardLayout, setCardLayout] = useState<'FULL' | 'COMPACT' | 'MINI'>(board.cardLayout || 'FULL');

  const updateMutation = useMutation({
    mutationFn: (data: Partial<AgileBoard>) => boardApi.updateBoard(board.id, data),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['board-data', board.id] });
      queryClient.invalidateQueries({ queryKey: ['boards'] });
      onSaved?.(updated);
      onClose();
    },
  });

  const handleSave = () => {
    updateMutation.mutate({ name, description, cardLayout });
  };

  return (
    <div className="sa-modal-overlay" onClick={onClose}>
      <div className="sa-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="sa-modal-header">
          <h2>Board Settings</h2>
          <button className="sa-modal-close" onClick={onClose}>×</button>
        </div>
        <div className="sa-modal-body">
          <div className="sa-form-group">
            <label>Board Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Board name"
            />
          </div>
          <div className="sa-form-group">
            <label>Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Description (optional)"
              rows={3}
            />
          </div>
          <div className="sa-form-group">
            <label>Card Layout</label>
            <select value={cardLayout} onChange={(e) => setCardLayout(e.target.value as 'FULL' | 'COMPACT' | 'MINI')}>
              <option value="FULL">Full Cards</option>
              <option value="COMPACT">Compact Cards</option>
              <option value="MINI">Mini Cards</option>
            </select>
          </div>
        </div>
        <div className="sa-modal-footer">
          <button className="sa-btn sa-btn-secondary" onClick={onClose}>Cancel</button>
          <button className="sa-btn sa-btn-primary" onClick={handleSave} disabled={updateMutation.isPending}>
            {updateMutation.isPending ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
}
