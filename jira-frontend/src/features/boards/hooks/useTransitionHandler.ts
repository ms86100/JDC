import React, { useState, useEffect } from 'react';
import type { BoardIssue } from '../../../api/boardApi';
import type { Transition } from '../../../api/transitionApi';

interface TransitionHandlerOptions {
  issue: BoardIssue;
  transitions: Transition[];
  onTransition: (transitionId: string, comment?: string) => void;
  onClose: () => void;
}

export function useTransitionHandler({
  issue,
  transitions,
  onTransition,
  onClose,
}: TransitionHandlerOptions) {
  const [showTransitionModal, setShowTransitionModal] = useState(false);
  const [selectedTransition, setSelectedTransition] = useState<Transition | null>(null);
  const [transitionComment, setTransitionComment] = useState('');

  useEffect(() => {
    if (selectedTransition) {
      setShowTransitionModal(true);
    }
  }, [selectedTransition]);

  const getAvailableTransitions = () => {
    const statusTransitions: Record<string, string[]> = {
      'To Do': ['In Progress', 'Done'],
      'In Progress': ['To Do', 'In Review', 'Done', 'Blocked'],
      'In Review': ['In Progress', 'Done'],
      'Blocked': ['In Progress'],
      'Done': ['To Do', 'In Progress'],
    };

    const currentStatus = issue.status || issue.statusCategory || 'To Do';
    const allowedStatuses = statusTransitions[currentStatus] || [];

    return transitions.filter((t) => {
      const toStatus = t.to?.name || t.to?.statusCategory || '';
      return allowedStatuses.some(
        (s) => toStatus.toLowerCase().includes(s.toLowerCase()) || toStatus === s,
      );
    });
  };

  const handleTransitionClick = (transition: Transition) => {
    setSelectedTransition(transition);
  };

  const handleTransitionConfirm = () => {
    if (selectedTransition) {
      onTransition(selectedTransition.id, transitionComment || undefined);
      setShowTransitionModal(false);
      setSelectedTransition(null);
      setTransitionComment('');
      onClose();
    }
  };

  const handleTransitionCancel = () => {
    setShowTransitionModal(false);
    setSelectedTransition(null);
    setTransitionComment('');
  };

  return {
    showTransitionModal,
    selectedTransition,
    transitionComment,
    setTransitionComment,
    getAvailableTransitions,
    handleTransitionClick,
    handleTransitionConfirm,
    handleTransitionCancel,
    setShowTransitionModal,
  };
}