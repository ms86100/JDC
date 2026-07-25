import React, { useState } from 'react';
import '../AircraftDesignStyles.css';

/* ─── colour tokens ─── */
const C = {
  brand: '#0052cc',
  success: '#00875a',
  danger: '#FF5630',
  warning: '#FFAB00',
  purple: '#6554C0',
  teal: '#00B8D9',
  dark: '#172b4d',
  subtle: '#6b778c',
  bg: '#f4f5f7',
  border: '#dfe1e6',
  white: '#fff',
  dbBg: '#253858',
} as const;

/* ─── tabs ─── */
const TABS = [
  'System Overview',
  'Service Map',
  'Domain Model',
  'Data Flow',
  'VVO Lifecycle',
  'Defect Management',
  'Plugin Matrix',
  'API Reference',
] as const;
type TabName = (typeof TABS)[number];

/* ─── small reusable pieces ─── */

function ServiceBox({ name, port, color, desc }: { name: string; port: string; color: string; desc: string }) {
  return (
    <div style={{ background: C.white, borderRadius: 6, border: `2px solid ${color}`, overflow: 'hidden', minWidth: 180 }}>
      <div style={{ background: color, color: C.white, padding: '6px 12px', fontSize: 13, fontWeight: 600 }}>{name}</div>
      <div style={{ padding: '8px 12px' }}>
        <div style={{ fontSize: 11, color: C.subtle }}>Port: {port}</div>
        <div style={{ fontSize: 12, color: C.dark, marginTop: 4 }}>{desc}</div>
      </div>
    </div>
  );
}

function DownArrow() {
  return (
    <div style={{ textAlign: 'center', margin: '10px 0' }}>
      <svg width="40" height="30">
        <path d="M20,0 L20,20 M14,14 L20,20 L26,14" stroke={C.subtle} fill="none" strokeWidth="2" />
      </svg>
    </div>
  );
}

function SectionHeading({ children }: { children: React.ReactNode }) {
  return <h3 style={{ fontSize: 16, fontWeight: 600, color: C.dark, margin: '0 0 12px' }}>{children}</h3>;
}

function Paragraph({ children }: { children: React.ReactNode }) {
  return <p style={{ fontSize: 13, color: C.dark, lineHeight: 1.65, margin: '0 0 16px' }}>{children}</p>;
}

function DiagramBox({
  label,
  sub,
  color,
  width = 160,
  height = 60,
  fontSize = 12,
}: {
  label: string;
  sub?: string;
  color: string;
  width?: number;
  height?: number;
  fontSize?: number;
}) {
  return (
    <div
      style={{
        width,
        minHeight: height,
        border: `2px solid ${color}`,
        borderRadius: 6,
        background: C.white,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: '6px 8px',
        boxSizing: 'border-box',
      }}
    >
      <div style={{ fontWeight: 600, fontSize, color: C.dark }}>{label}</div>
      {sub && <div style={{ fontSize: 10, color: C.subtle, marginTop: 2 }}>{sub}</div>}
    </div>
  );
}

function ArrowRight({ label, width = 60 }: { label?: string; width?: number }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', width }}>
      <svg width={width} height="20" style={{ overflow: 'visible' }}>
        <defs>
          <marker id="arrowR" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto">
            <path d="M0,0 L8,3 L0,6" fill={C.subtle} />
          </marker>
        </defs>
        <line x1="0" y1="10" x2={width - 8} y2="10" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowR)" />
      </svg>
      {label && <div style={{ fontSize: 9, color: C.subtle, marginTop: 2, whiteSpace: 'nowrap' }}>{label}</div>}
    </div>
  );
}

function ArrowDown({ label }: { label?: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', margin: '4px 0' }}>
      <svg width="20" height="30">
        <defs>
          <marker id="arrowD" markerWidth="8" markerHeight="6" refX="3" refY="6" orient="auto">
            <path d="M0,0 L3,6 L6,0" fill={C.subtle} />
          </marker>
        </defs>
        <line x1="10" y1="0" x2="10" y2="22" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowD)" />
      </svg>
      {label && <div style={{ fontSize: 9, color: C.subtle, whiteSpace: 'nowrap' }}>{label}</div>}
    </div>
  );
}

function StateBadge({ label, color, active }: { label: string; color: string; active?: boolean }) {
  return (
    <div
      style={{
        display: 'inline-block',
        padding: '4px 12px',
        borderRadius: 4,
        background: active ? color : C.white,
        color: active ? C.white : color,
        border: `2px solid ${color}`,
        fontSize: 11,
        fontWeight: 700,
        textTransform: 'uppercase',
        letterSpacing: '0.03em',
        whiteSpace: 'nowrap',
      }}
    >
      {label}
    </div>
  );
}

/* ─── collapsible section for API reference ─── */
function CollapsibleSection({
  title,
  badge,
  children,
}: {
  title: string;
  badge?: string;
  children: React.ReactNode;
}) {
  const [open, setOpen] = useState(false);
  return (
    <div style={{ border: `1px solid ${C.border}`, borderRadius: 6, marginBottom: 8, background: C.white }}>
      <button
        onClick={() => setOpen(!open)}
        style={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '10px 14px',
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          fontSize: 13,
          fontWeight: 600,
          color: C.dark,
          textAlign: 'left',
        }}
      >
        <span>
          {open ? '▼' : '▶'} {title}
        </span>
        {badge && (
          <span
            style={{
              fontSize: 11,
              fontWeight: 600,
              background: '#deebff',
              color: '#0747a6',
              padding: '2px 8px',
              borderRadius: 3,
            }}
          >
            {badge}
          </span>
        )}
      </button>
      {open && <div style={{ padding: '0 14px 12px' }}>{children}</div>}
    </div>
  );
}

function EndpointRow({ method, path, desc }: { method: string; path: string; desc: string }) {
  const methodColors: Record<string, string> = {
    GET: C.success,
    POST: C.brand,
    PUT: C.warning,
    PATCH: '#FFAB00',
    DELETE: C.danger,
  };
  return (
    <tr>
      <td style={{ padding: '6px 10px', borderBottom: `1px solid ${C.bg}`, whiteSpace: 'nowrap' }}>
        <span
          style={{
            display: 'inline-block',
            width: 52,
            textAlign: 'center',
            padding: '2px 0',
            borderRadius: 3,
            fontSize: 10,
            fontWeight: 700,
            color: C.white,
            background: methodColors[method] || C.subtle,
          }}
        >
          {method}
        </span>
      </td>
      <td style={{ padding: '6px 10px', borderBottom: `1px solid ${C.bg}`, fontFamily: 'monospace', fontSize: 12, color: C.dark }}>
        {path}
      </td>
      <td style={{ padding: '6px 10px', borderBottom: `1px solid ${C.bg}`, fontSize: 12, color: C.subtle }}>
        {desc}
      </td>
    </tr>
  );
}

/* =========================================================================
   Tab render functions
   ========================================================================= */

