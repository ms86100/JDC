import { useState, useEffect, useMemo, useCallback } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import {
  projectApi,
  CreateProjectWizardRequest,
  TemplateDetails,
} from '../../../api/projectApi';
import {
  templateApi,
  CatalogTemplate,
  TemplateCatalog,
  TemplateWithWorkflow,
} from '../../../api/templateApi';
import { appNotify } from '../../../lib/appNotify';
import '../styles/create-project-wizard.css';

const STEPS = [
  { id: 1, label: 'Template' },
  { id: 2, label: 'Details' },
  { id: 3, label: 'Review' },
  { id: 4, label: 'Create' },
] as const;

interface WizardState {
  step: number;
  template: CatalogTemplate | null;
  name: string;
  projectKey: string;
  description: string;
  leadUserId: string;
  visibility: 'PUBLIC' | 'PRIVATE';
  defaultAssigneeType: string;
}

const INITIAL_STATE: WizardState = {
  step: 1,
  template: null,
  name: '',
  projectKey: '',
  description: '',
  leadUserId: '',
  visibility: 'PUBLIC',
  defaultAssigneeType: 'PROJECT_LEAD',
};

function TemplatePreviewGraphic({ template }: { template: CatalogTemplate }) {
  const accent = template.previewAccent || template.color || '#0052CC';
  return (
    <div className="cpw-preview-graphic" style={{ '--preview-accent': accent } as React.CSSProperties}>
      <div className="cpw-preview-board">
        <div className="cpw-preview-col">
          <span className="cpw-preview-col-label">To Do</span>
          {[1, 2].map((i) => (
            <div key={i} className="cpw-preview-card" />
          ))}
        </div>
        <div className="cpw-preview-col cpw-preview-col--active">
          <span className="cpw-preview-col-label">In Progress</span>
          <div className="cpw-preview-card cpw-preview-card--highlight" />
        </div>
        <div className="cpw-preview-col">
          <span className="cpw-preview-col-label">Done</span>
          <div className="cpw-preview-card" />
        </div>
      </div>
      <div className="cpw-preview-badge">{template.workflowTypeLabel || 'Workflow'}</div>
    </div>
  );
}

