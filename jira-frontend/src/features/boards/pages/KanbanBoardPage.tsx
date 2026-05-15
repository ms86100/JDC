import React, { useState } from 'react';
import JiraGlobalLayout from '../../../components/JiraGlobalLayout';
import CreateProjectModal from '../../../components/CreateProjectModal';

interface Card {
  id: string;
  key: string;
  title: string;
  priority: string;
  type: string;
  assignee?: string;
}

interface Column {
  id: string;
  name: string;
  color: string;
  cards: Card[];
}

const INITIAL_COLUMNS: Column[] = [
  {
    id: 'backlog',
    name: 'BACKLOG',
    color: '#6b778c',
    cards: [
      { id: '1', key: 'MK-1', title: 'mine', priority: 'high', type: 'bug', assignee: 'SS' },
    ],
  },
  {
    id: 'selected',
    name: 'SELECTED FOR DEVELOPMENT',
    color: '#0052cc',
    cards: [],
  },
  {
    id: 'inprogress',
    name: 'IN PROGRESS',
    color: '#ff8b00',
    cards: [],
  },
  {
    id: 'done',
    name: 'DONE',
    color: '#36b37e',
    cards: [],
  },
];

export default function KanbanBoardPage() {
  const [columns] = useState<Column[]>(INITIAL_COLUMNS);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [qfFilters, setQfFilters] = useState<Record<string, boolean>>({});
  const [draggedCard, setDraggedCard] = useState<{ colId: string; card: Card } | null>(null);
  const [dragOverCol, setDragOverCol] = useState<string | null>(null);

  const toggleQf = (key: string) => {
    setQfFilters((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleDragStart = (colId: string, card: Card) => {
    setDraggedCard({ colId, card });
  };

  const handleDragEnd = () => {
    setDraggedCard(null);
    setDragOverCol(null);
  };

  const handleDragOver = (e: React.DragEvent, colId: string) => {
    e.preventDefault();
    setDragOverCol(colId);
  };

  const handleDragLeave = () => {
    setDragOverCol(null);
  };

  const handleDrop = (e: React.DragEvent, targetColId: string) => {
    e.preventDefault();
    if (draggedCard && draggedCard.colId !== targetColId) {
      // In real app: call API to move card
      console.log(`Move ${draggedCard.card.key} from ${draggedCard.colId} to ${targetColId}`);
    }
    setDraggedCard(null);
    setDragOverCol(null);
  };

  const getPriorityDot = (priority: string) => {
    switch (priority.toLowerCase()) {
      case 'highest': case 'high': return '#ff5630';
      case 'medium': return '#ffab00';
      case 'low': return '#36b37e';
      default: return '#dfe1e6';
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type.toLowerCase()) {
      case 'bug': return '🐛';
      case 'story': return '📖';
      case 'task': return '✓';
      case 'epic': return '⚡';
      default: return '📋';
    }
  };

  return (
    <JiraGlobalLayout
      projectName="My Kanban"
      projectKey="MK"
      boardName="MK board"
      activeSection="board"
    >
      <div className="ab-board-page">
        {/* Board Toolbar */}
        <div className="ab-board-toolbar">
          <div className="ab-board-title-area">
            <h1 className="ab-board-page-title">Kanban board</h1>
            <button className="ab-board-action-btn">
              MK board ▾
            </button>
          </div>
          <div className="ab-board-actions">
            <button className="ab-board-action-btn" title="Expand">
              ⛶
            </button>
          </div>
        </div>

        {/* Quick Filters */}
        <div className="ab-quick-filters-bar">
          <span className="ab-qf-label">Quick Filters:</span>
          <button
            className={`ab-qf-link ${qfFilters.mine ? 'active' : ''}`}
            onClick={() => toggleQf('mine')}
          >
            Only My Issues
          </button>
          <button
            className={`ab-qf-link ${qfFilters.recent ? 'active' : ''}`}
            onClick={() => toggleQf('recent')}
          >
            Recently Updated
          </button>
        </div>

        {/* Kanban Columns */}
        <div className="ab-kanban-board">
          {columns.map((col) => (
            <div
              key={col.id}
              className={`ab-kanban-col ${dragOverCol === col.id ? 'drag-over' : ''}`}
              onDragOver={(e) => handleDragOver(e, col.id)}
              onDragLeave={handleDragLeave}
              onDrop={(e) => handleDrop(e, col.id)}
            >
              {/* Column Header */}
              <div className="ab-kanban-col-header">
                <div className="ab-col-indicator" style={{ backgroundColor: col.color }} />
                <span className="ab-col-title">{col.name}</span>
                <span className="ab-col-count">{col.cards.length}</span>
                <button className="ab-col-add-btn" title="Add issue">+</button>
              </div>

              {/* Cards */}
              <div className="ab-col-cards">
                {col.cards.length === 0 ? (
                  col.id === 'done' ? (
                    <div className="ab-col-empty">
                      <p className="ab-col-empty-title">We're only showing recently modified issues.</p>
                      <p className="ab-col-empty-title">Looking for an older issue?</p>
                      <button className="ab-col-empty-cta">View all issues</button>
                    </div>
                  ) : (
                    <div className="ab-col-empty">
                      <button className="ab-col-empty-cta" onClick={() => setShowCreateModal(true)}>
                        + Create issue
                      </button>
                    </div>
                  )
                ) : (
                  col.cards.map((card) => (
                    <div
                      key={card.id}
                      className={`ab-jira-card ${draggedCard?.card.id === card.id ? 'dragging' : ''}`}
                      draggable
                      onDragStart={() => handleDragStart(col.id, card)}
                      onDragEnd={handleDragEnd}
                      style={{ borderLeftColor: getPriorityDot(card.priority) }}
                    >
                      <div className="ab-jira-card-header">
                        <span className="ab-card-type-icon">{getTypeIcon(card.type)}</span>
                        <span className="ab-card-key">{card.key}</span>
                        <div
                          className="ab-card-priority-dot"
                          style={{ background: getPriorityDot(card.priority) }}
                        />
                      </div>
                      <p className="ab-jira-card-title">{card.title}</p>
                      <div className="ab-jira-card-footer">
                        <div className="ab-card-assignee">
                          {card.assignee && (
                            <div className="ab-avatar-sm">{card.assignee}</div>
                          )}
                        </div>
                        <div className="ab-card-meta">
                          <span className="ab-card-meta-item">📎</span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Create Project Modal */}
      <CreateProjectModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSelect={(templateId) => {
          console.log('Selected template:', templateId);
        }}
      />
    </JiraGlobalLayout>
  );
}