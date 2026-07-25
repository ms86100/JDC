import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './features/auth/context/AuthContext';
import { AppToastProvider } from './components/ui/AppToast';
import { WebSocketProvider } from './features/tests/components/WebSocketProvider';
import { ErrorBoundary } from './components/ErrorBoundary';
import AppLayout from './components/layout/AppLayout';
import LoginPage from './features/auth/pages/LoginPage';
import RegisterPage from './features/auth/pages/RegisterPage';
import DashboardPage from './features/dashboard/pages/DashboardPage';
import ProjectsPage from './features/projects/pages/ProjectsPage';
import ProjectDetailPage from './features/projects/pages/ProjectDetailPage';
import ProjectDcLayout from './features/projects/components/ProjectDcLayout';
import ProjectBacklogPage from './features/projects/pages/ProjectBacklogPage';
import ProjectActiveBoardPage from './features/projects/pages/ProjectActiveBoardPage';
import ProjectReleasesPage from './features/projects/pages/ProjectReleasesPage';
import ProjectReportsPage from './features/projects/pages/ProjectReportsPage';
import ProjectComponentsPage from './features/projects/pages/ProjectComponentsPage';
import ProjectSettingsDcLayout from './features/projects/pages/ProjectSettingsDcLayout';
import ProjectIssuesLayout from './features/projects/pages/ProjectIssuesLayout';
import CreateProjectWizard from './features/projects/components/CreateProjectWizard';
import IssuesLayout from './features/issues/pages/IssuesLayout';
import IssueNavigatorPlaceholder from './features/issues/pages/IssueNavigatorPlaceholder';
import IssueDetailPage from './features/issues/pages/IssueDetailPage';
import WorkflowManagementPage from './features/workflows/pages/WorkflowManagementPage';
import WorkflowDetailPage from './features/workflows/pages/WorkflowDetailPage';
import WorkflowDesignerPage from './features/workflows/pages/WorkflowDesignerPage';
import WorkflowTransitionScreensPage from './features/workflows/pages/WorkflowTransitionScreensPage';
import WorkflowAdminToolsPage from './features/workflows/pages/WorkflowAdminToolsPage';
import WorkflowOpenPage from './features/workflows/pages/WorkflowOpenPage';
import WorkflowAdminShell from './features/workflows/components/WorkflowAdminShell';
import WorkflowAdminHubPage from './features/workflows/pages/WorkflowAdminHubPage';
import WorkflowAdminSchemesPage from './features/workflows/pages/WorkflowAdminSchemesPage';
import WorkflowAdminScreensAdminPage from './features/workflows/pages/WorkflowAdminScreensAdminPage';
import WorkflowAdminDefinitionsPage from './features/workflows/pages/WorkflowAdminDefinitionsPage';
import WorkflowAdminAuditPage from './features/workflows/pages/WorkflowAdminAuditPage';
import WorkflowScriptsPage from './features/workflows/pages/WorkflowScriptsPage';
import EnhancedSearchPage from './features/search/pages/EnhancedSearchPage';
import NotificationsPage from './features/notifications/pages/NotificationsPage';
import SprintsPage from './features/sprints/pages/SprintsPage';
import BoardsPage from './features/boards/pages/BoardsPage';
import KanbanBoardPage from './features/boards/pages/KanbanBoardPage';
import AuditLogsPage from './features/audit/pages/AuditLogsPage';
import MigrationPage from './features/migration/pages/MigrationPage';
import AdminRoutes from './features/admin/routes/AdminRoutes';
import SystemSettingsPage from './features/admin/pages/SystemSettingsPage';
import UserManagementPage from './features/admin/pages/UserManagementPage';
import ProgramsPage from './features/plans/pages/ProgramsPage';
import ProgramDetailDcPage from './features/plans/pages/ProgramDetailDcPage';
import ProgramDetailPage from './features/plans/pages/ProgramDetailPage';
import PlanDetailPage from './features/plans/pages/PlanDetailPage';
import PlanSettingsPage from './features/plans/pages/PlanSettingsPage';
import ProgramSettingsPage from './features/plans/pages/ProgramSettingsPage';
import ManagePlansPage from './features/plans/pages/ManagePlansPage';
import CreateProgramPage from './features/plans/pages/CreateProgramPage';
import CreatePlanPage from './features/plans/pages/CreatePlanPage';
import TestManagementPage from './features/tests/pages/TestManagementPage';
import TestDetailPage from './features/tests/pages/TestDetailPage';
import TestCreationPage from './features/tests/pages/TestCreationPage';
import TestExecutionHistoryPage from './features/tests/pages/TestExecutionHistoryPage';
import DefectTrackingPage from './features/tests/pages/DefectTrackingPage';
import EvidenceGalleryPage from './features/tests/pages/EvidenceGalleryPage';
import SharedStepsPage from './features/tests/pages/SharedStepsPage';
import DatasetPage from './features/tests/pages/DatasetPage';
import FlakyTestsPage from './features/tests/pages/FlakyTestsPage';
import QuarantinePage from './features/tests/pages/QuarantinePage';
import EnvironmentMatrixPage from './features/tests/pages/EnvironmentMatrixPage';
import TestSettingsPage from './features/tests/pages/TestSettingsPage';
import ReportingDashboardPage from './features/tests/pages/ReportingDashboardPage';
import ImpactAnalysisPage from './features/tests/pages/ImpactAnalysisPage';
import WorkflowListPage from './features/tests/pages/WorkflowListPage';
import WorkflowBuilderPage from './features/tests/pages/WorkflowBuilderPage';
import TimelinePage from './features/tests/pages/TimelinePage';
import PreconditionPage from './features/tests/pages/PreconditionPage';
import CoveragePage from './features/tests/pages/CoveragePage';
import RequirementVersionPage from './features/tests/pages/RequirementVersionPage';
import TraceabilityPage from './features/tests/pages/TraceabilityPage';
import EpicsPage from './features/epics/pages/EpicsPage';
import EpicDetailPage from './features/epics/pages/EpicDetailPage';
import AiTestPage from './features/tests/pages/AiTestPage';
import CiCdWebhooksPage from './features/tests/pages/CiCdWebhooksPage';
import TestImportPage from './features/tests/pages/TestImportPage';
import TimeTrackingReports from './features/time-tracking/pages/TimeTrackingReports';
import IssueBatchPage from './features/issues/pages/IssueBatchPage';
import GraphQLExplorerPage from './features/developer/pages/GraphQLExplorerPage';
import TestScreenConfigHubPage from './features/tests/pages/TestScreenConfigHubPage';
import PluginManagementPage from './features/tests/pages/PluginManagementPage';
import FlakyTestDashboardPage from './features/tests/pages/FlakyTestDashboardPage';
import VvoListPage from './features/aircraft-design/pages/VvoListPage';
import VvoDetailPage from './features/aircraft-design/pages/VvoDetailPage';
import HlvvoListPage from './features/aircraft-design/pages/HlvvoListPage';
import TechEventListPage from './features/aircraft-design/pages/TechEventListPage';
import TechEventDetailPage from './features/aircraft-design/pages/TechEventDetailPage';
import ChangeCardPage from './features/aircraft-design/pages/ChangeCardPage';
import BaselineManagementPage from './features/aircraft-design/pages/BaselineManagementPage';
import VvDashboardPage from './features/aircraft-design/pages/VvDashboardPage';
import CampaignPage from './features/aircraft-design/pages/CampaignPage';
import MasterDataAdminPage from './features/aircraft-design/pages/MasterDataAdminPage';
import ArchitecturePage from './features/aircraft-design/pages/ArchitecturePage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
});

