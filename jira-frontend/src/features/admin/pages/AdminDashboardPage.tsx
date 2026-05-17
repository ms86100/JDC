import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import AdminLayout from '../components/AdminLayout';
import './AdminDashboardPage.css';

interface AdminSection {
  key: string;
  title: string;
  description: string;
  icon: string;
  path: string;
  count?: number;
  features?: string[];
}

interface AdminCategory {
  key: string;
  title: string;
  icon: string;
  sections: AdminSection[];
}

const adminCategories: AdminCategory[] = [
  {
    key: 'user-management',
    title: 'User Management',
    icon: '👥',
    sections: [
      {
        key: 'users',
        title: 'Users',
        description: 'Manage user accounts, access, and permissions',
        icon: '👤',
        path: '/admin/users',
        features: ['Create/edit users', 'Deactivate accounts', 'Bulk user operations', 'User directory'],
      },
      {
        key: 'groups',
        title: 'Groups',
        description: 'Organize users into groups for easier management',
        icon: '👥',
        path: '/admin/groups',
        features: ['Create groups', 'Add/remove members', 'Group permissions'],
      },
      {
        key: 'roles',
        title: 'Project Roles',
        description: 'Define roles within projects',
        icon: '🎭',
        path: '/admin/roles',
        features: ['Role definitions', 'Role members', 'Default roles'],
      },
      {
        key: 'permissions',
        title: 'Global Permissions',
        description: 'Configure system-wide access controls',
        icon: '🔐',
        path: '/admin/permissions',
        features: ['Admin permissions', 'System access', 'Systems and Avionics permissions'],
      },
    ],
  },
  {
    key: 'projects',
    title: 'Projects',
    icon: '📁',
    sections: [
      {
        key: 'project-types',
        title: 'Project Types',
        description: 'Configure available project types',
        icon: '📋',
        path: '/admin/project-types',
        features: ['Software projects', 'Business projects', 'Custom types'],
      },
      {
        key: 'project-categories',
        title: 'Project Categories',
        description: 'Organize projects into categories',
        icon: '📂',
        path: '/admin/project-categories',
        features: ['Create categories', 'Assign projects', 'Category hierarchy'],
      },
    ],
  },
  {
    key: 'issues',
    title: 'Issues',
    icon: '🐛',
    sections: [
      {
        key: 'issue-types',
        title: 'Issue Types',
        description: 'Configure issue type schemes and types',
        icon: '📝',
        path: '/admin/issue-types',
        features: ['Bug', 'Story', 'Task', 'Epic', 'Subtask'],
      },
      {
        key: 'statuses',
        title: 'Statuses',
        description: 'Manage issue statuses and categories',
        icon: '🔄',
        path: '/admin/statuses',
        features: ['To Do', 'In Progress', 'Done', 'Custom statuses'],
      },
      {
        key: 'priorities',
        title: 'Priorities',
        description: 'Configure issue priority levels',
        icon: '⬆️',
        path: '/admin/priorities',
        features: ['Critical', 'High', 'Medium', 'Low'],
      },
      {
        key: 'resolutions',
        title: 'Resolutions',
        description: 'Define issue resolution options',
        icon: '✅',
        path: '/admin/resolutions',
        features: ['Fixed', 'Won\'t Fix', 'Duplicate', 'Cannot Reproduce'],
      },
    ],
  },
  {
    key: 'workflows',
    title: 'Workflows',
    icon: '⚙️',
    sections: [
      {
        key: 'workflows',
        title: 'Workflows',
        description: 'Design and manage workflow schemes',
        icon: '🔀',
        path: '/admin/workflows',
        features: ['Workflow designer', 'Transitions', 'Conditions', 'Post-functions'],
      },
      {
        key: 'workflow-schemes',
        title: 'Workflow Schemes',
        description: 'Assign workflows to projects',
        icon: '📊',
        path: '/admin/workflow-schemes',
        features: ['Scheme assignment', 'Issue type mapping'],
      },
      {
        key: 'screens',
        title: 'Screens',
        description: 'Configure field visibility on screens',
        icon: '🖥️',
        path: '/admin/screens',
        features: ['Create/edit screens', 'Field layout', 'Screen schemes'],
      },
    ],
  },
  {
    key: 'security',
    title: 'Security',
    icon: '🔒',
    sections: [
      {
        key: 'permission-schemes',
        title: 'Permission Schemes',
        description: 'Configure project permissions',
        icon: '🔑',
        path: '/admin/permission-schemes',
        features: ['Browse projects', 'Edit issues', 'Admin projects', 'Custom permissions'],
      },
      {
        key: 'notification-schemes',
        title: 'Notification Schemes',
        description: 'Configure email and notification rules',
        icon: '📧',
        path: '/admin/notification-schemes',
        features: ['Event notifications', 'Email templates', 'Recipient rules'],
      },
      {
        key: 'security-schemes',
        title: 'Issue Security Schemes',
        description: 'Configure issue-level security',
        icon: '🛡️',
        path: '/admin/security-schemes',
        features: ['Security levels', 'Level members', 'Grant access'],
      },
    ],
  },
  {
    key: 'custom-fields',
    title: 'Custom Fields',
    icon: '📦',
    sections: [
      {
        key: 'custom-fields',
        title: 'Custom Fields',
        description: 'Create and manage custom fields',
        icon: '🏷️',
        path: '/admin/custom-fields',
        features: ['Text fields', 'Date pickers', 'User picker', 'Select lists', 'Number fields'],
      },
      {
        key: 'field-contexts',
        title: 'Contexts',
        description: 'Configure field contexts and options',
        icon: '🎯',
        path: '/admin/field-contexts',
        features: ['Project contexts', 'Custom options', 'Default values'],
      },
    ],
  },
  {
    key: 'automation',
    title: 'Automation',
    icon: '⚡',
    sections: [
      {
        key: 'automation',
        title: 'Automation Rules',
        description: 'Automate workflows with rules',
        icon: '🤖',
        path: '/admin/automation',
        features: ['Triggers', 'Conditions', 'Actions', 'Rule logs'],
      },
    ],
  },
  {
    key: 'advanced',
    title: 'Advanced',
    icon: '🔧',
    sections: [
      {
        key: 'auditing',
        title: 'Auditing',
        description: 'View audit logs and track changes',
        icon: '📋',
        path: '/admin/auditing',
        features: ['Audit log viewer', 'Change history', 'Export logs'],
      },
      {
        key: 'api',
        title: 'API',
        description: 'Manage API access and tokens',
        icon: '🔌',
        path: '/admin/api',
        features: ['API tokens', 'REST API docs', 'Rate limits'],
      },
      {
        key: 'webhooks',
        title: 'Webhooks',
        description: 'Configure external integrations',
        icon: '🪝',
        path: '/admin/webhooks',
        features: ['Webhook events', 'Payload templates', 'Delivery logs'],
      },
    ],
  },
  {
    key: 'datacenter',
    title: 'Data Center',
    icon: '🖥️',
    sections: [
      {
        key: 'cluster',
        title: 'Cluster Nodes',
        description: 'Monitor cluster health and performance',
        icon: '🌐',
        path: '/admin/cluster',
        features: ['Node status', 'Health checks', 'Load distribution'],
      },
      {
        key: 'cache',
        title: 'Cache Statistics',
        description: 'Monitor and manage caches',
        icon: '💾',
        path: '/admin/cache',
        features: ['Cache sizes', 'Hit rates', 'Clear cache'],
      },
      {
        key: 'indexing',
        title: 'Indexing',
        description: 'Manage search index',
        icon: '🔍',
        path: '/admin/indexing',
        features: ['Index status', 'Reindex', 'Index statistics'],
      },
      {
        key: 'jobs',
        title: 'Scheduled Jobs',
        description: 'Manage background jobs',
        icon: '⏰',
        path: '/admin/jobs',
        features: ['Job status', 'Cron schedules', 'Manual run'],
      },
    ],
  },
];