function renderSystemOverview() {
  return (
    <div>
      <SectionHeading>Platform Architecture</SectionHeading>
      <Paragraph>
        SYSDOPS runs as a Jira Data Center clone purpose-built for aircraft design verification and validation.
        The platform comprises 12+ Spring Boot microservices, a single PostgreSQL 16 database with 14 isolated
        schemas, and a React 18 frontend served through an API gateway. Inter-service communication uses
        synchronous REST over HTTP (WebClient). Each service owns its schema and exposes a versioned REST API.
      </Paragraph>

      {/* Architecture Diagram */}
      <div style={{ position: 'relative', padding: 20, background: C.bg, borderRadius: 8, marginBottom: 24 }}>
        {/* Top: Frontend + Gateway */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: 40, marginBottom: 10 }}>
          <ServiceBox name="jira-frontend" port="3000" color={C.brand} desc="React 18 + Vite" />
          <ServiceBox name="jira-gateway" port="8080" color={C.dark} desc="API Gateway (routing)" />
        </div>
        <DownArrow />

        {/* Row 1 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 12 }}>
          <ServiceBox name="auth-service" port="8081" color={C.purple} desc="JWT auth, SAML SSO" />
          <ServiceBox name="user-service" port="8082" color={C.purple} desc="User/group CRUD" />
          <ServiceBox name="project-service" port="8083" color={C.success} desc="Projects, schemes, roles" />
          <ServiceBox name="issue-service" port="8084" color={C.success} desc="Issues, custom fields, change mgmt" />
        </div>

        {/* Row 2 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 12 }}>
          <ServiceBox name="workflow-service" port="8085" color={C.danger} desc="Workflows, automation, scripting" />
          <ServiceBox name="test-service" port="8095" color={C.danger} desc="VVO, Xray, defects, reports" />
          <ServiceBox name="admin-service" port="8093" color={C.warning} desc="Master data, assets, config" />
          <ServiceBox name="search-service" port="8088" color={C.teal} desc="JQL parser, full-text search" />
        </div>

        {/* Row 3 */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 10 }}>
          <ServiceBox name="sprint-service" port="8091" color={C.teal} desc="Sprints, boards, kanban" />
          <ServiceBox name="plan-service" port="--" color={C.teal} desc="Roadmaps, teams, goals" />
          <ServiceBox name="notification-service" port="8087" color={C.subtle} desc="Email, templates" />
          <ServiceBox name="dashboard-service" port="--" color={C.subtle} desc="Gadgets, charts" />
        </div>

        <DownArrow />

        {/* Database */}
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <div style={{ background: C.dbBg, color: C.white, padding: '16px 40px', borderRadius: 8, textAlign: 'center', maxWidth: 700 }}>
            <div style={{ fontWeight: 700, fontSize: 16 }}>PostgreSQL 16</div>
            <div style={{ fontSize: 12, opacity: 0.8, marginTop: 4 }}>
              14 schemas: jira_auth, jira_issue, jira_project, jira_workflow, jira_test, jira_admin,
              jira_search, jira_sprint, jira_plan, jira_comment, jira_notification, jira_audit,
              jira_version, jira_component
            </div>
          </div>
        </div>
      </div>

      {/* Key stats */}
      <div className="ads-stats">
        <div className="ads-stat ads-stat--brand">
          <span className="ads-stat-value">12+</span>
          <span className="ads-stat-label">Microservices</span>
        </div>
        <div className="ads-stat ads-stat--success">
          <span className="ads-stat-value">14</span>
          <span className="ads-stat-label">DB Schemas</span>
        </div>
        <div className="ads-stat ads-stat--warning">
          <span className="ads-stat-value">200+</span>
          <span className="ads-stat-label">REST Endpoints</span>
        </div>
        <div className="ads-stat">
          <span className="ads-stat-value">150+</span>
          <span className="ads-stat-label">Entity Models</span>
        </div>
      </div>

      {/* Technology Stack */}
      <div className="ads-card" style={{ marginTop: 8 }}>
        <h4 className="ads-card-title">Technology Stack</h4>
        <div className="ads-grid-3">
          <div>
            <div className="ads-section-title">Backend</div>
            <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, color: C.dark, lineHeight: 1.8 }}>
              <li>Java 21 + Spring Boot 3.3</li>
              <li>Spring Data JPA (Hibernate 6)</li>
              <li>Spring Security + JWT</li>
              <li>WebClient for inter-service calls</li>
              <li>GraalJS (script engine)</li>
              <li>Flyway migrations per schema</li>
            </ul>
          </div>
          <div>
            <div className="ads-section-title">Frontend</div>
            <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, color: C.dark, lineHeight: 1.8 }}>
              <li>React 18 + TypeScript 5</li>
              <li>Vite 5 (build tooling)</li>
              <li>React Router 6</li>
              <li>Axios for HTTP</li>
              <li>CSS Modules + ads-* design tokens</li>
              <li>No external UI library</li>
            </ul>
          </div>
          <div>
            <div className="ads-section-title">Infrastructure</div>
            <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13, color: C.dark, lineHeight: 1.8 }}>
              <li>PostgreSQL 16 (single instance)</li>
              <li>API Gateway (port 8080)</li>
              <li>Maven multi-module build</li>
              <li>Docker Compose (dev)</li>
              <li>Schema-per-service isolation</li>
              <li>Centralized YAML config</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 2: Service Map ── */
