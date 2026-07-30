import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { projectApi } from '../../../api/projectApi';
import {
  XRAY_CAPABILITIES,
  XRAY_GROUP_LABELS,
  XRAY_PLUGIN_LABEL,
  XRAY_PLUGIN_TAGLINE,
  xrayPath,
  type XrayCapability,
} from '../xrayNavRegistry';
import '../styles/xray-hub.css';

interface Props {
  /** When set, hub links are project-scoped */
  projectId?: string;
  showProjectPicker?: boolean;
}

export default function XrayTestHub({ projectId, showProjectPicker = !projectId }: Props) {
  const navigate = useNavigate();
  const [selectedProject, setSelectedProject] = useState(projectId ?? '');

  const { data: projects = [], isLoading } = useQuery({
    queryKey: ['xray-project-picker'],
    queryFn: async () => {
      const res = await projectApi.getAll({ size: 200 });
      return res.data?.content ?? [];
    },
    enabled: showProjectPicker,
  });

  const groups = (['core', 'quality', 'planning', 'integration', 'admin'] as const).map((g) => ({
    key: g,
    label: XRAY_GROUP_LABELS[g],
    items: XRAY_CAPABILITIES.filter((c) => c.group === g),
  }));

  const resolveHref = (cap: XrayCapability) => {
    const pid = projectId ?? selectedProject;
    if (cap.id === 'home') return pid ? `/tests/${pid}` : '/tests';
    if (!pid && cap.path !== 'plugins' && cap.path !== 'defects' && cap.path !== 'evidence') {
      return `/tests?needProject=${cap.path}`;
    }
    return xrayPath(pid, cap.path);
  };

  const handleOpenProject = () => {
    if (selectedProject) navigate(`/tests/${selectedProject}`);
  };

  return (
    <div className="xray-hub">
      <header className="xray-hub-header">
        <div className="xray-hub-badge">Xray plugin</div>
        <h1 className="xray-hub-title">{XRAY_PLUGIN_LABEL}</h1>
        <p className="xray-hub-tagline">{XRAY_PLUGIN_TAGLINE}</p>
      </header>

      {showProjectPicker && (
        <section className="xray-hub-project jdc-card">
          <h2 className="xray-hub-section-title">Select a project</h2>
          <p className="jdc-muted">
            Xray test assets are scoped per Avionics Systems project. Choose a project to open the test repository and modules.
          </p>
          <div className="xray-hub-project-row">
            <select
              className="jdc-input"
              value={selectedProject}
              onChange={(e) => setSelectedProject(e.target.value)}
              disabled={isLoading}
              aria-label="Project"
            >
              <option value="">Select project…</option>
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.projectKey})
                </option>
              ))}
            </select>
            <button
              type="button"
              className="jdc-btn jdc-btn-primary"
              disabled={!selectedProject}
              onClick={handleOpenProject}
            >
              Open Xray for project
            </button>
            <Link to="/projects" className="jdc-link">
              Browse projects
            </Link>
          </div>
        </section>
      )}

      {groups.map(({ key, label, items }) => (
        <section key={key} className="xray-hub-group">
          <h2 className="xray-hub-section-title">{label}</h2>
          <div className="xray-hub-grid">
            {items.map((cap) => {
              const href = resolveHref(cap);
              const needsProject = href.includes('needProject=');
              if (needsProject) {
                return (
                  <button
                    key={cap.id}
                    type="button"
                    className="xray-hub-card"
                    onClick={() => {
                      if (!selectedProject) {
                        document.querySelector<HTMLSelectElement>('.xray-hub-project select')?.focus();
                        return;
                      }
                      navigate(xrayPath(selectedProject, cap.path));
                    }}
                  >
                    <span className="xray-hub-card-label">{cap.label}</span>
                    <span className="xray-hub-card-desc">{cap.description}</span>
                  </button>
                );
              }
              return (
                <Link key={cap.id} to={href} className="xray-hub-card">
                  <span className="xray-hub-card-label">{cap.label}</span>
                  <span className="xray-hub-card-desc">{cap.description}</span>
                </Link>
              );
            })}
          </div>
        </section>
      ))}
    </div>
  );
}
