import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import './BulkCreateWizard.css';

interface BulkCreateWizardProps {
  initialStep?: number;
}

export default function BulkCreateWizard({ initialStep = 0 }: BulkCreateWizardProps) {
  const [currentStep, setCurrentStep] = useState(initialStep);

  const steps = [
    { label: 'Setup', key: 'setup' },
    { label: 'Settings', key: 'settings' },
    { label: 'Map fields', key: 'map-fields' },
    { label: 'Map values', key: 'map-values' },
  ];

  const handleNext = () => {
    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handleBack = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  return (
    <AdminLayout>
      <div className="bulk-wizard">
        {/* Header */}
        <div className="bulk-wizard-header">
          <h1 className="bulk-wizard-title">Bulk Create Setup</h1>
          <div className="bulk-wizard-stepper">
            {steps.map((step, index) => (
              <div
                key={step.key}
                className={`bulk-step ${index === currentStep ? 'bulk-step-active' : ''} ${index < currentStep ? 'bulk-step-completed' : ''}`}
              >
                <div className="bulk-step-indicator">
                  <div className="bulk-step-dot"></div>
                  {index < currentStep && <span className="bulk-step-check">✓</span>}
                </div>
                <span className="bulk-step-label">{step.label}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Content */}
        <div className="bulk-wizard-content">
          {currentStep === 0 && <BulkSetupStep />}
          {currentStep === 1 && <BulkSettingsStep />}
          {currentStep === 2 && <BulkMapFieldsStep />}
          {currentStep === 3 && <BulkMapValuesStep />}
        </div>

        {/* Footer */}
        <div className="bulk-wizard-footer">
          <button
            className="bulk-btn-primary"
            onClick={handleNext}
            disabled={currentStep === steps.length - 1}
          >
            Next
          </button>
          <button
            className="bulk-btn-link"
            onClick={handleBack}
            disabled={currentStep === 0}
          >
            Back
          </button>
        </div>

        {/* System Info */}
        <div className="bulk-wizard-system-info">
          <span>Systems and Avionics 11.3.0</span>
          <span className="bulk-info-separator">|</span>
          <span>Supported by Systems and Avionics</span>
        </div>
      </div>
    </AdminLayout>
  );
}

// Step 1: Setup
function BulkSetupStep() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [useConfigFile, setUseConfigFile] = useState(false);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setSelectedFile(e.target.files[0]);
    }
  };

  return (
    <div className="bulk-step-content">
      <h2 className="bulk-section-title">Setup</h2>
      <div className="bulk-section-divider"></div>

      <div className="bulk-form-section">
        <p className="bulk-instructions">
          To import issues in bulk, you need to provide the data in a CSV file format.
        </p>

        {/* CSV Source File */}
        <div className="bulk-form-row">
          <label className="bulk-form-label bulk-form-label-required">
            CSV Source File
          </label>
          <div className="bulk-form-input">
            <label className="bulk-file-button">
              <input
                type="file"
                accept=".csv"
                onChange={handleFileChange}
                className="bulk-file-input"
              />
              Choose File
            </label>
            <span className="bulk-file-name">
              {selectedFile ? selectedFile.name : 'No file chosen'}
            </span>
          </div>
        </div>
        <div className="bulk-form-row">
          <div className="bulk-form-label"></div>
          <span className="bulk-form-hint">The maximum file upload size is 10.00 MB.</span>
        </div>

        {/* Use Existing Config */}
        <div className="bulk-form-row">
          <div className="bulk-form-label"></div>
          <div className="bulk-form-input">
            <label className="bulk-checkbox">
              <input
                type="checkbox"
                checked={useConfigFile}
                onChange={(e) => setUseConfigFile(e.target.checked)}
              />
              <span>Use an existing configuration file</span>
            </label>
            <span className="bulk-form-hint">
              If you have used this importer before, you may have saved the configuration you used.
              You can use that configuration again to save time.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

// Step 2: Settings
function BulkSettingsStep() {
  const [delimiter, setDelimiter] = useState(',');
  const [encoding, setEncoding] = useState('UTF-8');
  const [hasHeader, setHasHeader] = useState(true);

  return (
    <div className="bulk-step-content">
      <h2 className="bulk-section-title">Settings</h2>
      <div className="bulk-section-divider"></div>

      <div className="bulk-form-section">
        <p className="bulk-instructions">
          Configure the CSV file settings for proper parsing.
        </p>

        {/* Delimiter */}
        <div className="bulk-form-row">
          <label className="bulk-form-label">Delimiter</label>
          <div className="bulk-form-input">
            <select
              className="bulk-form-select"
              value={delimiter}
              onChange={(e) => setDelimiter(e.target.value)}
            >
              <option value=",">Comma (,)</option>
              <option value=";">Semicolon (;)</option>
              <option value="	">Tab</option>
              <option value="|">Pipe (|)</option>
            </select>
          </div>
        </div>

        {/* Encoding */}
        <div className="bulk-form-row">
          <label className="bulk-form-label">File Encoding</label>
          <div className="bulk-form-input">
            <select
              className="bulk-form-select"
              value={encoding}
              onChange={(e) => setEncoding(e.target.value)}
            >
              <option value="UTF-8">UTF-8</option>
              <option value="UTF-16">UTF-16</option>
              <option value="ISO-8859-1">ISO-8859-1</option>
              <option value="Windows-1252">Windows-1252</option>
            </select>
          </div>
        </div>

        {/* Has Header */}
        <div className="bulk-form-row">
          <label className="bulk-form-label">First Row</label>
          <div className="bulk-form-input">
            <label className="bulk-checkbox">
              <input
                type="checkbox"
                checked={hasHeader}
                onChange={(e) => setHasHeader(e.target.checked)}
              />
              <span>Contains header row</span>
            </label>
            <span className="bulk-form-hint">
              When enabled, the first row is treated as column headers.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

// Step 3: Map Fields
function BulkMapFieldsStep() {
  const [mappings, setMappings] = useState([
    { csvColumn: '', avisysField: '' },
    { csvColumn: '', avisysField: '' },
    { csvColumn: '', avisysField: '' },
  ]);

  const avisysFields = [
    'Summary', 'Description', 'Issue Type', 'Priority', 'Status',
    'Assignee', 'Reporter', 'Labels', 'Components', 'Due Date',
    'Project', 'Sprint', 'Story Points', 'Epic Link'
  ];

  const handleMappingChange = (index: number, field: 'csvColumn' | 'avisysField', value: string) => {
    const newMappings = [...mappings];
    newMappings[index] = { ...newMappings[index], [field]: value };
    setMappings(newMappings);
  };

  return (
    <div className="bulk-step-content">
      <h2 className="bulk-section-title">Map fields</h2>
      <div className="bulk-section-divider"></div>

      <div className="bulk-form-section">
        <p className="bulk-instructions">
          Map CSV columns to Systems and Avionics issue fields.
        </p>

        <table className="bulk-mapping-table">
          <thead>
            <tr>
              <th>CSV Column</th>
              <th>Systems and Avionics Field</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {mappings.map((mapping, index) => (
              <tr key={index}>
                <td>
                  <input
                    type="text"
                    className="bulk-form-input-small"
                    value={mapping.csvColumn}
                    onChange={(e) => handleMappingChange(index, 'csvColumn', e.target.value)}
                    placeholder="Column name"
                  />
                </td>
                <td>
                  <select
                    className="bulk-form-select-full"
                    value={mapping.avisysField}
                    onChange={(e) => handleMappingChange(index, 'avisysField', e.target.value)}
                  >
                    <option value="">Select a field...</option>
                    {avisysFields.map((field) => (
                      <option key={field} value={field}>{field}</option>
                    ))}
                  </select>
                </td>
                <td>
                  <button
                    className="bulk-remove-btn"
                    onClick={() => setMappings(mappings.filter((_, i) => i !== index))}
                    disabled={mappings.length <= 1}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <button
          className="bulk-add-btn"
          onClick={() => setMappings([...mappings, { csvColumn: '', avisysField: '' }])}
        >
          + Add Mapping
        </button>
      </div>
    </div>
  );
}

// Step 4: Map Values
function BulkMapValuesStep() {
  const [valueMappings, setValueMappings] = useState([
    { csvValue: '', avisysValue: '' },
    { csvValue: '', avisysValue: '' },
  ]);

  return (
    <div className="bulk-step-content">
      <h2 className="bulk-section-title">Map values</h2>
      <div className="bulk-section-divider"></div>

      <div className="bulk-form-section">
        <p className="bulk-instructions">
          Map CSV values to Systems and Avionics status/priority/issue type values.
        </p>

        <table className="bulk-mapping-table">
          <thead>
            <tr>
              <th>CSV Value</th>
              <th>Systems and Avionics Value</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {valueMappings.map((mapping, index) => (
              <tr key={index}>
                <td>
                  <input
                    type="text"
                    className="bulk-form-input-small"
                    value={mapping.csvValue}
                    onChange={(e) => {
                      const newMappings = [...valueMappings];
                      newMappings[index] = { ...newMappings[index], csvValue: e.target.value };
                      setValueMappings(newMappings);
                    }}
                    placeholder="Value from CSV"
                  />
                </td>
                <td>
                  <input
                    type="text"
                    className="bulk-form-input-small"
                    value={mapping.avisysValue}
                    onChange={(e) => {
                      const newMappings = [...valueMappings];
                      newMappings[index] = { ...newMappings[index], avisysValue: e.target.value };
                      setValueMappings(newMappings);
                    }}
                    placeholder="Systems and Avionics value"
                  />
                </td>
                <td>
                  <button
                    className="bulk-remove-btn"
                    onClick={() => setValueMappings(valueMappings.filter((_, i) => i !== index))}
                    disabled={valueMappings.length <= 1}
                  >
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <button
          className="bulk-add-btn"
          onClick={() => setValueMappings([...valueMappings, { csvValue: '', avisysValue: '' }])}
        >
          + Add Mapping
        </button>
      </div>
    </div>
  );
}