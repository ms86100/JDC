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
  'Cluster Architecture',
  'Enterprise Hardening',
  'Service Map',
  'Issue & Workflow',
  'Domain Model',
  'Data Flow',
  'VVO Lifecycle',
  'Defect Management',
  'Plugin Matrix',
  'SIL Alternative',
  'API Reference',
  'Auth & Users',
  'Sprint & Search',
  'Database Schema',
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

/* ── Tab 2: Cluster Architecture ── */
function renderClusterArchitecture() {
  const clusterLayers = [
    { layer: 'Load Balancer', desc: 'API Gateway receives all traffic, routes via Docker DNS round-robin', color: C.dark, items: ['Health checks', 'Rate limiting (Redis-backed)', 'JWT validation', 'Path-based routing'] },
    { layer: 'Application Nodes', desc: 'Each service instance is a Node — fully capable of serving requests independently', color: C.brand, items: ['Stateless JWT auth', 'REST APIs', 'Business logic', 'ShedLock-coordinated schedulers', 'Caffeine L1 + Redis L2 caching'] },
    { layer: 'Shared State', desc: 'Single source of truth shared by all nodes', color: C.success, items: ['PostgreSQL (21 schemas)', 'Redis (cache, pub/sub, locking)', 'MinIO (S3-compatible storage)'] },
  ];

  const guardrails = [
    { rule: 'R1', title: 'No @Scheduled without @SchedulerLock', desc: 'Prevents duplicate execution across nodes' },
    { rule: 'R2', title: 'No in-memory state for coordination', desc: 'ConcurrentHashMap is JVM-local, invisible to other nodes' },
    { rule: 'R3', title: 'No direct filesystem for user data', desc: 'Use StorageProvider (MinIO/S3) for shared access' },
    { rule: 'R4', title: 'WebSocket must use ClusterEventBus', desc: 'Redis pub/sub relays messages across all nodes' },
    { rule: 'R5', title: 'No container_name on app services', desc: 'Prevents docker compose --scale' },
    { rule: 'R6', title: 'No fixed host port mappings', desc: 'Only gateway (8080) and frontend (3000) expose ports' },
    { rule: 'R7', title: 'Outbox pollers must be idempotent', desc: 'At-least-once delivery means possible re-processing' },
    { rule: 'R8', title: 'Cache evictions must propagate', desc: 'Use ClusterCacheManager, not bare Caffeine' },
  ];

  const components = [
    { name: 'DistributedLockService', pkg: 'com.jira.cluster.lock', desc: 'Database or Redis-backed distributed locking' },
    { name: 'ShedLockAutoConfiguration', pkg: 'com.jira.cluster.scheduler', desc: 'JDBC-based scheduler lock provider' },
    { name: 'StorageProvider', pkg: 'com.jira.cluster.storage', desc: 'Local / S3 (MinIO) file storage abstraction' },
    { name: 'ClusterCacheManager', pkg: 'com.jira.cluster.cache', desc: 'Caffeine L1 + Redis L2 with cross-node invalidation' },
    { name: 'ClusterEventBus', pkg: 'com.jira.cluster.event', desc: 'Redis pub/sub for real-time cross-node messaging' },
  ];

  return (
    <div>
      <SectionHeading>Cluster Architecture — Multi-Node Deployment</SectionHeading>
      <Paragraph>
        The platform supports Jira Data Center-style clustering: multiple identical application nodes behind a load
        balancer, sharing a database and object storage. Each service can be independently scaled — Docker DNS
        automatically round-robins requests across all instances. The jira-cluster-commons library provides
        distributed locking, coordinated scheduling, tiered caching, shared storage, and a cluster event bus.
      </Paragraph>

      {/* Cluster Diagram */}
      <div style={{ position: 'relative', padding: 20, background: C.bg, borderRadius: 8, marginBottom: 24 }}>
        <div style={{ textAlign: 'center', fontSize: 13, fontWeight: 600, color: C.dark, marginBottom: 12 }}>
          Multi-Node Cluster Topology
        </div>

        {/* Users */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 8 }}>
          <DiagramBox label="Users / Browser" color={C.subtle} width={200} />
        </div>
        <DownArrow />

        {/* Load Balancer */}
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 8 }}>
          <DiagramBox label="Gateway (Load Balancer)" sub="Port 8080 -- DNS round-robin" color={C.dark} width={280} />
        </div>
        <DownArrow />

        {/* Application Nodes */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 8, marginBottom: 8 }}>
          <DiagramBox label="Issue Node 1" sub=":8084" color={C.brand} width={130} height={50} fontSize={11} />
          <DiagramBox label="Issue Node 2" sub=":8084" color={C.brand} width={130} height={50} fontSize={11} />
          <DiagramBox label="Issue Node 3" sub=":8084" color={C.brand} width={130} height={50} fontSize={11} />
          <DiagramBox label="Auth Node 1" sub=":8081" color={C.purple} width={130} height={50} fontSize={11} />
          <DiagramBox label="Auth Node 2" sub=":8081" color={C.purple} width={130} height={50} fontSize={11} />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginBottom: 8 }}>
          <DiagramBox label="Workflow x2" sub=":8085" color={C.danger} width={140} height={50} fontSize={11} />
          <DiagramBox label="Notification x2" sub=":8087" color={C.warning} width={140} height={50} fontSize={11} />
          <DiagramBox label="Sprint x1" sub=":8091" color={C.teal} width={140} height={50} fontSize={11} />
          <DiagramBox label="Search x1" sub=":8088" color={C.teal} width={140} height={50} fontSize={11} />
        </div>
        <DownArrow />

        {/* Shared State */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: 16 }}>
          <DiagramBox label="PostgreSQL" sub="21 schemas" color={C.dbBg} width={180} />
          <DiagramBox label="Redis" sub="Cache + Pub/Sub + Locks" color={C.danger} width={200} />
          <DiagramBox label="MinIO" sub="S3-compatible storage" color={C.success} width={180} />
        </div>
      </div>

      {/* Cluster Layers */}
      <div className="ads-card" style={{ marginBottom: 16 }}>
        <h4 className="ads-card-title">Cluster Layers</h4>
        {clusterLayers.map((l) => (
          <div key={l.layer} style={{ marginBottom: 16, padding: 12, background: C.bg, borderRadius: 6, borderLeft: `4px solid ${l.color}` }}>
            <div style={{ fontWeight: 600, fontSize: 14, color: l.color }}>{l.layer}</div>
            <div style={{ fontSize: 12, color: C.subtle, margin: '4px 0 8px' }}>{l.desc}</div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {l.items.map((item) => (
                <span key={item} style={{ fontSize: 11, background: C.white, border: `1px solid ${C.border}`, borderRadius: 4, padding: '2px 8px', color: C.dark }}>{item}</span>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Key stats */}
      <div className="ads-stats">
        <div className="ads-stat ads-stat--brand">
          <span className="ads-stat-value">23</span>
          <span className="ads-stat-label">Cluster-Safe Schedulers</span>
        </div>
        <div className="ads-stat ads-stat--success">
          <span className="ads-stat-value">8</span>
          <span className="ads-stat-label">No-Regression Rules</span>
        </div>
        <div className="ads-stat ads-stat--warning">
          <span className="ads-stat-value">5</span>
          <span className="ads-stat-label">ArchUnit Guards</span>
        </div>
        <div className="ads-stat">
          <span className="ads-stat-value">3</span>
          <span className="ads-stat-label">Shared Infra Services</span>
        </div>
      </div>

      {/* Cluster Commons Library */}
      <div className="ads-card" style={{ marginTop: 8 }}>
        <h4 className="ads-card-title">jira-cluster-commons Library</h4>
        <Paragraph>
          Shared Maven module providing cluster primitives to all services. Auto-configured via Spring Boot
          starters — services only need to add the dependency.
        </Paragraph>
        <table className="ads-table">
          <thead>
            <tr><th>Component</th><th>Package</th><th>Purpose</th></tr>
          </thead>
          <tbody>
            {components.map((c) => (
              <tr key={c.name}>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>{c.name}</td>
                <td style={{ fontFamily: 'monospace', fontSize: 11, color: C.subtle }}>{c.pkg}</td>
                <td style={{ fontSize: 12 }}>{c.desc}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* No-Regression Guardrails */}
      <div className="ads-card" style={{ marginTop: 16 }}>
        <h4 className="ads-card-title">No-Regression Guardrails (8 Rules)</h4>
        <Paragraph>
          Four layers of defense prevent cluster-unsafe code: CLAUDE.md rules (AI-enforced), ArchUnit tests
          (build fails), Docker Compose validation script (CI), and PR review checklist (human).
        </Paragraph>
        <table className="ads-table">
          <thead>
            <tr><th>Rule</th><th>Constraint</th><th>Why</th></tr>
          </thead>
          <tbody>
            {guardrails.map((g) => (
              <tr key={g.rule}>
                <td style={{ fontWeight: 700, color: C.brand, textAlign: 'center' }}>{g.rule}</td>
                <td style={{ fontWeight: 600, fontSize: 12 }}>{g.title}</td>
                <td style={{ fontSize: 12, color: C.subtle }}>{g.desc}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Request Lifecycle */}
      <div className="ads-card" style={{ marginTop: 16 }}>
        <h4 className="ads-card-title">Request Lifecycle (Clustered)</h4>
        <div style={{ background: C.bg, borderRadius: 6, padding: 16, fontFamily: 'monospace', fontSize: 12, lineHeight: 1.8 }}>
          <div>1. User opens issue in browser</div>
          <div style={{ color: C.subtle, paddingLeft: 16 }}>|</div>
          <div>2. Load Balancer (Gateway) validates JWT <span style={{ color: C.success }}>(stateless -- any node works)</span></div>
          <div style={{ color: C.subtle, paddingLeft: 16 }}>|</div>
          <div>3. Docker DNS round-robin routes to Issue-Service-2</div>
          <div style={{ color: C.subtle, paddingLeft: 16 }}>|</div>
          <div>4. Permission check + business logic</div>
          <div style={{ color: C.subtle, paddingLeft: 16 }}>|</div>
          <div>5. Cache miss? <span style={{ color: C.brand }}>PostgreSQL -&gt; Caffeine L1 + Redis L2</span></div>
          <div>{'   '}Cache hit? <span style={{ color: C.success }}>Return from Caffeine L1 (local, fast)</span></div>
          <div style={{ color: C.subtle, paddingLeft: 16 }}>|</div>
          <div>6. WebSocket update? <span style={{ color: C.danger }}>ClusterEventBus (Redis pub/sub) -&gt; all clients</span></div>
          <div style={{ color: C.subtle, paddingLeft: 16 }}>|</div>
          <div>7. Response back through Gateway</div>
        </div>
      </div>

      {/* Scaling Commands */}
      <div className="ads-card" style={{ marginTop: 16 }}>
        <h4 className="ads-card-title">Scaling Commands</h4>
        <div style={{ background: C.dbBg, borderRadius: 6, padding: 16, fontFamily: 'monospace', fontSize: 12, color: '#a5d6a7', lineHeight: 1.8 }}>
          <div style={{ color: '#888' }}># Single-node (default)</div>
          <div>docker compose up --build</div>
          <div style={{ marginTop: 8, color: '#888' }}># Multi-node cluster test (predefined replicas)</div>
          <div>docker compose -f docker-compose.yml -f docker-compose.cluster.yml up</div>
          <div style={{ marginTop: 8, color: '#888' }}># Manual scaling</div>
          <div>docker compose up --scale issue-service=3 --scale gateway=2</div>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 3: Enterprise Hardening ── */
function renderEnterpriseHardening() {
  const securityItems = [
    { area: 'CORS', before: 'Wildcard * (any origin)', after: 'Configurable ${CORS_ALLOWED_ORIGINS}', severity: 'CRITICAL' },
    { area: 'JWT Tokens', before: '24h access, 7d refresh', after: '15min access, 8h refresh', severity: 'CRITICAL' },
    { area: 'JWT Secret', before: 'Hardcoded in YAML', after: 'Env-var only, no defaults in code', severity: 'CRITICAL' },
    { area: 'Permissions', before: 'FAILOPEN=true', after: 'FAILOPEN=false (fail-closed)', severity: 'CRITICAL' },
    { area: 'Redis', before: 'No authentication', after: '--requirepass enabled', severity: 'HIGH' },
    { area: 'Actuator', before: 'Public metrics+health', after: 'health,info,prometheus; show-details=when-authorized', severity: 'HIGH' },
    { area: 'Stack Traces', before: 'Exposed to clients', after: 'include-stacktrace=never', severity: 'MEDIUM' },
  ];

  const perfItems = [
    { area: 'HikariCP Pool', before: '10 connections (all services)', after: '50 (hot) / 30 (medium) / 20 (light)', severity: 'CRITICAL' },
    { area: 'Hibernate Batch', before: 'No batch config', after: 'batch_size=50, order_inserts/updates', severity: 'CRITICAL' },
    { area: 'Issue Entity Fetch', before: '3x EAGER JOIN on every query', after: 'LAZY fetch + on-demand EntityGraph', severity: 'CRITICAL' },
    { area: 'Thread Pools', before: '4x unbounded CachedThreadPool', after: 'Bounded FixedThreadPool (CPU*2, max 20)', severity: 'HIGH' },
    { area: 'Tomcat', before: 'Spring defaults', after: '200 threads, 8192 connections, 100 accept', severity: 'HIGH' },
    { area: 'Slow Query Log', before: 'Disabled', after: 'LOG_QUERIES_SLOWER_THAN_MS=500', severity: 'HIGH' },
    { area: 'Missing Indexes', before: 'No index on priority/type', after: 'idx_issue_priority, idx_issue_issue_type', severity: 'HIGH' },
  ];

  const resilienceItems = [
    { area: 'Circuit Breakers', before: '2/22 services', after: 'All services via Resilience4j (50% threshold, 30s open)', severity: 'CRITICAL' },
    { area: 'Distributed Tracing', before: 'Zipkin deployed but unused', after: 'Micrometer + OpenTelemetry + Zipkin export (W3C propagation)', severity: 'CRITICAL' },
    { area: 'Structured Logging', before: 'Plain text, no correlation', after: 'JSON (LogstashEncoder) + X-Correlation-ID in MDC', severity: 'HIGH' },
    { area: 'Retry Policies', before: 'Only migration-service', after: 'All services: 3 attempts, 500ms backoff', severity: 'HIGH' },
    { area: 'Graceful Shutdown', before: 'Not configured', after: 'server.shutdown=graceful + 30s drain', severity: 'HIGH' },
    { area: 'Health Checks', before: '2 custom indicators', after: 'Redis + Storage health auto-configured', severity: 'HIGH' },
    { area: 'Async Errors', before: 'Silently swallowed', after: 'ClusterAsyncExceptionHandler logs all failures', severity: 'HIGH' },
    { area: 'Idempotency', before: 'Workflow transitions only', after: 'Redis-backed IdempotencyService for all POST endpoints', severity: 'HIGH' },
  ];

  const severityColor = (s: string) => s === 'CRITICAL' ? C.danger : s === 'HIGH' ? C.warning : C.teal;

  const renderTable = (title: string, items: typeof securityItems, icon: string) => (
    <div className="ads-card" style={{ marginBottom: 16 }}>
      <h4 className="ads-card-title">{icon} {title}</h4>
      <table className="ads-table">
        <thead>
          <tr><th>Area</th><th>Before</th><th>After</th><th>Severity</th></tr>
        </thead>
        <tbody>
          {items.map((item) => (
            <tr key={item.area}>
              <td style={{ fontWeight: 600, fontSize: 12 }}>{item.area}</td>
              <td style={{ fontSize: 12, color: C.danger, textDecoration: 'line-through', opacity: 0.7 }}>{item.before}</td>
              <td style={{ fontSize: 12, color: C.success, fontWeight: 500 }}>{item.after}</td>
              <td style={{ textAlign: 'center' }}>
                <span style={{ fontSize: 10, fontWeight: 700, color: severityColor(item.severity), background: `${severityColor(item.severity)}15`, padding: '2px 8px', borderRadius: 4 }}>{item.severity}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  return (
    <div>
      <SectionHeading>Enterprise Hardening -- 4K Concurrent Users</SectionHeading>
      <Paragraph>
        Full security, performance, and resilience audit identified 15 CRITICAL, 33 HIGH, 20 MEDIUM issues
        across 22 microservices. All CRITICAL and HIGH findings have been remediated. The platform is now
        hardened for enterprise deployment supporting 4,000+ concurrent users with millions of records.
      </Paragraph>

      <div className="ads-stats">
        <div className="ads-stat" style={{ borderLeft: `4px solid ${C.danger}` }}>
          <span className="ads-stat-value">15</span>
          <span className="ads-stat-label">CRITICAL Fixed</span>
        </div>
        <div className="ads-stat" style={{ borderLeft: `4px solid ${C.warning}` }}>
          <span className="ads-stat-value">33</span>
          <span className="ads-stat-label">HIGH Fixed</span>
        </div>
        <div className="ads-stat" style={{ borderLeft: `4px solid ${C.success}` }}>
          <span className="ads-stat-value">68</span>
          <span className="ads-stat-label">Total Findings</span>
        </div>
        <div className="ads-stat" style={{ borderLeft: `4px solid ${C.brand}` }}>
          <span className="ads-stat-value">4K+</span>
          <span className="ads-stat-label">Concurrent Users</span>
        </div>
      </div>

      {renderTable('Security Hardening', securityItems, '\u{1F512}')}
      {renderTable('Performance Optimization', perfItems, '⚡')}
      {renderTable('Resilience & Observability', resilienceItems, '\u{1F6E1}')}

      <div className="ads-card" style={{ marginTop: 16 }}>
        <h4 className="ads-card-title">Cluster Commons Library (jira-cluster-commons)</h4>
        <Paragraph>
          All enterprise infrastructure is centralized in the shared library. Services get circuit breakers,
          tracing, structured logging, idempotency, and health indicators automatically by depending on cluster-commons.
        </Paragraph>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
          {[
            { name: 'ResilienceAutoConfiguration', desc: 'CircuitBreaker + Retry registries' },
            { name: 'CorrelationIdFilter', desc: 'X-Correlation-ID propagation + MDC' },
            { name: 'logback-spring.xml', desc: 'JSON structured logging (docker profile)' },
            { name: 'IdempotencyService', desc: 'Redis-backed POST deduplication' },
            { name: 'RedisHealthIndicator', desc: 'Redis connectivity health check' },
            { name: 'ClusterAsyncExceptionHandler', desc: 'Catches all @Async failures' },
          ].map((c) => (
            <div key={c.name} style={{ padding: 10, background: C.bg, borderRadius: 6, border: `1px solid ${C.border}` }}>
              <div style={{ fontWeight: 600, fontSize: 12, fontFamily: 'monospace', color: C.brand }}>{c.name}</div>
              <div style={{ fontSize: 11, color: C.subtle, marginTop: 4 }}>{c.desc}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ── Tab 4: Service Map ── */
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

/* ── Tab 3: Issue & Workflow Architecture ── */
function renderIssueWorkflow() {
  const issueFeatures = [
    { area: 'Core Issue CRUD', entities: 'Issue, IssueType, IssueStatus, IssuePriority, Resolution', service: 'issue-service', endpoints: 'POST/GET/PUT/DELETE /api/issues, GET /api/issues/{id}' },
    { area: 'Custom Fields', entities: 'CustomFieldDefinition, CustomFieldValue, CustomFieldOption', service: 'issue-service', endpoints: '/api/fields — 20+ field type handlers (text, select, cascading, user picker, etc.)' },
    { area: 'Issue Linking', entities: 'IssueLink, IssueLinkType (blocks, relates, clones, duplicates, causes, requires)', service: 'issue-service', endpoints: 'POST/GET/DELETE /api/issues/links' },
    { area: 'Change History', entities: 'ChangeGroup, ChangeItem — field-level audit trail', service: 'issue-service', endpoints: 'GET /api/issues/{id}/history — every field change tracked with old/new values' },
    { area: 'Versions & Components', entities: 'ProjectVersion (Fix/Affects), ProjectComponent', service: 'version-service, component-service', endpoints: '/api/versions, /api/components — with issue count aggregation' },
    { area: 'Epics', entities: 'Epic, EpicIssue, EpicProgressHistory', service: 'issue-service', endpoints: '/api/epics — progress tracking with story point aggregation' },
    { area: 'Time Tracking', entities: 'Worklog (originalEstimate, remainingEstimate, timeSpent)', service: 'issue-service', endpoints: '/api/worklogs — per-issue time logging' },
    { area: 'Watchers & Votes', entities: 'Watcher, Vote — with trigger-based counters', service: 'issue-service', endpoints: '/api/issues/{id}/watchers, /api/issues/{id}/votes' },
    { area: 'Attachments', entities: 'File-system storage with metadata', service: 'attachment-service', endpoints: '/api/attachments — upload/download/delete' },
    { area: 'Comments', entities: 'Comment with author, body, timestamps', service: 'comment-service', endpoints: '/api/comments — threaded discussion on issues' },
    { area: 'Labels & Security', entities: 'Label, SecurityLevel — access control per issue', service: 'issue-service', endpoints: '/api/issues/{id}/labels, security levels per scheme' },
    { area: 'Bulk Operations', entities: 'BulkIssueOperation — move, transition, edit, delete', service: 'issue-service', endpoints: 'POST /api/issues/bulk — batch processing' },
    { area: 'Issue Import/Export', entities: 'ImportService, IssueExportService — CSV/JSON', service: 'issue-service', endpoints: '/api/import, /api/issues/export' },
    { area: 'Dev Info Integration', entities: 'DevInfoCommit, DevInfoBranch, DevInfoPullRequest, DevInfoBuild', service: 'issue-service', endpoints: '/api/issues/{id}/dev-info — Git commit/branch/PR/build linkage' },
  ];

  const workflowFeatures = [
    { area: 'Workflow Definition', entities: 'Workflow, WorkflowStatus, WorkflowTransition, WorkflowVersion', detail: 'Named workflows with ordered statuses and directed transitions between them. Supports drafts, versioning, and sharing across projects.' },
    { area: 'Conditions (16 types)', entities: 'WorkflowCondition — PERMISSION, USER_GROUP, FIELD_VALUE, FIELD_CHANGED, PREVIOUS_STATUS, USER_IS_REPORTER/ASSIGNEE, LINKED_ISSUE_STATUS, SUBTASK_STATUS, SPRINT_STATUS, SCRIPT, AND/OR/NOT', detail: 'Gate transition visibility. Conditions are evaluated before showing a transition as available. Supports negate flag and nested AND/OR/NOT logic.' },
    { area: 'Validators (11 types)', entities: 'WorkflowValidator — FIELD_REQUIRED, FIELD_VALUE, REGEX, DATE_RANGE, USER_PERMISSION, SCRIPT, SUBTASK_RESOLUTION, LINKED_ISSUE_RESOLUTION, ATTACHMENT_COUNT, COMMENT_REQUIRED, TIME_TRACKING', detail: 'Block transition execution if data is invalid. Return field-level errors to the UI. Evaluated after conditions pass.' },
    { area: 'Post-Functions (30+ types)', entities: 'WorkflowPostFunction — ASSIGN_TO_*, SET_FIELD_VALUE, COPY_VALUE, CREATE_SUBTASK, CLONE_ISSUE, LINK_ISSUE, SEND_EMAIL, TRIGGER_WEBHOOK, AUTO_TRANSITION, SCRIPT_POST_FUNCTION, etc.', detail: 'Execute side effects after transition. Essential chain (status change, history, reindex, event fire) runs first, then configured post-functions in sequence order.' },
    { area: 'Transition Screens', entities: 'WorkflowScreen, WorkflowScreenTab, WorkflowScreenField', detail: 'Mandatory/optional field prompts shown during transition. Screen input validated by validators.' },
    { area: 'Workflow Schemes', entities: 'WorkflowScheme, WorkflowSchemeMapping — maps issue types to workflows within a project', detail: 'A project references a scheme, which maps each issue type to a specific workflow. Default workflow used for unmapped types.' },
    { area: 'Triggers (15 types)', entities: 'WorkflowTransitionTrigger — FIELD_CHANGE, DATE_BASED, COMMENT_ADDED, LINK_ADDED, STATUS_CHANGE, EXTERNAL_WEBHOOK, API_TRIGGER, SPRINT_START/COMPLETE, BUILD_SUCCESS, PULL_REQUEST', detail: 'Auto-fire transitions based on events. Includes cooldown and max fire count controls.' },
    { area: 'GraalJS Scripting', entities: 'ScriptDefinition, ScriptVersion, ScriptExecutionLog, ScriptSchedule', detail: 'ECMAScript 2022 scripts in sandboxed GraalVM engine. Types: CONDITION, VALIDATOR, POST_FUNCTION. Full JDC API (jdc.issue, jdc.project, jdc.user, jdc.search, jdc.workflow, jdc.log).' },
    { area: 'Automation Rules', entities: 'AutomationRule (trigger/conditions/actions/branch), AutomationExecutionLog', detail: 'Independent if-then rules that fire on issue events (not tied to workflow transitions). Supports FOR_EACH_LINKED_ISSUE and FOR_EACH_SUBTASK branching.' },
    { area: 'Execution Engine', entities: 'WorkflowExecutionEngine — 12-step pipeline', detail: 'Idempotency check → context resolution → project permission → status validation → optimistic lock → transition permission → conditions → validators → post-functions → history → event fire → idempotency store.' },
    { area: 'Event Outbox', entities: 'WorkflowEventOutbox, WorkflowEventPublisher, WorkflowEventOutboxProcessor', detail: 'Transactional outbox pattern for reliable event delivery. Events processed by scheduled job for downstream notification and automation triggering.' },
  ];

  const migrationFeatures = [
    { area: 'Data Center Import', entities: 'DcStagingEntry, DcUnknownCustomField — full Jira DC XML/JSON import', detail: 'Imports projects, issues, workflows, schemes, users, groups, custom fields from a Jira DC backup. Staged for review before commit.' },
    { area: 'Workflow XML Import', entities: 'WorkflowDescriptorModel, WorkflowStepModel, WorkflowActionModel, WorkflowFunctionDescriptor', detail: 'Import Jira DC workflow XML descriptors with full step/action/function/condition/validator mapping.' },
    { area: 'Field Mapping', entities: 'MigrationFieldController, CustomFieldsCompatController — maps source DC fields to target fields', detail: 'Handles field type conversion, custom field creation, and value transformation during import.' },
    { area: 'Import Wizard', entities: 'ImportWizardController — step-by-step guided import with preview', detail: 'Multi-step wizard: select source → map projects → map fields → preview → execute → verify.' },
    { area: 'Migration State Machine', entities: 'NodeState (IDLE, IMPORTING, VALIDATING, MAPPING, EXECUTING, COMPLETED, FAILED)', detail: 'Tracks migration job state with DLQ (dead letter queue) for failed records and SSE/WebSocket progress streaming.' },
    { area: 'Migration Health', entities: 'MigrationHealthController — monitors import job health and integrity', detail: 'Checks data integrity post-migration: orphaned issues, broken links, missing custom field values, scheme inconsistencies.' },
  ];

  return (
    <div>
      <h3 style={{ color: C.dark, marginBottom: '8px' }}>Issue Service Architecture</h3>
      <p style={{ color: C.subtle, marginBottom: '16px' }}>
        The issue-service is the central data store for all Jira issues with 48+ entities. It manages the full issue lifecycle including custom fields, linking, versioning, time tracking, and integrates with the workflow engine for status transitions.
      </p>

      {/* Issue-Workflow-Migration Relationship Diagram */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">Service Relationship: Issue ↔ Workflow ↔ Migration</div>
        <div style={{ padding: '16px', background: C.bg, borderRadius: '8px' }}>
          <svg viewBox="0 0 900 400" style={{ width: '100%', maxHeight: '400px' }}>
            {/* Issue Service Box */}
            <rect x="20" y="30" width="250" height="340" rx="8" fill="white" stroke={C.success} strokeWidth="2"/>
            <rect x="20" y="30" width="250" height="32" rx="8" fill={C.success}/>
            <text x="145" y="52" textAnchor="middle" fill="white" fontSize="13" fontWeight="700">issue-service (8084)</text>
            {['Issue CRUD + 48 entities', 'Custom Fields (20+ types)', 'Issue Links (7 link types)', 'Change History (field audit)', 'Versions & Components', 'Epics & Story Points', 'Time Tracking (Worklogs)', 'Bulk Operations', 'Dev Info (Git integration)', 'ChangeCard / DI / DCL', 'SystemStandard / MOD'].map((t, i) => (
              <text key={i} x="35" y={80 + i * 24} fontSize="11" fill={C.dark}>{t}</text>
            ))}

            {/* Workflow Service Box */}
            <rect x="330" y="30" width="250" height="340" rx="8" fill="white" stroke={C.danger} strokeWidth="2"/>
            <rect x="330" y="30" width="250" height="32" rx="8" fill={C.danger}/>
            <text x="455" y="52" textAnchor="middle" fill="white" fontSize="13" fontWeight="700">workflow-service (8085)</text>
            {['Workflow Engine (12-step)', 'Conditions (16 types)', 'Validators (11 types)', 'Post-Functions (30+ types)', 'GraalJS Scripting', 'Automation Rules', 'Transition Screens', 'Workflow Schemes', 'Triggers (15 event types)', 'Event Outbox Pattern', 'Script Console & REPL'].map((t, i) => (
              <text key={i} x="345" y={80 + i * 24} fontSize="11" fill={C.dark}>{t}</text>
            ))}

            {/* Migration Service Box */}
            <rect x="640" y="30" width="240" height="340" rx="8" fill="white" stroke={C.purple} strokeWidth="2"/>
            <rect x="640" y="30" width="240" height="32" rx="8" fill={C.purple}/>
            <text x="760" y="52" textAnchor="middle" fill="white" fontSize="13" fontWeight="700">migration-service</text>
            {['DC XML/JSON Import', 'Workflow XML Import', 'Field Mapping Engine', 'Import Wizard (5 steps)', 'State Machine (7 states)', 'DLQ Error Handling', 'SSE Progress Streaming', 'Health & Integrity Check', 'Custom Field Compat', 'Scheme Migration'].map((t, i) => (
              <text key={i} x="655" y={80 + i * 24} fontSize="11" fill={C.dark}>{t}</text>
            ))}

            {/* Arrows: Issue <-> Workflow */}
            <defs><marker id="ah" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto"><path d="M0,0 L8,3 L0,6" fill={C.brand}/></marker></defs>
            <line x1="270" y1="100" x2="325" y2="100" stroke={C.brand} strokeWidth="2" markerEnd="url(#ah)"/>
            <text x="297" y="94" textAnchor="middle" fontSize="9" fill={C.brand}>fetch issue</text>
            <line x1="325" y1="140" x2="270" y2="140" stroke={C.brand} strokeWidth="2" markerEnd="url(#ah)"/>
            <text x="297" y="134" textAnchor="middle" fontSize="9" fill={C.brand}>update status</text>
            <line x1="270" y1="180" x2="325" y2="180" stroke={C.teal} strokeWidth="2" markerEnd="url(#ah)"/>
            <text x="297" y="174" textAnchor="middle" fontSize="9" fill={C.teal}>execute transition</text>
            <line x1="325" y1="220" x2="270" y2="220" stroke={C.teal} strokeWidth="2" markerEnd="url(#ah)"/>
            <text x="297" y="214" textAnchor="middle" fontSize="9" fill={C.teal}>record history</text>

            {/* Arrows: Workflow <-> Migration */}
            <line x1="580" y1="100" x2="635" y2="100" stroke={C.purple} strokeWidth="2" markerEnd="url(#ah)"/>
            <text x="607" y="94" textAnchor="middle" fontSize="9" fill={C.purple}>import workflows</text>
            <line x1="635" y1="180" x2="580" y2="180" stroke={C.purple} strokeWidth="2" markerEnd="url(#ah)"/>
            <text x="607" y="174" textAnchor="middle" fontSize="9" fill={C.purple}>scheme mapping</text>

            {/* Arrows: Issue <-> Migration */}
            <path d="M145,370 L145,390 L760,390 L760,370" fill="none" stroke={C.subtle} strokeWidth="1.5" strokeDasharray="4,4"/>
            <text x="450" y="386" textAnchor="middle" fontSize="9" fill={C.subtle}>import issues, fields, schemes</text>
          </svg>
        </div>
      </div>

      {/* Issue Features Table */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">Issue Service — Feature Inventory (48+ Entities)</div>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead><tr><th style={{width:'18%'}}>Feature Area</th><th style={{width:'35%'}}>Key Entities</th><th style={{width:'15%'}}>Service</th><th style={{width:'32%'}}>API Endpoints</th></tr></thead>
            <tbody>
              {issueFeatures.map(f => (
                <tr key={f.area}>
                  <td style={{ fontWeight: 600, fontSize: '12px' }}>{f.area}</td>
                  <td style={{ fontSize: '11px', fontFamily: 'monospace' }}>{f.entities}</td>
                  <td><span className="ads-badge ads-badge--verified" style={{fontSize:'10px'}}>{f.service}</span></td>
                  <td style={{ fontSize: '11px' }}>{f.endpoints}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Workflow Engine Features Table */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">Workflow Service — Engine Architecture (31+ Entities)</div>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead><tr><th style={{width:'18%'}}>Component</th><th style={{width:'35%'}}>Entities / Types</th><th style={{width:'47%'}}>Detail</th></tr></thead>
            <tbody>
              {workflowFeatures.map(f => (
                <tr key={f.area}>
                  <td style={{ fontWeight: 600, fontSize: '12px' }}>{f.area}</td>
                  <td style={{ fontSize: '11px', fontFamily: 'monospace' }}>{f.entities}</td>
                  <td style={{ fontSize: '11px' }}>{f.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Workflow Execution Pipeline */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">Workflow Execution Engine — 12-Step Pipeline</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px', padding: '16px' }}>
          {[
            { step: '1', name: 'Idempotency', desc: 'Check duplicate key', color: C.subtle },
            { step: '2', name: 'Context', desc: 'Fetch issue + user data', color: C.brand },
            { step: '3', name: 'Project Perm', desc: 'EDIT_ISSUES check', color: C.purple },
            { step: '4', name: 'Status Valid', desc: 'fromStatus matches current', color: C.teal },
            { step: '5', name: 'Optimistic Lock', desc: 'Version check', color: C.subtle },
            { step: '6', name: 'Transition Perm', desc: 'Group/role check', color: C.purple },
            { step: '7', name: 'Conditions', desc: '16 condition types', color: C.warning },
            { step: '8', name: 'Validators', desc: '11 validator types', color: C.warning },
            { step: '9', name: 'Post-Functions', desc: 'Essential + configured', color: C.danger },
            { step: '10', name: 'History', desc: 'Record transition', color: C.success },
            { step: '11', name: 'Event Fire', desc: 'Outbox + automation', color: C.success },
            { step: '12', name: 'Idempotency Store', desc: 'Cache response', color: C.subtle },
          ].map(s => (
            <div key={s.step} style={{ background: 'white', borderRadius: '6px', border: `2px solid ${s.color}`, padding: '8px', textAlign: 'center' }}>
              <div style={{ fontWeight: 700, fontSize: '18px', color: s.color }}>{s.step}</div>
              <div style={{ fontWeight: 600, fontSize: '12px', color: C.dark }}>{s.name}</div>
              <div style={{ fontSize: '10px', color: C.subtle }}>{s.desc}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Migration Service Features Table */}
      <div className="ads-card">
        <div className="ads-card-title">Migration Service — Jira DC Data Import Architecture</div>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead><tr><th style={{width:'18%'}}>Feature</th><th style={{width:'35%'}}>Entities</th><th style={{width:'47%'}}>Detail</th></tr></thead>
            <tbody>
              {migrationFeatures.map(f => (
                <tr key={f.area}>
                  <td style={{ fontWeight: 600, fontSize: '12px' }}>{f.area}</td>
                  <td style={{ fontSize: '11px', fontFamily: 'monospace' }}>{f.entities}</td>
                  <td style={{ fontSize: '11px' }}>{f.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 4: Domain Model ── */
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

/* ── Tab 8: SIL Alternative (GraalJS Scripting Engine) ── */
function renderSilAlternative() {
  const silVsGraal = [
    { feature: 'Language', sil: 'SIL (Simple Issue Language) — proprietary DSL', graal: 'JavaScript (ECMAScript 2022) — industry standard' },
    { feature: 'Runtime', sil: 'SIL Manager plugin (cPrime)', graal: 'GraalVM Polyglot Engine (Oracle) — sandboxed' },
    { feature: 'Script Types', sil: 'Post-functions, Validators, Conditions, Listeners, Scheduled Jobs, REST endpoints', graal: 'Post-functions, Validators, Conditions, Scheduled Jobs, Console (REPL)' },
    { feature: 'Issue Access', sil: 'issue.status, issue.assignee, issue.customfield_10001', graal: 'jdc.issue.getFieldValue("status"), jdc.issue.setFieldValue("assignee", userId)' },
    { feature: 'Linked Issues', sil: 'linkedIssues(issue, "blocks")', graal: 'jdc.issue.getLinkedIssues() — returns full issue objects' },
    { feature: 'JQL Search', sil: 'selectIssues("project = X AND status = Open")', graal: 'jdc.search.jql("project = X AND status = Open", 100)' },
    { feature: 'User/Group', sil: 'isInGroup(currentUser(), "developers")', graal: 'jdc.user.isInGroup("developers")' },
    { feature: 'Comments', sil: 'addComment(issue, "text")', graal: 'jdc.issue.addComment("text")' },
    { feature: 'Transitions', sil: 'transition(issue, "Done")', graal: 'Handled by workflow engine post-function chain — no script needed' },
    { feature: 'HTTP Calls', sil: 'httpGet("https://api.example.com")', graal: 'DISABLED — no network access (security sandbox)' },
    { feature: 'Scheduling', sil: 'SIL Scheduled Jobs (cron)', graal: 'ScriptSchedule entity with cron expressions' },
    { feature: 'Debugging', sil: 'SIL Manager console + log()', graal: 'Script Console (POST /api/workflow/scripts/console) + jdc.log.info()' },
    { feature: 'Versioning', sil: 'File-based (.sil files)', graal: 'ScriptVersion entity with rollback support' },
    { feature: 'Security', sil: 'Full server access (file I/O, network, classes)', graal: 'Strict sandbox — no I/O, no threads, no native access, no Java classes, statement limit' },
    { feature: 'Performance', sil: 'Interpreted at runtime', graal: 'JIT-compiled by GraalVM with source caching' },
  ];

  const migrationExamples = [
    {
      title: 'Auto-assign on transition',
      sil: `// SIL script
if (issue.status == "In Progress") {
  issue.assignee = currentUser();
}`,
      graal: `// GraalJS equivalent
if (issueData.status === "In Progress") {
  jdc.issue.setFieldValue("assigneeId", userId);
}`,
      note: 'In our system, this is better done as a built-in ASSIGN_TO_CURRENT_USER post-function — no script needed.'
    },
    {
      title: 'Cascade date change to linked issues',
      sil: `// SIL script
string[] linked = linkedIssues(issue, "blocks");
for (string li in linked) {
  setCustomFieldValue(li, "end_date", issue.end_date);
}`,
      graal: `// GraalJS equivalent
const linked = jdc.issue.getLinkedIssues();
for (const li of linked) {
  if (li.linkType === "blocks") {
    jdc.issue.setFieldValue.call({issueId: li.id}, "end_date", issueData.end_date);
  }
}`,
      note: 'Better approach: use an Automation Rule (trigger: FIELD_CHANGED on end_date, branch: FOR_EACH_LINKED_ISSUE, action: UPDATE_FIELD).'
    },
    {
      title: 'Validate required fields on transition',
      sil: `// SIL validator
if (isEmpty(issue.description)) {
  return false, "Description is required";
}
return true;`,
      graal: `// GraalJS validator
if (!issueData.description || issueData.description.trim() === '') {
  return 'Description is required';
}
return null; // null = valid`,
      note: 'Better approach: use a built-in FIELD_REQUIRED validator on the workflow transition — no script needed.'
    },
    {
      title: 'Create sub-task on condition',
      sil: `// SIL post-function
if (issue.priority == "Highest") {
  createSubTask(issue, "Urgent Review", "Task");
}`,
      graal: `// GraalJS post-function
if (issueData.priority === "Highest") {
  jdc.issue.addComment("Priority is Highest — manual sub-task creation needed");
  // Sub-task creation via jdc API: planned enhancement
}`,
      note: 'Better approach: use a built-in CREATE_SUBTASK post-function with a FIELD_VALUE condition (priority = Highest).'
    },
  ];

  return (
    <div>
      <h3 style={{ color: C.dark, marginBottom: '8px' }}>SIL to GraalJS Migration Guide</h3>
      <p style={{ color: C.subtle, marginBottom: '24px' }}>
        SYSDOPS traditionally uses <strong>SIL (Simple Issue Language)</strong> scripts via the cPrime SIL Manager plugin for Jira automation.
        Our platform replaces SIL with a <strong>GraalJS-based scripting engine</strong> that provides equivalent functionality within a secure sandbox.
        Below is a comprehensive comparison and migration guide.
      </p>

      {/* Architecture Diagram: SIL vs GraalJS */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">Scripting Architecture Comparison</div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 60px 1fr', gap: '0', alignItems: 'start', padding: '16px' }}>
          {/* SIL Side */}
          <div style={{ background: '#fff3e0', borderRadius: '8px', padding: '16px', border: '2px solid #FF5630' }}>
            <div style={{ fontWeight: 700, fontSize: '16px', color: '#FF5630', marginBottom: '12px', textAlign: 'center' }}>SIL (Legacy)</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {['SIL Manager Plugin', 'SIL Script Files (.sil)', 'Full Server Access', 'No Sandbox', 'cPrime Proprietary'].map(item => (
                <div key={item} style={{ background: 'white', padding: '6px 10px', borderRadius: '4px', fontSize: '12px', border: '1px solid #dfe1e6' }}>{item}</div>
              ))}
            </div>
          </div>
          {/* Arrow */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
            <svg width="50" height="40"><path d="M5,20 L40,20 M34,14 L40,20 L34,26" stroke={C.brand} fill="none" strokeWidth="3"/></svg>
          </div>
          {/* GraalJS Side */}
          <div style={{ background: '#e3fcef', borderRadius: '8px', padding: '16px', border: '2px solid #00875a' }}>
            <div style={{ fontWeight: 700, fontSize: '16px', color: '#00875a', marginBottom: '12px', textAlign: 'center' }}>GraalJS (Our Platform)</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {['GraalVM Polyglot Engine', 'ScriptDefinition Entity (DB)', 'Strict Sandbox (no I/O)', 'Statement Limit + Timeout', 'ECMAScript 2022 Standard'].map(item => (
                <div key={item} style={{ background: 'white', padding: '6px 10px', borderRadius: '4px', fontSize: '12px', border: '1px solid #dfe1e6' }}>{item}</div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Feature Comparison Table */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">Feature-by-Feature Comparison</div>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead><tr><th style={{width:'15%'}}>Feature</th><th style={{width:'38%'}}>SIL (cPrime)</th><th style={{width:'38%'}}>GraalJS (Our Platform)</th><th style={{width:'9%'}}>Parity</th></tr></thead>
            <tbody>
              {silVsGraal.map(row => (
                <tr key={row.feature}>
                  <td style={{ fontWeight: 600 }}>{row.feature}</td>
                  <td style={{ fontSize: '12px', fontFamily: 'monospace', background: '#fff3e0' }}>{row.sil}</td>
                  <td style={{ fontSize: '12px', fontFamily: 'monospace', background: '#e3fcef' }}>{row.graal}</td>
                  <td style={{ textAlign: 'center' }}>{row.graal.includes('DISABLED') ? '🔒' : '✅'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Migration Examples */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">SIL to GraalJS Migration Examples</div>
        {migrationExamples.map((ex, i) => (
          <div key={i} style={{ borderBottom: i < migrationExamples.length - 1 ? '1px solid #dfe1e6' : 'none', padding: '16px' }}>
            <div style={{ fontWeight: 600, fontSize: '14px', marginBottom: '8px' }}>{i + 1}. {ex.title}</div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '8px' }}>
              <div>
                <div style={{ fontSize: '11px', fontWeight: 600, color: '#FF5630', marginBottom: '4px' }}>SIL Script</div>
                <pre style={{ background: '#fff3e0', padding: '10px', borderRadius: '4px', fontSize: '11px', overflow: 'auto', margin: 0, border: '1px solid #dfe1e6' }}>{ex.sil}</pre>
              </div>
              <div>
                <div style={{ fontSize: '11px', fontWeight: 600, color: '#00875a', marginBottom: '4px' }}>GraalJS Equivalent</div>
                <pre style={{ background: '#e3fcef', padding: '10px', borderRadius: '4px', fontSize: '11px', overflow: 'auto', margin: 0, border: '1px solid #dfe1e6' }}>{ex.graal}</pre>
              </div>
            </div>
            <div className="ads-alert ads-alert--info" style={{ fontSize: '12px', padding: '8px 12px' }}>
              <strong>Recommended:</strong> {ex.note}
            </div>
          </div>
        ))}
      </div>

      {/* JDC Script API Reference */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">JDC Script API (Available in GraalJS Scripts)</div>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead><tr><th>API</th><th>Methods</th><th>Description</th></tr></thead>
            <tbody>
              <tr><td style={{fontWeight:600}}>jdc.issue</td><td style={{fontSize:'11px',fontFamily:'monospace'}}>getCurrentIssue(), getFieldValue(field), setFieldValue(field, value), addComment(text), getComments(), getHistory(), getWatchers(), addWatcher(userId), link(targetKey, linkType), getLinkedIssues(), getAttachmentCount(), getIssue(idOrKey)</td><td>Full issue read/write access</td></tr>
              <tr><td style={{fontWeight:600}}>jdc.project</td><td style={{fontSize:'11px',fontFamily:'monospace'}}>getCurrentProject(), getVersions(projectId), getComponents(projectId), getMembers(projectId), getIssueTypes()</td><td>Project metadata access</td></tr>
              <tr><td style={{fontWeight:600}}>jdc.user</td><td style={{fontSize:'11px',fontFamily:'monospace'}}>getCurrentUser(), isInGroup(groupName), hasPermission(permission), getUserGroups()</td><td>User context and permissions</td></tr>
              <tr><td style={{fontWeight:600}}>jdc.workflow</td><td style={{fontSize:'11px',fontFamily:'monospace'}}>getCurrentTransition(), getAllStatuses()</td><td>Workflow context during transitions</td></tr>
              <tr><td style={{fontWeight:600}}>jdc.search</td><td style={{fontSize:'11px',fontFamily:'monospace'}}>jql(query, maxResults), findIssues(projectKey, statusName)</td><td>JQL search (max 500 results)</td></tr>
              <tr><td style={{fontWeight:600}}>jdc.log</td><td style={{fontSize:'11px',fontFamily:'monospace'}}>info(...args), warn(...args), error(...args), debug(...args)</td><td>Server-side logging</td></tr>
              <tr><td style={{fontWeight:600}}>console</td><td style={{fontSize:'11px',fontFamily:'monospace'}}>log(), warn(), error()</td><td>Script console output capture</td></tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Script Lifecycle Diagram */}
      <div className="ads-card" style={{ marginBottom: '24px' }}>
        <div className="ads-card-title">Script Execution Lifecycle</div>
        <div className="ads-pipeline">
          {['Create Script\n(POST /api/workflow/scripts)', 'Validate Syntax\n(/scripts/validate)', 'Test in Console\n(/scripts/console)', 'Enable Script\n(toggle isEnabled)', 'Attach to Workflow\n(condition/validator/post-fn)', 'Executes on\nTransition'].map((step, i) => (
            <React.Fragment key={i}>
              {i > 0 && <div className="ads-pipeline-arrow">&rarr;</div>}
              <div className="ads-pipeline-step ads-pipeline-step--done" style={{ minWidth: '130px', textAlign: 'center', fontSize: '11px', whiteSpace: 'pre-line' }}>{step}</div>
            </React.Fragment>
          ))}
        </div>
      </div>

      {/* When to use Scripts vs Built-in */}
      <div className="ads-card">
        <div className="ads-card-title">When to Use Scripts vs Built-in Features</div>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead><tr><th>Scenario</th><th>Recommendation</th><th>Why</th></tr></thead>
            <tbody>
              <tr><td>Set field value on transition</td><td><span className="ads-badge ads-badge--verified">Built-in: SET_FIELD_VALUE post-function</span></td><td>No script overhead, configurable via UI</td></tr>
              <tr><td>Validate required fields</td><td><span className="ads-badge ads-badge--verified">Built-in: FIELD_REQUIRED validator</span></td><td>Standard validation, clear error messages</td></tr>
              <tr><td>Auto-assign to current user</td><td><span className="ads-badge ads-badge--verified">Built-in: ASSIGN_TO_CURRENT_USER</span></td><td>Zero-config, works out of the box</td></tr>
              <tr><td>Check linked issue status</td><td><span className="ads-badge ads-badge--verified">Built-in: LINKED_ISSUE_STATUS condition</span></td><td>Supports direction, link type, requireAll</td></tr>
              <tr><td>Cascade updates to linked issues</td><td><span className="ads-badge ads-badge--new">Automation Rule: FOR_EACH_LINKED_ISSUE</span></td><td>No coding, configurable, auditable</td></tr>
              <tr><td>Complex multi-field validation</td><td><span className="ads-badge ads-badge--warning">Script: GraalJS VALIDATOR</span></td><td>When built-in validators are insufficient</td></tr>
              <tr><td>Cross-project data sync</td><td><span className="ads-badge ads-badge--warning">Script: GraalJS POST_FUNCTION</span></td><td>When built-in actions can't express the logic</td></tr>
              <tr><td>Custom calculation (e.g., risk score)</td><td><span className="ads-badge ads-badge--warning">Script: GraalJS CONDITION/POST_FUNCTION</span></td><td>Business logic too complex for config</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 9: API Reference ── */
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

/* ── Tab 11: Auth & Users ── */
function renderAuthUsers() {
  const roleMatrix = [
    { role: 'Administrator', manageUsers: true, manageBoards: true, deleteIssues: true, editIssues: true, readIssues: true },
    { role: 'Maintainer', manageUsers: true, manageBoards: true, deleteIssues: false, editIssues: true, readIssues: true },
    { role: 'Contributor', manageUsers: false, manageBoards: false, deleteIssues: false, editIssues: true, readIssues: true },
    { role: 'Reader', manageUsers: false, manageBoards: false, deleteIssues: false, editIssues: false, readIssues: true },
  ];

  const adminFeatures = [
    { area: 'System Settings', detail: 'Key-value config: application title, timezone, security policy, mail server, API keys' },
    { area: 'Permission Schemes', detail: '32 standard Jira DC permissions seeded (BROWSE_PROJECTS, CREATE_ISSUES, EDIT_ISSUES, DELETE_ISSUES, ASSIGN_ISSUES, etc.)' },
    { area: 'Notification Schemes', detail: '10 event types: Issue Created, Issue Updated, Issue Assigned, Comment Added, Status Changed, etc.' },
    { area: 'Priority Schemes', detail: 'Configurable priority sets per project (Highest, High, Medium, Low, Lowest + custom)' },
    { area: 'Screen Schemes', detail: 'Screen layouts for Create, Edit, View operations with field tab grouping' },
    { area: 'Field Configurations', detail: 'Required/optional/hidden per field per issue type context' },
    { area: 'Issue Type Schemes', detail: 'Maps which issue types are available per project' },
    { area: 'Workflow Schemes', detail: 'Proxy to workflow-service: maps issue types to workflows within a project' },
    { area: 'Appearance & License', detail: 'Logo, colors, announcement banner, license key management' },
    { area: 'LDAP Configuration', detail: 'Optional LDAP/AD directory integration for user sync' },
    { area: 'Master Data', detail: 'Aircraft programs, test means, systems, ATA chapters, suppliers, functions, teams, defect origins' },
    { area: 'Assets & Inventory', detail: 'Asset types, assets, asset-issue links -- test equipment and calibration tracking' },
  ];

  return (
    <div>
      <SectionHeading>Authentication, User Management & Administration</SectionHeading>
      <Paragraph>
        This tab covers four services that handle identity, access control, platform configuration, and
        notifications: auth-service (8081), user-service (8082), admin-service (8093), and
        notification-service (8087).
      </Paragraph>

      {/* Authentication Flow Diagram */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title" style={{ color: C.purple }}>Authentication Flow</h4>
        <div style={{ padding: 20, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <svg viewBox="0 0 920 260" style={{ width: '100%', maxHeight: 260 }}>
            {/* Login */}
            <rect x="10" y="30" width="120" height="50" rx="6" fill={C.white} stroke={C.brand} strokeWidth="2"/>
            <text x="70" y="52" textAnchor="middle" fontSize="12" fontWeight="600" fill={C.dark}>User Login</text>
            <text x="70" y="68" textAnchor="middle" fontSize="9" fill={C.subtle}>credentials / SSO</text>

            {/* Arrow */}
            <line x1="130" y1="55" x2="170" y2="55" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowR)"/>

            {/* Auth Service */}
            <rect x="170" y="20" width="140" height="70" rx="6" fill={C.white} stroke={C.purple} strokeWidth="2"/>
            <rect x="170" y="20" width="140" height="24" rx="6" fill={C.purple}/>
            <text x="240" y="37" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.white}>auth-service :8081</text>
            <text x="240" y="58" textAnchor="middle" fontSize="10" fill={C.dark}>JWT Generation</text>
            <text x="240" y="72" textAnchor="middle" fontSize="10" fill={C.dark}>SAML SSO</text>

            {/* Arrow */}
            <line x1="310" y1="55" x2="350" y2="55" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowR)"/>

            {/* Token Issued */}
            <rect x="350" y="30" width="130" height="50" rx="6" fill={C.white} stroke={C.success} strokeWidth="2"/>
            <text x="415" y="50" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.dark}>JWT Issued</text>
            <text x="415" y="66" textAnchor="middle" fontSize="9" fill={C.subtle}>access + refresh token</text>

            {/* Arrow */}
            <line x1="480" y1="55" x2="520" y2="55" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowR)"/>

            {/* Request with token */}
            <rect x="520" y="30" width="130" height="50" rx="6" fill={C.white} stroke={C.brand} strokeWidth="2"/>
            <text x="585" y="50" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.dark}>API Request</text>
            <text x="585" y="66" textAnchor="middle" fontSize="9" fill={C.subtle}>Bearer token header</text>

            {/* Arrow */}
            <line x1="650" y1="55" x2="690" y2="55" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowR)"/>

            {/* Gateway validates */}
            <rect x="690" y="20" width="130" height="70" rx="6" fill={C.white} stroke={C.dark} strokeWidth="2"/>
            <rect x="690" y="20" width="130" height="24" rx="6" fill={C.dark}/>
            <text x="755" y="37" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.white}>Gateway :8080</text>
            <text x="755" y="58" textAnchor="middle" fontSize="10" fill={C.dark}>Token Validation</text>
            <text x="755" y="72" textAnchor="middle" fontSize="10" fill={C.dark}>Route to Service</text>

            {/* Arrow down from gateway */}
            <line x1="755" y1="90" x2="755" y2="130" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowD)"/>

            {/* Service processes */}
            <rect x="690" y="130" width="130" height="50" rx="6" fill={C.white} stroke={C.success} strokeWidth="2"/>
            <text x="755" y="152" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.dark}>Target Service</text>
            <text x="755" y="168" textAnchor="middle" fontSize="9" fill={C.subtle}>processes request</text>

            {/* 401 flow */}
            <rect x="350" y="140" width="130" height="50" rx="6" fill={C.white} stroke={C.danger} strokeWidth="2"/>
            <text x="415" y="160" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.danger}>401 Unauthorized</text>
            <text x="415" y="175" textAnchor="middle" fontSize="9" fill={C.subtle}>token expired</text>

            <line x1="690" y1="165" x2="480" y2="165" stroke={C.danger} strokeWidth="1.5" strokeDasharray="4 3" markerEnd="url(#arrowR)"/>

            {/* Refresh arrow */}
            <line x1="415" y1="190" x2="415" y2="220" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowD)"/>

            <rect x="350" y="220" width="130" height="35" rx="6" fill={C.white} stroke={C.purple} strokeWidth="2"/>
            <text x="415" y="242" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.purple}>Token Refresh</text>

            <line x1="350" y1="237" x2="240" y2="90" stroke={C.purple} strokeWidth="1.5" strokeDasharray="4 3"/>

            {/* Token details */}
            <rect x="10" y="130" width="280" height="70" rx="6" fill={C.white} stroke={C.border} strokeWidth="1"/>
            <text x="150" y="150" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.dark}>Token Details</text>
            <text x="20" y="168" fontSize="10" fill={C.dark}>Access Token: short-lived (configurable, e.g. 15min)</text>
            <text x="20" y="183" fontSize="10" fill={C.dark}>Refresh Token: long-lived (configurable, e.g. 7 days)</text>

            {/* Auth entities */}
            <rect x="10" y="210" width="280" height="50" rx="6" fill={C.white} stroke={C.border} strokeWidth="1"/>
            <text x="150" y="230" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.dark}>Auth Entities</text>
            <text x="20" y="248" fontSize="10" fill={C.subtle}>Role, UserGroup, UserGroupMembership, SessionToken</text>
          </svg>
        </div>
        <div className="ads-alert ads-alert--info" style={{ marginTop: 12 }}>
          Roles supported: <strong>ROLE_ADMIN</strong>, <strong>ROLE_USER</strong>. Token strategy uses short-lived
          access tokens paired with long-lived refresh tokens. SAML SSO available for enterprise IdP integration.
        </div>
      </div>

      {/* User Management */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title" style={{ color: C.purple }}>User Management (user-service :8082)</h4>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr><th>Entity</th><th>Description</th><th>Key Fields</th></tr>
            </thead>
            <tbody>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>CwdUser</td>
                <td>Core user entity (Crowd directory model)</td>
                <td style={{ fontSize: 12 }}>id, username, displayName, email, active, directoryId, createdDate</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>CwdGroup</td>
                <td>User group (Crowd directory model)</td>
                <td style={{ fontSize: 12 }}>id, groupName, description, active, directoryId</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>OrganizationMemberId</td>
                <td>Organization membership link</td>
                <td style={{ fontSize: 12 }}>userId, organizationId, role, joinedDate</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div style={{ padding: '12px 16px', background: C.bg, borderRadius: 6, marginTop: 12, fontSize: 13 }}>
          <strong>Operations:</strong> User CRUD, group membership management, optional LDAP integration for directory sync,
          password management with bcrypt hashing, user search and autocomplete for pickers.
        </div>
      </div>

      {/* Admin Service */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title" style={{ color: C.warning }}>Admin Service (admin-service :8093)</h4>
        <Paragraph>
          The admin-service is the central configuration hub for the platform. It manages all schemes,
          system settings, master data categories, and the assets/inventory module. It shares the
          jira_admin schema with user-service.
        </Paragraph>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr><th style={{ width: '25%' }}>Feature Area</th><th>Detail</th></tr>
            </thead>
            <tbody>
              {adminFeatures.map((f) => (
                <tr key={f.area}>
                  <td style={{ fontWeight: 600, fontSize: 12 }}>{f.area}</td>
                  <td style={{ fontSize: 12 }}>{f.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Notification Service */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title" style={{ color: C.subtle }}>Notification Service (notification-service :8087)</h4>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr><th>Entity</th><th>Description</th><th>Details</th></tr>
            </thead>
            <tbody>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>EmailTemplate</td>
                <td>Thymeleaf-based email templates</td>
                <td style={{ fontSize: 12 }}>HTML templates with variable substitution for issue fields, user names, project info, links</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>NotificationEvent</td>
                <td>10 event types that trigger notifications</td>
                <td style={{ fontSize: 12 }}>ISSUE_CREATED, ISSUE_UPDATED, ISSUE_ASSIGNED, ISSUE_RESOLVED, ISSUE_CLOSED, COMMENT_ADDED, STATUS_CHANGED, MENTION, WATCHER_ADDED, SPRINT_STARTED</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>NotificationPreference</td>
                <td>Per-user notification preferences</td>
                <td style={{ fontSize: 12 }}>userId, eventType, channel (EMAIL/IN_APP), enabled flag -- users can opt-out per event type</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* User Roles Table */}
      <div className="ads-card">
        <h4 className="ads-card-title">SYSDOPS Role-Based Access Matrix</h4>
        <Paragraph>
          The platform defines four primary roles with a progressive permission model. Roles are
          assigned per project via ProjectRole/ProjectMember entities.
        </Paragraph>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr>
                <th>Role</th>
                <th style={{ textAlign: 'center' }}>Manage Users</th>
                <th style={{ textAlign: 'center' }}>Manage Boards</th>
                <th style={{ textAlign: 'center' }}>Delete Issues</th>
                <th style={{ textAlign: 'center' }}>Edit Issues</th>
                <th style={{ textAlign: 'center' }}>Read Issues</th>
              </tr>
            </thead>
            <tbody>
              {roleMatrix.map((r) => (
                <tr key={r.role}>
                  <td style={{ fontWeight: 700, color: C.dark }}>{r.role}</td>
                  {[r.manageUsers, r.manageBoards, r.deleteIssues, r.editIssues, r.readIssues].map((v, i) => (
                    <td key={i} style={{ textAlign: 'center', fontSize: 14, fontWeight: 700, color: v ? C.success : C.danger }}>
                      {v ? 'Yes' : 'No'}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 12: Sprint & Search ── */
function renderSprintSearch() {
  const jqlOperators = [
    { op: '=, !=', desc: 'Exact match / not equal', example: 'status = "Open"' },
    { op: '>, <, >=, <=', desc: 'Comparison (numbers, dates)', example: 'created > -7d' },
    { op: '~', desc: 'Contains (full-text)', example: 'summary ~ "engine failure"' },
    { op: 'IN, NOT IN', desc: 'Set membership', example: 'status IN ("Open", "In Progress")' },
    { op: 'IS, IS NOT', desc: 'Null checks', example: 'assignee IS EMPTY' },
    { op: 'WAS, WAS IN', desc: 'Historical state', example: 'status WAS "Open"' },
    { op: 'CHANGED', desc: 'Field change history', example: 'status CHANGED FROM "Open" TO "Done"' },
  ];

  const jqlFunctions = [
    { fn: 'currentUser()', desc: 'Currently authenticated user' },
    { fn: 'membersOf("group")', desc: 'All members of a user group' },
    { fn: 'latestReleasedVersion(project)', desc: 'Most recently released version' },
    { fn: 'startOfDay() / endOfDay()', desc: 'Date boundary functions' },
    { fn: 'startOfWeek() / endOfWeek()', desc: 'Week boundary functions' },
    { fn: 'startOfMonth() / endOfMonth()', desc: 'Month boundary functions' },
  ];

  const chartTypes = [
    { type: 'PIE', desc: 'Distribution charts (status, priority, assignee)' },
    { type: 'BAR', desc: 'Comparison charts (issues by project, sprint velocity)' },
    { type: 'LINE', desc: 'Trend charts (created vs resolved over time)' },
    { type: 'DONUT', desc: 'Proportional distribution with center metric' },
    { type: 'TABLE', desc: 'Tabular data gadget with sorting and filtering' },
    { type: 'STACKED_BAR', desc: 'Multi-dimension comparison (status by priority)' },
    { type: 'AREA', desc: 'Cumulative flow diagrams' },
    { type: 'SCATTER', desc: 'Correlation analysis (estimate vs actual)' },
    { type: 'GAUGE', desc: 'Single metric progress (SLA compliance, sprint health)' },
    { type: 'HEATMAP', desc: 'Activity density (commits per day, issues per component)' },
  ];

  return (
    <div>
      <SectionHeading>Sprint, Board, Search & Dashboard Architecture</SectionHeading>
      <Paragraph>
        This tab covers sprint-service (8091), search-service (8088), dashboard-service, and
        plan-service -- the services that power agile boards, JQL search, dashboards, and
        advanced roadmap planning.
      </Paragraph>

      {/* Sprint & Board Architecture SVG */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title" style={{ color: C.teal }}>Sprint & Board Architecture</h4>
        <div style={{ padding: 20, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <svg viewBox="0 0 880 350" style={{ width: '100%', maxHeight: 350 }}>
            {/* Board entity */}
            <rect x="30" y="20" width="200" height="100" rx="8" fill={C.white} stroke={C.teal} strokeWidth="2"/>
            <rect x="30" y="20" width="200" height="28" rx="8" fill={C.teal}/>
            <text x="130" y="39" textAnchor="middle" fontSize="12" fontWeight="600" fill={C.white}>AgileBoard</text>
            <text x="40" y="65" fontSize="10" fill={C.dark}>type: SCRUM | KANBAN</text>
            <text x="40" y="80" fontSize="10" fill={C.dark}>name, projectId, filterJql</text>
            <text x="40" y="95" fontSize="10" fill={C.dark}>boardConfig (swimlanes, colors)</text>

            {/* Arrow to columns */}
            <line x1="230" y1="70" x2="280" y2="70" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowR)"/>
            <text x="255" y="62" textAnchor="middle" fontSize="9" fill={C.subtle}>1:N</text>

            {/* BoardColumn */}
            <rect x="280" y="20" width="180" height="100" rx="8" fill={C.white} stroke={C.teal} strokeWidth="2"/>
            <rect x="280" y="20" width="180" height="28" rx="8" fill={C.teal}/>
            <text x="370" y="39" textAnchor="middle" fontSize="12" fontWeight="600" fill={C.white}>BoardColumn</text>
            <text x="290" y="65" fontSize="10" fill={C.dark}>name, position, minLimit, maxLimit</text>
            <text x="290" y="80" fontSize="10" fill={C.dark}>statusMappings[] (status IDs)</text>
            <text x="290" y="95" fontSize="10" fill={C.dark}>columnCategory (TODO/IN_PROGRESS/DONE)</text>

            {/* Arrow from board to sprint */}
            <line x1="130" y1="120" x2="130" y2="160" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowD)"/>
            <text x="155" y="145" fontSize="9" fill={C.subtle}>1:N via BoardSprint</text>

            {/* Sprint */}
            <rect x="30" y="160" width="200" height="100" rx="8" fill={C.white} stroke={C.brand} strokeWidth="2"/>
            <rect x="30" y="160" width="200" height="28" rx="8" fill={C.brand}/>
            <text x="130" y="179" textAnchor="middle" fontSize="12" fontWeight="600" fill={C.white}>Sprint</text>
            <text x="40" y="205" fontSize="10" fill={C.dark}>name, goal, startDate, endDate</text>
            <text x="40" y="220" fontSize="10" fill={C.dark}>state: PLANNED | ACTIVE | COMPLETED</text>
            <text x="40" y="235" fontSize="10" fill={C.dark}>velocity, completeDate</text>

            {/* Sprint Lifecycle */}
            <rect x="280" y="160" width="340" height="50" rx="8" fill={C.white} stroke={C.border} strokeWidth="1"/>
            <text x="450" y="178" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.dark}>Sprint Lifecycle</text>

            <rect x="290" y="195" width="80" height="24" rx="4" fill={C.brand}/>
            <text x="330" y="211" textAnchor="middle" fontSize="10" fontWeight="600" fill={C.white}>PLANNED</text>

            <line x1="370" y1="207" x2="400" y2="207" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowR)"/>

            <rect x="400" y="195" width="80" height="24" rx="4" fill={C.success}/>
            <text x="440" y="211" textAnchor="middle" fontSize="10" fontWeight="600" fill={C.white}>ACTIVE</text>

            <line x1="480" y1="207" x2="510" y2="207" stroke={C.subtle} strokeWidth="2" markerEnd="url(#arrowR)"/>

            <rect x="510" y="195" width="100" height="24" rx="4" fill={C.subtle}/>
            <text x="560" y="211" textAnchor="middle" fontSize="10" fontWeight="600" fill={C.white}>COMPLETED</text>

            {/* BoardConfig */}
            <rect x="500" y="20" width="200" height="100" rx="8" fill={C.white} stroke={C.warning} strokeWidth="2"/>
            <rect x="500" y="20" width="200" height="28" rx="8" fill={C.warning}/>
            <text x="600" y="39" textAnchor="middle" fontSize="12" fontWeight="600" fill={C.white}>BoardConfig</text>
            <text x="510" y="65" fontSize="10" fill={C.dark}>swimlaneStrategy (NONE/ASSIGNEE/EPIC)</text>
            <text x="510" y="80" fontSize="10" fill={C.dark}>quickFilters (JQL-based)</text>
            <text x="510" y="95" fontSize="10" fill={C.dark}>cardColors, cardFields, estimation</text>

            {/* Arrow board -> config */}
            <line x1="230" y1="40" x2="230" y2="40" stroke="none"/>
            <path d="M230,40 Q270,10 500,40" fill="none" stroke={C.subtle} strokeWidth="1.5" strokeDasharray="4 3"/>

            {/* Board types explanation */}
            <rect x="30" y="280" width="590" height="60" rx="8" fill={C.white} stroke={C.border} strokeWidth="1"/>
            <text x="50" y="302" fontSize="11" fontWeight="600" fill={C.dark}>Board Types:</text>
            <text x="50" y="320" fontSize="10" fill={C.dark}>Scrum: Sprint-based with backlog, active sprint, and completed sprints. Velocity tracking.</text>
            <text x="50" y="334" fontSize="10" fill={C.dark}>Kanban: Continuous flow with WIP limits per column. Cumulative flow diagram.</text>

            {/* Entity count */}
            <rect x="730" y="20" width="120" height="100" rx="8" fill={C.white} stroke={C.border} strokeWidth="1"/>
            <text x="790" y="42" textAnchor="middle" fontSize="11" fontWeight="600" fill={C.dark}>Entities</text>
            <text x="790" y="65" textAnchor="middle" fontSize="28" fontWeight="700" fill={C.teal}>5</text>
            <text x="790" y="82" textAnchor="middle" fontSize="10" fill={C.subtle}>Sprint, Board,</text>
            <text x="790" y="95" textAnchor="middle" fontSize="10" fill={C.subtle}>Column, Config,</text>
            <text x="790" y="108" textAnchor="middle" fontSize="10" fill={C.subtle}>BoardSprint</text>
          </svg>
        </div>
      </div>

      {/* Search Service - JQL Engine */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title" style={{ color: C.brand }}>Search Service -- JQL Engine (search-service :8088)</h4>
        <Paragraph>
          The search service implements a complete JQL (Jira Query Language) parser that translates
          JQL expressions into SQL queries. It supports operators, functions, date math, and historical
          queries (WAS/CHANGED). Results can be cached for performance.
        </Paragraph>

        <div className="ads-grid-3" style={{ marginBottom: 16 }}>
          <div>
            <div className="ads-section-title">JQL Operators</div>
            <div className="ads-table-wrap">
              <table className="ads-table">
                <thead><tr><th>Operator</th><th>Description</th><th>Example</th></tr></thead>
                <tbody>
                  {jqlOperators.map((o) => (
                    <tr key={o.op}>
                      <td style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 12, whiteSpace: 'nowrap' }}>{o.op}</td>
                      <td style={{ fontSize: 11 }}>{o.desc}</td>
                      <td style={{ fontFamily: 'monospace', fontSize: 10 }}>{o.example}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <div style={{ gridColumn: 'span 2' }}>
            <div className="ads-section-title">JQL Functions & Date Math</div>
            <div className="ads-table-wrap">
              <table className="ads-table">
                <thead><tr><th>Function</th><th>Description</th></tr></thead>
                <tbody>
                  {jqlFunctions.map((f) => (
                    <tr key={f.fn}>
                      <td style={{ fontFamily: 'monospace', fontWeight: 600, fontSize: 12 }}>{f.fn}</td>
                      <td style={{ fontSize: 12 }}>{f.desc}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="ads-alert ads-alert--info" style={{ marginTop: 8 }}>
              <strong>Date math:</strong> Supports relative offsets like <code>-1d</code> (1 day ago),
              <code>-2w</code> (2 weeks ago), <code>-3M</code> (3 months ago). Combine with functions:
              <code>created &gt;= startOfDay(-7d)</code>
            </div>
          </div>
        </div>

        <div style={{ padding: '12px 16px', background: C.bg, borderRadius: 6 }}>
          <strong>Query Pipeline:</strong> JQL string &rarr; Lexer/Tokenizer &rarr; AST Parser &rarr;
          Semantic Analyzer (resolve field names, validate types) &rarr; SQL Generator &rarr; Query Execution &rarr;
          Result Mapping &rarr; Response with pagination. Full-text search on <code>summary</code> and
          <code>description</code> fields via PostgreSQL <code>tsvector</code>.
        </div>
      </div>

      {/* Dashboard & Reporting */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title" style={{ color: C.success }}>Dashboard & Reporting (dashboard-service)</h4>
        <Paragraph>
          The dashboard service manages gadget instances that power configurable dashboards. Each gadget
          has a chart type, configuration JSON, and a data source (typically a JQL query or reference ID).
        </Paragraph>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr><th>Entity</th><th>Key Fields</th><th>Description</th></tr>
            </thead>
            <tbody>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>Gadget</td>
                <td style={{ fontSize: 12 }}>id, name, description, category</td>
                <td style={{ fontSize: 12 }}>Gadget type definition (e.g., "Issue Statistics", "Sprint Burndown")</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>GadgetInstance</td>
                <td style={{ fontSize: 12 }}>id, gadgetId, chartType, chartConfig (JSON), referenceId, dataSourceJql, position</td>
                <td style={{ fontSize: 12 }}>Configured instance on a user's dashboard with placement and data binding</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="ads-section-title" style={{ marginTop: 16 }}>Supported Chart Types</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 8 }}>
          {chartTypes.map((ct) => (
            <div key={ct.type} style={{
              padding: '8px 10px',
              background: C.white,
              borderRadius: 6,
              border: `1px solid ${C.border}`,
              textAlign: 'center',
            }}>
              <div style={{ fontWeight: 700, fontSize: 13, color: C.brand, marginBottom: 4 }}>{ct.type}</div>
              <div style={{ fontSize: 10, color: C.subtle, lineHeight: 1.4 }}>{ct.desc}</div>
            </div>
          ))}
        </div>
        <div className="ads-alert ads-alert--info" style={{ marginTop: 12 }}>
          Dashboard gadgets integrate with Custom Charts plugin functionality. Performance Objective
          dashboards provide project-level KPI tracking with configurable targets.
        </div>
      </div>

      {/* Plan Service - Advanced Roadmaps */}
      <div className="ads-card">
        <h4 className="ads-card-title" style={{ color: C.purple }}>Plan Service -- Advanced Roadmaps</h4>
        <Paragraph>
          The plan service implements Jira Advanced Roadmaps functionality: cross-project planning with
          dependencies, team capacity management, goal tracking, and schedule optimization.
        </Paragraph>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr><th>Entity</th><th>Key Fields</th><th>Description</th></tr>
            </thead>
            <tbody>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>PlanItem</td>
                <td style={{ fontSize: 12 }}>id, issueId, targetDate, targetEndDate, dependencies[]</td>
                <td style={{ fontSize: 12 }}>Roadmap item with scheduling dates and dependency links</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>PlanTeam</td>
                <td style={{ fontSize: 12 }}>id, name, planId, velocity, iterationLength</td>
                <td style={{ fontSize: 12 }}>Team definition within a plan for capacity calculation</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>PlanTeamMember</td>
                <td style={{ fontSize: 12 }}>id, teamId, userId, capacityHours, availability (%)</td>
                <td style={{ fontSize: 12 }}>Individual team member with capacity and availability settings</td>
              </tr>
              <tr>
                <td style={{ fontWeight: 600, fontFamily: 'monospace', fontSize: 12 }}>PlanGoal</td>
                <td style={{ fontSize: 12 }}>id, name, parentGoalId, progress, status</td>
                <td style={{ fontSize: 12 }}>Hierarchical goals with automatic progress tracking from linked items</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 16 }}>
          <div style={{ padding: '12px 16px', background: C.bg, borderRadius: 6 }}>
            <div style={{ fontWeight: 600, fontSize: 13, color: C.dark, marginBottom: 4 }}>Schedule Engine</div>
            <ul style={{ margin: 0, paddingLeft: 18, fontSize: 12, color: C.dark, lineHeight: 1.8 }}>
              <li>Forward scheduling (from start date)</li>
              <li>Backward scheduling (from deadline)</li>
              <li>Critical path analysis</li>
              <li>Dependency chain resolution</li>
              <li>Working days configuration (exclude weekends/holidays)</li>
            </ul>
          </div>
          <div style={{ padding: '12px 16px', background: C.bg, borderRadius: 6 }}>
            <div style={{ fontWeight: 600, fontSize: 13, color: C.dark, marginBottom: 4 }}>Capacity Planning</div>
            <ul style={{ margin: 0, paddingLeft: 18, fontSize: 12, color: C.dark, lineHeight: 1.8 }}>
              <li>Team velocity tracking per iteration</li>
              <li>Member availability percentage</li>
              <li>Capacity hours per sprint</li>
              <li>Over-allocation warnings</li>
              <li>Cross-team dependency visualization</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ── Tab 13: Database Schema ── */
function renderDatabaseSchema() {
  const schemas = [
    {
      schema: 'jira_auth', service: 'auth-service', port: '8081',
      keyTables: 'users, roles, user_roles, user_groups, user_group_memberships',
      tableCount: '5', relationships: 'users -> roles (M:N via user_roles)',
      color: C.purple,
    },
    {
      schema: 'jira_admin', service: 'admin-service + user-service', port: '8093/8082',
      keyTables: 'cwd_user, cwd_group, statuses, issue_types, priorities, resolutions, permission_schemes, system_settings, aircraft_programs, test_means, aircraft_systems, ata_chapters, system_suppliers, system_functions, reporter_teams, test_mean_defect_origins, asset_types, assets, asset_issue_links, project_roles',
      tableCount: '30+', relationships: 'aircraft_programs -> test_means, systems -> functions/suppliers',
      color: C.warning,
    },
    {
      schema: 'jira_project', service: 'project-service', port: '8083',
      keyTables: 'projects, project_types, project_templates, project_roles, project_members, issue_type_schemes, workflow_schemes, permission_schemes, notification_schemes, screen_schemes, field_configurations',
      tableCount: '15+', relationships: 'projects -> schemes (1:1 per type)',
      color: C.success,
    },
    {
      schema: 'jira_issue', service: 'issue-service', port: '8084',
      keyTables: 'issues, issue_types, issue_statuses, issue_priorities, resolutions, custom_field_definitions, custom_field_values, issue_links, issue_link_types, change_groups, change_items, labels, epics, worklogs, project_versions, project_components, change_card_metadata, design_item_metadata, dcl_metadata, deliverable_metadata, system_standard_metadata, review_sub_task_metadata, modification_metadata, external_page_links',
      tableCount: '30+', relationships: 'issues -> custom_field_values (1:N), issues -> change_card_metadata (1:1)',
      color: C.success,
    },
    {
      schema: 'jira_workflow', service: 'workflow-service', port: '8085',
      keyTables: 'workflows, workflow_statuses, workflow_transitions, workflow_conditions, workflow_validators, workflow_post_functions, workflow_schemes, workflow_scheme_mappings, workflow_transition_history, workflow_screens, workflow_triggers, script_definitions, script_versions, automation_rules, automation_execution_log',
      tableCount: '25+', relationships: 'workflows -> statuses -> transitions -> conditions/validators/post-functions',
      color: C.danger,
    },
    {
      schema: 'jira_test', service: 'test-service', port: '8095',
      keyTables: 'test_issue, test_step, test_set, test_plan, test_execution, test_run, step_result, requirement_link, defect_link, test_folder, shared_step, precondition, cucumber_scenario, vvo_definition, hlvvo_definition, test_request, vvo_test_request_link, tech_event, bench_defect, problem_report, export_template, coverage_*, environment_*, evidence_*, flaky_*, quarantine_*',
      tableCount: '50+', relationships: 'vvo_definition -> hlvvo_definition, test_plan -> test_execution, tech_event -> bench_defect/problem_report',
      color: C.danger,
    },
    {
      schema: 'jira_sprint', service: 'sprint-service', port: '8091',
      keyTables: 'sprints, agile_boards, board_columns, board_configs, board_sprints',
      tableCount: '5', relationships: 'boards -> columns, boards -> sprints',
      color: C.teal,
    },
    {
      schema: 'jira_plan', service: 'plan-service', port: '--',
      keyTables: 'plan_items, plan_teams, plan_team_members, plan_goals, dependencies, releases, initiatives, schedules',
      tableCount: '10+', relationships: 'plan_items -> dependencies (M:N), plan_teams -> members',
      color: C.teal,
    },
    {
      schema: 'jira_comment', service: 'comment-service', port: '--',
      keyTables: 'comments',
      tableCount: '1', relationships: 'comments -> issues (via issueId FK)',
      color: C.subtle,
    },
    {
      schema: 'jira_notification', service: 'notification-service', port: '8087',
      keyTables: 'email_templates, notification_events, notification_preferences',
      tableCount: '3', relationships: 'events -> templates',
      color: C.subtle,
    },
    {
      schema: 'jira_search', service: 'search-service', port: '8088',
      keyTables: 'jql_queries, jql_clauses',
      tableCount: '2', relationships: 'Stateless query processing',
      color: C.teal,
    },
    {
      schema: 'jira_audit', service: 'audit-service', port: '--',
      keyTables: 'audit_logs',
      tableCount: '1', relationships: 'Append-only audit trail',
      color: C.subtle,
    },
    {
      schema: 'jira_version', service: 'version-service', port: '--',
      keyTables: 'project_versions, issue_affects_versions',
      tableCount: '2', relationships: 'versions -> issues (M:N)',
      color: C.subtle,
    },
    {
      schema: 'jira_component', service: 'component-service', port: '--',
      keyTables: 'project_components, issue_components, component_assignment_rules, component_audit_log, component_metrics',
      tableCount: '5+', relationships: 'components -> issues (M:N)',
      color: C.subtle,
    },
  ];

  return (
    <div>
      <SectionHeading>Database Schema Reference</SectionHeading>
      <Paragraph>
        SYSDOPS uses a single PostgreSQL 16 instance with 14 isolated schemas. Each service owns its
        schema and manages its tables via Flyway migrations. Cross-schema references use logical IDs
        (not foreign keys) to maintain service isolation.
      </Paragraph>

      {/* Summary Stats */}
      <div className="ads-stats" style={{ marginBottom: 24 }}>
        <div className="ads-stat ads-stat--brand">
          <span className="ads-stat-value">14</span>
          <span className="ads-stat-label">Schemas</span>
        </div>
        <div className="ads-stat ads-stat--success">
          <span className="ads-stat-value">150+</span>
          <span className="ads-stat-label">Tables</span>
        </div>
        <div className="ads-stat ads-stat--warning">
          <span className="ads-stat-value">150+</span>
          <span className="ads-stat-label">Entity Models</span>
        </div>
        <div className="ads-stat">
          <span className="ads-stat-value">50+</span>
          <span className="ads-stat-label">Flyway Migrations</span>
        </div>
      </div>

      {/* Schema Table */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title">All 14 Schemas -- Tables & Relationships</h4>
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr>
                <th style={{ width: '12%' }}>Schema</th>
                <th style={{ width: '14%' }}>Service</th>
                <th style={{ width: '36%' }}>Key Tables</th>
                <th style={{ width: '8%', textAlign: 'center' }}>Tables</th>
                <th style={{ width: '30%' }}>Key Relationships</th>
              </tr>
            </thead>
            <tbody>
              {schemas.map((s) => (
                <tr key={s.schema}>
                  <td>
                    <span style={{
                      fontFamily: 'monospace',
                      fontWeight: 700,
                      fontSize: 12,
                      color: s.color,
                      background: `${s.color}15`,
                      padding: '2px 6px',
                      borderRadius: 3,
                      display: 'inline-block',
                    }}>
                      {s.schema}
                    </span>
                  </td>
                  <td>
                    <div style={{ fontSize: 12, fontWeight: 600, color: C.dark }}>{s.service}</div>
                    <div style={{ fontSize: 10, color: C.subtle }}>:{s.port}</div>
                  </td>
                  <td style={{ fontSize: 11, fontFamily: 'monospace', lineHeight: 1.6, wordBreak: 'break-word' }}>
                    {s.keyTables}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span style={{
                      fontWeight: 700,
                      fontSize: 14,
                      color: C.dark,
                      background: C.bg,
                      padding: '2px 8px',
                      borderRadius: 4,
                      display: 'inline-block',
                    }}>
                      {s.tableCount}
                    </span>
                  </td>
                  <td style={{ fontSize: 11, color: C.dark }}>{s.relationships}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Schema Visualization */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title">Schema Size Distribution</h4>
        <div style={{ display: 'flex', gap: 6, alignItems: 'flex-end', padding: '16px 0', height: 200, borderBottom: `1px solid ${C.border}` }}>
          {schemas
            .map((s) => ({ name: s.schema.replace('jira_', ''), count: parseInt(s.tableCount) || 1, color: s.color }))
            .sort((a, b) => b.count - a.count)
            .map((s) => {
              const maxH = 160;
              const maxCount = 50;
              const h = Math.max(20, (s.count / maxCount) * maxH);
              return (
                <div key={s.name} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end' }}>
                  <div style={{ fontSize: 10, fontWeight: 700, color: C.dark, marginBottom: 4 }}>{s.count}+</div>
                  <div style={{ width: '70%', height: h, background: s.color, borderRadius: '4px 4px 0 0', minWidth: 20 }} />
                  <div style={{ fontSize: 8, color: C.subtle, marginTop: 4, textAlign: 'center', transform: 'rotate(-45deg)', transformOrigin: 'top center', whiteSpace: 'nowrap' }}>
                    {s.name}
                  </div>
                </div>
              );
            })}
        </div>
      </div>

      {/* Cross-schema communication */}
      <div className="ads-card" style={{ marginBottom: 24 }}>
        <h4 className="ads-card-title">Cross-Schema Communication Pattern</h4>
        <Paragraph>
          Services do not use cross-schema JOINs or foreign keys. Instead, they reference entities in
          other schemas via logical IDs and REST API calls. This preserves schema isolation and allows
          independent service deployment.
        </Paragraph>
        <div style={{ padding: 16, background: C.bg, borderRadius: 8, overflowX: 'auto' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 700 }}>
            <DiagramBox label="issue-service" sub="jira_issue schema" color={C.success} width={140} height={55} />
            <ArrowRight label="REST call" width={80} />
            <DiagramBox label="workflow-service" sub="jira_workflow schema" color={C.danger} width={150} height={55} />
            <ArrowRight label="REST call" width={80} />
            <DiagramBox label="admin-service" sub="jira_admin schema" color={C.warning} width={140} height={55} />
          </div>
          <div style={{ textAlign: 'center', marginTop: 8, fontSize: 11, color: C.subtle }}>
            Inter-service calls use WebClient beans with logical IDs (issueId, projectId, userId) --
            never direct SQL across schema boundaries
          </div>
        </div>
      </div>

      {/* Entity Count Summary */}
      <div className="ads-card">
        <h4 className="ads-card-title">Entity Count Summary</h4>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, padding: '16px 0' }}>
          <div style={{ textAlign: 'center', padding: 16, background: C.bg, borderRadius: 8 }}>
            <div style={{ fontSize: 32, fontWeight: 700, color: C.brand }}>14</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: C.dark }}>Total Schemas</div>
            <div style={{ fontSize: 11, color: C.subtle, marginTop: 4 }}>One per service (some shared)</div>
          </div>
          <div style={{ textAlign: 'center', padding: 16, background: C.bg, borderRadius: 8 }}>
            <div style={{ fontSize: 32, fontWeight: 700, color: C.success }}>150+</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: C.dark }}>Total Tables</div>
            <div style={{ fontSize: 11, color: C.subtle, marginTop: 4 }}>Across all 14 schemas</div>
          </div>
          <div style={{ textAlign: 'center', padding: 16, background: C.bg, borderRadius: 8 }}>
            <div style={{ fontSize: 32, fontWeight: 700, color: C.warning }}>150+</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: C.dark }}>Entity Models</div>
            <div style={{ fontSize: 11, color: C.subtle, marginTop: 4 }}>JPA entities mapped to tables</div>
          </div>
          <div style={{ textAlign: 'center', padding: 16, background: C.bg, borderRadius: 8 }}>
            <div style={{ fontSize: 32, fontWeight: 700, color: C.purple }}>50+</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: C.dark }}>Flyway Migrations</div>
            <div style={{ fontSize: 11, color: C.subtle, marginTop: 4 }}>Versioned schema evolution</div>
          </div>
        </div>

        {/* Schema ownership note */}
        <div className="ads-alert ads-alert--info" style={{ marginTop: 16 }}>
          <strong>Schema Isolation Principle:</strong> Each service owns its schema exclusively. The only
          exception is <code>jira_admin</code>, which is shared between admin-service (configuration data)
          and user-service (CwdUser/CwdGroup entities). All cross-service data access happens via REST
          APIs, never via direct database queries across schema boundaries.
        </div>
      </div>
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
      case 'Cluster Architecture':
        return renderClusterArchitecture();
      case 'Enterprise Hardening':
        return renderEnterpriseHardening();
      case 'Service Map':
        return renderServiceMap();
      case 'Issue & Workflow':
        return renderIssueWorkflow();
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
      case 'SIL Alternative':
        return renderSilAlternative();
      case 'API Reference':
        return renderApiReference();
      case 'Auth & Users':
        return renderAuthUsers();
      case 'Sprint & Search':
        return renderSprintSearch();
      case 'Database Schema':
        return renderDatabaseSchema();
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
