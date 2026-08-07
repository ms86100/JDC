# Comprehensive Audit: Two Documents vs Implementation

## Context

Two source documents were provided:
1. **IFCS JIRA Use Cases** (1YC/1V IFCS System Development & Testing) — defines VVM Card, IVV Card, Group, Sub-Change Card issue types with GIT/Jenkins pipeline automation
2. **nFMS VVO Guidelines** (X2270RP2215975 Issue 2) — defines detailed VVO/HLVVO authoring rules, naming conventions, field taxonomies, multi-phase review workflows, and requirement traceability logic

This plan cross-references every requirement from both documents against the actual codebase to identify what's done, what's partially done, and what's missing.

---

## Section 1: UNDERSTANDING OF CONCRETE FUNCTIONALITY (from VVO Guidelines)

### 1.1 Data Baseline Integrations Required
- **MFCL (Master Functional Content List)** — functional requirements from SRD; VVOs trace to MFCL via `ptsMfclLinks` field
- **DTS (Detailed Technical Specification)** — design requirements; VVOs trace to changed/merged DTS via `associatedRequirements` field
- **SID (System Interface Document)** — interface requirements for Interface VVOs
- **JIRA modules** — NFMSST (testing) and NFMSS (specifications/DIs)
- **Design Items (DI)** — tracked in NFMSS, linked to VVOs via `nDi` field
- **Design Logs (DL)** — supplier specificities, checked via JDP Actions Log

### 1.2 Core Entities — VVO vs HLVVO Field Taxonomy

**HLVVO fields (per guidelines):**
- Summary (naming: `HLVVO_nFMS_<type>_<domain>_<cluster>`)
- Description (mandatory)
- Target Date, Version (mandatory), Assignee
- PTS/MFCL links (all MFCL/SID reqs covered by this HLVVO)
- Component (`[Cluster] xxxx`)
- Reference Documents (DTS baseline)
- Airbus Reference
- VVO Execution Responsible (N/A at HLVVO level)
- Proofreading (Table Grid: Order, Summary, Assignee, Status)
- Links: "is parent of" → VVOs, "links to" → GDrive review package + PRF

**VVO fields (per guidelines):**
- Summary (naming: `[VVO_nFMS_<cluster>_<number>.<decimal>] To check...`)
- VVO Execution Responsible (checkboxes: Airbus DO, Airbus Lab tests, Airbus Flight tests, Thales, Honeywell)
- VVO Usage (Maturity, Formal verification, Non regression)
- Component (`[Cluster] xxxx`)
- Operational Conditions (structured: Prerequisites + Test Cases)
- Expected Results (structured: TestCase 1, TestCase 2...)
- Necessary Real Systems
- Applicability (multi-select A/C programs)
- Supplier Applicability (Honeywell, Thales)
- Associated Requirements (DTS req IDs separated by ";")
- N°DI (Design Item numbers)
- Labels (Change, Merge, Clarification, NoChange, Pureflyt)
- Links: "is child of" → HLVVO, "relates" → other VVOs
- Version (mandatory), Reference Documents (DTS baseline)
- ID Doors (auto-imported from DOORS)
- Description (context, preferred test program)
- PTS/MFCL link/s

### 1.3 Automation Rules & Granularity Logic
- **Change/Merge DTS reqs** → High granularity VVOs (trace to DTS + DI + MFCL)
- **No Change/Clarification** → Low granularity "Good Functioning" VVOs (trace to MFCL only, not DTS)
- **Pureflyt** → Supplier-specific design deviations (label: Pureflyt)
- **Golden Rule**: Write Good Functioning VVOs FIRST, then Change/Merge VVOs
- **Labels logic**: Change+Merge → both labels; Change+NoChange → only Change label; Clarification+NoChange → only Clarification

### 1.4 Multi-Phase HLVVO Lifecycle (from VVO Guidelines page 36)
```
HLVVO Workflow (10 states):
NEW → PLANNED → VVO_WRITING_IN_PROGRESS → DESIGN_OFFICE_IN_REVIEW → LAB_IN_REVIEW → SUPPLIER_AIRBUS_VVO_ASSIGNMENT → SUPPLIER_IN_REVIEW → AUTHORIZE → CLOSED
+ ON_HOLD (from any state)
+ CANCELLED (from any state)
```

**VVO Workflow (simple 3 states):**
```
TO_DO ↔ IN_PROGRESS ↔ DONE (all transitions between any state)
```

### 1.5 Timing Model
- Phase 0 (KOM): Pre-writing preparation
- Phase 1 (DO): 5 weeks (2 weeks writing + 2 weeks DO review + 1 week updates)
- Phase 2 (LAB): 3 weeks (2 weeks LAB review + 1 week updates)
- Phase 3 (Supplier): 3 weeks (2 weeks supplier review + 1 week updates)
- Total theoretical: ~11 weeks per HLVVO

---

## Section 2: WHAT IS CURRENTLY IMPLEMENTED

### 2.1 Fully Implemented (15 items — DONE)

