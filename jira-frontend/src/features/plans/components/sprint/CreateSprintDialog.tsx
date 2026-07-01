import React, { useState, useEffect } from 'react';
import { useCreateSprint, useWorkingDays } from '../../hooks/useSprint';
import { useBoard } from '../../hooks/useBoardConfig';
import { appNotify } from '../../../../lib/appNotify';

interface CreateSprintDialogProps {
  boardId: string;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function CreateSprintDialog({ boardId, onClose, onSuccess }: CreateSprintDialogProps) {
  const { data: board } = useBoard(boardId);
  const { data: workingDays } = useWorkingDays();
  const createSprint = useCreateSprint();

  const [formData, setFormData] = useState({
    name: '',
    goal: '',
    startDate: '',
    endDate: '',
  });

  const [workingDaysCount, setWorkingDaysCount] = useState<number | null>(null);

  // Calculate working days when dates change
  useEffect(() => {
    if (formData.startDate && formData.endDate) {
      const start = new Date(formData.startDate);
      const end = new Date(formData.endDate);

      if (end >= start) {
        const count = calculateWorkingDays(start, end, workingDays);
        setWorkingDaysCount(count);
      } else {
        setWorkingDaysCount(null);
      }
    } else {
      setWorkingDaysCount(null);
    }
  }, [formData.startDate, formData.endDate, workingDays]);

  const calculateWorkingDays = (start: Date, end: Date, config: any): number => {
    if (!config) {
      // Default: exclude weekends
      let count = 0;
      const current = new Date(start);
      while (current <= end) {
        const dayOfWeek = current.getDay();
        if (dayOfWeek !== 0 && dayOfWeek !== 6) {
          count++;
        }
        current.setDate(current.getDate() + 1);
      }
      return count;
    }

    // Use working days config
    const workingDaysMap: Record<string, boolean> = {
      monday: config.monday ?? true,
      tuesday: config.tuesday ?? true,
      wednesday: config.wednesday ?? true,
      thursday: config.thursday ?? true,
      friday: config.friday ?? true,
      saturday: config.saturday ?? false,
      sunday: config.sunday ?? false,
    };

    let count = 0;
    const current = new Date(start);
    while (current <= end) {
      const dayNames = ['sunday', 'monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'];
      const dayName = dayNames[current.getDay()];
      if (workingDaysMap[dayName]) {
        count++;
      }
      current.setDate(current.getDate() + 1);
    }
    return count;
  };

  const handleSubmit = () => {
    if (!formData.name.trim()) {
      appNotify.warning('Please enter a sprint name');
      return;
    }

    createSprint.mutate({
      boardId,
      data: {
        name: formData.name,
        goal: formData.goal || undefined,
        startDate: formData.startDate || undefined,
        endDate: formData.endDate || undefined,
      },
    }, {
      onSuccess: () => {
        onSuccess?.();
        onClose();
      },
    });
  };

  const getToday = () => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  };

  const getDefaultEndDate = () => {
    const start = formData.startDate ? new Date(formData.startDate) : new Date();
    start.setDate(start.getDate() + 13); // 2 weeks sprint
    return start.toISOString().split('T')[0];
  };

  return (
    <div className="ab-modal-overlay">
      <div className="ab-modal ab-create-sprint-dialog">
        <div className="ab-modal-header">
          <h2>Create Sprint</h2>
          <button className="ab-btn-close" onClick={onClose}>&times;</button>
        </div>

        <div className="ab-modal-content">
          <div className="ab-form-group">
            <label>Sprint Name <span className="ab-required">*</span></label>
            <input
              type="text"
              className="ab-input"
              placeholder="e.g., Sprint 1"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              autoFocus
            />
          </div>

          <div className="ab-form-group">
            <label>Sprint Goal</label>
            <textarea
              className="ab-textarea"
              placeholder="What do you want to achieve in this sprint?"
              value={formData.goal}
              onChange={(e) => setFormData({ ...formData, goal: e.target.value })}
              rows={3}
            />
          </div>

          <div className="ab-form-row">
            <div className="ab-form-group">
              <label>Start Date</label>
              <input
                type="date"
                className="ab-input"
                value={formData.startDate}
                onChange={(e) => setFormData({
                  ...formData,
                  startDate: e.target.value,
                  endDate: formData.endDate || getDefaultEndDate(),
                })}
                min={getToday()}
              />
            </div>

            <div className="ab-form-group">
              <label>End Date</label>
              <input
                type="date"
                className="ab-input"
                value={formData.endDate || getDefaultEndDate()}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                min={formData.startDate || getToday()}
              />
            </div>
          </div>

          {/* Working Days Calculation - Key feature from video */}
          {workingDaysCount !== null && (
            <div className="ab-working-days-info">
              <span className="ab-info-icon">ℹ️</span>
              <span>
                This sprint contains <strong>{workingDaysCount} working days</strong>.
                {workingDaysCount === 10 && " "}
              </span>
              {workingDaysCount !== 10 && (
                <span className="ab-warning">
                  Standard sprints are typically 10 working days (2 weeks).
                </span>
              )}
            </div>
          )}

          {formData.startDate && formData.endDate && new Date(formData.endDate) < new Date(formData.startDate) && (
            <div className="ab-error-message">
              End date must be after start date
            </div>
          )}
        </div>

        <div className="ab-modal-actions">
          <button className="ab-btn ab-btn-primary" onClick={handleSubmit}>
            Create Sprint
          </button>
          <button className="ab-btn ab-btn-secondary" onClick={onClose}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
