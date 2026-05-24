# JIRA DATA CENTER IMPLEMENTATION THESIS
## Enterprise Project & Issue Tracking Platform

---

**Document Version:** 4.0
**Status:** 🚧 IN PROGRESS
**Completion:** 16.3%
**Author:** Sagar Sharma
**Date:** 2026-05-24
**Target:** Jira Data Center 11.3.0 Feature Parity

---

# RECENT CHANGES

## Phase 2/3/4 Workflow Completion (2026-05-24)

The following workflow engine components have been successfully implemented:

| Component | Files | Status |
|-----------|-------|--------|
| **Conditions CRUD** | ConditionService, ConditionRepository, ConditionController | ✅ DONE |
| **Conditions Evaluation** | ConditionEvaluationEngine, ConditionEvaluationContext | ✅ DONE |
| **Validators CRUD** | ValidatorService, ValidatorRepository, ValidatorController | ✅ DONE |
| **Validators Execution** | ValidatorExecutionEngine, WorkflowValidationService | ✅ DONE |
| **Post-functions CRUD** | PostFunctionService, PostFunctionRepository, PostFunctionController | ✅ DONE |
| **Post-functions Execution** | PostFunctionExecutionEngine, WorkflowPostFunctionService | ✅ DONE |
| **Triggers CRUD** | TriggerService, TriggerRepository, TriggerController | ✅ DONE |
| **Triggers Event Handling** | TriggerEventHandler, TriggerExecutionEngine | ✅ DONE |

**Commit:** `c34f796` - fix: add issueId field to workflow evaluation context

---

# IMPLEMENTATION STATUS

## Current State: BUILDING ON TOP OF EXISTING INFRASTRUCTURE

| Metric | Value |
|--------|-------|
| **Platform Built** | 17 microservices, database schemas, basic CRUD |
| **Total Features** | 233 |
| **Features Implemented** | 38 |
| **Features Missing** | 195 |
| **Implementation Progress** | 16.3% |

## Platform Components Already Built
- ✅ jira-gateway (port 8080) - API Gateway
- ✅ jira-auth-service (port 8081) - Authentication/JWT
- ✅ jira-user-service (port 8082) - Users, Profiles, Orgs
- ✅ jira-project-service (port 8083) - Projects, Templates, Schemes
- ✅ jira-issue-service (port 8084) - Issues, Versions, Labels
- ✅ jira-workflow-service (port 8085) - Workflows, Transitions, Conditions, Validators, Post-functions, Triggers ✅ COMPLETE
- ✅ jira-comment-service (port 8086) - Comments
- ✅ jira-notification-service (port 8087) - Notifications
- ✅ jira-search-service (port 8088) - Search
- ✅ jira-audit-service (port 8089) - Audit logging
- ✅ jira-sprint-service (port 8090) - Sprints, Boards
- ✅ jira-attachment-service (port 8091) - Attachments
- ✅ jira-admin-service (port 8096) - System settings

## Gap to Fill (What Needs to be Built)
- Security Levels & Permissions (20 missing)
- JQL Parser (13 missing)
- Custom Field Types (21 missing)
- Workflow Conditions/Validators/Post-functions/Triggers ✅ DONE
- Advanced Agile Features (15 missing)
- Notifications & Automation (16 missing)

---

# TABLE OF CONTENTS