| # | Item | Source Doc | Status |
|---|------|-----------|--------|
| 1 | Architecture Page IFCS diagrams 11-17 | IFCS | DONE |
| 2 | V12 IFCS issue types seed (VVM Card, IVV Card, Group, Sub-Change) | IFCS | DONE |
| 3 | V28 IFCS entity tables (4 tables in issue-service) | IFCS | DONE |
| 4 | Java entities (VvmCardMetadata, IvvCardMetadata, GroupMetadata, SubChangeMetadata) | IFCS | DONE |
| 5 | Java repositories (4 interfaces) | IFCS | DONE |
| 6 | ChangeManagement REST endpoints for IFCS (12 endpoints) | IFCS | DONE |
| 7 | V29 IFCS workflow definitions (4 workflows) | IFCS | DONE |
| 8 | V30 WorkflowScheme mapping for IFCS types | IFCS | DONE |
| 9 | Frontend demo data (VVM Cards, IVV Cards, Groups, Sub-Changes) | IFCS | DONE |
| 10 | Frontend pages (VvmCardListPage, IvvCardListPage, GroupListPage) | IFCS | DONE |
| 11 | App.tsx routes + navigation entries | IFCS | DONE |
| 12 | ifcsApi.ts frontend API client | IFCS | DONE |
| 13 | Gateway routes for all SYSDOPS paths | Both | DONE |
| 14 | All SYSDOPS controllers with REST endpoints | Both | DONE |
| 15 | VVO core fields (vvoUsage, vvoScope, operationalConditions, expectedResults, associatedRequirements, supplierApplicability, labels) | VVO Guidelines | DONE |

### 2.2 HLVVO Entity Fields (5/5 present)
- proofreadingData (JSONB) ✓
- targetDate ✓
- hlvvoVersion ✓
- specificationReference ✓
- airbusReference ✓

### 2.3 VVO Entity Fields (10/13 present)
Present: vvoUsage, vvoScope, executionResponsible, operationalConditions, expectedResults, associatedRequirements, supplierApplicability, labels, applicability, idDoors
Missing: ptsMfclLinks, nDi, referenceDocuments

---

## Section 3: GAPS & REMAINING IMPLEMENTATION ITEMS

### 3.1 CRITICAL GAPS (from VVO Guidelines)

| # | Gap | Severity | Details |
|---|-----|----------|---------|
| G1 | **HLVVO workflow missing 5 states** | CRITICAL | Implementation has 5 states (New, Plan, VVO Writing, Supplier Review, Authorize). Missing: DESIGN_OFFICE_IN_REVIEW, LAB_IN_REVIEW, SUPPLIER_AIRBUS_VVO_ASSIGNMENT, CLOSED, ON_HOLD, CANCELLED |
| G2 | **VVO entity missing 3 fields** | HIGH | `nDi` (Design Item numbers), `ptsMfclLinks` (MFCL refs at VVO level), `referenceDocuments` (DTS baseline ref) |
| G3 | **No VVO naming convention validation** | HIGH | No code enforces `[VVO_nFMS_<cluster>_<number>.<decimal>]` format |
| G4 | **executionResponsible lacks specific suppliers** | MEDIUM | Uses generic SUPPLIER instead of Thales/Honeywell checkboxes |
| G5 | **operationalConditions is unstructured** | MEDIUM | Flat text field instead of structured Prerequisites + Test Cases sections |
| G6 | **Frontend demo data doesn't follow VVO naming** | LOW | Uses NFMSDO-xxx instead of VVO_nFMS_xxx convention |
| G7 | **No label auto-assignment logic** | MEDIUM | Labels (Change/Merge/Clarification/NoChange/Pureflyt) must be manually set; no automation based on requirement type |
| G8 | **No DTS baseline version tracking** | MEDIUM | No field tracks which DTS baseline was used to write each VVO, and no mechanism to flag VVOs needing review on baseline change |

### 3.2 GAPS FROM IFCS DOCUMENT (already closed in prior session)
All 15 IFCS items are DONE — no remaining gaps.

### 3.3 UNRESOLVED CONFLICTS (from VVO Guidelines page 66)
- **VVO applicability mixing**: DO says possible to put requirements with different applicabilities in one VVO; LAB says not possible. This policy conflict needs a configuration flag.
- **Thales Pureflyt designs not always covered by DI**: The guidelines warn that "/!\ Thales Pureflyt introduces a new design not always covered by DI" — no automated detection exists.

---

## Section 4: TASK & SUBTASK EXECUTION PLAN

### Phase 1: VVO Entity Field Additions (issue-service)

**Task 1.1: Add missing VVO fields**
- Add `pts_mfcl_links TEXT[]` column to `vvo_definition` table (new Flyway migration in test-service)
- Add `n_di TEXT[]` column (Design Item numbers)
- Add `reference_documents TEXT` column (DTS baseline reference)
- Update VvoDefinition.java entity with 3 new fields
- Update VvoService create/update methods
- Update VvoController endpoints to accept new fields

**Task 1.2: Refine executionResponsible values**
- Change SUPPLIER to THALES and HONEYWELL as separate enum values
- Update VVO seed data (V12) and demo data

### Phase 2: HLVVO Workflow Completion (workflow-service)

