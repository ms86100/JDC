# JIRA DC PLAN MANAGEMENT - FEATURE REQUIREMENTS
## Extracted from: Jira DC Plan Management Tutorial Video

**Source:** YouTube Video - Jira DC Plan Management  
**Extraction Date:** 2026-05-12  
**Video Duration:** 7.2 minutes (~432 seconds)

---

# SECTION 1: CORE FEATURES IDENTIFIED

## 1. PROGRAM MANAGEMENT

### Description
Programs are top-level containers that group multiple Plans together for portfolio-level visibility.

### Requirements:
- [ ] **Program CRUD**
  - Create program with name and description
  - Edit program details
  - Delete program
  - View program list

- [ ] **Connected Plans**
  - Link multiple plans to a program
  - Add/remove plans from program
  - View all plans within a program

- [ ] **Program Access Control**
  - Open Access (anyone can view)
  - Restrict access to specific users/groups

### API Endpoints Needed:
```
POST   /api/programs              - Create program
GET    /api/programs              - List all programs
GET    /api/programs/{id}         - Get program by ID
PUT    /api/programs/{id}         - Update program
DELETE /api/programs/{id}         - Delete program
POST   /api/programs/{id}/plans   - Add plan to program
DELETE /api/programs/{id}/plans/{planId} - Remove plan from program
```

---

## 2. PLAN MANAGEMENT

### Description
Plans are containers for organizing work items (Epics, Stories, Subtasks) with timeline and scheduling capabilities.

### Requirements:
- [ ] **Plan CRUD**
  - Create plan with name
  - Set plan access (Open Access)
  - Select projects to include
  - Set exclusion filters
  - Edit plan settings
  - Delete plan

- [ ] **Plan Configuration**
  - Configure plan settings
  - Set custom dates for filtering
  - Configure warnings (target date beyond due date, issue dates)
  - Set group-by options (Epic, Story, Assignee, Component)
  - Configure swimlane level warnings
  - Enable/disable field display

- [ ] **Connected Plans**
  - Link plans together
  - Share with team members

### API Endpoints Needed:
```
POST   /api/plans                 - Create plan
GET    /api/plans                 - List plans
GET    /api/plans/{id}            - Get plan by ID
PUT    /api/plans/{id}            - Update plan
DELETE /api/plans/{id}            - Delete plan
GET    /api/plans/{id}/settings   - Get plan settings
PUT    /api/plans/{id}/settings   - Update plan settings
POST   /api/plans/{id}/share      - Share plan with users
```

---

## 3. BACKLOG VIEW

### Description
The backlog view shows all work items (Epics, Stories, Subtasks) organized for planning.

### Requirements:
- [ ] **Work Item Display**
  - Show Epic items with progress indicators
  - Show Story items with status
  - Show Subtask items
  - Display issue count per Epic

- [ ] **Filtering**
  - Filter by Epic
  - Filter by Story
  - Filter by Subtask
  - Filter by Release
  - Filter by Issue details (status, type, priority)
  - Filter by Assignee
  - Filter by Sprint
  - Filter by Project

- [ ] **Time-based Filtering**
  - 3 months view
  - 1 year view
  - Custom date ranges

- [ ] **Drag & Drop**
  - Reorder items within backlog
  - Move items between Epics
  - Persist ordering

- [ ] **Quick Actions**
  - Create Issue inline
  - Edit issue inline
  - View issue details

### API Endpoints Needed:
```
GET    /api/plans/{id}/backlog    - Get backlog items
POST   /api/plans/{id}/backlog/reorder - Reorder backlog items
GET    /api/plans/{id}/items      - Get all items (epics, stories, subtasks)
```

---

## 4. TEAMS VIEW

### Description
Organize work by teams for capacity planning and assignment.

### Requirements:
- [ ] **Team Management**
  - Add multiple teams to a plan
  - Remove teams
  - Invite shared team members

- [ ] **Team Capacity**
  - View team workload
  - Assign issues to teams
  - Track team progress

### API Endpoints Needed:
```
GET    /api/plans/{id}/teams      - Get teams in plan
POST   /api/plans/{id}/teams      - Add team to plan
DELETE /api/plans/{id}/teams/{teamId} - Remove team
GET    /api/teams/{id}/capacity   - Get team capacity
```

