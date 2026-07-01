import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import boardApi from '../../../api/boardApi';

interface ShareTarget {
  id: string;
  type: 'user' | 'group';
  name: string;
  permission: 'view' | 'edit' | 'admin';
}

interface SharingPanelProps {
  boardId: string;
  onClose: () => void;
}

export default function SharingPanel({ boardId, onClose }: SharingPanelProps) {
  const queryClient = useQueryClient();
  const [shares, setShares] = useState<ShareTarget[]>([]);
  const [newShare, setNewShare] = useState({ name: '', type: 'user' as const, permission: 'view' as const });
  const [searchResults, setSearchResults] = useState<Array<{ id: string; displayName: string; type: 'user' | 'group' }>>([]);

  const addShare = useMutation({
    mutationFn: (data: ShareTarget) => boardApi.updateBoard(boardId, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board-shares', boardId] });
      setShares((s) => [...s, { ...newShare, id: Date.now().toString() }]);
      setNewShare({ name: '', type: 'user', permission: 'view' });
    },
  });

  const removeShare = useMutation({
    mutationFn: (shareId: string) => boardApi.updateBoard(boardId, {}),
    onSuccess: () => {
      setShares((s) => s.filter((sh) => sh.id !== shareId));
    },
  });

  return (
    <div className="sa-sharing-panel">
      <h3>Share Board</h3>
      <p>Share this board with users or groups.</p>

      <div className="sa-share-add">
        <input
          type="text"
          placeholder="Search users or groups..."
          value={newShare.name}
          onChange={(e) => setNewShare((s) => ({ ...s, name: e.target.value }))}
        />
        <select
          value={newShare.type}
          onChange={(e) => setNewShare((s) => ({ ...s, type: e.target.value as 'user' | 'group' }))}
        >
          <option value="user">User</option>
          <option value="group">Group</option>
        </select>
        <select
          value={newShare.permission}
          onChange={(e) => setNewShare((s) => ({ ...s, permission: e.target.value as 'view' | 'edit' | 'admin' }))}
        >
          <option value="view">Can view</option>
          <option value="edit">Can edit</option>
          <option value="admin">Admin</option>
        </select>
        <button
          className="sa-btn sa-btn-primary"
          onClick={() => newShare.name && addShare.mutate(newShare)}
          disabled={!newShare.name}
        >
          Add
        </button>
      </div>

      <div className="sa-share-list">
        {shares.map((share) => (
          <div key={share.id} className="sa-share-item">
            <span className="sa-share-name">{share.name}</span>
            <span className="sa-share-type">{share.type}</span>
            <select
              value={share.permission}
              onChange={(e) => {
                setShares((s) =>
                  s.map((sh) =>
                    sh.id === share.id
                      ? { ...sh, permission: e.target.value as 'view' | 'edit' | 'admin' }
                      : sh,
                  ),
                );
              }}
            >
              <option value="view">Can view</option>
              <option value="edit">Can edit</option>
              <option value="admin">Admin</option>
            </select>
            <button
              className="sa-btn-icon"
              onClick={() => removeShare.mutate(share.id)}
            >
              ×
            </button>
          </div>
        ))}
      </div>

      <div className="sa-sharing-actions">
        <button className="sa-btn sa-btn-secondary" onClick={onClose}>Done</button>
      </div>
    </div>
  );
}
