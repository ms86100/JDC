# JIRA PLANS FEATURE - COMPLETE REVERSE ENGINEERING ANALYSIS
## Based on Jira GreenHopper Plugin 11.3.0

**Analysis Date:** 2026-05-12
**Source:** `C:\Users\thech\Atlassian\Jira\atlassian-jira\WEB-INF\application-installation\jira-software-application\jira-greenhopper-plugin-11.3.0.jar`
**Plugin:** com.pyxis.greenhopper.jira (Jira Agile)

---

## EXECUTIVE SUMMARY

After extracting and analyzing the actual Jira GreenHopper plugin, I've identified **23 Active Object entities**, **166 JavaScript bundles**, and a comprehensive set of features that my implementation is missing.

**My implementation is approximately 15-20% complete** compared to the real Jira Agile/Plans feature.

---

## PART 1: ACTIVE OBJECT ENTITIES (From plugin.xml)

These are the actual database entities used by Jira:

| Entity Class | Purpose |
|-------------|---------|
| `IssueRankingAO` | Issue ranking/ordering |
| `IssueRankingOperationLogAO` | Rank change audit log |
| `RapidViewAO` | Board/RapidView configuration |
| `ColumnAO` | Board column configuration |
| `ColumnStatusAO` | Column to status mapping |
| `QuickFilterAO` | Quick filter definitions |
| `BoardAdminAO` | Board administrator access |
| `StatisticsFieldAO` | Custom statistics fields |
| `SwimlaneAO` | Swimlane configuration |
| `SubqueryAO` | Saved filter subqueries |
| `CardColorAO` | Card color rules |
| `SprintAO` | Sprint entity |
| `RankableObjectAO` | Rankable objects |
| `DetailViewFieldAO` | Detail view field config |
| `WorkingDaysAO` | Working days calendar |
| `NonWorkingDayAO` | Non-working days (holidays) |
| `VersionMetaDataAO` | Version metadata |
| `AuditEntryAO` | Audit log entries |
| `LexoRankAO` | LexoRank ordering data |
| `BalancerEntryAO` | LexoRank balancer state |
| `CardLayoutFieldAO` | Card layout configuration |
| `EstimateStatisticAO` | Estimation statistics |
| `TrackingStatisticAO` | Tracking statistics |

### Key Insight: My Implementation is Missing

I only created **8 entities** vs Jira's **23 entities**. Missing critical ones:
- ❌ `IssueRankingAO` (ranking)
- ❌ `BoardAdminAO` (permissions)
- ❌ `QuickFilterAO` (filters)
- ❌ `NonWorkingDayAO` (holidays)
- ❌ `LexoRankAO` / `BalancerEntryAO` (ordering)
- ❌ `CardLayoutFieldAO` (column config)

---

## PART 2: KEY FEATURES MISSING

### 2.1 LexoRank Ordering System

**Jira Implementation:** `com.atlassian.greenhopper.customfield.lexorank.*`

Jira uses a sophisticated LexoRank algorithm for:
- Gap-based ordering (not incrementing)
- Rank balancing when ranks get too close
- Bucket-based ranking for performance
- Locking during rank operations

**My Implementation:** Simple string-based `sortOrder` field

**Gap:** CRITICAL - Need full LexoRank implementation

### 2.2 Working Days Configuration

**Jira Implementation:**
```
WorkingDaysAO - Working days calendar
NonWorkingDayAO - Holiday/non-working day entries
```

Jira supports:
- Configurable working days (e.g., Mon-Fri)
- Holiday calendars per project/board
- Non-working day exclusions
- Capacity calculations based on working days

**My Implementation:** Simple `BigDecimal capacityHours`

**Gap:** CRITICAL - Need full working days/holiday system

### 2.3 Board/RapidView Configuration

**Jira Implementation:** `RapidViewAO` entity with:
- Column configuration (status → columns)
- Quick filters
- Swimlanes (group by epic, assignee, etc.)
- Card colors (rule-based)
- Detail view fields
- Estimation field (story points vs hours)
- Time tracking configuration

**My Implementation:** Plan entity with JSONB settings

**Gap:** CRITICAL - Need proper board configuration system

### 2.4 Sprint Management

