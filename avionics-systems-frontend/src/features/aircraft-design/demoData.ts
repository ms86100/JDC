const pid = '10000000-0000-0000-0000-000000000001';

export const DEMO_PROJECT_ID = pid;

export const DEMO_VVOS = [
  { id: 'v001', issueKey: 'NFMSST-101', summary: '[VVO_nFMS_DIRTO_1.0] To check the lateral guidance mode engagement after DIR TO insertion', status: 'RELEASED', fixVersionName: 'STD-3.2', idDoors: 'VVO_nFMS_DIRTO_1.0', applicability: 'SA_CEONEO', assigneeName: 'P. Martin', hlvvoId: 'h001', projectId: pid, createdAt: '2025-11-15T10:00:00Z', vvoVersion: 3, labels: ['Change'], executionResponsible: ['Airbus Lab tests'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-355'], nDi: ['DI#004'], referenceDocuments: 'DTS Rev G', dtsBaselineVersion: 'Rev G' },
  { id: 'v002', issueKey: 'NFMSST-102', summary: '[VVO_nFMS_VERT_1.0] To check the vertical flight phase transitions (CLB/CRZ/DES)', status: 'VERIFIED', fixVersionName: 'STD-3.2', idDoors: 'VVO_nFMS_VERT_1.0', applicability: 'SA_CEONEO, LR_CEONEO', assigneeName: 'S. Dupont', hlvvoId: 'h001', projectId: pid, createdAt: '2025-11-16T09:30:00Z', vvoVersion: 2, labels: ['Merge'], executionResponsible: ['Thales', 'Honeywell'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-230'], nDi: [], referenceDocuments: 'DTS Rev G', dtsBaselineVersion: 'Rev G' },
  { id: 'v003', issueKey: 'NFMSST-103', summary: '[VVO_nFMS_HOLD_1.0] To check the HOLD pattern entry logic with wind correction', status: 'TO_BE_VERIFIED', fixVersionName: 'STD-3.3', idDoors: 'VVO_nFMS_HOLD_1.0', applicability: 'SA_CEONEO', assigneeName: 'A. Chen', hlvvoId: 'h001', projectId: pid, createdAt: '2025-12-01T14:20:00Z', vvoVersion: 1, labels: ['Change'], executionResponsible: ['Airbus Lab tests'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-370'], nDi: ['DI#112'], referenceDocuments: 'DTS Rev H', dtsBaselineVersion: 'Rev H' },
  { id: 'v004', issueKey: 'NFMSST-104', summary: '[VVO_nFMS_OFFSET_1.0] To check the OFFSET path computation accuracy', status: 'NEW', fixVersionName: '', idDoors: '', applicability: 'LR_CEONEO', assigneeName: 'M. Fischer', hlvvoId: 'h002', projectId: pid, createdAt: '2026-01-10T08:45:00Z', vvoVersion: 1, labels: ['NoChange'], executionResponsible: ['Airbus Lab tests'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-400'], nDi: [], referenceDocuments: 'DTS Rev H', dtsBaselineVersion: 'Rev H' },
  { id: 'v005', issueKey: 'NFMSST-105', summary: '[VVO_nFMS_LATFPLN_1.0] To check the alternate FPLN insertion and activation sequence', status: 'RELEASED', fixVersionName: 'STD-3.2', idDoors: 'VVO_nFMS_LATFPLN_1.0', applicability: 'SA_CEONEO, SA_NewAvionics', assigneeName: 'P. Martin', hlvvoId: 'h002', projectId: pid, createdAt: '2025-10-05T11:00:00Z', vvoVersion: 4, labels: ['Change', 'Merge'], executionResponsible: ['Airbus Lab tests'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-230'], nDi: ['DI#004'], referenceDocuments: 'DTS Rev G', dtsBaselineVersion: 'Rev G' },
  { id: 'v006', issueKey: 'NFMSST-106', summary: '[VVO_nFMS_DIRTO_2.0] To check the DIR TO waypoint sequencing and leg transition', status: 'RELEASED', fixVersionName: 'STD-3.1', idDoors: 'VVO_nFMS_DIRTO_2.0', applicability: 'SA_CEONEO', assigneeName: 'S. Dupont', hlvvoId: 'h001', projectId: pid, createdAt: '2025-09-20T16:00:00Z', vvoVersion: 2, labels: ['Change'], executionResponsible: ['Honeywell'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-360'], nDi: ['DI#141'], referenceDocuments: 'DTS Rev G', dtsBaselineVersion: 'Rev G' },
  { id: 'v007', issueKey: 'NFMSST-107', summary: '[VVO_nFMS_LATFPLN_2.0] To check the STRINGING procedure build from waypoint database', status: 'SUPERSEDED', fixVersionName: 'STD-3.0', idDoors: 'VVO_nFMS_LATFPLN_2.0', applicability: 'SA_CEONEO', assigneeName: 'A. Chen', hlvvoId: 'h001', projectId: pid, createdAt: '2025-06-12T10:30:00Z', vvoVersion: 1, labels: ['Clarification'], executionResponsible: ['Airbus Lab tests'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-410'], nDi: [], referenceDocuments: 'DTS Rev F', dtsBaselineVersion: 'Rev F' },
  { id: 'v008', issueKey: 'NFMSST-108', summary: '[VVO_nFMS_VERTAPPR_1.0] To check the INIT page fuel prediction computation', status: 'CANCELLED', fixVersionName: 'STD-3.2', idDoors: 'VVO_nFMS_VERTAPPR_1.0', applicability: 'LR_CEONEO', assigneeName: 'M. Fischer', hlvvoId: 'h002', projectId: pid, createdAt: '2025-11-01T13:15:00Z', vvoVersion: 1, labels: ['NoChange'], executionResponsible: ['Thales'], ptsMfclLinks: [], nDi: [], referenceDocuments: 'DTS Rev G', dtsBaselineVersion: 'Rev G' },
  { id: 'v009', issueKey: 'NFMSST-109', summary: '[VVO_nFMS_VERT_2.0] To check the VNAV path descent with speed/altitude constraints', status: 'TO_BE_VERIFIED', fixVersionName: 'STD-3.3', idDoors: '', applicability: 'SA_CEONEO', assigneeName: 'P. Martin', hlvvoId: 'h001', projectId: pid, createdAt: '2026-02-18T09:00:00Z', vvoVersion: 1, labels: ['Change'], executionResponsible: ['Airbus Lab tests'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-230'], nDi: ['DI#200'], referenceDocuments: 'DTS Rev H', dtsBaselineVersion: 'Rev H' },
  { id: 'v010', issueKey: 'NFMSST-110', summary: '[VVO_nFMS_LATFPLN_3.0] To check the lateral revision on active leg (CF/TF/DF)', status: 'NEW', fixVersionName: '', idDoors: '', applicability: 'SA_NewAvionics', assigneeName: 'S. Dupont', hlvvoId: 'h002', projectId: pid, createdAt: '2026-03-01T07:30:00Z', vvoVersion: 1, labels: ['Pureflyt'], executionResponsible: ['Thales'], ptsMfclLinks: [], nDi: [], referenceDocuments: 'DTS Rev H', dtsBaselineVersion: 'Rev H' },
  { id: 'v011', issueKey: 'NFMSST-111', summary: '[VVO_nFMS_MSF_RNPAR_1.0] To check the RNP-AR approach procedure loading and monitoring', status: 'VERIFIED', fixVersionName: 'STD-3.3', idDoors: 'VVO_nFMS_MSF_RNPAR_1.0', applicability: 'SA_CEONEO, LR_CEONEO', assigneeName: 'A. Chen', hlvvoId: 'h001', projectId: pid, createdAt: '2026-01-28T15:45:00Z', vvoVersion: 2, labels: ['Change'], executionResponsible: ['Airbus Lab tests'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-1205'], nDi: ['DI#089'], referenceDocuments: 'DTS Rev H', dtsBaselineVersion: 'Rev H' },
  { id: 'v012', issueKey: 'NFMSST-112', summary: '[VVO_nFMS_IF_IFCS_FMS_1.0] To check the TCAS/TAWS integration with FMS flight plan', status: 'RELEASED', fixVersionName: 'STD-3.2', idDoors: 'VVO_nFMS_IF_IFCS_FMS_1.0', applicability: 'SA_CEONEO', assigneeName: 'M. Fischer', hlvvoId: 'h002', projectId: pid, createdAt: '2025-08-10T10:00:00Z', vvoVersion: 3, labels: ['Change'], executionResponsible: ['Airbus Lab tests', 'Airbus Flight tests'], ptsMfclLinks: ['MPP-PTS-FMS-MFCL-420'], nDi: ['DI#150'], referenceDocuments: 'DTS Rev G', dtsBaselineVersion: 'Rev G' },
];

export const DEMO_HLVVOS = [
  { id: 'h001', issueKey: 'NFMSST-5', summary: 'HLVVO_nFMS_FC_LATERAL_DIRTO - Provide DIR TO function', status: 'AUTHORIZE', childVvoCount: 7, assigneeName: 'P. Martin', projectId: pid, createdAt: '2025-09-01T08:00:00Z' },
  { id: 'h002', issueKey: 'NFMSST-10', summary: 'HLVVO_nFMS_FC_VERT_APPR - Vertical approach management', status: 'LAB_IN_REVIEW', childVvoCount: 5, assigneeName: 'S. Dupont', projectId: pid, createdAt: '2025-10-15T09:00:00Z' },
  { id: 'h003', issueKey: 'NFMSST-15', summary: 'HLVVO_nFMS_FC_LATFPLN_ALTERNATE - Flight plan alternate management', status: 'DESIGN_OFFICE_IN_REVIEW', childVvoCount: 0, assigneeName: 'A. Chen', projectId: pid, createdAt: '2026-01-20T14:00:00Z' },
  { id: 'h004', issueKey: 'NFMSST-20', summary: 'HLVVO_nFMS_IF_IFCS_FMS - IFCS-FMS interface on SA New Avionics', status: 'VVO_WRITING_IN_PROGRESS', childVvoCount: 0, assigneeName: 'M. Fischer', projectId: pid, createdAt: '2026-03-05T11:30:00Z' },
  { id: 'h005', issueKey: 'NFMSST-25', summary: 'HLVVO_nFMS_MSF_RNPAR - RNP AR approach', status: 'ON_HOLD', childVvoCount: 2, assigneeName: 'A. Chen', projectId: pid, createdAt: '2026-02-10T08:00:00Z' },
  { id: 'h006', issueKey: 'NFMSST-30', summary: 'HLVVO_nFMS_FC_MFD_MCDU - MFD/MCDU display verification', status: 'CLOSED', childVvoCount: 12, assigneeName: 'P. Martin', projectId: pid, createdAt: '2025-06-01T08:00:00Z' },
];

export const DEMO_HLVVO_CHILDREN: Record<string, typeof DEMO_VVOS> = {
  h001: DEMO_VVOS.filter(v => v.hlvvoId === 'h001'),
  h002: DEMO_VVOS.filter(v => v.hlvvoId === 'h002'),
};

export const DEMO_TECH_EVENTS = [
  { id: 'te001', issueKey: 'NFMSDEF-301', summary: 'Lateral guidance mode fails to engage after DIR TO insertion', status: 'Open', reporterTeam: 'LAB nFMS 1V', program: 'A320 CEO/NEO', priority: 'High', assigneeName: 'P. Martin', createdAt: '2026-02-10T09:15:00Z' },
  { id: 'te002', issueKey: 'NFMSDEF-302', summary: 'VNAV path goes below MDA during non-precision approach', status: 'Analysis', reporterTeam: 'LAB nFMS 1V', program: 'A320 CEO/NEO', priority: 'Critical', assigneeName: 'S. Dupont', createdAt: '2026-02-12T14:00:00Z' },
  { id: 'te003', issueKey: 'NFMSDEF-303', summary: 'HOLD pattern timing incorrect with tailwind > 40kt', status: 'Classified', reporterTeam: 'DO nFMS 1PYC', program: 'A320 CEO/NEO', priority: 'Medium', assigneeName: 'A. Chen', createdAt: '2026-01-20T11:30:00Z' },
  { id: 'te004', issueKey: 'NFMSDEF-304', summary: 'STRINGING waypoints reorder after database cycle update', status: 'Resolved', reporterTeam: 'LAB nFMS 1V', program: 'A330 CEO/NEO', priority: 'Medium', assigneeName: 'M. Fischer', createdAt: '2025-12-15T08:45:00Z' },
  { id: 'te005', issueKey: 'NFMSDEF-305', summary: 'TCAS RA maneuver not reflected in FMS prediction', status: 'Closed', reporterTeam: 'Flight Test', program: 'A320 CEO/NEO', priority: 'High', assigneeName: 'P. Martin', createdAt: '2025-11-01T16:20:00Z' },
  { id: 'te006', issueKey: 'NFMSDEF-306', summary: 'Alternate FPLN fuel calculation discrepancy at MTOW', status: 'Open', reporterTeam: 'DO nFMS 1PYC', program: 'A350', priority: 'Low', assigneeName: 'S. Dupont', createdAt: '2026-03-01T10:00:00Z' },
  { id: 'te007', issueKey: 'NFMSDEF-307', summary: 'CF leg overshoot on tight turn radius (< 2 NM)', status: 'Resolver', reporterTeam: 'LAB nFMS 1V', program: 'A320 CEO/NEO', priority: 'High', assigneeName: 'A. Chen', createdAt: '2026-02-25T13:40:00Z' },
  { id: 'te008', issueKey: 'NFMSDEF-308', summary: 'RNP-AR approach lateral deviation exceeds ANP threshold', status: 'Assessed', reporterTeam: 'Flight Test', program: 'A320 New Avionics', priority: 'Critical', assigneeName: 'M. Fischer', createdAt: '2026-01-05T15:10:00Z' },
];

export const DEMO_CHANGE_CARDS = [
  {
    id: 'cc001', issueId: 'cc001', title: 'CC-2401: Lateral guidance engagement logic rework',
    classification: 'Major', designReviewRag: 'Amber',
    description: 'Rework lateral guidance engagement conditions to prevent false positive engagement after DIR TO waypoint insertion during HOLD pattern.',
    designJustification: 'Current logic does not check for active HOLD pattern before allowing LAT mode engagement. This causes uncommanded lateral deviation.',
    designImpact: 'FMS lateral guidance module, AP/FD interface ARINC 429 words 310-314',
    affectedParts: 'LGUID_MOD_V3.2, AP_INTF_V2.1',
    eifReference: 'EIF-2024-0142', eifStatus: 'Impact assessed',
    targetDate: '2026-06-30', milestones: 'PDR: 2026-04-15, CDR: 2026-05-20, LAR: 2026-06-25',
    reviewStatus: 'In Review', reviewComments: 'Design review scheduled for Sprint 24-W12. Safety team review pending.',
    certificationBasis: 'CS-25.1329 (Flight Guidance System)', complianceMethod: 'MC4 - Analysis + Test',
    maturityTestPlan: 'Maturity test on SIB-2 bench with A320 CEO/NEO load', maturityTestStatus: 'Planned (P2)',
    safetyAssessment: 'FMEA completed. No new hazard identified. DAL-C software change.', safetyClassification: 'Minor',
  },
];

export const DEMO_DESIGN_ITEMS = [
  { id: 'di001', partNumber: 'DI-2024-087', partName: 'Lateral Guidance Enhancement Package', revision: 'B', status: 'In Progress' },
];

export const DEMO_BASELINE_SUMMARY = {
  totalVvos: 12, newCount: 2, verifiedCount: 2, releasedCount: 5, cancelledCount: 1, supersededCount: 1,
};

export const DEMO_BASELINE_VVOS = DEMO_VVOS.filter(v => v.fixVersionName === 'STD-3.2');

export const DEMO_PROGRAMS = [
  { id: 'p001', name: 'A320 CEO/NEO', code: 'SA_CEONEO', description: 'Single-aisle CEO and NEO variants', active: true },
  { id: 'p002', name: 'A330 CEO/NEO', code: 'LR_CEONEO', description: 'Long-range CEO and NEO variants', active: true },
  { id: 'p003', name: 'A350', code: 'A350', description: 'A350 XWB family', active: true },
  { id: 'p004', name: 'A380', code: 'A380', description: 'A380 super-jumbo', active: false },
  { id: 'p005', name: 'New Avionics (NAx)', code: 'SA_NewAvionics', description: 'Next-generation avionics platform', active: true },
];

export const DEMO_TEST_MEANS = [
  { id: 'tm001', name: 'SIB-1 (System Integration Bench)', code: 'SIB1', programId: 'p001', active: true },
  { id: 'tm002', name: 'SIB-2 (System Integration Bench)', code: 'SIB2', programId: 'p001', active: true },
  { id: 'tm003', name: 'FIB (Full Integration Bench)', code: 'FIB', programId: 'p001', active: true },
  { id: 'tm004', name: 'SIMULATOR-A320', code: 'SIM320', programId: 'p001', active: true },
  { id: 'tm005', name: 'IRON BIRD', code: 'IRON', programId: 'p001', active: true },
  { id: 'tm006', name: 'SIB-LR (Long Range Bench)', code: 'SIBLR', programId: 'p002', active: true },
  { id: 'tm007', name: 'SIMULATOR-A350', code: 'SIM350', programId: 'p003', active: true },
];

export const DEMO_SYSTEMS = [
  { id: 'sys001', name: 'FMS (Flight Management System)', code: 'FMS', programId: 'p001', active: true },
  { id: 'sys002', name: 'AP/FD (Autopilot / Flight Director)', code: 'APFD', programId: 'p001', active: true },
  { id: 'sys003', name: 'FADEC (Full Authority Digital Engine Control)', code: 'FADEC', programId: 'p001', active: true },
  { id: 'sys004', name: 'ADIRS (Air Data Inertial Reference System)', code: 'ADIRS', programId: 'p001', active: true },
  { id: 'sys005', name: 'EFIS (Electronic Flight Instrument System)', code: 'EFIS', programId: 'p001', active: true },
  { id: 'sys006', name: 'TCAS (Traffic Collision Avoidance System)', code: 'TCAS', programId: 'p001', active: true },
  { id: 'sys007', name: 'TAWS (Terrain Awareness Warning System)', code: 'TAWS', programId: 'p001', active: true },
];

export const DEMO_ATA_CHAPTERS = [
  { id: 'ata001', name: 'ATA 22 - Auto Flight', code: '22', programId: 'p001', active: true },
  { id: 'ata002', name: 'ATA 23 - Communications', code: '23', programId: 'p001', active: true },
  { id: 'ata003', name: 'ATA 31 - Indicating / Recording Systems', code: '31', programId: 'p001', active: true },
  { id: 'ata004', name: 'ATA 34 - Navigation', code: '34', programId: 'p001', active: true },
  { id: 'ata005', name: 'ATA 42 - Integrated Modular Avionics', code: '42', programId: 'p001', active: true },
  { id: 'ata006', name: 'ATA 46 - Information Systems', code: '46', programId: 'p001', active: true },
  { id: 'ata007', name: 'ATA 73 - Engine Fuel and Control', code: '73', programId: 'p001', active: true },
];

export const DEMO_SUPPLIERS = [
  { id: 'sup001', name: 'Honeywell Aerospace', code: 'HON', programId: 'p001', systemId: 'sys001', active: true },
  { id: 'sup002', name: 'Thales Avionics', code: 'THA', programId: 'p001', systemId: 'sys001', active: true },
  { id: 'sup003', name: 'Collins Aerospace', code: 'COL', programId: 'p001', systemId: 'sys002', active: true },
  { id: 'sup004', name: 'Safran Electronics', code: 'SAF', programId: 'p001', systemId: 'sys003', active: true },
];

export const DEMO_FUNCTIONS = [
  { id: 'fn001', name: 'Lateral Guidance', code: 'LGUID', systemId: 'sys001', active: true },
  { id: 'fn002', name: 'Vertical Guidance', code: 'VGUID', systemId: 'sys001', active: true },
  { id: 'fn003', name: 'Flight Plan Management', code: 'FPLAN', systemId: 'sys001', active: true },
  { id: 'fn004', name: 'Performance Computation', code: 'PERF', systemId: 'sys001', active: true },
  { id: 'fn005', name: 'Navigation Database', code: 'NAVDB', systemId: 'sys001', active: true },
  { id: 'fn006', name: 'Autopilot Control Laws', code: 'APCL', systemId: 'sys002', active: true },
];

export const DEMO_REPORTER_TEAMS = [
  { id: 'rt001', name: 'LAB nFMS 1V', code: 'LAB_1V', active: true },
  { id: 'rt002', name: 'DO nFMS 1PYC', code: 'DO_1PYC', active: true },
  { id: 'rt003', name: 'Flight Test', code: 'FT', active: true },
  { id: 'rt004', name: 'Certification', code: 'CERT', active: true },
];

export const DEMO_DEFECT_ORIGINS = [
  { id: 'do001', name: 'Architecture', code: 'ARCH', active: true },
  { id: 'do002', name: 'Facilities', code: 'FAC', active: true },
  { id: 'do003', name: 'Instrumentation & Tools', code: 'INST', active: true },
  { id: 'do004', name: 'Simulation', code: 'SIM', active: true },
  { id: 'do005', name: 'Wiring', code: 'WIR', active: true },
  { id: 'do006', name: 'Hydraulic', code: 'HYD', active: true },
];

export const DEMO_VVO_DETAILS: Record<string, any> = Object.fromEntries(DEMO_VVOS.map(v => [v.id, {
  ...v,
  description: `Verify the ${v.summary.replace('Verify ', '').replace('Validate ', '').replace('Check ', '')}.\n\nThis VVO covers the end-to-end verification including nominal, degraded, and edge-case scenarios per the applicable system specification.`,
  priority: v.status === 'RELEASED' ? 'High' : 'Medium',
  reporterName: 'Design Office nFMS',
  vvoType: 'Functional',
  verificationMethod: 'Test',
  testLevel: 'System Integration',
  testCategory: v.summary.includes('lateral') || v.summary.includes('LAT') || v.summary.includes('DIR TO') ? 'Lateral Navigation' : 'Vertical Navigation',
  testMeanName: 'SIB-2 (System Integration Bench)',
  benchName: 'SIB-2 Toulouse',
  systemName: 'FMS (Flight Management System)',
  ataChapter: 'ATA 34 - Navigation',
  functionName: v.summary.includes('lateral') || v.summary.includes('LAT') ? 'Lateral Guidance' : 'Vertical Guidance',
  preConditions: 'FMS powered, navigation database loaded, flight plan active',
  passFailCriteria: 'All test steps pass. No deviation exceeds tolerance defined in specification.',
  testProcedureRef: `TP-NFMS-${v.id.replace('v0', '')}`,
  plannedDate: '2026-06-15',
  createdBy: 'System Admin',
  updatedAt: v.createdAt,
  requirementLinks: [
    { id: `rl-${v.id}-1`, requirementKey: v.idDoors || 'DOORS-pending', coverageStatus: v.status === 'RELEASED' ? 'COVERED' : 'PARTIALLY_COVERED' },
  ],
  linkedTests: [
    { id: `lt-${v.id}-1`, issueKey: `NFMSLAB-T${v.id.replace('v0', '')}1`, name: `Test: ${v.summary.slice(0, 50)}`, status: v.status === 'RELEASED' ? 'APPROVED' : 'DRAFT' },
    { id: `lt-${v.id}-2`, issueKey: `NFMSLAB-T${v.id.replace('v0', '')}2`, name: `Regression: ${v.summary.slice(0, 40)}`, status: 'DRAFT' },
  ],
}]));

export const DEMO_TECH_EVENT_DETAILS: Record<string, any> = Object.fromEntries(DEMO_TECH_EVENTS.map(te => [te.id, {
  ...te,
  description: `System anomaly detected during test execution.\n\n${te.summary}\n\nDetected on test bench during standard verification campaign.`,
  detectedOnDate: te.createdAt,
  detectedOnTestMean: 'SIB-2',
  defectType: 'Software',
  defectOrigin: 'Architecture',
  defectImpact: te.priority === 'Critical' ? 'Safety' : te.priority === 'High' ? 'Functional' : 'Operational',
  impactedSystem: 'FMS',
  impactedAta: 'ATA 34',
  impactedFunction: te.summary.includes('lateral') || te.summary.includes('LAT') ? 'Lateral Guidance' : 'Vertical Guidance',
  analysis: te.status !== 'Open' ? 'Root cause identified in flight plan computation module. Path constraint handler does not account for the reported edge case.' : '',
  resolution: te.status === 'Closed' || te.status === 'Resolved' ? 'Fix applied in FMS software build 3.2.1. Verified on SIB-2 bench.' : '',
  linkedBenchDefects: te.priority === 'Critical' ? [{ id: 'bd-1', issueKey: 'NFMSDEF-BD01', severity: 'High' }] : [],
  linkedProblemReports: te.priority === 'Critical' ? [{ id: 'pr-1', issueKey: 'NFMSDEF-PR01', prType: 'Significant MAJ' }] : [],
  linkedChangeCards: te.status === 'Classified' || te.status === 'Resolved' || te.status === 'Closed' ? [{ id: 'cc001', issueKey: 'CC-2401', title: 'Lateral guidance rework' }] : [],
}]));

export const DEMO_DASHBOARD = {
  projectId: pid,
  vvoMetrics: { total: 12, newCount: 2, verifiedCount: 2, releasedCount: 5 },
  techEventMetrics: { total: 8, openCount: 2, blockingCount: 1 },
  benchDefectMetrics: { total: 4, blockingCount: 1 },
  problemReportMetrics: { total: 3, openCount: 1 },
  coveragePercentage: 72,
  vvoStatusDistribution: { NEW: 2, TO_BE_VERIFIED: 2, VERIFIED: 2, RELEASED: 5, CANCELLED: 1, SUPERSEDED: 1 } as Record<string, number>,
  techEventTrend: [
    { label: 'Jan', count: 3 }, { label: 'Feb', count: 5 }, { label: 'Mar', count: 2 },
    { label: 'Apr', count: 4 }, { label: 'May', count: 1 }, { label: 'Jun', count: 3 },
  ],
  benchDefectSeverity: { Blocking: 1, High: 2, Low: 1 } as Record<string, number>,
  recentActivity: [
    { id: 'a1', type: 'VVO', summary: 'NFMSDO-111 verified by A. Chen', timestamp: '2026-03-20T14:30:00Z', user: 'A. Chen' },
    { id: 'a2', type: 'TECH_EVENT', summary: 'NFMSDEF-306 opened: Fuel calc discrepancy', timestamp: '2026-03-19T10:00:00Z', user: 'S. Dupont' },
    { id: 'a3', type: 'VVO', summary: 'NFMSDO-110 created: Lateral revision CF/TF/DF', timestamp: '2026-03-18T07:30:00Z', user: 'S. Dupont' },
    { id: 'a4', type: 'TECH_EVENT', summary: 'NFMSDEF-307 assigned to resolver team', timestamp: '2026-03-17T13:40:00Z', user: 'A. Chen' },
    { id: 'a5', type: 'CHANGE_CARD', summary: 'CC-2401 design review RAG set to Amber', timestamp: '2026-03-16T09:15:00Z', user: 'P. Martin' },
  ],
};

// ═══════ IFCS Issue Types ═══════

export const DEMO_VVM_CARDS = [
  {
    id: 'vvm001', issueKey: 'IFCS-17653', summary: 'VVM / IFCS - SA / FMS data treatment V0 - X1.2.0',
    status: 'CIA_FROZEN', projectId: pid, assigneeName: 'G. Bourdet', createdAt: '2026-04-15T14:34:00Z',
    team: 'AFS', applicability: 'IFCS - SA', acType: 'SA', acVersion: 'A320-NEO, A321-ACF, A321-XLR',
    engineType: 'CFM, PW', computer: 'IFCC', scope: 'AFS_FMS_Treatment',
    pipelineStatus: 'Scope: KO, Integrity: KO, ValidMat: KO, CIA: KO',
    ltrReference: 'SA27RP200053T',
    expertReview: 'Done', testingReview: 'To do', safetyReview: 'To do',
    validationCount: 4, verificationCount: 2,
  },
  {
    id: 'vvm002', issueKey: 'IFCS-12704', summary: 'VVM / IFCS - SA / [PFCS_AirbrakeLever] V0 - RELEASE_1.0.4',
    status: 'CIA_FROZEN', projectId: pid, assigneeName: 'C. Jaupître', createdAt: '2026-02-23T14:21:00Z',
    team: 'PFCS', applicability: 'IFCS - SA', acType: 'SA', acVersion: 'A320-NEO, A321-ACF, A321-XLR',
    engineType: 'None', computer: 'IFCC', scope: 'PFCS_AirbrakeLever',
    pipelineStatus: 'Scope: TO DO, Integrity: TO DO, ValidMat: TO DO, CIA: TO DO',
    ltrReference: 'SA27RP200053T',
    expertReview: 'Done', testingReview: 'None', safetyReview: 'To do',
    validationCount: 11, verificationCount: 18,
  },
  {
    id: 'vvm003', issueKey: 'IFCS-14158', summary: 'VVM / SA / MMR function X1.0.0 traceability & requirements',
    status: 'IN_PROGRESS', projectId: pid, assigneeName: 'G. Bourdet', createdAt: '2025-09-15T16:52:00Z',
    team: 'AFS', applicability: 'IFCS - SA', acType: 'SA', acVersion: 'A320-NEO',
    engineType: 'CFM', computer: 'IFCC', scope: 'AFS_MMR_Function',
    pipelineStatus: '',
    ltrReference: '',
    expertReview: 'To do', testingReview: 'To do', safetyReview: 'To do',
    validationCount: 0, verificationCount: 0,
  },
];

export const DEMO_IVV_CARDS = [
  { id: 'ivv001', issueKey: 'IFCS-16808', summary: 'IVV Validation / SA / AbLever_101_3515 / No Variant Specified / 2', status: 'BACKLOG', ivvType: 'VALIDATION', vvmCardId: 'vvm002', requirementImpact: 'AbLever_101_3515', level: 'L3', partitionName: 'AccuratePFC', testsStatus: 'UNCOVERED', assigneeName: 'Unassigned', createdAt: '2026-02-28T17:30:00Z' },
  { id: 'ivv002', issueKey: 'IFCS-16809', summary: 'IVV Validation / SA / AbLever_101_3516 / No Variant Specified / 2', status: 'BACKLOG', ivvType: 'VALIDATION', vvmCardId: 'vvm002', requirementImpact: 'AbLever_101_3516', level: 'L3', partitionName: 'AccuratePFC', testsStatus: 'UNCOVERED', assigneeName: 'Unassigned', createdAt: '2026-02-28T17:30:00Z' },
  { id: 'ivv003', issueKey: 'IFCS-16825', summary: 'IVV Verification / SA / AbLever_197_2590 / No Variant Specified / 1', status: 'BACKLOG', ivvType: 'VERIFICATION', vvmCardId: 'vvm002', requirementImpact: 'AbLever_197_2590', level: 'L2', partitionName: 'AccuratePFC', testsStatus: 'UNCOVERED', assigneeName: 'Unassigned', createdAt: '2026-02-24T10:53:00Z' },
  { id: 'ivv004', issueKey: 'IFCS-16828', summary: 'IVV Verification / SA / AbLever_146_3246 / No Variant Specified / 1', status: 'BACKLOG', ivvType: 'VERIFICATION', vvmCardId: 'vvm002', requirementImpact: 'AbLever_146_3246', level: 'L2', partitionName: 'AccuratePFC', testsStatus: 'UNCOVERED', assigneeName: 'J. Penado', createdAt: '2026-02-24T10:53:00Z' },
  { id: 'ivv005', issueKey: 'IFCS-16829', summary: 'IVV Verification / SA / AbLever_198_3185 / No Variant Specified / 1', status: 'PLANNED', ivvType: 'VERIFICATION', vvmCardId: 'vvm002', requirementImpact: 'AbLever_198_3185', level: 'L1', partitionName: 'AccuratePFC', testsStatus: 'UNCOVERED', assigneeName: 'Unassigned', createdAt: '2026-02-24T10:53:00Z' },
  { id: 'ivv006', issueKey: 'NIST-2', summary: 'IVV Verification / SA / AbLever_177_2522 / No Variant Specified / 4', status: 'BACKLOG', ivvType: 'VERIFICATION', vvmCardId: 'vvm002', requirementImpact: 'AbLever_177_2522', level: 'L4', partitionName: 'AccuratePFC', testsStatus: 'UNCOVERED', assigneeName: 'Unassigned', createdAt: '2026-02-24T10:53:00Z' },
  { id: 'ivv007', issueKey: 'NIST-3', summary: 'IVV Verification / SA / AbLever_178_2699 / No Variant Specified / 4', status: 'BACKLOG', ivvType: 'VERIFICATION', vvmCardId: 'vvm002', requirementImpact: 'AbLever_178_2699', level: 'L4', partitionName: 'AccuratePFC', testsStatus: 'UNCOVERED', assigneeName: 'Unassigned', createdAt: '2026-02-24T10:53:00Z' },
  { id: 'ivv008', issueKey: 'IFCS-16830', summary: 'IVV Verification / SA / AbLever_83_1840 / No Variant Specified / 1', status: 'DONE', ivvType: 'VERIFICATION', vvmCardId: 'vvm002', requirementImpact: 'AbLever_83_1840', level: 'L1', partitionName: 'AccuratePFC', testsStatus: 'COVERED', assigneeName: 'S. Sausa', createdAt: '2026-02-24T10:53:00Z' },
];

export const DEMO_GROUPS = [
  { id: 'grp001', issueKey: 'IFCS-10647', summary: 'PFCS BITE & Maintenance', status: 'IN_PROGRESS', impactedTeam: 'PFCS', childDeliverableCount: 3, assigneeName: 'Unassigned', createdAt: '2025-12-23T14:11:00Z' },
  { id: 'grp002', issueKey: 'IFCS-821', summary: 'Functions', status: 'TO_DO', impactedTeam: 'PFCS', childDeliverableCount: 5, assigneeName: 'Unassigned', createdAt: '2025-12-23T14:11:00Z' },
  { id: 'grp003', issueKey: 'IFCS-770', summary: 'MG3', status: 'DONE', impactedTeam: 'PFCS', childDeliverableCount: 2, assigneeName: 'Unassigned', createdAt: '2021-10-15T00:00:00Z' },
  { id: 'grp004', issueKey: 'IFCS-911', summary: 'System Reviews Single Aisle', status: 'IN_PROGRESS', impactedTeam: 'All', childDeliverableCount: 15, assigneeName: 'Unassigned', createdAt: '2022-03-15T00:00:00Z' },
];

export const DEMO_SUB_CHANGES = [
  { id: 'sc001', issueKey: 'IFCS-17728', summary: 'DFS - correct Defect', status: 'IN_PROGRESS', parentChangeCardId: 'cc001', gitBranch: 'IFCS-17728_DFS_correct', prStatus: 'OPEN', assigneeName: 'E. Stark', createdAt: '2026-04-20T10:00:00Z' },
  { id: 'sc002', issueKey: 'IFCS-15099', summary: 'TOM - First implementation', status: 'IN_REVIEW', parentChangeCardId: 'cc001', gitBranch: 'IFCS-15099_TOM_impl', prStatus: 'REVIEW', assigneeName: 'E. Stark', createdAt: '2026-03-15T09:00:00Z' },
  { id: 'sc003', issueKey: 'IFCS-17636', summary: 'MT PFCS - P0 - 1.2.0', status: 'TO_DO', parentChangeCardId: 'cc001', gitBranch: '', prStatus: '', assigneeName: 'R. Caravaca', createdAt: '2026-04-18T14:00:00Z' },
];
