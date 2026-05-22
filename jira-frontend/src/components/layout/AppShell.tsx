/**
 * Systems and Avionics — single application shell.
 * Two modes: 'workspace' (top + left rail + main) and 'admin' (top + 2-pane).
 *
 * Replaces three previous shells (AppLayout, JiraAdminLayout, JiraGlobalLayout)
 * via thin compatibility shims. Pages and routing are unchanged.
 *
 * Token contract: all colours/spacing/typography come from --sa-* tokens
 * defined in src/styles/tokens.css. No reliance on jira-classic.css here.
 *
 * Persistence: sidebar collapse state is kept under 'sa.sidebar.collapsed'.
 * The auth key 'accessToken' is NEVER read or written by this component.
 */
import React, { useEffect, useMemo, useState } from 'react';
import CreateIssueModal from '../../features/issues/components/CreateIssueModal';
import CreateProjectWizard from '../../features/projects/components/CreateProjectWizard';
import {
  Outlet, Link, useLocation, useNavigate,
} from 'react-router-dom';
import { useAuth } from '../../features/auth/context/AuthContext';
import { AppBrandMark } from './AppBrandMark';
import PlansTopNavDropdown from './PlansTopNavDropdown';
import MoreTopNavDropdown from './MoreTopNavDropdown';
import GlobalCreateMenu from './GlobalCreateMenu';
import { JIRA_DC_PRIMARY_TOP_NAV } from './jiraDcNavRegistry';
import { metaFor } from './routeMeta';
import { AdminNavSidebar } from './AdminNavSidebar';
import { WorkspaceNavSidebar } from './WorkspaceNavSidebar';
import { Breadcrumbs } from '../ui/Breadcrumbs';
import {
  getRouteContext,
  getProjectContextNav,
  getProgramContextNav,
  getPlanContextNav,
  contextSectionLabel,
} from './contextNav';
import { useContextEntity } from './useContextEntity';
import WebsudoBanner from './WebsudoBanner';

const COLLAPSE_KEY = 'sa.sidebar.collapsed';

/** Jira DC primary bar + Plans + More (Tests/Migration/etc. stay discoverable). */
const TOP_NAV: { label: string; path: string; isPlans?: boolean; isMore?: boolean }[] = [
  ...JIRA_DC_PRIMARY_TOP_NAV.map((i) => ({ label: i.label, path: i.path })),
  { label: 'Plans', path: '/plans', isPlans: true },
  { label: 'More', path: '/more', isMore: true },
];

export interface AppShellProps {
  mode?: 'workspace' | 'admin';
  children?: React.ReactNode;
}