const systemStats = {
  users: { total: 156, active: 142 },
  projects: { total: 24, active: 18, archived: 6 },
  issues: { total: 1247, open: 342, closed: 905 },
  automationRules: { total: 15, enabled: 12 },
};

export default function AdminDashboardPage() {
  const [expandedCategory, setExpandedCategory] = useState<string | null>('user-management');
  const [searchQuery, setSearchQuery] = useState('');

  const toggleCategory = (key: string) => {
    setExpandedCategory(expandedCategory === key ? null : key);
  };

  const filteredCategories = searchQuery
    ? adminCategories
        .map(category => ({
          ...category,
          sections: category.sections.filter(
            section =>
              section.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
              section.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
              section.features?.some(f => f.toLowerCase().includes(searchQuery.toLowerCase()))
          ),
        }))
        .filter(category => category.sections.length > 0)
    : adminCategories;

  return (
    <AdminLayout>
      <div className="admin-dashboard">
        <div className="admin-dashboard-header">
          <div>
            <h1>Systems and Avionics Administration</h1>
            <p>Manage your Systems and Avionics instance configuration</p>
          </div>
        </div>

        {/* System Overview */}
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
            <div className="overview-icon">🐛</div>
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
              <div className="overview-label">Automation Rules</div>
              <div className="overview-sublabel">{systemStats.automationRules.enabled} enabled</div>
            </div>
          </div>
        </div>

        {/* Search */}
        <div className="admin-search-section">
          <div className="admin-search-box">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder="Search admin settings..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            {searchQuery && (
              <button className="clear-search" onClick={() => setSearchQuery('')}>
                ×
              </button>
            )}
          </div>
        </div>

        {/* Quick Access */}
        <div className="admin-quick-access">
          <h2>Quick Access</h2>
          <div className="quick-access-grid">
            <Link to="/admin/users" className="quick-access-card">
              <span className="quick-access-icon">👤</span>
              <span className="quick-access-title">User Management</span>
            </Link>
            <Link to="/admin/projects" className="quick-access-card">
              <span className="quick-access-icon">📁</span>
              <span className="quick-access-title">Projects</span>
            </Link>
            <Link to="/admin/workflows" className="quick-access-card">
              <span className="quick-access-icon">⚙️</span>
              <span className="quick-access-title">Workflows</span>
            </Link>
            <Link to="/admin/issue-types" className="quick-access-card">
              <span className="quick-access-icon">📝</span>
              <span className="quick-access-title">Issue Types</span>
            </Link>
            <Link to="/admin/automation" className="quick-access-card">
              <span className="quick-access-icon">⚡</span>
              <span className="quick-access-title">Automation</span>
            </Link>
            <Link to="/admin/auditing" className="quick-access-card">
              <span className="quick-access-icon">📋</span>
              <span className="quick-access-title">Audit Logs</span>
            </Link>
          </div>
        </div>

        {/* Categories Grid */}
        <div className="admin-categories">
          {filteredCategories.map((category) => (
            <div key={category.key} className="admin-category">
              <button
                className={`admin-category-header ${expandedCategory === category.key ? 'expanded' : ''}`}
                onClick={() => toggleCategory(category.key)}
              >
                <span className="category-icon">{category.icon}</span>
                <span className="category-title">{category.title}</span>
                <span className="category-count">{category.sections.length}</span>
                <span className="expand-icon">{expandedCategory === category.key ? '−' : '+'}</span>
              </button>
              {expandedCategory === category.key && (
                <div className="admin-category-content">
                  {category.sections.map((section) => (
                    <Link key={section.key} to={section.path} className="admin-section-card">
                      <div className="section-header">
                        <span className="section-icon">{section.icon}</span>
                        <div className="section-title-group">
                          <h3 className="section-title">{section.title}</h3>
                          <p className="section-description">{section.description}</p>
                        </div>
                      </div>
                      {section.features && (
                        <div className="section-features">
                          {section.features.slice(0, 4).map((feature, idx) => (
                            <span key={idx} className="feature-tag">{feature}</span>
                          ))}
                          {section.features.length > 4 && (
                            <span className="feature-more">+{section.features.length - 4} more</span>
                          )}
                        </div>
                      )}
                    </Link>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>

        {filteredCategories.length === 0 && searchQuery && (
          <div className="no-results">
            <span className="no-results-icon">🔍</span>
            <h3>No results found</h3>
            <p>Try a different search term</p>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}