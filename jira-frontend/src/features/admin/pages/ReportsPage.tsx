import React from 'react';

export default function ReportsPage() {
  return (
    <div className="dc-page">
      <div className="dc-page-header" style={{ marginBottom: 24 }}>
        <h1 className="dc-page-title">Reports</h1>
        <p className="dc-page-subtitle">View and manage reports for your Systems and Avionics instance</p>
      </div>

      <div className="dc-card">
        <div className="dc-card-body">
          <div className="dc-empty">
            <span className="dc-empty-icon">📊</span>
            <h3 className="dc-empty-title">No reports generated yet</h3>
            <p className="dc-empty-description">
              Reports will appear here once data is available.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}