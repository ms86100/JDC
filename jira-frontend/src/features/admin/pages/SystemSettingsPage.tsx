import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import './SystemSettingsPage.css';

type TabType = 'general' | 'appearance' | 'attachments' | 'time-tracking' | 'subtasks' | 'import' | 'licensing';

export default function SystemSettingsPage() {
  const [activeTab, setActiveTab] = useState<TabType>('general');

  const renderGeneralSettings = () => (
    <div className="settings-section">
      <h3 className="section-title">General Configuration</h3>
      <div className="settings-group">
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Base URL</label>
            <p className="setting-description">The URL used to access Jira</p>
          </div>
          <input type="text" className="admin-form-input setting-input" defaultValue="https://jira.example.com" />
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Default Language</label>
            <p className="setting-description">Language used for new users</p>
          </div>
          <select className="admin-form-select setting-input" defaultValue="en_US">
            <option value="en_US">English (US)</option>
            <option value="en_GB">English (UK)</option>
            <option value="de">German</option>
            <option value="fr">French</option>
            <option value="es">Spanish</option>
            <option value="ja">Japanese</option>
            <option value="zh_CN">Chinese (Simplified)</option>
          </select>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Default Time Zone</label>
            <p className="setting-description">Time zone for the instance</p>
          </div>
          <select className="admin-form-select setting-input" defaultValue="America/New_York">
            <option value="America/New_York">Eastern Time (ET)</option>
            <option value="America/Chicago">Central Time (CT)</option>
            <option value="America/Denver">Mountain Time (MT)</option>
            <option value="America/Los_Angeles">Pacific Time (PT)</option>
            <option value="Europe/London">London (GMT)</option>
            <option value="Europe/Paris">Paris (CET)</option>
            <option value="Asia/Tokyo">Tokyo (JST)</option>
            <option value="Asia/Shanghai">Shanghai (CST)</option>
          </select>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Application Title</label>
            <p className="setting-description">Title shown in browser tab</p>
          </div>
          <input type="text" className="admin-form-input setting-input" defaultValue="Jira" />
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Maximum Attachment Size</label>
            <p className="setting-description">Maximum size for file attachments</p>
          </div>
          <select className="admin-form-select setting-input" defaultValue="10mb">
            <option value="1mb">1 MB</option>
            <option value="5mb">5 MB</option>
            <option value="10mb">10 MB</option>
            <option value="25mb">25 MB</option>
            <option value="50mb">50 MB</option>
            <option value="100mb">100 MB</option>
            <option value="unlimited">Unlimited</option>
          </select>
        </div>
      </div>
      <div className="settings-actions">
        <button className="admin-btn-primary">Save Changes</button>
      </div>
    </div>
  );

  const renderAppearanceSettings = () => (
    <div className="settings-section">
      <h3 className="section-title">Look and Feel</h3>
      <div className="settings-group">
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Logo</label>
            <p className="setting-description">Upload a custom logo for the header</p>
          </div>
          <div className="logo-upload">
            <div className="logo-preview">J</div>
            <button className="admin-btn-secondary">Change Logo</button>
          </div>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Color Theme</label>
            <p className="setting-description">Select the color scheme</p>
          </div>
          <select className="admin-form-select setting-input" defaultValue="dark">
            <option value="dark">Dark (Blue)</option>
            <option value="light">Light</option>
            <option value="high-contrast">High Contrast</option>
          </select>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Favourite Icon</label>
            <p className="setting-description">Icon shown in browser bookmarks</p>
          </div>
          <div className="logo-upload">
            <div className="logo-preview small">J</div>
            <button className="admin-btn-secondary">Upload Icon</button>
          </div>
        </div>
      </div>
      <div className="settings-actions">
        <button className="admin-btn-primary">Save Changes</button>
      </div>
    </div>
  );

  const renderAttachmentsSettings = () => (
    <div className="settings-section">
      <h3 className="section-title">Attachments</h3>
      <div className="settings-group">
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Attachment Storage</label>
            <p className="setting-description">Where attachments are stored</p>
          </div>
          <select className="admin-form-select setting-input" defaultValue="database">
            <option value="database">Database</option>
            <option value="filesystem">File System</option>
            <option value="s3">Amazon S3</option>
          </select>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Allowed File Types</label>
            <p className="setting-description">File extensions allowed for upload</p>
          </div>
          <input type="text" className="admin-form-input setting-input" defaultValue="gif,jpg,jpeg,png,doc,docx,xls,xlsx,pdf,zip,rar,txt,csv,svg" />
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Viewport Max Dimensions</label>
            <p className="setting-description">Maximum dimensions for inline image viewing</p>
          </div>
          <div className="dimension-inputs">
            <input type="number" className="admin-form-input" defaultValue="2048" style={{ width: '100px' }} />
            <span style={{ padding: '0 8px' }}>x</span>
            <input type="number" className="admin-form-input" defaultValue="2048" style={{ width: '100px' }} />
          </div>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Attachment Encoding</label>
            <p className="setting-description">Character encoding for attachments</p>
          </div>
          <select className="admin-form-select setting-input" defaultValue="UTF-8">
            <option value="UTF-8">UTF-8</option>
            <option value="ISO-8859-1">ISO-8859-1</option>
          </select>
        </div>
      </div>
      <div className="settings-actions">
        <button className="admin-btn-primary">Save Changes</button>
      </div>
    </div>
  );

  const renderTimeTrackingSettings = () => (
    <div className="settings-section">
      <h3 className="section-title">Time Tracking</h3>
      <div className="settings-group">
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Enable Time Tracking</label>
            <p className="setting-description">Allow users to log time on issues</p>
          </div>
          <label className="toggle-switch">
            <input type="checkbox" defaultChecked />
            <span className="toggle-slider"></span>
          </label>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Time Format</label>
            <p className="setting-description">How time is displayed</p>
          </div>
          <select className="admin-form-select setting-input" defaultValue="pretty">
            <option value="pretty">Pretty (e.g. 1h 30m)</option>
            <option value="days">Days (e.g. 1.5d)</option>
            <option value="hours">Hours (e.g. 7.5h)</option>
          </select>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Working Days Per Week</label>
            <p className="setting-description">Number of working days per week</p>
          </div>
          <input type="number" className="admin-form-input setting-input" defaultValue="5" style={{ width: '100px' }} />
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Hours Per Day</label>
            <p className="setting-description">Number of hours in a work day</p>
          </div>
          <input type="number" className="admin-form-input setting-input" defaultValue="8" style={{ width: '100px' }} />
        </div>
      </div>
      <div className="settings-actions">
        <button className="admin-btn-primary">Save Changes</button>
      </div>
    </div>
  );

  const renderSubtasksSettings = () => (
    <div className="settings-section">
      <h3 className="section-title">Subtasks</h3>
      <div className="settings-group">
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Allow Subtasks</label>
            <p className="setting-description">Enable subtasks on issues</p>
          </div>
          <label className="toggle-switch">
            <input type="checkbox" defaultChecked />
            <span className="toggle-slider"></span>
          </label>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Allow Subtasks of Subtasks</label>
            <p className="setting-description">Allow multiple levels of subtasks</p>
          </div>
          <label className="toggle-switch">
            <input type="checkbox" />
            <span className="toggle-slider"></span>
          </label>
        </div>
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Default Subtask Issue Type</label>
            <p className="setting-description">Issue type used when creating subtasks</p>
          </div>
          <select className="admin-form-select setting-input" defaultValue="subtask">
            <option value="subtask">Subtask</option>
            <option value="bug">Bug</option>
          </select>
        </div>
      </div>
      <div className="settings-actions">
        <button className="admin-btn-primary">Save Changes</button>
      </div>
    </div>
  );

  const renderImportSettings = () => (
    <div className="settings-section">
      <h3 className="section-title">Import</h3>
      <div className="settings-group">
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Import Tool</label>
            <p className="setting-description">Import data from other systems</p>
          </div>
          <div className="import-tools">
            <button className="import-tool-btn">
              <span className="import-tool-icon">T</span>
              <span>CSV</span>
            </button>
            <button className="import-tool-btn">
              <span className="import-tool-icon">J</span>
              <span>Jira</span>
            </button>
            <button className="import-tool-btn">
              <span className="import-tool-icon">B</span>
              <span>Bugzilla</span>
            </button>
            <button className="import-tool-btn">
              <span className="import-tool-icon">F</span>
              <span>FogBugz</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  const renderLicensingSettings = () => (
    <div className="settings-section">
      <h3 className="section-title">Licensing</h3>
      <div className="license-info-card">
        <div className="license-status">
          <span className="license-status-badge active">Active</span>
          <span className="license-type">Jira Data Center - Standard</span>
        </div>
        <div className="license-details">
          <div className="license-detail">
            <span className="license-detail-label">License ID</span>
            <span className="license-detail-value">LIC-12345-67890-ABCDE</span>
          </div>
          <div className="license-detail">
            <span className="license-detail-label">Expiry Date</span>
            <span className="license-detail-value">December 31, 2027</span>
          </div>
          <div className="license-detail">
            <span className="license-detail-label">Support</span>
            <span className="license-detail-value">Standard Support (expires Dec 31, 2026)</span>
          </div>
        </div>
      </div>

      <div className="settings-group">
        <div className="setting-item">
          <div className="setting-info">
            <label className="setting-label">Add License</label>
            <p className="setting-description">Enter a new license key</p>
          </div>
          <textarea className="admin-form-textarea setting-input" placeholder="Paste your license key here..." rows={3}></textarea>
        </div>
      </div>
      <div className="settings-actions">
        <button className="admin-btn-primary">Apply License</button>
      </div>
    </div>
  );

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">System Settings</h1>
          <p className="admin-page-description">
            Configure system-wide settings and preferences.
          </p>
        </div>

        <div className="settings-tabs">
          <button
            className={`settings-tab ${activeTab === 'general' ? 'active' : ''}`}
            onClick={() => setActiveTab('general')}
          >
            General
          </button>
          <button
            className={`settings-tab ${activeTab === 'appearance' ? 'active' : ''}`}
            onClick={() => setActiveTab('appearance')}
          >
            Look and Feel
          </button>
          <button
            className={`settings-tab ${activeTab === 'attachments' ? 'active' : ''}`}
            onClick={() => setActiveTab('attachments')}
          >
            Attachments
          </button>
          <button
            className={`settings-tab ${activeTab === 'time-tracking' ? 'active' : ''}`}
            onClick={() => setActiveTab('time-tracking')}
          >
            Time Tracking
          </button>
          <button
            className={`settings-tab ${activeTab === 'subtasks' ? 'active' : ''}`}
            onClick={() => setActiveTab('subtasks')}
          >
            Subtasks
          </button>
          <button
            className={`settings-tab ${activeTab === 'import' ? 'active' : ''}`}
            onClick={() => setActiveTab('import')}
          >
            Import
          </button>
          <button
            className={`settings-tab ${activeTab === 'licensing' ? 'active' : ''}`}
            onClick={() => setActiveTab('licensing')}
          >
            Licensing
          </button>
        </div>

        <div className="settings-content">
          {activeTab === 'general' && renderGeneralSettings()}
          {activeTab === 'appearance' && renderAppearanceSettings()}
          {activeTab === 'attachments' && renderAttachmentsSettings()}
          {activeTab === 'time-tracking' && renderTimeTrackingSettings()}
          {activeTab === 'subtasks' && renderSubtasksSettings()}
          {activeTab === 'import' && renderImportSettings()}
          {activeTab === 'licensing' && renderLicensingSettings()}
        </div>
      </div>
    </AdminLayout>
  );
}