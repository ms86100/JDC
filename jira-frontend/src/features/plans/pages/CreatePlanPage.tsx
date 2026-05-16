import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreatePlan } from '../hooks/usePlans';
import boardApi, { AgileBoard } from '../../../api/boardApi';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import { filterApi, SavedFilter } from '../../../api/filterApi';
import '../styles/plans.css';

interface IssueSource {
  id: string;
  type: 'board' | 'project' | 'filter';
  name: string;
}

interface ExclusionRule {
  field: string;
  operator: string;
  value: string;
}

export default function CreatePlanPage() {
  const navigate = useNavigate();
  const createPlan = useCreatePlan();

  const [formData, setFormData] = useState({
    name: '',
    access: 'OPEN' as 'OPEN' | 'RESTRICTED',
    issueSources: [] as IssueSource[],
    exclusionRules: [] as ExclusionRule[],
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loadingSources, setLoadingSources] = useState(false);
  const [availableBoards, setAvailableBoards] = useState<AgileBoard[]>([]);
  const [availableProjects, setAvailableProjects] = useState<ProjectResponse[]>([]);
  const [availableFilters, setAvailableFilters] = useState<SavedFilter[]>([]);

  const [showBoardDropdown, setShowBoardDropdown] = useState(false);
  const [showProjectDropdown, setShowProjectDropdown] = useState(false);
  const [showFilterDropdown, setShowFilterDropdown] = useState(false);
  const [showExclusionModal, setShowExclusionModal] = useState(false);

  useEffect(() => {
    loadSources();
  }, []);

  const loadSources = async () => {
    setLoadingSources(true);
    try {
      // Load boards
      const projectsRes = await projectApi.getAll();
      const projects = Array.isArray(projectsRes) ? projectsRes : [];

      const boardsPromises = projects.slice(0, 10).map((p: ProjectResponse) =>
        boardApi.getBoardsByProject(p.id).catch(() => [])
      );
      const boardsResults = await Promise.all(boardsPromises);
      const allBoards = boardsResults.flat();
      setAvailableBoards(allBoards);

      setAvailableProjects(projects);

      // Load filters
      try {
        const filtersRes = await filterApi.getSavedFilters('my');
        setAvailableFilters(filtersRes.data || []);
      } catch {
        setAvailableFilters([]);
      }
    } catch (error) {
      console.error('Failed to load sources:', error);
    } finally {
      setLoadingSources(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const addIssueSource = (type: 'board' | 'project' | 'filter', item: { id: string; name: string }) => {
    const newSource: IssueSource = { id: item.id, type, name: item.name };
    const exists = formData.issueSources.some(s => s.id === item.id && s.type === type);
    if (!exists) {
      setFormData(prev => ({
        ...prev,
        issueSources: [...prev.issueSources, newSource]
      }));
    }
  };

  const removeIssueSource = (id: string, type: 'board' | 'project' | 'filter') => {
    setFormData(prev => ({
      ...prev,
      issueSources: prev.issueSources.filter(s => !(s.id === id && s.type === type))
    }));
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.name.trim()) {
      newErrors.name = 'Plan name is required';
    }
    if (formData.issueSources.length === 0) {
      newErrors.issueSources = 'At least one issue source is required';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    try {
      const settings = {
        issueSources: formData.issueSources,
        exclusionRules: formData.exclusionRules,
        access: formData.access,
      };

      await createPlan.mutateAsync({
        name: formData.name,
        settings,
      });
      navigate('/plans');
    } catch (error) {
      console.error('Failed to create plan:', error);
    }
  };

  return (
    <div className="create-plan-page">
      <div className="create-page-container">
        <div className="create-page-header">
          <h1 className="create-page-title">Create plan</h1>
          <p className="create-page-description">
            Connect to issue sources in Jira Software to create a timeline that's always in sync with your work.
          </p>
          <p className="required-fields-note">
            Required fields are marked with an asterisk <span className="required">*</span>
          </p>
        </div>

        <form className="create-form" onSubmit={handleSubmit}>
          <div className="form-section">
            {/* Plan Name */}
            <div className="form-group">
              <label className="form-label" htmlFor="name">
                Plan name <span className="required">*</span>
              </label>
              <input
                type="text"
                id="name"
                name="name"
                className={`form-input ${errors.name ? 'input-error' : ''}`}
                value={formData.name}
                onChange={handleChange}
                placeholder="Enter a plan name"
                autoFocus
              />
              {errors.name && <span className="error-message">{errors.name}</span>}
            </div>

            {/* Access */}
            <div className="form-group">
              <label className="form-label" htmlFor="access">
                Access <span className="required">*</span>
              </label>
              <select
                id="access"
                name="access"
                className="form-select"
                value={formData.access}
                onChange={handleChange}
              >
                <option value="OPEN">Open</option>
                <option value="RESTRICTED">Restricted</option>
              </select>
            </div>

            {/* Issue Sources */}
            <div className="form-group">
              <label className="form-label">
                Issue sources <span className="required">*</span>
              </label>
              <div className="issue-sources-container">
                {/* Source Type Selector */}
                <div className="source-type-selector">
                  <div className="source-type-dropdown">
                    <button
                      type="button"
                      className="source-type-btn"
                      onClick={() => setShowBoardDropdown(!showBoardDropdown)}
                    >
                      Board
                      <span className="dropdown-arrow">▼</span>
                    </button>
                    {showBoardDropdown && (
                      <div className="source-dropdown-menu">
                        {loadingSources ? (
                          <div className="source-loading">Loading...</div>
                        ) : availableBoards.length > 0 ? (
                          availableBoards.map(board => (
                            <div
                              key={board.id}
                              className="source-dropdown-item"
                              onClick={() => {
                                addIssueSource('board', { id: board.id, name: board.name });
                                setShowBoardDropdown(false);
                              }}
                            >
                              <span className="source-icon">▦</span>
                              {board.name}
                            </div>
                          ))
                        ) : (
                          <div className="source-empty">No boards available</div>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="source-type-dropdown">
                    <button
                      type="button"
                      className="source-type-btn"
                      onClick={() => setShowProjectDropdown(!showProjectDropdown)}
                    >
                      Project
                      <span className="dropdown-arrow">▼</span>
                    </button>
                    {showProjectDropdown && (
                      <div className="source-dropdown-menu">
                        {loadingSources ? (
                          <div className="source-loading">Loading...</div>
                        ) : availableProjects.length > 0 ? (
                          availableProjects.map(project => (
                            <div
                              key={project.id}
                              className="source-dropdown-item"
                              onClick={() => {
                                addIssueSource('project', { id: project.id, name: project.name });
                                setShowProjectDropdown(false);
                              }}
                            >
                              <span className="source-icon">📁</span>
                              {project.name}
                            </div>
                          ))
                        ) : (
                          <div className="source-empty">No projects available</div>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="source-type-dropdown">
                    <button
                      type="button"
                      className="source-type-btn"
                      onClick={() => setShowFilterDropdown(!showFilterDropdown)}
                    >
                      Filter
                      <span className="dropdown-arrow">▼</span>
                    </button>
                    {showFilterDropdown && (
                      <div className="source-dropdown-menu">
                        {availableFilters.length > 0 ? (
                          availableFilters.map(filter => (
                            <div
                              key={filter.id}
                              className="source-dropdown-item"
                              onClick={() => {
                                addIssueSource('filter', { id: filter.id, name: filter.name });
                                setShowFilterDropdown(false);
                              }}
                            >
                              <span className="source-icon">⚙</span>
                              {filter.name}
                            </div>
                          ))
                        ) : (
                          <div className="source-empty">No filters available</div>
                        )}
                      </div>
                    )}
                  </div>
                </div>

                {/* Selected Sources */}
                {formData.issueSources.length > 0 && (
                  <div className="selected-sources">
                    {formData.issueSources.map((source, index) => (
                      <div key={`${source.type}-${source.id}`} className="selected-source-item">
                        <span className={`source-type-badge ${source.type}`}>
                          {source.type}
                        </span>
                        <span className="source-name">{source.name}</span>
                        <button
                          type="button"
                          className="remove-source-btn"
                          onClick={() => removeIssueSource(source.id, source.type)}
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                {/* Add Another Link */}
                {formData.issueSources.length > 0 && (
                  <button type="button" className="add-another-link">
                    + Add another
                  </button>
                )}

                {errors.issueSources && (
                  <span className="error-message">{errors.issueSources}</span>
                )}
              </div>
            </div>

            {/* Refine Issues Section */}
            <div className="form-group">
              <div className="refine-issues-section">
                <div className="refine-issues-icon">
                  <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                    <circle cx="24" cy="24" r="20" fill="#E6F0FF"/>
                    <path d="M24 14C18.477 14 14 18.477 14 24" stroke="#0066FF" strokeWidth="3" strokeLinecap="round"/>
                    <path d="M24 14C29.523 14 34 18.477 34 24" stroke="#00B8D4" strokeWidth="3" strokeLinecap="round"/>
                    <path d="M18 24C18 28.418 20.582 32 24 32C27.418 32 30 28.418 30 24" stroke="#0066FF" strokeWidth="3" strokeLinecap="round"/>
                    <circle cx="24" cy="24" r="4" fill="#0066FF"/>
                  </svg>
                </div>
                <div className="refine-issues-content">
                  <h3 className="refine-issues-title">Refine issues displayed</h3>
                  <p className="refine-issues-description">
                    Set rules to exclude certain issues from the issue sources selected.
                  </p>
                  <button
                    type="button"
                    className="btn-secondary btn-disabled"
                    disabled
                    onClick={() => setShowExclusionModal(true)}
                  >
                    Set exclusion rules
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate('/plans')}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={
                createPlan.isPending ||
                !formData.name.trim() ||
                formData.issueSources.length === 0
              }
            >
              {createPlan.isPending ? 'Creating...' : 'Create'}
            </button>
          </div>
        </form>
      </div>

      {/* Exclusion Rules Modal (placeholder) */}
      {showExclusionModal && (
        <div className="selector-overlay" onClick={() => setShowExclusionModal(false)}>
          <div className="selector-modal" onClick={(e) => e.stopPropagation()}>
            <div className="selector-header">
              <h2 className="selector-title">Exclusion Rules</h2>
              <button className="selector-close" onClick={() => setShowExclusionModal(false)}>
                ×
              </button>
            </div>
            <div className="selector-body">
              <p className="selector-description">
                Configure rules to exclude certain issues from appearing in your plan.
              </p>
              <div className="exclusion-rules-placeholder">
                <p>Exclusion rules configuration will be available soon.</p>
                <p>You can exclude by: Issue Type, Status, Resolution, Labels, Projects, Epics, etc.</p>
              </div>
            </div>
            <div className="selector-footer">
              <button className="btn-secondary" onClick={() => setShowExclusionModal(false)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}