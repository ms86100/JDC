import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ADMIN_CATEGORIES } from '../../../components/layout/adminCategories';
import { enableWebsudo } from '../../../components/layout/WebsudoBanner';
import './AdminDashboardPage.css';

const systemStats = {
  users: { total: 156, active: 142 },
  projects: { total: 24, active: 18 },
  issues: { total: 1247, open: 342 },
  automationRules: { total: 15, enabled: 12 },
};

const QUICK_LINKS = [
  { label: 'User Management', path: '/admin/users', icon: '👤' },
  { label: 'Project types', path: '/admin/project-types', icon: '📁' },
  { label: 'Workflows', path: '/admin/workflows', icon: '🔀' },
  { label: 'Issue types', path: '/admin/issue-types', icon: '📋' },
  { label: 'Automation', path: '/admin/automation', icon: '⚡' },
  { label: 'Audit logs', path: '/admin/auditing', icon: '📊' },
];

export default function AdminDashboardPage() {
  const [expandedCategory, setExpandedCategory] = useState<string | null>('users');
  const [searchQuery, setSearchQuery] = useState('');

  const dashboardCategories = useMemo(
    () =>
      ADMIN_CATEGORIES.map((cat) => ({
        key: cat.key,
        title: cat.label,
        icon: cat.icon,
        sections: cat.items.map((it) => ({
          key: it.path,
          title: it.label,
          description: `Configure ${it.label.toLowerCase()} for your instance`,
          icon: cat.icon,
          path: it.path,
        })),
      })),
    []
  );

  const filteredCategories = searchQuery
    ? dashboardCategories
        .map((category) => ({
          ...category,
          sections: category.sections.filter(
            (section) =>
              section.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
              section.description.toLowerCase().includes(searchQuery.toLowerCase())
          ),
        }))
        .filter((category) => category.sections.length > 0)
    : dashboardCategories;

  const toggleCategory = (key: string) => {
    setExpandedCategory(expandedCategory === key ? null : key);
  };

  return (
    <div className="admin-dashboard">
      <div className="admin-dashboard-hero">
        <div className="admin-dashboard-hero-content">
          <div className="admin-dashboard-hero-badge">Airbus Design System</div>
          <h1>Systems and Avionics Administration</h1>
          <p>
            Manage users, projects, workflows, and Data Center configuration — aligned with Jira
            Data Center administration.
          </p>
        </div>
      </div>

      <div className="admin-dashboard-websudo jdc-card" style={{ marginBottom: 20, padding: 16 }}>
        <h3 style={{ marginTop: 0, fontSize: 14 }}>Temporary administrator access (websudo)</h3>
        <p className="jdc-muted" style={{ margin: '0 0 12px', fontSize: 13 }}>
          Jira Data Center shows a yellow banner while you operate with elevated privileges. Enable for 60
          minutes to preview the websudo banner in the application shell.
        </p>
        <button
          type="button"
          className="jdc-btn jdc-btn-secondary"
          onClick={() => {
            enableWebsudo(60);
            window.dispatchEvent(new Event('sa-websudo-change'));
            window.location.href = '/dashboard';
          }}
        >
          Enable websudo (60 min)
        </button>
      </div>

      <div className="admin-overview">
        <div className="overview-card">
          <div className="overview-icon">👥</div>
          <div className="overview-content">
            <div className="overview-value">{systemStats.users.total}</div>
            <div className="overview-label">Users</div>
            <div className="overview-sublabel">{systemStats.users.active} active</div>
          </div>
        </div>
        <div className="overview-card">
          <div className="overview-icon">📁</div>
          <div className="overview-content">
            <div className="overview-value">{systemStats.projects.total}</div>
            <div className="overview-label">Projects</div>
            <div className="overview-sublabel">{systemStats.projects.active} active</div>
          </div>
        </div>
        <div className="overview-card">
          <div className="overview-icon">📋</div>
          <div className="overview-content">
            <div className="overview-value">{systemStats.issues.total}</div>
            <div className="overview-label">Issues</div>
            <div className="overview-sublabel">{systemStats.issues.open} open</div>
          </div>
        </div>
        <div className="overview-card">
          <div className="overview-icon">⚡</div>
          <div className="overview-content">
            <div className="overview-value">{systemStats.automationRules.total}</div>
            <div className="overview-label">Automation rules</div>
            <div className="overview-sublabel">{systemStats.automationRules.enabled} enabled</div>
          </div>
        </div>
      </div>

      <div className="admin-search-section">
        <div className="admin-search-box">
          <input
            type="search"
            placeholder="Search admin settings..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label="Search administration settings"
          />
          {searchQuery && (
            <button type="button" className="clear-search" onClick={() => setSearchQuery('')} aria-label="Clear search">
              ×
            </button>
          )}
        </div>
      </div>

      <div className="admin-quick-access">
        <h2>Quick access</h2>
        <div className="quick-access-grid">
          {QUICK_LINKS.map((link) => (
            <Link key={link.path} to={link.path} className="quick-access-card">
              <span className="quick-access-icon">{link.icon}</span>
              <span className="quick-access-title">{link.label}</span>
            </Link>
          ))}
        </div>
      </div>

      <div className="admin-categories">
        {filteredCategories.map((category) => (
          <div key={category.key} className="admin-category">
            <button
              type="button"
              className={`admin-category-header ${expandedCategory === category.key ? 'expanded' : ''}`}
              onClick={() => toggleCategory(category.key)}
              aria-expanded={expandedCategory === category.key}
            >
              <span className="category-icon">{category.icon}</span>
              <span className="category-title">{category.title}</span>
              <span className="category-count">{category.sections.length}</span>
              <span className="expand-icon" aria-hidden="true">
                {expandedCategory === category.key ? '−' : '+'}
              </span>
            </button>
            {expandedCategory === category.key && (
              <div className="admin-category-content">
                {category.sections.map((section) => (
                  <Link key={section.key} to={section.path} className="admin-section-card">
                    <h3 className="section-title">{section.title}</h3>
                    <p className="section-description">{section.description}</p>
                  </Link>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      {filteredCategories.length === 0 && searchQuery && (
        <div className="no-results">
          <h3>No results found</h3>
          <p>Try a different search term or browse categories in the left navigation.</p>
        </div>
      )}
    </div>
  );
}