---

## 5. RELEASES VIEW

### Description
Manage releases/versions within a plan with dependency tracking.

### Requirements:
- [ ] **Release Management**
  - Create release (e.g., "R 1.0", "v1.0")
  - Assign projects to release
  - Tag issues with release
  - Approve/release workflow

- [ ] **Release Information**
  - Display release name
  - Show tagged projects (e.g., "Scrum Project")
  - Track release approval status

### API Endpoints Needed:
```
GET    /api/plans/{id}/releases   - Get releases for plan
POST   /api/plans/{id}/releases   - Create release
PUT    /api/plans/{id}/releases/{id} - Update release
POST   /api/plans/{id}/releases/{id}/approve - Approve release
POST   /api/plans/{id}/releases/{id}/tag-issues - Tag issues with release
```

---

## 6. DEPENDENCIES VIEW

### Description
Visualize and manage dependencies between issues.

### Requirements:
- [ ] **Dependency Management**
  - Add dependency between issues
  - View dependencies by Epic
  - View dependencies by Team
  - Track dependency status

- [ ] **Dependency Types**
  - Blocks
  - Is blocked by
  - Relates to
  - etc.

### API Endpoints Needed:
```
GET    /api/plans/{id}/dependencies - Get dependencies
POST   /api/plans/{id}/dependencies - Add dependency
DELETE /api/plans/{id}/dependencies/{id} - Remove dependency
GET    /api/issues/{id}/dependencies - Get issue dependencies
```

---

## 7. SCHEDULING & WARNINGS

### Description
Configuration for timeline views and warning notifications.

### Requirements:
- [ ] **Warning Center**
  - Target date beyond due date warnings
  - Issue dates warnings
  - Auto-scalable warnings
  - Filter warnings by status (All values, Empty values)

- [ ] **Scheduling Configuration**
  - Manual scheduling mode
  - Automatic scheduling mode
  - Custom date settings

### API Endpoints Needed:
```
GET    /api/plans/{id}/warnings   - Get all warnings
PUT    /api/plans/{id}/warnings/settings - Update warning settings
GET    /api/plans/{id}/scheduling - Get scheduling config
PUT    /api/plans/{id}/scheduling - Update scheduling config
```

---

## 8. SCOPE MANAGEMENT

### Description
Manage the scope of work items included in a plan.

### Requirements:
- [ ] **Scope Items**
  - Add Epics to scope
  - Add Stories to scope
  - Add Subtasks to scope
  - Remove items from scope

- [ ] **Scope Filtering**
  - Auto-filters from Jira
  - Custom filter criteria

### API Endpoints Needed:
```
GET    /api/plans/{id}/scope      - Get scope items
POST   /api/plans/{id}/scope      - Add item to scope
DELETE /api/plans/{id}/scope/{itemId} - Remove from scope
```

---

## 9. EXPORT FUNCTIONALITY

### Requirements:
- [ ] **Export Options**
  - Export to Spreadsheet (CSV/Excel)
  - Export plan data

### API Endpoints Needed:
```
GET    /api/plans/{id}/export     - Export plan data
```

---

# SECTION 2: ENTITY RELATIONSHIPS

```
Program (1) ──────< Plan (N)
                    │
                    ├── Backlog Item (N)
                    │     ├── Epic (N)
                    │     ├── Story (N)
                    │     └── Subtask (N)
                    │
                    ├── Team (N)
                    │     └── TeamMember (N)
                    │
                    ├── Release (N)
                    │     └── TaggedIssue (N)
                    │
                    └── Dependency (N)
                          └── IssueDependency (N)
```

---

# SECTION 3: EXISTING IMPLEMENTATION STATUS

Based on gap analysis, here's what's already implemented vs. needed:

## ALREADY IMPLEMENTED:
| Feature | Status | Notes |
|---------|--------|-------|
| Basic Projects | ✅ | Project entity, CRUD |
| Basic Issues | ✅ | Issue entity with epic fields |
| Sprint Service | ✅ | Sprints exist |
| Board Service | ⚠️ | Partial, mock issues |
| Workflow Service | ⚠️ | Basic transitions |
| Version Service | ✅ | Just implemented |
| Component Service | ✅ | Just implemented |

