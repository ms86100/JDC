import React from 'react';
import TransitionScreenForm, {
  type AvailableTransition,
} from '../../issues/components/TransitionScreenForm';

interface Props {
  transition: AvailableTransition;
  comment: string;
  screenInput: Record<string, unknown>;
  isSubmitting: boolean;
  onCommentChange: (v: string) => void;
  onScreenInputChange: (v: Record<string, unknown>) => void;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function BoardTransitionModal({
  transition,
  comment,
  screenInput,
  isSubmitting,
  onCommentChange,
  onScreenInputChange,
  onConfirm,
  onCancel,
}: Props) {
  return (
    <div className="ab-modal-overlay" role="dialog" aria-modal="true" aria-labelledby="board-transition-title">
      <div className="ab-modal ab-board-transition-modal">
        <div className="ab-modal-header">
          <h2 id="board-transition-title">Transition issue</h2>
          <button type="button" className="ab-modal-close" onClick={onCancel} aria-label="Close">
            ×
          </button>
        </div>
        <div className="ab-modal-body">
          <p className="ab-board-transition-hint">
            Move to <strong>{transition.name}</strong>
          </p>
          <TransitionScreenForm
            transition={transition}
            comment={comment}
            onCommentChange={onCommentChange}
            screenInput={screenInput}
            onScreenInputChange={onScreenInputChange}
            onConfirm={onConfirm}
            onCancel={onCancel}
            isSubmitting={isSubmitting}
          />
        </div>
      </div>
    </div>
  );
}
