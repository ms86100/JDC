import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import './AdminLayout.css';

interface AdminLayoutProps {
  children: React.ReactNode;
}

// Jira DC admin sidebar nav items matching screenshot
const ADMIN_NAV_ITEMS = [
  { key: 'projects',        label: 'Projects',        icon: '📁', path: '/admin/project-types' },
  { key: 'issues',         label: 'Issues',          icon: '🐛', path: '/admin/issue-types' },
  { key: 'workflows',       label: 'Workflows',        icon: '⚙', path: '/admin/workflows' },
  { key: 'forms',          label: 'Forms',            icon: '📋', path: '/admin/system/general' },
  { key: 'automations',     label: 'Automations',     icon: '⚡', path: '/admin/automation' },
  { key: 'reports',         label: 'Reports',          icon: '📊', path: '/admin/reports' },
  { key: 'settings',        label: 'Settings',         icon: '🔧', path: '/admin/system/general' },
  { key: 'insights',        label: 'Insights',         icon: '🔍', path: '/admin/insights' },
];

export default function AdminLayout({ children }: AdminLayoutProps) {
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="dc-admin-layout">
      {/* Header */}
      <header className="dc-admin-header">
        <div className="dc-header-left">
          <button
            className="dc-sidebar-toggle"
            onClick={() => setCollapsed(!collapsed)}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? '▶' : '◀'}
          </button>
          <span className="dc-header-title">Jira Administration</span>
        </div>
        <div className="dc-header-right">
          <div className="dc-header-search">
            <input
              type="text"
              placeholder="Search"
              className="dc-header-search-input"
            />
          </div>
          <button className="dc-header-btn" title="Help">?</button>
          <button className="dc-header-btn" title="Settings">⚙</button>
          <div className="dc-header-avatar" title="User menu">SS</div>
        </div>
      </header>

      <div className="dc-admin-body">
        {/* Sidebar */}
        <aside className={`dc-admin-sidebar ${collapsed ? 'dc-collapsed' : ''}`}>
          <nav className="dc-sidebar-nav">
            {ADMIN_NAV_ITEMS.map((item) => (
              <Link
                key={item.key}
                to={item.path}
                className={`dc-sidebar-item ${location.pathname.startsWith(item.path) ? 'dc-active' : ''}`}
                title={item.label}
              >
                <span className="dc-sidebar-icon">{item.icon}</span>
                {!collapsed && <span className="dc-sidebar-label">{item.label}</span>}
              </Link>
            ))}
          </nav>
        </aside>

        {/* Content */}
        <main className="dc-admin-content">
          {children}
        </main>
      </div>
    </div>
  );
}