/**
 * Admin navigation — main menu always visible; submenu flyout only on hover/focus.
 */
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ADMIN_CATEGORIES } from './adminCategories';

const HIDE_DELAY_MS = 180;

function categoryForPath(pathname: string): string {
  for (const cat of ADMIN_CATEGORIES) {
    if (cat.items.some((it) => pathname === it.path || pathname.startsWith(`${it.path}/`))) {
      return cat.key;
    }
  }
  if (pathname === '/admin' || pathname === '/admin/') return 'system';
  return ADMIN_CATEGORIES[0].key;
}

function isItemActive(pathname: string, path: string): boolean {
  if (path === '/admin/users' && pathname.startsWith('/admin/users')) {
    return pathname === path || pathname.startsWith(`${path}/`);
  }
  return pathname === path || pathname.startsWith(`${path}/`);
}

export const AdminNavSidebar: React.FC<{ pathname: string }> = ({ pathname }) => {
  const routeCategory = useMemo(() => categoryForPath(pathname), [pathname]);
  const [hoveredKey, setHoveredKey] = useState<string | null>(null);
  const hideTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const flyoutCategory = useMemo(() => {
    if (!hoveredKey) return null;
    return ADMIN_CATEGORIES.find((c) => c.key === hoveredKey) ?? null;
  }, [hoveredKey]);

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

  return (
    <div
      className={`dc-admin-nav${flyoutCategory ? ' dc-admin-nav--flyout-open' : ''}`}
      aria-label="Administration navigation"
      onMouseLeave={scheduleCloseFlyout}
    >
      <nav className="dc-admin-nav-categories" aria-label="Configuration sections">
        <div className="dc-admin-nav-categories-title">Administration</div>
        {ADMIN_CATEGORIES.map((cat) => {
          const isHovered = hoveredKey === cat.key;
          const hasActiveChild = cat.items.some((it) => isItemActive(pathname, it.path));
          const isRouteSection = routeCategory === cat.key;
          return (
            <button
              key={cat.key}
              type="button"
              className={[
                'dc-admin-category-btn',
                isHovered ? 'is-hovered' : '',
                hasActiveChild ? 'has-active-child' : '',
                isRouteSection ? 'is-route-section' : '',
              ].filter(Boolean).join(' ')}
              onMouseEnter={() => openFlyout(cat.key)}
              onFocus={() => openFlyout(cat.key)}
              onBlur={scheduleCloseFlyout}
              aria-expanded={hoveredKey === cat.key}
              aria-haspopup="true"
            >
              <span className="dc-admin-category-icon" aria-hidden="true">
                {cat.icon}
              </span>
              <span className="dc-admin-category-label">{cat.label}</span>
              <span className="dc-admin-category-chevron" aria-hidden="true">›</span>
            </button>
          );
        })}
        <Link to="/admin" className="dc-admin-nav-overview">
          Overview
        </Link>
        {!hoveredKey && (
          <p className="dc-admin-nav-hint" aria-live="polite">
            Hover a section to browse its settings
          </p>
        )}
      </nav>

      {flyoutCategory && (
        <nav
          className="dc-admin-nav-flyout is-visible"
          aria-label={`${flyoutCategory.label} settings`}
          onMouseEnter={cancelCloseFlyout}
          onMouseLeave={scheduleCloseFlyout}
        >
          <div className="dc-admin-nav-flyout-header">
            <span className="dc-admin-nav-flyout-icon" aria-hidden="true">
              {flyoutCategory.icon}
            </span>
            <div>
              <div className="dc-admin-nav-flyout-title">{flyoutCategory.label}</div>
              <div className="dc-admin-nav-flyout-sub">
                {flyoutCategory.items.length} settings
              </div>
            </div>
          </div>
          <ul className="dc-admin-nav-items-list">
            {flyoutCategory.items.map((it) => {
              const active = isItemActive(pathname, it.path);
              return (
                <li key={it.path}>
                  <Link
                    to={it.path}
                    className={`dc-admin-nav-link${active ? ' is-active' : ''}`}
                    aria-current={active ? 'page' : undefined}
                  >
                    {it.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>
      )}
    </div>
  );
};

export default AdminNavSidebar;
