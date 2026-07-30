interface ProgramScheduleSettingsPopoverProps {
  onClose: () => void;
  estimation: string;
  onEstimationChange: (v: string) => void;
  showDependencies: boolean;
  onShowDependenciesChange: (v: boolean) => void;
}

export default function ProgramScheduleSettingsPopover({
  onClose,
  estimation,
  onEstimationChange,
  showDependencies,
  onShowDependenciesChange,
}: ProgramScheduleSettingsPopoverProps) {
  return (
    <div className="jdc-panel-overlay jdc-program-settings-popover">
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
        <strong>Schedule settings</strong>
        <button type="button" className="jdc-btn" onClick={onClose}>×</button>
      </div>
      <div className="jdc-form-row">
        <label>Estimation</label>
        <select value={estimation} onChange={(e) => onEstimationChange(e.target.value)}>
          <option value="days">Days</option>
          <option value="hours">Hours</option>
          <option value="story-points">Story points</option>
        </select>
      </div>
      <label className="jdc-checkbox-inline" style={{ marginTop: 12 }}>
        <input
          type="checkbox"
          checked={showDependencies}
          onChange={(e) => onShowDependenciesChange(e.target.checked)}
        />
        Show dependencies on schedule
      </label>
      <label className="jdc-checkbox-inline">
        <input type="checkbox" defaultChecked />
        Use sprint dates when missing
      </label>
    </div>
  );
}
