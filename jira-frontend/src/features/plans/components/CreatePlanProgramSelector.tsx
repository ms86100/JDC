import { useState } from 'react';
import '../styles/plans.css';

interface CreatePlanProgramSelectorProps {
  onSelect: (type: 'plan' | 'program') => void;
  onClose: () => void;
}

export default function CreatePlanProgramSelector({ onSelect, onClose }: CreatePlanProgramSelectorProps) {
  const [selectedType, setSelectedType] = useState<'plan' | 'program'>('plan');

  const handleCreate = () => {
    onSelect(selectedType);
  };

  return (
    <div className="selector-overlay" onClick={onClose}>
      <div className="selector-modal" onClick={(e) => e.stopPropagation()}>
        <div className="selector-header">
          <h2 className="selector-title">What would you like to create?</h2>
          <button className="selector-close" onClick={onClose}>
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M5 5L15 15M15 5L5 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
            </svg>
          </button>
        </div>

        <div className="selector-body">
          <p className="selector-description">
            Plans visualize your roadmaps by automatically or manually scheduling work for your teams,
            as you see fit. Programs track the progress of business initiatives in an aggregated view
            of work across multiple plans.
          </p>

          <div className="type-options">
            <label className={`type-option ${selectedType === 'plan' ? 'selected' : ''}`}>
              <input
                type="radio"
                name="create-type"
                value="plan"
                checked={selectedType === 'plan'}
                onChange={() => setSelectedType('plan')}
              />
              <div className="type-option-content">
                <div className="type-radio">
                  {selectedType === 'plan' && <div className="type-radio-dot"></div>}
                </div>
                <div className="type-option-details">
                  <span className="type-option-label">Plan</span>
                  <span className="type-option-hint">Create a timeline that syncs with your work</span>
                </div>
              </div>
            </label>

            <label className={`type-option ${selectedType === 'program' ? 'selected' : ''}`}>
              <input
                type="radio"
                name="create-type"
                value="program"
                checked={selectedType === 'program'}
                onChange={() => setSelectedType('program')}
              />
              <div className="type-option-content">
                <div className="type-radio">
                  {selectedType === 'program' && <div className="type-radio-dot"></div>}
                </div>
                <div className="type-option-details">
                  <span className="type-option-label">Program</span>
                  <span className="type-option-hint">Aggregate multiple plans into one view</span>
                </div>
              </div>
            </label>
          </div>
        </div>

        <div className="selector-footer">
          <button className="btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button className="btn-primary" onClick={handleCreate}>
            Create
          </button>
        </div>
      </div>
    </div>
  );
}
