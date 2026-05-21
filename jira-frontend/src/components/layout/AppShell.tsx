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
import {
  Outlet, NavLink, Link, useLocation, useNavigate,
} from 'react-router-dom';
import { useAuth } from '../../features/auth/context/AuthContext';
import { AppBrandMark } from './AppBrandMark';
import { metaFor } from './routeMeta';
import { AdminNavSidebar } from './AdminNavSidebar';
import { Breadcrumbs } from '../ui/Breadcrumbs';
import {
  getRouteContext,
  getProjectContextNav,
  getProgramContextNav,
  contextSectionLabel,
} from './contextNav';
import { useContextEntity } from './useContextEntity';

const COLLAPSE_KEY = 'sa.sidebar.collapsed';

const TOP_NAV: { label: string; path: string }[] = [
  { label: 'Dashboards', path: '/dashboard' },
  { label: 'Projects',   path: '/projects' },
  { label: 'Issues',     path: '/issues' },
  { label: 'Boards',     path: '/boards' },
  { label: 'Programs',   path: '/programs' },
];

const SIDE_NAV_WORK: { name: string; path: string }[] = [
  { name: 'Dashboard',     path: '/dashboard' },
  { name: 'Projects',      path: '/projects' },
  { name: 'Programs',      path: '/programs' },
  { name: 'Issues',        path: '/issues' },
  { name: 'Boards',        path: '/boards' },
  { name: 'Sprints',       path: '/sprints' },
  { name: 'Workflows',     path: '/workflows' },
  { name: 'Tests',         path: '/tests' },
  { name: 'Search',        path: '/search' },
  { name: 'Notifications', path: '/notifications' },
];

const SIDE_NAV_OPS: { name: string; path: string }[] = [
  { name: 'Administration', path: '/admin' },
  { name: 'Audit logs',     path: '/audit' },
  { name: 'Migration',      path: '/migration' },
];

