import { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { usePlan } from '../hooks/usePlans';
import { useBoards, useCreateBoard } from '../hooks/useBoardConfig';
import RoadmapView from '../components/roadmap/RoadmapView';
import TeamsView from '../components/teams/TeamsView';
import ReleasesView from '../components/releases/ReleasesView';
import DependenciesView from '../components/dependencies/DependenciesView';
import WarningsPanel from '../components/warnings/WarningsPanel';
import BoardDetailPage from './BoardDetailPage';
import { recordRecentPlanView } from '../utils/recentPlanViews';
import '../styles/plans.css';
import '../styles/plan-roadmap.css';
import { appNotify } from '../../../lib/appNotify';

type TabType = 'roadmap' | 'teams' | 'releases' | 'dependencies' | 'warnings' | 'boards';

export default function PlanDetailPage() {
  const { planId } = useParams<{ planId: string }>();
  const [activeTab, setActiveTab] = useState<TabType>('roadmap');
  const [moreOpen, setMoreOpen] = useState(false);

  const { data: plan, isLoading } = usePlan(planId || '');
  const { data: boards } = useBoards(planId || '');
  const createBoard = useCreateBoard();

  const [showCreateBoard, setShowCreateBoard] = useState(false);
  const [newBoardName, setNewBoardName] = useState('');
  const [newBoardType, setNewBoardType] = useState<'SCRUM' | 'KANBAN'>('SCRUM');
  const [selectedBoardId, setSelectedBoardId] = useState<string | null>(null);

  useEffect(() => {
    if (plan && planId) recordRecentPlanView(planId, plan.name);
  }, [plan, planId]);

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  if (!plan) {
    return (
      <div className="ab-empty-state">
        <h3>Plan not found</h3>
        <Link to="/plans" className="ab-btn ab-btn-primary">View plans</Link>
      </div>
    );
  }

  const handleCreateBoard = () => {
    if (!newBoardName.trim() || !planId) return;
    createBoard.mutate({
      planId,
      data: { name: newBoardName, boardType: newBoardType },
    }, {
      onSuccess: () => {
        setNewBoardName('');
        setShowCreateBoard(false);
      },
      onError: (error: Error) => {
        appNotify.error(error.message || 'Failed to create board');
      },
    });
  };

  const tabs: { id: TabType; label: string; count?: number }[] = [
    { id: 'roadmap', label: 'Roadmap', count: plan.itemCount },
    { id: 'teams', label: 'Teams', count: plan.teamCount },
    { id: 'releases', label: 'Releases', count: plan.releaseCount },
    { id: 'dependencies', label: 'Dependencies report' },
  ];

  if (selectedBoardId && activeTab === 'boards') {
    return (
      <div className="jdc-plan-page sa-plan-detail">
        <div className="ab-page-header">
          <button className="ab-btn ab-btn-secondary" onClick={() => setSelectedBoardId(null)}>
            Back to Boards
          </button>
        </div>
        <BoardDetailPage />
      </div>
    );
  }

  return (
    <div className="jdc-plan-page sa-plan-detail">
      <header className="sa-plan-detail-header">
        <div className="jdc-plan-top">
          <div className="jdc-plan-title-row">
            <h1 className="jdc-plan-title">{plan.name}</h1>
            <Link to={`/plans/${planId}/settings`} title="Plan settings">⚙</Link>
          </div>
          <div className="jdc-plan-tabs">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                className={`jdc-plan-tab ${activeTab === tab.id ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.id)}
              >
                {tab.label}
                {tab.count !== undefined && tab.count > 0 && ` (${tab.count})`}
              </button>
            ))}
            <div style={{ marginLeft: 'auto', position: 'relative' }}>
              <button type="button" className="jdc-plan-tab" onClick={() => setMoreOpen((o) => !o)}>More ▾</button>
              {moreOpen && (
                <div className="jdc-plans-flyout" style={{ right: 0, left: 'auto', top: '100%' }}>
                  <button type="button" className="jdc-flyout-item" onClick={() => { setActiveTab('warnings'); setMoreOpen(false); }}>Warnings</button>
                  <button type="button" className="jdc-flyout-item" onClick={() => { setActiveTab('boards'); setMoreOpen(false); }}>Boards ({boards?.length ?? 0})</button>
                </div>
              )}
            </div>
          </div>
        </div>
      </header>

      <div className="sa-plan-detail-body">
        {activeTab === 'roadmap' && <RoadmapView plan={plan} />}
        {activeTab === 'teams' && <div style={{ padding: 16 }}><TeamsView planId={planId || ''} /></div>}
        {activeTab === 'releases' && <div style={{ padding: 16 }}><ReleasesView planId={planId || ''} /></div>}
        {activeTab === 'dependencies' && <div style={{ padding: 16 }}><DependenciesView planId={planId || ''} /></div>}
        {activeTab === 'warnings' && <div style={{ padding: 16 }}><WarningsPanel planId={planId || ''} /></div>}
        {activeTab === 'boards' && (
          <div className="ab-boards-view" style={{ padding: 16 }}>
            <div className="ab-boards-header">
              <h3>Boards</h3>
              <button className="ab-btn ab-btn-primary" onClick={() => setShowCreateBoard(true)}>Create Board</button>
            </div>
            {boards && boards.length > 0 ? (
              <div className="ab-boards-grid">
                {boards.map((board) => (
                  <div key={board.id} className="ab-board-card" onClick={() => setSelectedBoardId(board.id)}>
                    <h4>{board.name}</h4>
                    <span>{board.boardType}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p>No boards yet.</p>
            )}
            {showCreateBoard && (
              <div className="ab-modal-overlay">
                <div className="ab-modal">
                  <h2>Create Board</h2>
                  <input className="ab-input" value={newBoardName} onChange={(e) => setNewBoardName(e.target.value)} />
                  <select className="ab-select" value={newBoardType} onChange={(e) => setNewBoardType(e.target.value as 'SCRUM' | 'KANBAN')}>
                    <option value="SCRUM">Scrum</option>
                    <option value="KANBAN">Kanban</option>
                  </select>
                  <button className="ab-btn ab-btn-primary" onClick={handleCreateBoard}>Create</button>
                  <button className="ab-btn ab-btn-secondary" onClick={() => setShowCreateBoard(false)}>Cancel</button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
