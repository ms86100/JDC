/**
 * Workspace navigation — category rail + hover flyout (same pattern as AdminNavSidebar).
 */
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  WORKSPACE_NAV_CATEGORIES,
  WORKSPACE_NAV_OPERATIONS,
  categoryForWorkspacePath,
  isWorkspaceItemActive,
  type WorkspaceNavCategory,
} from './workspaceNavCategories';
import { contextSectionLabel, type ContextNavItem } from './contextNav';
import type { RouteContext } from './contextNav';

const HIDE_DELAY_MS = 180;

function categoryHasActiveChild(
  pathname: string,
  search: string,
  cat: WorkspaceNavCategory,
): boolean {
  return cat.items.some((it) => isWorkspaceItemActive(pathname, search, it.path));
}

export const WorkspaceNavSidebar: React.FC<{
  pathname: string;
  search: string;
  collapsed: boolean;
  onToggleCollapse: () => void;
  routeContext: RouteContext | null;
  contextLabel: string | null;
  contextSubtitle?: string;
  contextLoading: boolean;
  contextNavItems: ContextNavItem[];
}> = ({
  pathname,
  search,
  collapsed,
  onToggleCollapse,
  routeContext,
  contextLabel,
  contextSubtitle,
  contextLoading,
  contextNavItems,
}) => {
  const routeCategory = useMemo(
    () => categoryForWorkspacePath(pathname, search),
    [pathname, search],
  );
  const [hoveredKey, setHoveredKey] = useState<string | null>(null);
  const hideTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const allCategories = useMemo(
    () => [...WORKSPACE_NAV_CATEGORIES, ...WORKSPACE_NAV_OPERATIONS],
    [],
  );

  const flyoutCategory = useMemo(() => {
    if (!hoveredKey) return null;
    return allCategories.find((c) => c.key === hoveredKey) ?? null;
  }, [hoveredKey, allCategories]);

  const openFlyout = (key: string) => {
    if (hideTimer.current) {
      clearTimeout(hideTimer.current);
      hideTimer.current = null;
    }
    setHoveredKey(key);
  };

  const scheduleCloseFlyout = () => {
    if (hideTimer.current) clearTimeout(hideTimer.current);
    hideTimer.current = setTimeout(() => {
      setHoveredKey(null);
      hideTimer.current = null;
    }, HIDE_DELAY_MS);
  };

  const cancelCloseFlyout = () => {
    if (hideTimer.current) {
      clearTimeout(hideTimer.current);
      hideTimer.current = null;
    }
  };

  useEffect(() => () => {
    if (hideTimer.current) clearTimeout(hideTimer.current);
  }, []);

  const renderCategoryButton = (cat: WorkspaceNavCategory) => {
    const isHovered = hoveredKey === cat.key;
    const hasActiveChild = categoryHasActiveChild(pathname, search, cat);
    const isRouteSection = routeCategory === cat.key;
    return (
      <button
        key={cat.key}
        type="button"
        className={[
          'sa-workspace-category-btn',
          isHovered ? 'is-hovered' : '',
          hasActiveChild ? 'has-active-child' : '',
          isRouteSection ? 'is-route-section' : '',
        ]
          .filter(Boolean)
          .join(' ')}
        onMouseEnter={() => openFlyout(cat.key)}
        onFocus={() => openFlyout(cat.key)}
        onBlur={scheduleCloseFlyout}
        aria-expanded={hoveredKey === cat.key}
        aria-haspopup="true"
        title={collapsed ? cat.label : undefined}
      >
        <span className="sa-workspace-category-icon" aria-hidden="true">
          {cat.icon}
        </span>
        <span className="sa-workspace-category-label">{cat.label}</span>
        <span className="sa-workspace-category-chevron" aria-hidden="true">
          ›
        </span>
      </button>
    );
  };

  return (
    <aside
      aria-label="Workspace navigation"
      className={[
        'sa-workspace-nav',
        collapsed ? 'sa-workspace-nav--collapsed' : '',
        flyoutCategory ? 'sa-workspace-nav--flyout-open' : '',
      ]
        .filter(Boolean)
        .join(' ')}
      onMouseLeave={scheduleCloseFlyout}
    >
      <div className="sa-workspace-nav-rail">
        {routeContext && contextNavItems.length > 0 && (
          <div className="sa-workspace-context">
            {!collapsed && (
              <div className="sa-context-nav-heading">
                <div className="sa-workspace-nav-section-title" style={{ paddingTop: 0 }}>
                  {contextSectionLabel(routeContext.type)}
                </div>
                {contextLoading ? (
                  <span className="sa-context-entity-name sa-context-entity-name--loading">
                    Loading…
                  </span>
                ) : contextLabel ? (
                  <>
                    <span className="sa-context-entity-name" title={contextLabel}>
                      {contextLabel}
                    </span>
                    {contextSubtitle && (
                      <span className="sa-context-entity-sub">{contextSubtitle}</span>
                    )}
                  </>
                ) : null}
              </div>
            )}
            {contextNavItems.map((i, idx) => {
              const active = (() => {
                const [pathOnly, query] = i.path.includes('?') ? i.path.split('?') : [i.path, ''];
                if (query) {
                  return pathname === pathOnly && search === `?${query}`;
                }
                if (i.end) return pathname === pathOnly && !search;
                return pathname === pathOnly || pathname.startsWith(`${pathOnly}/`);
              })();
              return (
                <Link
                  key={`${i.id ?? 'ctx'}-${i.path}-${idx}`}
                  to={i.path}
                  title={collapsed ? i.name : undefined}
                  className="sa-context-link"
                  style={{
                    display: 'flex',
                    padding: collapsed ? '8px' : '5px 14px',
                    margin: '1px 8px',
                    borderRadius: 'var(--sa-radius-sm)',
                    fontSize: 'var(--sa-fs-sm)',
                    fontWeight: 500,
                    textDecoration: 'none',
                    justifyContent: collapsed ? 'center' : 'flex-start',
                    background: active ? 'rgba(255,255,255,0.14)' : 'transparent',
                    borderLeft: active ? '2px solid #66a3ff' : '2px solid transparent',
                  }}
                >
                  {collapsed ? i.name.charAt(0).toUpperCase() : i.name}
                </Link>
              );
            })}
          </div>
        )}

        <div className="sa-workspace-nav-section-title">Work</div>
        {WORKSPACE_NAV_CATEGORIES.map((cat) => renderCategoryButton(cat))}

        <div className="sa-workspace-nav-section-title" style={{ marginTop: 8 }}>
          Operations
        </div>
        {WORKSPACE_NAV_OPERATIONS.map((cat) => renderCategoryButton(cat))}

        {!hoveredKey && !collapsed && (
          <p className="sa-workspace-nav-hint" aria-live="polite">
            Hover a section to browse destinations
          </p>
        )}

        <div className="sa-workspace-sidebar-collapse">
          <button
            type="button"
            onClick={onToggleCollapse}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? '»' : '‹'}
          </button>
        </div>
      </div>

      {flyoutCategory && (
        <nav
          className={`sa-workspace-nav-flyout${flyoutCategory ? ' is-visible' : ''}`}
          aria-label={`${flyoutCategory.label} destinations`}
          onMouseEnter={cancelCloseFlyout}
          onMouseLeave={scheduleCloseFlyout}
        >
          <div className="sa-workspace-nav-flyout-header">
            <span className="sa-workspace-nav-flyout-icon" aria-hidden="true">
              {flyoutCategory.icon}
            </span>
            <div>
              <div className="sa-workspace-nav-flyout-title">{flyoutCategory.label}</div>
              <div className="sa-workspace-nav-flyout-sub">
                {flyoutCategory.items.length} destinations
              </div>
            </div>
          </div>
          <ul className="sa-workspace-nav-items-list">
            {flyoutCategory.items.map((it) => {
              const active = isWorkspaceItemActive(pathname, search, it.path);
              const to = it.path.includes('?') ? it.path : it.path;
              return (
                <li key={`${flyoutCategory.key}-${it.path}-${it.label}`}>
                  <Link
                    to={to}
                    className={`sa-workspace-nav-link${active ? ' is-active' : ''}`}
                    aria-current={active ? 'page' : undefined}
                  >
                    {it.label}
                    {it.description && (
                      <span className="sa-workspace-nav-link-desc">{it.description}</span>
                    )}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>
      )}
    </aside>
  );
};

export default WorkspaceNavSidebar;