## NEEDS IMPLEMENTATION:
| Feature | Priority | Effort |
|---------|----------|--------|
| Program Entity & Service | HIGH | Medium |
| Plan Entity & Service | HIGH | High |
| Plan Backlog View | HIGH | High |
| Teams View | MEDIUM | Medium |
| Releases View | MEDIUM | Medium |
| Dependencies View | MEDIUM | Medium |
| Scheduling Settings | MEDIUM | Medium |
| Export Service | LOW | Low |

---

# SECTION 4: NEW DATABASE TABLES REQUIRED

```sql
-- Programs (Portfolio-level grouping)
CREATE TABLE programs (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    lead_user_id UUID,
    is_open_access BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Program-Plan linking
CREATE TABLE program_plans (
    program_id UUID REFERENCES programs(id),
    plan_id UUID REFERENCES plans(id),
    PRIMARY KEY (program_id, plan_id)
);

-- Plans (Roadmap containers)
CREATE TABLE plans (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    program_id UUID REFERENCES programs(id),
    owner_user_id UUID,
    is_open_access BOOLEAN DEFAULT TRUE,
    -- Settings stored as JSONB
    settings JSONB DEFAULT '{}',
    warning_settings JSONB DEFAULT '{}',
    scheduling_config JSONB DEFAULT '{}',
    group_by_mode VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Plan Work Items (denormalized for fast backlog loading)
CREATE TABLE plan_items (
    id UUID PRIMARY KEY,
    plan_id UUID REFERENCES plans(id),
    issue_id UUID REFERENCES issues(id),
    item_type VARCHAR(20) NOT NULL, -- 'EPIC', 'STORY', 'SUBTASK'
    sort_order INTEGER,
    target_date DATE,
    status VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Plan Teams
CREATE TABLE plan_teams (
    id UUID PRIMARY KEY,
    plan_id UUID REFERENCES plans(id),
    team_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP
);

-- Plan Team Members
CREATE TABLE plan_team_members (
    id UUID PRIMARY KEY,
    plan_team_id UUID REFERENCES plan_teams(id),
    user_id UUID,
    role VARCHAR(50),
    created_at TIMESTAMP
);

-- Plan Releases
CREATE TABLE plan_releases (
    id UUID PRIMARY KEY,
    plan_id UUID REFERENCES plans(id),
    name VARCHAR(255) NOT NULL,
    release_date DATE,
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, APPROVED, RELEASED
    approved_by UUID,
    approved_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Issue Dependencies (for plan view)
CREATE TABLE issue_dependencies (
    id UUID PRIMARY KEY,
    source_issue_id UUID REFERENCES issues(id),
    target_issue_id UUID REFERENCES issues(id),
    dependency_type VARCHAR(50) NOT NULL, -- 'BLOCKS', 'IS_BLOCKED_BY', 'RELATES_TO'
    plan_id UUID REFERENCES plans(id),
    created_at TIMESTAMP
);

-- Plan Warnings
CREATE TABLE plan_warnings (
    id UUID PRIMARY KEY,
    plan_id UUID REFERENCES plans(id),
    warning_type VARCHAR(50) NOT NULL,
    issue_id UUID REFERENCES issues(id),
    message TEXT,
    is_acknowledged BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);
```

---

# SECTION 5: IMPLEMENTATION PHASES

## Phase 1: Core Infrastructure (Week 1-2)

### 1.1 Database Migrations
- Create programs table
- Create plans table
- Create plan_items table
- Create plan_teams table
- Create plan_releases table
- Create issue_dependencies table

### 1.2 Program Service & Controller
- ProgramService
- ProgramController
- ProgramRepository

### 1.3 Plan Service & Controller
- PlanService
- PlanController
- PlanRepository

---

## Phase 2: Backlog Management (Week 3-4)

### 2.1 Plan Items
- Add items to backlog
- Remove items from backlog
- Reorder items (LexoRank)
- Filter by type, status, assignee

### 2.2 Epic Hierarchy
- Display Epic > Story > Subtask hierarchy
- Calculate Epic progress

---

## Phase 3: Teams & Releases (Week 5-6)

