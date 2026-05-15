import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import './AdminLayout.css';
import '../styles/admin-shared.css';

interface AdminLayoutProps {
  children: React.ReactNode;
}

const ADMIN_NAV_ITEMS = [
  { key: 'projects', label: 'Projects', icon: '📁', path: '/admin/project-types' },
  { key: 'issues', label: 'Issues', icon: '🐛', path: '/admin/issue-types' },
  { key: 'workflows', label: 'Workflows', icon: '⚙', path: '/admin/workflows' },
  { key: 'forms', label: 'Forms', icon: '📋', path: '/admin/system/general' },
  { key: 'automations', label: 'Automations', icon: '⚡', path: '/admin/automation' },
  { key: 'reports', label: 'Reports', icon: '📊', path: '/admin/reports' },
  { key: 'settings', label: 'Settings', icon: '🔧', path: '/admin/system/general' },
  { key: 'insights', label: 'Insights', icon: '🔍', path: '/admin/insights' },
];

export default function AdminLayout({ children }: AdminLayoutProps) {
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  return (
    <div className="dc-admin-layout">
      <header className="dc-admin-header">
        <div className="dc-header-left">
          <Link to="/dashboard" className="dc-back-to-jira" title="Back to Jira">
            ←
          </Link>
          <button
            type="button"
            className="dc-sidebar-toggle"
            onClick={() => setCollapsed(!collapsed)}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? '▶' : '◀'}
          </button>
          <span className="dc-header-title">Jira Administration</span>
        </div>
      </header>

      <div className="dc-admin-body">
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

        <main className="dc-admin-content">{children}</main>
      </div>
    </div>
  );
}
