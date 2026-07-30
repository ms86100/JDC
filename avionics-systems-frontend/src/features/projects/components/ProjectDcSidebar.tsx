import { NavLink } from 'react-router-dom';
import { getProjectDcNav, projectSettingsPath } from '../projectDcNav';

interface ProjectDcSidebarProps {
  projectId: string;
  projectKey?: string;
  projectName?: string;
  template?: string;
  category?: string;
  activeBoardPath?: string;
}

export default function ProjectDcSidebar({
  projectId,
  projectKey,
  projectName,
  template,
  category,
  activeBoardPath,
}: ProjectDcSidebarProps) {
  const navItems = getProjectDcNav(projectId, template, activeBoardPath, category);
  const settingsPath = projectSettingsPath(projectId, 'summary');

  return (
    <aside className="jdc-project-sidebar" aria-label="Project navigation">
      <div className="jdc-project-sidebar-header">
        {projectKey && <div className="jdc-project-sidebar-key">{projectKey}</div>}
        <div className="jdc-project-sidebar-name">{projectName ?? 'Project'}</div>
      </div>
      <nav className="jdc-project-nav">
        {navItems.map((item) => (
          <NavLink
            key={item.id}
            to={item.path}
            end={item.end}
            className={({ isActive }) =>
              `jdc-project-nav-item${isActive ? ' active' : ''}`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="jdc-project-nav-footer">
        <NavLink
          to={settingsPath}
          className={({ isActive }) =>
            `jdc-project-nav-item${isActive ? ' active' : ''}`
          }
        >
          Project settings
        </NavLink>
      </div>
    </aside>
  );
}
