# Aircraft Design System: SYSDOPS Domain Analysis & Implementation Roadmap

> **Role:** Lead Aerospace Systems Architect & Product Strategy Principal
> **Date:** 2026-07-24
> **Source Documents Indexed:** 8 files (5 PPTX from SYSDOPS DO/LAB Template, 1 PPTX from Archive, 1 DOCX traceability matrix, 1 training video noted)
> **Domain:** Aircraft Manufacturing & Design Lifecycle -- Design Office through Laboratory & RIG Testing

---

## File Index & Confirmation

| # | File | Type | Key Content |
|---|------|------|-------------|
| 1 | SYSDOPS Deliverable Template (1YA-1YD-1YC).pptx | PPTX (32 slides) | Jira project configuration for ESG System Development -- deliverable fields, workflows, risk/review tabs |
| 2 | SYSDOPS DO and LAB Template (V&V Template).pptx | PPTX (141 slides) | **Core reference** -- Full V&V template: HLVVO/VVO lifecycle, XRAY integration, Test/Pre-condition/Test Plan/Test Execution, Change Cards, DCL sync, campaign automation, DOORS export/import, Defect management (TechEvent/Bench Defect/Problem Report), Big Picture roadmap, XPorter reports |
| 3 | SYSDOPS Defect - practical user guide.pptx | PPTX (59 slides) | Defect management user guide -- TechEvent/Bench Defect/Problem Report creation, field attribute tables with LTM matching, workflow transitions, supplier synchronization, Diff History V3 |
| 4 | SYSDOPS Lab System Testing - practical user guide V2.pptx | PPTX (59 slides) | Lab testing user guide -- VVO search, test procedure writing, XRAY test repository, Test/VVO workflows, Test Execution, campaign management, dashboard configuration |
| 5 | SYSDOPS Lab System Testing - practical user guide V1.pptx | PPTX (34 slides) | Earlier version of V2 (subset -- no campaign management or dashboard sections) |
| 6 | Presentation DEVOPS sysdops.pptx | PPTX (14 slides) | DevOps toolchain overview -- Jira, GitHub, Confluence, Jenkins CI, Artifactory artifact management |
| 7 | VVOs per cluster from Test Plan (NFMSLST-5750).docx | DOCX | Requirement Traceability Matrix -- VVOs across 7 clusters (DIR TO, HOLD, LAT FPLN ALTN/INIT/STRINGING, OFFSET, VERT FLIGHT PHASE) with linked tests, defects, and test executions |
| 8 | Training on Defects in Jira (MP4) | Video | Noted; content inferred from Defect user guide PPTX |
| 9 | Archive MP4s (nFMS: Action, Deliverable, DI, Epic; FWS: bug) | Video | Training demos for nFMS issue types and FWS bug workflow |

---

## Phase 1: Deep Domain & Workflow Extraction

### 1.1 Core Lifecycle Map

The documents describe a **four-stage system V&V lifecycle** managed through Jira SYSDOPS with XRAY plugin integration:

```
Design Office (DO)                    Laboratory (LAB)
+-----------------------+            +--------------------------+
| Requirement Definition|            | Test Procedure Writing    |
| (VVO/HLVVO authoring) |  TRANSFER  | (from VVO requirements)  |
| Specification Mgmt    | ---------> | Test Campaign Creation    |
| (DCL, DI, Change Card)|            | (from LTR/CSV)           |
| DOORS Integration     |            | Test Execution & Runs     |
| Baseline Management   |            | Reporting (XRAY/XPorter) |
+-----------------------+            +--------------------------+
         |                                      |
         |  DEFECT PROJECT (Shared)              |
         |  +----------------------------------+ |
         +->| TechEvent (System anomalies)     |<+
            | Bench Defect (Test mean issues)   |
            | Problem Report (Certification)   |
            +----------------------------------+
                         |
              +----------v-----------+
              | SUPPLIER PROJECT      |
              | (Synchronized fields, |
              |  DCL exchange,        |
              |  TechEvent sharing)   |
              +----------------------+
```

**[Directly from documents]** SYSDOPS manages four generic XRAY object types:
1. **Requirement** (VVO, IVV) -- Input points to be covered by test
2. **Test** -- Procedures with ordered steps, actions, and expected results
3. **Change** (Design Item, Change Card) -- Design update tracking
4. **Defect** (TechEvent, Bench Defect) -- Anomaly reporting and tracking

### 1.2 Design Office (DO) Workflows

#### 1.2.1 VVO (Verification & Validation Objective) Lifecycle

**[Directly from documents]** VVOs are the central requirement artifact. The DO defines functionality to cover; the LAB team uses VVOs to write test procedures.

**VVO Workflow States (DO Project):**
```
New --> To be verified --> Verified --> Released --> [Superseded | Cancelled]
                                          |
                                     Plan Rework (new lifecycle)
```

- **New:** VVO just created
- **To be verified:** Content must be reviewed
- **Verified:** Content reviewed OK, not yet in a baseline
- **Released:** VVO included in a baseline, part of next baselines
- **Cancelled:** VVO descoped (included in baseline for DOORS update only)
- **Superseded:** Replaced by a new version (automatic on clone transition)

**Key Rules:**
- VVO edition impossible once in Verified/Released/Superseded/Cancelled status
- Only project admins can perform transitions and change Fix Version
- On transition to Verified/Released, all linked cloned issues automatically move to Superseded

#### 1.2.2 VVO Baselining Process (4 Steps)

**[Directly from documents]** A baseline is a snapshot of official VVOs at a point in time. The process follows four steps:

1. **Step 0:** All VVOs Released in current baseline
2. **Step 1:** Clone VVOs for new versions; cancel removed VVOs; create new VVOs
3. **Step 2:** Review -- Verified VVOs pass review; old versions auto-Superseded
4. **Step 3:** Create baseline (bulk update Fix Version for all Released/Verified/Cancelled)
5. **Step 4:** Publish baseline (bulk transition Verified to Released)