function renderServiceMap() {
  const services = [
    { name: 'auth-service', port: '8081', schema: 'jira_auth', entities: 4, keyModels: 'Role, UserGroup, Permission, SessionToken', dependsOn: '--', keyEndpoints: '/api/auth/login, /api/auth/register, /api/auth/token/refresh' },
    { name: 'user-service', port: '8082', schema: 'jira_admin', entities: 3, keyModels: 'CwdUser, CwdGroup, CwdMembership', dependsOn: 'auth', keyEndpoints: '/api/users, /api/groups' },
    { name: 'project-service', port: '8083', schema: 'jira_project', entities: 15, keyModels: 'Project, ProjectScheme, ProjectRole, Component, Version', dependsOn: 'auth, user', keyEndpoints: '/api/projects, /api/projects/{id}/roles' },
    { name: 'issue-service', port: '8084', schema: 'jira_issue', entities: 48, keyModels: 'Issue, CustomField, ChangeCard, DesignItem, DCL, SystemStandard, Deliverable', dependsOn: 'project, workflow', keyEndpoints: '/api/issues, /api/issues/{id}/change-card, /api/issues/{id}/design-item' },
    { name: 'workflow-service', port: '8085', schema: 'jira_workflow', entities: 31, keyModels: 'Workflow, Transition, Condition, Validator, PostFunction, AutomationRule, ScriptDefinition', dependsOn: 'issue, project', keyEndpoints: '/api/workflows, /api/automation/rules, /api/scripts' },
    { name: 'test-service', port: '8095', schema: 'jira_test', entities: 80, keyModels: 'VvoDefinition, HlvvoDefinition, TestIssue, TestPlan, TestExecution, TechEvent, BenchDefect, ProblemReport', dependsOn: 'admin, workflow', keyEndpoints: '/api/vvo, /api/tech-events, /api/vv-reports, /api/export-templates' },
    { name: 'admin-service', port: '8093', schema: 'jira_admin', entities: 58, keyModels: 'MasterDataCategory, MasterDataValue, Asset, AssetType, IssueType, Status, Priority', dependsOn: '--', keyEndpoints: '/api/admin/master-data, /api/admin/assets, /api/admin/issue-types' },
    { name: 'search-service', port: '8088', schema: 'jira_search', entities: 4, keyModels: 'JQLQuery, JQLClause, SearchIndex, SavedFilter', dependsOn: 'issue', keyEndpoints: '/api/search, /api/jql/parse, /api/jql/autocomplete' },
    { name: 'sprint-service', port: '8091', schema: 'jira_sprint', entities: 5, keyModels: 'Sprint, Board, BoardConfig, SprintIssue, BoardColumn', dependsOn: 'issue', keyEndpoints: '/api/sprints, /api/boards' },
    { name: 'plan-service', port: '--', schema: 'jira_plan', entities: 10, keyModels: 'PlanItem, PlanTeam, PlanGoal, PlanDependency, PlanRelease', dependsOn: 'project', keyEndpoints: '/api/plans, /api/plans/{id}/goals, /api/plans/{id}/teams' },
    { name: 'notification-service', port: '8087', schema: 'jira_notification', entities: 3, keyModels: 'EmailTemplate, NotificationEvent, NotificationPreference', dependsOn: 'user', keyEndpoints: '/api/notifications, /api/notification-preferences' },
    { name: 'dashboard-service', port: '--', schema: '--', entities: 2, keyModels: 'GadgetInstance, DashboardLayout', dependsOn: 'search', keyEndpoints: '/api/dashboards, /api/gadgets' },
  ];

  return (
    <div>
      <SectionHeading>Service Registry</SectionHeading>
      <Paragraph>
        Every service follows the same project structure: controller / service / repository / entity / dto / config.
        Inter-service communication is via REST calls through WebClient beans. Each service has its own
        application.yml with schema-specific datasource and Flyway configuration.
      </Paragraph>

      <div className="ads-table-wrap">
        <table className="ads-table">
          <thead>
            <tr>
              <th>Service</th>
              <th>Port</th>
              <th>Schema</th>
              <th>Entities</th>
              <th>Key Models</th>
              <th>Depends On</th>
              <th>Key Endpoints</th>
            </tr>
          </thead>
          <tbody>
            {services.map((s) => (
              <tr key={s.name}>
                <td>
                  <span style={{ fontWeight: 600, color: C.brand }}>{s.name}</span>
                </td>
                <td>
                  <code style={{ fontSize: 12, background: C.bg, padding: '2px 6px', borderRadius: 3 }}>{s.port}</code>
                </td>
                <td style={{ fontFamily: 'monospace', fontSize: 12 }}>{s.schema}</td>
                <td style={{ textAlign: 'center', fontWeight: 600 }}>{s.entities}+</td>
                <td style={{ fontSize: 12, maxWidth: 220 }}>{s.keyModels}</td>
                <td style={{ fontSize: 12, color: C.subtle }}>{s.dependsOn}</td>
                <td style={{ fontFamily: 'monospace', fontSize: 11, maxWidth: 260, wordBreak: 'break-all' }}>
                  {s.keyEndpoints}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Dependency Graph */}
      <div className="ads-card" style={{ marginTop: 24 }}>
        <h4 className="ads-card-title">Service Dependency Graph</h4>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <svg width="800" height="340" viewBox="0 0 800 340" style={{ display: 'block', margin: '0 auto' }}>
            {/* Nodes */}
            {[
              { id: 'auth', x: 350, y: 20, c: C.purple },
              { id: 'user', x: 200, y: 90, c: C.purple },
              { id: 'project', x: 100, y: 170, c: C.success },
              { id: 'issue', x: 300, y: 170, c: C.success },
              { id: 'workflow', x: 500, y: 170, c: C.danger },
              { id: 'admin', x: 680, y: 90, c: C.warning },
              { id: 'test', x: 500, y: 260, c: C.danger },
              { id: 'search', x: 100, y: 260, c: C.teal },
              { id: 'sprint', x: 300, y: 300, c: C.teal },
              { id: 'plan', x: 100, y: 310, c: C.teal },
              { id: 'notification', x: 680, y: 260, c: C.subtle },
              { id: 'dashboard', x: 680, y: 170, c: C.subtle },
            ].map((n) => (
              <g key={n.id}>
                <rect x={n.x} y={n.y} width={100} height={32} rx={4} fill={n.c} />
                <text x={n.x + 50} y={n.y + 20} textAnchor="middle" fill={C.white} fontSize={10} fontWeight={600}>
                  {n.id}
                </text>
              </g>
            ))}

            {/* Edges */}
            {[
              [400, 52, 250, 90], // auth -> user
              [250, 122, 150, 170], // user -> project
              [300, 52, 150, 170], // auth -> project
              [400, 52, 350, 170], // auth -> issue
              [200, 202, 300, 170], // project -> issue (bidirectional concept)
              [400, 202, 500, 170], // issue -> workflow
              [550, 202, 550, 260], // workflow -> test
              [730, 122, 550, 260], // admin -> test
              [350, 202, 150, 260], // issue -> search
              [350, 202, 350, 300], // issue -> sprint
              [150, 202, 150, 310], // project -> plan
              [250, 122, 730, 260], // user -> notification
              [150, 292, 680, 170], // search -> dashboard
            ].map(([x1, y1, x2, y2], i) => (
              <line key={i} x1={x1} y1={y1} x2={x2} y2={y2} stroke={C.border} strokeWidth={1.5} strokeDasharray="4 3" />
            ))}
          </svg>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 3: Domain Model ── */
function renderDomainModel() {
  return (
    <div>
      <SectionHeading>SYSDOPS Domain Entity Relationships</SectionHeading>
      <Paragraph>
        The SYSDOPS domain model maps aircraft verification and validation processes onto a Jira-style
        issue hierarchy. The core entities -- HLVVO, VVO, TestIssue, TechEvent, BenchDefect, ProblemReport,
        ChangeCard and DesignItem -- form a directed acyclic graph of traceability links.
      </Paragraph>

      {/* ER Diagram */}
      <div style={{ padding: 24, background: C.bg, borderRadius: 8, marginBottom: 24, overflowX: 'auto' }}>
        <div style={{ minWidth: 700, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0 }}>
          {/* HLVVO */}
          <DiagramBox label="HLVVO" sub="High-Level VVO (parent grouping)" color={C.brand} width={220} />
          <ArrowDown label="is parent of (1:N)" />

          {/* VVO row */}
          <div style={{ display: 'flex', gap: 24, alignItems: 'flex-start' }}>
            <DiagramBox label="VVO" sub="Design Office project" color={C.brand} />
            <DiagramBox label="VVO" sub="Design Office project" color={C.brand} />
            <DiagramBox label="VVO" sub="Design Office project" color={C.brand} />
          </div>
          <ArrowDown label="tested by (1:N)" />

          {/* Test + Execution */}
          <div style={{ display: 'flex', gap: 80, alignItems: 'flex-start' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <DiagramBox label="TestIssue" sub="LAB project" color={C.success} />
              <ArrowDown label="defect from" />
              <DiagramBox label="TechEvent (M1668)" sub="14-state workflow" color={C.danger} width={180} />
              <ArrowDown label="creates" />
              <DiagramBox label="ProblemReport" sub="4-state workflow" color={C.warning} />
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <DiagramBox label="TestExecution" sub="Campaign run" color={C.success} />
              <ArrowDown label="captures" />
              <DiagramBox label="BenchDefect" sub="6-state, test-means" color={C.danger} width={180} />
            </div>
          </div>

          {/* Change Management branch */}
          <div style={{ marginTop: 24, borderTop: `2px dashed ${C.border}`, paddingTop: 20, width: '100%' }}>
            <div style={{ textAlign: 'center', fontSize: 12, fontWeight: 600, color: C.subtle, marginBottom: 12 }}>
              CHANGE MANAGEMENT DOMAIN
            </div>
            <div style={{ display: 'flex', justifyContent: 'center', gap: 24, flexWrap: 'wrap' }}>
              <DiagramBox label="ChangeCard" sub="6-tab master form" color={C.purple} width={140} />
              <ArrowRight label="child of" />
              <DiagramBox label="DesignItem" sub="Design change unit" color={C.purple} width={140} />
              <ArrowRight label="tracked by" />
              <DiagramBox label="DCL" sub="Design Change List" color={C.purple} width={140} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'center', gap: 24, marginTop: 12, flexWrap: 'wrap' }}>
              <DiagramBox label="Deliverable" sub="Output artifact" color={C.teal} width={140} />
              <DiagramBox label="SystemStandard" sub="Compliance ref" color={C.teal} width={140} />
              <DiagramBox label="Modification" sub="Change record" color={C.teal} width={140} />
              <DiagramBox label="ReviewSubTask" sub="Review checklist" color={C.teal} width={140} />
            </div>
          </div>
        </div>
      </div>

      {/* Entity counts per service */}
      <div className="ads-card">
        <h4 className="ads-card-title">Entity Distribution by Service</h4>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {[
            { svc: 'test-service', count: 80, color: C.danger },
            { svc: 'admin-service', count: 58, color: C.warning },
            { svc: 'issue-service', count: 48, color: C.success },
            { svc: 'workflow-service', count: 31, color: C.danger },
            { svc: 'project-service', count: 15, color: C.success },
            { svc: 'plan-service', count: 10, color: C.teal },
            { svc: 'sprint-service', count: 5, color: C.teal },
            { svc: 'search-service', count: 4, color: C.teal },
            { svc: 'auth-service', count: 4, color: C.purple },
            { svc: 'user-service', count: 3, color: C.purple },
            { svc: 'notification-service', count: 3, color: C.subtle },
            { svc: 'dashboard-service', count: 2, color: C.subtle },
          ].map((e) => (
            <div
              key={e.svc}
              style={{
                padding: '8px 14px',
                borderRadius: 6,
                border: `1px solid ${C.border}`,
                background: C.white,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <span style={{ width: 10, height: 10, borderRadius: 2, background: e.color, display: 'inline-block' }} />
              <span style={{ fontSize: 12, fontWeight: 600, color: C.dark }}>{e.svc}</span>
              <span style={{ fontSize: 12, color: C.subtle }}>{e.count}+ entities</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ── Tab 4: Data Flow ── */
function renderDataFlow() {
  return (
    <div>
      <SectionHeading>Key Data Flows</SectionHeading>

      {/* Flow 1: Design Office to Lab */}
      <div className="ads-card" style={{ marginBottom: 16 }}>
        <h4 className="ads-card-title" style={{ color: C.brand }}>1. Design Office to Lab (V&V Pipeline)</h4>
        <Paragraph>
          VVOs are authored in Design Office projects, baselined, optionally exported to DOORS, then
          transferred to LAB projects where test engineers write test cases, create campaigns, and execute.
        </Paragraph>
        <div className="ads-pipeline">
          <span className="ads-pipeline-step ads-pipeline-step--done">VVO Authored</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step ads-pipeline-step--done">Baseline Created</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step ads-pipeline-step--done">DOORS Export</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step ads-pipeline-step--active">Transfer to LAB</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Tests Written</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Campaign Created</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Execution Run</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Report Generated</span>
        </div>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 800 }}>
            <DiagramBox label="VvoDefinition" sub="test-service" color={C.brand} width={130} height={50} />
            <ArrowRight label="snapshot" width={50} />
            <DiagramBox label="BaselineVersion" sub="test-service" color={C.brand} width={130} height={50} />
            <ArrowRight label="export" width={50} />
            <DiagramBox label="ExportTemplate" sub="DOORS format" color={C.teal} width={130} height={50} />
            <ArrowRight label="link" width={50} />
            <DiagramBox label="TestIssue" sub="test-service" color={C.success} width={120} height={50} />
            <ArrowRight label="plan" width={40} />
            <DiagramBox label="TestPlan" sub="test-service" color={C.success} width={110} height={50} />
            <ArrowRight label="run" width={40} />
            <DiagramBox label="TestExecution" sub="campaign" color={C.success} width={120} height={50} />
          </div>
        </div>
      </div>

      {/* Flow 2: Defect Management */}
      <div className="ads-card" style={{ marginBottom: 16 }}>
        <h4 className="ads-card-title" style={{ color: C.danger }}>2. Defect Management Flow</h4>
        <Paragraph>
          When a test execution reveals a failure, a TechEvent (M1668 form) is created. The supplier
          analyzes and may create a Change Card for the fix. The fix is re-tested to closure.
        </Paragraph>
        <div className="ads-pipeline">
          <span className="ads-pipeline-step ads-pipeline-step--done">Test Fails</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step ads-pipeline-step--active">TechEvent Created</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Supplier Analysis</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Change Card Opened</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Fix Implemented</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Re-test</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Closed</span>
        </div>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 700 }}>
            <DiagramBox label="TestExecution" sub="FAIL result" color={C.danger} width={130} height={50} />
            <ArrowRight label="raises" width={50} />
            <DiagramBox label="TechEvent" sub="M1668 form" color={C.danger} width={120} height={50} />
            <ArrowRight label="spawns" width={50} />
            <DiagramBox label="BenchDefect" sub="test-means issue" color={C.warning} width={130} height={50} />
            <ArrowRight label="" width={30} />
            <DiagramBox label="ProblemReport" sub="escalation" color={C.warning} width={130} height={50} />
            <ArrowRight label="fix via" width={50} />
            <DiagramBox label="ChangeCard" sub="6-tab form" color={C.purple} width={120} height={50} />
          </div>
        </div>
      </div>

      {/* Flow 3: Workflow Engine */}
      <div className="ads-card" style={{ marginBottom: 16 }}>
        <h4 className="ads-card-title" style={{ color: C.purple }}>3. Workflow Engine Transition</h4>
        <Paragraph>
          All status transitions route through the workflow engine. The WorkflowBridgeService in each
          owning service calls out to workflow-service which evaluates conditions, runs validators, and
          fires post-functions before returning the authorised transition.
        </Paragraph>
        <div className="ads-pipeline">
          <span className="ads-pipeline-step ads-pipeline-step--done">Transition Request</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step ads-pipeline-step--active">WorkflowBridgeService</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">ConditionEvaluator</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">ValidatorExecutor</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">PostFunctionExecutor</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">Status Updated</span>
        </div>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 750 }}>
            <DiagramBox label="UI / API" sub="transition button" color={C.brand} width={110} height={50} />
            <ArrowRight label="POST" width={40} />
            <DiagramBox label="Owning Service" sub="e.g. issue-service" color={C.success} width={130} height={50} />
            <ArrowRight label="REST" width={40} />
            <DiagramBox label="workflow-service" sub="engine pipeline" color={C.danger} width={140} height={50} />
            <ArrowRight label="callback" width={50} />
            <DiagramBox label="WorkflowInternal" sub="Controller" color={C.danger} width={130} height={50} />
            <ArrowRight label="persist" width={50} />
            <DiagramBox label="Entity" sub="status = new" color={C.success} width={100} height={50} />
          </div>
        </div>
      </div>

      {/* Flow 4: Automation Rule */}
      <div className="ads-card">
        <h4 className="ads-card-title" style={{ color: C.teal }}>4. Automation Rule Execution</h4>
        <Paragraph>
          Automation rules listen for domain events (status change, field update, scheduled cron). When
          triggered, the rule engine evaluates conditions, then fires actions (transition, update field,
          send email, run script).
        </Paragraph>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 650 }}>
            <DiagramBox label="Domain Event" sub="status_changed" color={C.teal} width={120} height={50} />
            <ArrowRight label="match" width={40} />
            <DiagramBox label="AutomationRule" sub="trigger check" color={C.teal} width={130} height={50} />
            <ArrowRight label="eval" width={40} />
            <DiagramBox label="Condition" sub="JQL / field" color={C.warning} width={110} height={50} />
            <ArrowRight label="fire" width={40} />
            <DiagramBox label="Action" sub="transition / email" color={C.success} width={120} height={50} />
            <ArrowRight label="log" width={40} />
            <DiagramBox label="AuditLog" sub="execution record" color={C.subtle} width={120} height={50} />
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 5: VVO Lifecycle ── */
function renderVvoLifecycle() {
  return (
    <div>
      <SectionHeading>VVO State Machine</SectionHeading>
      <Paragraph>
        Each VVO (Verification and Validation Objective) progresses through 6 states. Transitions
        are governed by the workflow engine with conditions (e.g. all tests passed) and validators
        (e.g. required fields filled).
      </Paragraph>

      {/* State Machine Diagram */}
      <div style={{ padding: 24, background: C.bg, borderRadius: 8, marginBottom: 24, overflowX: 'auto' }}>
        <div style={{ minWidth: 700, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
          {/* Row 1: NEW */}
          <StateBadge label="NEW" color={C.brand} active />
          <ArrowDown label="Author completes fields" />

          {/* Row 2: TO_BE_VERIFIED */}
          <StateBadge label="TO BE VERIFIED" color={C.warning} active />

          <div style={{ display: 'flex', gap: 60, marginTop: 8 }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <ArrowDown label="Reviewer approves" />
              <StateBadge label="VERIFIED" color={C.success} active />
              <ArrowDown label="PM releases" />
              <StateBadge label="RELEASED" color="#36b37e" active />
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <ArrowDown label="Cancel" />
              <StateBadge label="CANCELLED" color={C.danger} active />
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <ArrowDown label="Replaced by newer" />
              <StateBadge label="SUPERSEDED" color={C.subtle} active />
            </div>
          </div>
        </div>
      </div>

      {/* Transition table */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title">VVO Transition Rules</h4>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr>
                <th>From</th>
                <th>To</th>
                <th>Transition Name</th>
                <th>Condition</th>
                <th>Validator</th>
                <th>Post-Function</th>
              </tr>
            </thead>
            <tbody>
              {[
                ['NEW', 'TO_BE_VERIFIED', 'Submit for Review', 'Author is assignee', 'All required fields filled', 'Set reviewedBy'],
                ['TO_BE_VERIFIED', 'VERIFIED', 'Verify', 'User has Reviewer role', 'Test coverage >= threshold', 'Lock fields, snapshot'],
                ['VERIFIED', 'RELEASED', 'Release', 'User has PM role', 'No blocking TechEvents', 'Create baseline snapshot'],
                ['NEW', 'CANCELLED', 'Cancel', 'Author or PM', '--', 'Clear links'],
                ['TO_BE_VERIFIED', 'CANCELLED', 'Cancel', 'Author or PM', '--', 'Clear links'],
                ['RELEASED', 'SUPERSEDED', 'Supersede', 'New VVO linked', 'Successor VVO exists', 'Link predecessor/successor'],
              ].map(([from, to, name, cond, val, pf], i) => (
                <tr key={i}>
                  <td><span className={`ads-badge ads-badge--${from.toLowerCase()}`}>{from}</span></td>
                  <td><span className={`ads-badge ads-badge--${to.toLowerCase()}`}>{to}</span></td>
                  <td style={{ fontWeight: 500 }}>{name}</td>
                  <td style={{ fontSize: 12 }}>{cond}</td>
                  <td style={{ fontSize: 12 }}>{val}</td>
                  <td style={{ fontSize: 12 }}>{pf}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* 4-step Baselining */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title">4-Step Baselining Process</h4>
        <Paragraph>
          Baselining captures a frozen snapshot of VVO definitions at a point in time, allowing
          traceability comparison between versions. The process is irreversible once finalised.
        </Paragraph>
        <div className="ads-pipeline">
          <span className="ads-pipeline-step ads-pipeline-step--done">1. Select VVOs</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step ads-pipeline-step--done">2. Name Baseline</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step ads-pipeline-step--active">3. Snapshot Fields</span>
          <span className="ads-pipeline-arrow">&rarr;</span>
          <span className="ads-pipeline-step">4. Finalise (lock)</span>
        </div>
        <div className="ads-table-wrap" style={{ marginTop: 12 }}>
          <table className="ads-table">
            <thead>
              <tr><th>Step</th><th>API Endpoint</th><th>Description</th></tr>
            </thead>
            <tbody>
              <tr><td>1</td><td style={{ fontFamily: 'monospace', fontSize: 12 }}>POST /api/vvo/baseline</td><td>Create baseline with name and selected VVO IDs</td></tr>
              <tr><td>2</td><td style={{ fontFamily: 'monospace', fontSize: 12 }}>POST /api/vvo/baseline/{'{id}'}/snapshot</td><td>Snapshot all field values for selected VVOs</td></tr>
              <tr><td>3</td><td style={{ fontFamily: 'monospace', fontSize: 12 }}>GET /api/vvo/baseline/{'{id}'}/compare/{'{otherId}'}</td><td>Compare two baselines for delta report</td></tr>
              <tr><td>4</td><td style={{ fontFamily: 'monospace', fontSize: 12 }}>PUT /api/vvo/baseline/{'{id}'}/finalise</td><td>Lock baseline (irreversible)</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* DOORS Integration */}
      <div className="ads-card">
        <h4 className="ads-card-title">DOORS Integration Flow</h4>
        <Paragraph>
          VVO definitions can be exported to IBM DOORS format using configurable export templates.
          Templates support field mapping, section ordering, and conditional inclusion. Export
          produces DOCX, XLSX, or structured XML output.
        </Paragraph>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 600 }}>
            <DiagramBox label="VVO Set" sub="baseline or filter" color={C.brand} width={120} height={50} />
            <ArrowRight label="select" width={50} />
            <DiagramBox label="ExportTemplate" sub="field mapping" color={C.teal} width={130} height={50} />
            <ArrowRight label="render" width={50} />
            <DiagramBox label="DocumentExport" sub="Service" color={C.success} width={130} height={50} />
            <ArrowRight label="output" width={50} />
            <DiagramBox label="DOORS File" sub="DOCX / XLSX / XML" color={C.dark} width={130} height={50} />
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 6: Defect Management ── */
function renderDefectManagement() {
  return (
    <div>
      <SectionHeading>Defect Management Workflows</SectionHeading>
      <Paragraph>
        SYSDOPS models three interconnected defect types: TechEvent (the M1668 form, 14 states),
        BenchDefect (test-means issues, 6 states), and ProblemReport (escalated issues, 4 states).
        Each has its own workflow but they are linked by traceability relations.
      </Paragraph>

      {/* TechEvent M1668 */}
      <div className="ads-card" style={{ marginBottom: 16 }}>
        <h4 className="ads-card-title" style={{ color: C.danger }}>TechEvent (M1668) -- 14-State Workflow</h4>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ minWidth: 800 }}>
            {/* Row 1: Opening */}
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12, alignItems: 'center' }}>
              <StateBadge label="OPEN" color={C.brand} active />
              <ArrowRight width={40} />
              <StateBadge label="ANALYSIS" color={C.warning} active />
              <ArrowRight width={40} />
              <StateBadge label="CLASSIFICATION" color={C.warning} active />
              <ArrowRight width={40} />
              <StateBadge label="RESOLVER" color={C.purple} active />
            </div>

            {/* Row 2: Resolution path */}
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12, alignItems: 'center', paddingLeft: 20 }}>
              <ArrowRight width={30} />
              <StateBadge label="TO_BE_ASSESSED" color={C.warning} active />
              <ArrowRight width={30} />
              <StateBadge label="ASSESSED" color={C.success} active />
              <ArrowRight width={30} />
              <StateBadge label="TO_BE_VERIFIED" color={C.warning} active />
              <ArrowRight width={30} />
              <StateBadge label="VERIFIED" color={C.success} active />
            </div>

            {/* Row 3: Closure + side states */}
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12, alignItems: 'center', paddingLeft: 40 }}>
              <ArrowRight width={30} />
              <StateBadge label="CLOSED" color={C.subtle} active />
            </div>

            {/* Side states */}
            <div style={{ borderTop: `1px dashed ${C.border}`, paddingTop: 12, marginTop: 8 }}>
              <div style={{ fontSize: 11, fontWeight: 600, color: C.subtle, marginBottom: 8 }}>SIDE STATES (reachable from multiple points):</div>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <StateBadge label="ON_HOLD" color="#6b778c" />
                <StateBadge label="REJECTED" color={C.danger} />
                <StateBadge label="CANCELLED" color={C.danger} />
                <StateBadge label="DEFERRED" color="#6b778c" />
                <StateBadge label="DUPLICATE" color="#6b778c" />
              </div>
            </div>
          </div>
        </div>

        <div className="ads-table-wrap" style={{ marginTop: 12 }}>
          <table className="ads-table">
            <thead>
              <tr><th>State</th><th>Responsible</th><th>Key Action</th><th>Exits To</th></tr>
            </thead>
            <tbody>
              {[
                ['OPEN', 'Creator', 'Log event with M1668 fields', 'ANALYSIS'],
                ['ANALYSIS', 'Analyst', 'Root cause analysis', 'CLASSIFICATION, ON_HOLD'],
                ['CLASSIFICATION', 'Classifier', 'Assign severity, category', 'RESOLVER, REJECTED'],
                ['RESOLVER', 'Supplier', 'Propose fix', 'TO_BE_ASSESSED, DEFERRED'],
                ['TO_BE_ASSESSED', 'Assessor', 'Review proposed fix', 'ASSESSED, RESOLVER'],
                ['ASSESSED', 'Lead', 'Approve fix plan', 'TO_BE_VERIFIED'],
                ['TO_BE_VERIFIED', 'Tester', 'Re-test fix', 'VERIFIED, RESOLVER'],
                ['VERIFIED', 'PM', 'Confirm closure', 'CLOSED'],
                ['CLOSED', '--', 'Terminal state', '--'],
              ].map(([state, who, action, exits], i) => (
                <tr key={i}>
                  <td><span className={`ads-badge ads-badge--${state.toLowerCase()}`}>{state}</span></td>
                  <td style={{ fontWeight: 500 }}>{who}</td>
                  <td style={{ fontSize: 12 }}>{action}</td>
                  <td style={{ fontSize: 12 }}>{exits}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* BenchDefect */}
      <div className="ads-card" style={{ marginBottom: 16 }}>
        <h4 className="ads-card-title" style={{ color: C.warning }}>BenchDefect -- 6-State Workflow</h4>
        <Paragraph>
          BenchDefects track test-means issues (equipment failures, calibration errors) discovered
          during test execution. They are linked to the TechEvent that discovered them.
        </Paragraph>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', minWidth: 500 }}>
            <StateBadge label="OPEN" color={C.brand} active />
            <ArrowRight width={40} />
            <StateBadge label="ANALYSIS" color={C.warning} active />
            <ArrowRight width={40} />
            <StateBadge label="IN_PROGRESS" color={C.brand} active />
            <ArrowRight width={40} />
            <StateBadge label="RESOLVED" color={C.success} active />
            <ArrowRight width={40} />
            <StateBadge label="VERIFIED" color={C.success} active />
            <ArrowRight width={40} />
            <StateBadge label="CLOSED" color={C.subtle} active />
          </div>
        </div>
        <div className="ads-alert ads-alert--info" style={{ marginTop: 12 }}>
          BenchDefects are created automatically from a TechEvent when the defect category is
          "test-means". The TechEvent ID is stored in the benchDefect.sourceEventId field.
        </div>
      </div>

      {/* ProblemReport */}
      <div className="ads-card" style={{ marginBottom: 16 }}>
        <h4 className="ads-card-title" style={{ color: C.purple }}>ProblemReport -- 4-State Workflow</h4>
        <Paragraph>
          ProblemReports are escalation records created when a TechEvent requires formal tracking
          beyond the supplier. They follow a simpler lifecycle.
        </Paragraph>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', minWidth: 400 }}>
            <StateBadge label="OPEN" color={C.brand} active />
            <ArrowRight width={50} />
            <StateBadge label="IN_PROGRESS" color={C.warning} active />
            <ArrowRight width={50} />
            <StateBadge label="RESOLVED" color={C.success} active />
            <ArrowRight width={50} />
            <StateBadge label="CLOSED" color={C.subtle} active />
          </div>
        </div>
      </div>

      {/* Interconnection diagram */}
      <div className="ads-card">
        <h4 className="ads-card-title">Defect Entity Interconnections</h4>
        <div style={{ padding: 20, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0, minWidth: 500 }}>
            <DiagramBox label="TestExecution" sub="FAIL verdict" color={C.danger} width={180} />
            <ArrowDown label="creates" />
            <DiagramBox label="TechEvent (M1668)" sub="14-state master record" color={C.danger} width={200} />
            <div style={{ display: 'flex', gap: 80, marginTop: 8 }}>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <ArrowDown label="if test-means" />
                <DiagramBox label="BenchDefect" sub="6-state" color={C.warning} width={150} />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <ArrowDown label="if escalated" />
                <DiagramBox label="ProblemReport" sub="4-state" color={C.purple} width={150} />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <ArrowDown label="fix required" />
                <DiagramBox label="ChangeCard" sub="issue-service" color={C.brand} width={150} />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 7: Plugin Matrix ── */
function renderPluginMatrix() {
  const plugins = [
    { feature: 'Test Management', plugin: 'Xray for Jira', impl: 'TestIssue, TestPlan, TestExecution, TestSet', svc: 'test-service', status: 'Full' },
    { feature: 'VVO Requirements', plugin: 'Xray (Requirements)', impl: 'VvoDefinition, HlvvoDefinition, BaselineVersion', svc: 'test-service', status: 'Full' },
    { feature: 'Project Roadmap', plugin: 'BigPicture', impl: 'PlanItem, PlanTeam, PlanGoal, PlanDependency', svc: 'plan-service', status: 'Full' },
    { feature: 'Automation Rules', plugin: 'Automation for Jira', impl: 'AutomationRule, AutomationEventListener, AutomationAction', svc: 'workflow-service', status: 'Full' },
    { feature: 'Document Export', plugin: 'XPorter for Jira', impl: 'ExportTemplate, DocumentExportService, section/field mapping', svc: 'test-service', status: 'Full' },
    { feature: 'Custom Charts', plugin: 'Custom Charts for Jira', impl: 'GadgetInstance (chartType, chartConfig JSON)', svc: 'dashboard-service', status: 'Full' },
    { feature: 'Asset Tracking', plugin: 'Assets & Inventory', impl: 'Asset, AssetType, AssetAttribute, AssetIssueLink', svc: 'admin-service', status: 'Full' },
    { feature: 'Scripting Engine', plugin: 'ScriptRunner', impl: 'ScriptDefinition, GraalJS sandbox, script console', svc: 'workflow-service', status: 'Full' },
    { feature: 'Change Management', plugin: 'Native Jira DC', impl: 'ChangeCard, DesignItem, DCL, Deliverable, Modification', svc: 'issue-service', status: 'Full' },
    { feature: 'Defect Management', plugin: 'Native + Xray', impl: 'TechEvent, BenchDefect, ProblemReport', svc: 'test-service', status: 'Full' },
    { feature: 'Master Data Admin', plugin: 'ScriptRunner (Lists)', impl: 'MasterDataCategory, MasterDataValue, dynamic CRUD', svc: 'admin-service', status: 'Full' },
    { feature: 'V&V Reporting', plugin: 'Custom Charts + Xray', impl: 'VvReport, CoverageMatrix, StatusDistribution', svc: 'test-service', status: 'Full' },
    { feature: 'Workflow Designer', plugin: 'Native Jira DC', impl: 'Workflow, Transition, Condition, Validator, PostFunction', svc: 'workflow-service', status: 'Full' },
    { feature: 'JQL Search', plugin: 'Native Jira DC', impl: 'JQLParser, JQLClause, SavedFilter', svc: 'search-service', status: 'Full' },
    { feature: 'Sprint Planning', plugin: 'Native Jira DC', impl: 'Sprint, Board, BoardConfig, BoardColumn', svc: 'sprint-service', status: 'Full' },
  ];

  return (
    <div>
      <SectionHeading>Plugin-to-Feature Mapping</SectionHeading>
      <Paragraph>
        SYSDOPS replaces 9+ Jira Data Center marketplace plugins with native implementations. Each
        feature is purpose-built for aircraft V&V workflows, avoiding generic plugin limitations and
        licensing costs. Below is the complete mapping.
      </Paragraph>

      <div className="ads-table-wrap">
        <table className="ads-table">
          <thead>
            <tr>
              <th>Feature</th>
              <th>Jira DC Plugin</th>
              <th>Our Implementation</th>
              <th>Service</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {plugins.map((p, i) => (
              <tr key={i}>
                <td style={{ fontWeight: 600 }}>{p.feature}</td>
                <td>
                  <span style={{ fontSize: 12, color: C.subtle, fontStyle: 'italic' }}>{p.plugin}</span>
                </td>
                <td style={{ fontSize: 12, maxWidth: 280 }}>{p.impl}</td>
                <td>
                  <code style={{ fontSize: 11, background: C.bg, padding: '2px 6px', borderRadius: 3 }}>{p.svc}</code>
                </td>
                <td>
                  <span className="ads-badge ads-badge--verified">{p.status}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Benefits */}
      <div className="ads-card" style={{ marginTop: 24 }}>
        <h4 className="ads-card-title">Benefits of Native Implementation</h4>
        <div className="ads-grid-3">
          <div style={{ padding: 16, borderLeft: `3px solid ${C.success}` }}>
            <div style={{ fontWeight: 600, fontSize: 14, color: C.dark, marginBottom: 4 }}>Domain Alignment</div>
            <div style={{ fontSize: 13, color: C.subtle }}>
              Every feature is tailored to aircraft V&V semantics: M1668 TechEvent forms, VVO
              baselining, DOORS export formats, and aerospace-standard defect classification.
            </div>
          </div>
          <div style={{ padding: 16, borderLeft: `3px solid ${C.brand}` }}>
            <div style={{ fontWeight: 600, fontSize: 14, color: C.dark, marginBottom: 4 }}>Single Codebase</div>
            <div style={{ fontSize: 13, color: C.subtle }}>
              All features share the same entity model, authentication, and workflow engine.
              No plugin compatibility issues, no version-lock, no marketplace dependencies.
            </div>
          </div>
          <div style={{ padding: 16, borderLeft: `3px solid ${C.warning}` }}>
            <div style={{ fontWeight: 600, fontSize: 14, color: C.dark, marginBottom: 4 }}>Admin-Driven Config</div>
            <div style={{ fontSize: 13, color: C.subtle }}>
              Master Data CRUD lets administrators define all business values dynamically --
              no hardcoding of statuses, categories, severities, or classification codes.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 8: API Reference ── */
function renderApiReference() {
  return (
    <div>
      <SectionHeading>API Reference</SectionHeading>
      <Paragraph>
        All endpoints are prefixed with the gateway base URL (port 8080). Authentication is via
        JWT Bearer token. Responses follow standard envelope: {'{ data, message, status }'}.
        Below are the SYSDOPS-specific endpoints grouped by domain.
      </Paragraph>

      <div className="ads-alert ads-alert--info" style={{ marginBottom: 16 }}>
        Click each section to expand the endpoint list. Methods are colour-coded:
        <span style={{ marginLeft: 8 }}>
          <span style={{ display: 'inline-block', padding: '1px 6px', borderRadius: 3, background: C.success, color: C.white, fontSize: 10, fontWeight: 700, marginRight: 4 }}>GET</span>
          <span style={{ display: 'inline-block', padding: '1px 6px', borderRadius: 3, background: C.brand, color: C.white, fontSize: 10, fontWeight: 700, marginRight: 4 }}>POST</span>
          <span style={{ display: 'inline-block', padding: '1px 6px', borderRadius: 3, background: C.warning, color: C.white, fontSize: 10, fontWeight: 700, marginRight: 4 }}>PUT</span>
          <span style={{ display: 'inline-block', padding: '1px 6px', borderRadius: 3, background: C.danger, color: C.white, fontSize: 10, fontWeight: 700 }}>DELETE</span>
        </span>
      </div>

      {/* Master Data */}
      <CollapsibleSection title="Master Data (admin-service)" badge="28 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/admin/master-data/categories" desc="List all master data categories" />
            <EndpointRow method="POST" path="/api/admin/master-data/categories" desc="Create a new category" />
            <EndpointRow method="GET" path="/api/admin/master-data/categories/{id}" desc="Get category by ID" />
            <EndpointRow method="PUT" path="/api/admin/master-data/categories/{id}" desc="Update category" />
            <EndpointRow method="DELETE" path="/api/admin/master-data/categories/{id}" desc="Delete category" />
            <EndpointRow method="GET" path="/api/admin/master-data/categories/{id}/values" desc="List values for a category" />
            <EndpointRow method="POST" path="/api/admin/master-data/categories/{id}/values" desc="Add value to category" />
            <EndpointRow method="PUT" path="/api/admin/master-data/values/{id}" desc="Update a master data value" />
            <EndpointRow method="DELETE" path="/api/admin/master-data/values/{id}" desc="Delete a master data value" />
            <EndpointRow method="GET" path="/api/admin/master-data/values/search" desc="Search values across categories" />
            <EndpointRow method="POST" path="/api/admin/master-data/values/bulk" desc="Bulk create/update values" />
            <EndpointRow method="GET" path="/api/admin/issue-types" desc="List all issue types" />
            <EndpointRow method="POST" path="/api/admin/issue-types" desc="Create issue type" />
            <EndpointRow method="GET" path="/api/admin/statuses" desc="List all statuses" />
            <EndpointRow method="POST" path="/api/admin/statuses" desc="Create status" />
            <EndpointRow method="GET" path="/api/admin/priorities" desc="List all priorities" />
            <EndpointRow method="POST" path="/api/admin/priorities" desc="Create priority" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* VVO Management */}
      <CollapsibleSection title="VVO Management (test-service)" badge="12 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/vvo" desc="List VVOs with filtering and pagination" />
            <EndpointRow method="POST" path="/api/vvo" desc="Create a new VVO definition" />
            <EndpointRow method="GET" path="/api/vvo/{id}" desc="Get VVO by ID with full detail" />
            <EndpointRow method="PUT" path="/api/vvo/{id}" desc="Update VVO fields" />
            <EndpointRow method="DELETE" path="/api/vvo/{id}" desc="Delete VVO (only if NEW)" />
            <EndpointRow method="POST" path="/api/vvo/{id}/transition" desc="Execute workflow transition" />
            <EndpointRow method="GET" path="/api/vvo/{id}/transitions" desc="Get available transitions for current state" />
            <EndpointRow method="GET" path="/api/vvo/{id}/history" desc="Get VVO change history / audit log" />
            <EndpointRow method="POST" path="/api/vvo/{id}/link" desc="Link VVO to test issue or other entity" />
            <EndpointRow method="GET" path="/api/vvo/{id}/links" desc="Get all traceability links for a VVO" />
            <EndpointRow method="POST" path="/api/vvo/import" desc="Bulk import VVOs from CSV/XLSX" />
            <EndpointRow method="GET" path="/api/vvo/export" desc="Export VVOs to configured format" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* HLVVO */}
      <CollapsibleSection title="HLVVO Management (test-service)" badge="5 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/hlvvo" desc="List HLVVOs with child counts" />
            <EndpointRow method="POST" path="/api/hlvvo" desc="Create HLVVO parent grouping" />
            <EndpointRow method="GET" path="/api/hlvvo/{id}" desc="Get HLVVO with children" />
            <EndpointRow method="PUT" path="/api/hlvvo/{id}" desc="Update HLVVO" />
            <EndpointRow method="DELETE" path="/api/hlvvo/{id}" desc="Delete HLVVO" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* Baseline */}
      <CollapsibleSection title="Baseline Management (test-service)" badge="7 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/vvo/baseline" desc="List baselines for project" />
            <EndpointRow method="POST" path="/api/vvo/baseline" desc="Create new baseline" />
            <EndpointRow method="GET" path="/api/vvo/baseline/{id}" desc="Get baseline detail with snapshot" />
            <EndpointRow method="POST" path="/api/vvo/baseline/{id}/snapshot" desc="Take field snapshot" />
            <EndpointRow method="PUT" path="/api/vvo/baseline/{id}/finalise" desc="Finalise and lock baseline" />
            <EndpointRow method="GET" path="/api/vvo/baseline/{id}/compare/{otherId}" desc="Compare two baselines" />
            <EndpointRow method="DELETE" path="/api/vvo/baseline/{id}" desc="Delete baseline (only if not finalised)" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* TechEvent */}
      <CollapsibleSection title="TechEvent / M1668 (test-service)" badge="12 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/tech-events" desc="List tech events with filters" />
            <EndpointRow method="POST" path="/api/tech-events" desc="Create tech event (M1668 form)" />
            <EndpointRow method="GET" path="/api/tech-events/{id}" desc="Get tech event detail" />
            <EndpointRow method="PUT" path="/api/tech-events/{id}" desc="Update tech event fields" />
            <EndpointRow method="DELETE" path="/api/tech-events/{id}" desc="Delete tech event (if OPEN)" />
            <EndpointRow method="POST" path="/api/tech-events/{id}/transition" desc="Execute state transition" />
            <EndpointRow method="GET" path="/api/tech-events/{id}/transitions" desc="Get available transitions" />
            <EndpointRow method="GET" path="/api/tech-events/{id}/history" desc="Get audit trail" />
            <EndpointRow method="POST" path="/api/tech-events/{id}/bench-defect" desc="Create linked BenchDefect" />
            <EndpointRow method="POST" path="/api/tech-events/{id}/problem-report" desc="Create linked ProblemReport" />
            <EndpointRow method="GET" path="/api/tech-events/{id}/attachments" desc="List attachments" />
            <EndpointRow method="POST" path="/api/tech-events/{id}/attachments" desc="Upload attachment" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* BenchDefect */}
      <CollapsibleSection title="BenchDefect (test-service)" badge="5 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/bench-defects" desc="List bench defects" />
            <EndpointRow method="GET" path="/api/bench-defects/{id}" desc="Get bench defect detail" />
            <EndpointRow method="PUT" path="/api/bench-defects/{id}" desc="Update bench defect" />
            <EndpointRow method="POST" path="/api/bench-defects/{id}/transition" desc="Transition state" />
            <EndpointRow method="GET" path="/api/bench-defects/{id}/transitions" desc="Get available transitions" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* ProblemReport */}
      <CollapsibleSection title="ProblemReport (test-service)" badge="5 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/problem-reports" desc="List problem reports" />
            <EndpointRow method="GET" path="/api/problem-reports/{id}" desc="Get problem report detail" />
            <EndpointRow method="PUT" path="/api/problem-reports/{id}" desc="Update problem report" />
            <EndpointRow method="POST" path="/api/problem-reports/{id}/transition" desc="Transition state" />
            <EndpointRow method="GET" path="/api/problem-reports/{id}/transitions" desc="Get available transitions" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* Change Management */}
      <CollapsibleSection title="Change Management (issue-service)" badge="20+ endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/issues/{id}/change-card" desc="Get change card for issue" />
            <EndpointRow method="POST" path="/api/issues/{id}/change-card" desc="Create change card" />
            <EndpointRow method="PUT" path="/api/issues/{id}/change-card" desc="Update change card" />
            <EndpointRow method="GET" path="/api/issues/{id}/design-item" desc="Get design items for issue" />
            <EndpointRow method="POST" path="/api/issues/{id}/design-item" desc="Create design item" />
            <EndpointRow method="PUT" path="/api/issues/{id}/design-item/{diId}" desc="Update design item" />
            <EndpointRow method="GET" path="/api/issues/{id}/dcl" desc="Get DCL (Design Change List)" />
            <EndpointRow method="POST" path="/api/issues/{id}/dcl" desc="Create DCL entry" />
            <EndpointRow method="GET" path="/api/issues/{id}/deliverable" desc="Get deliverables" />
            <EndpointRow method="POST" path="/api/issues/{id}/deliverable" desc="Create deliverable" />
            <EndpointRow method="GET" path="/api/issues/{id}/system-standard" desc="Get system standards" />
            <EndpointRow method="POST" path="/api/issues/{id}/system-standard" desc="Link system standard" />
            <EndpointRow method="GET" path="/api/issues/{id}/review-sub-task" desc="Get review sub-tasks" />
            <EndpointRow method="POST" path="/api/issues/{id}/review-sub-task" desc="Create review sub-task" />
            <EndpointRow method="GET" path="/api/issues/{id}/modification" desc="Get modifications" />
            <EndpointRow method="POST" path="/api/issues/{id}/modification" desc="Create modification record" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* Reporting */}
      <CollapsibleSection title="V&V Reporting (test-service)" badge="7 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/vv-reports/dashboard" desc="Project V&V dashboard data" />
            <EndpointRow method="GET" path="/api/vv-reports/coverage-matrix" desc="VVO-to-test coverage matrix" />
            <EndpointRow method="GET" path="/api/vv-reports/status-distribution" desc="VVO status distribution" />
            <EndpointRow method="GET" path="/api/vv-reports/tech-event-trend" desc="TechEvent creation trend" />
            <EndpointRow method="GET" path="/api/vv-reports/defect-summary" desc="Defect summary by severity" />
            <EndpointRow method="GET" path="/api/vv-reports/execution-progress" desc="Test execution progress" />
            <EndpointRow method="POST" path="/api/vv-reports/generate" desc="Generate custom report" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* Document Export */}
      <CollapsibleSection title="Document Export (test-service)" badge="7 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/export-templates" desc="List export templates" />
            <EndpointRow method="POST" path="/api/export-templates" desc="Create export template" />
            <EndpointRow method="GET" path="/api/export-templates/{id}" desc="Get template detail" />
            <EndpointRow method="PUT" path="/api/export-templates/{id}" desc="Update template" />
            <EndpointRow method="DELETE" path="/api/export-templates/{id}" desc="Delete template" />
            <EndpointRow method="POST" path="/api/export-templates/{id}/preview" desc="Preview export output" />
            <EndpointRow method="POST" path="/api/export-templates/{id}/export" desc="Execute export (returns file)" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* Automation */}
      <CollapsibleSection title="Automation Rules (workflow-service)" badge="9 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/automation/rules" desc="List automation rules" />
            <EndpointRow method="POST" path="/api/automation/rules" desc="Create automation rule" />
            <EndpointRow method="GET" path="/api/automation/rules/{id}" desc="Get rule detail" />
            <EndpointRow method="PUT" path="/api/automation/rules/{id}" desc="Update rule" />
            <EndpointRow method="DELETE" path="/api/automation/rules/{id}" desc="Delete rule" />
            <EndpointRow method="POST" path="/api/automation/rules/{id}/enable" desc="Enable rule" />
            <EndpointRow method="POST" path="/api/automation/rules/{id}/disable" desc="Disable rule" />
            <EndpointRow method="GET" path="/api/automation/rules/{id}/logs" desc="Get execution logs" />
            <EndpointRow method="POST" path="/api/automation/rules/{id}/test" desc="Dry-run rule against sample" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* Assets */}
      <CollapsibleSection title="Asset Management (admin-service)" badge="10+ endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/admin/assets" desc="List assets with filters" />
            <EndpointRow method="POST" path="/api/admin/assets" desc="Create asset" />
            <EndpointRow method="GET" path="/api/admin/assets/{id}" desc="Get asset detail" />
            <EndpointRow method="PUT" path="/api/admin/assets/{id}" desc="Update asset" />
            <EndpointRow method="DELETE" path="/api/admin/assets/{id}" desc="Delete asset" />
            <EndpointRow method="GET" path="/api/admin/asset-types" desc="List asset types" />
            <EndpointRow method="POST" path="/api/admin/asset-types" desc="Create asset type" />
            <EndpointRow method="POST" path="/api/admin/assets/{id}/link/{issueId}" desc="Link asset to issue" />
            <EndpointRow method="DELETE" path="/api/admin/assets/{id}/link/{issueId}" desc="Unlink asset from issue" />
            <EndpointRow method="GET" path="/api/admin/assets/{id}/links" desc="Get issues linked to asset" />
          </tbody>
        </table>
      </CollapsibleSection>

      {/* Campaigns */}
      <CollapsibleSection title="Campaign Management (test-service)" badge="6 endpoints">
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <tbody>
            <EndpointRow method="GET" path="/api/campaigns" desc="List campaigns with filters" />
            <EndpointRow method="POST" path="/api/campaigns" desc="Create campaign" />
            <EndpointRow method="GET" path="/api/campaigns/{id}" desc="Get campaign with executions" />
            <EndpointRow method="PUT" path="/api/campaigns/{id}" desc="Update campaign" />
            <EndpointRow method="POST" path="/api/campaigns/{id}/execute" desc="Start campaign execution" />
            <EndpointRow method="GET" path="/api/campaigns/{id}/results" desc="Get execution results summary" />
          </tbody>
        </table>
      </CollapsibleSection>
    </div>
  );
}

