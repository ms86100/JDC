import React from 'react';
import type { MigrationStep } from '../constants/wizardSteps';
import { STEP_LABELS } from '../constants/wizardSteps';

interface MigrationWizardStepperProps {
  activeStepOrder: MigrationStep[];
  currentStep: MigrationStep;
  getStepIndex: (step: MigrationStep) => number;
}

export default function MigrationWizardStepper({
  activeStepOrder,
  currentStep,
  getStepIndex,
}: MigrationWizardStepperProps) {
  const visibleSteps = activeStepOrder.filter((s) => s !== 'complete');
  const currentIndex = getStepIndex(currentStep);

  return (
    <div className="flex items-center gap-2 mb-8" data-testid="migration-wizard-stepper">
      {visibleSteps.map((step, index) => {
        const stepIndex = activeStepOrder.indexOf(step);
        const isActive = stepIndex <= currentIndex && currentStep !== 'complete';
        const isCurrent =
          step === currentStep ||
          (currentStep === 'complete' && index === visibleSteps.length - 1);

        let pillClass = 'migration-step-pill migration-step-pill--pending';
        if (isCurrent) pillClass = 'migration-step-pill migration-step-pill--current';
        else if (isActive && stepIndex < currentIndex) pillClass = 'migration-step-pill migration-step-pill--done';

        return (
          <React.Fragment key={step}>
            <div className="flex items-center">
              <div className={pillClass}>
                {isActive && stepIndex < currentIndex ? '✓' : index + 1}
              </div>
              <span
                style={{
                  marginLeft: 8,
                  fontSize: 'var(--sa-fs-sm)',
                  color: isActive ? 'var(--sa-n900)' : 'var(--sa-n500)',
                }}
              >
                {STEP_LABELS[step]}
              </span>
            </div>
            {index < visibleSteps.length - 1 && (
              <div
                className="flex-1 h-1 mx-4 rounded"
                style={{
                  background:
                    stepIndex < currentIndex ? 'var(--sa-status-done-fg)' : 'var(--sa-n200)',
                }}
              />
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}
