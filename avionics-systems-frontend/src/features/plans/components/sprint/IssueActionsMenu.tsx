import React, { useState, useRef, useEffect } from 'react';
import { SprintIssueResponse } from '../../hooks/useSprint';

interface IssueActionsMenuProps {
  issue: SprintIssueResponse;
  sprintId: string;
  onRankToTop: (planItemId: string) => void;
  onRankToBottom: (planItemId: string) => void;
  onAssignToMe: (planItemId: string) => void;
  onMove: (planItemId: string) => void;
  onClone: (planItemId: string) => void;
  onCreateSubTask: (planItemId: string) => void;
  onArchive: (planItemId: string) => void;
  position?: { x: number; y: number };
}

export default function IssueActionsMenu({
  issue,
  sprintId,
  onRankToTop,
  onRankToBottom,
  onAssignToMe,
  onMove,
  onClone,
  onCreateSubTask,
  onArchive,
}: IssueActionsMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const handleAction = (action: () => void) => {
    action();
    setIsOpen(false);
  };

  if (!isOpen) {
    return (
      <button
        className="ab-btn ab-btn-sm ab-btn-icon"
        onClick={() => setIsOpen(true)}
        title="More actions"
      >
        ...
      </button>
    );
  }

  return (
    <div className="ab-issue-actions-menu" ref={menuRef}>
      <div className="ab-menu-header">
        <span className="ab-menu-issue-key">{issue.issueId?.slice(0, 12)}</span>
      </div>

      <div className="ab-menu-section">
        <button className="ab-menu-item" onClick={() => handleAction(() => onRankToTop(issue.planItemId))}>
          <span className="ab-menu-icon">↑</span>
          Rank to Top
        </button>
        <button className="ab-menu-item" onClick={() => handleAction(() => onRankToBottom(issue.planItemId))}>
          <span className="ab-menu-icon">↓</span>
          Rank to Bottom
        </button>
      </div>

      <div className="ab-menu-section">
        <button className="ab-menu-item" onClick={() => handleAction(() => onAssignToMe(issue.planItemId))}>
          <span className="ab-menu-icon">👤</span>
          Assign to me
        </button>
        <button className="ab-menu-item" onClick={() => handleAction(() => onMove(issue.planItemId))}>
          <span className="ab-menu-icon">↔</span>
          Move
        </button>
      </div>

      <div className="ab-menu-section">
        <button className="ab-menu-item" onClick={() => handleAction(() => onClone(issue.planItemId))}>
          <span className="ab-menu-icon">📋</span>
          Clone
        </button>
        <button className="ab-menu-item" onClick={() => handleAction(() => onCreateSubTask(issue.planItemId))}>
          <span className="ab-menu-icon">📎</span>
          Create sub-task
        </button>
      </div>

      <div className="ab-menu-section">
        <button className="ab-menu-item ab-menu-item-danger" onClick={() => handleAction(() => onArchive(issue.planItemId))}>
          <span className="ab-menu-icon">🗄️</span>
          Archive
        </button>
      </div>

      <div className="ab-menu-footer">
        <button className="ab-menu-close" onClick={() => setIsOpen(false)}>
          Close
        </button>
      </div>
    </div>
  );
}
