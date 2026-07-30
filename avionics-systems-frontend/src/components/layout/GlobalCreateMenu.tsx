import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import CreatePlanProgramSelector from '../../features/plans/components/CreatePlanProgramSelector';

type CreateMode = 'issue' | 'plan-program' | null;

interface GlobalCreateMenuProps {
  onCreateIssue: () => void;
}

export default function GlobalCreateMenu({ onCreateIssue }: GlobalCreateMenuProps) {
  const [mode, setMode] = useState<CreateMode>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const onPlanProgram = () => setMode('plan-program');
    window.addEventListener('openCreatePlanProgram', onPlanProgram);
    return () => window.removeEventListener('openCreatePlanProgram', onPlanProgram);
  }, []);

  return (
    <>
      <div style={{ position: 'relative', display: 'inline-flex' }}>
        <button
          type="button"
          className="sa-dc-create-btn"
          onClick={() => setMode((m) => (m === 'issue' || m === 'plan-program' ? null : 'issue'))}
          aria-haspopup="true"
          style={{
            marginLeft: 8,
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6,
            padding: '6px 14px',
            height: 30,
            cursor: 'pointer',
          }}
        >
          <span aria-hidden="true">+</span> Create
        </button>
        {mode === 'issue' && (
          <div
            className="jdc-plans-flyout"
            style={{ left: 0, top: 'calc(100% + 4px)', minWidth: 200 }}
            role="menu"
          >
            <button
              type="button"
              className="jdc-flyout-item"
              onClick={() => {
                setMode(null);
                onCreateIssue();
              }}
            >
              Issue
            </button>
            <button
              type="button"
              className="jdc-flyout-item"
              onClick={() => {
                setMode(null);
                window.dispatchEvent(new CustomEvent('openCreateProject'));
              }}
            >
              Project
            </button>
            <button
              type="button"
              className="jdc-flyout-item"
              onClick={() => {
                setMode(null);
                setMode('plan-program');
              }}
            >
              Plan or Program…
            </button>
          </div>
        )}
      </div>
      {mode === 'plan-program' && (
        <CreatePlanProgramSelector
          onClose={() => setMode(null)}
          onSelect={(type) => {
            setMode(null);
            navigate(type === 'plan' ? '/plans/create' : '/programs/create');
          }}
        />
      )}
    </>
  );
}
