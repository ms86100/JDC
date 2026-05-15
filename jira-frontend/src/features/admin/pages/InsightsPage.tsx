import React from 'react';
import AdminLayout from '../components/AdminLayout';

export default function InsightsPage() {
  return (
    <AdminLayout>
      <div className="dc-page">
        <div className="dc-page-header" style={{ marginBottom: 24 }}>
          <h1 className="dc-page-title">Insights</h1>
          <p className="dc-page-subtitle">Analytics and aggregate statistics for your Jira instance</p>
        </div>

        <div className="dc-card">
          <div className="dc-card-body">
            <div className="dc-empty">
              <span className="dc-empty-icon">🔍</span>
              <h3 className="dc-empty-title">Insights data is being collected</h3>
              <p className="dc-empty-description">
                Analytics will populate as users interact with Jira.
              </p>
            </div>
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}