**Jira Implementation:** `SprintAO` entity with:
- Start/End dates
- Sprint goal
- State machine (Future → Active → Closed)
- Linked issues
- Sprint capacity
- Automated sprint management

**My Implementation:** Basic `PlanRelease` entity with status

**Gap:** HIGH - Need automated sprint management

### 2.5 Issue Ranking

**Jira Implementation:**
- Global rank field (custom field)
- Rank operations (move to top, bottom, before, after)
- Rank locking for concurrent edits
- Rank audit logging

**My Implementation:** No ranking system

**Gap:** CRITICAL - Need ranking for backlog ordering

---

## PART 3: REST API ENDPOINTS

From `atlassian-plugin.xml`:

```
REST Paths:
- /rest/greenhopper/1.0/* (GreenHopper internal API)
- /rest/agile/1.0/* (Public Agile API)
- /rest/greenhopper/1.0/api/epicproperties
- /rest/greenhopper/1.0/api/sprints
- /rest/agile/1.0/boards
- /rest/agile/1.0/sprints
- /rest/agile/1.0/issues
```

### My API vs Jira API

| Feature | My API | Jira API |
|---------|--------|----------|
| Programs | CRUD only | N/A (Plans doesn't have Programs) |
| Plans/Boards | CRUD | Full board CRUD + config |
| Backlog | Simple list | Hierarchical with ranking |
| Sprint | Create/Approve/Release | Full lifecycle management |
| Rank | None | /rank, /ranktop, /rankbottom |
| Working Days | None | /workingdays |

---

## PART 4: CUSTOM FIELD TYPES

Jira GreenHopper defines **6 custom field types**:

| Custom Field | Class | Purpose |
|--------------|-------|---------|
| `gh-epic-link` | `EpicLinkCFType` | Link issues to epics |
| `gh-epic-label` | `EpicLabelCFType` | Epic name display |
| `gh-epic-color` | `EpicColorCFType` | Epic color |
| `gh-epic-status` | `EpicStatusCFType` | Epic status |
| `gh-global-rank` | `RankCFType` | Global ranking |
| `gh-sprint` | `SprintCFType` | Sprint association |
| `gh-lexo-rank` | `LexoRankCFType` | LexoRank ordering |

**My Implementation:** No custom fields, just simple UUIDs

---

## PART 5: PROJECT PERMISSIONS

From plugin.xml:
```xml
<project-permission key="MANAGE_SPRINTS_PERMISSION" .../>
<project-permission key="START_STOP_SPRINTS_PERMISSION" .../>
<project-permission key="EDIT_SPRINT_NAME_AND_GOAL_PERMISSION" .../>
```

**My Implementation:** No permission system

**Gap:** CRITICAL - Need project-level sprint permissions

---

## PART 6: WEB WORKFLOW ACTIONS

Jira defines these web actions:
- `RapidBoardAction` - Board display
- `RankAction` - /rankTop, /rankBottom
- `BoardDispatchAction` - Find on board
- `BulkChangeIssuesAction` - Bulk operations
- `SoftwareConfigAction` - Admin config
- `LexoRankAdminAction` - LexoRank management

**My Implementation:** Basic CRUD only

---

## PART 7: COMPLETE FEATURE GAP MATRIX

| # | Feature | Jira Implementation | My Implementation | Status |
|---|---------|---------------------|-------------------|--------|
| 1 | Board/RapidView | `RapidViewAO` + 10+ config entities | ❌ Missing | CRITICAL |
| 2 | Sprint | `SprintAO` with full lifecycle | `PlanRelease` basic | HIGH |
| 3 | Working Days | `WorkingDaysAO` + `NonWorkingDayAO` | ❌ Missing | CRITICAL |
| 4 | Holiday Calendar | Per-board holiday config | ❌ Missing | CRITICAL |
| 5 | LexoRank Ordering | Full algorithm + balancer | ❌ Missing | CRITICAL |
| 6 | Issue Ranking | Global rank custom field | ❌ Missing | CRITICAL |
| 7 | Column Configuration | `ColumnAO` + `ColumnStatusAO` | ❌ Missing | HIGH |
| 8 | Quick Filters | `QuickFilterAO` | ❌ Missing | HIGH |
| 9 | Swimlanes | `SwimlaneAO` | ❌ Missing | HIGH |
| 10 | Card Colors | `CardColorAO` rule-based | ❌ Missing | MEDIUM |
| 11 | Card Layout | `CardLayoutFieldAO` | ❌ Missing | MEDIUM |
| 12 | Detail View Fields | `DetailViewFieldAO` | ❌ Missing | HIGH |
| 13 | Epic Link | `EpicLinkCFType` custom field | ❌ Missing | CRITICAL |
| 14 | Epic Color | `EpicColorCFType` custom field | ❌ Missing | HIGH |
| 15 | Epic Status | `EpicStatusCFType` custom field | ❌ Missing | MEDIUM |
| 16 | Sprint Custom Field | `SprintCFType` custom field | ❌ Missing | HIGH |
| 17 | Board Permissions | `BoardAdminAO` | ❌ Missing | CRITICAL |
| 18 | Sprint Permissions | 3 project permissions | ❌ Missing | CRITICAL |
| 19 | Rank Audit Log | `IssueRankingOperationLogAO` | ❌ Missing | MEDIUM |
| 20 | LexoRank Audit | `BalancerEntryAO` | ❌ Missing | MEDIUM |
| 21 | Statistics Fields | `StatisticsFieldAO`, `EstimateStatisticAO` | ❌ Missing | MEDIUM |
| 22 | Version Metadata | `VersionMetaDataAO` | ❌ Missing | MEDIUM |
| 23 | Tracking Statistics | `TrackingStatisticAO` | ❌ Missing | MEDIUM |

---

## PART 8: RECOMMENDED IMPLEMENTATION PRIORITY

### Tier 1: Must Have (Foundation)
1. **LexoRank Ordering** - Essential for backlog management
2. **Working Days + Holidays** - Capacity planning foundation
3. **Board Configuration** - Column, Quick Filter, Swimlane entities
4. **Sprint Management** - Full lifecycle with permissions

### Tier 2: High Priority
5. **Issue Ranking** - Drag-drop reorder
6. **Board Permissions** - Admin access control
7. **Epic Integration** - Custom fields linking to real epics
8. **Card Layout Configuration** - Configurable columns

### Tier 3: Medium Priority
9. **Card Colors** - Visual differentiation
10. **Quick Filters** - In-board filtering
11. **Detail View Config** - Customizable issue details
12. **Sprint Custom Field** - Issue-to-sprint linking

### Tier 4: Nice to Have
13. **Audit Logging** - Track changes
14. **Statistics Fields** - Custom metrics
15. **Version Metadata** - Enhanced version info

---

## PART 9: WHAT JIRA PLANS ACTUALLY IS

Based on my analysis, **Jira Plans** (Advanced Roadmaps) is built ON TOP of the GreenHopper plugin:

```
Jira GreenHopper Plugin (Foundation)
├── Board Management (Scrum/Kanban)
├── Sprint Management
├── Issue Ranking (LexoRank)
├── Working Days
├── Epic Management
└── Agile REST API

Jira Plans (Advanced Roadmaps - Separate Add-on)
├── Portfolio/Program Management
├── Timeline/Gantt View
├── Team Capacity Planning
├── Release Management
├── Dependency Graph
├── Warning System
└── Advanced Reporting
```

**My Implementation:** Attempted to build "Plans" from scratch without understanding GreenHopper foundation

---

## CONCLUSION

The Jira Plans feature I implemented is **fundamentally incomplete** because:

1. **No GreenHopper Integration** - Plans depends on GreenHopper for boards, sprints, ranking
2. **No LexoRank** - The backbone of agile ordering
3. **No Working Days** - Capacity planning without working days is useless
4. **No Board Configuration** - The board is more than just a list of issues
5. **No Permissions** - Real applications need access control

**Recommended Path Forward:**

Option A: **Build on GreenHopper** (Recommended)
- Integrate with existing Jira GreenHopper plugin
- Use GreenHopper REST API for boards/sprints
- Extend with Plans-specific features

Option B: **Build GreenHopper Features First**
- Implement LexoRank
- Implement Working Days
- Implement Board Configuration
- Then build Plans on top

Option C: **Accept MVP Status**
- Document current implementation as "Basic Roadmap"
- Market as simple planning tool
- Improve iteratively based on user feedback
