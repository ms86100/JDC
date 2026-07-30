import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import AviSysAdminLayout from '../components/AviSysAdminLayout';
import UserManagementPage from '../pages/UserManagementPage';
import AviSysUserBrowser from '../pages/AviSysUserBrowser';
import AviSysCreateUser from '../pages/AviSysCreateUser';
import AviSysGroupsBrowser from '../pages/AviSysGroupsBrowser';
import AviSysViewGroup from '../pages/AviSysViewGroup';
import BulkCreateWizard from '../pages/BulkCreateWizard';
import IssueTypesPage from '../pages/IssueTypesPage';
import IssueTypeSchemesPage from '../pages/IssueTypeSchemesPage';
import PrioritiesPage from '../pages/PrioritiesPage';
import StatusesPage from '../pages/StatusesPage';
import ResolutionsPage from '../pages/ResolutionsPage';
import FieldConfigurationPage from '../pages/FieldConfigurationPage';
import WorkflowsPage from '../pages/WorkflowsPage';
import WorkflowDesignerPage from '../../workflows/pages/WorkflowDesignerPage';
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
import RolesPage from '../pages/RolesPage';
import SessionsPage from '../pages/SessionsPage';
import PasswordPolicyPage from '../pages/PasswordPolicyPage';
import ApplicationLinksPage from '../pages/ApplicationLinksPage';
import SamlConfigPage from '../pages/SamlConfigPage';
import EditUserPage from '../pages/EditUserPage';
import GroupMembersPage from '../pages/GroupMembersPage';
import SystemConfigPage from '../pages/SystemConfigPage';
import BoardTypesPage from '../pages/BoardTypesPage';
import QuickFilterPresetsPage from '../pages/QuickFilterPresetsPage';
import LinkTypesPage from '../pages/LinkTypesPage';
import NotificationEventsPage from '../pages/NotificationEventsPage';

export default function AdminRoutes() {
  return (
    <AviSysAdminLayout>
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
      <Route path="system/config" element={<SystemConfigPage />} />
      <Route path="reports" element={<ReportsPage />} />
      <Route path="insights" element={<InsightsPage />} />

      {/* User Management - Systems and Avionics Style */}
      <Route path="users" element={<AviSysUserBrowser />} />
      <Route path="users/create" element={<AviSysCreateUser />} />
      <Route path="users/edit/:userId" element={<EditUserPage />} />
      <Route path="groups" element={<AviSysGroupsBrowser />} />
      <Route path="groups/view" element={<AviSysViewGroup />} />
      <Route path="groups/members/:groupId" element={<GroupMembersPage />} />

      {/* Bulk Operations */}
      <Route path="bulk-create/*" element={<BulkCreateWizard />} />
      <Route path="bulk-create" element={<BulkCreateWizard />} />

      {/* Legacy User Management */}
      <Route path="user-management" element={<UserManagementPage />} />
      <Route path="roles" element={<RolesPage />} />
      <Route path="permissions" element={<PermissionsPage />} />
      <Route path="directories" element={<UserManagementPage />} />
      <Route path="password-policy" element={<PasswordPolicyPage />} />
      <Route path="sessions" element={<SessionsPage />} />

      {/* Issue Administration */}
      <Route path="issue-types" element={<IssueTypesPage />} />
      <Route path="issue-type-schemes" element={<IssueTypeSchemesPage />} />
      <Route path="priorities" element={<PrioritiesPage />} />
      <Route path="resolutions" element={<ResolutionsPage />} />
      <Route path="statuses" element={<StatusesPage />} />
      <Route path="field-config" element={<FieldConfigurationPage />} />
      <Route path="link-types" element={<LinkTypesPage />} />

      {/* Workflows & Screens */}
      <Route path="workflows" element={<WorkflowsPage />} />
      <Route path="workflows/:workflowId/designer" element={<WorkflowDesignerPage />} />
      <Route path="workflow-schemes" element={<WorkflowsPage />} />
      <Route path="screens" element={<ScreensPage />} />
      <Route path="screen-schemes" element={<ScreensPage />} />

      {/* Board Types */}
      <Route path="board-types" element={<BoardTypesPage />} />

      {/* Quick Filter Presets */}
      <Route path="quick-filters" element={<QuickFilterPresetsPage />} />

      {/* Projects */}
      <Route path="project-types" element={<ProjectAdministrationPage />} />
      <Route path="project-categories" element={<ProjectAdministrationPage />} />
      <Route path="archetypes" element={<ProjectAdministrationPage />} />

      {/* Schemes */}
      <Route path="permission-schemes" element={<PermissionsPage />} />
      <Route path="notification-schemes" element={<PermissionsPage />} />
      <Route path="notification-events" element={<NotificationEventsPage />} />
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
      <Route path="api/graphql" element={<Navigate to="/developer/graphql" replace />} />
      <Route path="webhooks" element={<Navigate to="/tests/webhooks" replace />} />
      <Route path="application-links" element={<ApplicationLinksPage />} />
      <Route path="links" element={<Navigate to="/admin/application-links" replace />} />
      <Route path="system/sso" element={<SamlConfigPage />} />
      <Route path="sso" element={<Navigate to="/admin/system/sso" replace />} />

      {/* Test Management (admin menu → workspace routes) */}
      <Route path="tests" element={<Navigate to="/tests" replace />} />
      <Route path="tests/sets" element={<Navigate to="/tests?view=sets" replace />} />
      <Route path="tests/plans" element={<Navigate to="/tests?view=plans" replace />} />
      <Route path="tests/environments" element={<Navigate to="/tests/environment-matrix" replace />} />
      <Route path="tests/import" element={<Navigate to="/tests/import" replace />} />
      <Route path="tests/reports" element={<Navigate to="/tests/reporting" replace />} />
      <Route path="tests/webhooks" element={<Navigate to="/tests/webhooks" replace />} />
      <Route path="tests/ai" element={<Navigate to="/tests/ai" replace />} />
      <Route path="oauth" element={<SystemSettingsPage />} />
      <Route path="dark-features" element={<SystemSettingsPage />} />
      </Routes>
    </AviSysAdminLayout>
  );
}