**6 Use Cases Documented:**
- VVO updated and ready for new baseline
- VVO unchanged (carried forward)
- VVO removed (Cancelled)
- New VVO created and ready
- VVO updated but NOT ready (remains in progress)
- New VVO created but NOT ready

#### 1.2.3 HLVVO (High Level V&V Objectives)

**[Directly from documents]** HLVVO groups several VVOs and manages the review process:

**Workflow:** New --> Plan --> VVO Writing in progress --> Supplier in Review --> Authorize
- On "Authorize" transition: all linked VVOs (type "is parent of") transition to Verified
- On "Supplier in Review" transition: VVOs in status "To be verified" with Usage "Formal verification" or "Non Regression" transition to Verified
- Only project administrators can transition

#### 1.2.4 DOORS Integration

**[Directly from documents]** All VVOs must be referenced in DOORS for Airbus process compliance:

- **Export:** Search Released/Cancelled VVOs by Fix Version, export via XPorter template "VVO export for Doors" (CSV format)
- **Import ID Doors:** Use Activity issue type with CSV attachment, press "Import Doors ID in VVO" button
- **Validation rules on import:** Empty ID Doors, duplicate IDs, mismatched IDs, wrong project references, invalid headers

#### 1.2.5 Design Change Management

**[Directly from documents]** Changes follow a hierarchical structure:

**DCL (Design Clarification Log):** Discussions about specification evolutions between suppliers and DO
- Shared with suppliers (limited fields visible)
- Bidirectional synchronization (Action Responsible, Requested By, Labels, Abstract)
- "Copy to supplier project" / "Copy to Airbus project" buttons

**Design Item (DI):** High-level design evolution
- Created when DCL evolutions must be implemented
- Can be shared with suppliers via field copy with "duplicates" link

**Change Card:** Detailed system change tracking with extensive field taxonomy:

| Tab | Key Fields |
|-----|-----------|
| Design | Change Type (Anomaly/Evolution), Component/s, Impact, Risk Description, Priority, Function (26 avionics functions), Applicability, BigPictureTeam |
| EIF | EIF Function (30+ engine functions), ICD/Code/SCADE Impact (TBC/Yes/No) |
| Planning | Start/End date, Story points, Time Tracking, Task Progress |
| Review | Quality Control Status, Code Generation Status, Design Review Status (Green/Amber/Red) |
| Certification | Classification (Type 0/1A/1B/2/3/Significant/Functional/Process/Life-cycle), Current Behavior, Change Rationale, Change Description |
| Maturity Test | Maturity Test (Yes/No), Priority (P1-P3), Objective |
| Safety | Safety Team Review Required, Safety Design Analysis, Safety Review Status, Safety Review Comment |

**Change Card Workflow:** In Analysis --> In Progress --> [Closed | No Change | Temporary Acceptance]
- "Resolved by" auto-set on transition to final status

### 1.3 Laboratory (LAB) Workflows

#### 1.3.1 VVO Transfer (DO to LAB)

**[Directly from documents]** VVOs transfer from DO to LAB project via dashboard gadget:
- Uses "ID Doors" as identifier for copy/update
- Available to users with Administrator/Contributor rights
- Can filter by version, component, execution responsible
- Option to send preview email without doing transfer
- Transferred VVOs are read-only in LAB project

**LAB VVO Workflow:**
```
New --> Covered --> [Update | Cancelled | To be corrected]
```
- **New:** Procedures not written yet
- **Covered:** All procedures written and linked
- **Update:** Automatically set when VVO modified by DO transfer (triggers impact analysis)
- **Cancelled:** VVO not applicable (not delivered by DO)
- **To be corrected:** VVO needs correction (agreed between DO and Lab)

#### 1.3.2 Test Management

**Test Workflow:**
```
Draft --> Internal Review --> External Review --> Approved --> [Script Error | Update]
```
- **Draft:** Procedure writing in progress
- **Internal Review:** Lab internal review
- **External Review:** Sent to DO for proofreading; field "Original Estimate" mandatory
- **Approved:** All DO comments addressed
- **Update:** Auto-set when linked VVO transitions to Update status
- **Script Error:** Script error identified, correction needed

**Test Fields (3 tabs):**
- General: Summary, Component/s, Applicability, Supplier Applicability, Necessary real systems, Test Mean Mini (Virtual/Real), VVO linking helpers (Component/Applicability/Supplier filters)
- Test Details: Test Type (Manual/Cucumber/Generic), Manual Test Steps (Action + Expected Result + VVO per step)
- Associations: Test Sets, Pre-Conditions, Test Plans

**VVO-to-Test Linking:**
- From VVO view: "Add Tests" (new or existing)
- From Test view: Edit with VVO filter helpers
- Auto-population: Component, Applicability, Supplier Applicability, Necessary real systems auto-filled from VVO if empty in test
- Consistency checks: Error messages when Component/Applicability/Supplier Applicability values differ between linked Test and VVO

#### 1.3.3 Test Campaign Automation

**[Directly from documents]** Campaign automation from CSV/LTR files:

**Process:**
1. DO exports VVO list from Test Request via XPorter template (columns: ID Doors, Summary, Applicability, Version, Fix Version, Priority)
2. User selects applicabilities in GSheet
3. CSV attached to Test Plan in LAB project
4. "Create Campaign" button automates:
   - For each VVO line in CSV, creates Test Execution for each linked test
   - Only if test in status "APPROVED"
   - Only if no existing Test Execution for the {test, applicability} pair in the Test Plan
   - Only if test "Supplier Applicability" contains Test Plan "Supplier Applicability"
   - Sets fields: test environment (from CSV applicability), Fix Version (from Test Plan), Component (from test), Supplier Applicability (from Test Plan), Original Estimate (from test), Priority (highest from linked VVOs)
   - Associates VVOs to Test Plan with "Relates" link
   - Email sent with detailed log (VVO_ID_Doors, Jira IDs, statuses, actions, InTestPlan)