// Tests submenu items
const TESTS_SUBMENU: { name: string; path: string; icon?: string }[] = [
  { name: 'Test Management', path: '/tests' },
  { name: 'Test Executions', path: '/tests/history' },
  { name: 'Shared Steps', path: '/tests/shared-steps' },
  { name: 'Datasets', path: '/tests/datasets' },
  { name: 'Flaky Tests', path: '/tests/flaky' },
  { name: 'Quarantine', path: '/tests/quarantine' },
  { name: 'Coverage', path: '/tests/coverage' },
  { name: 'Traceability', path: '/tests/traceability' },
  { name: 'Preconditions', path: '/tests/preconditions' },
  { name: 'Timeline', path: '/tests/timeline' },
  { name: 'Requirement Versions', path: '/tests/requirement-versions' },
  { name: 'Environment Matrix', path: '/tests/environment-matrix' },
  { name: 'Workflows', path: '/tests/workflows' },
  { name: 'Reporting', path: '/tests/reporting' },
  { name: 'Impact Analysis', path: '/tests/impact' },
  { name: 'Settings', path: '/tests/settings' },
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
  useEffect(() => {
    try {
      const v = localStorage.getItem(COLLAPSE_KEY);
      if (v === '1') setCollapsed(true);
    } catch { /* ignore */ }
  }, []);
  useEffect(() => {
    try { localStorage.setItem(COLLAPSE_KEY, collapsed ? '1' : '0'); } catch { /* ignore */ }
  }, [collapsed]);

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

  const meta = useMemo(() => metaFor(location.pathname), [location.pathname]);
  const routeContext = useMemo(() => getRouteContext(location.pathname), [location.pathname]);
  const {
    label: contextLabel,
    template: contextTemplate,
    subtitle: contextSubtitle,
    defaultBoardPath: contextBoardPath,
    isLoading: contextLoading,
  } = useContextEntity(routeContext);
  const contextNavItems = useMemo(() => {
    if (!routeContext) return [];
    if (routeContext.type === 'project') {
      return getProjectContextNav(routeContext.id, contextTemplate, contextBoardPath);
    }
    return getProgramContextNav(routeContext.id);
  }, [routeContext, contextTemplate, contextBoardPath]);
  const isAdmin = mode === 'admin' || location.pathname.startsWith('/admin');

  // Keep document title in sync.
  useEffect(() => {
    document.title = `${meta.title} · Systems and Avionics`;
  }, [meta.title]);

  const initials = (user?.username ?? 'U').slice(0, 2).toUpperCase();
  const railWidth = collapsed ? 56 : 232;

  const isTopActive = (p: string) => {
    if (p === '/dashboard') return location.pathname === '/' || location.pathname.startsWith('/dashboard');
    if (p === '/programs') return location.pathname.startsWith('/programs') || location.pathname.startsWith('/plans');
    return location.pathname === p || location.pathname.startsWith(p + '/');
  };

  return (
    <div style={{
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

      {/* TOP BAR (48px) */}
      <header role="banner" style={{
        height: 48, flexShrink: 0, display: 'flex', alignItems: 'center',
        padding: '0 var(--sa-space-3)', gap: 'var(--sa-space-4)',
        background: 'var(--sa-n0)', borderBottom: '1px solid var(--sa-n200)',
        boxShadow: 'var(--sa-elev-1)', zIndex: 'var(--sa-z-sticky)' as any,
      }}>
        <Link to="/dashboard" aria-label="Systems and Avionics — Home"
              style={{ display: 'inline-flex', alignItems: 'center', textDecoration: 'none' }}>
          <AppBrandMark size={22} />
        </Link>

        <nav aria-label="Primary" style={{ display: 'flex', gap: 2, marginLeft: 12 }}>
          {TOP_NAV.map((i) => {
            const active = isTopActive(i.path);
            return (
              <Link key={i.path} to={i.path}
                    style={{
                      padding: '6px 12px', borderRadius: 'var(--sa-radius-sm)',
                      fontSize: 'var(--sa-fs-sm)', fontWeight: 500,
                      color: active ? 'var(--sa-brand-700)' : 'var(--sa-n700)',
                      background: active ? 'var(--sa-brand-50)' : 'transparent',
                      textDecoration: 'none', transition: 'background var(--sa-motion-fast)',
                    }}>
                {i.label}
              </Link>
            );
          })}
        </nav>

        <button type="button"
                onClick={() => window.dispatchEvent(new CustomEvent('openCreateIssue'))}
                style={{
                  marginLeft: 8, display: 'inline-flex', alignItems: 'center', gap: 6,
                  padding: '6px 12px', height: 30,
                  background: 'var(--sa-brand-500)', color: 'var(--sa-n0)',
                  border: 0, borderRadius: 'var(--sa-radius-sm)',
                  fontFamily: 'var(--sa-font-sans)', fontSize: 'var(--sa-fs-sm)',
                  fontWeight: 600, cursor: 'pointer',
                }}>
          <span aria-hidden="true">+</span> Create
        </button>

        <div style={{ flex: 1 }} />

        {!isAdmin && (
          <div style={{ position: 'relative' }}>
            <input id="sa-global-search" type="search" placeholder="Search ( / )"
                   onKeyDown={(e) => { if (e.key === 'Enter') navigate('/search'); }}
                   style={{
                     width: 240, height: 30, padding: '0 12px',
                     background: 'var(--sa-n50)', border: '1px solid var(--sa-n200)',
                     borderRadius: 'var(--sa-radius-sm)', fontFamily: 'var(--sa-font-sans)',
                     fontSize: 'var(--sa-fs-sm)', color: 'var(--sa-n900)', outline: 'none',
                   }} />
          </div>
        )}

        <IconBtn title="Notifications" onClick={() => navigate('/notifications')}>🔔</IconBtn>
        <IconBtn title="Help">?</IconBtn>

        <div style={{ width: 1, height: 24, background: 'var(--sa-n200)', margin: '0 4px' }} />

        <button type="button" onClick={logout}
                title={`${user?.username ?? 'User'} — sign out`}
                style={{
                  width: 30, height: 30, borderRadius: '50%',
                  background: 'var(--sa-brand-600)', color: 'var(--sa-n0)',
                  border: 0, cursor: 'pointer', fontFamily: 'var(--sa-font-sans)',
                  fontSize: 'var(--sa-fs-xs)', fontWeight: 700,
                }}>
          {initials}
        </button>
      </header>

      {/* BODY */}
      <div style={{ display: 'flex', flex: 1, minHeight: 0 }}>
        {/* LEFT RAIL */}
        {isAdmin ? (
          <AdminNavSidebar pathname={location.pathname} />
        ) : (
          <WorkspaceSidebar
            collapsed={collapsed}
            onToggle={() => setCollapsed((c) => !c)}
            width={railWidth}
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
          className={isAdmin ? 'sa-admin-main' : undefined}
          style={isAdmin ? undefined : {
            flex: 1, minWidth: 0, overflow: 'auto', background: 'var(--sa-n50)',
          }}
        >
          <div className={isAdmin ? 'sa-admin-main-inner' : undefined}
               style={isAdmin ? undefined : { minHeight: '100%' }}>
            {!isAdmin && meta.breadcrumbs.length > 0 && (
              <div style={{
                padding: 'var(--sa-space-3) var(--sa-space-6) 0',
                background: 'var(--sa-n50)',
              }}>
                <Breadcrumbs items={meta.breadcrumbs} />
              </div>
            )}
            <div className={isAdmin ? 'sa-admin-content' : undefined}
                 style={isAdmin ? undefined : {
                   padding: 'var(--sa-space-4) var(--sa-space-6) var(--sa-space-8)',
                 }}>
              {children ?? <Outlet />}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
};

// -----------------------------------------------------------------------------
// Sub-components
// -----------------------------------------------------------------------------

const IconBtn: React.FC<React.PropsWithChildren<{ title: string; onClick?: () => void }>> = ({ title, onClick, children }) => (
  <button type="button" title={title} aria-label={title} onClick={onClick}
          style={{
            width: 30, height: 30, display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            background: 'transparent', color: 'var(--sa-n600)', border: 0,
            borderRadius: 'var(--sa-radius-sm)', cursor: 'pointer', fontSize: 14,
          }}>{children}</button>
);

const WorkspaceSidebar: React.FC<{
  collapsed: boolean;
  onToggle: () => void;
  width: number;
  routeContext: ReturnType<typeof getRouteContext>;
  contextLabel: string | null;
  contextSubtitle?: string;
  contextLoading: boolean;
  contextNavItems: ReturnType<typeof getProjectContextNav>;
  pathname: string;
  search: string;
}> = ({
  collapsed, onToggle, width, routeContext, contextLabel, contextSubtitle,
  contextLoading, contextNavItems, pathname, search,
}) => {
  const filteredWork = SIDE_NAV_WORK.filter((i) => i.path !== '/tests');
  return (
  <aside aria-label="Navigation" className="sa-workspace-sidebar" style={{
    width, flexShrink: 0,
    display: 'flex', flexDirection: 'column',
    background: 'var(--sa-n0)', borderRight: '1px solid var(--sa-n200)',
    transition: 'width var(--sa-motion-base)',
  }}>
    <div style={{ flex: 1, padding: 'var(--sa-space-3) var(--sa-space-2)', overflowY: 'auto' }}>
      {routeContext && contextNavItems.length > 0 && (
        <div className="sa-context-nav-block">
          {!collapsed && (
            <div className="sa-context-nav-heading">
              <SideLabel>{contextSectionLabel(routeContext.type)}</SideLabel>
              {contextLoading ? (
                <span className="sa-context-entity-name sa-context-entity-name--loading">Loading…</span>
              ) : contextLabel ? (
                <>
                  <span className="sa-context-entity-name" title={contextLabel}>{contextLabel}</span>
                  {contextSubtitle && (
                    <span className="sa-context-entity-sub">{contextSubtitle}</span>
                  )}
                </>
              ) : null}
            </div>
          )}
          {contextNavItems.map((i) => (
            <ContextSideLink
              key={i.path}
              name={i.name}
              path={i.path}
              end={i.end}
              collapsed={collapsed}
              pathname={pathname}
              search={search}
            />
          ))}
          <div className="sa-context-nav-divider" />
        </div>
      )}
      {!collapsed && <SideLabel>Work</SideLabel>}
      {filteredWork.map((i) => <SideLink key={i.path} {...i} collapsed={collapsed} />)}
      <div style={{ height: 6 }} />
      <TestsSubmenu collapsed={collapsed} pathname={pathname} />
      <div style={{ height: 12 }} />
      {!collapsed && <SideLabel>Operations</SideLabel>}
      {SIDE_NAV_OPS.map((i) => <SideLink key={i.path} {...i} collapsed={collapsed} />)}
    </div>
    <div style={{ padding: 'var(--sa-space-2)', borderTop: '1px solid var(--sa-n100)' }}>
      <button type="button" onClick={onToggle}
              title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              style={{
                width: '100%', height: 28, background: 'transparent',
                color: 'var(--sa-n600)', border: '1px solid var(--sa-n200)',
                borderRadius: 'var(--sa-radius-sm)', cursor: 'pointer',
                fontFamily: 'var(--sa-font-sans)', fontSize: 'var(--sa-fs-sm)',
              }}>{collapsed ? '»' : '‹'}</button>
    </div>
  </aside>
  );
};

const SideLabel: React.FC<React.PropsWithChildren> = ({ children }) => (
  <div style={{
    padding: '8px 10px 4px', fontSize: 'var(--sa-fs-xs)',
    fontWeight: 700, letterSpacing: 0.6, textTransform: 'uppercase',
    color: 'var(--sa-n500)',
  }}>{children}</div>
);

const ContextSideLink: React.FC<{
  name: string; path: string; end?: boolean; collapsed: boolean; pathname: string; search: string;
}> = ({ name, path, end, collapsed, pathname, search }) => {
  const [pathOnly, query] = path.includes('?') ? path.split('?') : [path, ''];
  const currentParams = new URLSearchParams(search.startsWith('?') ? search.slice(1) : search);
  const linkParams = new URLSearchParams(query);

  const isActive = (() => {
    if (pathOnly === '/boards' && linkParams.has('boardId')) {
      return pathname === '/boards' && currentParams.get('boardId') === linkParams.get('boardId');
    }
    if (query) {
      return pathname === pathOnly && search === `?${query}`;
    }
    if (end) {
      return pathname === pathOnly && !search;
    }
    return pathname === pathOnly || pathname.startsWith(pathOnly + '/');
  })();
  return (
    <NavLink
      to={path}
      end={end}
      title={collapsed ? name : undefined}
      className="sa-context-link"
      style={({ isActive: navActive }) => ({
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '5px 10px 5px 14px', margin: '1px 0',
        borderRadius: 'var(--sa-radius-sm)',
        borderLeft: navActive || isActive ? '2px solid var(--sa-brand-600)' : '2px solid transparent',
        color: navActive || isActive ? 'var(--sa-brand-700)' : 'var(--sa-n700)',
        background: navActive || isActive ? 'var(--sa-brand-50)' : 'transparent',
        textDecoration: 'none', fontSize: 'var(--sa-fs-sm)', fontWeight: 500,
        whiteSpace: 'nowrap', overflow: 'hidden',
      })}
    >
      {!collapsed && <span>{name}</span>}
    </NavLink>
  );
};

const SideLink: React.FC<{ name: string; path: string; collapsed: boolean }> = ({ name, path, collapsed }) => (
  <NavLink to={path} end={path === '/admin'}
           title={collapsed ? name : undefined}
           style={({ isActive }) => ({
             display: 'flex', alignItems: 'center', gap: 10,
             padding: '6px 10px', margin: '1px 0',
             borderRadius: 'var(--sa-radius-sm)',
             color: isActive ? 'var(--sa-brand-700)' : 'var(--sa-n700)',
             background: isActive ? 'var(--sa-brand-50)' : 'transparent',
             textDecoration: 'none', fontSize: 'var(--sa-fs-sm)', fontWeight: 500,
             whiteSpace: 'nowrap', overflow: 'hidden',
           })}>
    <span aria-hidden="true" style={{
      width: 6, height: 6, borderRadius: '50%', background: 'currentColor',
      flexShrink: 0, opacity: 0.55,
    }} />
    {!collapsed && <span>{name}</span>}
  </NavLink>
);

// Tests Submenu Component
const TestsSubmenu: React.FC<{ collapsed: boolean; pathname: string }> = ({ collapsed, pathname }) => {
  const [expanded, setExpanded] = useState(false);
  const isTestsActive = pathname.startsWith('/tests');

  return (
    <div style={{ marginBottom: 2 }}>
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        title={collapsed ? 'Tests' : undefined}
        style={{
          display: 'flex', alignItems: 'center', gap: 10,
          width: '100%', padding: '6px 10px', margin: '1px 0',
          borderRadius: 'var(--sa-radius-sm)',
          color: isTestsActive ? 'var(--sa-brand-700)' : 'var(--sa-n700)',
          background: isTestsActive ? 'var(--sa-brand-50)' : 'transparent',
          border: 0, cursor: 'pointer', fontFamily: 'var(--sa-font-sans)',
          fontSize: 'var(--sa-fs-sm)', fontWeight: 500,
          textDecoration: 'none', whiteSpace: 'nowrap', overflow: 'hidden',
        }}
      >
        <span aria-hidden="true" style={{
          width: 6, height: 6, borderRadius: '50%', background: 'currentColor',
          flexShrink: 0, opacity: isTestsActive ? 1 : 0.55,
        }} />
        {!collapsed && (
          <>
            <span style={{ flex: 1, textAlign: 'left' }}>Tests</span>
            <span style={{ fontSize: 10, transform: expanded ? 'rotate(90deg)' : 'rotate(0)', transition: 'transform 0.15s' }}>▶</span>
          </>
        )}
      </button>
      {!collapsed && expanded && (
        <div style={{
          marginLeft: 12, borderLeft: '1px solid var(--sa-n200)', paddingLeft: 8,
        }}>
          {TESTS_SUBMENU.map((item) => {
            const isActive = pathname === item.path || (item.path !== '/tests' && pathname.startsWith(item.path));
            return (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/tests'}
                title={item.name}
                style={({ isActive: navActive }) => ({
                  display: 'block',
                  padding: '4px 10px', margin: '1px 0',
                  borderRadius: 'var(--sa-radius-sm)',
                  color: navActive || isActive ? 'var(--sa-brand-700)' : 'var(--sa-n600)',
                  background: navActive || isActive ? 'var(--sa-brand-50)' : 'transparent',
                  textDecoration: 'none', fontSize: 'var(--sa-fs-xs)',
                  fontWeight: navActive || isActive ? 500 : 400,
                  whiteSpace: 'nowrap', overflow: 'hidden',
                })}
              >
                {item.name}
              </NavLink>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default AppShell;
