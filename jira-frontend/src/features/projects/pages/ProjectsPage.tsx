import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import CreateProjectWizard from '../components/CreateProjectWizard';
import './ProjectsPage.css';

type ViewMode = 'grid' | 'list';
type FilterType = 'all' | 'software' | 'business' | 'archived';

export default function ProjectsPage() {
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [filter, setFilter] = useState<FilterType>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateWizard, setShowCreateWizard] = useState(false);

  const { data: projects = [], isLoading } = useQuery<ProjectResponse[]>({
    queryKey: ['projects', filter, searchQuery],
    queryFn: async () => {
      const params: Record<string, string> = {};
      if (filter === 'archived') params.archived = 'true';
      return await projectApi.getAll(params);
    },
  });

  const filteredProjects = projects.filter(project => {
    if (filter === 'software' && project.projectType !== 'SOFTWARE') return false;
    if (filter === 'business' && project.projectType !== 'BUSINESS') return false;
    if (filter === 'archived' && !project.archived) return false;
    if (filter === 'all' && project.archived) return false;

    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      return (
        project.name.toLowerCase().includes(query) ||
        project.projectKey.toLowerCase().includes(query) ||
        project.description?.toLowerCase().includes(query)
      );
    }

    return true;
  });

  const getTemplateIcon = (template: string) => {
    switch (template) {
      case 'SCRUM': return '🏃';
      case 'KANBAN': return '📋';
      case 'BASIC': return '📝';
      case 'PROJECT_MANAGEMENT': return '🎯';
      case 'TASK_MANAGEMENT': return '✓';
      case 'PROCESS_MANAGEMENT': return '🔄';
      default: return '📁';
    }
  };

  const getTemplateLabel = (template: string) => {
    switch (template) {
      case 'SCRUM': return 'Scrum';
      case 'KANBAN': return 'Kanban';
      case 'BASIC': return 'Basic';
      case 'PROJECT_MANAGEMENT': return 'Project Management';
      case 'TASK_MANAGEMENT': return 'Task Management';
      case 'PROCESS_MANAGEMENT': return 'Process Management';
      default: return template;
    }
  };

  const getTypeBadge = (type: string) => {
    switch (type) {
      case 'SOFTWARE': return { bg: '#e5ecf7', color: '#00205b', label: 'Software' };
      case 'BUSINESS': return { bg: '#f0f4ff', color: '#004080', label: 'Business' };
      default: return { bg: '#f3f4f6', color: '#4b5563', label: type };
    }
  };

  const getClassificationBadge = (classification: string | undefined) => {
    switch (classification) {
      case 'PUBLIC': return { bg: '#dcfce7', color: '#166534', label: 'Public' };
      case 'RESTRICTED': return { bg: '#fff3cd', color: '#856404', label: 'Restricted' };
      case 'CONFIDENTIAL': return { bg: '#fee2e2', color: '#991b1b', label: 'Confidential' };
      case 'EXPORT_CONTROLLED': return { bg: '#e5f0ff', color: '#004080', label: 'Export Controlled' };
      default: return null;
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
    });
  };

  return (
    <div className="projects-page">
      <div className="projects-header">
        <div className="projects-header-left">
          <h1>Projects</h1>
          <p>Manage your projects and access their boards, issues, and settings</p>
        </div>
        <div className="projects-header-right">
          <button className="ds-button ds-button--primary" onClick={() => setShowCreateWizard(true)}>
            + Create Project
          </button>
        </div>
      </div>

      <div className="projects-toolbar">
        <div className="projects-toolbar-left">
          <div className="projects-search">
            <span className="search-icon">🔍</span>
            <input
              type="text"
              placeholder="Search projects..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <div className="projects-filter-group">
            <button
              className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
              onClick={() => setFilter('all')}
            >
              All Projects
            </button>
            <button
              className={`filter-btn ${filter === 'software' ? 'active' : ''}`}
              onClick={() => setFilter('software')}
            >
              Software
            </button>
            <button
              className={`filter-btn ${filter === 'business' ? 'active' : ''}`}
              onClick={() => setFilter('business')}
            >
              Business
            </button>
            <button
              className={`filter-btn ${filter === 'archived' ? 'active' : ''}`}
              onClick={() => setFilter('archived')}
            >
              Archived
            </button>
          </div>
        </div>
        <div className="projects-toolbar-right">
          <div className="view-toggle">
            <button
              className={`view-btn ${viewMode === 'grid' ? 'active' : ''}`}
              onClick={() => setViewMode('grid')}
              title="Grid view"
            >
              ▦
            </button>
            <button
              className={`view-btn ${viewMode === 'list' ? 'active' : ''}`}
              onClick={() => setViewMode('list')}
              title="List view"
            >
              ☰
            </button>
          </div>
        </div>
      </div>

      {isLoading ? (
        <div className="projects-loading">
          <div className="ds-spinner-large"></div>
        </div>
      ) : filteredProjects.length === 0 ? (
        <div className="projects-empty">
          <div className="empty-icon">📁</div>
          <h3>No projects found</h3>
          <p>
            {searchQuery
              ? 'No projects match your search criteria'
              : filter === 'archived'
              ? 'No archived projects'
              : 'Get started by creating your first project'}
          </p>
          {!searchQuery && filter !== 'archived' && (
            <button className="ds-button ds-button--primary" onClick={() => setShowCreateWizard(true)}>
              Create Project
            </button>
          )}
        </div>
      ) : viewMode === 'grid' ? (
        <div className="projects-grid">
          {filteredProjects.map((project) => {
            const typeBadge = getTypeBadge(project.projectType);
            const classBadge = getClassificationBadge(project.classification);

            return (
              <Link key={project.id} to={`/projects/${project.id}`} className="project-card">
                <div className="project-card-header">
                  <div className="project-avatar">
                    {project.avatarUrl ? (
                      <img src={project.avatarUrl} alt={project.name} />
                    ) : (
                      <span>{project.name.charAt(0).toUpperCase()}</span>
                    )}
                  </div>
                  <div className="project-badges">
                    {typeBadge && (
                      <span className="type-badge" style={{ background: typeBadge.bg, color: typeBadge.color }}>
                        {typeBadge.label}
                      </span>
                    )}
                    {classBadge && (
                      <span className="classification-badge" style={{ background: classBadge.bg, color: classBadge.color }}>
                        {classBadge.label}
                      </span>
                    )}
                  </div>
                </div>
                <div className="project-card-body">
                  <h3 className="project-name">{project.name}</h3>
                  <div className="project-key">{project.projectKey}</div>
                  {project.description && (
                    <p className="project-description">{project.description}</p>
                  )}
                </div>
                <div className="project-card-footer">
                  <div className="project-meta">
                    <span className="meta-item">
                      <span className="meta-icon">📋</span>
                      {project.issueCounter} issues
                    </span>
                    <span className="meta-item">
                      <span className="meta-icon">{getTemplateIcon(project.template)}</span>
                      {getTemplateLabel(project.template)}
                    </span>
                  </div>
                  <div className="project-lead">
                    {project.leadName && (
                      <span className="lead-badge">
                        <span className="lead-avatar">
                          {project.leadName.charAt(0).toUpperCase()}
                        </span>
                        {project.leadName}
                      </span>
                    )}
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      ) : (
        <div className="projects-list">
          <table className="ds-table">
            <thead>
              <tr>
                <th>Project</th>
                <th>Key</th>
                <th>Type</th>
                <th>Template</th>
                <th>Lead</th>
                <th>Issues</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredProjects.map((project) => {
                const typeBadge = getTypeBadge(project.projectType);
                const classBadge = getClassificationBadge(project.classification);

                return (
                  <tr key={project.id} className={project.archived ? 'archived-row' : ''}>
                    <td>
                      <Link to={`/projects/${project.id}`} className="project-link">
                        <div className="project-link-avatar">
                          {project.avatarUrl ? (
                            <img src={project.avatarUrl} alt={project.name} />
                          ) : (
                            <span>{project.name.charAt(0).toUpperCase()}</span>
                          )}
                        </div>
                        <span className="project-link-name">{project.name}</span>
                      </Link>
                    </td>
                    <td>
                      <span className="project-key-badge">{project.projectKey}</span>
                    </td>
                    <td>
                      {typeBadge && (
                        <span className="type-badge" style={{ background: typeBadge.bg, color: typeBadge.color }}>
                          {typeBadge.label}
                        </span>
                      )}
                    </td>
                    <td>
                      <span className="template-badge">
                        {getTemplateIcon(project.template)}
                        {getTemplateLabel(project.template)}
                      </span>
                    </td>
                    <td>
                      {project.leadName ? (
                        <span className="lead-name">{project.leadName}</span>
                      ) : (
                        <span className="no-lead">-</span>
                      )}
                    </td>
                    <td>
                      <span className="issue-count">{project.issueCounter}</span>
                    </td>
                    <td>
                      <span className="created-date">{formatDate(project.createdAt)}</span>
                    </td>
                    <td>
                      <div className="action-buttons">
                        <Link to={`/projects/${project.id}`} className="ds-button ds-button--ghost ds-button--xsmall">
                          View
                        </Link>
                        <button className="ds-button ds-button--ghost ds-button--xsmall">
                          Settings
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {showCreateWizard && (
        <CreateProjectWizard onClose={() => setShowCreateWizard(false)} />
      )}
    </div>
  );
}