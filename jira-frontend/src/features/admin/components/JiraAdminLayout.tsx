import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import './JiraAdminLayout.css';

interface JiraAdminLayoutProps {
  children: React.ReactNode;
}

const ADMIN_SIDEBAR_ITEMS = [
  { key: 'projects', label: 'Projects', icon: '📁', path: '/admin/project-types' },
  { key: 'issues', label: 'Issues', icon: '🐛', path: '/admin/issue-types' },
  { key: 'workflows', label: 'Workflows', icon: '⚙', path: '/admin/workflows' },
  { key: 'forms', label: 'Forms', icon: '📋', path: '/admin/forms' },
  { key: 'automations', label: 'Automations', icon: '⚡', path: '/admin/automation' },
  { key: 'reports', label: 'Reports', icon: '📊', path: '/admin/reports' },
  { key: 'settings', label: 'Settings', icon: '🔧', path: '/admin/system/general' },
  { key: 'insights', label: 'Insights', icon: '🔍', path: '/admin/insights' },
];

export default function JiraAdminLayout({ children }: JiraAdminLayoutProps) {
  const location = useLocation();

  return (
    <div className="jira-admin-root">
      {/* Global Jira Header */}
      <header className="jira-global-header">
        <div className="jira-header-left">
          <Link to="/dashboard" className="jira-logo">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="#0052CC"/>
              <path d="M2 17L12 22L22 17" stroke="#0052CC" strokeWidth="2"/>
              <path d="M2 12L12 17L22 12" stroke="#0052CC" strokeWidth="2"/>
            </svg>
            <span className="jira-logo-text">Jira Software</span>
          </Link>
          <nav className="jira-header-nav">
            <Link to="/dashboard" className="jira-nav-item">Dashboards</Link>
            <Link to="/projects" className="jira-nav-item">Projects</Link>
            <Link to="/issues" className="jira-nav-item">Issues</Link>
            <Link to="/plans" className="jira-nav-item">Plans</Link>
          </nav>
          <Link to="/create" className="jira-create-btn">+ Create</Link>
        </div>
        <div className="jira-header-right">
          <div className="jira-search-box">
            <span className="jira-search-icon">🔍</span>
            <input type="text" placeholder="Search" className="jira-search-input" />
          </div>
          <button className="jira-header-icon" title="Announcements">📢</button>
          <button className="jira-header-icon" title="Help">?</button>
          <button className="jira-user-avatar">MS</button>
        </div>
      </header>

      {/* Admin Secondary Header */}
      <div className="jira-admin-subheader">
        <Link to="/dashboard" className="jira-back-link">← Jira Administration</Link>
      </div>

      {/* Admin Body with Sidebar */}
      <div className="jira-admin-body">
        <aside className="jira-admin-sidebar">
          <nav className="jira-admin-nav">
            {ADMIN_SIDEBAR_ITEMS.map((item) => (
              <Link
                key={item.key}
                to={item.path}
                className={`jira-admin-nav-item ${location.pathname.startsWith(item.path) ? 'jira-admin-nav-active' : ''}`}
                title={item.label}
              >
                <span className="jira-admin-nav-icon">{item.icon}</span>
                <span className="jira-admin-nav-label">{item.label}</span>
              </Link>
            ))}
          </nav>
        </aside>
        <main className="jira-admin-content">{children}</main>
      </div>
    </div>
  );
}