#### 1.3.4 Test Execution & Runs

**Test Execution Workflow:** Created --> In Progress --> [Done | Aborted]

**Test Step Statuses:**
| Status | Description | Impact |
|--------|-------------|--------|
| TODO | Not started | -- |
| EXECUTING | Currently executing | -- |
| PASS | Step passed | VVO coverage OK |
| FAIL | Step failed | Links to defect |
| NOT-TESTABLE | Cannot be passed | VVO status = Not Run |
| NOT-REQUIRED | Not needed for this run | Counted as PASS for VVO coverage |

**Key Field: LabSheet Number** -- Mandatory; execution status won't reach final status without it.

### 1.4 Defect Management (DEFECT Project)

**[Directly from documents]** Single shared DEFECT project for all technical events to:
- Avoid copying issues between projects
- Avoid sharing projects with too many people
- Easily follow defects from multiple projects

#### 1.4.1 TechEvent (System Anomaly per Airbus Method M1668)

**Workflow (12 states, 7 transitions):**
```
Open --> Under Originator Analysis --> Under Resolver Analysis --> Classified
    |         |                              |                        |
    |    Under Test Mean Analysis      Ready for Review          To be Assessed
    |                                                                |
    |                                                  Resolved-Corrected / Resolved-Contained
    |                                                                |
    +--- Proposed for Cancellation --> Cancelled                   Closed
    +--- To be Refined (needs clarification)
```

**Key Transition Rules:**
- Field cascading dependencies: Detected on Test Means depends on Detected on A_C Program; Impacted AC System depends on Detected on A_C Program; etc.
- **Supplier Analysis** button (Under Resolver Analysis): Creates TechEvent in supplier project, synchronizes fields bidirectionally
- **Share Attachments** button: Updates shared attachments in supplier project
- **Create Change** button (Classified): Creates Change Card or Change Candidate in target project
- **Link Problem Report** button: Creates/links Problem Report with auto-filled fields

**TechEvent-to-LTM Field Matching (documented):**
| Jira Attribute | LTM Match |
|---------------|-----------|
| Summary | Title |
| Affects Version/s | Standard |
| Applicable to A_C Program | Applicability |
| Defect Impact | Impact |
| Defect Origin | Origin |
| Reporter | Author |
| Reporter Team | Test Team |
| Priority | Priority |
| Recording Reference | Recording Reference |

#### 1.4.2 Bench Defect (Test Means Anomaly)

**Workflow:** Same mechanism as TechEvent; created only in DEFECT project
- On transition: if Bench Defect came from TechEvent, TechEvent is reopened/closed accordingly
- **Severity levels:** Blocking, High, Low
- **Criticality** (only when Severity=High): P0, P1, P2, P3
- **Test Mean Defect Origin:** Cascading list with categories (Architecture, Facilities, Hydraulic, Instrumentation & Tools [22 sub-items], Mechanic, Simulation [8 sub-items], Wiring, Work Request)

#### 1.4.3 Problem Report (Certification-Facing)

**[Directly from documents]** Only open Problem Reports are transmitted to certification authorities.

**Origins:** Design Review, Safety Review, V&V Activity
**PR Types:** Significant CAT/HAZ, Significant MAJ, Functional, Functional "internal", Process, Life-cycle data

**Workflow:** Open --> Under Analysis --> [Closed | Rejected]

### 1.5 Reporting & Dashboards

**[Directly from documents]** XRAY reports available:

| Report | Content | Formats |
|--------|---------|---------|
| Traceability Report | Test executions with associated tests and VVOs | -- |
| Test Plans Report | Test plans with status counts, defects, progress | -- |
| Test Executions Report | Test executions with status by type, progress, defects | -- |
| Test Runs Report | Detailed test runs with dates and defects | -- |
| VVOs coverage from Test Plan | Requirements by cluster with test status | DOCX, PDF |
| Light VVOs coverage from Test Plan | Simplified version of above | DOCX, PDF |
| XLS VVOs coverage from Test Plan | Spreadsheet format | XLSX, CSV, PDF |
| TechEvent List from Test Plan | Defects covered by test plan | XLSX, CSV, PDF |
| Test Runs Detailed from Test Execution | Detailed test runs by cluster | DOCX, PDF |
| Test Execution KO | Failed test executions | XLSX, CSV, PDF |
| Export for Planning | Estimated duration by {Component, Test Means, Priority} | -- |

**Dashboard Configuration (Tech Leads):**
- "Custom Charts Simple Search" gadget with custom JQL toggles
- Filter by Test Plan, Component, Test Environment
- Click-through to search results page

### 1.6 Cross-Cutting Features

#### Diff History V3
**[Directly from documents]** Button generates HTML file attached to issue showing all field changes with colored highlighting:
- **Evolution mode:** Shows all changes between two dates
- **Difference mode:** Shows only net differences between versions at two dates
- V4 planned improvement: Manual Test Steps field support

#### Big Picture View
**[Directly from documents]** Test campaign roadmap visualization in Jira, using BigPictureTeam field for team assignment and synchronization.

#### DevOps Toolchain
**[Directly from documents]** SYSDOPS integrates with:
- **Jira:** Planning and issue tracking
- **GitHub:** Version control
- **Confluence:** Documentation
- **Jenkins:** CI/CD (build, test, deploy automation)
- **Artifactory:** Binary artifact management and versioning

### 1.7 Domain Nuances & Inferences

**[Inferred from aerospace domain knowledge, not directly stated in documents:]**

1. **Regulatory Framework:** The Classification field on Change Cards (Type 0, 1A, 1B, 2, 3, Significant CAT/HAZ, Significant MAJ) maps to EASA Part 21 change classification. Problem Reports being the only artifacts transmitted to certification authorities aligns with FAR/CS-25 compliance requirements.

