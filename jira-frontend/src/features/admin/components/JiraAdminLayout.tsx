import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import './JiraAdminLayout.css';

interface JiraAdminLayoutProps {
  children: React.ReactNode;
}

const USER_MANAGEMENT_ITEMS = [
  { key: 'users', label: 'Users', path: '/admin/users' },
  { key: 'groups', label: 'Groups', path: '/admin/groups' },
  { key: 'anonymization', label: 'Anonymization', path: '/admin/anonymization' },
];

const USER_DIRECTORIES_ITEMS = [
  { key: 'user-directories', label: 'User Directories', path: '/admin/directories' },
  { key: 'service-accounts', label: 'Service accounts', path: '/admin/service-accounts' },
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
          <Link to="/create" className="jira-create-btn">Create</Link>
        </div>
        <div className="jira-header-right">
          <div className="jira-search-box">
            <span className="jira-search-icon">🔍</span>
            <input type="text" placeholder="Search" className="jira-search-input" />
          </div>
          <button className="jira-header-icon" title="Announcements">📢</button>
          <button className="jira-header-icon" title="Help">?</button>
          <button className="jira-header-icon" title="Settings">⚙</button>
          <button className="jira-user-avatar">S</button>
        </div>
      </header>

      {/* Admin Secondary Header */}
      <div className="jira-admin-subheader">
        <h1 className="jira-admin-title">Administration</h1>
        <div className="jira-admin-search">
          <span className="jira-search-icon">🔍</span>
          <input type="text" placeholder="Search Jira admin" className="jira-admin-search-input" />
        </div>
        <nav className="jira-admin-tabs">
          <Link to="/admin/applications" className="jira-admin-tab">Applications</Link>
          <Link to="/admin/projects" className="jira-admin-tab">Projects</Link>
          <Link to="/admin/issues" className="jira-admin-tab">Issues</Link>
          <Link to="/admin/manage-apps" className="jira-admin-tab">Manage apps</Link>
          <Link to="/admin/user-management" className="jira-admin-tab jira-admin-tab-active">User management</Link>
          <Link to="/admin/upgrade" className="jira-admin-tab">Latest upgrade report</Link>
          <Link to="/admin/system" className="jira-admin-tab">System</Link>
        </nav>
        <div className="jira-admin-subheader-right">
          <span className="jira-info-icon">ℹ</span>
          <Link to="/projects/ProjectA" className="jira-back-link">← Back to project: ProjectA</Link>
        </div>
      </div>

      {/* Admin Body with Sidebar */}
      <div className="jira-admin-body">
        <aside className="jira-admin-sidebar">
          <div className="jira-sidebar-section">
            <h3 className="jira-sidebar-heading">USER MANAGEMENT</h3>
            <nav className="jira-sidebar-nav">
              {USER_MANAGEMENT_ITEMS.map((item) => (
                <Link
                  key={item.key}
                  to={item.path}
                  className={`jira-sidebar-item ${location.pathname === item.path ? 'jira-sidebar-item-active' : ''}`}
                >
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>
          <div className="jira-sidebar-section">
            <Link to="/admin/user-server" className="jira-sidebar-item">Jira user server</Link>
          </div>
          <div className="jira-sidebar-section">
            <h3 className="jira-sidebar-heading">USER DIRECTORIES</h3>
            <nav className="jira-sidebar-nav">
              {USER_DIRECTORIES_ITEMS.map((item) => (
                <Link
                  key={item.key}
                  to={item.path}
                  className={`jira-sidebar-item ${location.pathname === item.path ? 'jira-sidebar-item-active' : ''}`}
                >
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>
        </aside>
        <main className="jira-admin-content">{children}</main>
      </div>
    </div>
  );
}