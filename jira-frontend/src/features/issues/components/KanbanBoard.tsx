import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { useNavigate } from 'react-router-dom';

interface KanbanBoardProps {
  projectId?: string;
}

interface Column {
  id: string;
  title: string;
  status: string;
  color: string;
}

const DEFAULT_COLUMNS: Column[] = [
  { id: 'todo', title: 'To Do', status: 'To Do', color: '#6c757d' },
  { id: 'inprogress', title: 'In Progress', status: 'In Progress', color: '#0066ff' },
  { id: 'review', title: 'In Review', status: 'In Review', color: '#ff9200' },
  { id: 'done', title: 'Done', status: 'Done', color: '#28a745' },
];

export default function KanbanBoard({ projectId }: KanbanBoardProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [columns] = useState<Column[]>(DEFAULT_COLUMNS);
  const [draggedIssue, setDraggedIssue] = useState<IssueResponse | null>(null);
  const [dragOverColumn, setDragOverColumn] = useState<string | null>(null);

  const { data: statuses = [] } = useQuery({
    queryKey: ['statuses-kanban'],
    queryFn: () => issueApi.getStatuses().then((r) => r.data),
  });

  const { data: issues = [], isLoading } = useQuery<IssueResponse[]>({
    queryKey: ['issues-kanban', projectId],
    queryFn: async () => {
      const params: Record<string, string> = {};
      if (projectId) params['projectId'] = projectId;
      const response = await issueApi.getAll(params);
      const data = response.data;
      if (data && 'content' in data) {
        return data.content || [];
      }
      return [];
    },
  });

  const transitionMutation = useMutation({
    mutationFn: ({
      issueId,
      projectId,
      statusId,
    }: {
      issueId: string;
      projectId: string;
      statusId: string;
    }) => issueApi.transitionStatus(issueId, projectId, { statusId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issues-kanban', projectId] });
    },
  });

  const getIssuesByColumn = (column: Column) => {
    return issues?.filter((issue) => {
      const status = issue.status?.toLowerCase() || '';
      return status.includes(column.status.toLowerCase().replace(' ', '')) ||
             status === column.status.toLowerCase();
    }) || [];
  };

  const handleDragStart = (e: React.DragEvent, issue: IssueResponse) => {
    setDraggedIssue(issue);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', issue.id);
  };

  const handleDragOver = (e: React.DragEvent, columnId: string) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setDragOverColumn(columnId);
  };

  const handleDragLeave = () => {
    setDragOverColumn(null);
  };

  const handleDrop = (e: React.DragEvent, column: Column) => {
    e.preventDefault();
    setDragOverColumn(null);

    if (draggedIssue && draggedIssue.projectId && draggedIssue.status !== column.status) {
      const target = statuses.find(
        (s: { id: string; name: string }) =>
          s.name.toLowerCase() === column.status.toLowerCase()
      );
      if (target) {
        transitionMutation.mutate({
          issueId: draggedIssue.id,
          projectId: draggedIssue.projectId,
          statusId: target.id,
        });
      }
    }
    setDraggedIssue(null);
  };

  const handleDragEnd = () => {
    setDraggedIssue(null);
    setDragOverColumn(null);
  };

  const getPriorityIcon = (priority: string | undefined) => {
    switch (priority?.toLowerCase()) {
      case 'critical':
      case 'highest':
        return { icon: '🔴', color: '#dc3545' };
      case 'high':
        return { icon: '🟠', color: '#fd7e14' };
      case 'medium':
        return { icon: '🟡', color: '#ffc107' };
      case 'low':
      case 'lowest':
        return { icon: '🟢', color: '#28a745' };
      default:
        return { icon: '⚪', color: '#6c757d' };
    }
  };

  const getTypeIcon = (type: string | undefined) => {
    switch (type?.toLowerCase()) {
      case 'bug':
        return '🐛';
      case 'story':
        return '📖';
      case 'task':
        return '✓';
      case 'epic':
        return '⚡';
      default:
        return '📋';
    }
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  return (
    <div className="ab-kanban-board">
      <div className="ab-kanban-header">
        <h2>Kanban Board</h2>
        <div className="ab-kanban-actions">
          <span className="ab-issue-count">
            {issues?.length || 0} issues
          </span>
        </div>
      </div>

      <div className="ab-kanban-columns">
        {columns.map((column) => {
          const columnIssues = getIssuesByColumn(column);
          const isOver = dragOverColumn === column.id;

          return (
            <div
              key={column.id}
              className={`ab-kanban-column ${isOver ? 'ab-drag-over' : ''}`}
              onDragOver={(e) => handleDragOver(e, column.id)}
              onDragLeave={handleDragLeave}
              onDrop={(e) => handleDrop(e, column)}
            >
              <div className="ab-column-header">
                <div
                  className="ab-column-indicator"
                  style={{ backgroundColor: column.color }}
                />
                <h3 className="ab-column-title">{column.title}</h3>
                <span className="ab-column-count">{columnIssues.length}</span>
              </div>

              <div className="ab-column-content">
                {columnIssues.map((issue) => (
                  <div
                    key={issue.id}
                    className={`ab-kanban-card ${draggedIssue?.id === issue.id ? 'ab-dragging' : ''}`}
                    draggable
                    onDragStart={(e) => handleDragStart(e, issue)}
                    onDragEnd={handleDragEnd}
                    onClick={() => navigate(`/issues/${issue.id}`)}
                  >
                    <div className="ab-card-header">
                      <span className="ab-card-type" title={issue.issueType}>
                        {getTypeIcon(issue.issueType)}
                      </span>
                      <span className="ab-card-key">{issue.issueKey}</span>
                      <span
                        className="ab-card-priority"
                        style={{ color: getPriorityIcon(issue.priority).color }}
                        title={issue.priority}
                      >
                        {getPriorityIcon(issue.priority).icon}
                      </span>
                    </div>
                    <div className="ab-card-title">{issue.title}</div>
                    {issue.assigneeId && (
                      <div className="ab-card-footer">
                        <div className="ab-card-assignee">
                          <span className="ab-avatar-xs">
                            {(issue.assigneeId as string).charAt(0).toUpperCase()}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>
                ))}

                {columnIssues.length === 0 && (
                  <div className="ab-column-empty">
                    <p>No issues</p>
                    <p className="ab-drop-hint">Drop issues here</p>
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <style>{`
        .ab-kanban-board {
          height: 100%;
          display: flex;
          flex-direction: column;
        }

        .ab-kanban-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: var(--ab-spacing-lg);
        }

        .ab-kanban-header h2 {
          font-size: var(--ab-font-size-xl);
          font-weight: 600;
          margin: 0;
        }

        .ab-kanban-actions {
          display: flex;
          gap: var(--ab-spacing-sm);
        }

        .ab-issue-count {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          background: var(--ab-gray-100);
          border-radius: var(--ab-radius-full);
        }

        .ab-kanban-columns {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: var(--ab-spacing-md);
          flex: 1;
          overflow-x: auto;
          min-height: 500px;
        }

        @media (max-width: 1200px) {
          .ab-kanban-columns {
            grid-template-columns: repeat(2, 1fr);
          }
        }

        @media (max-width: 768px) {
          .ab-kanban-columns {
            grid-template-columns: 1fr;
          }
        }

        .ab-kanban-column {
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-lg);
          display: flex;
          flex-direction: column;
          min-height: 400px;
          transition: background var(--ab-transition-fast);
        }

        .ab-kanban-column.ab-drag-over {
          background: var(--ab-primary-50);
          border: 2px dashed var(--ab-primary-300);
        }

        .ab-column-header {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-md);
          border-bottom: 1px solid var(--ab-gray-200);
        }

        .ab-column-indicator {
          width: 4px;
          height: 16px;
          border-radius: 2px;
        }

        .ab-column-title {
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
          color: var(--ab-gray-700);
          margin: 0;
          flex: 1;
        }

        .ab-column-count {
          font-size: var(--ab-font-size-xs);
          font-weight: 600;
          color: var(--ab-gray-500);
          background: var(--ab-gray-200);
          padding: 2px 8px;
          border-radius: var(--ab-radius-full);
        }

        .ab-column-content {
          flex: 1;
          padding: var(--ab-spacing-sm);
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
        }

        .ab-kanban-card {
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-sm);
          cursor: grab;
          transition: all var(--ab-transition-fast);
        }

        .ab-kanban-card:hover {
          box-shadow: var(--ab-shadow-md);
          border-color: var(--ab-gray-300);
        }

        .ab-kanban-card.ab-dragging {
          opacity: 0.5;
          cursor: grabbing;
        }

        .ab-card-header {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-card-type {
          font-size: var(--ab-font-size-sm);
        }

        .ab-card-key {
          font-family: var(--ab-font-mono);
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
          flex: 1;
        }

        .ab-card-priority {
          font-size: var(--ab-font-size-xs);
        }

        .ab-card-title {
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-800);
          line-height: 1.4;
          margin-bottom: var(--ab-spacing-xs);
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .ab-card-footer {
          display: flex;
          justify-content: flex-end;
          margin-top: var(--ab-spacing-xs);
        }

        .ab-card-assignee {
          display: flex;
          align-items: center;
        }

        .ab-avatar-xs {
          width: 20px;
          height: 20px;
          border-radius: 50%;
          background: var(--ab-primary-500);
          color: var(--ab-white);
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 10px;
          font-weight: 600;
        }

        .ab-column-empty {
          text-align: center;
          padding: var(--ab-spacing-xl);
          color: var(--ab-gray-400);
        }

        .ab-column-empty p {
          margin: 0;
        }

        .ab-drop-hint {
          font-size: var(--ab-font-size-xs);
          margin-top: var(--ab-spacing-xs) !important;
        }
      `}</style>
    </div>
  );
}