2. **V-Model Alignment:** The DO-to-LAB transfer with VVO baselining implements the left side (requirements decomposition) to right side (verification) trace of the systems engineering V-model per ARP4754A.

3. **ICD Change Propagation:** The ICD CNTRL Impact and ICD BITE Impact fields on Change Cards indicate tracking of Interface Control Document changes, critical for multi-supplier avionics integration where interface changes can cascade across systems.

4. **Test Mean Types:** SIB (System Integration Bench), FIB (Full Integration Bench), SIMULATOR map to standard aerospace rig levels for progressive integration testing.

5. **LTR/LTRA Process:** Lab Test Request (LTR) and Lab Test Results Analysis (LTRA) are standard Airbus documents in the system verification process. The XPorter report generation for "VVOs coverage from Test Plan" directly supports LTRA sub-chapter generation.

6. **Multi-System Function (MSF):** The MSF field on TechEvents tracks cross-system anomalies critical in modern integrated avionics architectures where a single fault can propagate across multiple ATA chapters.

---

## Phase 2: Feature Matrix (Current vs. Missing vs. Business Rationale)

| # | Workflow Step / Domain Capability | Current Tool Status | Missing Nuance / Gap | Business Value & Rationale | Priority |
|---|-----------------------------------|--------------------|-----------------------|---------------------------|----------|
| 1 | **VVO Requirement Authoring** | Partially implemented (issue types exist) | No dedicated VVO issue type with full field taxonomy (Execution Responsible, Execution Delegation, VVO Usage, VVO Scope, Test Mean Type Requested, Operational Conditions, Expected Results, Real System Needed, Milestone/Target, Applicability multi-select, Supplier Applicability checkboxes, Associated Requirements, ID Doors, Version field with auto-increment on clone) | Prevents requirement traceability gaps during FAR/CS-25 certification audits; DO teams cannot define verification scope without VVO Usage and Scope fields | **MUST-HAVE** |
| 2 | **HLVVO Grouping & Review Process** | Not implemented | No parent-child VVO hierarchy, no review workflow with proofreading table grid, no auto-transition of child VVOs on HLVVO state changes | Blocks structured review process; without HLVVO, large verification campaigns cannot be organized or reviewed at the specification-chapter level | **MUST-HAVE** |
| 3 | **VVO Baselining (4-step process)** | Not implemented | No clone-with-auto-actions (clear Fix Version, clear HLVVO link, auto-increment Version), no bulk transition to Released, no Superseded auto-transition, no baseline tagging via Fix Version | Without baselining, there is no configuration-controlled snapshot of verification requirements; blocks DOORS export and regulatory delivery | **MUST-HAVE** |
| 4 | **DOORS Export/Import** | Not implemented | No XPorter CSV export template, no Activity issue type with "Import Doors ID in VVO" button, no validation rules for ID Doors import | VVOs must be referenced in DOORS for Airbus process compliance; manual export/import is error-prone and unauditable | **MUST-HAVE** |
| 5 | **VVO Transfer (DO to LAB)** | Not implemented | No transfer gadget/mechanism using ID Doors as identifier, no auto-copy/update, no email preview mode, no read-only VVO in LAB project | LAB team cannot receive verified requirements; manual copy creates version drift and breaks traceability | **MUST-HAVE** |
| 6 | **Test Procedure Authoring with VVO Linking** | Partially (basic test case management exists) | No VVO-to-Test linking with auto-population of Component/Applicability/Supplier fields, no VVO filter helpers (Component/Applicability/Supplier/Execution Responsible filters), no consistency checks between Test and VVO field values, no Test Mean Mini field | Test procedures disconnected from requirements means no coverage reporting, no automated campaign creation, and audit failure | **MUST-HAVE** |
| 7 | **Test Campaign Automation (Create Campaign)** | Not implemented | No CSV-driven campaign generation from Test Plan, no automatic Test Execution creation per {test, applicability} pair, no priority inheritance from VVOs, no campaign update (add-only, no delete), no log email with detailed actions | Manual campaign creation for hundreds of VVOs takes days; automation reduces test campaign setup from days to minutes | **MUST-HAVE** |
| 8 | **TechEvent Defect Management** | Partially (basic defect/bug tracking) | No 12-state M1668-compliant workflow, no cascading field dependencies (Detected on A_C Program drives Test Means/AC System/ATA/Supplier/Function/Partition), no supplier synchronization, no Create Change/Link Problem Report actions, no LTM field matching | Non-compliance with Airbus Method M1668; system defects cannot be properly classified, routed to resolver teams, or shared with suppliers | **MUST-HAVE** |
| 9 | **Bench Defect Management** | Not implemented | No separate Bench Defect type with cascading Test Mean Defect Origin (8 categories, 30+ sub-items), no severity/criticality matrix, no TechEvent reopening on Bench Defect state changes | Test mean issues conflated with system defects leads to incorrect anomaly classification and blocks lab clearance decisions | **MUST-HAVE** |
| 10 | **Problem Report (Certification)** | Not implemented | No Problem Report issue type with PR Type classification (Significant CAT/HAZ through Life-cycle data), no PR Origin tracking, no link to TechEvent, no Potential Effects/Justification fields | Problem Reports are the ONLY artifact transmitted to certification authorities; absence blocks EASA/FAA certification | **MUST-HAVE** |
| 11 | **Change Card with Multi-Tab Fields** | Not implemented | No 6-tab Change Card (Design, EIF, Planning, Review, Certification, Maturity Test, Safety) with 50+ fields, no Classification per EASA, no Safety Review workflow, no Design Review with RAG status | Design changes cannot be properly classified, reviewed, or certified; blocks the entire change management process from analysis to closure | **MUST-HAVE** |
| 12 | **DCL Synchronization (Airbus-Supplier)** | Not implemented | No bidirectional field synchronization between Airbus and Supplier projects, no "Copy to supplier/Airbus project" actions, no per-supplier field visibility | Specification discussions with suppliers happen outside the tool; no audit trail, no synchronization, risk of specification divergence | **SHOULD-HAVE** |
| 13 | **Design Item (DI) Tracking** | Not implemented | No DI issue type as parent of Change Cards, no supplier sharing with field copy | Design evolutions tracked informally; no hierarchical change grouping for impact analysis | **SHOULD-HAVE** |
| 14 | **Test Request (LTR/FTR)** | Not implemented | No Test Request issue type grouping VVOs with "Contain" link, no frozen state when Done, no XPorter export for LTR creation | DO cannot formally request test campaigns; VVO grouping for lab delivery is manual | **SHOULD-HAVE** |
| 15 | **XRAY Traceability Reports** | Partially (basic reports exist) | No VVO-as-requirement declaration in XRAY, no coverage rate calculation, no Test Plan/Test Execution/Test Runs reports with defect correlation | Cannot demonstrate requirement coverage to auditors; coverage rate must be manually calculated from spreadsheets | **SHOULD-HAVE** |
| 16 | **XPorter Report Templates (LTRA Generation)** | Not implemented | No "VVOs coverage from Test Plan" report by cluster/component, no "TechEvent List from Test Plan", no "Test Runs Detailed from Test Execution" | LTRA document generation requires manual copy-paste from Jira; each LTRA takes days instead of minutes | **SHOULD-HAVE** |
| 17 | **Export for Planning (Bench Slot Reservation)** | Not implemented | No duration estimation export per {Component, Test Means, Priority} triple | Bench slot reservation done by email/spreadsheet; risk of double-booking and campaign delays | **SHOULD-HAVE** |
| 18 | **Diff History (V3 with Evolution/Difference modes)** | Not implemented | No HTML generation of field change history, no date-range selection, no evolution vs. difference mode toggle | Change auditing requires manual comparison of Jira history; time-consuming and error-prone for certification evidence | **SHOULD-HAVE** |
| 19 | **Big Picture Integration (Test Campaign Roadmap)** | Not implemented | No BigPicture team synchronization, no Gantt-style test campaign visualization | Campaign planning done in external tools; no real-time visibility into test progress vs. plan | **SHOULD-HAVE** |
| 20 | **Deliverable Management (Project Management)** | Partially (basic task tracking) | No Deliverable issue type with Deliverable Type (SID/mSID/FRD/FDD/ICD), Milestone Type (EVM/Critical EVM), Planning tabs with baseline dates, Risk tabs with probability/consequence matrix, Review tabs | Project milestones tracked informally; no EVM tracking, no risk quantification, no review workflow for deliverables | **SHOULD-HAVE** |
| 21 | **VVO-Test Consistency Gadget** | Not implemented | No dashboard gadget that emails detailed inconsistency report between Test and VVO field values across all linked pairs | Inconsistencies between test procedures and requirements go undetected until audit; manual checking doesn't scale | **SHOULD-HAVE** |
| 22 | **TechEvent Supplier Synchronization** | Not implemented (covered under DCL above at higher level) | No auto-creation of TechEvent in supplier project, no field synchronization (Final Airbus Response, Supplier Analysis, Supplier Status), no attachment sharing button | Supplier analysis of system anomalies happens via email; no synchronized status tracking, risk of stale information | **SHOULD-HAVE** |
| 23 | **Data Hub Integration for Dynamic Field Values** | Not implemented | TechEvent/Bench Defect fields (Detected on A_C Program, Test Mean, Impacted AC System, ATA, System Supplier) should come from a Data Hub, not hardcoded | Violates the "no hardcoding" principle; new aircraft programs or test means require code changes instead of configuration | **MUST-HAVE** |
| 24 | **Generic notification for workflow transitions** | Partially exists | No transition-specific email notifications per the documented workflow requirements | Users miss status changes on VVOs they own; manual checking is unreliable | **SHOULD-HAVE** |
| 25 | **Automated CI/CD Pipeline (Jenkins + Artifactory)** | Not in scope for V&V tool | DevOps toolchain documented but not part of V&V application requirements | Out of scope -- covered by infrastructure team | **REJECTED** |
| 26 | **Confluence Integration** | Not in scope | Documentation tool mentioned but not part of V&V lifecycle workflows | Low value addition; existing documentation links sufficient | **REJECTED** |

