import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import './SystemInfoPage.css';

export default function SystemInfoPage() {
  const [activeTab, setActiveTab] = useState('system');

  return (
    <AdminLayout>
      <div className="dc-page">
        <div className="dc-page-header" style={{ marginBottom: 24 }}>
          <h1 className="dc-page-title">System Information</h1>
          <p className="dc-page-subtitle">View information about your Jira installation</p>
        </div>

        {/* Tabs */}
        <div className="dc-tabs" style={{ marginBottom: 16 }}>
          <button
            className={`dc-tab ${activeTab === 'system' ? 'dc-tab-active' : ''}`}
            onClick={() => setActiveTab('system')}
          >
            System Info
          </button>
          <button
            className={`dc-tab ${activeTab === 'database' ? 'dc-tab-active' : ''}`}
            onClick={() => setActiveTab('database')}
          >
            Database
          </button>
          <button
            className={`dc-tab ${activeTab === 'connection' ? 'dc-tab-active' : ''}`}
            onClick={() => setActiveTab('connection')}
          >
            Connection Pool
          </button>
        </div>

        {/* System Info Table */}
        <div className="dc-card" style={{ marginBottom: 16 }}>
          <div className="dc-card-header">
            <h3 className="dc-card-title">Jira Information</h3>
            <button className="dc-btn dc-btn-sm dc-btn-secondary">View log</button>
          </div>
          <div className="dc-card-body">
            <table className="dc-info-table">
              <tbody>
                <tr>
                  <td>Version</td>
                  <td>Jira DC 11.3.0.240912130</td>
                </tr>
                <tr>
                  <td>Build</td>
                  <td>#80212</td>
                </tr>
                <tr>
                  <td>Application Server</td>
                  <td>Apache Tomcat 10.1.33</td>
                </tr>
                <tr>
                  <td>Platform</td>
                  <td>Linux 6.10.3-200.fc40.x86_64 (amd64)</td>
                </tr>
                <tr>
                  <td>Active Directory</td>
                  <td>Atlassian Crowd</td>
                </tr>
                <tr>
                  <td>Server ID</td>
                  <td>AAABLAAAGQABAAh8AAB</td>
                </tr>
                <tr>
                  <td>Installation Date</td>
                  <td>Jan 16, 2025</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Build Information */}
        <div className="dc-card" style={{ marginBottom: 16 }}>
          <div className="dc-card-header">
            <h3 className="dc-card-title">Build Information</h3>
          </div>
          <div className="dc-card-body">
            <table className="dc-info-table">
              <tbody>
                <tr>
                  <td>Build Date</td>
                  <td>Jan 16, 2025</td>
                </tr>
                <tr>
                  <td>Build Revision</td>
                  <td>80212a91dabc1234567890abcdef</td>
                </tr>
                <tr>
                  <td>Database Type</td>
                  <td>PostgreSQL 17.4</td>
                </tr>
                <tr>
                  <td>Database Connection</td>
                  <td>Direct JDBC</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        {/* Memory Info */}
        <div className="dc-card">
          <div className="dc-card-header">
            <h3 className="dc-card-title">Memory</h3>
            <button className="dc-btn dc-btn-sm dc-btn-secondary">GC Log</button>
          </div>
          <div className="dc-card-body">
            <table className="dc-info-table">
              <tbody>
                <tr>
                  <td>Used</td>
                  <td>4.05 GB</td>
                </tr>
                <tr>
                  <td>Maximum</td>
                  <td>8.00 GB</td>
                </tr>
                <tr>
                  <td>Total Allocated</td>
                  <td>8.00 GB</td>
                </tr>
                <tr>
                  <td>Free</td>
                  <td>3.95 GB</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}