### 3.1 Team Management
- Add teams to plan
- Track team capacity
- Team workload view

### 3.2 Release Management
- Create releases
- Tag issues with releases
- Release approval workflow

---

## Phase 4: Dependencies & Warnings (Week 7-8)

### 4.1 Dependency Management
- Add/remove dependencies
- Visualize dependencies
- Dependency warnings

### 4.2 Warning System
- Target date warnings
- Due date warnings
- Warning acknowledgment

---

## Phase 5: UI Views (Week 9-12)

### 5.1 Backlog UI
- Drag-drop reordering
- Inline editing
- Filtering panel

### 5.2 Timeline/Gantt View
- Visual roadmap
- Epic timeline bars

### 5.3 Export Feature
- CSV export
- Excel export

---

# SECTION 6: TECHNICAL NOTES

## LexoRank Implementation
For backlog ordering, implement LexoRank algorithm:
- Generate rank strings between existing items
- Handle concurrent insertions
- Support rebalancing when ranks get too close

## Issue Hierarchy Resolution
When loading backlog:
1. Fetch all issues for plan's projects
2. Build in-memory tree: Epic > Story > Subtask
3. Calculate rollup values (story points, progress)

## Performance Considerations
- Cache plan views with 5-minute TTL
- Use lazy loading for issue details
- Batch-load issue dependencies
- Implement pagination for large backlogs

---

# SECTION 7: API RESPONSE EXAMPLES

## Get Plan Backlog Response
```json
{
  "planId": "uuid",
  "planName": "Sample Plan",
  "items": [
    {
      "id": "uuid",
      "issueId": "uuid",
      "issueKey": "PROJ-1",
      "title": "Epic: User Authentication",
      "type": "EPIC",
      "status": "In Progress",
      "progress": 45,
      "storyPoints": 21,
      "children": [
        {
          "id": "uuid",
          "issueKey": "PROJ-2",
          "title": "Story 1",
          "type": "STORY",
          "status": "To Do",
          "storyPoints": 5,
          "children": [...]
        }
      ]
    }
  ],
  "pagination": {
    "page": 0,
    "size": 50,
    "totalElements": 120
  }
}
```

## Get Plan Warnings Response
```json
{
  "planId": "uuid",
  "warnings": [
    {
      "id": "uuid",
      "type": "TARGET_DATE_BEYOND_DUE",
      "issueKey": "PROJ-5",
      "message": "Target date extends beyond due date"
    },
    {
      "id": "uuid",
      "type": "ISSUE_DATE_MISSING",
      "issueKey": "PROJ-8",
      "message": "Issue has no start or due date"
    }
  ],
  "stats": {
    "total": 15,
    "targetDateBeyondDue": 5,
    "issueDatesMissing": 10
  }
}
```

---

# SECTION 8: FILES TO CREATE

## New Services:
```
jira-plan-service/src/main/java/com/jira/plan/
├── service/
│   ├── ProgramService.java
│   ├── PlanService.java
│   ├── PlanItemService.java
│   ├── PlanTeamService.java
│   ├── PlanReleaseService.java
│   └── DependencyService.java
├── controller/
│   ├── ProgramController.java
│   ├── PlanController.java
│   ├── BacklogController.java
│   ├── TeamController.java
│   ├── ReleaseController.java
│   └── DependencyController.java
├── entity/
│   ├── Program.java
│   ├── Plan.java
│   ├── PlanItem.java
│   ├── PlanTeam.java
│   ├── PlanRelease.java
│   └── IssueDependency.java
├── dto/
│   ├── CreateProgramRequest.java
│   ├── ProgramResponse.java
│   ├── CreatePlanRequest.java
│   ├── PlanResponse.java
│   ├── BacklogItemResponse.java
│   ├── TeamResponse.java
│   ├── ReleaseResponse.java
│   └── DependencyResponse.java
└── repository/
    ├── ProgramRepository.java
    ├── PlanRepository.java
    ├── PlanItemRepository.java
    ├── PlanTeamRepository.java
    ├── PlanReleaseRepository.java
    └── IssueDependencyRepository.java
```

---

**Document Status:** Ready for Implementation  
**Estimated Total Effort:** 12 weeks  
**Priority:** HIGH - Core portfolio management feature