---

## Phase 3: Architectural & Functional Gap Deep-Dive

### 3.1 MUST-HAVE Capabilities

#### Gap 1: VVO Requirement Authoring with Full Field Taxonomy

**Why it is needed:**
- Design Office engineers cannot define verification scope, execution responsibility, or test mean requirements without the documented 25+ VVO fields
- VVO Usage (Maturity/Formal verification/Non Regression) drives which VVOs participate in baseline reviews
- VVO Execution Delegation enables multi-system function (MSF) teams to delegate verification across 37 aircraft systems
- Without Operational Conditions and Expected Results, test engineers have insufficient specification to write procedures

**How to implement:**
1. **Data Model:** Create `VvoDefinition` entity with all fields from the V&V Template. Key relationships:
   - VVO --> HLVVO (many-to-one, link type "is parent of")
   - VVO --> Test (one-to-many, link type "tested by")
   - VVO --> VVO (version chain via "clone" link, auto-Superseded behavior)
   - VVO --> Test Request (many-to-one, link type "Contain")
2. **Dynamic Field Values:** Applicability, Supplier Applicability, VVO Execution Delegation systems (37 options), Real System Needed -- all sourced from admin master data CRUD, never hardcoded
3. **Version Management:** Clone action that auto-increments Version field, clears Fix Version and HLVVO link, creates "clone" link to original
4. **Workflow Engine:** 6-state workflow (New, To be verified, Verified, Released, Cancelled, Superseded) with:
   - Admin-only transitions for Verified/Released
   - Auto-transition of cloned VVOs to Superseded on source transition to Verified/Released
   - Read-only enforcement for Verified/Released/Superseded/Cancelled states

