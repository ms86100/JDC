import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './features/auth/context/AuthContext';
import { WebSocketProvider } from './features/tests/components/WebSocketProvider';
import AppLayout from './components/layout/AppLayout';
import LoginPage from './features/auth/pages/LoginPage';
import RegisterPage from './features/auth/pages/RegisterPage';
import DashboardPage from './features/dashboard/pages/DashboardPage';
import ProjectsPage from './features/projects/pages/ProjectsPage';
import ProjectDetailPage from './features/projects/pages/ProjectDetailPage';
import ProjectSettingsPage from './features/projects/pages/ProjectSettingsPage';
import CreateProjectWizard from './features/projects/components/CreateProjectWizard';
import IssuesPage from './features/issues/pages/IssuesPage';
import IssueDetailPage from './features/issues/pages/IssueDetailPage';
import WorkflowManagementPage from './features/workflows/pages/WorkflowManagementPage';
import WorkflowDetailPage from './features/workflows/pages/WorkflowDetailPage';
import WorkflowDesignerPage from './features/workflows/pages/WorkflowDesignerPage';
import SearchPage from './features/search/pages/SearchPage';
import NotificationsPage from './features/notifications/pages/NotificationsPage';
import SprintsPage from './features/sprints/pages/SprintsPage';
import KanbanBoard from './features/issues/components/KanbanBoard';
import BoardsPage from './features/boards/pages/BoardsPage';
import KanbanBoardPage from './features/boards/pages/KanbanBoardPage';
import AuditLogsPage from './features/audit/pages/AuditLogsPage';
import MigrationPage from './features/migration/pages/MigrationPage';
import AdminRoutes from './features/admin/routes/AdminRoutes';
import SystemSettingsPage from './features/admin/pages/SystemSettingsPage';
import UserManagementPage from './features/admin/pages/UserManagementPage';
import ProgramsPage from './features/plans/pages/ProgramsPage';
import ProgramDetailPage from './features/plans/pages/ProgramDetailPage';
import PlanDetailPage from './features/plans/pages/PlanDetailPage';
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

const queryClient = new QueryClient();

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('accessToken');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <WebSocketProvider showStatusIndicator={true}>
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
                <Route path="projects/:projectId" element={<ProjectDetailPage />} />
                <Route path="projects/:projectId/settings" element={<ProjectSettingsPage />} />
                <Route path="issues" element={<IssuesPage />} />
                <Route path="issues/:issueId" element={<IssueDetailPage />} />
                <Route path="kanban" element={<KanbanBoard />} />
                <Route path="sprints" element={<SprintsPage />} />
                <Route path="workflows" element={<WorkflowManagementPage />} />
                <Route path="workflows/:workflowId" element={<WorkflowDetailPage />} />
                <Route path="workflows/:workflowId/designer" element={<WorkflowDesignerPage />} />
                <Route path="search" element={<SearchPage />} />
                <Route path="notifications" element={<NotificationsPage />} />
                <Route path="boards" element={<BoardsPage />} />
                <Route path="board/classic" element={<KanbanBoardPage />} />
                <Route path="audit" element={<AuditLogsPage />} />
                <Route path="migration" element={<MigrationPage />} />
                <Route path="programs" element={<ProgramsPage />} />
                <Route path="programs/create" element={<CreateProgramPage />} />
                <Route path="programs/:programId" element={<ProgramDetailPage />} />
                <Route path="plans" element={<ManagePlansPage />} />
                <Route path="plans/create" element={<CreatePlanPage />} />
                <Route path="plans/:planId" element={<PlanDetailPage />} />
                <Route path="tests" element={<TestManagementPage />} />
                <Route path="tests/:projectId" element={<TestManagementPage />} />
                <Route path="tests/:testId" element={<TestDetailPage />} />
                <Route path="tests/create" element={<TestCreationPage />} />
                <Route path="tests/create/:projectId" element={<TestCreationPage />} />
                <Route path="tests/:testId/history" element={<TestExecutionHistoryPage />} />
                <Route path="tests/defects" element={<DefectTrackingPage />} />
                <Route path="tests/evidence" element={<EvidenceGalleryPage />} />
                <Route path="tests/shared-steps" element={<SharedStepsPage />} />
                <Route path="tests/shared-steps/:projectId" element={<SharedStepsPage />} />
                <Route path="tests/datasets" element={<DatasetPage />} />
                <Route path="tests/datasets/:projectId" element={<DatasetPage />} />
                <Route path="tests/flaky" element={<FlakyTestsPage />} />
                <Route path="tests/flaky/:projectId" element={<FlakyTestsPage />} />
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
        </WebSocketProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;