export default function CreateProjectWizard({ onClose }: { onClose?: () => void }) {
  const navigate = useNavigate();
  const [wizardState, setWizardState] = useState<WizardState>(INITIAL_STATE);
  const [selectedCategoryKey, setSelectedCategoryKey] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [keyValidation, setKeyValidation] = useState({ valid: true, available: true });

  const handleClose = useCallback(() => {
    if (onClose) onClose();
    else navigate('/projects');
  }, [onClose, navigate]);

  const { data: catalog, isLoading: catalogLoading, error: catalogError } = useQuery({
    queryKey: ['templateCatalog'],
    queryFn: () => templateApi.getCatalog().then((r) => r.data),
    retry: 2,
  });

  const filteredCatalog = useMemo(
    () => (catalog ? templateApi.searchCatalog(searchQuery, catalog) : null),
    [catalog, searchQuery]
  );

  const activeCategoryKey = useMemo(() => {
    if (!filteredCatalog) return null;
    if (selectedCategoryKey) {
      const exists = filteredCatalog.categories.some((c) => c.categoryKey === selectedCategoryKey);
      if (exists) return selectedCategoryKey;
    }
    return filteredCatalog.categories[0]?.categoryKey ?? null;
  }, [filteredCatalog, selectedCategoryKey]);

  const activeCategory = useMemo(
    () => filteredCatalog?.categories.find((c) => c.categoryKey === activeCategoryKey),
    [filteredCatalog, activeCategoryKey]
  );

  const { data: templateWorkflow } = useQuery({
    queryKey: ['templateWorkflow', wizardState.template?.id],
    queryFn: () =>
      templateApi
        .getTemplateWithWorkflow(wizardState.template!.id)
        .then((r) => r.data),
    enabled: !!wizardState.template && wizardState.step >= 3,
  });

  const { data: templateSchemeDetails } = useQuery({
    queryKey: ['templateSchemeDetails', wizardState.template?.id],
    queryFn: () =>
      projectApi.getTemplateDetails(wizardState.template!.id).then((r) => r.data),
    enabled: !!wizardState.template && wizardState.step >= 3,
  });

  useEffect(() => {
    if (wizardState.projectKey.length >= 2) {
      projectApi.checkProjectKey(wizardState.projectKey).then((res) => {
        setKeyValidation({ valid: res.data.valid, available: res.data.available });
      });
    } else {
      setKeyValidation({ valid: true, available: true });
    }
  }, [wizardState.projectKey]);

  useEffect(() => {
    if (wizardState.name && wizardState.step === 2) {
      const words = wizardState.name.trim().split(/\s+/);
      let key = '';
      for (const word of words) {
        if (key.length >= 10) break;
        const cleaned = word.replace(/[^a-zA-Z0-9]/g, '');
        if (cleaned) key += cleaned.charAt(0).toUpperCase();
      }
      if (!key) key = 'PRJ';
      while (key.length < 3) key += 'X';
      setWizardState((prev) => ({
        ...prev,
        projectKey: prev.projectKey || key.substring(0, 10),
      }));
    }
  }, [wizardState.name, wizardState.step]);

  const createMutation = useMutation({
    mutationFn: (data: CreateProjectWizardRequest) => projectApi.createViaWizard(data),
    onSuccess: (response) => {
      const project = response.data;
      appNotify.success(
        `Project "${project.name}" (${project.projectKey}) was created successfully.`
      );
      if (onClose) {
        onClose();
      }
      navigate(`/projects/${project.id}`);
    },
  });

  const handleTemplateSelect = (template: CatalogTemplate) => {
    setWizardState((prev) => ({
      ...prev,
      template,
      defaultAssigneeType: template.defaultAssigneeType || 'PROJECT_LEAD',
    }));
  };

  const handleNext = () => setWizardState((prev) => ({ ...prev, step: Math.min(prev.step + 1, 4) }));
  const handleBack = () => setWizardState((prev) => ({ ...prev, step: Math.max(prev.step - 1, 1) }));

  const handleSubmit = () => {
    if (!wizardState.template) return;
    const request: CreateProjectWizardRequest = {
      projectType: (wizardState.template.projectTypeCategory || 'COMPANY_MANAGED') as
        | 'COMPANY_MANAGED'
        | 'TEAM_MANAGED',
      templateId: wizardState.template.id,
      name: wizardState.name,
      projectKey: wizardState.projectKey,
      description: wizardState.description,
      leadUserId: wizardState.leadUserId || undefined,
      defaultAssigneeType: wizardState.defaultAssigneeType,
    };
    createMutation.mutate(request);
  };

  const canProceed = () => {
    switch (wizardState.step) {
      case 1:
        return wizardState.template !== null;
      case 2:
        return (
          wizardState.name.length >= 1 &&
          wizardState.projectKey.length >= 2 &&
          keyValidation.valid &&
          keyValidation.available
        );
      case 3:
        return true;
      case 4:
        return true;
      default:
        return false;
    }
  };

  const selected = wizardState.template;

  return (
    <div className="cpw-overlay" role="dialog" aria-modal="true" aria-labelledby="cpw-title">
      <div className="cpw-modal">
        <header className="cpw-header">
          <div>
            <h2 id="cpw-title" className="cpw-title">
              Create project
            </h2>
            <p className="cpw-subtitle">
              Choose a template to configure workflows, boards, and modules automatically.
            </p>
          </div>
          <button
            type="button"
            className="cpw-close"
            onClick={handleClose}
            aria-label="Close create project dialog"
          >
            ✕
          </button>
        </header>

        <nav className="cpw-steps" aria-label="Creation progress">
          {STEPS.map((s, idx) => (
            <div key={s.id} className="cpw-steps-row">
              <div
                className={[
                  'cpw-step',
                  wizardState.step === s.id ? 'cpw-step--active' : '',
                  wizardState.step > s.id ? 'cpw-step--done' : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
              >
                <span className="cpw-step-num">{wizardState.step > s.id ? '✓' : s.id}</span>
                <span className="cpw-step-label">{s.label}</span>
              </div>
              {idx < STEPS.length - 1 && <div className="cpw-step-line" />}
            </div>
          ))}
        </nav>

        <div className="cpw-body">
          {wizardState.step === 1 && (
            <TemplateSelectionStep
              catalog={filteredCatalog}
              loading={catalogLoading}
              error={catalogError}
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
              activeCategoryKey={activeCategoryKey}
              activeCategory={activeCategory}
              onCategorySelect={setSelectedCategoryKey}
              selected={selected}
              onSelect={handleTemplateSelect}
              catalogRaw={catalog}
            />
          )}

          {wizardState.step === 2 && (
            <DetailsStep
              state={wizardState}
              onChange={setWizardState}
              keyValidation={keyValidation}
              template={selected}
            />
          )}

          {wizardState.step === 3 && (
            <ReviewStep
              state={wizardState}
              template={selected}
              workflow={templateWorkflow}
              schemeDetails={templateSchemeDetails}
            />
          )}

          {wizardState.step === 4 && (
            <ConfirmStep
              state={wizardState}
              template={selected}
              isPending={createMutation.isPending}
              isError={createMutation.isError}
            />
          )}
        </div>

        {wizardState.step === 1 && (
          <div className="cpw-footer-links">
            <Link to="/migration" className="cpw-footer-link" onClick={handleClose}>
              Import data
            </Link>
            <span className="cpw-footer-sep">·</span>
            <Link to="/migration?import=workflow-xml" className="cpw-footer-link">
              Import shared configuration
            </Link>
            <span className="cpw-footer-sep">·</span>
            <button
              type="button"
              className="cpw-footer-link cpw-footer-link-btn"
              onClick={() => {
                window.open('https://marketplace.atlassian.com/search?query=workflow', '_blank', 'noopener');
              }}
            >
              Sample data &amp; marketplace
            </button>
          </div>
        )}

        <footer className="cpw-footer">
          {wizardState.step > 1 && (
            <button type="button" className="cpw-btn cpw-btn--secondary" onClick={handleBack}>
              Back
            </button>
          )}
          <div className="cpw-footer-spacer" />
          {wizardState.step < 4 ? (
            <button
              type="button"
              className="cpw-btn cpw-btn--primary"
              onClick={handleNext}
              disabled={!canProceed()}
            >
              {wizardState.step === 1 && selected ? `Continue with ${selected.name}` : 'Next'}
            </button>
          ) : (
            <button
              type="button"
              className="cpw-btn cpw-btn--primary cpw-btn--create"
              onClick={handleSubmit}
              disabled={createMutation.isPending || !selected}
            >
              {createMutation.isPending ? 'Creating project…' : 'Create project'}
            </button>
          )}
        </footer>
      </div>
    </div>
  );
}

function TemplateSelectionStep({
  catalog,
  loading,
  error,
  searchQuery,
  onSearchChange,
  activeCategoryKey,
  activeCategory,
  onCategorySelect,
  selected,
  onSelect,
  catalogRaw,
}: {
  catalog: TemplateCatalog | null | undefined;
  loading: boolean;
  error: unknown;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  activeCategoryKey: string | null;
  activeCategory: TemplateCatalog['categories'][0] | undefined;
  onCategorySelect: (key: string) => void;
  selected: CatalogTemplate | null;
  onSelect: (t: CatalogTemplate) => void;
  catalogRaw: TemplateCatalog | undefined;
}) {
  if (loading) {
    return (
      <div className="cpw-loading">
        <div className="cpw-spinner" />
        <p>Loading project templates…</p>
      </div>
    );
  }

  if (error) {
    const axiosErr = error as { response?: { status?: number } };
    const status = axiosErr.response?.status;
    const message =
      status === 401
        ? 'Your session may have expired. Sign out and sign in again, then reopen Create project.'
        : status === 502 || status === 503
          ? 'Project service is temporarily unavailable. Wait a moment and try again.'
          : 'Check your connection and try again.';
    return (
      <div className="cpw-empty">
        <span className="cpw-empty-icon">⚠️</span>
        <h3>Unable to load templates</h3>
        <p>{message}</p>
      </div>
    );
  }

  if (!catalog?.categories.length) {
    return (
      <div className="cpw-empty">
        <span className="cpw-empty-icon">📋</span>
        <h3>No templates match your search</h3>
        <p>Try a different keyword or clear the search.</p>
      </div>
    );
  }

  return (
    <div className="cpw-template-step">
      <div className="cpw-template-toolbar">
        <div className="cpw-search-wrap">
          <span className="cpw-search-icon" aria-hidden>
            🔍
          </span>
          <input
            type="search"
            className="cpw-search"
            placeholder="Search templates, workflows, or features…"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            aria-label="Search templates"
          />
        </div>
        {catalogRaw && catalogRaw.recommended.length > 0 && !searchQuery && (
          <div className="cpw-recommended-strip">
            <span className="cpw-recommended-label">Recommended</span>
            {catalogRaw.recommended.map((t) => (
              <button
                key={t.id}
                type="button"
                className={[
                  'cpw-recommended-chip',
                  selected?.id === t.id ? 'cpw-recommended-chip--selected' : '',
                ]
                  .filter(Boolean)
                  .join(' ')}
                onClick={() => onSelect(t)}
              >
                <span>{t.iconEmoji || '📁'}</span> {t.name}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="cpw-two-panel">
        <aside className="cpw-sidebar" aria-label="Template categories">
          <ul className="cpw-category-list">
            {catalog.categories.map((cat) => (
              <li key={cat.categoryKey}>
                <button
                  type="button"
                  className={[
                    'cpw-category-btn',
                    activeCategoryKey === cat.categoryKey ? 'cpw-category-btn--active' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => onCategorySelect(cat.categoryKey)}
                >
                  <span className="cpw-category-emoji">{cat.iconEmoji}</span>
                  <span className="cpw-category-text">
                    <span className="cpw-category-name">{cat.name}</span>
                    <span className="cpw-category-count">{cat.templates.length} templates</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </aside>

        <div className="cpw-main-panel">
          {activeCategory && (
            <p className="cpw-category-desc">{activeCategory.description}</p>
          )}

          <div className="cpw-template-layout">
            <div className="cpw-template-list" role="listbox" aria-label="Templates">
              {activeCategory?.templates.map((t) => (
                <button
                  key={t.id}
                  type="button"
                  role="option"
                  aria-selected={selected?.id === t.id}
                  className={[
                    'cpw-template-item',
                    selected?.id === t.id ? 'cpw-template-item--selected' : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                  onClick={() => onSelect(t)}
                >
                  <span
                    className="cpw-template-item-icon"
                    style={{ backgroundColor: t.previewAccent || t.color }}
                  >
                    {t.iconEmoji || '📁'}
                  </span>
                  <span className="cpw-template-item-body">
                    <span className="cpw-template-item-name">{t.name}</span>
                    <span className="cpw-template-item-short">
                      {t.shortDescription || t.description}
                    </span>
                    <span className="cpw-template-item-type">{t.workflowTypeLabel}</span>
                  </span>
                </button>
              ))}
            </div>

            <div className="cpw-template-detail">
              {selected ? (
                <TemplateDetailPanel template={selected} />
              ) : (
                <div className="cpw-detail-placeholder">
                  <span className="cpw-detail-placeholder-icon">👈</span>
                  <h3>Select a template</h3>
                  <p>
                    Choose a template from the list to preview workflows, enabled modules, and
                    what your team gets after creation.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function TemplateDetailPanel({ template }: { template: CatalogTemplate }) {
  return (
    <div className="cpw-detail">
      <div className="cpw-detail-header">
        <span
          className="cpw-detail-icon"
          style={{ backgroundColor: template.previewAccent || template.color }}
        >
          {template.iconEmoji || '📁'}
        </span>
        <div>
          <h3 className="cpw-detail-title">{template.name}</h3>
          <span className="cpw-detail-workflow-type">{template.workflowTypeLabel}</span>
        </div>
      </div>

      <TemplatePreviewGraphic template={template} />

      <p className="cpw-detail-desc">{template.description}</p>

      {template.useCases && (
        <div className="cpw-detail-section">
          <h4>Best for</h4>
          <p>{template.useCases}</p>
        </div>
      )}

      {template.capabilities?.length > 0 && (
        <div className="cpw-detail-section">
          <h4>Enabled after creation</h4>
          <ul className="cpw-capability-list">
            {template.capabilities.map((c) => (
              <li key={c.key}>
                <span className="cpw-capability-check">✓</span>
                {c.label}
              </li>
            ))}
          </ul>
        </div>
      )}

      {template.instructions && (
        <div className="cpw-detail-tip">
          <strong>Tip:</strong> {template.instructions}
        </div>
      )}

      <div className="cpw-detail-summary">
        <span className="cpw-detail-summary-label">Selected</span>
        <span className="cpw-detail-summary-value">{template.name}</span>
      </div>
    </div>
  );
}

function DetailsStep({
  state,
  onChange,
  keyValidation,
  template,
}: {
  state: WizardState;
  onChange: React.Dispatch<React.SetStateAction<WizardState>>;
  keyValidation: { valid: boolean; available: boolean };
  template: CatalogTemplate | null;
}) {
  return (
    <div className="cpw-details-step">
      <div className="cpw-details-intro">
        <h3>Configure project details</h3>
        <p>
          {template ? (
            <>
              Creating a <strong>{template.name}</strong> project ({template.workflowTypeLabel}).
            </>
          ) : (
            'Enter the basic information for your project.'
          )}
        </p>
      </div>

      <div className="cpw-form">
        <div className="cpw-form-row">
          <label className="cpw-label" htmlFor="project-name">
            Project name <span className="cpw-required">*</span>
          </label>
          <input
            id="project-name"
            type="text"
            className="cpw-input"
            value={state.name}
            onChange={(e) => onChange((p) => ({ ...p, name: e.target.value }))}
            placeholder="e.g., Customer Portal"
            maxLength={200}
          />
        </div>

        <div className="cpw-form-row">
          <label className="cpw-label" htmlFor="project-key">
            Project key <span className="cpw-required">*</span>
          </label>
          <input
            id="project-key"
            type="text"
            className={`cpw-input ${!keyValidation.valid || !keyValidation.available ? 'cpw-input--error' : ''}`}
            value={state.projectKey}
            onChange={(e) =>
              onChange((p) => ({
                ...p,
                projectKey: e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''),
              }))
            }
            placeholder="e.g., CP"
            maxLength={10}
          />
          {!keyValidation.valid && (
            <span className="cpw-field-error">Key must be 2–10 uppercase letters or numbers.</span>
          )}
          {!keyValidation.available && keyValidation.valid && (
            <span className="cpw-field-error">This key is already in use.</span>
          )}
          <span className="cpw-field-hint">Used as the issue prefix (e.g., {state.projectKey || 'KEY'}-123). Cannot be changed later.</span>
        </div>

        <div className="cpw-form-row">
          <label className="cpw-label" htmlFor="project-desc">
            Description
          </label>
          <textarea
            id="project-desc"
            className="cpw-textarea"
            value={state.description}
            onChange={(e) => onChange((p) => ({ ...p, description: e.target.value }))}
            placeholder="What is this project for?"
            rows={3}
          />
        </div>

        <div className="cpw-form-grid">
          <div className="cpw-form-row">
            <label className="cpw-label" htmlFor="visibility">
              Visibility
            </label>
            <select
              id="visibility"
              className="cpw-select"
              value={state.visibility}
              onChange={(e) =>
                onChange((p) => ({
                  ...p,
                  visibility: e.target.value as 'PUBLIC' | 'PRIVATE',
                }))
              }
            >
              <option value="PUBLIC">Public — visible to all licensed users</option>
              <option value="PRIVATE">Private — visible to project members only</option>
            </select>
          </div>

          <div className="cpw-form-row">
            <label className="cpw-label" htmlFor="default-assignee">
              Default assignee
            </label>
            <select
              id="default-assignee"
              className="cpw-select"
              value={state.defaultAssigneeType}
              onChange={(e) => onChange((p) => ({ ...p, defaultAssigneeType: e.target.value }))}
            >
              <option value="PROJECT_LEAD">Project lead</option>
              <option value="UNASSIGNED">Unassigned</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  );
}

function ReviewStep({
  state,
  template,
  workflow,
  schemeDetails,
}: {
  state: WizardState;
  template: CatalogTemplate | null;
  workflow?: TemplateWithWorkflow;
  schemeDetails?: TemplateDetails;
}) {
  if (!template) return null;

  return (
    <div className="cpw-review-step">
      <h3>Review configuration</h3>
      <p className="cpw-review-intro">
        Confirm the template-driven setup before creating your project.
      </p>

      <div className="cpw-review-grid">
        <section className="cpw-review-card">
          <h4>Template</h4>
          <div className="cpw-review-template-row">
            <span className="cpw-review-template-icon">{template.iconEmoji}</span>
            <div>
              <strong>{template.name}</strong>
              <span>{template.workflowTypeLabel}</span>
            </div>
          </div>
          <p className="cpw-review-muted">{template.shortDescription}</p>
        </section>

        <section className="cpw-review-card">
          <h4>Project</h4>
          <dl className="cpw-review-dl">
            <div>
              <dt>Name</dt>
              <dd>{state.name}</dd>
            </div>
            <div>
              <dt>Key</dt>
              <dd>
                <code>{state.projectKey}</code>
              </dd>
            </div>
            <div>
              <dt>Visibility</dt>
              <dd>{state.visibility === 'PUBLIC' ? 'Public' : 'Private'}</dd>
            </div>
            <div>
              <dt>Management</dt>
              <dd>
                {template.projectTypeCategory === 'TEAM_MANAGED'
                  ? 'Team-managed'
                  : 'Company-managed'}
              </dd>
            </div>
          </dl>
        </section>

        <section className="cpw-review-card cpw-review-card--wide">
          <h4>Schemes &amp; modules</h4>
          <div className="cpw-scheme-grid">
            <SchemeItem label="Issue types" value={schemeDetails?.issueTypeSchemeName || workflow?.issueTypeScheme?.name} />
            <SchemeItem label="Workflow" value={schemeDetails?.workflowSchemeName || workflow?.workflowScheme?.name} />
            <SchemeItem label="Permissions" value={schemeDetails?.permissionSchemeName || workflow?.permissionScheme?.name} />
            <SchemeItem label="Notifications" value={schemeDetails?.notificationSchemeName || workflow?.notificationScheme?.name} />
            <SchemeItem label="Screens" value={schemeDetails?.screenSchemeName || workflow?.screenScheme?.name} />
          </div>

          {workflow?.issueTypes && workflow.issueTypes.length > 0 && (
            <div className="cpw-review-issue-types">
              <span className="cpw-review-subhead">Issue types</span>
              <div className="cpw-tag-list">
                {workflow.issueTypes.map((it) => (
                  <span key={it.id} className="cpw-tag">
                    {it.issueTypeName}
                    {it.isDefault ? ' (default)' : ''}
                  </span>
                ))}
              </div>
            </div>
          )}

          {template.capabilities?.length > 0 && (
            <div className="cpw-review-capabilities">
              <span className="cpw-review-subhead">Enabled modules</span>
              <ul>
                {template.capabilities.map((c) => (
                  <li key={c.key}>{c.label}</li>
                ))}
              </ul>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function SchemeItem({ label, value }: { label: string; value?: string }) {
  return (
    <div className="cpw-scheme-item">
      <span className="cpw-scheme-label">{label}</span>
      <span className="cpw-scheme-value">{value || 'Default'}</span>
    </div>
  );
}

function ConfirmStep({
  state,
  template,
  isPending,
  isError,
}: {
  state: WizardState;
  template: CatalogTemplate | null;
  isPending: boolean;
  isError: boolean;
}) {
  return (
    <div className="cpw-confirm-step">
      <div className="cpw-confirm-hero">
        <span className="cpw-confirm-icon">{template?.iconEmoji || '🚀'}</span>
        <h3>Ready to create {state.name}?</h3>
        <p>
          Your <strong>{template?.name}</strong> project will be provisioned with pre-configured
          workflows, issue types, boards, and reports. Click <strong>Create project</strong> to finish.
        </p>
      </div>

      <ul className="cpw-confirm-checklist">
        <li>Project key: <code>{state.projectKey}</code></li>
        <li>Template: {template?.name}</li>
        <li>{template?.capabilities?.length || 0} modules will be enabled</li>
      </ul>

      {isError && (
        <div className="cpw-alert cpw-alert--error" role="alert">
          Failed to create the project. Please try again.
        </div>
      )}

      {isPending && (
        <div className="cpw-loading cpw-loading--inline">
          <div className="cpw-spinner" />
          <p>Provisioning project schemes and navigation…</p>
        </div>
      )}
    </div>
  );
}
