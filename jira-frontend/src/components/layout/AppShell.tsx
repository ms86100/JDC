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
import { ADMIN_CATEGORIES } from './adminCategories';
import { Breadcrumbs } from '../ui/Breadcrumbs';

const COLLAPSE_KEY = 'sa.sidebar.collapsed';

const TOP_NAV: { label: string; path: string }[] = [
  { label: 'Dashboards', path: '/dashboard' },
  { label: 'Projects',   path: '/projects' },
  { label: 'Issues',     path: '/issues' },
  { label: 'Boards',     path: '/boards' },
  { label: 'Plans',      path: '/programs' },
];

const SIDE_NAV_WORK: { name: string; path: string }[] = [
  { name: 'Dashboard',     path: '/dashboard' },
  { name: 'Projects',      path: '/projects' },
  { name: 'Programs',      path: '/programs' },
  { name: 'Issues',        path: '/issues' },
  { name: 'Boards',        path: '/boards' },
  { name: 'Sprints',       path: '/sprints' },
  { name: 'Workflows',     path: '/workflows' },
  { name: 'Search',        path: '/search' },
  { name: 'Notifications', path: '/notifications' },
];

const SIDE_NAV_OPS: { name: string; path: string }[] = [
  { name: 'Administration', path: '/admin' },
  { name: 'Audit logs',     path: '/audit' },
  { name: 'Migration',      path: '/migration' },
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

        <div style={{ position: 'relative' }}>
          <span aria-hidden="true" style={{
            position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)',
            color: 'var(--sa-n500)', fontSize: 14,
          }}>⌕</span>
          <input id="sa-global-search" type="search" placeholder='Search ( / )'
                 onKeyDown={(e) => { if (e.key === 'Enter') navigate('/search'); }}
                 style={{
                   width: 240, height: 30, padding: '0 10px 0 30px',
                   background: 'var(--sa-n50)', border: '1px solid var(--sa-n200)',
                   borderRadius: 'var(--sa-radius-sm)', fontFamily: 'var(--sa-font-sans)',
                   fontSize: 'var(--sa-fs-sm)', color: 'var(--sa-n900)', outline: 'none',
                 }} />
        </div>

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
          <AdminSidebar pathname={location.pathname} />
        ) : (
          <WorkspaceSidebar
            collapsed={collapsed}
            onToggle={() => setCollapsed((c) => !c)}
            width={railWidth}
          />
        )}

        {/* MAIN */}
        <main id="sa-main" role="main" tabIndex={-1}
              style={{
                flex: 1, minWidth: 0, overflow: 'auto', background: 'var(--sa-n50)',
              }}>
          {meta.breadcrumbs.length > 0 && (
            <div style={{
              padding: 'var(--sa-space-3) var(--sa-space-6) 0',
              background: 'var(--sa-n50)',
            }}>
              <Breadcrumbs items={meta.breadcrumbs} />
            </div>
          )}
          <div style={{ padding: 'var(--sa-space-4) var(--sa-space-6) var(--sa-space-8)' }}>
            {children ?? <Outlet />}
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
  collapsed: boolean; onToggle: () => void; width: number;
}> = ({ collapsed, onToggle, width }) => (
  <aside aria-label="Navigation" style={{
    width, flexShrink: 0,
    display: 'flex', flexDirection: 'column',
    background: 'var(--sa-n0)', borderRight: '1px solid var(--sa-n200)',
    transition: 'width var(--sa-motion-base)',
  }}>
    <div style={{ flex: 1, padding: 'var(--sa-space-3) var(--sa-space-2)' }}>
      {!collapsed && <SideLabel>Work</SideLabel>}
      {SIDE_NAV_WORK.map((i) => <SideLink key={i.path} {...i} collapsed={collapsed} />)}
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

const SideLabel: React.FC<React.PropsWithChildren> = ({ children }) => (
  <div style={{
    padding: '8px 10px 4px', fontSize: 'var(--sa-fs-xs)',
    fontWeight: 700, letterSpacing: 0.6, textTransform: 'uppercase',
    color: 'var(--sa-n500)',
  }}>{children}</div>
);

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

const AdminSidebar: React.FC<{ pathname: string }> = ({ pathname }) => (
  <aside aria-label="Administration navigation" style={{
    width: 260, flexShrink: 0, overflow: 'auto',
    background: 'var(--sa-n0)', borderRight: '1px solid var(--sa-n200)',
    padding: 'var(--sa-space-3) var(--sa-space-2)',
  }}>
    {ADMIN_CATEGORIES.map((cat) => (
      <div key={cat.key} style={{ marginBottom: 'var(--sa-space-4)' }}>
        <SideLabel>{cat.label}</SideLabel>
        {cat.items.map((it) => {
          const active = pathname === it.path;
          return (
            <Link key={it.path} to={it.path}
                  style={{
                    display: 'block', padding: '5px 10px', margin: '1px 0',
                    borderRadius: 'var(--sa-radius-sm)',
                    color: active ? 'var(--sa-brand-700)' : 'var(--sa-n700)',
                    background: active ? 'var(--sa-brand-50)' : 'transparent',
                    textDecoration: 'none', fontSize: 'var(--sa-fs-sm)',
                  }}>{it.label}</Link>
          );
        })}
      </div>
    ))}
  </aside>
);

export default AppShell;
