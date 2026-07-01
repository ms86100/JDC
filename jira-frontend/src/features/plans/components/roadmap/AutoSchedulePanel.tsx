interface AutoSchedulePanelProps {
  onClose: () => void;
  onPreview: () => void;
}

export default function AutoSchedulePanel({ onClose, onPreview }: AutoSchedulePanelProps) {
  return (
    <>
      <button
        type="button"
        className="sa-rm-modal-backdrop"
        aria-label="Close auto-schedule"
        onClick={onClose}
      />
      <div
        className="sa-rm-modal sa-rm-modal--autoschedule"
        role="dialog"
        aria-labelledby="autoschedule-title"
        aria-modal="true"
      >
        <div className="sa-rm-modal-header">
          <h2 id="autoschedule-title">Auto-schedule</h2>
          <button type="button" className="sa-rm-popover-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <div className="sa-rm-modal-body">
          <p className="sa-rm-modal-lead">
            Select which issues you want to auto-schedule using the checkboxes in the Scope section.
            If you select none, the auto-scheduler will create a plan for all issues based on ranking,
            team velocity, and dependencies.
          </p>

          <fieldset className="sa-rm-fieldset">
            <legend>Overwriting issue values</legend>
            <table className="sa-rm-overwrite-table">
              <thead>
                <tr>
                  <th scope="col" className="sa-rm-overwrite-table__label-col" />
                  <th scope="col">Sprints</th>
                  <th scope="col">Releases</th>
                  <th scope="col">Teams</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th scope="row">All values</th>
                  <td><input type="radio" name="sprints" aria-label="Sprints — all values" /></td>
                  <td><input type="radio" name="releases" aria-label="Releases — all values" /></td>
                  <td><input type="radio" name="teams" aria-label="Teams — all values" /></td>
                </tr>
                <tr>
                  <th scope="row">Empty values only</th>
                  <td>
                    <input
                      type="radio"
                      name="sprints"
                      defaultChecked
                      aria-label="Sprints — empty values only"
                    />
                  </td>
                  <td>
                    <input
                      type="radio"
                      name="releases"
                      defaultChecked
                      aria-label="Releases — empty values only"
                    />
                  </td>
                  <td>
                    <input
                      type="radio"
                      name="teams"
                      defaultChecked
                      aria-label="Teams — empty values only"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </fieldset>
        </div>

        <div className="sa-rm-modal-footer">
          <button type="button" className="sa-rm-btn" onClick={onClose}>
            Cancel
          </button>
          <button type="button" className="sa-rm-btn sa-rm-btn--primary" onClick={onPreview}>
            Preview results
          </button>
        </div>
      </div>
    </>
  );
}
