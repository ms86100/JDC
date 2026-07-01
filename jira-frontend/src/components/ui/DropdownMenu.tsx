import React, { useState, useRef, useEffect } from 'react';

interface DropdownMenuProps {
  trigger: React.ReactNode;
  children: React.ReactNode;
  align?: 'left' | 'right';
}

export default function DropdownMenu({ trigger, children, align = 'left' }: DropdownMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setIsOpen(false);
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <div ref={menuRef} style={{ position: 'relative', display: 'inline-flex' }}>
      <div onClick={() => setIsOpen(!isOpen)}>{trigger}</div>
      {isOpen && (
        <div
          style={{
            position: 'absolute',
            top: '100%',
            [align]: 0,
            marginTop: '4px',
            minWidth: '160px',
            background: 'white',
            border: '1px solid var(--sa-n200)',
            borderRadius: '3px',
            boxShadow: '0 1px 4px rgba(9,30,66,0.12)',
            zIndex: 200,
          }}
          role="menu"
        >
          {children}
        </div>
      )}
    </div>
  );
}

interface MenuItemProps {
  children: React.ReactNode;
  onClick?: () => void;
  icon?: React.ReactNode;
  danger?: boolean;
}

export function MenuItem({ children, onClick, icon, danger }: MenuItemProps) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        width: '100%',
        padding: '6px 12px',
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        fontSize: '13px',
        fontFamily: 'inherit',
        color: danger ? 'var(--sa-danger-500)' : 'var(--sa-n700)',
        textAlign: 'left',
      }}
      onMouseEnter={(e) => (e.currentTarget.style.background = 'var(--sa-n50)')}
      onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
      role="menuitem"
    >
      {icon && <span style={{ width: 16, textAlign: 'center', flexShrink: 0 }}>{icon}</span>}
      {children}
    </button>
  );
}

export function MenuDivider() {
  return <div style={{ height: 1, background: 'var(--sa-n200)', margin: '4px 0' }} />;
}