export const AppShell: React.FC<AppShellProps> = ({ mode, children }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  // SSR-safe default; rehydrate from localStorage on mount.
  const [collapsed, setCollapsed] = useState(false);
  const [showGlobalCreateIssue, setShowGlobalCreateIssue] = useState(false);
  const [showGlobalCreateProject, setShowGlobalCreateProject] = useState(false);
  useEffect(() => {
    try {
      const v = localStorage.getItem(COLLAPSE_KEY);
      if (v === '1') setCollapsed(true);
    } catch { /* ignore */ }
  }, []);
  useEffect(() => {
    try { localStorage.setItem(COLLAPSE_KEY, collapsed ? '1' : '0'); } catch { /* ignore */ }
  }, [collapsed]);

  useEffect(() => {
    const onIssue = () => setShowGlobalCreateIssue(true);
    const onProject = () => setShowGlobalCreateProject(true);
    window.addEventListener('openCreateIssue', onIssue);
    window.addEventListener('openCreateProject', onProject);
    return () => {
      window.removeEventListener('openCreateIssue', onIssue);
      window.removeEventListener('openCreateProject', onProject);
    };
  }, []);

  // Global `/` to focus search.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === '/' && !['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) {
        const el = document.getElementById('sa-global-search') as HTMLInputElement | null;
        if (el) { e.preventDefault(); el.focus(); }
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, []);

  const routeContext = useMemo(() => getRouteContext(location.pathname), [location.pathname]);
  const {
    label: contextLabel,
    template: contextTemplate,
    subtitle: contextSubtitle,
    defaultBoardPath: contextBoardPath,
    isLoading: contextLoading,
  } = useContextEntity(routeContext);
  const meta = useMemo(
    () =>
      metaFor(location.pathname, {
        entityLabel: contextLabel ?? undefined,
        entitySubtitle: contextSubtitle ?? undefined,
      }),
    [location.pathname, contextLabel, contextSubtitle],
  );
  const contextNavItems = useMemo(() => {
    if (!routeContext) return [];
    if (routeContext.type === 'project') {
      return getProjectContextNav(routeContext.id, contextTemplate, contextBoardPath);
    }
    if (routeContext.type === 'plan') {
      return getPlanContextNav(routeContext.id);
    }
    return getProgramContextNav(routeContext.id);
  }, [routeContext, contextTemplate, contextBoardPath]);
  const isAdmin = mode === 'admin' || location.pathname.startsWith('/admin');

  // Keep document title in sync.
  useEffect(() => {
    document.title = `${meta.title} · Systems and Avionics`;
  }, [meta.title]);

  const initials = (user?.username ?? 'U').slice(0, 2).toUpperCase();

  const isTopActive = (p: string) => {
    if (p === '/dashboard') return location.pathname === '/' || location.pathname.startsWith('/dashboard');
    if (p === '/plans') return location.pathname.startsWith('/programs') || location.pathname.startsWith('/plans');
    return location.pathname === p || location.pathname.startsWith(p + '/');
  };

  const openCreateIssue = () => window.dispatchEvent(new CustomEvent('openCreateIssue'));

  const isBoardFullBleed =
    !isAdmin &&
    (location.pathname.startsWith('/board/') ||
      location.pathname === '/kanban' ||
      location.pathname.startsWith('/kanban/'));

  const isSplitView =
    !isAdmin &&
    (isBoardFullBleed ||
      location.pathname.startsWith('/issues') ||
      location.pathname.startsWith('/plans/') ||
      location.pathname.startsWith('/programs/') ||
      /^\/projects\/[^/]+/.test(location.pathname));

  return (
    <div
      className={isAdmin ? undefined : 'sa-shell-dc'}
      style={{
      display: 'flex', flexDirection: 'column', height: '100vh',
      background: 'var(--sa-n50)', color: 'var(--sa-n900)',
      fontFamily: 'var(--sa-font-sans)', fontSize: 'var(--sa-fs-base)',
      lineHeight: 'var(--sa-lh-base)', overflow: 'hidden',
    }}>
      {/* Skip link */}
      <a href="#sa-main"
         style={{
           position: 'absolute', left: -9999, top: 8,
           background: 'var(--sa-brand-600)', color: 'var(--sa-n0)',
           padding: '8px 12px', borderRadius: 'var(--sa-radius-sm)', zIndex: 9999,
         }}
         onFocus={(e) => (e.currentTarget.style.left = '8px')}
         onBlur={(e) => (e.currentTarget.style.left = '-9999px')}>
        Skip to main content
      </a>

      {/* TOP BAR */}
      <header
        role="banner"
        className={isAdmin ? 'sa-header sa-header--admin' : 'sa-header sa-header--dc'}
        style={{
          height: 48,
          flexShrink: 0,
          background: isAdmin ? 'var(--sa-n0)' : undefined,
          borderBottom: isAdmin ? '1px solid var(--sa-n200)' : undefined,
          boxShadow: isAdmin ? 'var(--sa-elev-1)' : undefined,
        }}
      >
        <div className="sa-dc-header-brand">
          <Link to="/dashboard" aria-label="Systems and Avionics — Home" className="sa-dc-header-brand-link">
            <AppBrandMark size={22} inverted={!isAdmin} />
          </Link>
        </div>

        <nav aria-label="Primary" className="sa-dc-primary-nav">
          {TOP_NAV.map((i) => {
            const active = isTopActive(i.path);
            if (i.isPlans) {
              return <PlansTopNavDropdown key={i.path} active={active} />;
            }
            if (i.isMore) {
              return <MoreTopNavDropdown key={i.path} active={active} />;
            }
            return (
              <Link key={i.path} to={i.path}
                    className={`sa-dc-nav-link ${active ? 'active' : ''}`}
                    style={{
                      padding: '6px 12px', borderRadius: 'var(--jdc-radius, 3px)',
                      fontSize: 'var(--sa-fs-sm)', fontWeight: 500,
                      textDecoration: 'none',
                    }}>
                {i.label}
              </Link>
            );
          })}
        </nav>

        {!isAdmin && <GlobalCreateMenu onCreateIssue={openCreateIssue} />}

        <div className="sa-dc-header-spacer" aria-hidden="true" />

        {!isAdmin && (
          <div className="sa-dc-header-search">
            <div className="jdc-search-field jdc-search-field--header">
              <span className="jdc-search-field-icon" aria-hidden="true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="7" />
                  <path d="M20 20L16 16" strokeLinecap="round" />
                </svg>
              </span>
              <input
                id="sa-global-search"
                type="search"
                placeholder="Search issues, projects…"
                className="sa-dc-search jdc-input jdc-input--search"
                onKeyDown={(e) => {
                  if (e.key === 'Enter') navigate('/search');
                }}
              />
            </div>
          </div>
        )}

        <div className="sa-dc-header-actions">
        <IconBtn title="Notifications" className="sa-dc-icon-btn" onClick={() => navigate('/notifications')}>🔔</IconBtn>
        <IconBtn
          title="Help & capability map"
          className="sa-dc-icon-btn"
          onClick={() => navigate('/migration?view=catalog')}
        >
          ?
        </IconBtn>
        {isAdmin ? (
          <Link
            to="/dashboard"
            className="sa-dc-nav-link"
            style={{ padding: '6px 12px', fontSize: 'var(--sa-fs-sm)', textDecoration: 'none' }}
          >
            Back to app
          </Link>
        ) : (
          <IconBtn title="Administration" className="sa-dc-icon-btn" onClick={() => navigate('/admin')}>⚙</IconBtn>
        )}

        <div style={{ width: 1, height: 24, background: isAdmin ? 'var(--sa-n200)' : 'rgba(255,255,255,0.35)', margin: '0 4px' }} />

        <button type="button" onClick={logout} className="sa-dc-user-avatar"
                title={`${user?.username ?? 'User'} — sign out`}>
          {initials}
        </button>
        </div>
      </header>

      {!isAdmin && <WebsudoBanner />}

      {/* BODY — admin row sits below header with a clear seam (no rail flush against top bar) */}
      <div className={isAdmin ? 'sa-admin-body-row' : undefined} style={{ display: 'flex', flex: 1, minHeight: 0 }}>
        {/* LEFT RAIL */}
        {isAdmin ? (
          <AdminNavSidebar pathname={location.pathname} />
        ) : (
          <WorkspaceNavSidebar
            collapsed={collapsed}
            onToggleCollapse={() => setCollapsed((c) => !c)}
            routeContext={routeContext}
            contextLabel={contextLabel}
            contextSubtitle={contextSubtitle}
            contextLoading={contextLoading}
            contextNavItems={contextNavItems}
            pathname={location.pathname}
            search={location.search}
          />
        )}

        {/* MAIN */}
        <main
          id="sa-main"
          role="main"
          tabIndex={-1}
          className={
            isAdmin
              ? 'sa-admin-main'
              : `sa-main-workspace${isSplitView ? ' sa-main--split-view' : ''}`
          }
        >
          <div className={isAdmin ? 'sa-admin-main-inner' : 'sa-main-workspace-inner'}>
            {!isSplitView && meta.breadcrumbs.length > 0 && (
              <div className="sa-main-breadcrumbs">
                <Breadcrumbs items={meta.breadcrumbs} />
              </div>
            )}
            <div className={isAdmin ? 'sa-admin-content' : 'sa-main-workspace-content'}>
              {children ?? <Outlet />}
            </div>
          </div>
        </main>
      </div>
      {!isAdmin && showGlobalCreateIssue && (
        <CreateIssueModal
          onClose={() => setShowGlobalCreateIssue(false)}
          onSuccess={() => setShowGlobalCreateIssue(false)}
        />
      )}
      {!isAdmin && showGlobalCreateProject && (
        <CreateProjectWizard onClose={() => setShowGlobalCreateProject(false)} />
      )}
    </div>
  );
};

// -----------------------------------------------------------------------------
// Sub-components
// -----------------------------------------------------------------------------

const IconBtn: React.FC<React.PropsWithChildren<{ title: string; onClick?: () => void; className?: string }>> = ({
  title, onClick, className, children,
}) => (
  <button type="button" title={title} aria-label={title} onClick={onClick}
          className={className}
          style={{
            width: 30, height: 30, display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            background: 'transparent', color: 'inherit', border: 0,
            borderRadius: 'var(--sa-radius-sm)', cursor: 'pointer', fontSize: 14,
          }}>{children}</button>
);

export default AppShell;
