import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useBoard, useCreateBoard } from '../hooks/useBoardConfig';
import { useSprints, useCreateSprint, useStartSprint, useCloseSprint } from '../hooks/useSprint';
import BoardConfigPanel from '../components/board/BoardConfigPanel';
import SprintBoard from '../components/sprint/SprintBoard';
import CreateSprintDialog from '../components/sprint/CreateSprintDialog';
import PermissionManager from '../components/permissions/PermissionManager';

export default function BoardDetailPage() {
  const { boardId } = useParams<{ boardId: string }>();
  const navigate = useNavigate();

  const { data: board, isLoading: boardLoading } = useBoard(boardId!);
  const { data: sprints, isLoading: sprintsLoading } = useSprints(boardId!);
  const createBoard = useCreateBoard();
  const createSprint = useCreateSprint();
  const startSprint = useStartSprint();
  const closeSprint = useCloseSprint();

  const [showBoardConfig, setShowBoardConfig] = useState(false);
  const [showPermissions, setShowPermissions] = useState(false);
  const [showCreateSprint, setShowCreateSprint] = useState(false);
  const [selectedSprintId, setSelectedSprintId] = useState<string | null>(null);
  const [newSprintName, setNewSprintName] = useState('');
  const [newSprintGoal, setNewSprintGoal] = useState('');

  if (boardLoading || sprintsLoading) {
    return <div className="ab-page-loading">Loading board...</div>;
  }

  if (!board) {
    return <div className="ab-page-error">Board not found</div>;
  }

  const handleCreateSprint = () => {
    if (!newSprintName.trim()) return;
    createSprint.mutate({
      boardId: boardId!,
      data: { name: newSprintName, goal: newSprintGoal },
    }, {
      onSuccess: () => {
        setNewSprintName('');
        setNewSprintGoal('');
        setShowCreateSprint(false);
      },
      onError: (error: Error) => {
        alert(error.message || 'Failed to create sprint');
      },
    });
  };

  const activeSprints = sprints?.filter(s => s.state === 'ACTIVE') || [];
  const futureSprints = sprints?.filter(s => s.state === 'FUTURE') || [];
  const closedSprints = sprints?.filter(s => s.state === 'CLOSED' || s.state === 'ABANDONED') || [];

  return (
    <div className="ab-board-detail-page">
      {/* Header */}
      <div className="ab-page-header">
        <div className="ab-header-left">
          <button className="ab-btn ab-btn-secondary" onClick={() => navigate(-1)}>
            Back
          </button>
          <h1 className="ab-page-title">{board.name}</h1>
          <span className={`ab-board-type-badge ${board.boardType?.toLowerCase()}`}>
            {board.boardType}
          </span>
        </div>
        <div className="ab-header-actions">
          <button className="ab-btn ab-btn-secondary" onClick={() => setShowBoardConfig(true)}>
            Configure Board
          </button>
          <button className="ab-btn ab-btn-secondary" onClick={() => setShowPermissions(true)}>
            Permissions
          </button>
          <button className="ab-btn ab-btn-primary" onClick={() => setShowCreateSprint(true)}>
            Create Sprint
          </button>
        </div>
      </div>

      {/* Sprint Tabs */}
      {selectedSprintId ? (
        <SprintBoard sprintId={selectedSprintId} boardId={boardId!} />
      ) : (
        <div className="ab-sprints-overview">
          {/* Active Sprints */}
          {activeSprints.length > 0 && (
            <div className="ab-sprint-group">
              <h2 className="ab-group-title">Active Sprints</h2>
              <div className="ab-sprint-cards">
                {activeSprints.map(sprint => (
                  <div
                    key={sprint.id}
                    className="ab-sprint-card ab-active"
                    onClick={() => setSelectedSprintId(sprint.id)}
                  >
                    <h3 className="ab-sprint-name">{sprint.name}</h3>
                    {sprint.goal && <p className="ab-sprint-goal">{sprint.goal}</p>}
                    <div className="ab-sprint-stats">
                      <span>{sprint.totalIssues} issues</span>
                      <span>{sprint.completedIssues} completed</span>
                    </div>
                    <div className="ab-sprint-actions">
                      <button
                        className="ab-btn ab-btn-sm ab-btn-success"
                        onClick={(e) => { e.stopPropagation(); closeSprint.mutate({ sprintId: sprint.id }); }}
                      >
                        Close Sprint
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Future Sprints */}
          {futureSprints.length > 0 && (
            <div className="ab-sprint-group">
              <h2 className="ab-group-title">Future Sprints</h2>
              <div className="ab-sprint-cards">
                {futureSprints.map(sprint => (
                  <div
                    key={sprint.id}
                    className="ab-sprint-card ab-future"
                    onClick={() => setSelectedSprintId(sprint.id)}
                  >
                    <h3 className="ab-sprint-name">{sprint.name}</h3>
                    {sprint.goal && <p className="ab-sprint-goal">{sprint.goal}</p>}
                    <div className="ab-sprint-actions">
                      <button
                        className="ab-btn ab-btn-sm ab-btn-primary"
                        onClick={(e) => { e.stopPropagation(); startSprint.mutate({ sprintId: sprint.id }); }}
                      >
                        Start Sprint
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Closed Sprints */}
          {closedSprints.length > 0 && (
            <div className="ab-sprint-group">
              <h2 className="ab-group-title">Closed Sprints</h2>
              <div className="ab-sprint-cards">
                {closedSprints.map(sprint => (
                  <div
                    key={sprint.id}
                    className="ab-sprint-card ab-closed"
                    onClick={() => setSelectedSprintId(sprint.id)}
                  >
                    <h3 className="ab-sprint-name">{sprint.name}</h3>
                    <div className="ab-sprint-stats">
                      <span>Velocity: {sprint.velocity || 0}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Empty State */}
          {sprints?.length === 0 && (
            <div className="ab-empty-state">
              <h3>No Sprints Yet</h3>
              <p>Create your first sprint to start tracking work.</p>
              <button className="ab-btn ab-btn-primary" onClick={() => setShowCreateSprint(true)}>
                Create Sprint
              </button>
            </div>
          )}
        </div>
      )}

      {/* Back to Sprints Button */}
      {selectedSprintId && (
        <button className="ab-btn ab-btn-secondary" onClick={() => setSelectedSprintId(null)}>
          Back to All Sprints
        </button>
      )}

      {/* Modals */}
      {showBoardConfig && (
        <div className="ab-modal-overlay">
          <BoardConfigPanel boardId={boardId!} onClose={() => setShowBoardConfig(false)} />
        </div>
      )}

      {showPermissions && (
        <div className="ab-modal-overlay">
          <PermissionManager boardId={boardId!} onClose={() => setShowPermissions(false)} />
        </div>
      )}

      {showCreateSprint && (
        <CreateSprintDialog
          boardId={boardId!}
          onClose={() => setShowCreateSprint(false)}
          onSuccess={() => {}}
        />
      )}
    </div>
  );
}