import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { usePlan } from '../hooks/usePlans';
import { useBoards, useCreateBoard } from '../hooks/useBoardConfig';
import BacklogView from '../components/backlog/BacklogView';
import TeamsView from '../components/teams/TeamsView';
import ReleasesView from '../components/releases/ReleasesView';
import DependenciesView from '../components/dependencies/DependenciesView';
import WarningsPanel from '../components/warnings/WarningsPanel';
import PlanHeader from '../components/layout/PlanHeader';
import BoardDetailPage from './BoardDetailPage';
import '../styles/plans.css';

type TabType = 'backlog' | 'teams' | 'releases' | 'dependencies' | 'warnings' | 'boards';

export default function PlanDetailPage() {
  const { planId } = useParams<{ planId: string }>();
  const [activeTab, setActiveTab] = useState<TabType>('backlog');

  const { data: plan, isLoading } = usePlan(planId || '');
  const { data: boards } = useBoards(planId || '');
  const createBoard = useCreateBoard();

  const [showCreateBoard, setShowCreateBoard] = useState(false);
  const [newBoardName, setNewBoardName] = useState('');
  const [newBoardType, setNewBoardType] = useState<'SCRUM' | 'KANBAN'>('SCRUM');
  const [selectedBoardId, setSelectedBoardId] = useState<string | null>(null);

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
        <Link to="/programs" className="ab-btn ab-btn-primary">Back to Programs</Link>
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
        alert(error.message || 'Failed to create board');
      },
    });
  };

  const tabs: { id: TabType; label: string; count?: number }[] = [
    { id: 'backlog', label: 'Backlog', count: plan.itemCount },
    { id: 'teams', label: 'Teams', count: plan.teamCount },
    { id: 'releases', label: 'Releases', count: plan.releaseCount },
    { id: 'dependencies', label: 'Dependencies' },
    { id: 'warnings', label: 'Warnings' },
    { id: 'boards', label: 'Boards', count: boards?.length },
  ];

  // If board is selected, show board detail page
  if (selectedBoardId && activeTab === 'boards') {
    return (
      <div className="ab-plan-detail-page">
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
    <div className="ab-plan-detail-page">
      <PlanHeader plan={plan} />

      <div className="ab-tabs">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            className={`ab-tab ${activeTab === tab.id ? 'ab-tab-active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
            {tab.count !== undefined && tab.count > 0 && (
              <span className="ab-tab-count">{tab.count}</span>
            )}
          </button>
        ))}
      </div>

      <div className="ab-content">
        {activeTab === 'backlog' && <BacklogView planId={planId || ''} />}
        {activeTab === 'teams' && <TeamsView planId={planId || ''} />}
        {activeTab === 'releases' && <ReleasesView planId={planId || ''} />}
        {activeTab === 'dependencies' && <DependenciesView planId={planId || ''} />}
        {activeTab === 'warnings' && <WarningsPanel planId={planId || ''} />}
        {activeTab === 'boards' && (
          <div className="ab-boards-view">
            <div className="ab-boards-header">
              <h3>Boards</h3>
              <button
                className="ab-btn ab-btn-primary"
                onClick={() => setShowCreateBoard(true)}
              >
                Create Board
              </button>
            </div>

            {boards && boards.length > 0 ? (
              <div className="ab-boards-grid">
                {boards.map(board => (
                  <div
                    key={board.id}
                    className="ab-board-card"
                    onClick={() => setSelectedBoardId(board.id)}
                  >
                    <div className="ab-board-icon">
                      {board.boardType === 'SCRUM' ? '🏃' : '📋'}
                    </div>
                    <h4 className="ab-board-name">{board.name}</h4>
                    <span className={`ab-board-type ${board.boardType?.toLowerCase()}`}>
                      {board.boardType}
                    </span>
                    <div className="ab-board-meta">
                      <span>{board.columns?.length || 0} columns</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="ab-empty-state">
                <p>No boards yet. Create your first board to start managing sprints.</p>
              </div>
            )}

            {showCreateBoard && (
              <div className="ab-modal-overlay">
                <div className="ab-modal">
                  <div className="ab-modal-header">
                    <h2>Create Board</h2>
                    <button
                      className="ab-btn-close"
                      onClick={() => setShowCreateBoard(false)}
                    >
                      &times;
                    </button>
                  </div>
                  <div className="ab-modal-content">
                    <div className="ab-form-group">
                      <label>Board Name</label>
                      <input
                        type="text"
                        className="ab-input"
                        placeholder="e.g., Sprint Board"
                        value={newBoardName}
                        onChange={(e) => setNewBoardName(e.target.value)}
                      />
                    </div>
                    <div className="ab-form-group">
                      <label>Board Type</label>
                      <select
                        className="ab-select"
                        value={newBoardType}
                        onChange={(e) => setNewBoardType(e.target.value as 'SCRUM' | 'KANBAN')}
                      >
                        <option value="SCRUM">Scrum</option>
                        <option value="KANBAN">Kanban</option>
                      </select>
                    </div>
                    <div className="ab-modal-actions">
                      <button
                        className="ab-btn ab-btn-primary"
                        onClick={handleCreateBoard}
                      >
                        Create Board
                      </button>
                      <button
                        className="ab-btn ab-btn-secondary"
                        onClick={() => setShowCreateBoard(false)}
                      >
                        Cancel
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
