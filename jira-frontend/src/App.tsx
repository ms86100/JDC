import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './features/auth/context/AuthContext';
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
import WorkflowPage from './features/workflows/pages/WorkflowPage';
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
        <BrowserRouter>
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
              <Route path="workflows" element={<WorkflowPage />} />
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
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;