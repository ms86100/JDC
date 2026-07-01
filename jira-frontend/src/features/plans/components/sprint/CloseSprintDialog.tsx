import React, { useState } from 'react';
import {
  useCloseSprintWithMove,
  useSprints,
  SprintResponse,
  SprintIssueResponse,
} from '../../hooks/useSprint';

interface CloseSprintDialogProps {
  sprint: SprintResponse;
  boardId: string;
  incompleteIssues: SprintIssueResponse[];
  onClose: () => void;
}

type MoveOption = 'next_sprint' | 'backlog' | 'leave';

export default function CloseSprintDialog({
  sprint,
  boardId,
  incompleteIssues,
  onClose,
}: CloseSprintDialogProps) {
  const { data: allSprints } = useSprints(boardId);
  const closeSprintWithMove = useCloseSprintWithMove();

  const [moveOption, setMoveOption] = useState<MoveOption>('next_sprint');
  const [targetSprintId, setTargetSprintId] = useState<string>('');

  const futureSprints = (allSprints ?? []).filter(
    (s) => s.state === 'FUTURE' && s.id !== sprint.id
  );

  const handleSubmit = () => {
    let moveIncompleteToSprintId: string | undefined;

    if (moveOption === 'next_sprint' && targetSprintId) {
      moveIncompleteToSprintId = targetSprintId;
    }
    // 'backlog' and 'leave' do not pass a target sprint id

    closeSprintWithMove.mutate(
      {
        sprintId: sprint.id,
        userId: 'current_user',
        moveIncompleteToSprintId,
      },
      {
        onSuccess: () => {
          onClose();
        },
      }
    );
  };

  return (
    <div className="ab-modal-overlay">
      <div
        className="ab-modal"
        style={{
          maxWidth: '560px',
          width: '100%',
          backgroundColor: '#fff',
          borderRadius: 'var(--ab-radius-lg, 8px)',
          padding: 'var(--ab-spacing-lg, 24px)',
        }}
      >
        {/* Header */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 'var(--ab-spacing-md, 16px)',
          }}
        >
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 600 }}>
            Complete Sprint: {sprint.name}
          </h2>
          <button
            className="ab-btn-close"
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '20px',
              cursor: 'pointer',
              color: 'var(--ab-gray-500, #6b7280)',
            }}
          >
            &times;
          </button>
        </div>

        {/* Incomplete issues summary */}
        <div
          style={{
            marginBottom: 'var(--ab-spacing-md, 16px)',
            padding: 'var(--ab-spacing-sm, 12px)',
            backgroundColor: 'var(--ab-gray-50, #f9fafb)',
            borderRadius: 'var(--ab-radius-md, 6px)',
            border: '1px solid var(--ab-gray-200, #e5e7eb)',
          }}
        >
          <p style={{ margin: 0, fontWeight: 500, marginBottom: 'var(--ab-spacing-xs, 8px)' }}>
            {incompleteIssues.length} incomplete{' '}
            {incompleteIssues.length === 1 ? 'issue' : 'issues'}
          </p>

          <ul
            style={{
              margin: 0,
              paddingLeft: 'var(--ab-spacing-md, 16px)',
              maxHeight: '160px',
              overflowY: 'auto',
              listStyle: 'none',
              padding: 0,
            }}
          >
            {incompleteIssues.map((issue) => (
              <li
                key={issue.id}
                style={{
                  padding: 'var(--ab-spacing-xs, 4px) 0',
                  fontSize: '13px',
                  color: 'var(--ab-gray-700, #374151)',
                  borderBottom: '1px solid var(--ab-gray-100, #f3f4f6)',
                }}
              >
                {issue.issueId}
              </li>
            ))}
          </ul>
        </div>

        {/* Move option selection */}
        <div style={{ marginBottom: 'var(--ab-spacing-md, 16px)' }}>
          <label
            style={{
              display: 'block',
              fontWeight: 500,
              marginBottom: 'var(--ab-spacing-xs, 8px)',
              fontSize: '14px',
            }}
          >
            What would you like to do with incomplete issues?
          </label>

          <select
            value={moveOption}
            onChange={(e) => setMoveOption(e.target.value as MoveOption)}
            style={{
              width: '100%',
              padding: 'var(--ab-spacing-sm, 8px) var(--ab-spacing-sm, 12px)',
              borderRadius: 'var(--ab-radius-md, 6px)',
              border: '1px solid var(--ab-gray-300, #d1d5db)',
              fontSize: '14px',
              backgroundColor: '#fff',
              cursor: 'pointer',
            }}
          >
            <option value="next_sprint">Move to next sprint</option>
            <option value="backlog">Move to backlog</option>
            <option value="leave">Leave in closed sprint</option>
          </select>
        </div>

        {/* Target sprint dropdown (only when moving to another sprint) */}
        {moveOption === 'next_sprint' && (
          <div style={{ marginBottom: 'var(--ab-spacing-md, 16px)' }}>
            <label
              style={{
                display: 'block',
                fontWeight: 500,
                marginBottom: 'var(--ab-spacing-xs, 8px)',
                fontSize: '14px',
              }}
            >
              Target Sprint
            </label>

            {futureSprints.length === 0 ? (
              <p
                style={{
                  margin: 0,
                  fontSize: '13px',
                  color: 'var(--ab-gray-500, #6b7280)',
                  fontStyle: 'italic',
                }}
              >
                No future sprints available. Create a new sprint first.
              </p>
            ) : (
              <select
                value={targetSprintId}
                onChange={(e) => setTargetSprintId(e.target.value)}
                style={{
                  width: '100%',
                  padding: 'var(--ab-spacing-sm, 8px) var(--ab-spacing-sm, 12px)',
                  borderRadius: 'var(--ab-radius-md, 6px)',
                  border: '1px solid var(--ab-gray-300, #d1d5db)',
                  fontSize: '14px',
                  backgroundColor: '#fff',
                  cursor: 'pointer',
                }}
              >
                <option value="">Select a sprint</option>
                {futureSprints.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            )}
          </div>
        )}

        {/* Actions */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            gap: 'var(--ab-spacing-sm, 8px)',
            paddingTop: 'var(--ab-spacing-md, 16px)',
            borderTop: '1px solid var(--ab-gray-200, #e5e7eb)',
          }}
        >
          <button
            className="ab-btn ab-btn-secondary"
            onClick={onClose}
            style={{
              padding: 'var(--ab-spacing-sm, 8px) var(--ab-spacing-md, 16px)',
              borderRadius: 'var(--ab-radius-md, 6px)',
              border: '1px solid var(--ab-gray-300, #d1d5db)',
              backgroundColor: '#fff',
              cursor: 'pointer',
              fontSize: '14px',
            }}
          >
            Cancel
          </button>
          <button
            className="ab-btn ab-btn-primary"
            onClick={handleSubmit}
            disabled={
              closeSprintWithMove.isPending ||
              (moveOption === 'next_sprint' && !targetSprintId && futureSprints.length > 0)
            }
            style={{
              padding: 'var(--ab-spacing-sm, 8px) var(--ab-spacing-md, 16px)',
              borderRadius: 'var(--ab-radius-md, 6px)',
              border: 'none',
              backgroundColor: 'var(--ab-primary-600, #2563eb)',
              color: '#fff',
              cursor: 'pointer',
              fontSize: '14px',
              opacity:
                closeSprintWithMove.isPending ||
                (moveOption === 'next_sprint' && !targetSprintId && futureSprints.length > 0)
                  ? 0.6
                  : 1,
            }}
          >
            {closeSprintWithMove.isPending ? 'Completing...' : 'Complete Sprint'}
          </button>
        </div>
      </div>
    </div>
  );
}