function ProjectSettingsRedirect() {
  const { projectId } = useParams<{ projectId: string }>();
  return <Navigate to={`/projects/${projectId}/settings/summary`} replace />;
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isLoading, user } = useAuth();
  if (isLoading) {
    // Show loading spinner while auth state is hydrating
    return (
      <div className="ab-loading-container">
        <div className="ab-spinner" />
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <AppToastProvider>
          <WebSocketProvider>
            <ErrorBoundary>
              <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
                <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route
                path="/"
                element={
                  <ProtectedRoute>
                    <AppLayout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<Navigate to="/dashboard" replace />} />
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="projects" element={<ProjectsPage />} />
                <Route path="projects/create" element={<CreateProjectWizard />} />
                <Route path="projects/:projectId" element={<ProjectDcLayout />}>
                  <Route index element={<ProjectDetailPage />} />
                  <Route path="backlog" element={<ProjectBacklogPage />} />
                  <Route path="board" element={<Navigate to="active" replace />} />
                  <Route path="board/active" element={<ProjectActiveBoardPage />} />
                  <Route path="sprints/active" element={<Navigate to="../board/active" replace />} />
                  <Route path="releases" element={<ProjectReleasesPage />} />
                  <Route path="reports" element={<ProjectReportsPage />} />
                  <Route path="components" element={<ProjectComponentsPage />} />
                  <Route path="issues" element={<ProjectIssuesLayout />}>
                    <Route index element={<IssueNavigatorPlaceholder />} />
                    <Route path=":issueId" element={<IssueDetailPage />} />
                  </Route>
                  <Route path="settings" element={<Navigate to="summary" replace />} />
                  <Route path="settings/:section" element={<ProjectSettingsDcLayout />} />
                </Route>
                <Route path="projects/:projectId/settings" element={<ProjectSettingsRedirect />} />
                <Route path="issues" element={<IssuesLayout />}>
                  <Route index element={<IssueNavigatorPlaceholder />} />
                  <Route path=":issueId" element={<IssueDetailPage />} />
                </Route>
                <Route path="issues/batch" element={<IssueBatchPage />} />
                <Route path="developer/graphql" element={<GraphQLExplorerPage />} />
                <Route path="epics" element={<EpicsPage />} />
                <Route path="epics/:epicId" element={<EpicDetailPage />} />
                <Route path="reports/time-tracking" element={<TimeTrackingReports />} />
                <Route path="kanban" element={<KanbanBoardPage />} />
                <Route path="sprints" element={<SprintsPage />} />
                <Route path="workflows" element={<WorkflowManagementPage />} />
                <Route path="workflows/open" element={<WorkflowOpenPage />} />
                <Route path="workflows/admin" element={<WorkflowAdminShell />}>
                  <Route index element={<WorkflowAdminHubPage />} />
                  <Route path="tools" element={<WorkflowAdminToolsPage />} />
                  <Route path="schemes" element={<WorkflowAdminSchemesPage />} />
                  <Route path="screens" element={<WorkflowAdminScreensAdminPage />} />
                  <Route path="definitions" element={<WorkflowAdminDefinitionsPage />} />
                  <Route path="audit" element={<WorkflowAdminAuditPage />} />
                  <Route path="scripts" element={<WorkflowScriptsPage />} />
                </Route>
                <Route path="workflows/admin-tools" element={<Navigate to="/workflows/admin/tools" replace />} />
                <Route path="workflows/screens" element={<WorkflowTransitionScreensPage />} />
                <Route path="workflows/:workflowId/designer" element={<WorkflowDesignerPage />} />
                <Route path="workflows/:workflowId" element={<WorkflowDetailPage />} />
                <Route path="search" element={<EnhancedSearchPage />} />
                <Route path="notifications" element={<NotificationsPage />} />
                <Route path="boards" element={<BoardsPage />} />
                <Route path="board/classic" element={<KanbanBoardPage />} />
                <Route path="audit" element={<AuditLogsPage />} />
                <Route path="migration" element={<MigrationPage />} />
                <Route path="programs" element={<ProgramsPage />} />
                <Route path="programs/create" element={<CreateProgramPage />} />
                <Route path="programs/:programId" element={<ProgramDetailDcPage />} />
                <Route path="programs/:programId/portfolio" element={<ProgramDetailPage />} />
                <Route path="programs/:programId/settings" element={<ProgramSettingsPage />} />
                <Route path="plans" element={<ManagePlansPage />} />
                <Route path="plans/create" element={<CreatePlanPage />} />
                <Route path="plans/:planId" element={<PlanDetailPage />} />
                <Route path="plans/:planId/settings" element={<PlanSettingsPage />} />
                <Route path="tests" element={<TestManagementPage />} />
                <Route path="tests/create" element={<TestCreationPage />} />
                <Route path="tests/create/:projectId" element={<TestCreationPage />} />
                <Route path="tests/screen-config" element={<TestScreenConfigHubPage />} />
                <Route path="tests/screen-config/:projectId" element={<TestScreenConfigHubPage />} />
                <Route path="tests/plugins" element={<PluginManagementPage />} />
                <Route path="tests/defects" element={<DefectTrackingPage />} />
                <Route path="tests/evidence" element={<EvidenceGalleryPage />} />
                <Route path="tests/shared-steps" element={<SharedStepsPage />} />
                <Route path="tests/shared-steps/:projectId" element={<SharedStepsPage />} />
                <Route path="tests/datasets" element={<DatasetPage />} />
                <Route path="tests/datasets/:projectId" element={<DatasetPage />} />
                <Route path="tests/flaky" element={<FlakyTestsPage />} />
                <Route path="tests/flaky/:projectId" element={<FlakyTestsPage />} />
                <Route path="tests/flaky-dashboard" element={<FlakyTestDashboardPage />} />
                <Route path="tests/flaky-dashboard/:projectId" element={<FlakyTestDashboardPage />} />
                <Route path="tests/quarantine" element={<QuarantinePage />} />
                <Route path="tests/quarantine/:projectId" element={<QuarantinePage />} />
                <Route path="tests/environment-matrix" element={<EnvironmentMatrixPage />} />
                <Route path="tests/environment-matrix/:projectId" element={<EnvironmentMatrixPage />} />
                <Route path="tests/settings" element={<TestSettingsPage />} />
                <Route path="tests/settings/:projectId" element={<TestSettingsPage />} />
                <Route path="tests/reporting" element={<ReportingDashboardPage />} />
                <Route path="tests/reporting/:projectId" element={<ReportingDashboardPage />} />
                <Route path="tests/impact" element={<ImpactAnalysisPage />} />
                <Route path="tests/impact/:projectId" element={<ImpactAnalysisPage />} />
                <Route path="tests/workflows" element={<WorkflowListPage />} />
                <Route path="tests/workflows/:projectId" element={<WorkflowListPage />} />
                <Route path="tests/workflows/builder" element={<WorkflowBuilderPage />} />
                <Route path="tests/workflows/builder/:workflowId" element={<WorkflowBuilderPage />} />
                <Route path="tests/timeline" element={<TimelinePage />} />
                <Route path="tests/timeline/:projectId" element={<TimelinePage />} />
                <Route path="tests/preconditions" element={<PreconditionPage />} />
                <Route path="tests/preconditions/:projectId" element={<PreconditionPage />} />
                <Route path="tests/coverage" element={<CoveragePage />} />
                <Route path="tests/coverage/:projectId" element={<CoveragePage />} />
                <Route path="tests/requirement-versions" element={<RequirementVersionPage />} />
                <Route path="tests/requirement-versions/:projectId" element={<RequirementVersionPage />} />
                <Route path="tests/traceability" element={<TraceabilityPage />} />
                <Route path="tests/traceability/:projectId" element={<TraceabilityPage />} />
                <Route path="tests/ai" element={<AiTestPage />} />
                <Route path="tests/webhooks" element={<CiCdWebhooksPage />} />
                <Route path="tests/import" element={<TestImportPage />} />
                <Route path="tests/import/:projectId" element={<TestImportPage />} />
                <Route path="tests/:testId/history" element={<TestExecutionHistoryPage />} />
                <Route path="tests/:testId/execute" element={<TestDetailPage />} />
                <Route path="tests/:testId" element={<TestDetailPage />} />
                <Route path="tests/:projectId" element={<TestManagementPage />} />
                {/* Aircraft Design System */}
                <Route path="aircraft-design/vvos" element={<VvoListPage />} />
                <Route path="aircraft-design/vvos/:id" element={<VvoDetailPage />} />
                <Route path="aircraft-design/hlvvos" element={<HlvvoListPage />} />
                <Route path="aircraft-design/tech-events" element={<TechEventListPage />} />
                <Route path="aircraft-design/tech-events/:id" element={<TechEventDetailPage />} />
                <Route path="aircraft-design/change-cards" element={<ChangeCardPage />} />
                <Route path="aircraft-design/baselines" element={<BaselineManagementPage />} />
                <Route path="aircraft-design/dashboard" element={<VvDashboardPage />} />
                <Route path="aircraft-design/campaigns" element={<CampaignPage />} />
                <Route path="aircraft-design/master-data" element={<MasterDataAdminPage />} />
                <Route path="aircraft-design/architecture" element={<ArchitecturePage />} />
              </Route>
              <Route
                path="/admin/*"
                element={
                  <ProtectedRoute>
                    <AdminRoutes />
                  </ProtectedRoute>
                }
              />
                </Routes>
              </BrowserRouter>
            </ErrorBoundary>
          </WebSocketProvider>
        </AppToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;