**Task 2.1: Add 5 missing HLVVO states**
- New Flyway migration adding states: DESIGN_OFFICE_IN_REVIEW, LAB_IN_REVIEW, SUPPLIER_AIRBUS_VVO_ASSIGNMENT, CLOSED, ON_HOLD, CANCELLED
- Add transitions:
  - VVO_WRITING → DESIGN_OFFICE_IN_REVIEW (Send for DO Review)
  - DESIGN_OFFICE_IN_REVIEW → VVO_WRITING (DO Review NOK, return)
  - DESIGN_OFFICE_IN_REVIEW → LAB_IN_REVIEW (DO Review OK)
  - LAB_IN_REVIEW → VVO_WRITING (LAB Review NOK, return)
  - LAB_IN_REVIEW → SUPPLIER_AIRBUS_VVO_ASSIGNMENT (LAB Review OK)
  - SUPPLIER_AIRBUS_VVO_ASSIGNMENT → SUPPLIER_IN_REVIEW (Send to Supplier)
  - SUPPLIER_IN_REVIEW → VVO_WRITING (Supplier NOK)
  - SUPPLIER_IN_REVIEW → AUTHORIZE (Supplier OK)
  - AUTHORIZE → CLOSED (Close HLVVO)
  - Any → ON_HOLD (Put on Hold)
  - ON_HOLD → previous state (Resume)
  - Any → CANCELLED (Cancel)

### Phase 3: VVO Naming Convention Validation

**Task 3.1: Backend validation**
- Add regex validation in VvoService.createVvo() that enforces `VVO_nFMS_<cluster>_<number>.<decimal>` on the `summary` field
- Add unit/decimal auto-increment logic: new VVO in same cluster → increment unit; specific test case → increment decimal

**Task 3.2: Frontend naming helper**
- Add auto-suggest in VVO create form that generates the next available name based on cluster and existing VVOs

### Phase 4: Structured Operational Conditions

**Task 4.1: Add structured prerequisites/test-cases model**
- Replace flat `operationalConditions` TEXT with JSONB containing:
  ```json
  {
    "prerequisites": ["FMS is in CLB or CRZ flight phase", "Engines are ON"],
    "testCases": [
      {"id": 1, "description": "Check that DIR-TO INTERCEPT waypoint sequences correctly"},
      {"id": 2, "description": "Check that ND displays DIR-TO path after activation"}
    ]
  }
  ```
- Same for `expectedResults`:
  ```json
  {
    "testCaseResults": [
      {"testCaseId": 1, "result": "FMS sequences DIR-TO INTERCEPT waypoint in LAT AUTO mode"},
      {"testCaseId": 2, "result": "DIR-TO path displayed on ND with correct symbology"}
    ]
  }
  ```

### Phase 5: DTS Baseline Tracking & Synchronization

**Task 5.1: Baseline version tracking**
- Add `dts_baseline_version` field to VvoDefinition
- Add `baseline_verified` boolean (false when DTS baseline changes, true after re-verification)
- Add batch endpoint to flag all VVOs as needing re-verification when a new DTS baseline is released

### Phase 6: Label Auto-Assignment Engine

**Task 6.1: Automated label logic**
- When `associatedRequirements` contains DTS requirements with change_rational = "Change" → auto-add "Change" label
- When "Merge" → auto-add "Merge"
- When only "Clarification"/"No change" → auto-add "Clarification" or "NoChange"
- Pureflyt → auto-add when supplier-specific Thales design deviation detected

### Phase 7: Frontend Demo Data Alignment

**Task 7.1: Update demo data naming**
- Change VVO issueKeys from `NFMSDO-xxx` to `NFMSST-xxx` (matching NFMSST project)
- Change VVO summaries to follow `[VVO_nFMS_<cluster>_<n>.<d>] To check...` format
- Change idDoors values to match DOORS import format

### Phase 8: Architecture Page — VVO Guidelines Integration

**Task 8.1: Add VVO Guidelines diagrams to Architecture page**
- Add Mermaid diagram: HLVVO Authoring Process (Phase 0→1→2→3 with KOM, DO Review, LAB Review, Supplier Review)
- Add Mermaid diagram: HLVVO Global Architecture (MFCL → DI → DTS → VVO → HLVVO hierarchy)
- Add Mermaid diagram: VVO Granularity Decision Flowchart (Change/Merge → High granularity, No Change → Low granularity)
- Add Mermaid diagram: nFMS Testing Traceability (SRD → MFCL → DTS → VVO → LTR → Test Procedure → TRA)
- Add comparison table: VVO Types (Change/Merge, Good Functioning, Specific, MFD-MCDU)

---

## Verification

1. `mvn compile -pl avionics-systems-test-service,avionics-systems-workflow-service` — verify new entities and migrations compile
2. `npm run build` in frontend — verify demo data and new pages build
3. Docker rebuild and verify all routes return HTTP 200
4. Verify HLVVO workflow has all 10 states via seed data inspection
5. Verify VVO naming validation rejects invalid formats
6. Verify Architecture page shows VVO Guidelines diagrams alongside IFCS diagrams