**Regression Prevention:**
- New entity, minimal impact on existing issue types
- Risk area: workflow engine must support auto-transitions triggered by linked issue state changes; verify existing PostFunctionExecutor handles this pattern
- Decouple VVO-specific field validation from generic issue validation to avoid breaking existing issue creation flows

#### Gap 2: VVO Baselining with DOORS Integration

**Why it is needed:**
- Baselines are the configuration-controlled delivery unit for regulatory compliance
- Without baselining, there is no mechanism to snapshot which VVOs were delivered for a given system standard
- DOORS integration is mandatory for Airbus process compliance (all VVOs must be referenced)

**How to implement:**
1. **Baseline Entity:** Leverage existing Fix Version mechanism as baseline identifier
2. **Bulk Operations API:** Endpoint to:
   - Tag multiple VVOs with a Fix Version (bulk change)
   - Transition multiple VVOs from Verified to Released (bulk transition)
3. **DOORS Export:** XPorter-style CSV generation with configurable column mapping
4. **DOORS Import:** Activity issue type with CSV upload, "Import Doors ID in VVO" action with 6 validation rules:
   - Empty ID Doors check
   - Duplicate ID Doors check
   - Mismatch with existing ID Doors in Jira
   - Cross-project reference check
   - Issue existence check
   - Header format validation

**Regression Prevention:**
- Bulk operations must be transactional; partial failures must roll back
- DOORS import validation must not modify existing VVO data until all rows pass validation
- Fix Version management may conflict with existing version-service usage; ensure VVO baselines use a distinct version category or namespace

#### Gap 3: Test Campaign Automation

**Why it is needed:**
- Manual campaign creation for 200+ VVOs with multiple applicabilities takes 2-3 days
- Automated campaign reduces this to minutes with full audit trail
- Priority inheritance from VVOs ensures highest-priority tests execute first on limited bench slots

**How to implement:**
1. **CSV Parser Service:** Parse LTR CSV with columns: ID Doors, Summary, Applicability, Version, Fix Version, Priority [, Configuration]
2. **Campaign Generator Logic:**
   ```
   For each VVO row in CSV:
     For each Test linked to VVO:
       If Test.status == "APPROVED" AND
          Test.SupplierApplicability contains TestPlan.SupplierApplicability AND
          NOT EXISTS TestExecution for {test, applicability} in TestPlan:
            Create TestExecution with:
              - testEnvironment = CSV.Applicability
              - fixVersion = TestPlan.FixVersion
              - component = Test.Component
              - supplierApplicability = TestPlan.SupplierApplicability
              - originalEstimate = Test.OriginalEstimate
              - priority = MAX(all linked VVO priorities in CSV)
            Associate TestExecution to TestPlan and Test
       Associate VVO to TestPlan with "Relates" link
   ```
3. **Idempotency:** Re-running adds only new Test Executions; never updates or deletes existing ones
4. **Logging:** Persist log to "Create Campaign Result" field + email with CSV attachment

**Regression Prevention:**
- Campaign creation must be idempotent; verify no duplicate Test Executions on re-run
- Priority inheritance logic must handle VVOs linked to multiple tests correctly
- Large campaigns (500+ Test Executions) must not timeout; implement async job with progress tracking

#### Gap 4: TechEvent Defect Management (M1668-Compliant)

**Why it is needed:**
- Airbus Method M1668 mandates the TechEvent process for system anomaly management
- 12-state workflow enables proper routing between V&V teams and Design teams
- Cascading field dependencies (6 levels) prevent data entry errors and ensure consistency
- Supplier synchronization is required for multi-supplier programs (e.g., Honeywell/Thales for nFMS)

**How to implement:**
1. **12-State Workflow Configuration:**
   - Define transitions with mandatory fields per transition (documented in user guide)
   - Implement transition screens displaying mandatory fields
   - Configure role-based transition visibility (V&V team vs. Design team)
2. **Cascading Field Dependencies:**
   ```
   Detected on A_C Program --> drives -->
     - Detected on Test Means (filtered list)
     - Impacted AC System (filtered list)
     - Impacted ATA (filtered list)
   Detected on A_C Program + Impacted AC System --> drives -->
     - System Supplier (filtered list)
   Impacted AC System --> drives -->
     - Impacted Function (filtered list)
     - Impacted Partition (filtered list)
   ```
   All cascading lists sourced from Data Hub (admin master data)
3. **Power Actions:**
   - "Supplier Analysis": Clone TechEvent to supplier project with field sync
   - "Share Attachments": Update attachments in supplier project
   - "Create Change": Create Change Card/Change Candidate in target project with "Change" link
   - "Link Problem Report": Create/link PR with auto-populated fields

**Regression Prevention:**
- Cascading dependencies must not break if Data Hub values change; implement graceful degradation
- Supplier synchronization must handle network failures with retry/queue mechanism
- Existing defect/bug workflows must remain unaffected; TechEvent is a new issue type in a separate project

#### Gap 5: Data Hub Integration (No Hardcoding)

**Why it is needed:**
- Per existing feedback memory: "All features must be configurable with admin master data CRUD, never hardcode business values"
- Documents show 15+ fields whose values come from "List from Data Hub": A/C Programs, Test Means, AC Systems, ATA Chapters, System Suppliers, Functions, Partitions
- New aircraft programs (e.g., NAx) or new test benches should require zero code changes

**How to implement:**
1. **Master Data Service:** Admin CRUD endpoints for all reference data categories:
   - A/C Programs (with sub-programs)
   - Test Means (per program)
   - AC Systems (per program)
   - ATA Chapters (per program)
   - System Suppliers (per program + system)
   - Functions (per system)
   - Partitions (per system)
   - Engine types, AC Series
