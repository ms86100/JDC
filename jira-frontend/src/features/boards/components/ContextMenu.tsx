import React, { useEffect, useRef, useState } from 'react';
import type { BoardIssue } from '../../../api/boardApi';

interface MenuItem {
  id: string;
  label: string;
  shortcut?: string;
  icon?: React.ReactNode;
  disabled?: boolean;
  danger?: boolean;
  divider?: boolean;
  submenu?: MenuItem[];
}

interface ContextMenuProps {
  issue: BoardIssue | null;
  position: { x: number; y: number };
  onClose: () => void;
  onAction: (action: string, issueId?: string) => void;
}

export default function ContextMenu({ issue, position, onClose, onAction }: ContextMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);
  const [openSubmenu, setOpenSubmenu] = useState<string | null>(null);

  const menuItems: MenuItem[] = issue
    ? [
        { id: 'open', label: 'Open issue', shortcut: 'Enter' },
        { id: 'edit', label: 'Edit issue' },
        { id: 'assign', label: 'Assign to me' },
        { divider: true, id: 'divider1', label: '' },
        {
          id: 'status',
          label: 'Change status',
          submenu: [
            { id: 'status-todo', label: 'To Do' },
            { id: 'status-inprogress', label: 'In Progress' },
            { id: 'status-review', label: 'In Review' },
            { id: 'status-done', label: 'Done' },
          ],
        },
        {
          id: 'priority',
          label: 'Change priority',
          submenu: [
            { id: 'priority-highest', label: 'Highest' },
            { id: 'priority-high', label: 'High' },
            { id: 'priority-medium', label: 'Medium' },
            { id: 'priority-low', label: 'Low' },
            { id: 'priority-lowest', label: 'Lowest' },
          ],
        },
        { id: 'labels', label: 'Edit labels' },
        { id: 'components', label: 'Edit components' },
        { divider: true, id: 'divider2', label: '' },
        { id: 'copy', label: 'Copy issue', shortcut: 'Ctrl+C' },
        { id: 'move', label: 'Move issue' },
        { id: 'clone', label: 'Clone issue' },
        { divider: true, id: 'divider3', label: '' },
        { id: 'archive', label: 'Archive issue' },
        { id: 'delete', label: 'Delete issue', danger: true },
      ]
    : [];

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [onClose]);

  useEffect(() => {
    if (menuRef.current) {
      const rect = menuRef.current.getBoundingClientRect();
      const viewportWidth = window.innerWidth;
      const viewportHeight = window.innerHeight;

      let adjustedX = position.x;
      let adjustedY = position.y;

      if (rect.right > viewportWidth) {
        adjustedX = viewportWidth - rect.width - 10;
      }
      if (rect.bottom > viewportHeight) {
        adjustedY = viewportHeight - rect.height - 10;
      }

      menuRef.current.style.left = `${adjustedX}px`;
      menuRef.current.style.top = `${adjustedY}px`;
    }
  }, [position]);

  const handleItemClick = (item: MenuItem) => {
    if (item.divider || item.disabled) return;
    if (item.submenu) {
      setOpenSubmenu(openSubmenu === item.id ? null : item.id);
      return;
    }
    if (issue) {
      onAction(item.id, issue.id);
    }
    onClose();
  };

  const renderMenuItem = (item: MenuItem, depth = 0) => {
    if (item.divider) {
      return <div key={item.id} className="sa-context-menu-divider" />;
    }

    const hasSubmenu = item.submenu && item.submenu.length > 0;
    const isOpen = openSubmenu === item.id;

    return (
      <div key={item.id} className="sa-context-menu-item-wrapper">
        <button
          type="button"
          className={`sa-context-menu-item${item.disabled ? ' is-disabled' : ''}${item.danger ? ' is-danger' : ''}`}
          onClick={() => handleItemClick(item)}
          disabled={item.disabled}
        >
          {item.icon && <span className="sa-context-menu-icon">{item.icon}</span>}
          <span className="sa-context-menu-label">{item.label}</span>
          {item.shortcut && <span className="sa-context-menu-shortcut">{item.shortcut}</span>}
          {hasSubmenu && <span className="sa-context-menu-arrow">›</span>}
        </button>
        {hasSubmenu && isOpen && (
          <div className="sa-context-menu-submenu">
            {item.submenu!.map((subItem) => renderMenuItem(subItem, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  if (!issue) return null;

  return (
    <div
      ref={menuRef}
      className="sa-context-menu"
      role="menu"
      style={{
        left: position.x,
        top: position.y,
      }}
    >
      <div className="sa-context-menu-header">
        <span className="sa-context-menu-issue-key">{issue.issueKey}</span>
        <span className="sa-context-menu-issue-title">{issue.summary?.slice(0, 40)}{issue.summary && issue.summary.length > 40 ? '…' : ''}</span>
      </div>
      <div className="sa-context-menu-items">
        {menuItems.map((item) => renderMenuItem(item))}
      </div>
    </div>
  );
}
