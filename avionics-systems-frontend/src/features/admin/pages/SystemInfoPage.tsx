import React, { useState } from 'react';
import './SystemInfoPage.css';

type TabId = 'system' | 'database' | 'memory';

interface InfoRow {
  label: string;
  value: string;
  mono?: boolean;
}

function InfoPanel({ title, action, rows }: { title: string; action?: React.ReactNode; rows: InfoRow[] }) {
  return (
    <section className="ab-info-panel">
      <header className="ab-info-panel-header">
        <h2 className="ab-info-panel-title">{title}</h2>
        {action}
      </header>
      <dl className="ab-info-dl">
        {rows.map((row) => (
          <div key={row.label} className="ab-info-row">
            <dt>{row.label}</dt>
            <dd className={row.mono ? 'ab-info-mono' : undefined}>{row.value}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

export default function SystemInfoPage() {
  const [activeTab, setActiveTab] = useState<TabId>('system');

  const memoryUsedGb = 4.05;
  const memoryMaxGb = 8;
  const memoryPct = Math.round((memoryUsedGb / memoryMaxGb) * 100);

  return (
    <div className="dc-page ab-admin-page">
      <header className="dc-page-header">
        <h1 className="dc-page-title">System information</h1>
        <p className="dc-page-subtitle">
          View version, platform, database, and runtime details for your Systems and Avionics instance.
        </p>
      </header>

      <div className="dc-tabs" role="tablist" aria-label="System information sections">
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'system'}
          className={`dc-tab${activeTab === 'system' ? ' dc-tab-active' : ''}`}
          onClick={() => setActiveTab('system')}
        >
          System info
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'database'}
          className={`dc-tab${activeTab === 'database' ? ' dc-tab-active' : ''}`}
          onClick={() => setActiveTab('database')}
        >
          Database
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'memory'}
          className={`dc-tab${activeTab === 'memory' ? ' dc-tab-active' : ''}`}
          onClick={() => setActiveTab('memory')}
        >
          Memory
        </button>
      </div>

      <div className="ab-tab-panels">
        {activeTab === 'system' && (
          <div role="tabpanel" className="ab-tab-panel">
            <InfoPanel
              title="Systems and Avionics"
              action={
                <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary">
                  View log
                </button>
              }
              rows={[
                { label: 'Version', value: 'Systems and Avionics 11.3.0.240912130' },
                { label: 'Build', value: '#80212', mono: true },
                { label: 'Application server', value: 'Apache Tomcat 10.1.33' },
                { label: 'Platform', value: 'Linux 6.10.3-200.fc40.x86_64 (amd64)' },
                { label: 'Directory', value: 'Systems and Avionics Directory' },
                { label: 'Server ID', value: 'AAABLAAAGQABAAh8AAB', mono: true },
                { label: 'Installation date', value: '16 Jan 2025' },
              ]}
            />
            <InfoPanel
              title="Build"
              rows={[
                { label: 'Build date', value: '16 Jan 2025' },
                { label: 'Build revision', value: '80212a91dabc1234567890abcdef', mono: true },
              ]}
            />
          </div>
        )}

        {activeTab === 'database' && (
          <div role="tabpanel" className="ab-tab-panel">
            <InfoPanel
              title="Database connection"
              rows={[
                { label: 'Database type', value: 'PostgreSQL 17.4' },
                { label: 'Connection', value: 'Direct JDBC' },
                { label: 'Host', value: 'localhost:5432' },
                { label: 'Database name', value: 'avisys_platform' },
                { label: 'Pool status', value: 'Healthy' },
                { label: 'Active connections', value: '12 / 20' },
              ]}
            />
          </div>
        )}

        {activeTab === 'memory' && (
          <div role="tabpanel" className="ab-tab-panel">
            <InfoPanel
              title="JVM memory"
              action={
                <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary">
                  GC log
                </button>
              }
              rows={[
                { label: 'Used', value: `${memoryUsedGb.toFixed(2)} GB` },
                { label: 'Maximum', value: `${memoryMaxGb.toFixed(2)} GB` },
                { label: 'Total allocated', value: `${memoryMaxGb.toFixed(2)} GB` },
                { label: 'Free', value: `${(memoryMaxGb - memoryUsedGb).toFixed(2)} GB` },
              ]}
            />
            <div className="ab-memory-meter">
              <div className="ab-memory-meter-label">
                <span>Heap utilisation</span>
                <span>{memoryPct}%</span>
              </div>
              <div className="ab-memory-meter-track">
                <div className="ab-memory-meter-fill" style={{ width: `${memoryPct}%` }} />
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