2. **Cascading Resolution API:** Given a parent value, return valid child values
3. **Frontend:** Dynamic select lists that fetch options from master data service, with cascading refresh on parent value change

**Regression Prevention:**
- Must not break existing dropdown/select components; implement as a parallel data source
- Cache master data client-side with TTL to avoid API call on every field focus
- Migration: pre-populate all current hardcoded values into master data tables

### 3.2 SHOULD-HAVE Capabilities

#### Gap 12: DCL Synchronization (Airbus-Supplier)

**Why it is needed:**
- Specification discussions between Airbus and suppliers currently happen via email, creating audit gaps
- Bidirectional synchronization ensures both parties have current information

**How to implement:**
- Add DCL issue type with supplier-specific Description fields (Description Thales, Description Honeywell)
- Implement "Copy to supplier project" / "Copy to Airbus project" power actions
- Synchronize fields (Action Responsible, Requested By, Labels, Abstract) on any field update in either project
- Per-supplier field visibility rules based on project role

**Regression Prevention:**
- Synchronization must be idempotent; duplicate sync events must not create duplicate issues
- Field visibility rules must not affect existing project permission schemes

#### Gap 15: XRAY Traceability Reports

**Why it is needed:**
- Certification auditors require coverage rate reports showing which requirements have been tested and their results
- The traceability matrix (as shown in the DOCX example) groups VVOs by cluster with linked tests, defects, and test executions

**How to implement:**
- Declare VVO as "Requirement" in XRAY configuration to enable built-in coverage reports
- Implement XPorter templates for:
  - VVOs coverage from Test Plan (by cluster/component)
  - TechEvent List from Test Plan
  - Test Runs Detailed from Test Execution
- Support DOCX, PDF, XLSX, CSV output formats

**Regression Prevention:**
- Report generation for large test plans (1000+ test executions) must be async with progress indicator
- Template engine must handle missing data gracefully (empty cells, not errors)

#### Gap 18: Diff History (Evolution/Difference Modes)

**Why it is needed:**
- Change auditing for certification requires showing what changed between two configuration snapshots
- Manual Jira history comparison is time-consuming and error-prone

**How to implement:**
- "Diff History" button on VVO, TechEvent, Change Card issue types
- Date range picker with mode selection (Evolution/Difference)
- HTML generation comparing field values between two dates:
  - Evolution: All intermediate changes shown with timestamps
  - Difference: Only net delta between version at date A and version at date B
- Generated HTML stored as issue attachment (replaces previous)

**Regression Prevention:**
- HTML generation must handle all field types (text, select, multi-select, dates, users)
- Large issues with 100+ field changes must not timeout; paginate or limit history depth

---

## Phase 4: Phased Implementation Roadmap

### Phase 1: Data Schema, Master Data & Core Entities (Weeks 1-4)

**Goal:** Establish the data foundation without changing any existing workflows.

| Week | Deliverable | Dependencies |
|------|------------|-------------|
| 1 | Master Data Service: A/C Programs, Test Means, AC Systems, ATA Chapters, System Suppliers with admin CRUD API and cascading resolution | None (greenfield service) |
| 1 | Database migrations: `vvo_definition`, `hlvvo_definition`, `vvo_baseline`, `doors_mapping` tables | Existing DB infrastructure |
| 2 | VVO entity with full field taxonomy (25+ fields), VVO-to-HLVVO parent relationship, VVO version chain (clone link) | Master Data Service |
| 2 | HLVVO entity with proofreading table grid, parent-of relationship to VVOs | VVO entity |
| 3 | Change Card entity with 6-tab field structure (Design, EIF, Planning, Review, Certification, Maturity Test, Safety) | Master Data Service |
| 3 | Design Item entity (parent of Change Cards), DCL entity with supplier-specific fields | Change Card entity |
| 4 | TechEvent entity with full field taxonomy (40+ fields), cascading field dependencies | Master Data Service |
| 4 | Bench Defect entity with Test Mean Defect Origin cascading list, Problem Report entity with PR Type classification | TechEvent entity |
| 4 | Test Request entity, Deliverable entity with EVM/Risk/Review tabs | None |

**Validation Gate:** All entities have CRUD APIs, field validation, and admin master data sourcing verified via integration tests.

### Phase 2: Design Office Workflows (Weeks 5-8)

**Goal:** Enable the full DO lifecycle from VVO authoring through baselining and DOORS integration.

| Week | Deliverable | Dependencies |
|------|------------|-------------|
| 5 | VVO 6-state workflow engine (New, To be verified, Verified, Released, Cancelled, Superseded) with admin-only transitions | VVO entity, workflow engine |
| 5 | VVO clone action with auto-increment, auto-clear, and auto-Superseded behavior | VVO workflow |
| 6 | HLVVO workflow with auto-transition of child VVOs on Authorize/Supplier in Review | HLVVO entity, VVO workflow |
| 6 | VVO baselining: Fix Version tagging, bulk transition, 6 use case flows | VVO workflow |
| 7 | DOORS export (CSV via XPorter template), DOORS import (Activity issue type with 6 validation rules) | VVO baselining |
| 7 | Change Card workflow with Certification/Safety/Maturity Test tab logic, auto-set Resolved by | Change Card entity |
| 8 | DCL synchronization between Airbus and Supplier projects (bidirectional) | DCL entity |
| 8 | Design Item and Change Card parent-child relationship, DI sharing with suppliers | DI entity, Change Card workflow |

**Validation Gate:** End-to-end test: Create VVO -> Review -> Baseline -> Export to DOORS -> Import ID Doors -> Transfer to LAB (stub).

### Phase 3: Lab & RIG Test Integration (Weeks 9-14)

**Goal:** Enable the full LAB lifecycle from VVO reception through test execution, campaign automation, and defect management.

