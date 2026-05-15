import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useClusterNodes, useScheduledJobs, ClusterNode, ScheduledJob } from '../hooks/useAdminApi';
import './DataCenterPage.css';

type TabType = 'overview' | 'cluster' | 'cache' | 'jobs' | 'services' | 'indexing';

export default function DataCenterPage() {
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const { data: clusterNodes } = useClusterNodes();
  const { data: scheduledJobs } = useScheduledJobs();

  const renderOverview = () => (
    <div className="datacenter-overview">
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Cluster Nodes</div>
          <div className="admin-stat-value">{clusterNodes?.length || 0}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Scheduled Jobs</div>
          <div className="admin-stat-value">{scheduledJobs?.length || 0}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Running Jobs</div>
          <div className="admin-stat-value">
            {scheduledJobs?.filter(j => j.isRunning).length || 0}
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Enabled Jobs</div>
          <div className="admin-stat-value">
            {scheduledJobs?.filter(j => j.isEnabled).length || 0}
          </div>
        </div>
      </div>

      <div className="overview-section">
        <h3 className="section-title">System Health</h3>
        <div className="health-grid">
          <div className="health-card">
            <div className="health-icon healthy">✓</div>
            <div className="health-info">
              <div className="health-title">Database</div>
              <div className="health-status healthy">Connected</div>
            </div>
          </div>
          <div className="health-card">
            <div className="health-icon healthy">✓</div>
            <div className="health-info">
              <div className="health-title">Cache</div>
              <div className="health-status healthy">Operational</div>
            </div>
          </div>
          <div className="health-card">
            <div className="health-icon healthy">✓</div>
            <div className="health-info">
              <div className="health-title">Search Index</div>
              <div className="health-status healthy">Up to date</div>
            </div>
          </div>
          <div className="health-card">
            <div className="health-icon healthy">✓</div>
            <div className="health-info">
              <div className="health-title">Mail Queue</div>
              <div className="health-status healthy">Empty</div>
            </div>
          </div>
        </div>
      </div>

      <div className="overview-section">
        <h3 className="section-title">Quick Actions</h3>
        <div className="quick-actions">
          <button className="quick-action-btn">
            <span className="quick-action-icon">⟳</span>
            Clear Cache
          </button>
          <button className="quick-action-btn">
            <span className="quick-action-icon">⌂</span>
            Reindex All
          </button>
          <button className="quick-action-btn">
            <span className="quick-action-icon">⬇</span>
            System Backup
          </button>
          <button className="quick-action-btn">
            <span className="quick-action-icon">⚙</span>
            Service Manager
          </button>
        </div>
      </div>
    </div>
  );

  const renderClusterNodes = () => (
    <div className="cluster-section">
      <div className="section-header">
        <h3 className="section-title">Cluster Nodes</h3>
        <p className="section-description">Manage nodes in your Data Center cluster.</p>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Node</th>
              <th>IP Address</th>
              <th>Type</th>
              <th>State</th>
              <th>CPU Usage</th>
              <th>Memory Usage</th>
              <th>Last Heartbeat</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {clusterNodes?.map((node) => (
              <tr key={node.id}>
                <td>
                  <div className="node-cell">
                    <span className={`node-status ${node.nodeState.toLowerCase()}`}></span>
                    <span className="node-name">{node.nodeName}</span>
                  </div>
                </td>
                <td>{node.nodeIp}</td>
                <td>{node.nodeType}</td>
                <td>
                  <span className={`admin-status admin-status-${node.nodeState === 'ONLINE' ? 'active' : 'inactive'}`}>
                    {node.nodeState}
                  </span>
                </td>
                <td>
                  <div className="usage-bar">
                    <div className="usage-bar-fill" style={{ width: `${node.cpuUsage}%` }}></div>
                    <span className="usage-value">{node.cpuUsage}%</span>
                  </div>
                </td>
                <td>
                  <div className="usage-bar">
                    <div className="usage-bar-fill" style={{ width: `${node.memoryUsage}%` }}></div>
                    <span className="usage-value">{node.memoryUsage}%</span>
                  </div>
                </td>
                <td>{new Date(node.lastHeartbeat).toLocaleString()}</td>
                <td>
                  <div className="action-buttons">
                    <button className="admin-btn-secondary">Drain</button>
                    <button className="admin-btn-secondary">Details</button>
                  </div>
                </td>
              </tr>
            )) || (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '24px' }}>Loading...</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );

  const renderScheduledJobs = () => (
    <div className="jobs-section">
      <div className="section-header">
        <h3 className="section-title">Scheduled Jobs</h3>
        <p className="section-description">Monitor and manage scheduled background jobs.</p>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Job Name</th>
              <th>Type</th>
              <th>Trigger</th>
              <th>Status</th>
              <th>Last Run</th>
              <th>Next Run</th>
              <th>Duration</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {scheduledJobs?.map((job) => (
              <tr key={job.id}>
                <td>
                  <div className="job-name">{job.jobName}</div>
                  <div className="job-description">{job.description}</div>
                </td>
                <td>{job.jobType}</td>
                <td>
                  {job.cronExpression || (job.intervalMs ? `Every ${job.intervalMs / 1000}s` : 'Manual')}
                </td>
                <td>
                  <div className="job-status">
                    {job.isRunning ? (
                      <span className="admin-status admin-status-pending">Running</span>
                    ) : job.isEnabled ? (
                      <span className="admin-status admin-status-active">Enabled</span>
                    ) : (
                      <span className="admin-status admin-status-inactive">Disabled</span>
                    )}
                  </div>
                </td>
                <td>{job.lastRunAt ? new Date(job.lastRunAt).toLocaleString() : 'Never'}</td>
                <td>{job.nextRunAt ? new Date(job.nextRunAt).toLocaleString() : '-'}</td>
                <td>{job.lastDurationMs ? `${(job.lastDurationMs / 1000).toFixed(1)}s` : '-'}</td>
                <td>
                  <div className="action-buttons">
                    <button className="admin-btn-secondary" disabled={job.isRunning}>Run Now</button>
                    <button className="admin-btn-secondary">
                      {job.isEnabled ? 'Disable' : 'Enable'}
                    </button>
                  </div>
                </td>
              </tr>
            )) || (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '24px' }}>Loading...</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );

  const renderCacheSection = () => (
    <div className="cache-section">
      <div className="section-header">
        <h3 className="section-title">Cache Statistics</h3>
        <p className="section-description">Monitor and manage application caches.</p>
      </div>

      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <button className="admin-btn-secondary">Refresh</button>
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-danger">Clear All Caches</button>
        </div>
      </div>

      <div className="cache-grid">
        <div className="cache-card">
          <div className="cache-name">Issue Cache</div>
          <div className="cache-stats">
            <div className="cache-stat">
              <span className="cache-stat-label">Entries</span>
              <span className="cache-stat-value">12,345</span>
            </div>
            <div className="cache-stat">
              <span className="cache-stat-label">Hit Rate</span>
              <span className="cache-stat-value">94.5%</span>
            </div>
            <div className="cache-stat">
              <span className="cache-stat-label">Size</span>
              <span className="cache-stat-value">24 MB</span>
            </div>
          </div>
          <button className="admin-btn-secondary">Clear</button>
        </div>

        <div className="cache-card">
          <div className="cache-name">Project Cache</div>
          <div className="cache-stats">
            <div className="cache-stat">
              <span className="cache-stat-label">Entries</span>
              <span className="cache-stat-value">156</span>
            </div>
            <div className="cache-stat">
              <span className="cache-stat-label">Hit Rate</span>
              <span className="cache-stat-value">98.2%</span>
            </div>
            <div className="cache-stat">
              <span className="cache-stat-label">Size</span>
              <span className="cache-stat-value">3 MB</span>
            </div>
          </div>
          <button className="admin-btn-secondary">Clear</button>
        </div>

        <div className="cache-card">
          <div className="cache-name">User Cache</div>
          <div className="cache-stats">
            <div className="cache-stat">
              <span className="cache-stat-label">Entries</span>
              <span className="cache-stat-value">2,891</span>
            </div>
            <div className="cache-stat">
              <span className="cache-stat-label">Hit Rate</span>
              <span className="cache-stat-value">91.7%</span>
            </div>
            <div className="cache-stat">
              <span className="cache-stat-label">Size</span>
              <span className="cache-stat-value">8 MB</span>
            </div>
          </div>
          <button className="admin-btn-secondary">Clear</button>
        </div>

        <div className="cache-card">
          <div className="cache-name">Workflow Cache</div>
          <div className="cache-stats">
            <div className="cache-stat">
              <span className="cache-stat-label">Entries</span>
              <span className="cache-stat-value">47</span>
            </div>
            <div className="cache-stat">
              <span className="cache-stat-label">Hit Rate</span>
              <span className="cache-stat-value">99.1%</span>
            </div>
            <div className="cache-stat">
              <span className="cache-stat-label">Size</span>
              <span className="cache-stat-value">1 MB</span>
            </div>
          </div>
          <button className="admin-btn-secondary">Clear</button>
        </div>
      </div>
    </div>
  );

  const renderIndexingSection = () => (
    <div className="indexing-section">
      <div className="section-header">
        <h3 className="section-title">Search Indexing</h3>
        <p className="section-description">Manage the search index for faster issue lookups.</p>
      </div>

      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Total Documents</div>
          <div className="admin-stat-value">125,432</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Index Size</div>
          <div className="admin-stat-value">1.2 GB</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Last Index Time</div>
          <div className="admin-stat-value">2 min ago</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Index Health</div>
          <div className="admin-stat-value healthy">Good</div>
        </div>
      </div>

      <div className="indexing-actions">
        <button className="admin-btn-primary">Reindex All</button>
        <button className="admin-btn-secondary">Reindex Issues Only</button>
        <button className="admin-btn-secondary">Optimize Index</button>
      </div>
    </div>
  );

  const renderServicesSection = () => (
    <div className="services-section">
      <div className="section-header">
        <h3 className="section-title">Services</h3>
        <p className="section-description">Monitor and manage system services.</p>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Service</th>
              <th>Status</th>
              <th>Uptime</th>
              <th>Last Error</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Email Service</td>
              <td><span className="admin-status admin-status-active">Running</span></td>
              <td>14d 6h 23m</td>
              <td>None</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Restart</button>
                </div>
              </td>
            </tr>
            <tr>
              <td>Indexing Service</td>
              <td><span className="admin-status admin-status-active">Running</span></td>
              <td>14d 6h 23m</td>
              <td>None</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Restart</button>
                </div>
              </td>
            </tr>
            <tr>
              <td>Backup Service</td>
              <td><span className="admin-status admin-status-inactive">Stopped</span></td>
              <td>-</td>
              <td>None</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-primary">Start</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Data Center Administration</h1>
          <p className="admin-page-description">
            Manage cluster nodes, caching, indexing, and scheduled jobs for Data Center.
          </p>
        </div>

        <div className="datacenter-tabs">
          <button
            className={`datacenter-tab ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            Overview
          </button>
          <button
            className={`datacenter-tab ${activeTab === 'cluster' ? 'active' : ''}`}
            onClick={() => setActiveTab('cluster')}
          >
            Cluster Nodes
          </button>
          <button
            className={`datacenter-tab ${activeTab === 'cache' ? 'active' : ''}`}
            onClick={() => setActiveTab('cache')}
          >
            Cache
          </button>
          <button
            className={`datacenter-tab ${activeTab === 'jobs' ? 'active' : ''}`}
            onClick={() => setActiveTab('jobs')}
          >
            Scheduled Jobs
          </button>
          <button
            className={`datacenter-tab ${activeTab === 'indexing' ? 'active' : ''}`}
            onClick={() => setActiveTab('indexing')}
          >
            Indexing
          </button>
          <button
            className={`datacenter-tab ${activeTab === 'services' ? 'active' : ''}`}
            onClick={() => setActiveTab('services')}
          >
            Services
          </button>
        </div>

        <div className="datacenter-content">
          {activeTab === 'overview' && renderOverview()}
          {activeTab === 'cluster' && renderClusterNodes()}
          {activeTab === 'cache' && renderCacheSection()}
          {activeTab === 'jobs' && renderScheduledJobs()}
          {activeTab === 'indexing' && renderIndexingSection()}
          {activeTab === 'services' && renderServicesSection()}
        </div>
      </div>
    </AdminLayout>
  );
}