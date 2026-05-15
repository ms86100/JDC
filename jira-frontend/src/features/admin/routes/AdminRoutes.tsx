import React from 'react';
import { Routes, Route } from 'react-router-dom';
import UserManagementPage from '../pages/UserManagementPage';
import IssueTypesPage from '../pages/IssueTypesPage';
import PrioritiesPage from '../pages/PrioritiesPage';
import StatusesPage from '../pages/StatusesPage';
import WorkflowsPage from '../pages/WorkflowsPage';
import ScreensPage from '../pages/ScreensPage';
import DataCenterPage from '../pages/DataCenterPage';
import AuditLogsPage from '../pages/AuditLogsPage';
import SystemSettingsPage from '../pages/SystemSettingsPage';
import CustomFieldsPage from '../pages/CustomFieldsPage';
import ProjectAdministrationPage from '../pages/ProjectAdministrationPage';
import PermissionsPage from '../pages/PermissionsPage';
import AutomationPage from '../pages/AutomationPage';
import AdminDashboardPage from '../pages/AdminDashboardPage';
import SystemInfoPage from '../pages/SystemInfoPage';
import ReportsPage from '../pages/ReportsPage';
import InsightsPage from '../pages/InsightsPage';

export default function AdminRoutes() {
  return (
    <Routes>
      {/* Dashboard */}
      <Route path="/" element={<AdminDashboardPage />} />
      <Route path="/overview" element={<AdminDashboardPage />} />

      {/* System Settings */}
      <Route path="system/general" element={<SystemSettingsPage />} />
      <Route path="system/appearance" element={<SystemSettingsPage />} />
      <Route path="system/attachments" element={<SystemSettingsPage />} />
      <Route path="system/time-tracking" element={<SystemSettingsPage />} />
      <Route path="system/subtasks" element={<SystemSettingsPage />} />
      <Route path="system/import" element={<SystemSettingsPage />} />
      <Route path="system/licensing" element={<SystemSettingsPage />} />
      <Route path="system/info" element={<SystemInfoPage />} />
      <Route path="reports" element={<ReportsPage />} />
      <Route path="insights" element={<InsightsPage />} />

      {/* User Management */}
      <Route path="users" element={<UserManagementPage />} />
      <Route path="groups" element={<UserManagementPage />} />
      <Route path="roles" element={<UserManagementPage />} />
      <Route path="permissions" element={<PermissionsPage />} />
      <Route path="directories" element={<UserManagementPage />} />
      <Route path="password-policy" element={<UserManagementPage />} />
      <Route path="sessions" element={<UserManagementPage />} />

      {/* Issue Administration */}
      <Route path="issue-types" element={<IssueTypesPage />} />
      <Route path="issue-type-schemes" element={<IssueTypesPage />} />
      <Route path="priorities" element={<PrioritiesPage />} />
      <Route path="resolutions" element={<IssueTypesPage />} />
      <Route path="statuses" element={<StatusesPage />} />
      <Route path="field-config" element={<IssueTypesPage />} />

      {/* Workflows & Screens */}
      <Route path="workflows" element={<WorkflowsPage />} />
      <Route path="workflow-schemes" element={<WorkflowsPage />} />
      <Route path="screens" element={<ScreensPage />} />
      <Route path="screen-schemes" element={<ScreensPage />} />

      {/* Projects */}
      <Route path="project-types" element={<ProjectAdministrationPage />} />
      <Route path="project-categories" element={<ProjectAdministrationPage />} />
      <Route path="archetypes" element={<ProjectAdministrationPage />} />

      {/* Schemes */}
      <Route path="permission-schemes" element={<PermissionsPage />} />
      <Route path="notification-schemes" element={<PermissionsPage />} />
      <Route path="security-schemes" element={<PermissionsPage />} />

      {/* Custom Fields */}
      <Route path="custom-fields" element={<CustomFieldsPage />} />
      <Route path="field-types" element={<CustomFieldsPage />} />
      <Route path="field-contexts" element={<CustomFieldsPage />} />

      {/* Data Center */}
      <Route path="cluster" element={<DataCenterPage />} />
      <Route path="cache" element={<DataCenterPage />} />
      <Route path="indexing" element={<DataCenterPage />} />
      <Route path="jobs" element={<DataCenterPage />} />
      <Route path="services" element={<DataCenterPage />} />

      {/* Advanced */}
      <Route path="automation" element={<AutomationPage />} />
      <Route path="mail" element={<SystemSettingsPage />} />
      <Route path="auditing" element={<AuditLogsPage />} />
      <Route path="api" element={<SystemSettingsPage />} />
      <Route path="webhooks" element={<WorkflowsPage />} />
      <Route path="oauth" element={<SystemSettingsPage />} />
      <Route path="links" element={<SystemSettingsPage />} />
      <Route path="dark-features" element={<SystemSettingsPage />} />
    </Routes>
  );
}