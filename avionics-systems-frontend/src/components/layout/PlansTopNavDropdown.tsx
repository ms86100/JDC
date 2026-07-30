import { useCallback, useEffect, useRef, useState, type CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { planApi } from '../../api/planApi';
import { getRecentPlanViews, recordRecentPlanView } from '../../features/plans/utils/recentPlanViews';

interface PlansTopNavDropdownProps {
  active: boolean;
}

const navLinkStyle: CSSProperties = {
  padding: '6px 12px',
  borderRadius: 'var(--jdc-radius, 3px)',
  fontSize: 'var(--sa-fs-sm, 14px)',
  fontWeight: 500,
  textDecoration: 'none',
  display: 'inline-flex',
  alignItems: 'center',
};

export default function PlansTopNavDropdown({ active }: PlansTopNavDropdownProps) {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);
  const [menuPos, setMenuPos] = useState<{ top: number; left: number } | null>(null);

  const { data: rawPlans } = useQuery({
    queryKey: ['plans-nav-recent'],
    queryFn: async () => {
      try {
        const res = await planApi.getPlans();
        const d = res.data;
        return Array.isArray(d) ? d : (d && Array.isArray(d.content)) ? d.content : [];
      } catch {
        return [];
      }
    },
    staleTime: 60_000,
    retry: 1,
  });
  const plans = Array.isArray(rawPlans) ? rawPlans : [];

  const recentIds = getRecentPlanViews();
  const recentPlans = recentIds
    .map((id: string) => plans.find((p: any) => p.id === id))
    .filter(Boolean)
    .slice(0, 5) as typeof plans;

  const fallbackRecent = plans.slice(0, 3);
  const displayRecent = recentPlans.length > 0 ? recentPlans : fallbackRecent;

  const updateMenuPos = useCallback(() => {
    if (!wrapRef.current) return;
    const r = wrapRef.current.getBoundingClientRect();
    setMenuPos({ top: r.bottom + 4, left: r.left });
  }, []);

  useEffect(() => {
    if (!open) return;
    updateMenuPos();
    const onLayout = () => updateMenuPos();
    window.addEventListener('resize', onLayout);
    window.addEventListener('scroll', onLayout, true);
    return () => {
      window.removeEventListener('resize', onLayout);
      window.removeEventListener('scroll', onLayout, true);
    };
  }, [open, updateMenuPos]);

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  return (
    <div ref={wrapRef} className="sa-dc-plans-nav">
      <Link
        to="/plans"
        className={`sa-dc-nav-link ${active ? 'active' : ''}`}
        style={{
          ...navLinkStyle,
          borderTopRightRadius: 0,
          borderBottomRightRadius: 0,
          paddingRight: 10,
        }}
        onClick={() => setOpen(false)}
      >
        Plans
      </Link>
      <button
        type="button"
        className={`sa-dc-nav-link sa-dc-plans-caret ${active ? 'active' : ''}`}
        onClick={() => {
          setOpen((o) => !o);
          if (!open) updateMenuPos();
        }}
        aria-expanded={open}
        aria-haspopup="true"
        aria-label="Plans menu"
        style={{
          ...navLinkStyle,
          border: 'none',
          cursor: 'pointer',
          borderTopLeftRadius: 0,
          borderBottomLeftRadius: 0,
          paddingLeft: 6,
          paddingRight: 10,
        }}
      >
        <span aria-hidden="true">▾</span>
      </button>
      {open && menuPos && (
        <div
          className="jdc-plans-flyout jdc-plans-flyout--topnav"
          role="menu"
          style={{ top: menuPos.top, left: menuPos.left }}
        >
          {displayRecent.length > 0 && (
            <>
              <div className="jdc-plans-flyout-section">Recently viewed</div>
              {displayRecent.map((plan) => (
                <Link
                  key={plan.id}
                  to={`/plans/${plan.id}`}
                  role="menuitem"
                  onClick={() => {
                    recordRecentPlanView(plan.id, plan.name);
                    setOpen(false);
                  }}
                >
                  {plan.name}
                </Link>
              ))}
            </>
          )}
          <Link to="/plans" role="menuitem" onClick={() => setOpen(false)}>
            View all plans
          </Link>
          <Link to="/programs" role="menuitem" onClick={() => setOpen(false)}>
            Programs (Advanced Roadmaps)
          </Link>
          <Link to="/plans?tab=teams" role="menuitem" onClick={() => setOpen(false)}>
            Manage shared teams
          </Link>
          <button
            type="button"
            className="jdc-flyout-item"
            role="menuitem"
            onClick={() => {
              setOpen(false);
              window.dispatchEvent(new CustomEvent('openCreatePlanProgram'));
            }}
          >
            Create plan or program…
          </button>
        </div>
      )}
    </div>
  );
}
