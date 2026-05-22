import { useEffect, useState } from 'react';
import CreatePlanProgramSelector from '../../plans/components/CreatePlanProgramSelector';
import { useNavigate } from 'react-router-dom';
import IntroductionGadget from '../components/IntroductionGadget';
import AssignedToMeGadget from '../components/AssignedToMeGadget';
import ActivityStreamGadget from '../components/ActivityStreamGadget';
import '../DashboardStyles.css';

export default function DashboardPage() {
  const navigate = useNavigate();
  const [showCreateSelector, setShowCreateSelector] = useState(false);

  useEffect(() => {
    const onCreate = () => setShowCreateSelector(true);
    window.addEventListener('openCreatePlanProgram', onCreate);
    return () => window.removeEventListener('openCreatePlanProgram', onCreate);
  }, []);

  return (
    <div className="jdc-dashboard-page">
      <h1 className="jdc-dashboard-title">System Dashboard</h1>
      <div className="jdc-gadget-grid">
        <IntroductionGadget />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <AssignedToMeGadget />
          <ActivityStreamGadget />
        </div>
      </div>
      {showCreateSelector && (
        <CreatePlanProgramSelector
          onClose={() => setShowCreateSelector(false)}
          onSelect={(type) => {
            setShowCreateSelector(false);
            navigate(type === 'plan' ? '/plans/create' : '/programs/create');
          }}
        />
      )}
    </div>
  );
}