/* =========================================================================
   Main Component
   ========================================================================= */

export default function ArchitecturePage() {
  const [activeTab, setActiveTab] = useState<TabName>('System Overview');

  function renderContent() {
    switch (activeTab) {
      case 'System Overview':
        return renderSystemOverview();
      case 'Service Map':
        return renderServiceMap();
      case 'Domain Model':
        return renderDomainModel();
      case 'Data Flow':
        return renderDataFlow();
      case 'VVO Lifecycle':
        return renderVvoLifecycle();
      case 'Defect Management':
        return renderDefectManagement();
      case 'Plugin Matrix':
        return renderPluginMatrix();
      case 'API Reference':
        return renderApiReference();
      default:
        return null;
    }
  }

  return (
    <div className="ads-page">
      {/* Header */}
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">Architecture Documentation</h1>
          <p className="ads-page-subtitle">
            SYSDOPS Aircraft Design System -- technical reference for architects and tech leads
          </p>
        </div>
        <div className="ads-toolbar">
          <span style={{ fontSize: 12, color: C.subtle, padding: '6px 12px', background: C.bg, borderRadius: 4 }}>
            Last updated: July 2026
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div className="ads-tabs">
        {TABS.map((tab) => (
          <button
            key={tab}
            className={`ads-tab${activeTab === tab ? ' ads-tab--active' : ''}`}
            onClick={() => setActiveTab(tab)}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      <div>{renderContent()}</div>
    </div>
  );
}
