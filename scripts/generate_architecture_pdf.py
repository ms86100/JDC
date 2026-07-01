#!/usr/bin/env python3
"""Generate Jira Platform Architecture PDF."""

from fpdf import FPDF
from pathlib import Path
from datetime import date

OUTPUT = Path(__file__).resolve().parent.parent / "docs" / "JIRA_PLATFORM_ARCHITECTURE.pdf"


class ArchPDF(FPDF):
    def header(self):
        if self.page_no() > 1:
            self.set_font("Helvetica", "I", 8)
            self.set_text_color(100, 100, 100)
            self.cell(0, 8, "Jira Platform - Architecture Reference", align="L")
            self.cell(0, 8, f"Page {self.page_no()}", align="R", new_x="LMARGIN", new_y="NEXT")
            self.ln(2)

    def footer(self):
        self.set_y(-12)
        self.set_font("Helvetica", "I", 8)
        self.set_text_color(120, 120, 120)
        self.cell(0, 10, f"Generated {date.today().isoformat()}", align="C")

    def title_page(self):
        self.add_page()
        self.ln(40)
        self.set_font("Helvetica", "B", 28)
        self.set_text_color(30, 60, 120)
        self.multi_cell(0, 14, "Jira Platform\nArchitecture Reference", align="C")
        self.ln(10)
        self.set_font("Helvetica", "", 14)
        self.set_text_color(60, 60, 60)
        self.multi_cell(
            0, 8,
            "Per-Service API Map\nIssue Create & Workflow Transition Sequences\nGateway Route Gap Analysis",
            align="C",
        )
        self.ln(20)
        self.set_font("Helvetica", "", 11)
        self.cell(0, 8, f"Document date: {date.today().strftime('%B %d, %Y')}", align="C", new_x="LMARGIN", new_y="NEXT")
        self.cell(0, 8, "Repository: jira-platform", align="C")

    def _content_width(self):
        return self.w - self.l_margin - self.r_margin

    def h1(self, text):
        self.ln(4)
        self.set_font("Helvetica", "B", 16)
        self.set_text_color(30, 60, 120)
        self.multi_cell(self._content_width(), 10, text)
        self.ln(2)

    def h2(self, text):
        self.ln(2)
        self.set_font("Helvetica", "B", 12)
        self.set_text_color(50, 50, 50)
        self.multi_cell(self._content_width(), 8, text)
        self.ln(1)

    def h3(self, text):
        self.set_font("Helvetica", "B", 10)
        self.set_text_color(70, 70, 70)
        self.multi_cell(self._content_width(), 7, text)

    def body(self, text):
        self.set_font("Helvetica", "", 10)
        self.set_text_color(30, 30, 30)
        w = self.w - self.l_margin - self.r_margin
        self.multi_cell(w, 5, text)
        self.ln(1)

    def bullet(self, text):
        self.set_font("Helvetica", "", 10)
        w = self.w - self.l_margin - self.r_margin
        self.multi_cell(w, 5, f"  - {text}")

    def table(self, headers, rows, col_widths=None):
        self.set_font("Helvetica", "B", 8)
        self.set_fill_color(230, 235, 245)
        w = col_widths or [int(190 / len(headers))] * len(headers)
        for i, h in enumerate(headers):
            self.cell(w[i], 7, h[:40], border=1, fill=True)
        self.ln()
        self.set_font("Helvetica", "", 7)
        self.set_fill_color(255, 255, 255)
        for row in rows:
            if self.get_y() > 270:
                self.add_page()
            for i, cell in enumerate(row):
                self.cell(w[i], 6, str(cell)[:50], border=1)
            self.ln()

    def code_block(self, lines):
        self.set_font("Courier", "", 8)
        self.set_fill_color(245, 245, 245)
        w = self._content_width()
        for line in lines:
            if self.get_y() > 275:
                self.add_page()
            self.cell(w, 4, line[:95], fill=True, new_x="LMARGIN", new_y="NEXT")
        self.ln(2)