| Week | Deliverable | Dependencies |
|------|------------|-------------|
| 9 | VVO Transfer gadget (DO to LAB) using ID Doors identifier, read-only VVO in LAB project | VVO entity with DOORS ID, LAB project setup |
| 9 | LAB VVO workflow (New, Covered, Update, Cancelled, To be corrected) with auto-Update on transfer | VVO Transfer |
| 10 | Test entity with VVO linking (auto-population, filter helpers, consistency checks), Test Mean Mini field | LAB VVO, Test entity |
| 10 | Test workflow (Draft -> Internal Review -> External Review -> Approved -> Update/Script Error) with auto-Update on VVO change | Test entity, VVO workflow |
| 11 | Test Plan and Test Execution entities with full field taxonomy | Test entity |
| 11 | Campaign automation: CSV parser, "Create Campaign" button, idempotent Test Execution creation, priority inheritance, email logging | Test Plan, Test Execution, VVO-Test links |
| 12 | TechEvent 12-state workflow, cascading field dependencies, transition screens with mandatory fields | TechEvent entity, Master Data |
| 12 | TechEvent power actions: Supplier Analysis (sync), Share Attachments, Create Change, Link Problem Report | TechEvent workflow, Change Card, Problem Report |
| 13 | Bench Defect workflow with TechEvent reopening behavior, Problem Report workflow with certification classification | TechEvent, Bench Defect, Problem Report entities |
| 13 | XRAY Traceability Reports (coverage rate, Test Plans, Test Executions, Test Runs) | All test entities |
| 14 | XPorter report templates (VVOs coverage, TechEvent List, Test Runs Detailed, Test Execution KO) | XRAY Reports |
| 14 | Diff History V3 (Evolution/Difference modes) on VVO, TechEvent, Change Card | All entities |

**Validation Gate:** End-to-end test: DO creates VVO -> Baseline -> Transfer to LAB -> Write Test -> Create Campaign from CSV -> Execute Test -> Log TechEvent -> Share with Supplier -> Classify -> Create Change -> Resolve -> Close -> Generate LTRA report.

### Phase 4: Dashboards, Planning & Polish (Weeks 15-16)

| Week | Deliverable | Dependencies |
|------|------------|-------------|
| 15 | Export for Planning (duration by Component/Test Means/Priority), VVO-Test Consistency Gadget | Test Plan, Test Execution |
| 15 | Dashboard configuration (Custom Charts Simple Search gadgets, JQL toggles, Test Plan/Component/Test Environment filters) | All entities |
| 16 | Big Picture integration (team sync, Gantt view for test campaigns) | Test Plan, Test Execution |
| 16 | Deliverable management (EVM tracking, Risk matrix, Review workflow) | Deliverable entity |

**Validation Gate:** Full regression suite across all DO, LAB, and DEFECT workflows. Performance benchmarks for campaign creation (500+ VVOs in < 30 seconds) and report generation (1000+ test executions in < 60 seconds).

---

## Appendix A: Issue Type Summary

| Issue Type | Project | XRAY Object | Purpose |
|-----------|---------|-------------|---------|
| HLVVO | DO | Requirement (parent) | Groups VVOs for review process |
| VVO | DO | Requirement | V&V Objective -- requirement for LAB |
| VVO (read-only) | LAB | Requirement | Transferred VVO for test writing |
| Test | LAB | Test | Test procedure with steps |
| Pre-condition | LAB | -- | Reusable test pre-conditions |
| Test Plan | LAB | -- | Test campaign grouping |
| Test Execution | LAB | -- | Test run instance |
| Test Request | DO | -- | LTR/FTR grouping of VVOs |
| Design Item | DO | Change | High-level design evolution |
| Change Card | DO | Change | Detailed design change |
| DCL | DO | -- | Specification clarification log |
| TechEvent | DEFECT | Defect | System anomaly (M1668) |
| Bench Defect | DEFECT | Defect | Test means anomaly |
| Problem Report | DEFECT | -- | Certification-facing report |
| Deliverable | PM | -- | Document delivery tracking |
| Activity | DO | -- | Utility (DOORS import) |
| Epic | DO | -- | Standard Jira epic |

## Appendix B: Workflow State Machines (Quick Reference)

### VVO (DO)
```
New -> To be verified -> Verified -> Released -> [Plan Rework -> New]
                              |          |
                         Superseded  Cancelled
```

### VVO (LAB)
```
New -> Covered -> [Update | Cancelled | To be corrected -> New]
```

### Test
```
Draft -> Internal Review -> External Review -> Approved -> [Script Error -> Draft]
                                                     |
                                                  Update -> Draft
```

### TechEvent
```
Open -> Under Originator Analysis -> Under Resolver Analysis -> Classified -> To be Assessed
   |              |                          |                                      |
   |    Under Test Mean Analysis      Ready for Review         Resolved-Corrected / Contained
   |                                                                                |
   +-- Proposed for Cancellation -> Cancelled                                     Closed
   +-- To be Refined -> Under Originator Analysis
```

### Change Card
```
In Analysis -> In Progress -> [Closed | No Change | Temporary Acceptance]
```

### Problem Report
```
Open -> Under Analysis -> [Closed | Rejected]
```

## Appendix C: Video Training Assets (Not Extractable -- Noted for Reference)

| File | Location | Inferred Content |
|------|----------|-----------------|
| Training on Defects in Jira (2024-02-02).mp4 | SYSDOPS DO and LAB Template | TechEvent/Bench Defect creation and workflow walkthrough |
| Action.mp4 | Archive/nFMS | Action issue type demo |
| Deliverable.mp4 | Archive/nFMS | Deliverable issue type demo |
| DI.mp4 | Archive/nFMS | Design Item creation and workflow |
| Epic.mp4 | Archive/nFMS | Epic issue type demo |
| bug.mp4 | Archive/FWS | Flight Warning System bug workflow |
| Jira connection presentation.mp4 | Archive | Jira access and connection setup |
| How To View User Profile in Jira.mp4 | Archive | User profile management |