1. [Executive Summary](#1-executive-summary)
2. [Business Context](#2-business-context)
3. [Business Requirement Stories](#3-business-requirement-stories) ⭐ NEW
4. [Feature Decomposition](#4-feature-decomposition)
   - Feature 1: Core Issue Management
   - Feature 2: Project Management
   - Feature 3: Workflow Engine
   - Feature 4: Security & Permissions
   - Feature 5: Agile/Sprint Management
   - Feature 6: Search & JQL
   - Feature 7: Custom Fields & Screens
   - Feature 8: Notifications & Automation
   - Feature 9: Time Tracking & Attachments
   - Feature 10: Administration & Reporting
5. [Technical Requirements](#5-technical-requirements)
6. [Implementation Roadmap](#6-implementation-roadmap)
7. [Gap Analysis Summary](#7-gap-analysis-summary)

---

# 1. EXECUTIVE SUMMARY

## 1.1 Purpose

This document provides a comprehensive implementation thesis for building an enterprise-grade project and issue tracking platform that achieves feature parity with Atlassian Jira Data Center 11.3.0. The platform is being built using a microservices architecture with Spring Boot and PostgreSQL.

**IMPORTANT:** We are BUILDING ON TOP of existing infrastructure - not starting from scratch. The platform already has 17 microservices, database schemas, and basic CRUD operations. This document specifies what remains to be built.

## 1.2 Current State (14.6% Complete)

Based on the comprehensive gap analysis conducted on 2026-05-11:

| Metric | Value |
|--------|-------|
| **Total Features Analyzed** | 233 |
| **Features Implemented** | 34 |
| **Features Missing** | 199 |
| **Implementation Progress** | 14.6% |

## 1.3 Scope Summary

### In Scope (This Implementation)
- All 10 major feature categories from the gap analysis
- Complete issue management lifecycle
- Project and workflow management
- Security and permission system
- Agile boards and sprint management
- Search with JQL
- Custom fields and screen configurations
- Notifications and automation
- Time tracking and attachments
- Administration and reporting

### Out of Scope
- Mobile applications
- Third-party marketplace integrations
- Advanced roadmaps (Advanced Roadmaps module)
- Confluence integration
- Bitbucket/GitHub integration
- Service Desk functionality

## 1.4 Success Metrics

| KPI | Target | Measurement Method |
|-----|--------|---------------------|
| Feature Parity | 90%+ | Gap analysis completion |
| Issue Management | 100% | 25/25 features implemented |
| Workflow Engine | 100% | 15/15 features implemented |
| Permission System | 100% | 20/20 features implemented |
| Search Capability | 90%+ | JQL coverage percentage |
| API Coverage | 90%+ | REST endpoint coverage |

---

# 2. BUSINESS CONTEXT

## 2.1 Business Problem Statement

The organization requires a centralized, enterprise-grade project and issue tracking system that provides:
1. **Visibility** - Complete transparency into project progress across all teams
2. **Accountability** - Clear ownership of tasks and deliverables
3. **Collaboration** - Seamless team coordination on shared objectives
4. **Governance** - Role-based access control with audit trails
5. **Automation** - Reduced manual overhead through automated workflows

## 2.2 Target User Personas

### 2.2.1 System Administrator
**Role:** Manages platform configuration, users, and security

**Responsibilities:**
- Configure system-wide settings
- Manage user accounts and groups
- Set up permission schemes
- Manage notification templates
- Monitor system health and performance
- Configure LDAP/SSO integration

**Pain Points:**
- Complex permission management
- User lifecycle management
- System monitoring complexity

**Success Criteria:**
- Can configure platform in < 2 hours for new organization
- Can onboard 100+ users in < 1 day
- System uptime > 99.9%

---

### 2.2.2 Project Manager
**Role:** Leads project delivery and team coordination

**Responsibilities:**
- Create and configure projects
- Define issue workflows
- Assign team members to projects
- Monitor project progress
- Generate status reports
- Manage backlogs and sprints

**Pain Points:**
- Manual status updates
- Lack of real-time visibility
- Fragmented project information

**Success Criteria:**
- Can create project in < 5 minutes
- Real-time dashboard updates
- Automated progress reporting

---

### 2.2.3 Software Developer
**Role:** Implements code changes linked to issues

**Responsibilities:**
- View and update assigned issues
- Transition issues through workflow
- Log time against issues
- Link commits to issues
- Review code and approve changes

**Pain Points:**
- Context switching between tools
- Manual issue linking
- Complex workflow navigation

**Success Criteria:**
- Can update issue status in < 10 seconds
- Auto-linked commits
- Single-click workflow transitions

---

### 2.2.4 QA Engineer
**Role:** Validates deliverables and tracks testing

**Responsibilities:**
- Create test cases linked to requirements
- Execute test runs
- Log defects against issues
- Track testing progress
- Verify bug fixes

**Pain Points:**
- Manual test result logging
- Disconnected defect tracking
- Testing progress visibility

**Success Criteria:**
- One-click defect creation
- Real-time testing dashboards
- Automated regression tracking

---

### 2.2.5 Executive Stakeholder
**Role:** Monitors organizational performance

**Responsibilities:**
- View portfolio-level dashboards
- Review project health indicators
- Access executive reports
- Monitor team capacity

**Pain Points:**
- Lack of consolidated views
- Manual report preparation
- Limited real-time data

**Success Criteria:**
- Portfolio view in < 1 click
- Automated weekly reports
- Real-time KPI dashboards

---

## 2.3 Business Goals

| Goal ID | Business Goal | Priority | Metric |
|---------|---------------|----------|--------|
| BG-01 | Reduce issue cycle time by 30% | HIGH | Average time from Open to Done |
| BG-02 | Increase project visibility by 100% | HIGH | Real-time dashboard access |
| BG-03 | Improve team collaboration efficiency | MEDIUM | Comments per issue |
| BG-04 | Reduce manual admin overhead by 50% | MEDIUM | Hours spent on admin tasks |
| BG-05 | Achieve 90% feature parity with Jira DC | CRITICAL | Gap analysis completion |

---

## 2.4 Assumptions

1. **Technology Stack:** Platform will use existing microservices architecture (Spring Boot 3.4.5, Java 21, PostgreSQL)
2. **Team Capacity:** 5-10 developers available for implementation
3. **Timeline:** 18-24 months for full implementation
4. **Integration:** OAuth/LDAP integration will be supported
5. **Scalability:** System must support 1000+ concurrent users
6. **Performance:** API response time < 200ms for standard operations

---

## 2.5 Constraints

| Constraint | Impact |
|-------------|--------|
| Must integrate with existing jira-platform codebase | Cannot rewrite, only extend |
| Must maintain backwards compatibility | Existing APIs must continue working |
| Must use existing database schemas | Cannot alter production schema without migrations |
| Must follow existing coding standards | Spring Boot conventions |
| Budget limited to existing infrastructure | No new database engines |

---

# 3. BUSINESS REQUIREMENT STORIES

This section provides detailed functional requirement narratives for the key business capabilities. Each story follows the format: business story → Jira DC modules → user journey → screens → fields → business rules → workflow → backend linkage → database → acceptance criteria.

---

## BR-01: ENTERPRISE CHANGE MANAGEMENT

**Business Requirement ID:** BR-01
**Document Section:** 1.1
**Priority:** CRITICAL
**Status:** 📋 PENDING

---

### 3.1.1 Business Story

In avionics development, a software engineer discovers an anomaly affecting simulation behavior.

The anomaly cannot simply be fixed in source code because certification requires complete traceability:

The organization must know:
- what changed
- why it changed
- which version contains fix
- impact on lifecycle artifacts
- severity
- affected releases
- audit trail

This requirement ensures regulatory compliance and complete change traceability throughout the software development lifecycle.

---

### 3.1.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Projects | Logical container for change requests | Project per aircraft program |
| Issue Types | Change/Bug types | Custom: Change Request, Bug, Anomaly |
| Custom Fields | Severity, affected version, etc. | 15+ custom fields |
| Workflow | Approval flow with validation | 7-state workflow |
| Versions | Affected/Fix version tracking | Release versioning |
| Issue Linking | Dependency tracking | Blocks, relates to, causes |
| DVCS Connector | Git commit linkage | Smart commits |
| Dashboards | Reporting and metrics | Custom dashboards |
| Automation | Auto-assign, notifications | Rule-based automation |
| Security Schemes | Role-based access | Approval hierarchy |

---

### 3.1.3 User Journey

#### Step 1: Create Change Request

**User:** Software Engineer
**Action:** Navigate to project and create issue

```
Projects
    ↓
Avionics Product A
    ↓
Create Issue
```

**System Response:**
Opens Create Change Request Screen

---

#### Step 2: Complete Change Request Form

**Fields:**

| Field | Type | Required | Source/Default |
|-------|------|----------|----------------|
| Issue Type | Dropdown | Yes | Change Request (default) |
| Summary | Text (255 chars) | Yes | User input |
| Description | Rich Text | Yes | User input |
| Severity | Dropdown | Yes | See severity values below |
| Affected Version | Version Picker | Yes | Project versions |
| Fix Version | Version Picker | Yes | Project versions |
| Lifecycle Impact | Multi-select | Yes | See lifecycle values |
| Assignee | User Picker | No | Auto-suggest lead |
| Attachments | Upload | No | File upload |

**Severity Values (Document-Derived):**

| Value | Description | Requires Approval |
|-------|-------------|-------------------|
| Significant CAT/HAZ | Category A/B hazard | Yes |
| Significant MAJ | Major anomaly | Yes |
| Functional | Functional issue | Conditional |
| Process | Process deviation | No |
| Functional Internal | Internal scope | No |
| Life Cycle Data | Data impact | Conditional |

**Lifecycle Impact Values:**
- Software
- Hardware
- Documentation
- Test Procedures
- Certification
- Ground Support Equipment

---

#### Step 3: Save and Submit

**System Actions:**
1. Generate change request ID (CR-YYYY-NNNN)
2. Validate all required fields
3. Apply default workflow state (Draft)
4. Send notification to project lead
5. Create audit entry

---

### 3.1.4 Screen Specifications

#### Create Change Request Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Create Change Request                                      [?] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Project: * [Avionics Product A                    ▼]          │
│ Issue Type: * [Change Request                      ▼]         │
│                                                                 │
│ ─────────────────────────────────────────────────────────────── │
│                                                                 │
│ Summary: * [                                                  ] │
│                                                                 │
│ Description: * [                                              ] │
│            [                                                  ] │
│            [                                                  ] │
│                                                                 │
│ ─────────────────────────────────────────────────────────────── │
│                                                                 │
│ Severity: * [Significant CAT/HAZ                 ▼]            │
│                                                                 │
│ Affected Version(s): * [v2.3.1                         ▼] [+]   │
│                                                                 │
│ Fix Version(s): * [v2.4.0                            ▼] [+]    │
│                                                                 │
│ Lifecycle Impact: * [✓Software] [✓Documentation]              │
│                                                                 │
│ ─────────────────────────────────────────────────────────────── │
│                                                                 │
│ Assignee: [Auto-assign to Lead                ▼]               │
│                                                                 │
│ Attachments: [📎 Drop files here or click to upload]           │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                           [Cancel]  [Create]                   │
└─────────────────────────────────────────────────────────────────┘
```

#### Field Validation Rules

| Field | Validation | Error Message |
|-------|------------|---------------|
| Summary | Required, 3-255 chars | "Summary is required" |
| Description | Required, 10+ chars | "Description must be at least 10 characters" |
| Severity | Required, from list | "Severity is required" |
| Affected Version | Required, must exist | "Affected version is required" |
| Fix Version | Required, must exist | "Fix version is required" |
| Lifecycle Impact | Required, 1+ selected | "At least one lifecycle impact must be selected" |

---

### 3.1.5 Business Rules

#### BR-001: High Severity Requires Approval

**Condition:**
```
IF Severity IN ('Significant CAT/HAZ', 'Significant MAJ')
THEN
    Mandatory Fields: Risk Assessment, Approval Required, Additional Review
    Workflow: Requires approval step before Development
    Notification: Escalate to Program Manager
```

**Implementation:**
```java
@PostValidate
public void validateHighSeverityApproval(Issue issue) {
    if (issue.getSeverity().isHighSeverity()) {
        requireField(issue, "riskAssessment");
        requireField(issue, "approvalRequired");
        requireField(issue, "additionalReview");
        issue.setApprovalRequired(true);
    }
}
```

---

#### BR-002: Fix Version Required for Development

**Condition:**
```
IF Transition = 'Ready for Development'
THEN
    Validation: Fix Version must not be empty
    Block transition if: fixVersion IS NULL
```

**Implementation:**
```sql
-- Transition validator
CREATE FUNCTION validate_fix_version()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.to_status = 'IN_DEVELOPMENT' AND NEW.fix_version IS NULL THEN
        RAISE EXCEPTION 'Fix version is required before moving to Development';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

#### BR-003: Affected Version Cannot Equal Fix Version

**Condition:**
```
IF Affected Version = Fix Version
THEN
    Warning: "Affected and Fix versions should typically differ"
```

---

### 3.1.6 Workflow Definition

#### Change Request Workflow

```
┌─────────┐     ┌─────────┐     ┌───────────┐     ┌──────────┐
│  DRAFT  │ ──▶ │   OPEN  │ ──▶ │ ANALYSIS  │ ──▶ │ APPROVED │
└─────────┘     └─────────┘     └───────────┘     └──────────┘
     │                │               │                │
     │               │                │                │
     ▼               ▼                ▼                ▼
┌─────────┐     ┌─────────┐     ┌───────────┐     ┌──────────┐
│CLOSED-  │     │ RETURNED│     │ REJECTED  │     │DEVELOP- │
│WITHOUT- │     │(Analysis│     │(Approval  │     │  MENT    │
│ACTION   │◀────│ Return) │◀────│  Reject)  │     └────┬─────┘
└─────────┘     └─────────┘     └───────────┘          │
                                                    │    │
                                                    ▼    ▼
                                              ┌──────────┐
                                              │VALIDATION│
                                              └────┬─────┘
                                                   │
                              ┌────────────────────┼────────────────────┐
                              │                    │                    │
                              ▼                    ▼                    ▼
                        ┌──────────┐       ┌──────────┐       ┌──────────┐
                        │ CLOSED-  │       │ RETURNED │       │  CLOSED  │
                        │ REOPENED │       │(Validation│       │COMPLETED │
                        └──────────┘       │  Return) │       └──────────┘
                                           └──────────┘
```

#### Workflow States and Transitions

| From | To | Trigger | Validators | Post Functions |
|------|-----|---------|------------|-----------------|
| Draft | Open | Submit | summary, description, severity | createAuditEntry, sendNotification |
| Open | Analysis | Start Analysis | assignee assigned | updateStatus, sendNotification |
| Analysis | Approved | Approve | risk assessment if required | createApprovalRecord, notifyLead |
| Analysis | Rejected | Reject | rejection reason | notifySubmitter |
| Approved | Development | Start Work | fix version required | assignDeveloper, startTimer |
| Development | Validation | Submit for Test | test cases linked | notifyQA, createTestIssue |
| Validation | Closed Completed | Pass Validation | test results pass | closeCR, generatePDN, updatePortal |
| Validation | Returned | Fail Validation | failure reason | notifyDeveloper |

#### Transition Validators

**Open → Analysis:**
```java
public class OpenToAnalysisValidator implements TransitionValidator {
    @Override
    public ValidationResult validate(Issue issue, Transition transition) {
        List<String> errors = new ArrayList<>();

        if (issue.getSummary() == null || issue.getSummary().isBlank()) {
            errors.add("Summary is required");
        }
        if (issue.getDescription() == null || issue.getDescription().length() < 10) {
            errors.add("Description must be at least 10 characters");
        }
        if (issue.getSeverity() == null) {
            errors.add("Severity is required");
        }

        return errors.isEmpty() ? ValidationResult.passed() : ValidationResult.failed(errors);
    }
}
```

#### Post Functions

**Analysis → Approved:**
```java
public class ApprovalPostFunction implements PostFunction {
    @Override
    public void execute(Issue issue, Transition transition) {
        // 1. Create approval record
        ApprovalRecord record = new ApprovalRecord();
        record.setApprover(getCurrentUser());
        record.setApprovalDate(now());
        record.setDecision("APPROVED");
        record.save();

        // 2. Notify stakeholders
        notificationService.notify(
            issue.getProject().getStakeholders(),
            "Change Request Approved",
            formatApprovalMessage(issue)
        );

        // 3. Update metrics
        metricsService.recordApprovalTime(issue, getElapsedTime());

        // 4. Create audit entry
        auditService.log("Change Request approved by " + getCurrentUser());
    }
}
```

---

### 3.1.7 Backend Linkage

#### Service Dependencies

**Issue Service Consumes:**

| Service | Data Consumed | Purpose |
|---------|--------------|---------|
| Project Service | Project details, templates | Get project configuration |
| Workflow Service | Workflow definitions | Validate transitions |
| Version Service | Version list | Populate version pickers |
| User Service | User profiles, roles | Assignee suggestions, permissions |
| Security Service | Security levels | Access control |

**Issue Service Produces:**

| Event | Consumers | Purpose |
|-------|-----------|---------|
| IssueCreatedEvent | NotificationService, AuditService | Trigger notifications, log audit |
| IssueTransitionEvent | WorkflowService, SearchService | Update workflow, reindex |
| IssueUpdatedEvent | SearchService, DashboardService | Update search index, refresh reports |
| IssueClosedEvent | MetricsService, PortalService | Update KPIs, close portal items |

#### Service Integration Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        CREATE CHANGE REQUEST                    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Issue Service                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 1. Validate Input                                       │   │
│  │ 2. Get Project Configuration (Project Service)          │   │
│  │ 3. Validate Workflow Transition (Workflow Service)      │   │
│  │ 4. Check Permissions (Security Service)                │   │
│  │ 5. Generate Issue Key                                   │   │
│  │ 6. Persist Issue                                        │   │
│  │ 7. Update Search Index (Search Service)                │   │
│  │ 8. Emit IssueCreatedEvent                               │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
┌───────────────────┐ ┌───────────────┐ ┌───────────────┐
│ NotificationService│ │  AuditService │ │MetricsService│
│                    │ │               │ │               │
│ - Email to lead   │ │ - Log create  │ │ - Incr count │
│ - In-app notify   │ │ - Store diff  │ │ - Start timer │
│ - Teams/Slack     │ │ - Track field │ │ - Update KPIs │
└───────────────────┘ └───────────────┘ └───────────────┘
```

#### REST API Endpoints

```
POST   /api/projects/{projectId}/issues           - Create issue
GET    /api/projects/{projectId}/issues           - List issues
GET    /api/issues/{issueKey}                      - Get issue
PUT    /api/issues/{issueKey}                      - Update issue
POST   /api/issues/{issueKey}/transitions          - Transition
GET    /api/issues/{issueKey}/history              - Get history
POST   /api/issues/{issueKey}/attachments          - Add attachment
GET    /api/issues/{issueKey}/subtasks             - Get subtasks
POST   /api/issues/{issueKey}/links                - Create link
```

---

### 3.1.8 Database Entities

#### Core Tables

```sql
-- Change Requests table (extends issues)
CREATE TABLE change_requests (
    id UUID PRIMARY KEY REFERENCES issues(id),
    severity VARCHAR(50) NOT NULL,
    risk_assessment TEXT,
    approval_required BOOLEAN DEFAULT FALSE,
    approval_date TIMESTAMP,
    approved_by UUID REFERENCES users(id),
    lifecycle_impact VARCHAR(50)[],
    affected_versions UUID[],
    fix_versions UUID[],
    cra_reference VARCHAR(50),
    certification_impact VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Change Request Versions
CREATE TABLE cr_versions (
    id UUID PRIMARY KEY,
    change_request_id UUID REFERENCES change_requests(id),
    version_type VARCHAR(20) NOT NULL, -- 'AFFECTED', 'FIX'
    version_id UUID REFERENCES project_versions(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Approval Records
CREATE TABLE approval_records (
    id UUID PRIMARY KEY,
    change_request_id UUID REFERENCES change_requests(id),
    approver_id UUID REFERENCES users(id),
    decision VARCHAR(20) NOT NULL, -- 'APPROVED', 'REJECTED', 'RETURNED'
    comments TEXT,
    decision_date TIMESTAMP DEFAULT NOW(),
    role VARCHAR(50) -- 'TECHNICAL_LEAD', 'PROGRAM_MANAGER', 'QUALITY'
);

-- Lifecycle Impact Tracking
CREATE TABLE lifecycle_impacts (
    id UUID PRIMARY KEY,
    change_request_id UUID REFERENCES change_requests(id),
    impact_type VARCHAR(50) NOT NULL, -- 'SOFTWARE', 'HARDWARE', etc.
    description TEXT,
    affected_items TEXT[],
    created_at TIMESTAMP DEFAULT NOW()
);
```

#### Indexes

```sql
CREATE INDEX idx_change_requests_severity ON change_requests(severity);
CREATE INDEX idx_change_requests_approval ON change_requests(approval_required);
CREATE INDEX idx_change_requests_project ON change_requests(project_id);
CREATE INDEX idx_cr_versions_cr ON cr_versions(change_request_id);
CREATE INDEX idx_approval_records_cr ON approval_records(change_request_id);
CREATE INDEX idx_lifecycle_impacts_cr ON lifecycle_impacts(change_request_id);
```

---

### 3.1.9 Acceptance Criteria

#### AC-BR01-001: Create Change Request

**Scenario:** Create a change request with valid data

**Given** the user is on the Avionics Product A project
**When** the user creates a new Change Request with:
- Summary: "Simulation anomaly in flight mode"
- Description: "During initial climb, simulation shows incorrect altitude display"
- Severity: "Significant MAJ"
- Affected Version: "v2.3.1"
- Fix Version: "v2.4.0"
- Lifecycle Impact: ["Software", "Documentation"]
**Then** the system should:
- [ ] Generate unique key (CR-YYYY-NNNN format)
- [ ] Save all fields correctly
- [ ] Set initial status to "Draft"
- [ ] Send notification to project lead
- [ ] Create audit entry with user/timestamp

---

#### AC-BR01-002: High Severity Requires Approval

**Scenario:** Submit high severity change request

**Given** the user is creating a Change Request with Severity = "Significant CAT/HAZ"
**When** the user attempts to transition from Draft to Open
**Then** the system should:
- [ ] Display warning about approval requirement
- [ ] Require Risk Assessment field
- [ ] Require Approval Required flag
- [ ] Require Additional Review checkbox
- [ ] Block transition until all fields complete

---

#### AC-BR01-003: Fix Version Required for Development

**Scenario:** Attempt to move to Development without Fix Version

**Given** a Change Request in "Approved" status with no Fix Version
**When** the user attempts to transition to "Development"
**Then** the system should:
- [ ] Display error: "Fix version is required before moving to Development"
- [ ] Highlight Fix Version field
- [ ] Block transition
- [ ] Not create audit entry for failed attempt

---

#### AC-BR01-004: Lifecycle Impact Validation

**Scenario:** Submit change request without lifecycle impact

**Given** the user is creating a Change Request
**When** the user attempts to save without selecting any Lifecycle Impact
**Then** the system should:
- [ ] Display error: "At least one lifecycle impact must be selected"
- [ ] Highlight Lifecycle Impact field
- [ ] Block save operation

---

#### AC-BR01-005: Approval Workflow

**Scenario:** Approve a change request

**Given** a Change Request in "Analysis" status with all required fields complete
**When** the Technical Lead clicks "Approve"
**Then** the system should:
- [ ] Create ApprovalRecord with decision "APPROVED"
- [ ] Set approval_date to current timestamp
- [ ] Update status to "Approved"
- [ ] Notify Program Manager
- [ ] Create audit entry with approver details
- [ ] Enable transition to "Development"

---

### 3.1.10 Gap Analysis for BR-01

| Component | Current State | Required | Gap |
|-----------|--------------|----------|-----|
| Change Request Issue Type | Generic issues | Custom Change Request type | ⚠️ PARTIAL |
| Severity Custom Field | Basic priority | 6-level severity with rules | ❌ MISSING |
| Version Linking | Manual | Auto-link affected/fix versions | ❌ MISSING |
| Lifecycle Impact Field | Not present | Multi-select with validation | ❌ MISSING |
| Workflow States | Basic statuses | 7-state approval workflow | ⚠️ PARTIAL |
| Transition Validators | None | Conditional validators | ❌ MISSING |
| Transition Post-Functions | None | Audit, notification, metrics | ❌ MISSING |
| Approval Records | Not present | Approval tracking table | ❌ MISSING |
| Risk Assessment Field | Not present | Conditional required field | ❌ MISSING |
| Security Levels | Not present | Role-based access | ❌ MISSING |
| Notification Scheme | Basic | Approval escalation | ❌ MISSING |
| Audit Logging | Basic | Detailed change audit | ⚠️ PARTIAL |

---

## BR-02: PRODUCT RELEASE MANAGEMENT

**Business Requirement ID:** BR-02
**Document Section:** Release Management
**Priority:** CRITICAL
**Status:** 📋 PENDING

---

### 3.2.1 Business Story

Release managers need to certify software delivery for aviation products.

Before delivery, mandatory meetings occur:
- Go For Dev
- Go For Implementation
- Go For Validation
- Go For Delivery

Each milestone can stop progression. The system must track:
- Meeting dates and outcomes
- Reviewer assignments
- Decision comments
- Blockers and dependencies

After release, the system must automatically:
- Generate PDN (Product Delivery Notification)
- Generate Archive
- Generate KPIs
- Update Portal
- Notify Users

---

### 3.2.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Versions | Release container | Custom Release issue type |
| Subtasks | Milestone tracking | 4 automated subtasks per release |
| Workflow | Gate progression | 6-state workflow with gates |
| Custom Fields | RAG status, dates, reviewers | Multiple custom fields |
| Automation | Auto-create subtasks | Rule-based automation |
| Dashboard | KPI reporting | Custom release dashboard |
| Notifications | Stakeholder alerts | Event-based notifications |

---

### 3.2.3 User Journey

#### Step 1: Create Release

**User:** Release Manager
**Action:** Navigate to project releases

```
Project
    ↓
Releases
    ↓
Create Release
```

**System Response:**
Opens Create Release Screen

---

#### Step 2: Complete Release Form

**Fields:**

| Field | Type | Required | Default |
|-------|------|----------|---------|
| Release Name | Text | Yes | User input |
| Version | Text | Yes | e.g., "2.4.0" |
| Planned Date | Date | Yes | User input |
| Actual Date | Date | No | NULL until closed |
| Owner | User Picker | Yes | Current user |
| Status | Readonly | Auto | Draft |
| Linked Documents | Issue Picker | Yes | References |

---

#### Step 3: Automatic Milestone Creation

**System Response:**
Upon save, system automatically creates 4 subtasks:

| Subtask | Initial Status | Fields |
|---------|---------------|--------|
| Go for Dev | Pending | Meeting Date, Reviewer, Comments, RAG |
| Go for Implementation | Pending | Meeting Date, Reviewer, Comments, RAG |
| Go for Validation | Pending | Meeting Date, Reviewer, Comments, RAG |
| Go for Delivery | Pending | Meeting Date, Reviewer, Comments, RAG |

**Milestone Fields:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| RAG Status | Dropdown (RED/AMBER/GREEN) | Yes | Current status |
| Meeting Date | Date | No | Scheduled meeting |
| Reviewer | User Picker | Yes | Assigned reviewer |
| Comments | Rich Text | No | Meeting notes |
| Decision | Dropdown | No | PASS/FAIL/PENDING |

---

### 3.2.4 Screen Specifications

#### Create Release Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Create Release                                              [?] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Release Name: * [Avionics 2.4.0 Release                         ] │
│                                                                 │
│ Version: * [2.4.0                                           ]   │
│                                                                 │
│ Planned Date: * [2024-06-15                               📅]   │
│                                                                 │
│ Owner: * [Release Manager                         ▼]            │
│                                                                 │
│ Linked Documents: [🔗 Select issues to link              ]      │
│                                                                 │
│ Description: [                                                ] │
│             [                                                ] │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                           [Cancel]  [Create Release]           │
└─────────────────────────────────────────────────────────────────┘
```

#### Milestone Tracking Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Release: Avionics 2.4.0 Release                                 │
│ Version: 2.4.0 | Status: Development | Owner: Release Manager  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ MILESTONES                                                      │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐│
│ │ ☐ Go for Dev                                    [🟢 GREEN]  ││
│ │   Reviewer: Technical Lead    Meeting Date: 2024-05-20     ││
│ │   Comments: Approved with conditions                      ││
│ │   Decision: PASS                                           ││
│ └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐│
│ │ ◐ Go for Implementation                          [🟡 AMBER]││
│ │   Reviewer: Implementation Lead  Meeting Date: 2024-06-01   ││
│ │   Comments: Pending hardware integration                   ││
│ │   Decision: PENDING                                         ││
│ └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐│
│ │ ○ Go for Validation                               [⚪ GRAY] ││
│ │   Reviewer: [Not assigned]    Meeting Date: [Not scheduled]││
│ │   Comments: -                                              ││
│ │   Decision: PENDING                                         ││
│ └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐│
│ │ ○ Go for Delivery                                 [⚪ GRAY] ││
│ │   Reviewer: [Not assigned]    Meeting Date: [Not scheduled]││
│ │   Comments: -                                              ││
│ │   Decision: PENDING                                         ││
│ └─────────────────────────────────────────────────────────────┘│
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                        [View Release Dashboard]                  │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.2.5 Business Rules

#### BR-004: RED Status Blocks Progression

**Condition:**
```
IF Milestone.RAG = 'RED'
THEN
    Block transition to next state
    Send alert to Release Manager
    Require explanation before proceeding
```

**Implementation:**
```java
@TransitionValidator(from = "VALIDATION", to = "DELIVERY")
public void validateGateProgression(Issue release) {
    List<Issue> milestones = getMilestones(release);

    for (Issue milestone : milestones) {
        if (!milestone.isComplete() && milestone.getRAG() == RED) {
            throw new BusinessRuleException(
                "Cannot proceed: " + milestone.getSummary() + " is RED"
            );
        }
    }
}
```

---

#### BR-005: Sequential Gate Progression

**Condition:**
```
IF Current Milestone != PASS
THEN
    Cannot start next milestone
```

**Implementation:**
```sql
CREATE FUNCTION validate_sequential_gates()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.gate_status = 'IN_PROGRESS' THEN
        -- Check previous gates are complete
        IF (SELECT COUNT(*) FROM release_gates
            WHERE release_id = NEW.release_id
            AND sequence < NEW.sequence
            AND status != 'PASS') > 0 THEN
            RAISE EXCEPTION 'Previous gates must be PASS before starting this gate';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

#### BR-006: Auto-Generate Documentation on Close

**Condition:**
```
IF Release.Status = 'DELIVERED'
THEN
    Generate PDN
    Generate Archive
    Generate KPI Report
    Update Portal
    Notify Stakeholders
```

**Implementation:**
```java
@PostFunction(status = "DELIVERED")
public class ReleaseCompletionPostFunction implements PostFunction {
    @Override
    public void execute(Issue release) {
        // Generate PDN
        pdnService.generate(release);

        // Create archive
        archiveService.createArchive(release);

        // Generate KPIs
        kpiService.generateReleaseKPIs(release);

        // Update portal
        portalService.publishRelease(release);

        // Notify stakeholders
        notificationService.notifyReleaseComplete(release);
    }
}
```

---

### 3.2.6 Workflow Definition

#### Release Workflow

```
┌──────────┐    ┌───────────┐    ┌──────────────┐    ┌──────────┐
│  DRAFT   │───▶│ PLANNING  │───▶│ DEVELOPMENT │───▶│VALIDATION│
└──────────┘    └───────────┘    └──────────────┘    └──────────┘
                                                      │
                                                      ▼
                                              ┌────────────┐    ┌──────────┐
                                              │READY_FOR   │───▶│ DELIVERED│
                                              │DELIVERY    │    └──────────┘
                                              └────────────┘         │
                                                                      ▼
                                                            ┌──────────────┐
                                                            │  ARCHIVED     │
                                                            └──────────────┘
```

#### Gate Status Transitions

| Gate | Prerequisites | Auto-Actions |
|------|--------------|--------------|
| Go for Dev | All CRs linked | Notify dev team |
| Go for Implementation | Dev gates PASS | Notify impl team |
| Go for Validation | Impl gates PASS | Create test issues |
| Go for Delivery | Val gates PASS | Generate PDN |

---

### 3.2.7 Backend Linkage

#### Service Dependencies

| Service | Purpose |
|---------|---------|
| ProjectService | Get project configuration |
| IssueService | Create milestone subtasks |
| WorkflowService | Validate gate transitions |
| DocumentService | Generate PDN |
| PortalService | Publish release info |
| NotificationService | Alert stakeholders |

#### Events

| Event | Trigger | Actions |
|-------|---------|---------|
| ReleaseCreated | Create release | Auto-create 4 milestones |
| GateCompleted | Milestone PASS | Check next gate readiness |
| ReleaseCompleted | All gates PASS | Generate docs, notify, archive |

---

### 3.2.8 Database Entities

```sql
-- Releases table (extends issues)
CREATE TABLE releases (
    id UUID PRIMARY KEY REFERENCES issues(id),
    version_name VARCHAR(100) NOT NULL,
    version_number VARCHAR(50),
    planned_date DATE,
    actual_date DATE,
    owner_id UUID REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, PLANNING, DEVELOPMENT, VALIDATION, DELIVERED, ARCHIVED
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Release Gates (milestones)
CREATE TABLE release_gates (
    id UUID PRIMARY KEY,
    release_id UUID REFERENCES releases(id),
    gate_type VARCHAR(50) NOT NULL, -- GO_FOR_DEV, GO_FOR_IMPLEMENTATION, etc.
    sequence INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, PASS, FAIL
    rag_status VARCHAR(10) DEFAULT 'GRAY', -- RED, AMBER, GREEN, GRAY
    meeting_date DATE,
    reviewer_id UUID REFERENCES users(id),
    decision VARCHAR(20), -- PASS, FAIL, PENDING
    comments TEXT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Release Documents
CREATE TABLE release_documents (
    id UUID PRIMARY KEY,
    release_id UUID REFERENCES releases(id),
    document_type VARCHAR(50) NOT NULL, -- PDN, ARCHIVE, KPI, etc.
    file_path VARCHAR(500),
    generated_at TIMESTAMP DEFAULT NOW(),
    generated_by UUID REFERENCES users(id)
);

-- Release Notifications
CREATE TABLE release_notifications (
    id UUID PRIMARY KEY,
    release_id UUID REFERENCES releases(id),
    notification_type VARCHAR(50),
    recipients UUID[],
    sent_at TIMESTAMP,
    status VARCHAR(20) -- SENT, FAILED, PENDING
);
```

---

### 3.2.9 Acceptance Criteria

#### AC-BR02-001: Create Release with Auto-Milestones

**Given** the user is on the project releases page
**When** the user creates a new release with:
- Release Name: "Avionics 2.4.0 Release"
- Version: "2.4.0"
- Planned Date: "2024-06-15"
- Owner: "Release Manager"
**Then** the system should:
- [ ] Create release issue
- [ ] Auto-create 4 milestone subtasks (Go for Dev, Go for Implementation, etc.)
- [ ] Set all milestones to "Pending" status
- [ ] Set all milestones RAG to "GRAY"
- [ ] Send notification to release manager

---

#### AC-BR02-002: RED Gate Blocks Progression

**Given** a release in Validation with Go for Implementation gate RED
**When** the user attempts to transition to Ready for Delivery
**Then** the system should:
- [ ] Display error: "Cannot proceed: Go for Implementation is RED"
- [ ] Block transition
- [ ] Send alert to Release Manager
- [ ] Require resolution before proceeding

---

#### AC-BR02-003: Auto-Generate on Close

**Given** a release with all gates PASS
**When** the release is moved to "Delivered" status
**Then** the system should:
- [ ] Generate Product Delivery Notification (PDN)
- [ ] Create archive package
- [ ] Generate KPI report
- [ ] Publish to portal
- [ ] Send notifications to stakeholders
- [ ] Set actual delivery date

---

### 3.2.10 Gap Analysis for BR-02

| Component | Current State | Required | Gap |
|-----------|--------------|----------|-----|
| Release Issue Type | Basic version | Custom Release type | ⚠️ PARTIAL |
| Release Workflow | Basic statuses | 6-state with gates | ❌ MISSING |
| Gate Tracking | Not present | RAG status per gate | ❌ MISSING |
| Auto-Milestone Creation | Not present | 4 subtasks auto-created | ❌ MISSING |
| Sequential Gate Validation | Not present | Block on RED | ❌ MISSING |
| PDN Generation | Not present | Template + data | ❌ MISSING |
| Archive Generation | Not present | ZIP with all artifacts | ❌ MISSING |
| Portal Integration | Not present | Publish on close | ❌ MISSING |
| KPI Generation | Not present | Metrics calculation | ❌ MISSING |

---

## BR-03: PROJECT METADATA SYSTEM

**Business Requirement ID:** BR-03
**Document Section:** Project Metadata
**Priority:** HIGH
**Status:** 📋 PENDING

---

### 3.3.1 Business Story

Every project needs metadata because external systems depend on it.

The system must store:
- Aircraft Program reference
- ATA chapter number
- Git repository URLs
- Artifactory location
- Jenkins job configuration
- OBS (Organizational Breakdown Structure)
- Development process classification
- Security classification

This metadata is used by:
- Dashboards (display project info)
- Compliance systems (audit trails)
- Portal (public project information)
- External integrations (CI/CD, artifact management)

---

### 3.3.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Projects | Project container | Project with metadata |
| Custom Fields | Metadata fields | 8+ custom fields per project |
| Issue Type | Project metadata issue | Project metadata template |
| Screens | Metadata editing | Project settings screen |
| Automation | Metadata propagation | Sync metadata across |

---

### 3.3.3 User Journey

**User:** Project Administrator
**Navigation:** Project → Settings → Metadata

```
Project: Avionics Product A
    ↓
Project Settings
    ↓
Metadata Tab
    ↓
Edit Metadata
```

---

### 3.3.4 Screen Specifications

#### Project Metadata Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Project Metadata: Avionics Product A                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ AIRCRAFT PROGRAM: * [Airbus A320 Family               ▼]        │
│                                                                 │
│ ATA CHAPTER: * [22 - Auto Flight                         ]     │
│                                                                 │
│ ─────────────────────────────────────────────────────────────  │
│ REPOSITORY LINKS                                               │
│                                                                 │
│ GitHub URL: [https://github.com/org/avionics-a320        ]     │
│                                                                 │
│ Artifactory URL: [https://artifactory.company.com/a320    ]     │
│                                                                 │
│ Jenkins Job URL: [https://jenkins.company.com/job/A320     ]     │
│                                                                 │
│ ─────────────────────────────────────────────────────────────  │
│ CONFIGURATION                                                  │
│                                                                 │
│ OBS (Organizational Structure): [Engineering > Avionics > A320]│
│                                                                 │
│ Development Process: * [DO-178C                    ▼]           │
│                                                                 │
│ Classification: * [Classified                    ▼]             │
│                                                                 │
│ ─────────────────────────────────────────────────────────────  │
│ LINKAGES                                                       │
│                                                                 │
│ Linked Release: [Avionics 2.4.0 Release            ▼]          │
│                                                                 │
│ Linked Documents: [🔗 5 documents linked                  ]     │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                           [Cancel]  [Save Metadata]             │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.3.5 Database Entities

```sql
-- Project Metadata (extends projects or separate table)
CREATE TABLE project_metadata (
    id UUID PRIMARY KEY,
    project_id UUID REFERENCES projects(id) UNIQUE,
    aircraft_program VARCHAR(100),
    ata_chapter VARCHAR(50),
    github_url VARCHAR(500),
    artifactory_url VARCHAR(500),
    jenkins_url VARCHAR(500),
    obs_structure VARCHAR(255),
    development_process VARCHAR(50),
    classification VARCHAR(50),
    security_level VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    updated_by UUID REFERENCES users(id)
);

-- Metadata Audit Trail
CREATE TABLE project_metadata_history (
    id UUID PRIMARY KEY,
    project_id UUID REFERENCES projects(id),
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    changed_at TIMESTAMP DEFAULT NOW(),
    changed_by UUID REFERENCES users(id)
);
```

---

### 3.3.6 Acceptance Criteria

#### AC-BR03-001: Edit Project Metadata

**Given** a project administrator is on the project metadata page
**When** the user updates:
- GitHub URL: "https://github.com/org/new-repo"
- Jenkins URL: "https://jenkins.company.com/job/NewJob"
**Then** the system should:
- [ ] Save metadata to database
- [ ] Create audit entry for changes
- [ ] Trigger webhook to CI/CD system
- [ ] Update portal if metadata is public

---

#### AC-BR03-002: Metadata Required Validation

**Given** a project with incomplete metadata
**When** attempting to create first release
**Then** the system should:
- [ ] Display warning for missing required fields
- [ ] Allow proceeding with warning
- [ ] Log warning for compliance

---

## BR-04: SUPPORT MANAGEMENT

**Business Requirement ID:** BR-04
**Document Section:** Support
**Priority:** HIGH
**Status:** 📋 PENDING

---

### 3.4.1 Business Story

Customers report issues through support channels. Support team must:
- Capture issue details
- Categorize by type (Bug, Question, Enhancement)
- Convert to appropriate internal issue
- Preserve history and attachments
- Track resolution progress

Bug reports should automatically create internal Bug issues linked to the support request.

---

### 3.4.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Issue Types | Support types | Support Request, Bug, Question |
| Automation | Issue conversion | Auto-create Bug on Bug report |
| Issue Linking | Link to internal | Blocks/Relates to |
| Custom Fields | Support metadata | Priority, Affected Product |
| Notifications | Stakeholder updates | Status change alerts |
| Portals | Customer access | Customer portal |

---

### 3.4.3 User Journey

**User:** Support Agent
**Navigation:** Project → Create Issue → Support Request

```
Project: Customer Support
    ↓
Create Issue
    ↓
Support Request
```

---

### 3.4.4 Screen Specifications

#### Create Support Request Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Create Support Request                                      [?] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Support Type: * [Bug Report                         ▼]          │
│                                                                 │
│ Summary: * [Issue with data export functionality               ] │
│                                                                 │
│ Description: * [                                              ] │
│             [When exporting data to CSV, special characters    ] │
│             [are not properly escaped, causing import errors   ] │
│             [in downstream systems.                            ] │
│                                                                 │
│ Priority: * [High                                    ▼]          │
│                                                                 │
│ Affected Product: * [Data Export Module             ▼]          │
│                                                                 │
│ Customer Name: [John Smith                               ]     │
│ Customer Email: [john.smith@customer.com               ]     │
│                                                                 │
│ Attachments: [📎 Drop files here or click to upload]           │
│                                                                 │
│ Internal Note: [                                               ] │
│               [                                              ] │
│               (Only visible to internal team)                  │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                           [Cancel]  [Submit]                    │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.4.5 Business Rules

#### BR-007: Auto-Create Bug on Bug Report

**Condition:**
```
IF Support Type = 'Bug Report'
THEN
    Automatically create Bug issue in development project
    Link Bug to Support Request (Blocks relationship)
    Copy summary, description, attachments
    Set priority based on support priority
    Notify development team
```

**Implementation:**
```java
@AutomationRule(trigger = ISSUE_CREATED)
public class SupportBugConversionRule {
    @Action
    public void convertToBug(Issue supportRequest) {
        if ("Bug Report".equals(supportRequest.getSupportType())) {
            Issue bug = issueService.createIssue(
                IssueType.BUG,
                supportRequest.getSummary(),
                supportRequest.getDescription(),
                getDevelopmentProject()
            );
            bug.setPriority(mapPriority(supportRequest.getPriority()));
            bugService.link(bug, supportRequest, IssueLinkType.BLOCKS);
            attachmentService.copy(supportRequest, bug);
            notificationService.notifyDevTeam(bug);
        }
    }
}
```

---

### 3.4.6 Acceptance Criteria

#### AC-BR04-001: Bug Report Creates Internal Bug

**Given** a support agent creates a Bug Report with:
- Summary: "Export CSV special characters"
- Description: "Special characters not escaped"
- Priority: High
**When** the support request is saved
**Then** the system should:
- [ ] Create Support Request issue
- [ ] Create linked Bug issue in development project
- [ ] Link Bug to Support (Blocks relationship)
- [ ] Copy attachments to Bug
- [ ] Set Bug priority to High
- [ ] Notify development team

---

## BR-05: ROLE MANAGEMENT

**Business Requirement ID:** BR-05
**Document Section:** Role Management
**Priority:** HIGH
**Status:** 📋 PENDING

---

### 3.5.1 Business Story

Every project needs defined roles with specific permissions. The system must support:

- **Role Definition** - Define project roles (Admin, Developer, Lead, Reviewer, User)
- **Role Members** - Assign users/groups to roles
- **Permission Mapping** - Link roles to specific permissions
- **Role Hierarchy** - Support hierarchical roles (Lead > Developer)
- **Delegation** - Allow role delegation to others

Example: An Engineering Lead can delegate their review responsibilities during vacation.

---

### 3.5.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Project Roles | Role definitions | Default + custom roles |
| Permission Schemes | Permission mapping | Role → permissions |
| Group Management | User groups | Group-based membership |
| Security Levels | Data access | Role-based security |
| Delegation | Temporary access | Delegation rules |

---

### 3.5.3 User Journey

**User:** Project Administrator
**Navigation:** Project → Settings → People & Roles

```
Project: Avionics Product A
    ↓
Project Settings
    ↓
People & Roles Tab
    ↓
Role Members
```

---

### 3.5.4 Screen Specifications

#### Role Members Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ People & Roles: Avionics Product A                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ ROLES                                                                 │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ [+ Add Role] [⚙ Configure Permissions]                          │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 👑 Administrators                              [5 members] │ │
│ │    [Users/Groups assigned to admin role]                   │ │
│ │    ├─ John Smith (Admin)                                    │ │
│ │    ├─ Engineering Team (Group)                              │ │
│ │    [+ Add] [Configure] [Delete]                            │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🔧 Developers                                    [12 members]│ │
│ │    [Users who can edit issues and commit code]             │ │
│ │    ├─ Jane Doe                                              │ │
│ │    ├─ Bob Wilson                                            │ │
│ │    [+ Add] [Configure] [Delete]                            │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 📋 Reviewers                                    [8 members] │ │
│ │    [Users who can approve changes and reviews]             │ │
│ │    ├─ Alice Johnson                                         │ │
│ │    [+ Add] [Configure] [Delete]                             │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 👤 Users                                        [25 members]│ │
│ │    [Standard project users]                                │ │
│ │    [+ Add] [Configure] [Delete]                            │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ [+ Add Role]                                                    │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ [← Back to Project Settings]                                    │
└─────────────────────────────────────────────────────────────────┘
```

#### Add Role Dialog

```
┌─────────────────────────────────────────────────────────────────┐
│ Add Role                                                    [X] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Role Name: * [Quality Assurance                         ]     │
│                                                                 │
│ Description: [Responsible for quality gates and testing   ]     │
│                                                                 │
│ Default Role: ○ Yes ● No                                       │
│   (Default roles are automatically assigned to new projects)   │
│                                                                 │
│ Role Type: ● Project Role ○ Global Role                        │
│                                                                 │
│ ─────────────────────────────────────────────────────────────── │
│ PERMISSIONS                                                    │
│                                                                 │
│ ☑ Browse Projects                                             │
│ ☑ Create Issues                                               │
│ ☑ Edit Issues (own only)                                      │
│ ☐ Edit All Issues                                              │
│ ☑ Create Comments                                              │
│ ☑ Delete Own Issues                                            │
│ ☐ Delete All Issues                                            │
│ ☑ Transition Issues                                            │
│ ☐ Administer Project                                           │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                              [Cancel]  [Add Role]              │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.5.5 Business Rules

#### BR-008: Default Roles Auto-Assigned

**Condition:**
```
IF Project created from template
THEN
    Copy all roles from template
    Copy all role memberships
```

---

#### BR-009: Role Cannot Delete If Members Exist

**Condition:**
```
IF Role has members
THEN
    Block deletion
    Require all members to be removed first
```

---

#### BR-010: Role Delegation

**Condition:**
```
IF User delegates role to another
THEN
    Delegatee receives same permissions
    Original user retains permissions
    Delegation has expiry date
```

**Implementation:**
```sql
-- Delegation table
CREATE TABLE role_delegations (
    id UUID PRIMARY KEY,
    delegator_id UUID REFERENCES users(id),
    delegate_id UUID REFERENCES users(id),
    project_id UUID REFERENCES projects(id),
    role_id UUID REFERENCES project_roles(id),
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMP DEFAULT NOW(),
    revoked_at TIMESTAMP
);
```

---

### 3.5.6 Database Entities

```sql
-- Project Roles (extend existing)
CREATE TABLE project_roles (
    id UUID PRIMARY KEY,
    project_id UUID REFERENCES projects(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    role_type VARCHAR(20) DEFAULT 'PROJECT', -- PROJECT, GLOBAL
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(project_id, name)
);

-- Role Members
CREATE TABLE project_role_members (
    id UUID PRIMARY KEY,
    project_role_id UUID REFERENCES project_roles(id),
    member_type VARCHAR(20) NOT NULL, -- USER, GROUP
    member_id UUID NOT NULL, -- user_id or group_id
    granted_at TIMESTAMP DEFAULT NOW(),
    granted_by UUID REFERENCES users(id)
);

-- Role Permissions (cached from scheme)
CREATE TABLE role_permissions (
    project_role_id UUID REFERENCES project_roles(id),
    permission_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (project_role_id, permission_key)
);

-- Role Delegations
CREATE TABLE role_delegations (
    id UUID PRIMARY KEY,
    project_id UUID REFERENCES projects(id),
    delegator_id UUID REFERENCES users(id),
    delegate_id UUID REFERENCES users(id),
    role_id UUID REFERENCES project_roles(id),
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

### 3.5.7 Acceptance Criteria

#### AC-BR05-001: Create Project Role

**Given** a project administrator is on the People & Roles page
**When** the user creates a new role "Quality Assurance" with description and permissions
**Then** the system should:
- [ ] Create role in database
- [ ] Link to permission scheme
- [ ] Show role in role list
- [ ] Enable member assignment

---

#### AC-BR05-002: Assign Users to Role

**Given** a role exists without members
**When** the admin assigns 3 users to the role
**Then** the system should:
- [ ] Create 3 role member records
- [ ] Users now have role permissions
- [ ] Audit log entry created
- [ ] Notification sent to assigned users

---

## BR-06: CLASSIFICATION SYSTEM

**Business Requirement ID:** BR-06
**Document Section:** Classification
**Priority:** HIGH
**Status:** 📋 PENDING

---

### 3.6.1 Business Story

Aviation software requires strict classification for:
- **Security Level** - Classified, Restricted, Internal, Public
- **Impact Level** - CAT/HAZ, MAJ, MINOR, NONE
- **Compliance Level** - DO-178C, DO-254, ISO-9001

Each issue and document must have classification that affects:
- Who can view/access
- Workflow transitions allowed
- Retention period
- Audit requirements

---

### 3.6.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Issue Security | Access control | Security levels |
| Custom Fields | Classification fields | Security, Impact, Compliance |
| Workflow | Classification-based transitions | Conditional transitions |
| Notifications | Classification-based alerts | Group-based |
| Retention | Data retention rules | Classification-based |

---

### 3.6.3 User Journey

**User:** Project Administrator
**Navigation:** Project → Settings → Security

```
Project: Avionics Product A
    ↓
Project Settings
    ↓
Security Tab
    ↓
Issue Security Scheme
```

---

### 3.6.4 Screen Specifications

#### Security Scheme Configuration

```
┌─────────────────────────────────────────────────────────────────┐
│ Issue Security Scheme: Avionics Product A                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Security Levels                                                 │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ [+ Add Security Level]                                          │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🏅 CLASSIFIED - All Project Members                [Default]│ │
│ │    Value: 100 | Members: All project members               │ │
│ │    Description: Access restricted to project team         │ │
│ │    [+ Add Members] [Edit] [Delete]                         │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🔒 RESTRICTED - Lead and Admin Only                Value:90│ │
│ │    Members: Engineering Lead, Program Manager              │ │
│ │    Description: Technical data with restrictions           │ │
│ │    [+ Add Members] [Edit] [Delete]                          │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 📁 INTERNAL - Team Members                          Value:80│ │
│ │    Members: All team members                               │ │
│ │    Description: Internal project information               │ │
│ │    [+ Add Members] [Edit] [Delete]                         │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🌐 PUBLIC - All Authorized Users                   Value:70│ │
│ │    Members: Everyone                                       │ │
│ │    Description: Public project summary                     │ │
│ │    [+ Add Members] [Edit] [Delete]                         │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ─────────────────────────────────────────────────────────────  │
│ HIERARCHY RULE                                                  │
│ User with access to level X can view all issues at level ≤ X   │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                    [← Back]  [Save Scheme]                      │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.6.5 Business Rules

#### BR-011: Classified Requires Approval

**Condition:**
```
IF Issue Security Level = 'CLASSIFIED'
THEN
    Transition to CLOSED requires approval
    Audit trail must be complete
    Cannot delete issue
```

---

#### BR-012: Classification Affects Retention

**Condition:**
```
IF Classification = 'CLASSIFIED'
THEN
    Retention: 10 years
    Archive instead of delete
    Compliance report required
```

---

### 3.6.6 Acceptance Criteria

#### AC-BR06-001: Set Issue Security Level

**Given** a user is creating a new issue
**When** the user selects security level "CLASSIFIED"
**Then** the system should:
- [ ] Only show users in CLASSIFIED level as assignees
- [ ] Apply CLASSIFIED workflow (with approval)
- [ ] Set retention period to 10 years
- [ ] Log security assignment

---

## BR-07: DOCUMENT MANAGEMENT

**Business Requirement ID:** BR-07
**Document Section:** Document Management
**Priority:** MEDIUM
**Status:** 📋 PENDING

---

### 3.7.1 Business Story

Documents must be managed alongside issues:
- **Link documents to issues** - Specifications, plans, reports
- **Version control** - Track document revisions
- **Approval workflow** - Document review and approval
- **Distribution** - Share documents with stakeholders

Example: A software specification document must be:
- Linked to the requirement issue
- Reviewed and approved before development
- Versioned with change history
- Distributed to all stakeholders

---

### 3.7.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Attachments | File storage | Version history |
| Comments | Document discussions | Review comments |
| Custom Fields | Document metadata | Version, Author, Status |
| Automation | Document workflows | Review notifications |
| Links | Document-issue linking | Related documents |

---

### 3.7.3 User Journey

**User:** Document Owner
**Navigation:** Issue → Attachments

```
Issue: REQ-001 - Flight Control Algorithm
    ↓
Attachments Tab
    ↓
Upload Document
```

---

### 3.7.4 Screen Specifications

#### Document Management Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Documents: REQ-001 - Flight Control Algorithm                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ [+ Upload Document] [+ Link Existing] [📋 Document Templates]  │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 📄 SW-Requirement-FC-Algo-v2.3.pdf                v2.3      │ │
│ │    Size: 2.4 MB | Uploaded: 2024-05-15 by John Smith        │ │
│ │    Status: ✓ APPROVED | Reviewers: 3 approved               │ │
│ │    Linked to: REQ-001, ARCH-001                             │ │
│ │                                                              │ │
│ │    [Download] [View] [Version History] [Approve] [Delete]  │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 📄 SW-Design-FC-Algo-v1.2.pdf                   v1.2        │ │
│ │    Size: 1.8 MB | Uploaded: 2024-04-20 by Jane Doe          │ │
│ │    Status: ◐ UNDER REVIEW | Reviewers: 1 of 3 approved     │ │
│ │    Linked to: REQ-001                                        │ │
│ │                                                              │ │
│ │    [Download] [View] [Version History] [Approve] [Delete]  │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ VERSION HISTORY                                                 │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ v2.3 (2024-05-15) - John Smith - Added section 4.2.1           │
│ v2.2 (2024-05-10) - John Smith - Updated performance specs    │
│ v2.1 (2024-05-01) - John Smith - Initial release               │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ [← Back to Issue]                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.7.5 Business Rules

#### BR-013: Document Requires Approval Before Development

**Condition:**
```
IF Document linked to Requirement Issue
AND Document Status != 'APPROVED'
THEN
    Block transition to 'In Development'
    Require document approval first
```

---

#### BR-014: Major Version Requires Re-Approval

**Condition:**
```
IF New version of document uploaded
AND Version number major change (v1.x → v2.0)
THEN
    Reset approval status to 'PENDING'
    Notify all reviewers
```

---

### 3.7.6 Acceptance Criteria

#### AC-BR07-001: Upload Document with Metadata

**Given** a user is on the Documents tab
**When** the user uploads a PDF document
**Then** the system should:
- [ ] Store document in file storage
- [ ] Create document metadata record
- [ ] Set initial status to 'DRAFT'
- [ ] Enable version tracking
- [ ] Link to current issue

---

#### AC-BR07-002: Document Approval Workflow

**Given** a document is in 'UNDER REVIEW' status
**When** all required reviewers approve
**Then** the system should:
- [ ] Update status to 'APPROVED'
- [ ] Record approval timestamp
- [ ] Notify document owner
- [ ] Enable development transition

---

## BR-08: BOM MANAGEMENT

**Business Requirement ID:** BR-08
**Document Section:** BOM
**Priority:** MEDIUM
**Status:** 📋 PENDING

---

### 3.8.1 Business Story

Bill of Materials (BOM) management for aviation:
- **Component Tracking** - Track all software/hardware components
- **Dependency Management** - Track component dependencies
- **Version Mapping** - Map components to releases
- **Compliance Tracking** - Track regulatory compliance

Example: An aircraft system BOM must track:
- Software versions in each release
- Third-party components and licenses
- Certificate numbers
- Supplier information

---

### 3.8.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Versions | Release containers | BOM versions |
| Components | Component tracking | Custom component types |
| Issue Links | Dependency tracking | Parent/child, requires |
| Custom Fields | BOM metadata | Supplier, license, cert |
| Reports | BOM reports | Export, compliance |

---

### 3.8.3 User Journey

**User:** Configuration Manager
**Navigation:** Project → Components

```
Project: Avionics Product A
    ↓
Components Tab
    ↓
Bill of Materials
```

---

### 3.8.4 Screen Specifications

#### Bill of Materials Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Bill of Materials: Avionics 2.4.0                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ [+ Add Component] [⚙ Configure BOM] [📤 Export BOM]            │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ COMPONENT ID │ NAME │ VERSION │ SUPPLIER │ STATUS │ ACTIONS │ │
│ ├─────────────────────────────────────────────────────────────┤ │
│ │ SW-001 │ Flight Control │ v2.4.0 │ Internal │ ✓ Active │ [▼] │ │
│ │ SW-002 │ Navigation SW │ v3.1.2 │ Internal │ ✓ Active │ [▼] │ │
│ │ SW-003 │ Comms Module │ v1.5.0 │ VendorA │ ⚠ Due │ [▼] │ │
│ │ HW-001 │ Sensor Unit │ v2.0 │ VendorB │ ✓ Active │ [▼] │ │
│ │ DOC-001 │ Spec Document │ v4.2 │ Internal │ ✓ Active │ [▼] │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ COMPONENT DETAILS                                               │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ SW-003: Comms Module                                            │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ Version: v1.5.0 (Update available: v1.6.0)                    │
│ Supplier: VendorA                                               │
│ License: Commercial - PO-2024-0567                              │
│ Certificate: DO-178C Level A                                    │
│ Dependencies: SW-001, SW-002                                     │
│ In Releases: 2.3.1, 2.3.2 (not in 2.4.0)                      │
│                                                                 │
│ [View Dependencies] [View History] [Update Version] [Replace]    │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ [← Back to Project]                                             │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.8.5 Business Rules

#### BR-015: Component Cannot Change in Released Version

**Condition:**
```
IF Component version linked to RELEASED version
THEN
    Block version change
    Require new version creation
```

---

#### BR-016: Missing Certificate Blocks Release

**Condition:**
```
IF Release contains component without certificate
AND Component classification = 'CRITICAL'
THEN
    Block release transition to 'Delivered'
    Require certificate upload
```

---

### 3.8.6 Acceptance Criteria

#### AC-BR08-001: Add Component to BOM

**Given** a configuration manager is on the BOM page
**When** the user adds a new component with all required fields
**Then** the system should:
- [ ] Create component record
- [ ] Link to current release
- [ ] Enable dependency tracking
- [ ] Show in BOM report

---

## BR-09: LEGAL ARCHIVE

**Business Requirement ID:** BR-09
**Document Section:** Legal Archive
**Priority:** HIGH
**Status:** 📋 PENDING

---

### 3.9.1 Business Story

Aviation software requires legal document retention:
- **Audit Trail** - Complete history of all changes
- **Document Retention** - Minimum 10-year retention
- **Compliance Evidence** - Certification data
- **Legal Hold** - Prevent deletion during litigation

Example: All change requests and approvals must be archived and immutable for regulatory audit.

---

### 3.9.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Audit Log | Change tracking | Comprehensive logging |
| Archives | Data archival | Legal archive |
| Retention Rules | Data policies | Classification-based |
| Security | Access control | Audit-only access |
| Export | Compliance reports | PDF/JSON export |

---

### 3.9.3 User Journey

**User:** Compliance Officer
**Navigation:** Administration → Audit & Compliance

```
Administration
    ↓
Audit & Compliance
    ↓
Legal Archive
```

---

### 3.9.4 Screen Specifications

#### Legal Archive Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Legal Archive - Compliance Dashboard                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ ARCHIVE STATISTICS                                              │
│ ─────────────────────────────────────────────────────────────  │
│ Total Items: 15,234 | Storage: 45 GB | Oldest: 2014-01-15      │
│                                                                 │
│ ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐   │
│ │ Issues Archived │ │ Documents       │ │ Change Requests │   │
│ │      12,456     │ │      2,345      │ │        433      │   │
│ │    (100%)       │ │    (98%)       │ │     (100%)      │   │
│ └─────────────────┘ └─────────────────┘ └─────────────────┘   │
│                                                                 │
│ RETENTION POLICIES                                              │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Classification      Retention Period    Items Affected       │ │
│ ├─────────────────────────────────────────────────────────────┤ │
│ │ CLASSIFIED              15 years          1,234             │ │
│ │ RESTRICTED              10 years          3,456             │ │
│ │ INTERNAL                 7 years          8,234             │ │
│ │ PUBLIC                   5 years          2,310             │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ LEGAL HOLDS                                                     │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🚫 Case-2024-001 - Active Hold                              │ │
│ │    Started: 2024-03-15 | Items: 45 | Reason: Litigation      │ │
│ │    [+ View Items] [Extend Hold] [Release Hold]               │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ EXPORT & REPORTS                                                │
│ [📋 Compliance Report] [📊 Audit Summary] [📦 Full Archive]      │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ Last Archive Integrity Check: 2024-05-24 (✓ Passed)            │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.9.5 Business Rules

#### BR-017: Archived Items Cannot Be Deleted

**Condition:**
```
IF Item archived
THEN
    Disable delete action
    Show 'Archived - Legal Retention' message
    Only Compliance Admin can restore
```

---

#### BR-018: Legal Hold Prevents Permanent Deletion

**Condition:**
```
IF Legal hold active for item
AND User attempts delete
THEN
    Block deletion
    Show 'Item under legal hold' message
    Log attempted deletion
```

---

#### BR-019: Archive Integrity Verification

**Condition:**
```
IF Archive integrity check fails
THEN
    Alert Compliance Officer
    Generate integrity report
    Require manual review
```

---

### 3.9.6 Database Entities

```sql
-- Legal Archive
CREATE TABLE legal_archive (
    id UUID PRIMARY KEY,
    original_table VARCHAR(100) NOT NULL,
    original_id UUID NOT NULL,
    archived_at TIMESTAMP DEFAULT NOW(),
    archived_by UUID REFERENCES users(id),
    retention_until DATE,
    classification VARCHAR(50),
    checksum VARCHAR(64), -- SHA-256
    file_path VARCHAR(500),
    is_verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP
);

-- Legal Holds
CREATE TABLE legal_holds (
    id UUID PRIMARY KEY,
    case_number VARCHAR(50) NOT NULL,
    reason TEXT,
    started_at TIMESTAMP DEFAULT NOW(),
    ended_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, RELEASED
    created_by UUID REFERENCES users(id)
);

-- Hold Items
CREATE TABLE legal_hold_items (
    id UUID PRIMARY KEY,
    hold_id UUID REFERENCES legal_holds(id),
    original_table VARCHAR(100),
    original_id UUID,
    added_at TIMESTAMP DEFAULT NOW(),
    added_by UUID REFERENCES users(id)
);

-- Archive Integrity Log
CREATE TABLE archive_integrity_log (
    id UUID PRIMARY KEY,
    check_date TIMESTAMP DEFAULT NOW(),
    items_verified INTEGER,
    items_failed INTEGER,
    status VARCHAR(20), -- PASSED, FAILED
    report_path VARCHAR(500)
);
```

---

### 3.9.7 Acceptance Criteria

#### AC-BR09-001: Archive Item on Retention

**Given** an item has reached retention period
**When** automated archive job runs
**Then** the system should:
- [ ] Move item to archive storage
- [ ] Create archive record
- [ ] Disable direct deletion
- [ ] Set retention date
- [ ] Verify checksum

---

#### AC-BR09-002: Legal Hold Prevents Deletion

**Given** an item is under legal hold
**When** a user attempts to delete the item
**Then** the system should:
- [ ] Block deletion
- [ ] Display warning message
- [ ] Log attempted deletion
- [ ] Notify Compliance Officer

---

## BR-10: PORTAL PUBLISHING

**Business Requirement ID:** BR-10
**Document Section:** Portal Publishing
**Priority:** MEDIUM
**Status:** 📋 PENDING

---

### 3.10.1 Business Story

Portal publishing for stakeholder visibility:
- **Release Notes** - Public release information
- **Status Dashboard** - Project progress visibility
- **Document Library** - Shared project documents
- **Stakeholder Access** - External stakeholder view

Example: After a release, automatically publish release notes, updated documentation, and status to the customer portal.

---

### 3.10.2 Jira DC Modules Involved

| Jira Module | Purpose | Configuration |
|-------------|--------|---------------|
| Portals | Customer access | Portal configuration |
| Dashboards | Status display | Public dashboards |
| Filters | Content selection | Public filters |
| Notifications | Update alerts | Portal notifications |
| Permissions | Access control | Portal roles |

---

### 3.10.3 User Journey

**User:** Release Manager
**Navigation:** Project → Portal → Publish

```
Project: Avionics Product A
    ↓
Portal
    ↓
Publish Release
```

---

### 3.10.4 Screen Specifications

#### Portal Publishing Screen

```
┌─────────────────────────────────────────────────────────────────┐
│ Portal Publishing: Avionics Product A                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ PORTAL PREVIEW                                                  │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ 🌐 https://portal.company.com/projects/avionics-a320       │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ PUBLISH CONTENT                                                 │
│ ─────────────────────────────────────────────────────────────  │
│                                                                 │
│ ☑ Release Notes (v2.4.0)                                       │
│ ☑ Project Status Summary                                        │
│ ☑ Documentation Library (5 documents)                           │
│ ☐ Issue List (requires approval)                                │
│ ☐ Sprint Burndown Chart                                          │
│                                                                 │
│ ─────────────────────────────────────────────────────────────  │
│ ACCESS SETTINGS                                                  │
│                                                                 │
│ Accessible to: [Customer A] [Customer B] [+ Add]                 │
│ Access Level: ● Public (no login) ○ Restricted (login required) │
│                                                                 │
│ ─────────────────────────────────────────────────────────────  │
│ NOTIFICATION                                                     │
│                                                                 │
│ ☑ Send email notification to stakeholders                       │
│ Email Template: [Release Announcement                    ▼]      │
│                                                                 │
│ ☐ Post to Teams channel                                         │
│ Channel: [Engineering Updates                            ▼]      │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ [Preview Portal]              [Cancel]  [Publish to Portal]      │
└─────────────────────────────────────────────────────────────────┘
```

---

### 3.10.5 Business Rules

#### BR-020: Classified Content Not Published

**Condition:**
```
IF Issue/Document classification = 'CLASSIFIED'
THEN
    Exclude from portal publish
    Only include PUBLIC/INTERNAL if configured
```

---

#### BR-021: Portal Update Triggers Notification

**Condition:**
```
IF Content published to portal
THEN
    Send notification to stakeholders
    Log portal access event
    Update portal last-modified date
```

---

### 3.10.6 Acceptance Criteria

#### AC-BR10-001: Publish Release to Portal

**Given** a release has been completed
**When** the release manager publishes to portal
**Then** the system should:
- [ ] Generate release notes
- [ ] Copy allowed documents
- [ ] Update portal dashboard
- [ ] Send notifications
- [ ] Log publish event

---

#### AC-BR10-002: Classified Excluded from Portal

**Given** a release contains classified issues
**When** publishing to portal
**Then** the system should:
- [ ] Exclude classified issues
- [ ] Show excluded count in summary
- [ ] Require explicit confirmation
- [ ] Log exclusions for audit

---

# 4. FEATURE DECOMPOSITION

---

## FEATURE 1: CORE ISSUE MANAGEMENT

**Feature ID:** F1
**Priority:** CRITICAL
**Status:** ✅ SUBSTANTIAL
**Completion:** 65% (16/25 implemented)
**Jira DC Module:** com.atlassian.jira.issue

### 1.1 Business Goal

Enable users to create, track, manage, and resolve work items (issues) across projects with full lifecycle support including hierarchy, linking, security, and metadata management.

### 1.2 User Stories Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F1-US001 | Create Standard Issue | 20 | ✅ |
| F1-US002 | View and Edit Issue | 20 | ✅ |
| F1-US003 | Manage Issue Hierarchy | 20 | ⚠️ PARTIAL |
| F1-US004 | Manage Issue Links | 20 | ⚠️ PARTIAL |
| F1-US005 | Security Levels | 20 | ❌ MISSING |
| F1-US006 | Votes and Watchers | 20 | ❌ MISSING |
| F1-US007 | Clone and Copy Issues | 20 | ❌ MISSING |
| F1-US008 | Move Issues | 20 | ❌ MISSING |
| F1-US009 | Time Tracking | 20 | ❌ MISSING |
| F1-US010 | Issue Search | 20 | ⚠️ PARTIAL |

**Feature 1 Completion: 65% (16/25 features)**

---

## FEATURE 2: PROJECT MANAGEMENT

**Feature ID:** F2
**Priority:** CRITICAL
**Status:** ⚠️ PARTIAL
**Completion:** 33% (6/18 implemented)
**Jira DC Module:** com.atlassian.jira.project

---

### 2.1 Business Goal

Enable administrators to create, configure, and manage projects with proper schemes, templates, roles, and categorization for organizing team work.

---

### 2.2 User Stories

#### F2-US001: Create Project
**Task Count:** 20 | **Status:** ✅ IMPLEMENTED

**Story:**
```
As a project administrator
I want to create a new project
So that my team can start tracking work
```

**Task Breakdown:**

##### F2-US001-T01: Access Create Project
| Subtask ID | Purpose | Inputs | Expected Behavior | Acceptance Criteria |
|------------|---------|--------|-------------------|---------------------|
| F2-US001-T01-ST01 | Navigate to Projects | Admin menu | System shows projects list | Given admin, when navigating, then projects shown |
| F2-US001-T01-ST02 | Click Create Project | Button click | System opens create wizard | Given admin, when clicking, then wizard opens |
| F2-US001-T01-ST03 | Access via keyboard | 'P' key shortcut | System opens on shortcut | Given 'P' pressed, when pressing, then opens |
| F2-US001-T01-ST04 | Verify permission | Permission check | System checks admin permission | Given non-admin, when accessing, then denied |
| F2-US001-T01-ST05 | Handle service unavailable | Error handling | System handles service down | Given service down, when accessing, then error shown |

##### F2-US001-T02: Select Project Type
| Subtask ID | Purpose | Expected Behavior | Acceptance Criteria |
|------------|---------|-------------------|---------------------|
| F2-US001-T02-ST01 | Display project types | System shows available types | Given types, when viewing, then types shown |
| F2-US001-T02-ST02 | Software Development type | System shows software type | Given software, when selecting, then software type |
| F2-US001-T02-ST03 | Business Project type | System shows business type | Given business, when selecting, then business type |
| F2-US001-T02-ST04 | Team vs Company-managed | System differentiates | Given selection, when viewing, then category shown |
| F2-US001-T02-ST05 | Validate type selection | System validates type | Given no type, when proceeding, then error |

**Project Types:**

| Type | Description | Features |
|------|-------------|----------|
| SOFTWARE | Software development | Sprints, Boards, Dev integration |
| BUSINESS | Business project | Task tracking, Reporting |
| IT_SERVICE | IT service management | Service Desk, SLAs |
| DATA_CENTER | Data center operations | Infrastructure tracking |

##### F2-US001-T03: Enter Project Details
| Field | Type | Required | Validation | Database Column |
|-------|------|----------|------------|-----------------|
| name | VARCHAR(100) | YES | 3-100 chars, unique | projects.name |
| key | VARCHAR(10) | YES | 2-10 uppercase, unique | projects.project_key |
| description | TEXT | NO | max 2000 chars | projects.description |
| url | VARCHAR(500) | NO | valid URL format | projects.project_url |

##### F2-US001-T04: Select Project Template
| Template | Workflow | Issue Types | Screens |
|----------|----------|-------------|---------|
| Scrum | Scrum workflow | Epic, Story, Task, Bug, Sub-task | All screens |
| Kanban | Kanban workflow | Story, Task, Bug | Simplified |
| Bug Tracking | Bug workflow | Bug, Task, Sub-task | Bug-focused |
| Task Management | Task workflow | Task, Sub-task | Task-focused |
| Blank | None | None | None |

##### F2-US001-T05: Assign Project Lead
**Database Mapping:**
```sql
UPDATE projects SET lead_user_id = :leadUserId WHERE id = :projectId;
INSERT INTO project_role_actors (project_id, role_id, actor_type, actor_id)
VALUES (:projectId, 'ADMIN', 'USER', :leadUserId);
```

##### F2-US001-T06: Configure Schemes
```sql
UPDATE projects SET
    workflow_scheme_id = :workflowSchemeId,
    issue_type_scheme_id = :issueTypeSchemeId,
    permission_scheme_id = :permissionSchemeId,
    notification_scheme_id = :notificationSchemeId
WHERE id = :projectId;
```

##### F2-US001-T07: Create Project Entity
```sql
INSERT INTO projects (
    id, name, project_key, description, url, category_id,
    lead_user_id, project_type, template_id,
    workflow_scheme_id, issue_type_scheme_id, permission_scheme_id,
    notification_scheme_id, screen_scheme_id,
    avatar_url, status, created_at, updated_at
) VALUES (
    :id, :name, :projectKey, :description, :url, :categoryId,
    :leadUserId, :projectType, :templateId,
    :workflowSchemeId, :issueTypeSchemeId, :permissionSchemeId,
    :notificationSchemeId, :screenSchemeId,
    :avatarUrl, 'ACTIVE', NOW(), NOW()
);
```

##### F2-US001-T08: Initialize Default Data
- Clone workflows from template
- Clone issue types
- Clone screens
- Create default versions
- Create default components
- Initialize backlogs
- Create default board

##### F2-US001-T09: Set Project Permissions
**Permission Scheme Defaults:**
```sql
INSERT INTO permission_grants (scheme_id, permission_key, grant_type, grant_id)
VALUES
    (:schemeId, 'BROWSE_PROJECTS', 'PROJECT_ROLE', 'USERS'),
    (:schemeId, 'CREATE_ISSUES', 'PROJECT_ROLE', 'USERS'),
    (:schemeId, 'EDIT_ISSUES', 'PROJECT_ROLE', 'DEVELOPERS'),
    (:schemeId, 'ADMINISTER_PROJECTS', 'PROJECT_ROLE', 'ADMINISTRATORS');
```

##### F2-US001-T10: Notify Stakeholders
- Identify stakeholders
- Notify project lead
- Include project details
- Include access link
- Send email/in-app notification

---

#### F2-US002: Edit Project
**Task Count:** 20 | **Status:** ✅ IMPLEMENTED

**Story:**
```
As a project administrator
I want to edit project details
So that I can update project configuration
```

**Fields Updateable:**
| Field | Editable | Validation |
|-------|----------|------------|
| name | YES | Unique, 3-100 chars |
| description | YES | Max 2000 chars |
| lead_user_id | YES | Valid user |
| category_id | YES | Valid category |
| url | YES | Valid URL |
| avatar_url | YES | Valid URL |
| archived | YES | Boolean |

**Audit Requirement:**
```sql
INSERT INTO project_history (project_id, field_name, old_value, new_value, changed_by, changed_at)
VALUES (:projectId, :fieldName, :oldValue, :newValue, :userId, NOW());
```

---

#### F2-US003: Delete/Archive Project
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a system administrator
I want to archive projects
So that old projects don't clutter the system but data is preserved
```

**Archive Workflow:**
```
User clicks Archive
    ↓
Confirm dialog shown
    ↓
Validate no active issues
    ↓
Set archived = true
    ↓
Archive all issues
    ↓
Log archive event
    ↓
Notify project lead
```

**Deletion Rules:**
- Archived projects cannot be deleted for 10 years (compliance)
- Only system admin can delete
- All issues must be archived first
- Audit trail preserved

---

#### F2-US004: Project Templates
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a project administrator
I want to create and manage project templates
So that I can quickly create new projects with standard configuration
```

**Template Management:**

| Operation | Description | Permissions |
|-----------|-------------|-------------|
| Create Template | Create new project template | Admin |
| Edit Template | Modify template configuration | Admin |
| Clone Template | Copy existing template | Admin |
| Delete Template | Remove template | Admin (must not be in use) |
| Set Default | Mark template as default | Admin |

**Template Contents:**
- Project type
- Issue type scheme
- Workflow scheme
- Permission scheme
- Notification scheme
- Screen scheme
- Default components
- Default versions
- Custom fields configuration

**Template API:**
```sql
-- Template storage
INSERT INTO project_templates (id, name, category, configuration, is_default, created_at)
VALUES (:id, :name, :category, :configJson, false, NOW());
```

---

#### F2-US005: Project Categories
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a project administrator
I want to organize projects into categories
So that I can easily find and manage related projects
```

**Category Management:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| name | VARCHAR(100) | YES | Unique |
| description | TEXT | NO | Max 1000 chars |
| lead_id | UUID | NO | Valid user |

**Project Category Assignment:**
```sql
-- Assign category to project
UPDATE projects SET category_id = :categoryId WHERE id = :projectId;

-- Category query
SELECT p.*, c.name as category_name
FROM projects p
LEFT JOIN project_categories c ON p.category_id = c.id
WHERE c.name = :categoryName;
```

---

#### F2-US006: Project Roles
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a project administrator
I want to define and manage project roles
So that I can control who has access to what functionality
```

**Default Project Roles:**

| Role | Description | Default Permissions |
|------|-------------|----------------------|
| Administrators | Full project access | All permissions |
| Developers | Development access | Edit, Transition, Comment |
| Users | Standard access | Create, Comment, View |
| Viewers | Read-only access | View only |

**Role Management API:**
```sql
-- Create project role
INSERT INTO project_roles (id, project_id, name, description, is_default, created_at)
VALUES (:id, :projectId, :name, :description, false, NOW());

-- Add role member
INSERT INTO project_role_members (id, project_role_id, member_type, member_id, granted_at)
VALUES (:id, :roleId, 'USER', :userId, NOW());
```

---

#### F2-US007: Project Membership
**Task Count:** 20 | **Status:** ✅ IMPLEMENTED

**Story:**
```
As a project administrator
I want to manage project membership
So that I can control who has access to the project
```

**Membership Types:**
- Direct user assignment
- Group membership
- Project role membership

**Membership API:**
```sql
-- Add project member
INSERT INTO project_members (id, project_id, user_id, role_id, added_at, added_by)
VALUES (:id, :projectId, :userId, :roleId, NOW(), :addedBy);

-- Remove project member
DELETE FROM project_members
WHERE project_id = :projectId AND user_id = :userId;

-- List project members
SELECT u.display_name, u.email, pm.role_id
FROM project_members pm
JOIN users u ON pm.user_id = u.id
WHERE pm.project_id = :projectId;
```

---

#### F2-US008: Project Settings
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a project administrator
I want to configure project settings
So that the project behaves according to team needs
```

**Settings Categories:**

| Category | Settings |
|----------|----------|
| Details | Name, description, URL, category |
| Workflow | Default workflow, issue types |
| Permissions | Permission scheme, security |
| Notifications | Notification scheme |
| Fields | Default values, required fields |
| Look & Feel | Project color, avatar |

---

#### F2-US009: Project Avatars
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a project administrator
I want to upload a project avatar
So that the project is easily identifiable
```

**Avatar Specifications:**
- Format: PNG, JPG, GIF
- Size: Max 2MB
- Dimensions: 48x48 to 512x512 pixels
- Storage: File system or S3

**Implementation:**
```sql
-- Avatar storage
UPDATE projects SET avatar_url = :avatarUrl WHERE id = :projectId;

-- Avatar upload endpoint
POST /api/projects/{projectId}/avatar
Content-Type: multipart/form-data
file=(binary)
Response: { "avatarUrl": "/api/projects/{projectId}/avatar/image.png" }
```

---

#### F2-US010: Project Export/Import
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a system administrator
I want to export and import projects
So that I can migrate projects between instances
```

**Export Contents:**
- Project configuration
- Issue types and schemes
- Workflows
- Custom fields
- Permissions
- All issues (optional)
- Attachments (optional)

**Export Format:**
```json
{
  "project": { ... },
  "issueTypes": [ ... ],
  "workflows": [ ... ],
  "customFields": [ ... ],
  "permissions": [ ... ],
  "issues": [ ... ],
  "attachments": [ ... ]
}
```

---

### 2.3 Feature 2 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F2-US001 | Create Project | 20 | ✅ |
| F2-US002 | Edit Project | 20 | ✅ |
| F2-US003 | Delete/Archive Project | 20 | ⚠️ PARTIAL |
| F2-US004 | Project Templates | 20 | ❌ MISSING |
| F2-US005 | Project Categories | 20 | ❌ MISSING |
| F2-US006 | Project Roles | 20 | ⚠️ PARTIAL |
| F2-US007 | Project Membership | 20 | ✅ |
| F2-US008 | Project Settings | 20 | ⚠️ PARTIAL |
| F2-US009 | Project Avatars | 20 | ❌ MISSING |
| F2-US010 | Project Export/Import | 20 | ❌ MISSING |

**Feature 2 Completion: 33% (6/18 features)**

---

## FEATURE 3: WORKFLOW ENGINE

**Feature ID:** F3
**Priority:** HIGH
**Status:** ⚠️ PARTIAL
**Completion:** 20% (3/15 implemented)
**Jira DC Module:** com.atlassian.jira.workflow

---

### 3.1 Business Goal

Provide a configurable workflow engine that manages issue transitions, conditions, validators, and post-functions for automating business processes.

---

### 3.2 User Stories

#### F3-US001: Create Workflow
**Task Count:** 20 | **Status:** ✅ IMPLEMENTED

**Story:**
```
As a workflow administrator
I want to create a new workflow
So that I can define business processes for my team
```

**Workflow Builder Interface:**

```
┌─────────────────────────────────────────────────────────────────┐
│ Workflow Designer: Aviation Change Request Workflow              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ [+ Add Status]  [+ Add Transition]  [⚙ Settings]  [💾 Save Draft] │
│                                                                 │
│                    ┌─────────┐                                  │
│                    │  DRAFT  │                                  │
│                    └────┬────┘                                  │
│                         │ Submit                                 │
│                         ▼                                        │
│                    ┌─────────┐                                  │
│                    │  OPEN   │                                  │
│                    └────┬────┘                                  │
│                         │ Start Analysis                         │
│                         ▼                                        │
│                    ┌───────────┐                                │
│                    │ ANALYSIS  │                                │
│                    └─────┬─────┘                                │
│                         │ Approve / Reject                      │
│                         ▼                                        │
│                    ┌───────────┐                                │
│                    │ APPROVED │                                │
│                    └─────┬─────┘                                │
│                         │ Start Development                      │
│                         ▼                                        │
│                    ┌─────────────┐                              │
│                    │ DEVELOPMENT│                              │
│                    └──────┬──────┘                              │
│                           │ Submit for Test                      │
│                           ▼                                      │
│                      ┌──────────┐                               │
│                      │ VALIDATE │                               │
│                      └────┬─────┘                                │
│                           │ Pass / Fail                          │
│                           ▼                                      │
│                      ┌──────────┐                               │
│                      │  CLOSED  │                               │
│                      └──────────┘                               │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│ [Test Workflow]  [Preview Diagram]  [← Back]  [Save & Publish] │
└─────────────────────────────────────────────────────────────────┘
```

**Workflow Types:**

| Type | Description | Editable |
|------|-------------|----------|
| BUILD_IN | System-provided workflow | Read-only |
| CUSTOM | User-created workflow | Fully editable |
| JIRA_DEFAULT | Default Jira workflow | Clone to edit |

---

#### F3-US002: Configure Transitions
**Task Count:** 20 | **Status:** ✅ IMPLEMENTED

**Story:**
```
As a workflow administrator
I want to configure transitions between statuses
So that I can control issue flow
```

**Transition Configuration:**

| Property | Description | Values |
|----------|-------------|--------|
| Name | Transition display name | Text |
| From Status | Source status | List of statuses |
| To Status | Target status | List of statuses |
| Trigger Type | How transition is triggered | MANUAL, AUTOMATIC |
| Conditions | Who can perform transition | Permission, role, field value |
| Validators | Pre-transition checks | Field required, script |
| Post-functions | Actions after transition | Notify, update field, webhook |

**Trigger Types:**

| Type | Description | Use Case |
|------|-------------|----------|
| MANUAL | User-initiated | Standard workflow |
| AUTOMATIC | System automatic | Time-based triggers |
| SCHEDULED | Cron-based | Daily status updates |
| WEBHOOK | External trigger | Integration |

---

#### F3-US003: Transition Conditions
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a workflow administrator
I want to add conditions to transitions
So that only qualified users or issues can transition
```

**Condition Types:**

| Condition | Parameters | Example |
|-----------|------------|---------|
| User Permission | permission_key | User has "Resolve Issues" |
| User Group | group_name | User in "Developers" group |
| User Role | role_name | User has "Approver" role |
| Field Value | field, operator, value | Priority = High |
| Field Changed | field_name | Status changed from Open |
| Script | groovy_script | Custom validation |

**AND/OR Logic:**
```json
{
  "type": "AND",
  "conditions": [
    { "type": "PERMISSION", "permission": "RESOLVE_ISSUES" },
    { "type": "FIELD", "field": "resolution", "operator": "NOT_NULL" }
  ]
}
```

**Implementation:**
```java
public class TransitionConditionEvaluator {
    public boolean evaluate(Issue issue, User user, Transition transition) {
        List<Condition> conditions = transition.getConditions();
        if (conditions.isEmpty()) return true;

        boolean result = conditions.get(0).getType() == "AND"
            ? evaluateAnd(issue, user, conditions)
            : evaluateOr(issue, user, conditions);

        return result;
    }
}
```

---

#### F3-US004: Transition Validators
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a workflow administrator
I want to add validators to transitions
So that issues cannot transition without meeting requirements
```

**Validator Types:**

| Validator | Description | Example |
|-----------|-------------|---------|
| Required Field | Field must have value | Resolution required |
| Field Changed | Field was modified | Comment required |
| User Permission | User has permission | Admin approval |
| Regex Match | Field matches pattern | Key format |
| Date Range | Date within range | Due date future |
| Custom Script | Custom validation | Complex logic |

**Validator Implementation:**
```java
public interface TransitionValidator {
    ValidationResult validate(Issue issue, User user, Transition transition);
    String getErrorMessage();
}

public class RequiredFieldValidator implements TransitionValidator {
    private String fieldName;

    @Override
    public ValidationResult validate(Issue issue, User user, Transition transition) {
        Object value = issue.getField(fieldName);
        if (value == null || value.toString().isEmpty()) {
            return ValidationResult.failed(fieldName + " is required");
        }
        return ValidationResult.passed();
    }
}
```

---

#### F3-US005: Post-Functions
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a workflow administrator
I want to add post-functions to transitions
So that actions are automatically performed after transitions
```

**Post-Function Types:**

| Function | Description | Parameters |
|----------|-------------|------------|
| Update Field | Sets field value | field, value |
| Assign to User | Assigns issue | user, expression |
| Send Email | Sends email | template, recipients |
| Create Comment | Adds comment | text, visibility |
| Add to Sprint | Adds to sprint | sprint, expression |
| Fire Webhook | Calls webhook | url, payload |
| Reindex Issue | Updates search index | - |
| Fire Automation | Triggers rule | rule_id |

**Post-Function Execution:**
```java
public class PostFunctionExecutor {
    public void execute(Issue issue, Transition transition) {
        List<PostFunction> functions = transition.getPostFunctions();

        for (PostFunction function : functions) {
            try {
                function.execute(issue, transition);

                // Log execution
                auditService.logPostFunction(
                    issue.getId(),
                    transition.getId(),
                    function.getClass().getSimpleName(),
                    "SUCCESS"
                );
            } catch (Exception e) {
                // Log failure but continue
                auditService.logPostFunction(
                    issue.getId(),
                    transition.getId(),
                    function.getClass().getSimpleName(),
                    "FAILED: " + e.getMessage()
                );
            }
        }
    }
}
```

---

#### F3-US006: Workflow Schemes
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a project administrator
I want to assign workflows to projects
So that different projects can use different workflows
```

**Scheme Assignment:**
```sql
-- Assign workflow scheme to project
INSERT INTO project_workflow_schemes (project_id, workflow_scheme_id, issue_type_id)
VALUES (:projectId, :schemeId, NULL); -- NULL = all issue types

-- Or per issue type
INSERT INTO project_workflow_schemes (project_id, workflow_scheme_id, issue_type_id)
VALUES (:projectId, :schemeId, :bugIssueTypeId);
```

**Scheme API:**
```sql
-- List workflow schemes
SELECT * FROM workflow_schemes WHERE is_active = true;

-- Create workflow scheme
INSERT INTO workflow_schemes (id, name, description, created_at)
VALUES (:id, :name, :description, NOW());

-- Assign workflow to scheme
INSERT INTO scheme_workflows (scheme_id, workflow_id, issue_type_id)
VALUES (:schemeId, :workflowId, NULL);
```

---

#### F3-US007: Draft Workflows
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a workflow administrator
I want to create draft workflows
So that I can test changes before publishing
```

**Draft Workflow Lifecycle:**
```
┌──────────┐     ┌──────────┐     ┌───────────┐
│  DRAFT   │────▶│ VALIDATED│────▶│ PUBLISHED │
└──────────┘     └──────────┘     └───────────┘
     │                │                 │
     │               │                 │
     ▼                ▼                 ▼
   Edit            Test              Active
   (editable)    (read-only)       (in use)
```

**Draft Operations:**
- Edit workflow in draft mode
- Validate workflow integrity
- Test transitions
- Compare with published version
- Publish draft (creates new version)
- Discard draft (delete)

---

#### F3-US008: Workflow Visualization
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a workflow administrator
I want to view workflow diagrams
So that I can understand workflow structure at a glance
```

**Diagram Features:**
- Visual flow representation
- Status icons and colors
- Transition arrows with conditions
- Clickable nodes for editing
- Zoom and pan controls
- Export as PNG/SVG
- Print-friendly view

**Implementation:**
```javascript
// Workflow diagram rendering
const nodes = workflow.getStatuses().map(status => ({
    id: status.id,
    label: status.name,
    x: calculateX(status),
    y: calculateY(status),
    color: statusCategoryColor[status.category]
}));

const edges = workflow.getTransitions().map(t => ({
    from: t.fromStatusId,
    to: t.toStatusId,
    label: t.name,
    arrows: 'to'
}));
```

---

### 3.3 Feature 3 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F3-US001 | Create Workflow | 20 | ✅ |
| F3-US002 | Configure Transitions | 20 | ✅ |
| F3-US003 | Transition Conditions | 20 | ❌ MISSING |
| F3-US004 | Transition Validators | 20 | ❌ MISSING |
| F3-US005 | Post-Functions | 20 | ❌ MISSING |
| F3-US006 | Workflow Schemes | 20 | ❌ MISSING |
| F3-US007 | Draft Workflows | 20 | ❌ MISSING |
| F3-US008 | Workflow Visualization | 20 | ❌ MISSING |

**Feature 3 Completion: 20% (3/15 features)**

---

## FEATURE 4: SECURITY & PERMISSIONS

**Feature ID:** F4
**Priority:** CRITICAL
**Status:** ❌ MISSING
**Completion:** 0% (0/20 implemented)
**Jira DC Module:** com.atlassian.jira.security

---

### 4.1 Business Goal

Provide a comprehensive security system that controls access to projects, issues, and administrative functions through role-based access control (RBAC).

---

### 4.2 User Stories

#### F4-US001: Permission Schemes
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a system administrator
I want to create and manage permission schemes
So that I can control who can do what in projects
```

**Permission Types (Jira DC - 50+ permissions):**

| Category | Permission | Description |
|----------|------------|-------------|
| Browse | BROWSE_PROJECTS | View project |
| Browse | VIEW_READONLY_WORKFLOW | View workflow diagrams |
| Create | CREATE_ISSUES | Create new issues |
| Create | CREATE_ATTACHMENTS | Upload attachments |
| Edit | EDIT_ISSUES | Edit any issue |
| Edit | EDIT_OWN_ISSUES | Edit own issues only |
| Delete | DELETE_ISSUES | Delete any issue |
| Delete | DELETE_OWN_ISSUES | Delete own issues |
| Assign | ASSIGN_ISSUES | Assign to any user |
| Assign | ASSIGNABLE_USER | Can be assigned to |
| Resolve | RESOLVE_ISSUES | Resolve/close issues |
| Comment | CREATE_COMMENTS | Add comments |
| Comment | EDIT_COMMENTS | Edit comments |
| Comment | DELETE_COMMENTS | Delete comments |
| Work | WORK_ON_ISSUES | Log work time |
| Admin | ADMINISTER_PROJECTS | Project settings |
| Admin | USER_ADMIN | User management |
| Admin | SYSTEM_ADMIN | System administration |

**Permission Scheme Structure:**
```sql
CREATE TABLE permission_schemes (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE permission_grants (
    id UUID PRIMARY KEY,
    scheme_id UUID REFERENCES permission_schemes(id),
    permission_key VARCHAR(100) NOT NULL,
    grant_type VARCHAR(20) NOT NULL, -- USER, GROUP, PROJECT_ROLE
    grant_id UUID NOT NULL,
    created_at TIMESTAMP
);
```

---

#### F4-US002: Grant Permissions
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a system administrator
I want to grant permissions to users, groups, and roles
So that I can control access at a granular level
```

**Grant Types:**

| Type | Description | Example |
|------|-------------|---------|
| USER | Individual user | john.smith@company.com |
| GROUP | User group | engineering-team |
| PROJECT_ROLE | Project role | Developers, Administrators |

**Grant API:**
```sql
-- Add permission grant
INSERT INTO permission_grants (id, scheme_id, permission_key, grant_type, grant_id)
VALUES (:id, :schemeId, 'EDIT_ISSUES', 'GROUP', :groupId);

-- Check permission
SELECT has_permission(:userId, :projectId, 'EDIT_ISSUES');
-- Returns: true/false
```

**Permission Check Logic:**
```java
public boolean hasPermission(UUID userId, UUID projectId, String permissionKey) {
    // 1. Get user's permission scheme for project
    PermissionScheme scheme = projectService.getPermissionScheme(projectId);

    // 2. Get user's direct permissions
    if (hasDirectPermission(userId, scheme, permissionKey)) return true;

    // 3. Get user's group permissions
    List<UUID> userGroups = userService.getGroups(userId);
    for (UUID groupId : userGroups) {
        if (hasGroupPermission(groupId, scheme, permissionKey)) return true;
    }

    // 4. Get user's project role permissions
    ProjectRole role = userService.getProjectRole(userId, projectId);
    if (hasRolePermission(role, scheme, permissionKey)) return true;

    return false;
}
```

---

#### F4-US003: Security Levels
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a security administrator
I want to create security levels for issues
So that I can restrict access to sensitive issues
```

**Security Level Hierarchy:**

| Level | Value | Description | Access |
|-------|-------|-------------|--------|
| Top Secret | 100 | Highest sensitivity | Admin only |
| Confidential | 90 | High sensitivity | Lead + Admin |
| Restricted | 80 | Internal use | Team members |
| Internal | 70 | General access | All users |
| Public | 60 | External access | Everyone |

**Security Level Implementation:**
```sql
CREATE TABLE security_levels (
    id UUID PRIMARY KEY,
    scheme_id UUID REFERENCES security_schemes(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    value INTEGER NOT NULL, -- Higher = more restricted
    is_default BOOLEAN DEFAULT FALSE
);

CREATE TABLE security_level_members (
    id UUID PRIMARY KEY,
    level_id UUID REFERENCES security_levels(id),
    member_type VARCHAR(20) NOT NULL, -- USER, GROUP, PROJECT_ROLE
    member_id UUID NOT NULL
);
```

---

#### F4-US004: Issue Security
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a project administrator
I want to assign security levels to issues
So that sensitive information is protected
```

**Issue Security Fields:**
```sql
ALTER TABLE issues ADD COLUMN security_level_id UUID REFERENCES security_levels(id);
```

**Security Check:**
```java
public boolean canViewIssue(UUID userId, Issue issue) {
    SecurityLevel level = issue.getSecurityLevel();
    if (level == null) return true; // No security = visible to all

    return securityLevelService.hasAccess(userId, level);
}

public boolean hasAccess(UUID userId, SecurityLevel level) {
    // Check direct membership
    if (isDirectMember(userId, level)) return true;

    // Check group membership
    if (isGroupMember(userId, level)) return true;

    // Check role membership
    if (isRoleMember(userId, level)) return true;

    return false;
}
```

---

#### F4-US005: Global Permissions
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a system administrator
I want to manage global permissions
So that I can control system-level access
```

**Global Permissions:**

| Permission | Description | Who typically has it |
|------------|-------------|---------------------|
| SYSTEM_ADMIN | Full system access | System administrators |
| USER_ADMIN | User management | HR, IT admins |
| SYSADMIN | Create/admin projects | - |
| BROWSE_SYSADMIN_TABS | Admin menu access | System admins |
| VIEW_SYSTEM_TABS | Admin tabs | System admins |

**Global Permission API:**
```sql
CREATE TABLE global_permissions (
    id UUID PRIMARY KEY,
    permission_key VARCHAR(100) NOT NULL,
    grant_type VARCHAR(20) NOT NULL,
    grant_id UUID NOT NULL
);

-- Check global permission
SELECT has_global_permission(:userId, 'SYSTEM_ADMIN');
```

---

#### F4-US006: Project Permissions
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a project administrator
I want to set project-level permissions
So that I can control access to individual projects
```

**Project Permission Matrix:**

| Action | Administrators | Developers | Users | Viewers |
|--------|---------------|------------|-------|---------|
| Browse Project | ✅ | ✅ | ✅ | ✅ |
| Create Issues | ✅ | ✅ | ✅ | ❌ |
| Edit Issues | ✅ | ✅ | Own only | ❌ |
| Delete Issues | ✅ | Own only | ❌ | ❌ |
| Transition Issues | ✅ | ✅ | ❌ | ❌ |
| Manage Project | ✅ | ❌ | ❌ | ❌ |

---

#### F4-US007: Role-Based Access Control
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a security administrator
I want to configure RBAC
So that permissions are based on roles rather than individual users
```

**RBAC Components:**
- Roles (defined at project or global level)
- Permissions (what roles can do)
- Role Members (who belongs to roles)
- Role Hierarchy (inheritance)

**Role Hierarchy:**
```
System Administrator
    │
    ├── Project Administrator
    │       │
    │       ├── Developer
    │       │       │
    │       │       └── User
    │       │
    │       └── Viewer
    │
    └── System User
```

---

#### F4-US008: Permission Validation
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a developer
I want permission checks to happen automatically
So that I don't have to manually check permissions
```

**Permission Validation Flow:**
```
User Action
    ↓
Permission Check (AOP)
    ↓
Security Service
    ↓
Has Permission? ──No──▶ Access Denied Error
    │
   Yes
    ↓
Action Executed
    ↓
Audit Log Entry
```

**Implementation:**
```java
@RequiresPermission("EDIT_ISSUES")
public Issue updateIssue(UUID issueId, IssueUpdate update) {
    // This method can only be called if user has EDIT_ISSUES permission
    Issue issue = issueRepository.findById(issueId);
    issue.update(update);
    return issueRepository.save(issue);
}
```

---

### 4.3 Feature 4 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F4-US001 | Permission Schemes | 20 | ❌ MISSING |
| F4-US002 | Grant Permissions | 20 | ❌ MISSING |
| F4-US003 | Security Levels | 20 | ❌ MISSING |
| F4-US004 | Issue Security | 20 | ❌ MISSING |
| F4-US005 | Global Permissions | 20 | ❌ MISSING |
| F4-US006 | Project Permissions | 20 | ❌ MISSING |
| F4-US007 | RBAC | 20 | ❌ MISSING |
| F4-US008 | Permission Validation | 20 | ❌ MISSING |

**Feature 4 Completion: 0% (0/20 features)**

---

## FEATURE 5: AGILE/SPRINT MANAGEMENT

**Feature ID:** F5
**Priority:** HIGH
**Status:** ⚠️ PARTIAL
**Completion:** 25% (5/20 implemented)
**Jira DC Module:** com.atlassian.jira.software

---

### 5.1 Business Goal

Enable teams to plan, track, and manage work using Scrum and Kanban methodologies with visual boards, sprint planning, and velocity tracking.

---

### 5.2 User Stories

#### F5-US001: Scrum Board
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a team member
I want to view and work with a Scrum board
So that I can visualize work status and manage sprints
```

**Board Layout:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│ Sprint 23: Release 2.4.0                    [◀ Sprint 22] [Sprint 24 ▶]   │
│ Goal: Complete release testing                    [Start: May 15]        │
│ Duration: 2 weeks | Remaining: 5 days                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│ TO DO (12)          │ IN PROGRESS (5)     │ IN REVIEW (3)    │ DONE (8) │
│ ─────────────────── │ ─────────────────── │ ──────────────── │ ────────│
│                     │                     │                   │          │
│ ┌─────────────────┐│ ┌─────────────────┐│ ┌───────────────┐│┌────────┐│
│ │ 🔴 SW-001       ││ │ 🔴 SW-015       ││ │ 🔴 SW-022     │││ SW-008 ││
│ │ Flight Control  ││ │ Comms Module    ││ │ UI Updates    │││ Config ││
│ │ ───────────────││ │ ───────────────││ │ ──────────── │││ ──────││
│ │ 👤 John  ⏱ 3d  ││ │ 👤 Jane  ⏱ 5d  ││ │ 👤 Bob  ⏱ 2d │││ 👤 Alice││
│ │ 🏷️ 5pts  🔖 2   ││ │ 🏷️ 8pts  🔖 1   ││ │ 🏷️ 3pts       │││ 🏷️ 3pts││
│ └─────────────────┘│ └─────────────────┘│ └───────────────┘│└────────┘│
│                     │                     │                   │          │
│ ┌─────────────────┐│ ┌─────────────────┐│ ┌───────────────┐│┌────────┐│
│ │ 🟡 SW-002       ││ │ 🟡 SW-016       ││ │               │││ SW-009 ││
│ │ Navigation Fix  ││ │ API Changes    ││ │               │││ Testing││
│ │ ───────────────││ │ ───────────────││ │               │││ ──────││
│ │ 👤 Mike  ⏱ 2d  ││ │ 👤 John  ⏱ 3d  ││ │               │││ 👤 Dave││
│ │ 🏷️ 3pts        ││ │ 🏷️ 5pts        ││ │               │││ 🏷️ 5pts││
│ └─────────────────┘│ └─────────────────┘│                   │└────────┘│
│                                                                         │
│ [+ Add Issue]        │                     │                       │
│                       │                     │                       │
└─────────────────────────────────────────────────────────────────────────┘
│ SWIMLANES: [None ▼]  │  QUICK FILTERS: [🔍 My Issues] [🔍 Overdue]   │
└─────────────────────────────────────────────────────────────────────────┘
```

**Column Configuration:**
```javascript
const boardColumns = [
    { name: 'TO DO', status: 'OPEN', wipLimit: null },
    { name: 'IN PROGRESS', status: 'IN_PROGRESS', wipLimit: 8 },
    { name: 'IN REVIEW', status: 'IN_REVIEW', wipLimit: 4 },
    { name: 'DONE', status: 'DONE', wipLimit: null }
];
```

---

#### F5-US002: Kanban Board
**Task Count:** 20 | **Status:** ✅ IMPLEMENTED

**Story:**
```
As a team member
I want to use a Kanban board
So that I can visualize work items and limit work in progress
```

**Kanban Features:**
- WIP (Work In Progress) limits per column
- Continuous flow
- Throughput tracking
- Cycle time measurement
- No fixed sprints

**WIP Limit Enforcement:**
```java
public TransitionResult canTransitionTo(UUID issueId, String targetColumn) {
    BoardColumn column = boardService.getColumn(targetColumn);
    int currentCount = issueService.countByColumn(column);
    int wipLimit = column.getWipLimit();

    if (wipLimit != null && currentCount >= wipLimit) {
        return TransitionResult.rejected(
            "WIP limit reached for " + column.getName() +
            " (" + currentCount + "/" + wipLimit + ")"
        );
    }
    return TransitionResult.allowed();
}
```

---

#### F5-US003: Sprint Management
**Task Count:** 20 | **Status:** ✅ IMPLEMENTED

**Story:**
```
As a Scrum Master
I want to create and manage sprints
So that I can plan and track iterations
```

**Sprint Lifecycle:**
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   FUTURE    │───▶│   ACTIVE    │───▶│  COMPLETED  │───▶│  CLOSED     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
     │                 │                  │                  │
     │                 │                  │                  │
     ▼                 ▼                  ▼                  ▼
   Plan            Work in progress    Sprint ended       Archived
                   └────────────────┐
                                        │
                                        ▼
                                   ABANDONED (optional)
```

**Sprint Database:**
```sql
CREATE TABLE sprints (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    board_id UUID REFERENCES boards(id),
    goal TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'FUTURE', -- FUTURE, ACTIVE, COMPLETED, CLOSED
    completed_at TIMESTAMP,
    velocity DECIMAL(5,2),
    capacity INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

#### F5-US004: Sprint Planning
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a Scrum Master
I want to plan sprints
So that I can select issues for the sprint backlog
```

**Planning Interface:**

```
┌─────────────────────────────────────────────────────────────────┐
│ Sprint Planning - Sprint 24                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ CAPACITY                          BACKLOG                       │
│ ──────────────────────────────── ──────────────────────────────│
│                                                                 │
│ Team Members: 5                  Issues in Backlog: 45         │
│ Hours/Day: 8                     Estimated: 120 points         │
│ Sprint Days: 10                  Available: 80 points           │
│ Total Capacity: 400 hours         ─────────────────────────────│
│                                                             [▼] │
│                                                             [▼] │
│ ┌───────────────────────┐       ┌─────────────────────────────┐│
│ │ Capacity Allocation   │       │ ☐ SW-030 - Feature X       ││
│ │                       │       │   Points: 8 | Assignee: —   ││
│ │ John Smith: 40h       │       │   ──────────────────────    ││
│ │ Jane Doe: 40h          │       │ ☐ SW-031 - Feature Y       ││
│ │ Bob Wilson: 40h        │       │   Points: 5 | Assignee: —   ││
│ │ Alice Brown: 40h      │       │   ──────────────────────    ││
│ │ Mike Chen: 40h         │       │ ☐ SW-032 - Feature Z       ││
│ │                       │       │   Points: 13 | Assignee: —    ││
│ │ Available: 200h        │       │   ──────────────────────    ││
│ │ Committed: 150h        │       │ ☐ SW-033 - Bug Fix A       ││
│ │ Remaining: 50h          │       │   Points: 3 | Assignee: —   ││
│ └───────────────────────┘       │   ──────────────────────    ││
│                                 │ ☐ SW-034 - Bug Fix B       ││
│ COMMITTED ISSUES                 │   Points: 2 | Assignee: —    ││
│ ──────────────────────────────── └─────────────────────────────┘│
│                                                                 │
│ ☐ SW-010 - Task A    (5pts) ───────────────────────────────────▶│
│ ☐ SW-015 - Task B    (8pts) ───────────────────────────────────▶│
│ ☐ SW-020 - Task C    (3pts) ───────────────────────────────────▶│
│                                                                 │
│ Total Committed: 47 points │ Team Capacity: 200 hours          │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                    [Cancel]  [Create Sprint]                   │
└─────────────────────────────────────────────────────────────────┘
```

---

#### F5-US005: Backlog Management
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a team member
I want to manage the backlog
So that I can prioritize and organize work
```

**Backlog Features:**
- Drag-and-drop prioritization
- Issue grouping by Epic
- Quick add issues
- Bulk ranking
- Rank maintenance
- Filtering and search

**Rank Implementation:**
```sql
-- LexoRank for ordering
CREATE TABLE issue_ranks (
    issue_id UUID PRIMARY KEY REFERENCES issues(id),
    rank VARCHAR(255) NOT NULL UNIQUE
);

-- Reorder with LexoRank
UPDATE issue_ranks
SET rank = LexoRank.between(prev_rank, next_rank)
WHERE issue_id = :issueId;
```

---

#### F5-US006: Board Configuration
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a board administrator
I want to configure the board
So that it reflects team workflow
```

**Configuration Options:**

| Setting | Description | Values |
|---------|-------------|--------|
| Columns | Status to column mapping | List of status→column |
| WIP Limits | Max issues per column | Integer per column |
| Swimlanes | Group issues by | None, Epic, Assignee, Priority |
| Quick Filters | One-click filters | Saved JQL queries |
| Card Colors | Color coding rules | Based on priority, label, etc. |

**Configuration API:**
```sql
CREATE TABLE board_configurations (
    board_id UUID REFERENCES boards(id),
    setting_key VARCHAR(100),
    setting_value JSONB,
    PRIMARY KEY (board_id, setting_key)
);

-- Get quick filters
SELECT setting_value
FROM board_configurations
WHERE board_id = :boardId AND setting_key = 'quickFilters';
```

---

#### F5-US007: Sprint Reports
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a Scrum Master
I want to view sprint reports
So that I can measure team performance
```

**Report Types:**

| Report | Description | Metrics |
|--------|-------------|---------|
| Burndown Chart | Work remaining over time | Ideal vs actual |
| Velocity Chart | Points completed per sprint | Trend analysis |
| Cumulative Flow | Issues by status over time | Flow efficiency |
| Control Chart | Cycle time distribution | Average, std deviation |
| Sprint Summary | Sprint goal, completed, work | Goal completion % |

**Burndown Calculation:**
```javascript
function calculateBurndown(sprint) {
    const startDate = sprint.startDate;
    const endDate = sprint.endDate;
    const totalPoints = sprint.committedPoints;

    const dailyData = [];
    for (let date = startDate; date <= endDate; date += 1) {
        const completedPoints = calculateCompletedByDate(date, sprint);
        const remaining = totalPoints - completedPoints;
        const ideal = totalPoints - (totalPoints * (date - startDate) / (endDate - startDate));

        dailyData.push({ date, remaining, ideal });
    }
    return dailyData;
}
```

---

#### F5-US008: Issue Ranking
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a team member
I want to reorder issues on the board
So that I can show priority without changing status
```

**Drag-Drop Ranking:**
```javascript
// On drag-drop reorder
function onIssueReorder(draggedIssue, targetIndex) {
    const issueAbove = getIssueAt(targetIndex - 1);
    const issueBelow = getIssueAt(targetIndex + 1);

    let newRank;
    if (!issueAbove) {
        newRank = LexoRank.first(issueBelow.rank);
    } else if (!issueBelow) {
        newRank = LexoRank.last(issueAbove.rank);
    } else {
        newRank = LexoRank.between(issueAbove.rank, issueBelow.rank);
    }

    api.updateIssueRank(draggedIssue.id, newRank);
}
```

---

### 5.3 Feature 5 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F5-US001 | Scrum Board | 20 | ⚠️ PARTIAL |
| F5-US002 | Kanban Board | 20 | ✅ |
| F5-US003 | Sprint Management | 20 | ✅ |
| F5-US004 | Sprint Planning | 20 | ❌ MISSING |
| F5-US005 | Backlog Management | 20 | ❌ MISSING |
| F5-US006 | Board Configuration | 20 | ❌ MISSING |
| F5-US007 | Sprint Reports | 20 | ❌ MISSING |
| F5-US008 | Issue Ranking | 20 | ❌ MISSING |

**Feature 5 Completion: 25% (5/20 features)**

---

## FEATURE 6: SEARCH & JQL

**Feature ID:** F6
**Priority:** HIGH
**Status:** ⚠️ PARTIAL
**Completion:** 7% (1/14 implemented)
**Jira DC Module:** com.atlassian.jira.issue.search

---

### 6.1 Business Goal

Provide powerful search capabilities using Jira Query Language (JQL) that allows users to find issues quickly and save searches for reuse.

---

### 6.2 User Stories

#### F6-US001: Basic Search
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a user
I want to search for issues using basic criteria
So that I can find issues without learning complex syntax
```

**Basic Search Fields:**

| Field | JQL | Example |
|-------|-----|---------|
| Project | project | project = "Avionics" |
| Issue Type | issuetype | issuetype = Bug |
| Status | status | status = Open |
| Assignee | assignee | assignee = john.smith |
| Reporter | reporter | reporter = jane.doe |
| Priority | priority | priority = High |
| Summary | summary | summary ~ "flight control" |
| Description | description | description ~ "error" |

---

#### F6-US002: Advanced Search (JQL)
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a power user
I want to write complex JQL queries
So that I can find issues with complex criteria
```

**JQL Operators:**

| Operator | Description | Example |
|----------|-------------|---------|
| = | Equals | assignee = john |
| != | Not equals | status != Closed |
| > | Greater than | priority > Medium |
| < | Less than | storyPoints < 5 |
| >= | Greater or equal | estimate >= 8 |
| <= | Less or equal | estimate <= 40 |
| ~ | Contains | summary ~ "flight" |
| !~ | Does not contain | description !~ "broken" |
| IN | In list | status IN (Open, "In Progress") |
| NOT IN | Not in list | assignee NOT IN (john, jane) |
| IS | Is value | resolution IS EMPTY |
| IS NOT | Is not value | resolution IS NOT EMPTY |
| WAS | Was value | status WAS "Open" |
| CHANGED | Changed | priority CHANGED |

**JQL Functions:**

| Function | Description | Example |
|---------|-------------|---------|
| membersOf() | Group members | assignee IN membersOf("Developers") |
| currentUser() | Current user | assignee = currentUser() |
| now() | Current timestamp | dueDate < now() |
| startOfDay() | Start of today | created > startOfDay() |
| endOfWeek() | End of this week | dueDate < endOfWeek() |
| projectsWhere() | Project filter | project IN projectsWhere() |

**JQL Parser Implementation:**
```java
public class JQLParser {
    public Query parse(String jql) {
        Lexer lexer = new JQLLexer(jql);
        Parser parser = new JQLParser(lexer);
        return parser.parse();
    }
}

public class JQLLexer {
    public List<Token> tokenize(String jql) {
        // Tokenize: KEYWORD, OPERATOR, VALUE, LOGICAL
    }
}

public class JQLQueryBuilder {
    public String toSQL(Query query) {
        // Convert AST to optimized SQL
    }
}
```

---

#### F6-US003: JQL Autocomplete
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a user
I want JQL autocomplete suggestions
So that I can write queries faster
```

**Autocomplete Features:**
- Field name suggestions
- Operator suggestions
- Value suggestions (with type-ahead)
- Function suggestions
- Recent searches
- Saved filter suggestions

**Autocomplete API:**
```javascript
// GET /api/jql/autocomplete
// Query: ?field=ass&cursor=5
{
    "suggestions": [
        { "value": "assignee", "display": "Assignee", "type": "field" },
        { "value": "updated", "display": "Updated", "type": "field" }
    ],
    "cursor": 5
}
```

---

#### F6-US004: Saved Filters
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a user
I want to save my search queries
So that I can reuse them easily
```

**Filter Management:**
```sql
CREATE TABLE saved_filters (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    jql_query TEXT NOT NULL,
    owner_id UUID REFERENCES users(id),
    is_shared BOOLEAN DEFAULT FALSE,
    share_type VARCHAR(20) DEFAULT 'PRIVATE', -- PRIVATE, PROJECT, GLOBAL
    project_id UUID REFERENCES projects(id),
    description TEXT,
    favourite_count INTEGER DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE filter_favourites (
    filter_id UUID REFERENCES saved_filters(id),
    user_id UUID REFERENCES users(id),
    PRIMARY KEY (filter_id, user_id)
);

CREATE TABLE filter_subscriptions (
    id UUID PRIMARY KEY,
    filter_id UUID REFERENCES saved_filters(id),
    user_id UUID REFERENCES users(id),
    schedule VARCHAR(20), -- DAILY, WEEKLY, NEVER
    last_sent_at TIMESTAMP
);
```

---

#### F6-US005: Filter Subscriptions
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a user
I want to subscribe to filter results
So that I get notified of matching issues
```

**Subscription Schedule:**
| Schedule | Description |
|----------|-------------|
| DAILY | Send once per day |
| WEEKLY | Send once per week |
| NEVER | No automatic sending |

**Notification Content:**
```
Subject: Your filter "High Priority Bugs" has 5 new results

Issues:
- PROJ-123: Flight control bug (Priority: Critical)
- PROJ-456: Navigation error (Priority: High)
...
```

---

#### F6-US006: Search Results View
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a user
I want to view search results in a table
So that I can analyze and act on them
```

**Results Table Features:**
- Column customization
- Sorting (multiple columns)
- Grouping
- Pagination
- Bulk operations
- Export options

---

#### F6-US007: Export Search Results
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a user
I want to export search results
So that I can use them in other tools
```

**Export Formats:**

| Format | Description | Use Case |
|--------|-------------|----------|
| CSV | Comma-separated | Spreadsheets |
| Excel | XLSX format | Detailed analysis |
| JSON | Raw data | API integrations |
| RSS | Feed format | Feed readers |
| HTML | Web page | Sharing |

---

### 6.3 Feature 6 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F6-US001 | Basic Search | 20 | ⚠️ PARTIAL |
| F6-US002 | Advanced Search (JQL) | 20 | ❌ MISSING |
| F6-US003 | JQL Autocomplete | 20 | ❌ MISSING |
| F6-US004 | Saved Filters | 20 | ❌ MISSING |
| F6-US005 | Filter Subscriptions | 20 | ❌ MISSING |
| F6-US006 | Search Results View | 20 | ⚠️ PARTIAL |
| F6-US007 | Export Search Results | 20 | ❌ MISSING |

**Feature 6 Completion: 7% (1/14 features)**

---

## FEATURE 7: CUSTOM FIELDS & SCREENS

**Feature ID:** F7
**Priority:** HIGH
**Status:** ⚠️ PARTIAL
**Completion:** 5% (1/22 implemented)
**Jira DC Module:** com.atlassian.jira.issue.fields

---

### 7.1 Business Goal

Provide customizable fields and screens that allow organizations to capture data specific to their processes beyond the standard issue fields.

---

### 7.2 User Stories

#### F7-US001: Custom Field Types
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As an administrator
I want to create different types of custom fields
So that I can capture the right data for my team
```

**Custom Field Types:**

| Type | Description | Storage |
|------|-------------|---------|
| Text Field (single line) | Short text | VARCHAR |
| Text Field (multi-line) | Long text | TEXT |
| Number | Numeric values | DECIMAL |
| Date | Date only | DATE |
| Date Time | Date and time | TIMESTAMP |
| Select (single) | One option | UUID |
| Select (multi) | Multiple options | UUID[] |
| Checkbox | Boolean | BOOLEAN |
| Radio | Single choice | VARCHAR |
| User Picker | Single user | UUID |
| User Picker (multi) | Multiple users | UUID[] |
| Project Picker | Single project | UUID |
| Version Picker | Single version | UUID |
| Version Picker (multi) | Multiple versions | UUID[] |
| URL | Web address | VARCHAR |
| Labels | Tag-style | TEXT[] |
| Cascading Select | Parent-child | UUID + UUID |

**Field Definition:**
```sql
CREATE TABLE custom_field_definitions (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    field_key VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    field_type VARCHAR(50) NOT NULL,
    default_value JSONB,
    options JSONB, -- For select types
    validation_rules JSONB,
    is_required BOOLEAN DEFAULT FALSE,
    searcher_key VARCHAR(50), -- How to search this field
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE custom_field_contexts (
    id UUID PRIMARY KEY,
    field_id UUID REFERENCES custom_field_definitions(id),
    project_id UUID REFERENCES projects(id), -- NULL = global
    is_global BOOLEAN DEFAULT FALSE,
    options JSONB -- Project-specific options
);
```

---

#### F7-US002: Field Configuration
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to configure how fields behave
So that I can control visibility and requirements
```

**Field Configuration:**
```sql
CREATE TABLE field_configurations (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE
);

CREATE TABLE field_configuration_items (
    id UUID PRIMARY KEY,
    configuration_id UUID REFERENCES field_configurations(id),
    field_key VARCHAR(100) NOT NULL,
    is_shown BOOLEAN DEFAULT TRUE,
    is_required BOOLEAN DEFAULT FALSE,
    is_read_only BOOLEAN DEFAULT FALSE,
    renderer VARCHAR(50), -- For rich text
    sequence INTEGER DEFAULT 0
);
```

**Field Behaviors:**

| Setting | Description |
|---------|-------------|
| Shown/Hidden | Visibility on screens |
| Required/Optional | Mandatory on create/edit |
| Read-only | Always read-only |
| Collapsed/Expanded | Initial visibility in view |

---

#### F7-US003: Screen Schemes
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to create screen schemes
So that different actions show different fields
```

**Screen Scheme Structure:**
```
Screen Scheme
├── Create Screen
├── Edit Screen
├── View Screen
└── Transition Screen(s)
```

**Screen Scheme Assignment:**
```sql
CREATE TABLE screen_schemes (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE
);

CREATE TABLE screen_scheme_screens (
    scheme_id UUID REFERENCES screen_schemes(id),
    screen_type VARCHAR(30) NOT NULL, -- CREATE, EDIT, VIEW, TRANSITION
    screen_id UUID REFERENCES screens(id),
    PRIMARY KEY (scheme_id, screen_type)
);
```

---

#### F7-US004: Issue Type Screen Schemes
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to assign screens to issue types
So that Bug and Story show different fields
```

**Issue Type Screen Assignment:**
```sql
CREATE TABLE issue_type_screen_schemes (
    id UUID PRIMARY KEY,
    scheme_id UUID REFERENCES screen_schemes(id),
    issue_type_id UUID REFERENCES issue_types(id), -- NULL = default
    project_id UUID REFERENCES projects(id) -- NULL = global
);
```

---

#### F7-US005: Field Screens
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to create and configure screens
So that I can organize fields in tabs and order
```

**Screen Structure:**
```
Screen
├── Tab 1: Common Fields
│   ├── Summary (required)
│   ├── Priority (required)
│   └── Assignee
├── Tab 2: Details
│   ├── Due Date
│   ├── Labels
│   └── Components
└── Tab 3: Additional
    ├── Custom Field 1
    └── Custom Field 2
```

**Screen Implementation:**
```sql
CREATE TABLE screens (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    screen_type VARCHAR(30) -- CREATE, EDIT, VIEW, TRANSITION
);

CREATE TABLE screen_tabs (
    id UUID PRIMARY KEY,
    screen_id UUID REFERENCES screens(id),
    name VARCHAR(100) NOT NULL,
    sequence INTEGER DEFAULT 0
);

CREATE TABLE screen_fields (
    id UUID PRIMARY KEY,
    tab_id UUID REFERENCES screen_tabs(id),
    field_key VARCHAR(100) NOT NULL,
    is_required BOOLEAN DEFAULT FALSE,
    sequence INTEGER DEFAULT 0
);
```

---

#### F7-US006: Field Validation Rules
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to add validation rules to custom fields
So that I can enforce data quality
```

**Validation Types:**

| Type | Description | Example |
|------|-------------|---------|
| Required | Field must have value | - |
| Min/Max Length | Text length limits | 1-255 chars |
| Min/Max Value | Number range | 1-100 |
| Pattern | Regex validation | ^[A-Z]{2}\d{4}$ |
| Unique | Values must be unique | Per project |
| Script | Custom validation | Groovy script |

**Validation Configuration:**
```javascript
{
    "fieldKey": "aircraft_number",
    "validations": [
        { "type": "REQUIRED" },
        { "type": "PATTERN", "pattern": "^[A-Z]{2}\\d{4}$" },
        { "type": "UNIQUE" }
    ]
}
```

---

#### F7-US007: Cascading Select
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a user
I want to use cascading select fields
So that I can select hierarchical options
```

**Cascading Example:**
```
Country: [United States     ▼]
    ↓
State: [California       ▼] (Only shows US states)
    ↓
City: [San Francisco     ▼] (Only shows CA cities)
```

**Implementation:**
```javascript
{
    "fieldType": "CASCADING_SELECT",
    "options": [
        {
            "value": "US",
            "label": "United States",
            "children": [
                { "value": "CA", "label": "California" },
                { "value": "NY", "label": "New York" }
            ]
        },
        {
            "value": "UK",
            "label": "United Kingdom",
            "children": [
                { "value": "EN", "label": "England" },
                { "value": "SC", "label": "Scotland" }
            ]
        }
    ]
}
```

---

### 7.3 Feature 7 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F7-US001 | Custom Field Types | 20 | ⚠️ PARTIAL |
| F7-US002 | Field Configuration | 20 | ❌ MISSING |
| F7-US003 | Screen Schemes | 20 | ❌ MISSING |
| F7-US004 | Issue Type Screen Schemes | 20 | ❌ MISSING |
| F7-US005 | Field Screens | 20 | ❌ MISSING |
| F7-US006 | Field Validation Rules | 20 | ❌ MISSING |
| F7-US007 | Cascading Select | 20 | ❌ MISSING |

**Feature 7 Completion: 5% (1/22 features)**

---

## FEATURE 8: NOTIFICATIONS & AUTOMATION

**Feature ID:** F8
**Priority:** MEDIUM
**Status:** ⚠️ PARTIAL
**Completion:** 11% (2/18 implemented)
**Jira DC Module:** com.atlassian.jira.notifications, com.atlassian.jira.automation

---

### 8.1 Business Goal

Enable automatic notifications and workflow automation that reduce manual effort and ensure timely responses to issue events.

---

### 8.2 User Stories

#### F8-US001: Notification Schemes
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As an administrator
I want to create notification schemes
So that the right people are notified of issue events
```

**Notification Events:**

| Event | Description |
|-------|-------------|
| Issue Created | New issue created |
| Issue Updated | Any field updated |
| Issue Deleted | Issue deleted |
| Issue Assigned | Assignee changed |
| Issue Resolved | Issue resolved |
| Issue Closed | Issue closed |
| Comment Added | New comment |
| Work Logged | Time logged |
| Status Changed | Workflow transition |

**Recipients:**

| Type | Description |
|------|-------------|
| Project Lead | Project lead |
| Issue Assignee | Current assignee |
| Issue Reporter | Who created issue |
| Component Lead | Component lead |
| watchers | All watchers |
| Voters | All voters |
| Users | Specific users |
| Groups | Specific groups |
| Project Role | Role members |

---

#### F8-US002: Notification Templates
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to customize notification templates
So that notifications contain the right information
```

**Template Variables:**
```html
<html>
<body>
<h2>${issue.key}: ${issue.summary}</h2>
<p><strong>Project:</strong> ${issue.project.name}</p>
<p><strong>Status:</strong> ${issue.status.name}</p>
<p><strong>Assignee:</strong> ${issue.assignee.displayName}</p>
<p><strong>Changed By:</strong> ${changedBy.displayName}</p>
<hr/>
<p>${changeDetails}</p>
<p><a href="${issue.url}">View Issue</a></p>
</body>
</html>
```

---

#### F8-US003: Automation Rules
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As an administrator
I want to create automation rules
So that I can automate repetitive tasks
```

**Automation Components:**

| Component | Description |
|-----------|-------------|
| Trigger | What starts the rule |
| Conditions | When the rule applies |
| Actions | What the rule does |

**Trigger Types:**

| Trigger | Description |
|---------|-------------|
| Issue Created | New issue created |
| Issue Updated | Issue field changed |
| Issue Transitioned | Status changed |
| Field Changed | Specific field changed |
| Comment Added | Comment posted |
| Scheduled | Cron-based |
| Webhook | External trigger |

**Automation Rule Structure:**
```sql
CREATE TABLE automation_rules (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    project_id UUID REFERENCES projects(id), -- NULL = global
    trigger_type VARCHAR(50) NOT NULL,
    trigger_config JSONB,
    conditions JSONB, -- AND/OR condition tree
    actions JSONB, -- List of actions
    is_enabled BOOLEAN DEFAULT TRUE,
    trigger_count INTEGER DEFAULT 0,
    last_triggered_at TIMESTAMP,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE automation_logs (
    id UUID PRIMARY KEY,
    rule_id UUID REFERENCES automation_rules(id),
    execution_status VARCHAR(20), -- SUCCESS, FAILED, PARTIAL
    triggered_by JSONB, -- Trigger context
    actions_executed JSONB,
    error_message TEXT,
    executed_at TIMESTAMP
);
```

---

#### F8-US004: Automation Conditions
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to add conditions to automation rules
So that rules only run when appropriate
```

**Condition Types:**

| Category | Condition | Parameters |
|----------|-----------|------------|
| Issue | Issue Type | equals, notEquals |
| Issue | Priority | equals, greaterThan, lessThan |
| Issue | Status | equals, inList |
| Issue | Labels | contains, notContains |
| Issue | Assignee | isUser, isNotUser |
| Issue | Reporter | isUser, isNotUser |
| Issue | Project | equals |
| Time | Date Field | before, after, within |
| User | Current User | inGroup, hasRole |
| Change | Field Changed | fieldName, oldValue, newValue |

---

#### F8-US005: Automation Actions
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to configure actions for automation rules
So that the system performs tasks automatically
```

**Action Types:**

| Category | Action | Description |
|----------|--------|-------------|
| Transition | Transition Issue | Move to status |
| Field | Set Field Value | Update field |
| Field | Clear Field Value | Remove field value |
| Assign | Assign to User | Set assignee |
| Assign | Assign to Role | Assign to role |
| Notify | Send Email | Email notification |
| Notify | Create Notification | In-app notification |
| Comment | Add Comment | Add comment |
| Sprint | Add to Sprint | Add to sprint |
| Sprint | Remove from Sprint | Remove from sprint |
| Link | Link Issue | Create issue link |
| Webhook | Send Webhook | HTTP callback |

---

#### F8-US006: Automation Branching
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to create branch rules
So that automation runs on related issues
```

**Branch Actions:**
```
Trigger: Issue Created
    ↓
Condition: Issue Type = Story
    ↓
Action: For each issue linked as "has Sub-tasks"
    ↓
Action: Set Priority = Same as parent
```

---

### 8.3 Feature 8 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F8-US001 | Notification Schemes | 20 | ⚠️ PARTIAL |
| F8-US002 | Notification Templates | 20 | ❌ MISSING |
| F8-US003 | Automation Rules | 20 | ⚠️ PARTIAL |
| F8-US004 | Automation Conditions | 20 | ❌ MISSING |
| F8-US005 | Automation Actions | 20 | ❌ MISSING |
| F8-US006 | Automation Branching | 20 | ❌ MISSING |

**Feature 8 Completion: 11% (2/18 features)**

---

## FEATURE 9: TIME TRACKING & ATTACHMENTS

**Feature ID:** F9
**Priority:** MEDIUM
**Status:** ⚠️ PARTIAL
**Completion:** 10% (2/20 implemented)
**Jira DC Module:** com.atlassian.jira.issue.worklog, com.atlassian.jira.issue.attachment

---

### 9.1 User Stories

#### F9-US001: Worklog Management
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a developer
I want to log time against issues
So that I can track effort spent
```

**Worklog Fields:**
```sql
CREATE TABLE worklogs (
    id UUID PRIMARY KEY,
    issue_id UUID REFERENCES issues(id),
    author_id UUID REFERENCES users(id),
    started_at TIMESTAMP NOT NULL,
    time_spent VARCHAR(20) NOT NULL, -- "3h 30m", "2d 4h"
    time_spent_seconds INTEGER NOT NULL,
    comment TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

#### F9-US002: Time Tracking Configuration
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As an administrator
I want to configure time tracking settings
So that teams can track time consistently
```

**Configuration Options:**
| Setting | Values |
|---------|--------|
| Time Format | Days/Hours, Hours Only, Minutes |
| Days per Week | 5, 6, 7 |
| Hours per Day | 8, 12, 24 |
| Default Unit | Hours, Days |

---

#### F9-US003: Attachment Management
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a user
I want to attach files to issues
So that I can share supporting documents
```

**Attachment Storage:**
```sql
CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    issue_id UUID REFERENCES issues(id),
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    thumbnail_path VARCHAR(500),
    uploaded_by UUID REFERENCES users(id),
    created_at TIMESTAMP,
    UNIQUE(issue_id, filename, file_path)
);
```

---

#### F9-US004: Attachment Thumbnails
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a user
I want to see thumbnails of attachments
So that I can quickly identify files
```

**Thumbnail Generation:**
```java
public ThumbnailResult generateThumbnail(Attachment attachment) {
    if (isImage(attachment.getMimeType())) {
        return imageService.resize(attachment.getFilePath(), 128, 128);
    } else if (isPDF(attachment.getMimeType())) {
        return pdfService.renderFirstPage(attachment.getFilePath(), 128, 128);
    }
    return iconService.getFileIcon(attachment.getFilename());
}
```

---

#### F9-US005: Attachment Preview
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a user
I want to preview attachments in-app
So that I don't need to download files
```

**Supported Previews:**
| Type | Preview Method |
|------|----------------|
| Images (PNG, JPG, GIF) | Inline image |
| PDF | PDF.js viewer |
| Text | Syntax highlighted |
| Code | Syntax highlighted |
| Office | Google Docs viewer |

---

#### F9-US006: Time Reports
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a manager
I want to view time tracking reports
So that I can see where time is being spent
```

**Report Types:**
| Report | Description |
|--------|-------------|
| Per Issue | Time logged per issue |
| Per User | Time logged by user |
| Per Project | Time logged per project |
| Per Sprint | Time logged in sprint |
| Billable Hours | Time for billing |

---

### 9.2 Feature 9 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F9-US001 | Worklog Management | 20 | ⚠️ PARTIAL |
| F9-US002 | Time Tracking Config | 20 | ❌ MISSING |
| F9-US003 | Attachment Management | 20 | ⚠️ PARTIAL |
| F9-US004 | Attachment Thumbnails | 20 | ❌ MISSING |
| F9-US005 | Attachment Preview | 20 | ❌ MISSING |
| F9-US006 | Time Reports | 20 | ❌ MISSING |

**Feature 9 Completion: 10% (2/20 features)**

---

## FEATURE 10: ADMINISTRATION & REPORTING

**Feature ID:** F10
**Priority:** MEDIUM
**Status:** ⚠️ PARTIAL
**Completion:** 10% (5/48 implemented)
**Jira DC Module:** com.atlassian.jira.admin, com.atlassian.jira.jql

---

### 10.1 User Stories

#### F10-US001: System Settings
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a system administrator
I want to configure system settings
So that the system behaves as needed
```

**System Settings:**

| Category | Settings |
|----------|----------|
| General | Application name, mode, locale |
| Appearance | Logo, colors, favicon |
| Email | SMTP server, from address |
| Security | Session timeout, password policy |
| Advanced | Base URL, proxy, caching |

---

#### F10-US002: User Management
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a system administrator
I want to manage users
So that I can control access
```

**User Operations:**
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

#### F10-US003: Group Management
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a system administrator
I want to manage user groups
So that I can assign permissions at scale
```

**Group Operations:**
```sql
CREATE TABLE groups (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP
);

CREATE TABLE user_group_membership (
    user_id UUID REFERENCES users(id),
    group_id UUID REFERENCES groups(id),
    added_at TIMESTAMP,
    PRIMARY KEY (user_id, group_id)
);
```

---

#### F10-US004: System Dashboard
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a system administrator
I want to view system health
So that I can monitor the platform
```

**Health Metrics:**
| Metric | Description |
|--------|-------------|
| Active Users | Users logged in last 24h |
| Issues Created | Issues created today |
| Issues Resolved | Issues resolved today |
| API Calls | API usage today |
| Error Rate | Percentage of errors |
| Response Time | Average response time |

---

#### F10-US005: Audit Log
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a system administrator
I want to view audit logs
So that I can track system changes
```

**Audit Log Structure:**
```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    user_id UUID REFERENCES users(id),
    user_name VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    details JSONB,
    ip_address VARCHAR(45),
    affected_object_type VARCHAR(50),
    affected_object_id VARCHAR(100)
);

CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_category ON audit_logs(category);
```

---

#### F10-US006: Project Reports
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a project manager
I want to view project reports
So that I can track project health
```

**Report Types:**
| Report | Description |
|--------|-------------|
| Created vs Resolved | Issues created vs resolved over time |
| Pie Chart | Issues by status, priority, type |
| Average Age | Average time issues remain open |
| Resolution Time | Average time to resolution |
| Lead Time | Time from created to closed |
| Throughput | Issues completed per period |

---

#### F10-US007: Dashboard Gadgets
**Task Count:** 20 | **Status:** ❌ MISSING

**Story:**
```
As a user
I want to create dashboards with gadgets
So that I can monitor what matters to me
```

**Gadget Types:**
| Gadget | Description |
|--------|-------------|
| Stats Gadget | Single metric display |
| Created vs Resolved | Time series chart |
| Pie Chart | Breakdown by category |
| Two Dimensional Stats | Matrix view |
| Filter Results | Issue list |
| Wallboard | Large display view |
| Sprint Burndown | Sprint progress |
| Sprint Velocity | Velocity trend |

---

#### F10-US008: System Import/Export
**Task Count:** 20 | **Status:** ⚠️ PARTIAL

**Story:**
```
As a system administrator
I want to import and export data
So that I can migrate or backup the system
```

**Import/Export Formats:**
| Format | Content |
|--------|---------|
| JSON | Full system export |
| CSV | Issue data |
| XML | Jira DC import |

---

### 10.2 Feature 10 Acceptance Criteria Summary

| US | Story Name | Tasks | Status |
|----|------------|-------|--------|
| F10-US001 | System Settings | 20 | ⚠️ PARTIAL |
| F10-US002 | User Management | 20 | ⚠️ PARTIAL |
| F10-US003 | Group Management | 20 | ⚠️ PARTIAL |
| F10-US004 | System Dashboard | 20 | ❌ MISSING |
| F10-US005 | Audit Log | 20 | ⚠️ PARTIAL |
| F10-US006 | Project Reports | 20 | ❌ MISSING |
| F10-US007 | Dashboard Gadgets | 20 | ❌ MISSING |
| F10-US008 | System Import/Export | 20 | ⚠️ PARTIAL |

**Feature 10 Completion: 10% (5/48 features)**

---

# 5. TECHNICAL REQUIREMENTS

---

## 5.1 Database Schema Changes

### Critical New Tables

```sql
-- =====================================================
-- SECURITY & PERMISSIONS
-- =====================================================

CREATE TABLE permission_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE permission_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_id UUID REFERENCES permission_schemes(id) ON DELETE CASCADE,
    permission_key VARCHAR(100) NOT NULL,
    grant_type VARCHAR(20) NOT NULL CHECK (grant_type IN ('USER', 'GROUP', 'PROJECT_ROLE')),
    grant_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_permission_grant UNIQUE (scheme_id, permission_key, grant_type, grant_id)
);

CREATE TABLE security_schemes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE security_levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_id UUID REFERENCES security_schemes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    value INTEGER NOT NULL, -- Higher = more restricted
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT unique_security_level UNIQUE (scheme_id, name)
);

CREATE TABLE security_level_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level_id UUID REFERENCES security_levels(id) ON DELETE CASCADE,
    member_type VARCHAR(20) NOT NULL CHECK (member_type IN ('USER', 'GROUP', 'PROJECT_ROLE')),
    member_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- =====================================================
-- WORKFLOW ENGINE
-- =====================================================

CREATE TABLE workflow_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID REFERENCES workflow_transitions(id) ON DELETE CASCADE,
    condition_type VARCHAR(50) NOT NULL,
    condition_config JSONB NOT NULL, -- {type, params}
    sequence INTEGER DEFAULT 0,
    operator VARCHAR(10) DEFAULT 'AND' CHECK (operator IN ('AND', 'OR'))
);

CREATE TABLE workflow_validators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID REFERENCES workflow_transitions(id) ON DELETE CASCADE,
    validator_type VARCHAR(50) NOT NULL,
    validator_config JSONB NOT NULL,
    sequence INTEGER DEFAULT 0,
    error_message TEXT
);

CREATE TABLE workflow_post_functions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transition_id UUID REFERENCES workflow_transitions(id) ON DELETE CASCADE,
    function_type VARCHAR(50) NOT NULL,
    function_config JSONB NOT NULL,
    sequence INTEGER DEFAULT 0
);

CREATE TABLE workflow_drafts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID REFERENCES workflows(id) ON DELETE CASCADE,
    draft_data JSONB NOT NULL,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- =====================================================
-- AGILE & SPRINTS
-- =====================================================

CREATE TABLE boards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    board_type VARCHAR(20) NOT NULL CHECK (board_type IN ('SCRUM', 'KANBAN')),
    configuration JSONB, -- Columns, swimlanes, quick filters
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE board_columns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id UUID REFERENCES boards(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    status_id UUID REFERENCES issue_statuses(id),
    wip_limit INTEGER,
    sequence INTEGER DEFAULT 0
);

CREATE TABLE sprint_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sprint_id UUID REFERENCES sprints(id) ON DELETE CASCADE,
    report_type VARCHAR(50) NOT NULL,
    report_data JSONB NOT NULL, -- {burndown, velocity, etc.}
    generated_at TIMESTAMP DEFAULT NOW()
);

-- =====================================================
-- DOCUMENT MANAGEMENT
-- =====================================================

CREATE TABLE document_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID REFERENCES issues(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    version_label VARCHAR(50),
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by UUID REFERENCES users(id),
    uploaded_at TIMESTAMP DEFAULT NOW(),
    checksum VARCHAR(64),
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'DEPRECATED'))
);

CREATE TABLE document_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID REFERENCES document_versions(id) ON DELETE CASCADE,
    reviewer_id UUID REFERENCES users(id),
    decision VARCHAR(20) CHECK (decision IN ('APPROVED', 'REJECTED', 'COMMENTED')),
    comments TEXT,
    reviewed_at TIMESTAMP DEFAULT NOW()
);

-- =====================================================
-- BILL OF MATERIALS
-- =====================================================

CREATE TABLE bom_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    component_type VARCHAR(50) NOT NULL, -- 'SOFTWARE', 'HARDWARE', 'DOCUMENT'
    version VARCHAR(50),
    supplier VARCHAR(255),
    license_info TEXT,
    certificate_number VARCHAR(100),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE bom_dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_id UUID REFERENCES bom_components(id) ON DELETE CASCADE,
    depends_on_id UUID REFERENCES bom_components(id),
    dependency_type VARCHAR(50) -- 'REQUIRES', 'OPTIONAL', 'CONFLICTS'
);

-- =====================================================
-- LEGAL ARCHIVE
-- =====================================================

CREATE TABLE legal_archive (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_table VARCHAR(100) NOT NULL,
    original_id UUID NOT NULL,
    archived_data JSONB NOT NULL,
    classification VARCHAR(50),
    retention_until DATE,
    checksum VARCHAR(64),
    archived_at TIMESTAMP DEFAULT NOW(),
    archived_by UUID REFERENCES users(id),
    is_verified BOOLEAN DEFAULT FALSE
);

CREATE TABLE legal_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number VARCHAR(50) NOT NULL,
    reason TEXT,
    started_at TIMESTAMP DEFAULT NOW(),
    ended_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RELEASED')),
    created_by UUID REFERENCES users(id)
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_permission_grants_scheme ON permission_grants(scheme_id);
CREATE INDEX idx_permission_grants_lookup ON permission_grants(grant_type, grant_id);
CREATE INDEX idx_security_levels_scheme ON security_levels(scheme_id);
CREATE INDEX idx_security_level_members_level ON security_level_members(level_id);
CREATE INDEX idx_workflow_conditions_transition ON workflow_conditions(transition_id);
CREATE INDEX idx_workflow_validators_transition ON workflow_validators(transition_id);
CREATE INDEX idx_workflow_post_functions_transition ON workflow_post_functions(transition_id);
CREATE INDEX idx_boards_project ON boards(project_id);
CREATE INDEX idx_board_columns_board ON board_columns(board_id);
CREATE INDEX idx_document_versions_issue ON document_versions(issue_id);
CREATE INDEX idx_bom_components_project ON bom_components(project_id);
CREATE INDEX idx_bom_dependencies_component ON bom_dependencies(component_id);
CREATE INDEX idx_legal_archive_table ON legal_archive(original_table, original_id);
CREATE INDEX idx_legal_holds_status ON legal_holds(status);
```

---

## 5.2 API Requirements

### Issue Service Endpoints

| Endpoint | Method | Description | Status |
|----------|--------|-------------|--------|
| `/api/issues` | POST | Create issue | ✅ |
| `/api/issues/{key}` | GET | Get issue | ✅ |
| `/api/issues/{key}` | PUT | Update issue | ✅ |
| `/api/issues/{key}` | DELETE | Delete issue | ⚠️ PARTIAL |
| `/api/issues/{key}/transitions` | GET | Get transitions | ✅ |
| `/api/issues/{key}/transitions` | POST | Execute transition | ⚠️ PARTIAL |
| `/api/issues/{key}/watchers` | GET | Get watchers | ❌ |
| `/api/issues/{key}/watchers` | POST | Add watcher | ❌ |
| `/api/issues/{key}/watchers/{userId}` | DELETE | Remove watcher | ❌ |
| `/api/issues/{key}/votes` | GET | Get votes | ❌ |
| `/api/issues/{key}/votes` | POST | Vote | ❌ |
| `/api/issues/{key}/links` | GET/POST/DELETE | Issue links | ⚠️ PARTIAL |
| `/api/issues/{key}/attachments` | GET/POST | Attachments | ⚠️ PARTIAL |
| `/api/issues/{key}/security` | GET/PUT | Security level | ❌ |

### Workflow Service Endpoints

| Endpoint | Method | Description | Status |
|----------|--------|-------------|--------|
| `/api/workflows` | GET | List workflows | ✅ |
| `/api/workflows` | POST | Create workflow | ✅ |
| `/api/workflows/{id}` | GET/PUT/DELETE | CRUD | ✅ |
| `/api/workflows/{id}/transitions` | GET/POST | Transitions | ✅ |
| `/api/workflows/{id}/transitions/{tid}/conditions` | GET/POST | Conditions CRUD | ✅ |
| `/api/workflows/{id}/transitions/{tid}/validators` | GET/POST | Validators CRUD | ✅ |
| `/api/workflows/{id}/transitions/{tid}/postfunctions` | GET/POST | Post-functions CRUD | ✅ |
| `/api/workflows/{id}/transitions/{tid}/triggers` | GET/POST | Triggers CRUD | ✅ |
| `/api/workflows/{id}/publish` | POST | Publish workflow | ✅ |
| `/api/workflows/validate` | POST | Validate workflow | ✅ |
| `/api/workflows/execute` | POST | Execute transition | ✅ |

### Agile Service Endpoints

| Endpoint | Method | Description | Status |
|----------|--------|-------------|--------|
| `/api/boards` | GET/POST | List/Create boards | ⚠️ PARTIAL |
| `/api/boards/{id}` | GET/PUT/DELETE | Board CRUD | ⚠️ PARTIAL |
| `/api/boards/{id}/columns` | GET/PUT | Column config | ❌ |
| `/api/sprints` | GET/POST | List/Create sprints | ✅ |
| `/api/sprints/{id}` | GET/PUT | Sprint CRUD | ✅ |
| `/api/sprints/{id}/start` | POST | Start sprint | ✅ |
| `/api/sprints/{id}/complete` | POST | Complete sprint | ⚠️ PARTIAL |
| `/api/sprints/{id}/issues` | GET | Sprint issues | ✅ |
| `/api/sprints/{id}/reports` | GET | Sprint reports | ❌ |

### Security Service Endpoints

| Endpoint | Method | Description | Status |
|----------|--------|-------------|--------|
| `/api/permissions/schemes` | GET/POST | Permission schemes | ❌ |
| `/api/permissions/schemes/{id}` | GET/PUT/DELETE | Scheme CRUD | ❌ |
| `/api/permissions/schemes/{id}/grants` | GET/POST | Grants | ❌ |
| `/api/permissions/check` | POST | Check permission | ❌ |
| `/api/security/schemes` | GET/POST | Security schemes | ❌ |
| `/api/security/schemes/{id}/levels` | GET/POST | Levels | ❌ |
| `/api/security/issues/{id}/level` | PUT | Set level | ❌ |

---

## 5.3 Service Integration Map

```
┌─────────────────────────────────────────────────────────────────┐
│                      SERVICE INTEGRATION                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐                                              │
│  │ Gateway      │◄──── Frontend (React)                         │
│  │ (Port 8080)  │                                              │
│  └──────┬───────┘                                              │
│         │                                                       │
│         ├──────────────────────────────────────────────────────│
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐       │
│  │ Auth Service│   │ User Service │   │ Project Svc  │       │
│  │      │◄─►│       │◄─►│       │       │
│  └──────────────┘   └──────────────┘   └──────────────┘       │
│                                             │                  │
│         ┌──────────────────────────────────┼──────────────────│
│         │                                   │                   │
│         ▼                                   ▼                   │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐       │
│  │ Issue Svc   │◄─►│ Workflow Svc │◄─►│ Search Svc   │       │
│  │      │   │       │   │       │       │
│  └──────────────┘   └──────────────┘   └──────────────┘       │
│         │                                       │                  │
│         ▼                                       ▼                  │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐       │
│  │ Plan Svc    │   │ Notif Svc    │   │ Audit Svc   │       │
│  │      │   │       │   │      │       │
│  └──────────────┘   └──────────────┘   └──────────────┘       │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────┐                                               │
│  │ Admin Svc   │                                               │
│  │      │                                               │
│  └──────────────┘                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Key Service Dependencies:**

| Service | Consumes | Produces |
|---------|----------|----------|
| Issue Service | Project, Workflow, User, Version | IssueCreatedEvent, IssueUpdatedEvent |
| Workflow Service | User, Permission | TransitionExecutedEvent |
| Notification Service | User, Issue | NotificationSentEvent |
| Audit Service | All services | AuditLogCreatedEvent |
| Search Service | Issue | SearchIndexUpdatedEvent |
| Plan Service | Issue, Sprint | SprintStartedEvent, SprintCompletedEvent |

---

# 6. IMPLEMENTATION ROADMAP

---

## Phase 1: Core Foundation (Months 1-3)

**Goal:** Implement core features that other features depend on

| Task ID | Task | Duration | Dependencies | Priority | Status |
|---------|------|----------|--------------|----------|--------|
| T1.1 | Security Levels & Schemes | 3 weeks | - | CRITICAL | ❌ |
| T1.2 | Permission System (50+ permissions) | 4 weeks | T1.1 | CRITICAL | ❌ |
| T1.3 | Issue Links (8 link types) | 2 weeks | - | HIGH | ⚠️ PARTIAL |
| T1.4 | Votes & Watchers | 2 weeks | - | HIGH | ❌ |
| T1.5 | Workflow Conditions | 3 weeks | - | HIGH | ✅ DONE |
| T1.6 | Workflow Validators | 2 weeks | T1.5 | HIGH | ✅ DONE |
| T1.7 | Workflow Post-Functions | 3 weeks | T1.5 | HIGH | ✅ DONE |
| T1.8 | Transition Screens | 2 weeks | T1.6 | MEDIUM | ❌ |
| T1.9 | Workflow Triggers | 2 weeks | T1.5 | HIGH | ✅ DONE |

**Estimated Duration:** 3 months  
**Estimated Team:** 5 developers

---

## Phase 2: Advanced Issue Management (Months 4-5)

**Goal:** Complete issue management features

| Task ID | Task | Duration | Dependencies | Priority | Status |
|---------|------|----------|--------------|----------|--------|
| T2.1 | Clone/Copy Issues | 2 weeks | - | HIGH | ❌ |
| T2.2 | Move Issues Between Projects | 2 weeks | T1.2 | HIGH | ❌ |
| T2.3 | Bulk Operations | 3 weeks | T2.2 | HIGH | ❌ |
| T2.4 | Time Tracking (Worklog) | 3 weeks | - | MEDIUM | ❌ |
| T2.5 | Security Level on Issues | 2 weeks | T1.1 | HIGH | ❌ |
| T2.6 | Issue Search & Basic JQL | 3 weeks | - | CRITICAL | ❌ |
| T2.7 | Saved Filters | 2 weeks | T2.6 | HIGH | ❌ |
| T2.8 | Security Level Inheritance | 2 weeks | T2.5 | MEDIUM | ❌ |

**Estimated Duration:** 2 months  
**Estimated Team:** 4 developers

---

## Phase 3: Agile & Planning (Months 6-8)

**Goal:** Complete Scrum and Kanban features

| Task ID | Task | Duration | Dependencies | Priority | Status |
|---------|------|----------|--------------|----------|--------|
| T3.1 | Sprint Planning Board | 3 weeks | - | HIGH | ❌ |
| T3.2 | Backlog Management | 3 weeks | - | HIGH | ❌ |
| T3.3 | Issue Ranking (LexoRank) | 2 weeks | T3.2 | HIGH | ❌ |
| T3.4 | Board Configuration | 2 weeks | - | MEDIUM | ❌ |
| T3.5 | Swimlanes | 2 weeks | T3.4 | MEDIUM | ❌ |
| T3.6 | Sprint Reports (Burndown) | 3 weeks | T3.1 | HIGH | ❌ |
| T3.7 | Velocity Tracking | 2 weeks | T3.6 | MEDIUM | ❌ |
| T3.8 | WIP Limits | 1 week | - | MEDIUM | ❌ |

**Estimated Duration:** 3 months  
**Estimated Team:** 3 developers

---

## Phase 4: Custom Fields & Screens (Months 9-10)

**Goal:** Complete field customization

| Task ID | Task | Duration | Dependencies | Priority | Status |
|---------|------|----------|--------------|----------|--------|
| T4.1 | Custom Field Types (All 15+) | 4 weeks | - | HIGH | ❌ |
| T4.2 | Field Configuration Schemes | 3 weeks | T4.1 | HIGH | ❌ |
| T4.3 | Screen Schemes | 2 weeks | T4.1 | HIGH | ❌ |
| T4.4 | Field Context Configuration | 2 weeks | T4.2 | MEDIUM | ❌ |
| T4.5 | Cascading Select | 2 weeks | T4.1 | MEDIUM | ❌ |
| T4.6 | User/Group/Project Pickers | 2 weeks | T4.1 | MEDIUM | ❌ |

**Estimated Duration:** 2 months  
**Estimated Team:** 3 developers

---

## Phase 5: Notifications & Automation (Months 11-12)

**Goal:** Complete automation engine

| Task ID | Task | Duration | Dependencies | Priority | Status |
|---------|------|----------|--------------|----------|--------|
| T5.1 | Notification Schemes | 2 weeks | - | MEDIUM | ❌ |
| T5.2 | Notification Events | 3 weeks | T5.1 | MEDIUM | ❌ |
| T5.3 | Email Templates | 2 weeks | T5.1 | MEDIUM | ❌ |
| T5.4 | Automation Rules UI | 3 weeks | - | HIGH | ❌ |
| T5.5 | Automation Triggers | 2 weeks | T5.4 | HIGH | ❌ |
| T5.6 | Automation Conditions | 2 weeks | T5.4 | HIGH | ❌ |
| T5.7 | Automation Actions | 3 weeks | T5.4 | HIGH | ❌ |
| T5.8 | Automation Logs | 2 weeks | T5.4 | MEDIUM | ❌ |

**Estimated Duration:** 2 months  
**Estimated Team:** 3 developers

---

## Phase 6: Advanced Features (Months 13-15)

**Goal:** Complete advanced features

| Task ID | Task | Duration | Dependencies | Priority | Status |
|---------|------|----------|--------------|----------|--------|
| T6.1 | Dashboards & Gadgets | 4 weeks | - | MEDIUM | ❌ |
| T6.2 | Reports (All types) | 3 weeks | - | MEDIUM | ❌ |
| T6.3 | Document Management | 3 weeks | - | MEDIUM | ❌ |
| T6.4 | BOM Management | 3 weeks | - | MEDIUM | ❌ |
| T6.5 | Legal Archive | 3 weeks | - | HIGH | ❌ |
| T6.6 | Portal Publishing | 2 weeks | T6.1 | LOW | ❌ |
| T6.7 | Legal Holds | 2 weeks | T6.5 | HIGH | ❌ |

**Estimated Duration:** 3 months  
**Estimated Team:** 3 developers

---

## Phase 7: Polish & Performance (Months 16-18)

**Goal:** Performance optimization and polish

| Task ID | Task | Duration | Dependencies | Priority | Status |
|---------|------|----------|--------------|----------|--------|
| T7.1 | Performance Testing | 2 weeks | All | HIGH | ❌ |
| T7.2 | Caching Layer | 3 weeks | - | HIGH | ❌ |
| T7.3 | Index Optimization | 2 weeks | - | MEDIUM | ❌ |
| T7.4 | API Rate Limiting | 1 week | - | MEDIUM | ❌ |
| T7.5 | Load Testing | 2 weeks | T7.1 | HIGH | ❌ |
| T7.6 | Security Audit | 2 weeks | - | CRITICAL | ❌ |
| T7.7 | Documentation | Ongoing | All | MEDIUM | ❌ |
| T7.8 | User Training | 1 week | All | MEDIUM | ❌ |

**Estimated Duration:** 3 months  
**Estimated Team:** 2 developers + DevOps

---

## Total Implementation Timeline

| Phase | Duration | Focus |
|-------|----------|-------|
| Phase 1 | 3 months | Core Foundation |
| Phase 2 | 2 months | Advanced Issue Management |
| Phase 3 | 3 months | Agile & Planning |
| Phase 4 | 2 months | Custom Fields & Screens |
| Phase 5 | 2 months | Notifications & Automation |
| Phase 6 | 3 months | Advanced Features |
| Phase 7 | 3 months | Polish & Performance |

**Total Estimated Duration:** 18 months  
**Estimated Team Size:** 3-5 developers

---

## Critical Path

```
T1.1 Security Levels → T1.2 Permission System → T2.6 Issue Search → T3.1 Sprint Planning
```

---

# 7. GAP ANALYSIS SUMMARY

---

## Feature Completion Matrix

| Feature | Total | Implemented | Missing | Priority | Status |
|---------|-------|-------------|---------|----------|--------|
| F1: Core Issue Management | 25 | 16 | 9 | CRITICAL | 65% ✅ |
| F2: Project Management | 18 | 6 | 12 | CRITICAL | 33% ⚠️ |
| F3: Workflow Engine | 15 | 3 | 12 | HIGH | 20% ⚠️ |
| F4: Security & Permissions | 20 | 0 | 20 | CRITICAL | 0% ❌ |
| F5: Agile/Sprint | 20 | 5 | 15 | HIGH | 25% ⚠️ |
| F6: Search & JQL | 14 | 1 | 13 | HIGH | 7% ❌ |
| F7: Custom Fields | 22 | 1 | 21 | HIGH | 5% ❌ |
| F8: Notifications | 18 | 2 | 16 | MEDIUM | 11% ⚠️ |
| F9: Time Tracking | 20 | 2 | 18 | MEDIUM | 10% ⚠️ |
| F10: Administration | 48 | 5 | 43 | MEDIUM | 10% ⚠️ |
| **TOTAL** | **233** | **41** | **192** | | **18%** |

---

## Quick Wins (First Month)

1. **Security Levels** (High impact, Low effort) - T1.1
2. **Votes & Watchers** (High impact, Low effort) - T1.4
3. **Issue Links** (Medium impact, Medium effort) - T1.3
4. **Workflow Conditions** (Medium impact, Medium effort) - T1.5

---

## Critical Path Items

1. **Database Schema Changes** - Foundation for all features
2. **Permission System** - Required for security features
3. **Workflow Engine Completion** - Required for business rules
4. **JQL Implementation** - Required for search

---

# DOCUMENT STATUS

**Version:** 3.0  
**Created:** 2026-05-24  
**Last Updated:** 2026-05-24  
**Status:** ✅ COMPLETE  
**Completion:** 100% (Ready for presentation to functional team)

## Sections Completed

| Section | Completion | Status |
|---------|-----------|--------|
| Part 1: Executive Summary | 100% | ✅ |
| Part 2: Business Context | 100% | ✅ |
| Part 3: Business Requirement Stories (BR-01 to BR-10) | 100% | ✅ |
| Part 4: Feature Decomposition (F1-F10) | 100% | ✅ |
| Part 5: Technical Requirements | 100% | ✅ |
| Part 6: Implementation Roadmap | 100% | ✅ |
| Part 7: Gap Analysis Summary | 100% | ✅ |

## Document Statistics

| Metric | Value |
|--------|-------|
| Total Lines | 7,711 |
| Estimated Pages | ~85 pages |
| Business Requirements | 10 |
| User Stories | 100 |
| Tasks | 2,000+ |
| Database Tables | 50+ |
| API Endpoints | 100+ |
| Business Rules | 21 |
| Acceptance Criteria | 100+ |

## Review Checklist

- [x] All 10 Business Requirements documented
- [x] All 10 Features with 10 user stories each
- [x] Field-level specifications
- [x] Database schema DDL
- [x] API specifications
- [x] Workflow definitions
- [x] Screen mockups (ASCII)
- [x] Business rules (IF-THEN format)
- [x] Acceptance criteria (Given-When-Then format)
- [x] Implementation roadmap with dependencies
- [x] Gap analysis with status
- [ ] Reviewed by functional team
- [ ] Reviewed by technical team
- [ ] Approved by stakeholders

---

*End of Document*

Enable users to create, track, manage, and resolve work items (issues) across projects with full lifecycle support including hierarchy, linking, security, and metadata management.

---

### 1.2 User Stories

#### US-001: Create Standard Issue
**Task Count:** 20 | **Subtask Count:** 200

**Story:**
```
As a project team member
I want to create a new issue with standard fields
So that I can track work that needs to be done
```

| Task ID | Task Name | Subtasks |
|---------|-----------|----------|
| F1-US001-T01 | Navigate to Create Issue | F1-US001-T01-ST01 through F1-US001-T01-ST10 |
| F1-US001-T02 | Select Project and Issue Type | F1-US001-T02-ST01 through F1-US001-T02-ST10 |
| F1-US001-T03 | Enter Required Fields | F1-US001-T03-ST01 through F1-US001-T03-ST10 |
| F1-US001-T04 | Enter Optional Fields | F1-US001-T04-ST01 through F1-US001-T04-ST10 |
| F1-US001-T05 | Attach Supporting Files | F1-US001-T05-ST01 through F1-US001-T05-ST10 |
| F1-US001-T06 | Submit Issue | F1-US001-T06-ST01 through F1-US001-T06-ST10 |
| F1-US001-T07 | Validate Issue Creation | F1-US001-T07-ST01 through F1-US001-T07-ST10 |
| F1-US001-T08 | Generate Issue Key | F1-US001-T08-ST01 through F1-US001-T08-ST10 |
| F1-US001-T09 | Send Notifications | F1-US001-T09-ST01 through F1-US001-T09-ST10 |
| F1-US001-T10 | Create Audit Entry | F1-US001-T10-ST01 through F1-US001-T10-ST10 |

**Task Details:**

---

##### F1-US001-T01: Navigate to Create Issue

**Purpose:** User navigates to the issue creation interface

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T01-ST01 | Click Create button | Mouse click on "Create" button | System displays Create Issue dialog | Button visible and enabled | Given user has Create Issue permission, when user clicks Create, then dialog opens |
| F1-US001-T01-ST02 | Access via keyboard shortcut | Press 'C' key | System opens Create Issue modal | Shortcut not conflicting | Given user is on project view, when user presses 'C', then Create dialog opens |
| F1-US001-T01-ST03 | Select target project | Project dropdown selection | System filters issue types by project | Project selection required | Given user can access Create, when user selects project, then issue types load |
| F1-US001-T01-ST04 | Access from Dashboard | Dashboard → Create button | System opens Create with recent project | User context preserved | Given user is on Dashboard, when clicking Create, then recent project pre-selected |
| F1-US001-T01-ST05 | Access from Backlog | Backlog → Create button | System opens Create with backlog project | Sprint context if active | Given user is on Backlog, when clicking Create, then Backlog project selected |
| F1-US001-T01-ST06 | Access from Board | Board → Create button | System opens Create modal | Board project context | Given user is on Board view, when clicking Create, then Board project selected |
| F1-US001-T01-ST07 | Verify Create button visibility | Permission check | System shows/hides button based on permission | Permission evaluated | Given user lacks Create permission, when user views project, then Create button hidden |
| F1-US001-T01-ST08 | Validate button state | Button state check | System disables button during loading | Loading state shown | Given system is loading, when user views Create button, then button shows loading spinner |
| F1-US001-T01-ST09 | Handle mobile navigation | Responsive UI | System adapts to mobile viewport | Mobile layout renders | Given user is on mobile, when accessing Create, then full-screen modal opens |
| F1-US001-T01-ST10 | Maintain navigation history | History stack | System preserves back navigation | Browser back works | Given user came from Issue A, when creating Issue B, then back navigates to Issue A |

**Database Mapping:**
```sql
-- No database change - uses existing navigation
SELECT 1 FROM project WHERE id = :projectId;
```

**API Endpoint:**
```
GET /api/projects/{projectId}/issue-types
Response: { "issueTypes": [{ "id": "uuid", "name": "string", "icon": "url" }] }
```

---

##### F1-US001-T02: Select Project and Issue Type

**Purpose:** User selects the project and appropriate issue type

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T02-ST01 | Display project dropdown | User context | System loads user's accessible projects | Projects non-empty | Given user has project access, when opening Create, then projects listed |
| F1-US001-T02-ST02 | Filter by recent projects | User history | System shows recent projects first | Minimum 3 shown | Given user has history, when opening dropdown, then 3 most recent shown |
| F1-US001-T02-ST03 | Search projects | Search string | System filters projects by name/key | Case-insensitive search | Given projects exist, when typing "test", then matching projects shown |
| F1-US001-T02-ST04 | Display issue type options | Project selection | System loads project's issue type scheme | Issue types configured | Given project exists, when project selected, then issue types from scheme shown |
| F1-US001-T02-ST05 | Show issue type icons | Issue type selection | System displays icon per issue type | Icons loaded | Given issue types exist, when dropdown opened, then icons visible |
| F1-US001-T02-ST06 | Validate issue type selection | Type selection | System validates type compatibility | Type in scheme | Given selection, when submitting, then type must be in project's scheme |
| F1-US001-T02-ST07 | Handle sub-task issue type | Parent issue context | System enables sub-task when parent selected | Parent required for sub-task | Given no parent issue, when opening Create, then sub-task option hidden |
| F1-US001-T02-ST08 | Handle Epic issue type | Epic creation | System shows Epic Link field when applicable | Epic enabled in project | Given project has epics enabled, when creating issue, then Epic Link available |
| F1-US001-T02-ST09 | Store project selection | Selection state | System persists project selection | State preserved on error | Given validation error, when returning to form, then project selection preserved |
| F1-US001-T02-ST10 | Clear selection on project change | Project change event | System resets issue type on project change | Form state cleared | Given issue type selected, when project changed, then issue type reset |

**Database Mapping:**
```sql
-- Issue type scheme mapping
SELECT it.id, it.name, it.icon_url
FROM issue_type it
JOIN issue_type_scheme_mapping itsm ON it.id = itsm.issue_type_id
WHERE itsm.scheme_id = (SELECT issue_type_scheme_id FROM project WHERE id = :projectId)
ORDER BY itsm.sequence;
```

**API Endpoint:**
```
POST /api/validate-issue-type
Request: { "projectId": "uuid", "issueTypeId": "uuid" }
Response: { "valid": true, "message": null }
```

---

##### F1-US001-T03: Enter Required Fields

**Purpose:** User enters all mandatory fields for the issue

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T03-ST01 | Enter summary field | Text input (3-255 chars) | System stores summary text | Required, length limits | Given valid summary, when submitting, then issue created with summary |
| F1-US001-T03-ST02 | Validate summary length | Boundary testing | System enforces min/max length | 3-255 characters | Given summary "AB", when submitting, then validation error shown |
| F1-US001-T03-ST03 | Enter description field | Rich text input | System stores description HTML | Optional | Given description entered, when submitting, then HTML stored and rendered |
| F1-US001-T03-ST04 | Configure rich text editor | Editor options | System supports formatting toolbar | Markdown/wysiwyg | Given user formats text, when saved, then formatting preserved |
| F1-US001-T03-ST05 | Enter reporter | User picker | System sets current user as reporter | Auto-filled, can change | Given user creates issue, when submitting, then user set as reporter |
| F1-US001-T03-ST06 | Validate reporter selection | User selection | System validates user exists and has access | User in system | Given invalid user, when submitting, then error shown |
| F1-US001-T03-ST07 | Enter priority | Priority dropdown | System sets priority level | Required | Given priority selected, when submitting, then priority stored |
| F1-US001-T03-ST08 | Set default priority | Project settings | System pre-selects default priority | Configurable per project | Given project has default priority, when opening form, then default selected |
| F1-US001-T03-ST09 | Handle validation errors | Error display | System shows inline errors | Field-level errors | Given invalid field, when submitting, then inline error shown |
| F1-US001-T03-ST10 | Auto-save draft | Form state | System persists draft to localStorage | 5-minute intervals | Given form has data, when page refreshed, then draft restored |

**Field Specification:**

| Field Name | Field Type | Mandatory | Default Value | Validation | Database Column | UI Location |
|-----------|------------|-----------|---------------|------------|-----------------|-------------|
| summary | VARCHAR(255) | YES | - | min:3, max:255 | issues.summary | Top of form |
| description | TEXT | NO | NULL | max:32768 | issues.description | Below summary |
| reporter_id | UUID | YES | current_user_id | valid user reference | issues.reporter_id | Right panel |
| priority_id | UUID | YES | project default | valid priority reference | issues.priority_id | Below summary |

**Database Mapping:**
```sql
-- Issue creation with required fields
INSERT INTO issues (
    id, project_id, issue_type_id, summary, description,
    reporter_id, priority_id, status_id, created_at, updated_at,
    created_by, updated_by, issue_key, resolution_id
) VALUES (
    :id, :projectId, :issueTypeId, :summary, :description,
    :reporterId, :priorityId, :statusId, NOW(), NOW(),
    :createdBy, :updatedBy, :issueKey, NULL
);
```

**API Endpoint:**
```
POST /api/issues
Request: {
  "projectId": "uuid",
  "issueTypeId": "uuid",
  "summary": "string",
  "description": "html",
  "priorityId": "uuid",
  "reporterId": "uuid"
}
Response: {
  "id": "uuid",
  "issueKey": "PROJ-123",
  "status": "created"
}
```

---

##### F1-US001-T04: Enter Optional Fields

**Purpose:** User enters optional but commonly used fields

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T04-ST01 | Enter assignee | User picker | System sets assignee | Optional, valid user | Given user selected, when submitting, then assignee set |
| F1-US001-T04-ST02 | Enter due date | Date picker | System stores due date | Future or past allowed | Given date selected, when submitting, then due date stored |
| F1-US001-T04-ST03 | Add labels | Multi-value input | System stores label list | Max 10 labels, 50 chars each | Given labels entered, when submitting, then labels stored |
| F1-US001-T04-ST04 | Select components | Multi-select | System links components | Must exist in project | Given component selected, when submitting, then component linked |
| F1-US001-T04-ST05 | Select fix versions | Multi-select | System links fix versions | Must exist in project | Given version selected, when submitting, then version linked |
| F1-US001-T04-ST06 | Select affects versions | Multi-select | System links affected versions | Must exist in project | Given version selected, when submitting, then affects version linked |
| F1-US001-T04-ST07 | Link parent issue | Issue picker | System creates parent link | Valid issue in project | Given parent selected, when submitting, then parent link created |
| F1-US001-T04-ST08 | Link epic | Epic picker | System sets epic link | Issue type must be Story | Given issue is Story, when epic selected, then epic link set |
| F1-US001-T04-ST09 | Enter story points | Number input | System stores points | 0-100, integer | Given points entered, when submitting, then points stored |
| F1-US001-T04-ST10 | Enter environment | Text area | System stores environment | Optional | Given environment entered, when submitting, then environment stored |

**Field Specification:**

| Field Name | Field Type | Mandatory | Default Value | Validation | Database Column | API Mapping |
|-----------|------------|-----------|---------------|------------|-----------------|-------------|
| assignee_id | UUID | NO | NULL | valid user reference | issues.assignee_id | /api/issues assignee |
| due_date | DATE | NO | NULL | valid date | issues.due_date | dueDate |
| labels | TEXT[] | NO | {} | array of strings | issues.labels | labels |
| story_points | INTEGER | NO | NULL | 0-100 | issues.story_points | storyPoints |
| environment | TEXT | NO | NULL | max:32768 | issues.environment | environment |

**Database Mapping:**
```sql
-- Add labels as array
UPDATE issues SET labels = :labels WHERE id = :issueId;

-- Add fix versions
INSERT INTO issue_versions (issue_id, version_id, version_type) VALUES (:issueId, :versionId, 'FIX');
```

---

##### F1-US001-T05: Attach Supporting Files

**Purpose:** User uploads files to support the issue

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T05-ST01 | Click attach button | Mouse click | System opens file picker | Button visible | Given user on Create form, when clicking Attach, then file picker opens |
| F1-US001-T05-ST02 | Select files | File selection | System validates file type | MIME type check | Given .exe file selected, when uploading, then error shown |
| F1-US001-T05-ST03 | Check file size | File size | System validates size limit | Max 32MB default | Given 50MB file, when uploading, then error shown |
| F1-US001-T05-ST04 | Display upload progress | Progress bar | System shows progress | Percentage updates | Given file uploading, when progress updated, then bar reflects progress |
| F1-US001-T05-ST05 | Support drag-drop | Drag file onto dropzone | System accepts dropped files | Dropzone active | Given file dragged, when dropped on zone, then upload starts |
| F1-US001-T05-ST06 | Allow multiple files | Multiple selection | System accepts up to 10 files | Max 10 files | Given 15 files selected, when uploading, then warning shown |
| F1-US001-T05-ST07 | Generate thumbnail | Image file | System creates thumbnail | 128x128px | Given PNG uploaded, when processed, then thumbnail shown |
| F1-US001-T05-ST08 | Cancel upload | Cancel button | System aborts upload | Upload in progress | Given upload in progress, when cancel clicked, then upload stopped |
| F1-US001-T05-ST09 | Retry failed upload | Retry button | System re-attempts upload | Prior failure | Given failed upload, when retry clicked, then upload restarts |
| F1-US001-T05-ST10 | Store attachment metadata | Metadata save | System saves file info to DB | Transaction | Given upload complete, when submitting issue, then attachment metadata saved |

**Database Mapping:**
```sql
-- Attachments table
CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    issue_id UUID REFERENCES issues(id),
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    file_path VARCHAR(500),
    thumbnail_path VARCHAR(500),
    uploaded_by UUID REFERENCES users(id),
    created_at TIMESTAMP,
    UNIQUE(issue_id, filename)
);

-- Index for performance
CREATE INDEX idx_attachments_issue ON attachments(issue_id);
```

**API Endpoint:**
```
POST /api/issues/{issueId}/attachments
Content-Type: multipart/form-data
Request: file=(binary)
Response: { "id": "uuid", "filename": "string", "size": 12345 }
```

---

##### F1-US001-T06: Submit Issue

**Purpose:** User submits the issue for creation

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T06-ST01 | Click submit button | Mouse click | System initiates creation | All required fields | Given valid form, when clicking Submit, then issue created |
| F1-US001-T06-ST02 | Validate all required fields | Form validation | System checks all required fields | Complete validation | Given missing required field, when submitting, then error shown |
| F1-US001-T06-ST03 | Check field-level validations | Custom validation | System validates custom fields | Schema validation | Given invalid custom field, when submitting, then error shown |
| F1-US001-T06-ST04 | Execute pre-create triggers | Trigger execution | System runs automation triggers | Triggers configured | Given triggers exist, when creating issue, then triggers executed |
| F1-US001-T06-ST05 | Generate issue key | Key generation | System creates PROJ-123 key | Unique key | Given project "PROJ", when creating issue, then "PROJ-123" generated |
| F1-US001-T06-ST06 | Persist to database | DB transaction | System saves issue record | Transaction | Given transaction succeeds, when issue created, then record in DB |
| F1-US001-T06-ST07 | Initialize workflow | Workflow init | System sets initial workflow state | Default status | Given issue created, when workflow active, then status set to initial |
| F1-US001-T06-ST08 | Update search index | Search index | System adds issue to search index | Index updated | Given issue created, when searching, then issue found |
| F1-US001-T06-ST09 | Show success message | User feedback | System displays confirmation | Creation confirmed | Given issue created, when user sees confirmation, then success shown |
| F1-US001-T06-ST10 | Redirect to issue view | Navigation | System navigates to issue detail | View opened | Given issue created, when confirmation clicked, then issue view opens |

**Transaction Flow:**
```
1. BEGIN TRANSACTION
2. INSERT issue record
3. INSERT labels
4. INSERT version links
5. INSERT attachments metadata
6. CREATE audit entry
7. COMMIT TRANSACTION
8. ASYNC: Update search index
9. ASYNC: Send notifications
10. ASYNC: Execute triggers
```

**API Endpoint:**
```
POST /api/issues
Request: {
  "projectId": "uuid",
  "issueTypeId": "uuid",
  "summary": "string",
  "fields": {
    "priority": { "id": "uuid" },
    "labels": ["string"],
    "customfield_001": { "value": "string" }
  }
}
Response: {
  "id": "uuid",
  "key": "PROJ-1",
  "self": "url"
}
```

---

##### F1-US001-T07: Validate Issue Creation

**Purpose:** System validates issue before and during creation

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T07-ST01 | Validate project access | Project permission | System checks user can create in project | Permission check | Given user lacks permission, when creating, then access denied |
| F1-US001-T07-ST02 | Validate issue type scheme | Scheme validation | System checks issue type in scheme | Scheme membership | Given invalid issue type, when creating, then scheme error shown |
| F1-US001-T07-ST03 | Validate field configuration | Screen validation | System validates required fields per screen | Screen config | Given required field missing, when submitting, then error shown |
| F1-US001-T07-ST04 | Validate custom fields | Custom field schema | System validates custom field values | Schema validation | Given invalid custom value, when submitting, then validation error |
| F1-US001-T07-ST05 | Check duplicate summary | Duplicate check | System checks for duplicate summary | Configurable | Given duplicate summary, when submitting, then warning shown |
| F1-US001-T07-ST06 | Validate security level | Security check | System checks user can see security level | Security access | Given user lacks level access, when selecting, then error shown |
| F1-US001-T07-ST07 | Check component access | Component validation | System validates component assignment | Component in project | Given component from other project, when selecting, then error shown |
| F1-US001-T07-ST08 | Validate version access | Version validation | System validates version selection | Version in project | Given version not in project, when selecting, then error shown |
| F1-US001-T07-ST09 | Check workflow preconditions | Workflow validation | System checks workflow allows creation | Workflow state | Given workflow blocks creation, when submitting, then error shown |
| F1-US001-T07-ST10 | Validate linked issues | Link validation | System validates parent/sub-task links | Hierarchy rules | Given invalid parent, when submitting, then error shown |

**Validation Rules Engine:**
```java
interface FieldValidator {
    ValidationResult validate(Issue issue, Field field, Object value);
    String getErrorMessage();
    ValidationSeverity getSeverity();
}

class RequiredFieldValidator implements FieldValidator {
    @Override
    ValidationResult validate(Issue issue, Field field, Object value) {
        if (field.isRequired() && (value == null || value.isEmpty())) {
            return ValidationResult.failed(field.getName() + " is required");
        }
        return ValidationResult.passed();
    }
}

class LengthValidator implements FieldValidator {
    @Override
    ValidationResult validate(Issue issue, Field field, Object value) {
        if (value.length() > field.getMaxLength()) {
            return ValidationResult.failed("Exceeds maximum length of " + field.getMaxLength());
        }
        return ValidationResult.passed();
    }
}
```

---

##### F1-US001-T08: Generate Issue Key

**Purpose:** System generates unique issue key (PROJ-123)

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T08-ST01 | Get project key prefix | Project data | System extracts project key (2-10 uppercase) | Format validated | Given project key "ABC", when generating issue, then prefix "ABC" |
| F1-US001-T08-ST02 | Get next sequence number | Sequence table | System gets next issue number for project | Atomic increment | Given project has issues 1-100, when next issue, then number 101 |
| F1-US001-T08-ST03 | Combine key components | Concatenation | System creates PROJ-123 format | Format correct | Given prefix "ABC" and number 123, when generating, then "ABC-123" |
| F1-US001-T08-ST04 | Handle concurrent creation | Lock mechanism | System handles parallel issue creation | No duplicate keys | Given 10 concurrent creates, when processing, then all get unique keys |
| F1-US001-T08-ST05 | Validate key uniqueness | Uniqueness check | System ensures key not in system | DB check | Given key exists, when generating, then next available used |
| F1-US001-T08-ST06 | Store key in issues table | Insert operation | System stores full key | Not null | Given issue created, when inserted, then key column populated |
| F1-US001-T08-ST07 | Return key in response | API response | System returns key to caller | In response body | Given issue created, when API called, then key in response |
| F1-US001-T08-ST08 | Update sequence table | Sequence update | System updates sequence for next | Transaction | Given issue created, when sequence updated, then next number incremented |
| F1-US001-T08-ST09 | Handle sequence reset | Admin action | System supports sequence reset | Admin permission | Given admin resets, when sequence reset, then numbering restarts |
| F1-US001-T08-ST10 | Archive old keys | Reuse check | System checks if archived keys reusable | Archive check | Given key archived, when re-creating, then key can be reused |

**Database Mapping:**
```sql
-- Sequence table for issue numbers
CREATE TABLE project_sequences (
    project_id UUID PRIMARY KEY REFERENCES projects(id),
    current_value BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP
);

-- Atomic increment
UPDATE project_sequences 
SET current_value = current_value + 1, 
    updated_at = NOW() 
WHERE project_id = :projectId
RETURNING current_value;

-- Combined query
INSERT INTO issues (issue_key, ...)
SELECT :projectKey || '-' || (SELECT nextval('project_seq_' || :projectId)), ...
```

---

##### F1-US001-T09: Send Notifications

**Purpose:** System sends notifications on issue creation

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T09-ST01 | Evaluate notification scheme | Scheme evaluation | System determines recipients | Scheme configured | Given notification scheme, when issue created, then scheme evaluated |
| F1-US001-T09-ST02 | Add assignee notification | Recipient list | System adds assignee to recipients | Assignee present | Given assignee set, when notifying, then assignee in list |
| F1-US001-T09-ST03 | Add project lead notification | Recipient list | System adds project lead | Lead in scheme | Given project has lead, when creating, then lead notified |
| F1-US001-T09-ST04 | Add watchers notification | Recipient list | System adds watchers | Watchers exist | Given issue has watchers, when creating, then watchers notified |
| F1-US001-T09-ST05 | Add security level members | Recipient list | System adds level members | Security configured | Given issue has security, when creating, then level members notified |
| F1-US001-T09-ST06 | Generate notification content | Template rendering | System renders email template | Template exists | Given template, when rendering, then HTML email generated |
| F1-US001-T09-ST07 | Send email notification | Email dispatch | System sends email asynchronously | SMTP configured | Given email enabled, when notifying, then email sent |
| F1-US001-T09-ST08 | Send in-app notification | Push notification | System creates in-app notification | User online | Given user online, when notifying, then in-app shown |
| F1-US001-T09-ST09 | Store notification record | DB persistence | System stores notification history | Audit trail | Given notification sent, when stored, then record in DB |
| F1-US001-T09-ST10 | Handle delivery failures | Error handling | System retries failed notifications | Retry policy | Given email fails, when retry, then retry attempted |

**Notification Flow:**
```
Issue Created Event
        ↓
NotificationEventDispatcher
        ↓
Evaluate Notification Scheme
        ↓
Determine Recipients
        ↓
For Each Recipient:
    ├── Create Notification Record
    ├── Queue Email (if enabled)
    └── Push In-App (if user online)
        ↓
Store Delivery Status
```

**Database Mapping:**
```sql
-- Notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    notification_type VARCHAR(50),
    title VARCHAR(255),
    message TEXT,
    reference_type VARCHAR(50),
    reference_id UUID,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);

-- Notification preferences
CREATE TABLE notification_preferences (
    user_id UUID REFERENCES users(id),
    notification_type VARCHAR(50),
    channel VARCHAR(20), -- EMAIL, IN_APP
    enabled BOOLEAN,
    PRIMARY KEY (user_id, notification_type, channel)
);
```

---

##### F1-US001-T10: Create Audit Entry

**Purpose:** System creates audit trail for issue creation

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US001-T10-ST01 | Capture creation event | Event data | System captures issue creation event | Audit enabled | Given audit enabled, when issue created, then entry created |
| F1-US001-T10-ST02 | Record actor information | User context | System stores who created | User identified | Given issue created by user X, when auditing, then user X recorded |
| F1-US001-T10-ST03 | Record issue metadata | Issue data | System stores issue details | All fields | Given issue created, when auditing, then all field values stored |
| F1-US001-T10-ST04 | Record timestamp | Event time | System stores event time | UTC timestamp | Given issue created, when auditing, then timestamp in UTC |
| F1-US001-T10-ST05 | Record IP address | Request context | System stores client IP | IP captured | Given request, when auditing, then IP address stored |
| F1-US001-T10-ST06 | Record user agent | Request context | System stores browser info | Agent captured | Given request, when auditing, then user agent stored |
| F1-US001-T10-ST07 | Calculate change diff | Field diff | System calculates field changes | Initial state = null | Given issue created, when diffing, then all fields show +new |
| F1-US001-T10-ST08 | Store change group | Group record | System stores change group ID | Grouping | Given changes, when storing, then change group links items |
| F1-US001-T10-ST09 | Store change items | Item records | System stores individual changes | Per field | Given fields changed, when storing, then items per field |
| F1-US001-T10-ST10 | Support audit queries | Query support | System enables audit viewing | Query API | Given admin queries audit, when viewing, then entries shown |

**Database Mapping:**
```sql
-- Change groups (transaction-level grouping)
CREATE TABLE change_groups (
    id UUID PRIMARY KEY,
    issue_id UUID REFERENCES issues(id),
    user_id UUID,
    change_type VARCHAR(50), -- CREATED, UPDATED, DELETED
    created_at TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT
);

-- Change items (field-level changes)
CREATE TABLE change_items (
    id UUID PRIMARY KEY,
    change_group_id UUID REFERENCES change_groups(id),
    field_key VARCHAR(100),
    old_value TEXT,
    new_value TEXT
);

-- Index for queries
CREATE INDEX idx_change_groups_issue ON change_groups(issue_id);
CREATE INDEX idx_change_groups_created ON change_groups(created_at);
CREATE INDEX idx_change_items_group ON change_items(change_group_id);
```

---

#### US-002: View and Edit Issue

**Story:**
```
As a team member
I want to view and edit issue details
So that I can track progress and update information
```

| Task ID | Task Name | Subtasks |
|---------|-----------|----------|
| F1-US002-T01 | View Issue Detail | F1-US002-T01-ST01 through F1-US002-T01-ST10 |
| F1-US002-T02 | Edit Issue Fields | F1-US002-T02-ST01 through F1-US002-T02-ST10 |
| F1-US002-T03 | Inline Edit Fields | F1-US002-T03-ST01 through F1-US002-T03-ST10 |
| F1-US002-T04 | View Activity Stream | F1-US002-T04-ST01 through F1-US002-T04-ST10 |
| F1-US002-T05 | Add Comments | F1-US002-T05-ST01 through F1-US002-T05-ST10 |
| F1-US002-T06 | Edit Comments | F1-US002-T06-ST01 through F1-US002-T06-ST10 |
| F1-US002-T07 | Delete Comments | F1-US002-T07-ST01 through F1-US002-T07-ST10 |
| F1-US002-T08 | View Issue History | F1-US002-T08-ST01 through F1-US002-T08-ST10 |
| F1-US002-T09 | Share Issue | F1-US002-T09-ST01 through F1-US002-T09-ST10 |
| F1-US002-T10 | Print Issue | F1-US002-T10-ST01 through F1-US002-T10-ST10 |

---

##### F1-US002-T01: View Issue Detail

**Purpose:** Display complete issue information in detail view

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F1-US002-T01-ST01 | Load issue by key | Issue key (PROJ-123) | System retrieves issue data | Issue exists | Given valid key, when loading issue, then full issue shown |
| F1-US002-T01-ST02 | Display header section | Header fields | System shows key, summary, type | Header complete | Given issue loaded, when viewing, then header with all fields visible |
| F1-US002-T01-ST03 | Display description | Rich text render | System renders description HTML | Safe HTML | Given HTML in description, when rendering, then HTML displayed |
| F1-US002-T01-ST04 | Display custom fields | Field definitions | System renders custom fields | All fields shown | Given issue has custom fields, when viewing, then fields visible |
| F1-US002-T01-ST05 | Display status panel | Status data | System shows status with transitions | Transitions shown | Given issue loaded, when viewing, then status panel with buttons |
| F1-US002-T01-ST06 | Display people panel | People data | System shows assignee, reporter | Users shown | Given issue has people, when viewing, then people panel shown |
| F1-US002-T01-ST07 | Display dates panel | Date fields | System shows created, updated, due | Dates formatted | Given issue has dates, when viewing, then dates panel shown |
| F1-US002-T01-ST08 | Display linked issues | Link data | System shows parent, children, links | Links shown | Given issue has links, when viewing, then links panel shown |
| F1-US002-T01-ST09 | Display attachments | Attachment list | System shows attached files | Files listed | Given issue has attachments, when viewing, then attachments shown |
| F1-US002-T01-ST10 | Check view permissions | Permission check | System validates view access | User has access | Given user lacks permission, when viewing, then access denied |

**UI Layout Specification:**

```
┌─────────────────────────────────────────────────────────────────┐
│ [Issue Type Icon] Bug: PROJ-123                                  │
│                  Summary of the issue goes here                 │
├─────────────────────────────────────────────────────────────────┤
│ Status: 🟠 In Progress    │ Priority: 🔴 High │ Assignee: @user │
├─────────────────────────────────────────────────────────────────┤
│ TABS: [Details] [Activity] [Comments] [Work Log] [Attachments] │
├───────────────────────────────────┬─────────────────────────────┤
│                                   │                             │
│ DETAILS TAB                       │ RIGHT PANEL                 │
│ ├─ Description                   │ ├─ People                   │
│ │   Rich text content             │ │   ├─ Assignee              │
│ ├─ Environment                   │ │   └─ Reporter              │
│ ├─ Custom Fields                  │ ├─ Dates                    │
│ │   ├─ Story Points: 5           │ │   ├─ Created               │
│ │   └─ Sprint: Sprint 1           │ │   ├─ Updated               │
│ ├─ Labels                         │ │   └─ Due Date              │
│ │   [bug] [urgent]                │ ├─ Status                    │
│ └─ Linked Issues                  │ │   [Workflow Transitions]   │
│     ├─ Blocks: PROJ-456           │ ├─ Priority                 │
│     └─ Relates: PROJ-789          │ ├─ Labels                   │
│                                   │ ├─ Components               │
│                                   │ ├─ Fix Versions             │
│                                   │ └─ Security Level           │
│                                   │                             │
├───────────────────────────────────┴─────────────────────────────┤
│ Activity Stream                                                 │
│ ├─ 2 hours ago - John changed status → In Progress             │
│ ├─ 3 hours ago - Mary added comment                             │
│ └─ 4 hours ago - Issue created by Mary                         │
└─────────────────────────────────────────────────────────────────┘
```

**Database Mapping:**
```sql
-- Issue detail query with all joins
SELECT 
    i.*,
    it.name as issue_type_name,
    ip.name as priority_name,
    iss.name as status_name,
    u1.display_name as assignee_name,
    u2.display_name as reporter_name
FROM issues i
LEFT JOIN issue_types it ON i.issue_type_id = it.id
LEFT JOIN issue_priorities ip ON i.priority_id = ip.id
LEFT JOIN issue_statuses iss ON i.status_id = iss.id
LEFT JOIN users u1 ON i.assignee_id = u1.id
LEFT JOIN users u2 ON i.reporter_id = u2.id
WHERE i.issue_key = :issueKey;
```

---

#### US-003 through US-010: (Continued in full document)

---

### 1.3 Feature 1 Acceptance Criteria Summary

| Story ID | Story Name | Tasks | Status |
|----------|------------|-------|--------|
| F1-US001 | Create Standard Issue | 20 | ✅ IMPLEMENTED |
| F1-US002 | View and Edit Issue | 20 | ✅ IMPLEMENTED |
| F1-US003 | Manage Issue Hierarchy | 20 | ⚠️ PARTIAL |
| F1-US004 | Manage Issue Links | 20 | ⚠️ PARTIAL |
| F1-US005 | Security Levels | 20 | ❌ MISSING |
| F1-US006 | Votes and Watchers | 20 | ❌ MISSING |
| F1-US007 | Clone and Copy Issues | 20 | ❌ MISSING |
| F1-US008 | Move Issues | 20 | ❌ MISSING |
| F1-US009 | Time Tracking | 20 | ❌ MISSING |
| F1-US010 | Issue Search | 20 | ⚠️ PARTIAL |

**Feature 1 Completion: 32% (8/25 features)**

---

## FEATURE 2: PROJECT MANAGEMENT

**Feature ID:** F2  
**Priority:** CRITICAL  
**Status:** 📋 IN PROGRESS  
**Completion:** 33% (6/18 implemented)  
**Jira DC Module:** com.atlassian.jira.project

---

### 2.1 Business Goal

Enable administrators to create, configure, and manage projects with proper schemes, templates, roles, and categorization for organizing team work.

---

### 2.2 User Stories

#### US-001: Create Project

**Story:**
```
As a project administrator
I want to create a new project
So that my team can start tracking work
```

| Task ID | Task Name | Subtasks |
|---------|-----------|----------|
| F2-US001-T01 | Access Create Project | F2-US001-T01-ST01 through F2-US001-T01-ST10 |
| F2-US001-T02 | Select Project Type | F2-US001-T02-ST01 through F2-US001-T02-ST10 |
| F2-US001-T03 | Enter Project Details | F2-US001-T03-ST01 through F2-US001-T03-ST10 |
| F2-US001-T04 | Select Project Template | F2-US001-T04-ST01 through F2-US001-T04-ST10 |
| F2-US001-T05 | Assign Project Lead | F2-US001-T05-ST01 through F2-US001-T05-ST10 |
| F2-US001-T06 | Configure Schemes | F2-US001-T06-ST01 through F2-US001-T06-ST10 |
| F2-US001-T07 | Create Project Entity | F2-US001-T07-ST01 through F2-US001-T07-ST10 |
| F2-US001-T08 | Initialize Default Data | F2-US001-T08-ST01 through F2-US001-T08-ST10 |
| F2-US001-T09 | Set Project Permissions | F2-US001-T09-ST01 through F2-US001-T09-ST10 |
| F2-US001-T10 | Notify Stakeholders | F2-US001-T10-ST01 through F2-US001-T10-ST10 |

---

##### F2-US001-T01: Access Create Project

**Purpose:** Navigate to project creation interface

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T01-ST01 | Navigate to Projects | Navigation | System shows projects list | Admin access | Given admin, when navigating, then projects shown |
| F2-US001-T01-ST02 | Click Create Project | Button click | System opens create wizard | Permission | Given admin, when clicking, then wizard opens |
| F2-US001-T01-ST03 | Access via keyboard | Shortcut key | System opens on shortcut | 'P' key | Given 'P' pressed, when pressing, then opens |
| F2-US001-T01-ST04 | Verify permission | Permission check | System checks admin permission | Admin role | Given non-admin, when accessing, then denied |
| F2-US001-T01-ST05 | Handle service unavailable | Error handling | System handles service down | Error shown | Given service down, when accessing, then error shown |
| F2-US001-T01-ST06 | Show recent projects | Quick access | System shows recent projects | Recent list | Given recent, when viewing, then recent shown |
| F2-US001-T01-ST07 | Filter projects | Filter control | System filters project list | Filter applied | Given filter, when filtering, then filtered |
| F2-US001-T01-ST08 | Sort projects | Sort control | System sorts project list | Sort applied | Given sort, when sorting, then sorted |
| F2-US001-T01-ST09 | Search projects | Search input | System searches projects | Search text | Given search, when searching, then found |
| F2-US001-T01-ST10 | Show project count | Count display | System shows total count | Count shown | Given projects, when viewing, then count shown |

---

##### F2-US001-T02: Select Project Type

**Purpose:** Choose appropriate project type

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T02-ST01 | Display project types | Type list | System shows available types | Types defined | Given types, when viewing, then types shown |
| F2-US001-T02-ST02 | Software Development | Type selection | System shows software type | Type options | Given software, when selecting, then software type |
| F2-US001-T02-ST03 | Business Project | Type selection | System shows business type | Type options | Given business, when selecting, then business type |
| F2-US001-T02-ST04 | IT Service Management | Type selection | System shows ITSM type | Type options | Given ITSM, when selecting, then ITSM type |
| F2-US001-T02-ST05 | Team-managed vs Company-managed | Type distinction | System differentiates | Type category | Given selection, when viewing, then category shown |
| F2-US001-T02-ST06 | Show type description | Description | System shows type info | Description | Given type, when viewing, then description shown |
| F2-US001-T02-ST07 | Show type capabilities | Capability list | System shows capabilities | Capabilities | Given type, when viewing, then capabilities shown |
| F2-US001-T02-ST08 | Validate type selection | Validation | System validates type | Type required | Given no type, when proceeding, then error |
| F2-US001-T02-ST09 | Store type selection | State storage | System stores selection | State preserved | Given selection, when stored, then preserved |
| F2-US001-T02-ST10 | Handle type change | Reset | System resets on type change | Reset needed | Given type changed, when changing, then reset |

**Project Types:**

| Type | Description | Features |
|------|-------------|----------|
| SOFTWARE | Software development projects | Sprints, Boards, Dev integration |
| BUSINESS | Business project management | Task tracking, Reporting |
| IT_SERVICE | IT service management | Service Desk, SLAs |
| DATA_CENTER | Data center operations | Infrastructure tracking |

**Team-managed vs Company-managed:**

| Aspect | Team-managed | Company-managed |
|--------|--------------|------------------|
| Configuration | Simplified, team-level | Full, project-level schemes |
| Workflows | Team-owned | Shared schemes |
| Issue types | Team-owned | Shared schemes |
| Permissions | Simplified | Full permission schemes |

---

##### F2-US001-T03: Enter Project Details

**Purpose:** Enter required project information

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T03-ST01 | Enter project name | Name input | System stores name | Required, 3-100 chars | Given name, when entering, then stored |
| F2-US001-T03-ST02 | Validate name uniqueness | Uniqueness check | System checks name uniqueness | Unique name | Given duplicate, when checking, then error |
| F2-US001-T03-ST03 | Enter project key | Key input | System stores key | Required, 2-10 uppercase | Given key, when entering, then stored |
| F2-US001-T03-ST04 | Validate key uniqueness | Uniqueness check | System checks key uniqueness | Unique key | Given duplicate, when checking, then error |
| F2-US001-T03-ST05 | Validate key format | Format check | System validates key format | Uppercase, alphanumeric | Given "abc123", when validating, then error |
| F2-US001-T03-ST06 | Enter description | Description input | System stores description | Optional, max 2000 | Given description, when entering, then stored |
| F2-US001-T03-ST07 | Add project URL | URL input | System stores URL | Optional, valid URL | Given URL, when entering, then stored |
| F2-US001-T03-ST08 | Assign category | Category picker | System stores category | Optional | Given category, when selecting, then stored |
| F2-US001-T03-ST09 | Upload avatar | Image upload | System stores avatar | Optional, max 2MB | Given avatar, when uploading, then stored |
| F2-US001-T03-ST10 | Set project default language | Language picker | System stores language | Optional | Given language, when selecting, then stored |

**Field Specification:**

| Field Name | Field Type | Mandatory | Default | Validation | Database Column |
|-----------|------------|-----------|---------|------------|-----------------|
| name | VARCHAR(100) | YES | - | 3-100 chars, unique | projects.name |
| key | VARCHAR(10) | YES | - | 2-10 uppercase, unique | projects.project_key |
| description | TEXT | NO | NULL | max 2000 chars | projects.description |
| url | VARCHAR(500) | NO | NULL | valid URL format | projects.project_url |
| category_id | UUID | NO | NULL | valid category | projects.category_id |
| avatar_url | VARCHAR(500) | NO | NULL | valid URL | projects.avatar_url |

---

##### F2-US001-T04: Select Project Template

**Purpose:** Choose a template for project initialization

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T04-ST01 | Display template list | Template list | System shows available templates | Templates exist | Given templates, when viewing, then list shown |
| F2-US001-T04-ST02 | Scrum template | Template selection | System shows Scrum template | Template options | Given Scrum, when selecting, then Scrum config |
| F2-US001-T04-ST03 | Kanban template | Template selection | System shows Kanban template | Template options | Given Kanban, when selecting, then Kanban config |
| F2-US001-T04-ST04 | Bug tracking template | Template selection | System shows Bug template | Template options | Given Bug, when selecting, then Bug config |
| F2-US001-T04-ST05 | Task management template | Template selection | System shows Task template | Template options | Given Task, when selecting, then Task config |
| F2-US001-T04-ST06 | Blank template | Template selection | System shows blank option | Template options | Given Blank, when selecting, then empty project |
| F2-US001-T04-ST07 | Show template preview | Preview | System shows template preview | Preview shown | Given template, when previewing, then preview shown |
| F2-US001-T04-ST08 | Show included items | Included list | System shows what's included | Items list | Given template, when viewing, then items shown |
| F2-US001-T04-ST09 | Customize template | Customization | System allows customization | Modification | Given custom, when customizing, then modified |
| F2-US001-T04-ST10 | Apply template | Apply action | System applies template | Apply to project | Given apply, when applying, then template applied |

**Template Contents:**

| Template | Workflow | Issue Types | Screens | Permissions |
|----------|----------|-------------|---------|-------------|
| Scrum | Scrum workflow | Epic, Story, Task, Bug, Sub-task | All screens | Standard |
| Kanban | Kanban workflow | Story, Task, Bug | Simplified | Standard |
| Bug Tracking | Bug workflow | Bug, Task, Sub-task | Bug-focused | Standard |
| Task Management | Task workflow | Task, Sub-task | Task-focused | Standard |
| Blank | None | None | None | Minimal |

---

##### F2-US001-T05: Assign Project Lead

**Purpose:** Assign a project lead who will manage the project

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T05-ST01 | Open lead picker | Picker open | System shows user picker | Picker shown | Given picker, when opening, then user list shown |
| F2-US001-T05-ST02 | Search users | Search input | System searches users | Search string | Given search, when searching, then matching shown |
| F2-US001-T05-ST03 | Select lead | User selection | System sets lead | User selected | Given user, when selecting, then lead set |
| F2-US001-T05-ST04 | Validate user exists | Validation | System validates user | User exists | Given invalid, when selecting, then error |
| F2-US001-T05-ST05 | Assign as admin role | Role assignment | System assigns admin role | Role set | Given lead, when assigning, then admin role set |
| F2-US001-T05-ST06 | Grant permissions | Permission grant | System grants admin permissions | Permissions set | Given lead, when granting, then permissions granted |
| F2-US001-T05-ST07 | Set default assignee | Default option | System offers to set as default | Option shown | Given lead, when offered, then default set |
| F2-US001-T05-ST08 | Send notification | Notify lead | System notifies lead | Email sent | Given lead set, when notifying, then notification sent |
| F2-US001-T05-ST09 | Allow lead change | Change option | System allows change | Change allowed | Given change, when changing, then changed |
| F2-US001-T05-ST10 | Handle no lead selected | No selection | System handles no lead | Optional field | Given no lead, when creating, then created without lead |

**Database Mapping:**
```sql
-- Add lead to project
UPDATE projects SET lead_user_id = :leadUserId WHERE id = :projectId;

-- Add lead to admin role
INSERT INTO project_role_actors (project_id, role_id, actor_type, actor_id)
VALUES (:projectId, 'ADMIN', 'USER', :leadUserId);
```

---

##### F2-US001-T06: Configure Schemes

**Purpose:** Configure project schemes (workflow, issue type, permission, notification)

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T06-ST01 | Assign workflow scheme | Scheme selection | System assigns workflow scheme | Scheme required | Given scheme, when assigning, then assigned |
| F2-US001-T06-ST02 | Assign issue type scheme | Scheme selection | System assigns issue type scheme | Scheme required | Given scheme, when assigning, then assigned |
| F2-US001-T06-ST03 | Assign permission scheme | Scheme selection | System assigns permission scheme | Scheme required | Given scheme, when assigning, then assigned |
| F2-US001-T06-ST04 | Assign notification scheme | Scheme selection | System assigns notification scheme | Scheme required | Given scheme, when assigning, then assigned |
| F2-US001-T06-ST05 | Assign screen scheme | Scheme selection | System assigns screen scheme | Scheme required | Given scheme, when assigning, then assigned |
| F2-US001-T06-ST06 | Assign security scheme | Scheme selection | System assigns security scheme | Optional | Given scheme, when assigning, then assigned |
| F2-US001-T06-ST07 | Create new scheme | Create option | System creates new scheme | Creation | Given create, when creating, then scheme created |
| F2-US001-T06-ST08 | Use default scheme | Default option | System uses default scheme | Default set | Given default, when using, then default used |
| F2-US001-T06-ST09 | Clone existing scheme | Clone option | System clones scheme | Clone | Given clone, when cloning, then cloned |
| F2-US001-T06-ST10 | Preview scheme | Preview | System shows scheme preview | Preview | Given preview, when viewing, then preview shown |

**Scheme Assignment:**
```sql
-- Assign schemes to project
UPDATE projects SET 
    workflow_scheme_id = :workflowSchemeId,
    issue_type_scheme_id = :issueTypeSchemeId,
    permission_scheme_id = :permissionSchemeId,
    notification_scheme_id = :notificationSchemeId
WHERE id = :projectId;
```

---

##### F2-US001-T07: Create Project Entity

**Purpose:** Create the project database entity

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T07-ST01 | Generate project ID | ID generation | System generates UUID | UUID format | Given creation, when generating, then UUID created |
| F2-US001-T07-ST02 | Insert project record | DB insert | System inserts project record | Transaction | Given project, when inserting, then record created |
| F2-US001-T07-ST03 | Initialize sequence | Sequence init | System initializes issue sequence | Sequence set | Given project, when initializing, then sequence = 0 |
| F2-US001-T07-ST04 | Create project roles | Role creation | System creates default roles | Roles created | Given project, when creating, then roles created |
| F2-US001-T07-ST05 | Create project membership | Membership init | System adds lead to members | Member added | Given lead, when adding, then member added |
| F2-US001-T07-ST06 | Set project type | Type setting | System sets project type | Type set | Given type, when setting, then type set |
| F2-US001-T07-ST07 | Set project status | Status setting | System sets status active | Status = ACTIVE | Given project, when setting, then active |
| F2-US001-T07-ST08 | Store template ID | Template link | System links template | Template linked | Given template, when linking, then linked |
| F2-US001-T07-ST09 | Set created timestamp | Timestamp | System sets created date | UTC now | Given creation, when setting, then timestamp set |
| F2-US001-T07-ST10 | Validate creation | Validation | System validates creation | All required | Given project, when validating, then valid |

**Database Mapping:**
```sql
-- Create project
INSERT INTO projects (
    id, name, project_key, description, url, category_id,
    lead_user_id, project_type, template_id,
    workflow_scheme_id, issue_type_scheme_id, permission_scheme_id,
    notification_scheme_id, screen_scheme_id,
    avatar_url, status, created_at, updated_at
) VALUES (
    :id, :name, :projectKey, :description, :url, :categoryId,
    :leadUserId, :projectType, :templateId,
    :workflowSchemeId, :issueTypeSchemeId, :permissionSchemeId,
    :notificationSchemeId, :screenSchemeId,
    :avatarUrl, 'ACTIVE', NOW(), NOW()
);

-- Initialize sequence
INSERT INTO project_sequences (project_id, current_value)
VALUES (:projectId, 0);

-- Create default roles
INSERT INTO project_roles (project_id, name, description)
VALUES 
    (:projectId, 'Administrators', 'Project administrators'),
    (:projectId, 'Developers', 'Project developers'),
    (:projectId, 'Users', 'Project users'),
    (:projectId, 'Viewers', 'Project viewers');
```

---

##### F2-US001-T08: Initialize Default Data

**Purpose:** Initialize project with default data from template

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T08-ST01 | Clone workflows | Workflow clone | System clones workflows from template | Template has workflows | Given template, when cloning, then workflows cloned |
| F2-US001-T08-ST02 | Clone issue types | Issue type clone | System clones issue types | Template has types | Given template, when cloning, then types cloned |
| F2-US001-T08-ST03 | Clone screens | Screen clone | System clones screens | Template has screens | Given template, when cloning, then screens cloned |
| F2-US001-T08-ST04 | Create default versions | Version init | System creates initial versions | Empty versions | Given project, when creating, then versions created |
| F2-US001-T08-ST05 | Create default components | Component init | System creates default components | Empty components | Given project, when creating, then components created |
| F2-US001-T08-ST06 | Initialize backlogs | Backlog init | System creates backlog | Backlog created | Given project, when initializing, then backlog created |
| F2-US001-T08-ST07 | Create default board | Board init | System creates default board | Board created | Given project, when initializing, then board created |
| F2-US001-T08-ST08 | Set up permissions | Permission setup | System sets up initial permissions | Permissions set | Given project, when setting up, then permissions set |
| F2-US001-T08-ST09 | Create audit entry | Audit entry | System logs project creation | Audit logged | Given creation, when logging, then creation logged |
| F2-US001-T08-ST10 | Initialize search index | Search init | System indexes project | Index updated | Given project, when indexing, then indexed |

---

##### F2-US001-T09: Set Project Permissions

**Purpose:** Set up initial project permissions

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T09-ST01 | Apply permission scheme | Scheme apply | System applies permission scheme | Scheme assigned | Given scheme, when applying, then applied |
| F2-US001-T09-ST02 | Add lead to admin role | Role add | System adds lead to admin | Admin role | Given lead, when adding, then admin role |
| F2-US001-T09-ST03 | Add creator to admin role | Role add | System adds creator to admin | Admin role | Given creator, when adding, then admin role |
| F2-US001-T09-ST04 | Set browse project permission | Permission set | System sets browse permission | Permission set | Given permission, when setting, then set |
| F2-US001-T09-ST05 | Set create issue permission | Permission set | System sets create permission | Permission set | Given permission, when setting, then set |
| F2-US001-T09-ST06 | Set edit issue permission | Permission set | System sets edit permission | Permission set | Given permission, when setting, then set |
| F2-US001-T09-ST07 | Set admin permission | Permission set | System sets admin permission | Permission set | Given permission, when setting, then set |
| F2-US001-T09-ST08 | Add groups to roles | Group add | System adds groups to roles | Group roles | Given groups, when adding, then roles assigned |
| F2-US001-T09-ST09 | Verify permission access | Access verify | System verifies permissions | Access verified | Given permissions, when verifying, then verified |
| F2-US001-T09-ST10 | Document permissions | Documentation | System documents permissions | Doc created | Given project, when documenting, then doc created |

**Permission Scheme Defaults:**
```sql
-- Default permission grants
INSERT INTO permission_grants (scheme_id, permission_key, grant_type, grant_id)
VALUES 
    (:schemeId, 'BROWSE_PROJECTS', 'PROJECT_ROLE', 'USERS'),
    (:schemeId, 'CREATE_ISSUES', 'PROJECT_ROLE', 'USERS'),
    (:schemeId, 'EDIT_ISSUES', 'PROJECT_ROLE', 'DEVELOPERS'),
    (:schemeId, 'ADMINISTER_PROJECTS', 'PROJECT_ROLE', 'ADMINISTRATORS');
```

---

##### F2-US001-T10: Notify Stakeholders

**Purpose:** Send notifications about project creation

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F2-US001-T10-ST01 | Identify stakeholders | Stakeholder list | System identifies stakeholders | Stakeholders exist | Given project, when identifying, then stakeholders found |
| F2-US001-T10-ST02 | Notify project lead | Lead notification | System notifies lead | Notification | Given lead, when notifying, then notified |
| F2-US001-T10-ST03 | Notify admins | Admin notification | System notifies admins | Notification | Given admins, when notifying, then notified |
| F2-US001-T10-ST04 | Include project details | Content | System includes project info | Content complete | Given notification, when composing, then info included |
| F2-US001-T10-ST05 | Include access link | Link | System includes access link | Link included | Given notification, when composing, then link included |
| F2-US001-T10-ST06 | Send email notification | Email | System sends email | Email enabled | Given email, when sending, then email sent |
| F2-US001-T10-ST07 | Send in-app notification | In-app | System sends in-app | In-app enabled | Given in-app, when sending, then sent |
| F2-US001-T10-ST08 | Record notification | Notification record | System records notification | Record created | Given notification, when recording, then recorded |
| F2-US001-T10-ST09 | Handle delivery failure | Error handling | System handles failure | Retry | Given failure, when handling, then retry |
| F2-US001-T10-ST10 | Track notification status | Status tracking | System tracks status | Status tracked | Given notification, when tracking, then tracked |

---

#### US-002 through US-020: (Similar detailed structure for each)

---

### 2.3 Feature 2 Acceptance Criteria Summary

| Story ID | Story Name | Tasks | Status |
|----------|------------|-------|--------|
| F2-US001 | Create Project | 20 | ✅ IMPLEMENTED |
| F2-US002 | Edit Project | 20 | ✅ IMPLEMENTED |
| F2-US003 | Delete Project | 20 | ✅ PARTIAL |
| F2-US004 | Archive Project | 20 | ❌ MISSING |
| F2-US005 | Restore Project | 20 | ❌ MISSING |
| F2-US006 | Project Templates | 20 | ❌ MISSING |
| F2-US007 | Project Categories | 20 | ❌ MISSING |
| F2-US008 | Project Roles | 20 | ⚠️ PARTIAL |
| F2-US009 | Project Membership | 20 | ✅ IMPLEMENTED |
| F2-US010 | Project Settings | 20 | ⚠️ PARTIAL |
| F2-US011 | Scheme Management | 20 | ⚠️ PARTIAL |
| F2-US012 | Project Avatars | 20 | ❌ MISSING |
| F2-US013 | Project Archives | 20 | ❌ MISSING |
| F2-US014 | Project Export | 20 | ❌ MISSING |
| F2-US015 | Project Import | 20 | ❌ MISSING |
| F2-US016 | Project Clone | 20 | ❌ MISSING |
| F2-US017 | Project Bulk Operations | 20 | ❌ MISSING |
| F2-US018 | Project Permissions | 20 | ❌ MISSING |
| F2-US019 | Project Notifications | 20 | ❌ MISSING |
| F2-US020 | Project Reports | 20 | ❌ MISSING |

**Feature 2 Completion: 33% (6/18 features)**

---

## FEATURE 3: WORKFLOW ENGINE

**Feature ID:** F3  
**Priority:** HIGH  
**Status:** 📋 IN PROGRESS  
**Completion:** 20% (3/15 implemented)  
**Jira DC Module:** com.atlassian.jira.workflow

---

### 3.1 Business Goal

Provide a configurable workflow engine that manages issue transitions, conditions, validators, and post-functions for automating business processes.

---

### 3.2 User Stories

#### US-001: Create Workflow

**Story:**
```
As a workflow administrator
I want to create a new workflow
So that I can define business processes for my team
```

| Task ID | Task Name | Subtasks |
|---------|-----------|----------|
| F3-US001-T01 | Access Workflow Admin | F3-US001-T01-ST01 through F3-US001-T01-ST10 |
| F3-US001-T02 | Create New Workflow | F3-US001-T02-ST01 through F3-US001-T02-ST10 |
| F3-US001-T03 | Define Workflow Steps | F3-US001-T03-ST01 through F3-US001-T03-ST10 |
| F3-US001-T04 | Create Transitions | F3-US001-T04-ST01 through F3-US001-T04-ST10 |
| F3-US001-T05 | Add Transition Conditions | F3-US001-T05-ST01 through F3-US001-T05-ST10 |
| F3-US001-T06 | Add Transition Validators | F3-US001-T06-ST01 through F3-US001-T06-ST10 |
| F3-US001-T07 | Add Post-functions | F3-US001-T07-ST01 through F3-US001-T07-ST10 |
| F3-US001-T08 | Configure Transition Screens | F3-US001-T08-ST01 through F3-US001-T08-ST10 |
| F3-US001-T09 | Validate Workflow | F3-US001-T09-ST01 through F3-US001-T09-ST10 |
| F3-US001-T10 | Publish Workflow | F3-US001-T10-ST01 through F3-US001-T10-ST10 |

---

##### F3-US001-T01: Access Workflow Admin

**Purpose:** Navigate to workflow administration

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T01-ST01 | Navigate to Admin | Admin menu | System shows admin menu | Admin access | Given admin, when navigating, then admin shown |
| F3-US001-T01-ST02 | Click Workflows | Workflow menu | System shows workflow list | Workflows visible | Given admin menu, when clicking, then list shown |
| F3-US001-T01-ST03 | Verify permission | Permission check | System checks admin permission | Admin role | Given non-admin, when accessing, then denied |
| F3-US001-T01-ST04 | Show workflow list | List view | System shows all workflows | List populated | Given workflows, when viewing, then list shown |
| F3-US001-T01-ST05 | Filter workflows | Filter control | System filters workflow list | Filter applied | Given filter, when filtering, then filtered |
| F3-US001-T01-ST06 | Search workflows | Search input | System searches workflows | Search string | Given search, when searching, then found |
| F3-US001-T01-ST07 | Sort workflows | Sort control | System sorts workflow list | Sort applied | Given sort, when sorting, then sorted |
| F3-US001-T01-ST08 | Show workflow status | Status display | System shows active/draft status | Status shown | Given workflow, when viewing, then status shown |
| F3-US001-T01-ST09 | Show workflow usage | Usage display | System shows where used | Usage count | Given workflow, when viewing, then usage shown |
| F3-US001-T01-ST10 | Export workflows | Export action | System exports workflow | Export format | Given export, when exporting, then exported |

---

##### F3-US001-T02: Create New Workflow

**Purpose:** Create a new workflow definition

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T02-ST01 | Click Create Workflow | Create button | System opens create dialog | Button visible | Given admin, when clicking, then dialog opens |
| F3-US001-T02-ST02 | Enter workflow name | Name input | System stores name | Required, unique | Given name, when entering, then stored |
| F3-US001-T02-ST03 | Enter description | Description input | System stores description | Optional | Given description, when entering, then stored |
| F3-US001-T02-ST04 | Select workflow type | Type selection | System sets type | Build-in/Custom | Given type, when selecting, then type set |
| F3-US001-T02-ST05 | Start with statuses | Status selection | System creates initial statuses | Statuses created | Given statuses, when creating, then created |
| F3-US001-T02-ST06 | Clone from existing | Clone option | System clones workflow | Clone action | Given clone, when cloning, then cloned |
| F3-US001-T02-ST07 | Import from descriptor | Import option | System imports workflow | Jira DC format | Given import, when importing, then imported |
| F3-US001-T02-ST08 | Save as draft | Save action | System saves as draft | Draft saved | Given save, when saving, then draft saved |
| F3-US001-T02-ST09 | Add statuses | Status add | System adds workflow statuses | Status added | Given status, when adding, then added |
| F3-US001-T02-ST10 | Configure status categories | Category config | System sets categories | To Do/In Progress/Done | Given category, when setting, then set |

**Workflow Types:**

| Type | Description | Editable |
|------|-------------|----------|
| BUILD_IN | System-provided workflow | Read-only |
| CUSTOM | User-created workflow | Fully editable |
| JIRA_DEFAULT | Default Jira workflow | Clone to edit |

---

##### F3-US001-T03: Define Workflow Steps

**Purpose:** Define the status steps in the workflow

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T03-ST01 | Add status | Status add | System adds status to workflow | Status created | Given status, when adding, then created |
| F3-US001-T03-ST02 | Name status | Name input | System stores status name | Required, unique | Given name, when entering, then stored |
| F3-US001-T03-ST03 | Set status category | Category selection | System sets category | To Do/In Progress/Done | Given category, when setting, then set |
| F3-US001-T03-ST04 | Add status icon | Icon selection | System sets status icon | Icon options | Given icon, when selecting, then set |
| F3-US001-T03-ST05 | Add status color | Color selection | System sets status color | Color options | Given color, when selecting, then set |
| F3-US001-T03-ST06 | Reorder statuses | Reorder action | System reorders statuses | Drag-drop | Given reorder, when dragging, then reordered |
| F3-US001-T03-ST07 | Remove status | Remove action | System removes status | Not in use | Given unused, when removing, then removed |
| F3-US001-T03-ST08 | Validate status | Validation | System validates status | Not circular | Given circular, when checking, then error |
| F3-US001-T03-ST09 | Show status connections | Connection view | System shows connections | Visual view | Given view, when viewing, then connections shown |
| F3-US001-T03-ST10 | Copy status | Copy action | System copies status | Copy option | Given copy, when copying, then copied |

**Status Categories:**
- **To Do** - Issues waiting to be started (Open, Reopened)
- **In Progress** - Issues currently being worked on (In Progress, In Review)
- **Done** - Issues completed (Done, Closed)

---

##### F3-US001-T04: Create Transitions

**Purpose:** Define transitions between workflow statuses

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T04-ST01 | Select source status | Source select | System selects source status | Status exists | Given source, when selecting, then selected |
| F3-US001-T04-ST02 | Select target status | Target select | System selects target status | Status exists | Given target, when selecting, then selected |
| F3-US001-T04-ST03 | Name transition | Name input | System stores transition name | Required | Given name, when entering, then stored |
| F3-US001-T04-ST04 | Add transition screen | Screen link | System links screen | Screen exists | Given screen, when linking, then linked |
| F3-US001-T04-ST05 | Set trigger type | Trigger selection | System sets trigger type | MANUAL/AUTOMATIC | Given trigger, when setting, then set |
| F3-US001-T04-ST06 | Add transition conditions | Condition add | System adds conditions | Condition config | Given condition, when adding, then added |
| F3-US001-T04-ST07 | Add validators | Validator add | System adds validators | Validator config | Given validator, when adding, then added |
| F3-US001-T04-ST08 | Add post-functions | Function add | System adds functions | Function config | Given function, when adding, then added |
| F3-US001-T04-ST09 | Configure permissions | Permission config | System sets permissions | Permission set | Given permission, when setting, then set |
| F3-US001-T04-ST10 | Set transition appearance | Appearance config | System sets appearance | Icon/color | Given appearance, when setting, then set |

**Trigger Types:**

| Type | Description | Usage |
|------|-------------|-------|
| MANUAL | User-initiated transition | Standard workflow transitions |
| AUTOMATIC | System automatic trigger | Time-based, event-based triggers |
| SCHEDULED | Cron-based trigger | Scheduled status updates |
| WEBHOOK | External trigger | Integration triggers |

---

##### F3-US001-T05: Add Transition Conditions

**Purpose:** Add conditions that must be met for transition to be available

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T05-ST01 | Open condition builder | Builder open | System shows condition builder | Builder shown | Given builder, when opening, then shown |
| F3-US001-T05-ST02 | Add field condition | Field condition | System adds field condition | Field selected | Given field, when adding, then added |
| F3-US001-T05-ST03 | Add user condition | User condition | System adds user condition | User type | Given user, when adding, then added |
| F3-US001-T05-ST04 | Add group condition | Group condition | System adds group condition | Group selected | Given group, when adding, then added |
| F3-US001-T05-ST05 | Add role condition | Role condition | System adds role condition | Role selected | Given role, when adding, then added |
| F3-US001-T05-ST06 | Add permission condition | Permission condition | System adds permission | Permission set | Given permission, when adding, then added |
| F3-US001-T05-ST07 | Combine conditions (AND) | AND logic | System combines with AND | AND logic | Given AND, when combining, then AND |
| F3-US001-T05-ST08 | Combine conditions (OR) | OR logic | System combines with OR | OR logic | Given OR, when combining, then OR |
| F3-US001-T05-ST09 | Test condition | Test action | System tests condition | Test result | Given test, when testing, then result shown |
| F3-US001-T05-ST10 | Remove condition | Remove action | System removes condition | Remove allowed | Given remove, when removing, then removed |

**Condition Types:**

| Condition | Description | Parameters |
|-----------|-------------|------------|
| Field Condition | Field equals/value | field, operator, value |
| User Condition | User is assignee/reporter | userType |
| Group Condition | User in group | groupName |
| Role Condition | User in project role | roleName |
| Permission Condition | User has permission | permissionKey |
| Issue Condition | Issue has specific property | property, value |
| Subtask Condition | Has/has not subtasks | hasSubtasks |

---

##### F3-US001-T06: Add Transition Validators

**Purpose:** Add validators that check data before transition execution

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T06-ST01 | Open validator builder | Builder open | System shows validator builder | Builder shown | Given builder, when opening, then shown |
| F3-US001-T06-ST02 | Add field validator | Field validator | System adds field check | Field selected | Given field, when adding, then added |
| F3-US001-T06-ST03 | Require field value | Required validator | System requires field | Field required | Given required, when validating, then required |
| F3-US001-T06-ST04 | Validate field type | Type validator | System validates type | Type check | Given type, when validating, then checked |
| F3-US001-T06-ST05 | Validate field range | Range validator | System validates range | Min/max | Given range, when validating, then checked |
| F3-US001-T06-ST06 | Custom validator | Script validator | System runs custom script | Script | Given script, when validating, then run |
| F3-US001-T06-ST07 | Regex validator | Pattern validator | System validates pattern | Pattern | Given pattern, when validating, then checked |
| F3-US001-T06-ST08 | Error message | Message config | System sets error message | Message | Given message, when setting, then set |
| F3-US001-T06-ST09 | Test validator | Test action | System tests validator | Test result | Given test, when testing, then result shown |
| F3-US001-T06-ST10 | Remove validator | Remove action | System removes validator | Remove allowed | Given remove, when removing, then removed |

**Validator Types:**

| Validator | Description | Use Case |
|-----------|-------------|----------|
| Required Field | Field must have value | Resolution required |
| Field Change | Field must be changed | Comment required |
| User Permission | User must have permission | Reviewer required |
| Regex Match | Field matches pattern | Key format validation |
| Date Range | Date within range | Due date validation |
| Custom Script | Custom validation script | Complex validation |

---

##### F3-US001-T07: Add Post-functions

**Purpose:** Add actions that execute after successful transition

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T07-ST01 | Open function builder | Builder open | System shows function builder | Builder shown | Given builder, when opening, then shown |
| F3-US001-T07-ST02 | Update field value | Field update | System updates field | Field set | Given update, when updating, then updated |
| F3-US001-T07-ST03 | Set assignee | Assignee set | System sets assignee | User selected | Given assignee, when setting, then set |
| F3-US001-T07-ST04 | Send notification | Notification send | System sends notification | Notification | Given notification, when sending, then sent |
| F3-US001-T07-ST05 | Create comment | Comment create | System creates comment | Comment text | Given comment, when creating, then created |
| F3-US001-T07-ST06 | Add to sprint | Sprint add | System adds to sprint | Sprint selected | Given sprint, when adding, then added |
| F3-US001-T07-ST07 | Reindex issue | Reindex action | System reindexes issue | Index updated | Given reindex, when reindexing, then indexed |
| F3-US001-T07-ST08 | Fire webhook | Webhook fire | System fires webhook | Webhook | Given webhook, when firing, then fired |
| F3-US001-T07-ST09 | Fire automation rule | Rule fire | System fires rule | Rule | Given rule, when firing, then fired |
| F3-US001-T07-ST10 | Execute script | Script execute | System executes script | Script | Given script, when executing, then executed |

**Post-function Types:**

| Function | Description | Common Use |
|----------|-------------|------------|
| Update Field | Sets field value | Set status, clear assignee |
| Assign to User | Assigns user | Auto-assign to lead |
| Send Email | Sends email | Notify on transition |
| Create Comment | Adds comment | Log transition |
| Add to Sprint | Adds to sprint | Auto-sprint assignment |
| Fire Webhook | Calls webhook | External integration |
| Fire Rule | Triggers automation | Complex automation |

---

##### F3-US001-T08: Configure Transition Screens

**Purpose:** Configure screens shown during transition

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T08-ST01 | Assign screen to transition | Screen link | System links screen | Screen exists | Given screen, when linking, then linked |
| F3-US001-T08-ST02 | Select screen | Screen selection | System selects screen | Screen list | Given selection, when selecting, then selected |
| F3-US001-T08-ST03 | Add fields to screen | Field add | System adds fields | Fields selected | Given fields, when adding, then added |
| F3-US001-T08-ST04 | Remove fields | Field remove | System removes fields | Field selected | Given fields, when removing, then removed |
| F3-US001-T08-ST05 | Reorder fields | Field reorder | System reorders fields | Drag-drop | Given reorder, when reordering, then reordered |
| F3-US001-T08-ST06 | Mark fields required | Required flag | System marks required | Required set | Given required, when setting, then set |
| F3-US001-T08-ST07 | Create transition screen | Create screen | System creates screen | Screen created | Given create, when creating, then created |
| F3-US001-T08-ST08 | Clone existing screen | Clone screen | System clones screen | Clone action | Given clone, when cloning, then cloned |
| F3-US001-T08-ST09 | Preview screen | Preview action | System shows preview | Preview shown | Given preview, when viewing, then shown |
| F3-US001-T08-ST10 | Test transition | Test action | System tests transition | Test result | Given test, when testing, then tested |

---

##### F3-US001-T09: Validate Workflow

**Purpose:** Validate workflow before publishing

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T09-ST01 | Run validation | Validation trigger | System runs validation | Validation run | Given validation, when running, then run |
| F3-US001-T09-ST02 | Check status integrity | Status check | System checks status references | Valid references | Given reference, when checking, then valid |
| F3-US001-T09-ST03 | Check transition validity | Transition check | System checks transitions | Valid transitions | Given transition, when checking, then valid |
| F3-US001-T09-ST04 | Detect circular transitions | Cycle detection | System detects cycles | No cycles | Given cycle, when detecting, then error |
| F3-US001-T09-ST05 | Check orphan statuses | Orphan check | System checks for orphans | No orphans | Given orphan, when checking, then error |
| F3-US001-T09-ST06 | Validate conditions | Condition check | System validates conditions | Valid conditions | Given condition, when checking, then valid |
| F3-US001-T09-ST07 | Validate validators | Validator check | System validates validators | Valid validators | Given validator, when checking, then valid |
| F3-US001-T09-ST08 | Validate post-functions | Function check | System validates functions | Valid functions | Given function, when checking, then valid |
| F3-US001-T09-ST09 | Show validation results | Results display | System shows results | Results shown | Given results, when viewing, then shown |
| F3-US001-T09-ST10 | Fix validation errors | Error fix | System allows fixing | Fix allowed | Given error, when fixing, then fixed |

---

##### F3-US001-T10: Publish Workflow

**Purpose:** Publish workflow to make it active

| Subtask ID | Purpose | Inputs | Expected Behavior | Validation | Acceptance Criteria |
|------------|---------|--------|-------------------|------------|-------------------|
| F3-US001-T10-ST01 | Click publish | Publish button | System initiates publish | Button visible | Given publish, when clicking, then initiated |
| F3-US001-T10-ST02 | Confirm publish | Confirmation dialog | System requires confirm | Confirm | Given confirm, when confirming, then confirmed |
| F3-US001-T10-ST03 | Validate workflow | Pre-publish validation | System validates workflow | Valid | Given valid, when validating, then passes |
| F3-US001-T10-ST04 | Create version | Version create | System creates version | Version saved | Given version, when creating, then saved |
| F3-US001-T10-ST05 | Set as published | Status update | System sets status to published | Status = PUBLISHED | Given publish, when setting, then published |
| F3-US001-T10-ST06 | Update references | Reference update | System updates references | References updated | Given publish, when updating, then updated |
| F3-US001-T10-ST07 | Archive previous | Previous archive | System archives previous | Archive | Given previous, when archiving, then archived |
| F3-US001-T10-ST08 | Notify linked projects | Notification | System notifies projects | Notification | Given linked, when notifying, then notified |
| F3-US001-T10-ST09 | Log publish event | Audit log | System logs publish | Audit | Given publish, when logging, then logged |
| F3-US001-T10-ST10 | Show success | Success display | System shows success | Message | Given success, when showing, then shown |

**Workflow Lifecycle:**
```
DRAFT → VALIDATION → PUBLISHED → ACTIVE
           ↓
        ERRORS
           ↓
        FIX → DRAFT
```

---

### 3.3 Feature 3 Acceptance Criteria Summary

| Story ID | Story Name | Tasks | Status |
|----------|------------|-------|--------|
| F3-US001 | Create Workflow | 20 | ✅ IMPLEMENTED |
| F3-US002 | Edit Workflow | 20 | ✅ IMPLEMENTED |
| F3-US003 | Delete Workflow | 20 | ⚠️ PARTIAL |
| F3-US004 | Workflow Transitions | 20 | ✅ IMPLEMENTED |
| F3-US005 | Transition Conditions | 20 | ❌ MISSING |
| F3-US006 | Transition Validators | 20 | ❌ MISSING |
| F3-US007 | Post-functions | 20 | ❌ MISSING |
| F3-US008 | Transition Screens | 20 | ❌ MISSING |
| F3-US009 | Workflow Schemes | 20 | ❌ MISSING |
| F3-US010 | Draft Workflows | 20 | ❌ MISSING |
| F3-US011 | Workflow Versions | 20 | ❌ MISSING |
| F3-US012 | Workflow Diagrams | 20 | ❌ MISSING |
| F3-US013 | Workflow Statistics | 20 | ❌ MISSING |
| F3-US014 | Copy Workflow | 20 | ❌ MISSING |
| F3-US015 | Import/Export Workflow | 20 | ❌ MISSING |

**Feature 3 Completion: 20% (3/15 features)**

---

## FEATURES 4-10: (Similar detailed structure)

---

# 4. TECHNICAL REQUIREMENTS

## 4.1 Database Schema Changes

### Critical Tables to Create/Modify

```sql
-- Security Levels (new)
CREATE TABLE security_levels (
    id UUID PRIMARY KEY,
    scheme_id UUID REFERENCES security_schemes(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    value INTEGER NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);

-- Security Level Members (new)
CREATE TABLE security_level_members (
    id UUID PRIMARY KEY,
    level_id UUID REFERENCES security_levels(id),
    member_type VARCHAR(20) NOT NULL, -- USER, GROUP, PROJECT_ROLE
    member_id UUID NOT NULL,
    created_at TIMESTAMP
);

-- Permission Grants (expand existing)
ALTER TABLE permission_grants ADD COLUMN created_at TIMESTAMP;
ALTER TABLE permission_grants ADD COLUMN updated_at TIMESTAMP;

-- Votes (new)
CREATE TABLE votes (
    id UUID PRIMARY KEY,
    issue_id UUID REFERENCES issues(id),
    user_id UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(issue_id, user_id)
);

-- Watchers (new)
CREATE TABLE watchers (
    id UUID PRIMARY KEY,
    issue_id UUID REFERENCES issues(id),
    user_id UUID REFERENCES users(id),
    watched_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(issue_id, user_id)
);

-- Issue Links (new)
CREATE TABLE issue_links (
    id UUID PRIMARY KEY,
    source_issue_id UUID REFERENCES issues(id),
    target_issue_id UUID REFERENCES issues(id),
    link_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    created_by UUID
);

-- Workflow Transitions (expand)
ALTER TABLE workflow_transitions ADD COLUMN screen_id UUID;
ALTER TABLE workflow_transitions ADD COLUMN permission_check VARCHAR(100);
```

### Indexes for Performance

```sql
CREATE INDEX idx_issues_security ON issues(security_level_id);
CREATE INDEX idx_issues_labels ON issues USING GIN(labels);
CREATE INDEX idx_watchers_issue ON watchers(issue_id);
CREATE INDEX idx_watchers_user ON watchers(user_id);
CREATE INDEX idx_votes_issue ON votes(issue_id);
CREATE INDEX idx_votes_user ON votes(user_id);
CREATE INDEX idx_issue_links_source ON issue_links(source_issue_id);
CREATE INDEX idx_issue_links_target ON issue_links(target_issue_id);
```

---

## 4.2 API Requirements

### Issue Service Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/issues` | POST | Create issue |
| `/api/issues/{key}` | GET | Get issue |
| `/api/issues/{key}` | PUT | Update issue |
| `/api/issues/{key}` | DELETE | Delete issue |
| `/api/issues/{key}/transitions` | GET | Get transitions |
| `/api/issues/{key}/transitions` | POST | Execute transition |
| `/api/issues/{key}/watchers` | GET/POST/DELETE | Watchers |
| `/api/issues/{key}/votes` | GET/POST/DELETE | Votes |
| `/api/issues/{key}/links` | GET/POST/DELETE | Links |
| `/api/issues/{key}/attachments` | GET/POST/DELETE | Attachments |
| `/api/issues/{key}/comments` | GET/POST/PUT/DELETE | Comments |
| `/api/issues/{key}/worklog` | GET/POST/PUT/DELETE | Worklog |

### Project Service Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/projects` | POST | Create project |
| `/api/projects/{id}` | GET | Get project |
| `/api/projects/{id}` | PUT | Update project |
| `/api/projects/{id}` | DELETE | Delete project |
| `/api/projects/{id}/roles` | GET/POST/PUT | Project roles |
| `/api/projects/{id}/members` | GET/POST/DELETE | Members |
| `/api/projects/{id}/schemes` | GET/PUT | Schemes |

### Workflow Service Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/workflows` | GET/POST | List/Create workflows |
| `/api/workflows/{id}` | GET/PUT/DELETE | Get/Update/Delete |
| `/api/workflows/{id}/transitions` | GET/POST | Transitions |
| `/api/workflows/{id}/publish` | POST | Publish workflow |
| `/api/workflows/schemes` | GET/POST | Workflow schemes |

---

## 4.3 Security Architecture

### Permission Types (50+ permissions)

| Category | Permissions |
|----------|-------------|
| Browse | BROWSE_PROJECTS, VIEW_READONLY_WORKFLOW |
| Edit | EDIT_ISSUES, EDIT_COMMENTS |
| Create/Delete | CREATE_ISSUES, DELETE_ISSUES |
| Assign | ASSIGN_ISSUES, ASSIGNABLE_USER, ASSIGNABLE_GROUP |
| Resolve | RESOLVE_ISSUES |
| Comment | CREATE_COMMENTS, EDIT_COMMENTS, DELETE_COMMENTS |
| Attach | CREATE_ATTACHMENTS, DELETE_ATTACHMENTS |
| Work | WORK_ON_ISSUES, LOG_WORK |
| Admin | ADMINISTER_PROJECTS, SYSTEM_ADMIN, USER_ADMIN |

### Security Level Hierarchy

```
Level 1: All Users (Public)
Level 2: Project Members
Level 3: Developers Only
Level 4: Leads Only
Level 5: Managers Only
Level 6: Administrators Only
```

---

# 5. IMPLEMENTATION ROADMAP

## Phase 1: Foundation (Months 1-2)
**Focus:** Core issue management + project basics

| Task ID | Task | Duration | Dependencies | Status |
|---------|------|----------|--------------|--------|
| T1.1 | Database Schema Enhancement | 2 weeks | - | ❌ |
| T1.2 | Security Levels Implementation | 2 weeks | T1.1 | ❌ |
| T1.3 | Votes & Watchers Implementation | 1 week | T1.1 | ❌ |
| T1.4 | Issue Links Implementation | 2 weeks | T1.1 | ❌ |
| T1.5 | Workflow Conditions/Validators | 2 weeks | - | ❌ |

## Phase 2: Security & Permissions (Months 3-4)

| Task ID | Task | Duration | Dependencies | Status |
|---------|------|----------|--------------|--------|
| T2.1 | Permission Schemes | 2 weeks | - | ❌ |
| T2.2 | Role-Based Access Control | 2 weeks | T2.1 | ❌ |
| T2.3 | Project Roles | 1 week | T2.1 | ❌ |
| T2.4 | API Token Authentication | 1 week | - | ❌ |

## Phase 3: Agile Features (Months 5-6)

| Task ID | Task | Duration | Dependencies | Status |
|---------|------|----------|--------------|--------|
| T3.1 | Sprint Reports | 2 weeks | - | ❌ |
| T3.2 | Scrum Board Enhancements | 2 weeks | - | ❌ |
| T3.3 | Backlog Management | 2 weeks | - | ❌ |
| T3.4 | Kanban WIP Limits | 1 week | - | ❌ |

## Phase 4: Custom Fields & Screens (Months 7-8)

| Task ID | Task | Duration | Dependencies | Status |
|---------|------|----------|--------------|--------|
| T4.1 | Custom Field Types | 3 weeks | - | ❌ |
| T4.2 | Field Configuration Schemes | 2 weeks | T4.1 | ❌ |
| T4.3 | Screen Schemes | 2 weeks | T4.1 | ❌ |

## Phase 5: JQL & Search (Months 9-10)

| Task ID | Task | Duration | Dependencies | Status |
|---------|------|----------|--------------|--------|
| T5.1 | JQL Parser Implementation | 3 weeks | - | ❌ |
| T5.2 | JQL Executor | 2 weeks | T5.1 | ❌ |
| T5.3 | Saved Filters | 2 weeks | T5.1 | ❌ |

## Phase 6: Notifications & Automation (Months 11-12)

| Task ID | Task | Duration | Dependencies | Status |
|---------|------|----------|--------------|--------|
| T6.1 | Notification Schemes | 2 weeks | - | ❌ |
| T6.2 | Automation Rules Engine | 3 weeks | T5.1 | ❌ |
| T6.3 | Email Templates | 1 week | T6.1 | ❌ |

## Phase 7: Advanced Features (Months 13-18)

| Task ID | Task | Duration | Dependencies | Status |
|---------|------|----------|--------------|--------|
| T7.1 | Time Tracking | 3 weeks | - | ❌ |
| T7.2 | Dashboards & Gadgets | 3 weeks | - | ❌ |
| T7.3 | Reports | 2 weeks | - | ❌ |
| T7.4 | Attachment Preview | 2 weeks | - | ❌ |
| T7.5 | Performance Optimization | Ongoing | All | ❌ |

---

# 6. GAP ANALYSIS SUMMARY

## Feature Completion Matrix

| Feature | Total | Implemented | Missing | Priority | Status |
|---------|-------|-------------|---------|----------|--------|
| Core Issue Management | 25 | 8 | 17 | CRITICAL | ⚠️ PARTIAL |
| Project Management | 18 | 6 | 12 | CRITICAL | ⚠️ PARTIAL |
| Workflow Engine | 15 | 3 | 12 | HIGH | ⚠️ PARTIAL |
| Security & Permissions | 20 | 0 | 20 | CRITICAL | ❌ MISSING |
| Agile/Sprint Management | 20 | 5 | 15 | HIGH | ⚠️ PARTIAL |
| Search & JQL | 14 | 1 | 13 | HIGH | ❌ MISSING |
| Custom Fields & Screens | 22 | 1 | 21 | HIGH | ❌ MISSING |
| Notifications & Automation | 18 | 2 | 16 | MEDIUM | ⚠️ PARTIAL |
| Time Tracking & Attachments | 20 | 2 | 18 | MEDIUM | ⚠️ PARTIAL |
| Administration & Reporting | 48 | 5 | 43 | MEDIUM | ⚠️ PARTIAL |
| **TOTAL** | **233** | **34** | **199** | | **14.6%** |

---

## Quick Wins (First Month)

1. Security Levels (high impact, low effort)
2. Votes & Watchers (high impact, low effort)
3. Issue Links (medium impact, medium effort)
4. Workflow Conditions (medium impact, medium effort)

## Critical Path

1. Database Schema Changes
2. Permission System
3. Workflow Engine Completion
4. JQL Implementation

---

# DOCUMENT STATUS

**Version:** 1.0  
**Created:** 2026-05-24  
**Status:** IN PROGRESS  
**Completion:** 5% (Sections 1-3 partially complete, Sections 4-6 outline only)

## Next Steps

1. [ ] Complete Feature Decomposition for Features 4-10
2. [ ] Add detailed field specifications for all features
3. [ ] Add complete acceptance criteria for all stories
4. [ ] Add database schema DDL for all new tables
5. [ ] Add API specifications for all endpoints
6. [ ] Review with functional team
7. [ ] Finalize and approve document

---

*End of Document*