def build():
    pdf = ArchPDF()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.title_page()

    # --- Section 1: Overview ---
    pdf.add_page()
    pdf.h1("1. Platform Overview")
    pdf.body(
        "The Jira Platform is a microservices system: React SPA (port 3000) -> Spring Cloud Gateway "
        "(8080) -> 14 domain services (8081-8094) -> PostgreSQL (per-service databases). "
        "JWT authentication is enforced at the gateway; identity is forwarded via X-User-Id, "
        "X-Username, and X-User-Roles headers."
    )
    pdf.h2("1.1 Service Ports")
    pdf.table(
        ["Service", "Port", "DB"],
        [
            ["jira-gateway", "8080", "-"],
            ["jira-auth-service", "8081", "auth_db"],
            ["jira-user-service", "8082", "user_db"],
            ["jira-project-service", "8083", "project_db"],
            ["jira-issue-service", "8084", "issue_db"],
            ["jira-workflow-service", "8085", "workflow_db"],
            ["jira-comment-service", "8086", "comment_db"],
            ["jira-notification-service", "8087", "notification_db"],
            ["jira-search-service", "8088", "search_db"],
            ["jira-audit-service", "8089", "audit_db"],
            ["jira-attachment-service", "8090", "attachment_db"],
            ["jira-sprint-service", "8091", "sprint_db"],
            ["jira-plan-service", "8092", "plan_db"],
            ["jira-admin-service", "8093", "admin DB"],
            ["jira-migration-service", "8094", "migration_db"],
            ["jira-frontend", "3000", "-"],
        ],
        [45, 20, 35],
    )

    # --- Section 2: Gateway routes ---
    pdf.add_page()
    pdf.h1("2. Current Gateway Routes (local profile)")
    pdf.body("Defined in jira-gateway/src/main/resources/application-local.yml")
    pdf.table(
        ["Path Predicate", "Target", "Filters"],
        [
            ["/api/auth/**", ":8081", "StripPrefix=1"],
            ["/api/users/**", ":8082", "-"],
            ["/api/projects/**", ":8083", "-"],
            ["/api/issues/**", ":8084", "-"],
            ["/api/components/**", ":8084", "-"],
            ["/api/versions/**", ":8084", "-"],
            ["/api/workflows/**", ":8085", "-"],
            ["/api/comments/**", ":8086", "StripPrefix=1"],
            ["/api/notifications/**", ":8087", "-"],
            ["/api/search/**", ":8088", "-"],
            ["/api/audit/**", ":8089", "-"],
            ["/api/attachments/**", ":8090", "-"],
            ["/api/sprints/**", ":8091", "-"],
            ["/api/plans/**", ":8092", "-"],
        ],
        [55, 25, 30],
    )

    # --- Section 3: Per-service API maps ---
    pdf.add_page()
    pdf.h1("3. Per-Service API Map")

    services = [
        ("3.1 Auth Service (:8081)", "Base: /auth (gateway: /api/auth -> StripPrefix)", [
            ("POST", "/auth/register", "Register user"),
            ("POST", "/auth/login", "Login, returns JWT"),
            ("POST", "/auth/refresh", "Refresh access token"),
            ("GET", "/auth/me", "Current user profile"),
        ]),
        ("3.2 User Service (:8082)", "Base: /api/users", [
            ("GET", "/api/users", "List/search users"),
            ("GET", "/api/users/{id}", "Get user"),
            ("POST", "/api/users", "Create user"),
            ("PUT", "/api/users/{id}", "Update user"),
            ("DELETE", "/api/users/{id}", "Delete user"),
        ]),
        ("3.3 Project Service (:8083)", "Base: /api/projects", [
            ("GET/POST", "/api/projects", "List / create projects"),
            ("GET/PUT/DELETE", "/api/projects/{id}", "CRUD project"),
            ("POST", "/api/projects/{id}/archive", "Archive project"),
            ("GET", "/api/projects/{id}/members", "Project members"),
            ("POST", "/api/projects/{id}/members", "Add member"),
        ]),
        ("3.4 Issue Service (:8084)", "Base: /api/issues (+ components, versions)", [
            ("POST", "/api/issues", "Create issue (requires X-User-Id)"),
            ("GET", "/api/issues", "Search/list with filters"),
            ("GET", "/api/issues/{id}", "Get issue"),
            ("PUT", "/api/issues/{id}", "Update issue"),
            ("PATCH", "/api/issues/{id}/status", "Status transition (validates workflow)"),
            ("DELETE", "/api/issues/{id}", "Delete issue"),
            ("GET", "/api/issues/search", "JQL search (in-issue service)"),
            ("*", "/api/issues/{id}/worklogs", "Worklog CRUD"),
            ("*", "/api/issues/{id}/labels", "Label CRUD"),
            ("*", "/api/issues/{id}/links", "Issue links"),
            ("*", "/api/issues/{id}/history", "Change history"),
            ("*", "/api/components", "Component CRUD"),
            ("*", "/api/versions", "Version CRUD"),
        ]),
        ("3.5 Workflow Service (:8085)", "Base: /api/workflows + /api/admin/workflows", [
            ("POST", "/api/workflows", "Create workflow"),
            ("GET", "/api/workflows/project/{projectId}", "Project workflows"),
            ("GET", "/api/workflows/project/{pid}/validate-transition", "Validate from/to status"),
            ("POST", "/api/workflows/issues/{issueId}/execute", "Execute transition"),
            ("GET", "/api/workflows/{wfId}/allowed-transitions", "Allowed transitions"),
            ("*", "/api/admin/workflows/**", "Admin: schemes, screens, versions, audit"),
        ]),
        ("3.6 Comment Service (:8086)", "Gateway strips /api prefix", [
            ("POST", "/comments", "Create (frontend: /api/comments)"),
            ("GET", "/comments/issue/{issueId}", "List by issue"),
            ("PUT/DELETE", "/comments/{id}", "Update/delete"),
        ]),
        ("3.7 Notification Service (:8087)", "Base: /api/notifications", [
            ("GET", "/api/notifications", "List notifications"),
            ("PUT", "/api/notifications/{id}/read", "Mark read"),
        ]),
        ("3.8 Search Service (:8088)", "Base: /api/jql (NOT /api/search in controller)", [
            ("POST", "/api/jql/search", "JQL search execution"),
            ("GET", "/api/jql/validate", "Validate JQL syntax"),
        ]),
        ("3.9 Audit Service (:8089)", "Base: /api/audit", [
            ("GET", "/api/audit/logs", "Search audit logs"),
            ("GET", "/api/audit/logs/{type}/{id}", "Entity audit trail"),
            ("POST", "/api/audit/logs", "Create audit entry"),
        ]),
        ("3.10 Attachment Service (:8090)", "Base: /api/attachments", [
            ("POST", "/api/attachments", "Upload (multipart)"),
            ("GET", "/api/attachments/issue/{issueId}", "List by issue"),
            ("GET", "/api/attachments/{id}/download", "Download file"),
            ("DELETE", "/api/attachments/{id}", "Delete"),
        ]),
        ("3.11 Sprint Service (:8091)", "Agile boards, sprints, dashboards", [
            ("*", "/api/sprints/**", "Sprint CRUD, start/complete"),
            ("*", "/api/boards/**", "Agile board config & issues"),
            ("*", "/api/dashboards/**", "Dashboard & gadgets"),
            ("*", "/api/filters/**", "Saved JQL filters"),
            ("*", "/api/bulk-operations/**", "Bulk issue operations"),
        ]),
        ("3.12 Plan Service (:8092)", "Programs, plans, Advanced Roadmaps", [
            ("*", "/api/plans/**", "Plans, backlog, teams, releases"),
            ("*", "/api/plans/programs/**", "Program management"),
            ("*", "/api/plans/boards/**", "Plan board config & sprints"),
            ("*", "/api/plans/sprints/**", "Plan-scoped sprints"),
            ("*", "/api/plans/working-days/**", "Working days & holidays"),
        ]),
        ("3.13 Admin Service (:8093)", "Base: /api/admin", [
            ("*", "/api/admin/settings", "System settings"),
            ("*", "/api/admin/users/**", "User administration"),
            ("*", "/api/admin/issues/**", "Issue types, priorities, schemes"),
            ("*", "/api/admin/projects/**", "Project admin"),
            ("*", "/api/admin/datacenter/**", "DC cluster settings"),
            ("*", "/api/admin/audit/**", "Admin audit views"),
        ]),
        ("3.14 Migration Service (:8094)", "Base: /api/migration", [
            ("POST", "/api/migration/import/csv", "CSV import"),
            ("POST", "/api/migration/import/jira-dc", "Jira DC import"),
            ("GET", "/api/migration/jobs/{id}", "Job status"),
            ("*", "/api/migration/wizard/**", "Import wizard steps"),
            ("*", "/api/sse/**", "Server-sent events for progress"),
        ]),
    ]

    for title, base, endpoints in services:
        if pdf.get_y() > 240:
            pdf.add_page()
        pdf.h2(title)
        pdf.body(base)
        pdf.table(["Method", "Path", "Description"], endpoints, [18, 75, 97])

    # --- Section 4: Sequence diagrams ---
    pdf.add_page()
    pdf.h1("4. Sequence: Issue Create")
    pdf.body("Flow when user creates an issue from the React UI via issueApi.create().")
    pdf.code_block([
        "Actor: User (Browser)",
        "  |",
        "  | POST /api/issues  { projectId, title, issueTypeId, ... }",
        "  | Authorization: Bearer <JWT>",
        "  v",
        "jira-gateway (:8080)",
        "  |-- Validate JWT, extract X-User-Id, X-Username, X-User-Roles",
        "  |-- Route to issue-service (:8084)",
        "  v",
        "IssueController.createIssue(request, X-User-Id)",
        "  v",
        "IssueService.createIssue()",
        "  |-- GET project-service: resolve project key for issue key (e.g. PROJ-42)",
        "  |       RestTemplate -> http://localhost:8083/api/projects/{projectId}",
        "  |-- Load IssueType, IssueStatus (default Open), IssuePriority from issue_db",
        "  |-- generateIssueKey(projectKey)",
        "  |-- issueRepository.save(issue)",
        "  v",
        "201 Created IssueResponse { id, issueKey, status, ... }",
        "",
        "Optional downstream (not always synchronous):",
        "  - search-service index (if wired)",
        "  - audit-service log entry",
        "  - notification-service (assignee notify)",
    ])

    pdf.add_page()
    pdf.h1("5. Sequence: Workflow Status Transition")
    pdf.body("Two paths exist: (A) simple status patch on issue service, (B) full workflow execute.")
    pdf.h3("Path A - PATCH /api/issues/{id}/status (used by issueApi.transitionStatus)")
    pdf.code_block([
        "Browser -> Gateway -> IssueController.updateIssueStatus(id, {statusId}, projectId)",
        "  IssueService.updateIssueStatus():",
        "    1. Load issue, get current status",
        "    2. validateTransition(projectId, fromStatus, toStatus)",
        "         GET workflow-service:",
        "           /api/workflows/project/{projectId}/validate-transition",
        "           ?fromStatus=X&toStatus=Y",
        "    3. If invalid -> 400 InvalidTransitionException",
        "    4. Update issue.status, save, return IssueResponse",
    ])
    pdf.h3("Path B - POST /api/workflows/issues/{issueId}/execute (workflow engine)")
    pdf.code_block([
        "Browser -> Gateway -> WorkflowController.executeTransition(issueId, transitionId)",
        "  WorkflowService.executeTransition():",
        "    1. Load transition, workflow, issue (may call issue-service)",
        "    2. Evaluate conditions (role, field value, etc.)",
        "    3. Run validators",
        "    4. Apply post-functions (update fields, assign, etc.)",
        "    5. Update issue status to transition target",
        "    6. Return TransitionExecutionResponse",
    ])
    pdf.body(
        "Note: Frontend issueApi uses Path A. Admin workflow designer uses /api/admin/workflows "
        "on workflow-service. WorkflowsPage.tsx incorrectly calls port 8082 (user service)."
    )

    # --- Section 6: Gateway gap analysis ---
    pdf.add_page()
    pdf.h1("6. Gateway Route Gap Analysis")
    pdf.body(
        "Comparison of frontend API calls (jira-frontend/src/api/* and hooks) against "
        "gateway routes in application-local.yml. Status: MISSING = no gateway route."
    )

    gaps = [
        ("CRITICAL", "/api/admin/**", "8093", "adminApi, useAdminApi, admin pages"),
        ("CRITICAL", "/api/admin/workflows/**", "8085", "WorkflowsPage (also wrong port 8082)"),
        ("CRITICAL", "/api/migration/**", "8094", "migrationApi, MigrationPage"),
        ("CRITICAL", "/api/jql/**", "8088", "serviceApi.searchApi.jqlSearch"),
        ("HIGH", "/api/boards/**", "8091", "boardApi, KanbanBoard, BoardsPage"),
        ("HIGH", "/api/dashboards/**", "8091", "dashboardApi, DashboardPage"),
        ("HIGH", "/api/filters/**", "8091", "filterApi, SavedFilters"),
        ("HIGH", "/api/bulk-operations/**", "8091", "bulkApi"),
        ("MEDIUM", "/api/custom-fields/**", "8084?", "issueApi custom fields - verify backend"),
        ("MEDIUM", "/api/resolutions/**", "8084?", "issueApi resolutions - verify backend"),
        ("MEDIUM", "/search (no /api prefix)", "8088", "serviceApi.searchApi.search"),
        ("LOW", "/api/ws/**", "8094", "migration WebSocket"),
        ("LOW", "/api/sse/**", "8094", "migration SSE progress"),
        ("CONFIG", "/api/comments/**", "8086", "StripPrefix=1 may break path /api/comments/issue/{id}"),
        ("CONFIG", "/api/search/**", "8088", "Routed but controller uses /api/jql not /api/search"),
    ]

    pdf.table(["Priority", "Frontend Path", "Service Port", "Used By"], gaps, [22, 45, 22, 50])

    pdf.h2("6.1 Recommended Gateway Routes to Add")
    pdf.code_block([
        "- id: admin-service",
        "  uri: http://localhost:8093",
        "  predicates: [Path=/api/admin/**]",
        "  # Exclude /api/admin/workflows if routing to workflow-service",
        "",
        "- id: workflow-admin-service",
        "  uri: http://localhost:8085",
        "  predicates: [Path=/api/admin/workflows/**]",
        "",
        "- id: migration-service",
        "  uri: http://localhost:8094",
        "  predicates: [Path=/api/migration/**, /api/sse/**, /api/ws/**]",
        "",
        "- id: jql-service",
        "  uri: http://localhost:8088",
        "  predicates: [Path=/api/jql/**]",
        "",
        "- id: boards-service",
        "  uri: http://localhost:8091",
        "  predicates:",
        "    - Path=/api/boards/**, /api/dashboards/**, /api/filters/**, /api/bulk-operations/**",
    ])

    pdf.h2("6.2 Frontend Fixes Required")
    pdf.bullet("WorkflowsPage.tsx: change API_BASE from localhost:8082 to gateway /api/admin/workflows")
    pdf.bullet("serviceApi searchApi: align /search and /api/jql paths with gateway routes")
    pdf.bullet("RegisterPage.tsx: use apiClient instead of hardcoded fetch to :8080")
    pdf.bullet("Add jira-admin-service to root pom.xml <modules> for Maven builds")

    pdf.h2("6.3 Comment Service StripPrefix Issue")
    pdf.body(
        "Gateway strips first path segment (/api) so /api/comments becomes /comments at comment-service. "
        "CommentController uses /comments paths - this works for POST /api/comments. "
        "GET /api/comments/issue/{id} becomes /comments/issue/{id} - verify controller mapping matches."
    )

    # --- Section 7: Inter-service deps ---
    pdf.add_page()
    pdf.h1("7. Inter-Service Communication")
    pdf.table(
        ["Caller", "Callee", "Purpose"],
        [
            ["issue-service", "project-service", "Project key for issue key generation"],
            ["issue-service", "workflow-service", "Validate status transitions"],
            ["search-service", "issue-service", "JQL result hydration"],
            ["notification-service", "user-service", "Recipient lookup"],
            ["migration-service", "all services", "Import orchestration via RestTemplate clients"],
            ["sprint/board-service", "issue-service", "Board issue data"],
            ["workflow-service", "issue-service", "Transition execution updates"],
        ],
        [40, 40, 50],
    )

    pdf.h1("8. Frontend Feature -> API Mapping")
    pdf.table(
        ["Feature", "Primary API Modules", "Gateway Prefix"],
        [
            ["Auth", "authApi", "/api/auth/**"],
            ["Projects", "projectApi, issueApi.projectApi", "/api/projects/**"],
            ["Issues", "issueApi, labelApi, worklogApi, changeHistoryApi", "/api/issues/**"],
            ["Boards/Kanban", "boardApi", "/api/boards/** MISSING"],
            ["Sprints", "sprintApi", "/api/sprints/**"],
            ["Plans", "planApi, useSprint, useLexoRank", "/api/plans/**"],
            ["Search", "serviceApi.searchApi", "/api/jql/** MISSING"],
            ["Admin", "adminApi, useAdminApi", "/api/admin/** MISSING"],
            ["Migration", "serviceApi.migrationApi", "/api/migration/** MISSING"],
            ["Workflows UI", "WorkflowsPage fetch", "BROKEN (wrong host)"],
        ],
        [35, 60, 45],
    )

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    pdf.output(str(OUTPUT))
    print(f"PDF written to: {OUTPUT}")
    return OUTPUT


if __name__ == "__main__":
    build()
