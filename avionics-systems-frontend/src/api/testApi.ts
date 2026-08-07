import axiosClient from './axiosClient';

export interface TestStep {
  id?: string;
  index: number;
  description: string;
  expectedResult: string;
  testData?: string;
}

export interface CreateTestRequest {
  projectId: string;
  name: string;
  description?: string;
  priority?: string;
  labels?: string[];
  precondition?: string;
  testType?: 'MANUAL' | 'AUTOMATED' | 'BDD';
  steps?: TestStep[];
  requirementKeys?: string[];
  folderId?: string;
  gherkinFeatureKey?: string;
  gherkinScenarioId?: string;
}

export interface UpdateTestRequest {
  name?: string;
  description?: string;
  priority?: string;
  labels?: string[];
  precondition?: string;
  testType?: 'MANUAL' | 'AUTOMATED' | 'BDD';
  testStatus?: 'DRAFT' | 'READY' | 'APPROVED' | 'DEPRECATED';
  steps?: TestStep[];
  requirementKeys?: string[];
  folderId?: string;
}

export interface TestResponse {
  id: string;
  issueKey: string;
  projectId: string;
  name: string;
  description?: string;
  status: string;
  priority?: string;
  labels: string[];
  testType: string;
  testStatus: string;
  testPriority?: string;
  testSteps: TestStep[];
  requirementKeys: string[];
  gherkinFeatureKey?: string;
  gherkinScenarioId?: string;
  testOwnerId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TestSetRequest {
  projectId: string;
  name: string;
  description?: string;
  folderId?: string;
  testIds?: string[];
}

export interface TestSetResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  status: string;
  testCount: number;
  lastExecutedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TestPlanRequest {
  projectId: string;
  name: string;
  description?: string;
  testSetIds?: string[];
  testCycle?: string;
  testEnv?: string;
  startDate?: string;
  endDate?: string;
}

export interface TestPlanResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  status: string;
  testSetCount: number;
  totalTests: number;
  testCycle?: string;
  testEnv?: string;
  startDate?: string;
  endDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TestExecutionRequest {
  testPlanId?: string;
  testSetId?: string;
  testIds?: string[];
  name?: string;
  description?: string;
  testEnv?: string;
  testCycle?: string;
  assigneeId?: string;
}

export interface StepResultRequest {
  stepIndex: number;
  status: 'PASSED' | 'FAILED' | 'BLOCKED' | 'SKIPPED' | 'UNTESTED';
  comment?: string;
  evidenceUrls?: string[];
  defectKey?: string;
}

export interface TestExecutionResponse {
  id: string;
  testPlanId?: string;
  testSetId?: string;
  testId: string;
  issueKey: string;
  name: string;
  status: string;
  assigneeId?: string;
  testEnv?: string;
  testCycle?: string;
  startedAt?: string;
  finishedAt?: string;
  duration?: number;
  createdAt?: string;
  executedBy?: string;
  stepResults?: StepResultResponse[];
}

export interface StepResultResponse {
  stepIndex: number;
  description: string;
  expectedResult: string;
  status: string;
  comment?: string;
  evidenceUrls?: string[];
  defectKey?: string;
}

export interface RequirementLinkRequest {
  testId: string;
  requirementKey: string;
  coverageStatus?: 'COVERED' | 'PARTIALLY_COVERED' | 'NOT_COVERED';
}

export interface RequirementLinkResponse {
  id: string;
  testIssueId: string;
  testIssueKey: string;
  requirementKey: string;
  coverageStatus: string;
  lastExecutionStatus?: string;
}

export interface TestEnvironmentRequest {
  projectId: string;
  name: string;
  description?: string;
  environmentType?: string;
  url?: string;
  config?: Record<string, string>;
  variables?: Record<string, string>;
  credentials?: Record<string, string>;
}

export interface TestEnvironmentResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  environmentType: string;
  url?: string;
  config?: Record<string, string>;
  variables?: Record<string, string>;
  sortOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TestImportRequest {
  projectId: string;
  importType: 'CUCUMBER' | 'JUNIT';
  fileName: string;
}

export interface TestImportResponse {
  id: string;
  projectId: string;
  importType: string;
  status: string;
  totalTests: number;
  passedTests: number;
  failedTests: number;
  totalSteps: number;
  fileName: string;
  createdAt: string;
  finishedAt?: string;
  errorMessage?: string;
}

export interface TraceabilityMatrixResponse {
  requirementKey: string;
  testCount: number;
  coverageStatus: string;
  tests: {
    testId: string;
    issueKey: string;
    name: string;
    status?: string;
    lastExecutionStatus?: string;
  }[];
}

export interface TestSummaryReport {
  projectId?: string;
  totalTests: number;
  totalTestSets: number;
  totalTestPlans: number;
  totalExecutions: number;
  overallPassRate?: number;
  passRate?: number;
  failRate?: number;
  blockedRate?: number;
  skippedRate?: number;
  testsPassed?: number;
  testsFailed?: number;
  testsBlocked?: number;
  testsNotRun?: number;
  executionsByStatus?: Record<string, number>;
  topFailingTests?: {
    issueKey: string;
    name: string;
    failureCount: number;
  }[];
  recentExecutions?: TestExecutionResponse[];
}

// Test API
const testApi = {
  // Tests
  createTest: (data: CreateTestRequest): Promise<TestResponse> =>
    axiosClient.post('/api/tests', data).then(r => r.data),

  getTest: (testId: string): Promise<TestResponse> =>
    axiosClient.get(`/api/tests/${testId}`).then(r => r.data),

  updateTest: (testId: string, data: UpdateTestRequest): Promise<TestResponse> =>
    axiosClient.put(`/api/tests/${testId}`, data).then(r => r.data),

  deleteTest: (testId: string): Promise<void> =>
    axiosClient.delete(`/api/tests/${testId}`).then(r => r.data),

  searchTests: (params: {
    projectId?: string;
    folderId?: string;
    testType?: string;
    testStatus?: string;
    requirementKey?: string;
    search?: string;
  }): Promise<TestResponse[]> =>
    axiosClient.get('/api/tests/project/' + params.projectId, { params }).then(r => r.data),

  // Test Sets
  createTestSet: (data: TestSetRequest): Promise<TestSetResponse> =>
    axiosClient.post('/api/test-sets', data).then(r => r.data),

  getTestSet: (testSetId: string): Promise<TestSetResponse> =>
    axiosClient.get(`/api/test-sets/${testSetId}`).then(r => r.data),

  updateTestSet: (testSetId: string, data: Partial<TestSetRequest>): Promise<TestSetResponse> =>
    axiosClient.put(`/api/test-sets/${testSetId}`, data).then(r => r.data),

  deleteTestSet: (testSetId: string): Promise<void> =>
    axiosClient.delete(`/api/test-sets/${testSetId}`).then(r => r.data),

  getTestSetsByProject: (projectId: string): Promise<TestSetResponse[]> =>
    axiosClient.get(`/api/test-sets/project/${projectId}`).then(r => r.data),

  addTestsToSet: (testSetId: string, testIds: string[]): Promise<void> =>
    axiosClient.post(`/api/test-sets/${testSetId}/tests`, { testIds }).then(r => r.data),

  removeTestsFromSet: (testSetId: string, testIds: string[]): Promise<void> =>
    axiosClient.delete(`/api/test-sets/${testSetId}/tests`, { data: { testIds } }).then(r => r.data),

  // Test Plans
  createTestPlan: (data: TestPlanRequest): Promise<TestPlanResponse> =>
    axiosClient.post('/api/test-plans', data).then(r => r.data),

  getTestPlan: (testPlanId: string): Promise<TestPlanResponse> =>
    axiosClient.get(`/api/test-plans/${testPlanId}`).then(r => r.data),

  updateTestPlan: (testPlanId: string, data: Partial<TestPlanRequest>): Promise<TestPlanResponse> =>
    axiosClient.put(`/api/test-plans/${testPlanId}`, data).then(r => r.data),

  deleteTestPlan: (testPlanId: string): Promise<void> =>
    axiosClient.delete(`/api/test-plans/${testPlanId}`).then(r => r.data),

  getTestPlansByProject: (projectId: string): Promise<TestPlanResponse[]> =>
    axiosClient.get(`/api/test-plans/project/${projectId}`).then(r => r.data),

  startTestPlan: (testPlanId: string, userId?: string): Promise<TestExecutionResponse[]> =>
    axiosClient.post(`/api/test-plans/${testPlanId}/start`).then(r => r.data),

  // Test Executions
  createExecution: (data: TestExecutionRequest): Promise<TestExecutionResponse> =>
    axiosClient.post('/api/test-executions', data).then(r => r.data),

  getExecution: (executionId: string): Promise<TestExecutionResponse> =>
    axiosClient.get(`/api/test-executions/${executionId}`).then(r => r.data),

  getExecutionsByTest: (testId: string): Promise<TestExecutionResponse[]> =>
    axiosClient.get(`/api/test-executions/test/${testId}`).then(r => r.data),

  getExecutionsByPlan: (testPlanId: string): Promise<TestExecutionResponse[]> =>
    axiosClient.get(`/api/test-executions/plan/${testPlanId}`).then(r => r.data),

  startExecution: (executionId: string, userId?: string): Promise<TestExecutionResponse> =>
    axiosClient.post(`/api/test-executions/${executionId}/start`).then(r => r.data),

  completeExecution: (executionId: string, status?: string): Promise<TestExecutionResponse> =>
    axiosClient.post(`/api/test-executions/${executionId}/complete`, { status }).then(r => r.data),

  addStepResult: (executionId: string, data: StepResultRequest): Promise<TestExecutionResponse> =>
    axiosClient.put(`/api/test-executions/${executionId}/steps`, data).then(r => r.data),

  getExecutionHistory: (testId: string): Promise<{
    issueKey: string;
    executions: TestExecutionResponse[];
  }[]> =>
    axiosClient.get(`/api/test-executions/history/${testId}`).then(r => r.data),

  // Requirement Links
  createRequirementLink: (data: RequirementLinkRequest): Promise<RequirementLinkResponse> =>
    axiosClient.post('/api/requirements/links', data).then(r => r.data),

  getRequirementLinks: (testId: string): Promise<RequirementLinkResponse[]> =>
    axiosClient.get(`/api/requirements/links/test/${testId}`).then(r => r.data),

  getRequirementCoverage: (requirementKey: string): Promise<TraceabilityMatrixResponse> =>
    axiosClient.get(`/api/requirements/coverage/${requirementKey}`).then(r => r.data),

  removeRequirementLink: (linkId: string): Promise<void> =>
    axiosClient.delete(`/api/requirements/links/${linkId}`).then(r => r.data),

  // Traceability Matrix
  getTraceabilityMatrix: (projectId: string): Promise<TraceabilityMatrixResponse[]> =>
    axiosClient.get(`/api/traceability/matrix`, { params: { projectId } }).then(r => r.data),

  // Test Environments
  createTestEnvironment: (projectId: string, data: TestEnvironmentRequest): Promise<TestEnvironmentResponse> =>
    axiosClient.post(`/api/test-environments?projectId=${projectId}`, data).then(r => r.data),

  getTestEnvironments: (projectId: string): Promise<TestEnvironmentResponse[]> =>
    axiosClient.get(`/api/test-environments?projectId=${projectId}`).then(r => r.data),

  getTestEnvironment: (envId: string): Promise<TestEnvironmentResponse> =>
    axiosClient.get(`/api/test-environments/${envId}`).then(r => r.data),

  updateTestEnvironment: (envId: string, data: Partial<TestEnvironmentRequest>): Promise<TestEnvironmentResponse> =>
    axiosClient.put(`/api/test-environments/${envId}`, data).then(r => r.data),

  deleteTestEnvironment: (envId: string): Promise<void> =>
    axiosClient.delete(`/api/test-environments/${envId}`).then(r => r.data),

  // Import
  importCucumber: (projectId: string, file: File): Promise<TestImportResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.post(`/api/import/cucumber/file?projectId=${projectId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },

  importJUnit: (projectId: string, file: File): Promise<TestImportResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.post(`/api/import/junit/file?projectId=${projectId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },

  getImportStatus: (importId: string): Promise<TestImportResponse> =>
    axiosClient.get(`/api/import/status/${importId}`).then(r => r.data),

  getImportHistory: (projectId: string): Promise<TestImportResponse[]> =>
    axiosClient.get(`/api/import/history/${projectId}`).then(r => r.data),

  // Reports
  getTestSummary: (projectId: string): Promise<TestSummaryReport> =>
    axiosClient.get(`/api/reports/summary?projectId=${projectId}`).then(r => r.data),

  getTestTrend: (testId: string, days?: number): Promise<{ date: string; passRate: number }[]> =>
    axiosClient.get(`/api/reports/trend/${testId}`, { params: { days } }).then(r => r.data),

  getCoverageReport: (projectId: string): Promise<{ requirement: string; coverage: number; tests: number }[]> =>
    axiosClient.get(`/api/reports/coverage?projectId=${projectId}`).then(r => r.data),

  // Test Settings
  getTestSettings: (projectId: string): Promise<Record<string, unknown>> =>
    axiosClient.get(`/api/test-settings?projectId=${projectId}`).then(r => r.data),

  saveTestSettings: (projectId: string, settings: Record<string, unknown>): Promise<void> =>
    axiosClient.put(`/api/test-settings?projectId=${projectId}`, settings).then(r => r.data),
};

// ==================== DATASET API ====================

export interface CreateDatasetRequest {
  projectId: string;
  name: string;
  description?: string;
  dataFormat?: 'TABULAR' | 'CSV' | 'JSON';
  columnNames?: string[];
  columnTypes?: string[];
  rows?: string[][];
  csvData?: string;
  jsonData?: string;
  folderId?: string;
}

export interface UpdateDatasetRequest {
  name?: string;
  description?: string;
  columnNames?: string[];
  columnTypes?: string[];
  rows?: string[][];
  csvData?: string;
  jsonData?: string;
  isImmutable?: boolean;
}

export interface DatasetResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  dataFormat: string;
  columnNames?: string[];
  columnTypes?: string[];
  rowCount: number;
  version: number;
  isImmutable: boolean;
  folderId?: string;
  rows?: string[][];
  totalVersions?: number;
  versions?: DatasetVersionResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface DatasetVersionResponse {
  id: string;
  datasetId: string;
  versionNumber: number;
  columnNames?: string[];
  columnTypes?: string[];
  data?: string[][];
  rowCount: number;
  changeSummary?: string;
  createdBy?: string;
  isImmutable: boolean;
  createdAt: string;
}

export interface BindDatasetRequest {
  testId: string;
  datasetId: string;
  datasetVersionId?: string;
  columnMappings?: Record<string, string>;
}

export interface DatasetBindingResponse {
  id: string;
  testId: string;
  testIssueKey?: string;
  datasetId: string;
  datasetName?: string;
  datasetVersionId?: string;
  datasetVersion?: number;
  boundColumns?: string[];
  rowCount: number;
  createdBy?: string;
  createdAt?: string;
}

// ==================== SHARED STEP API ====================

export interface SharedStepDto {
  order?: number;
  stepType?: string;
  description: string;
  expectedResult?: string;
  parameters?: Record<string, string>;
  attachments?: string[];
}

export interface CreateSharedStepRequest {
  projectId: string;
  name: string;
  description?: string;
  steps: SharedStepDto[];
  folderId?: string;
}

export interface SharedStepResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  steps: SharedStepDto[];
  currentVersion: number;
  usageCount: number;
  folderId?: string;
  versions?: SharedStepVersionResponse[];
  impact?: SharedStepImpactResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface SharedStepVersionResponse {
  id: string;
  sharedStepId: string;
  versionNumber: number;
  steps: SharedStepDto[];
  changeSummary?: string;
  createdBy?: string;
  isCurrent: boolean;
  createdAt: string;
}

export interface SharedStepImpactResponse {
  testId: string;
  testIssueKey?: string;
  testName?: string;
  usageCount: number;
  lastUsedAt?: string;
  status?: string;
}

export interface InsertSharedStepRequest {
  testId: string;
  sharedStepId: string;
  position: number;
  parameters?: Record<string, string>;
  sharedStepVersionId?: string;
}

export interface EmbeddedStepResponse {
  id: string;
  testId: string;
  stepIndex: number;
  sharedStepId: string;
  sharedStepName?: string;
  sharedStepVersion?: number;
  embeddedSteps?: SharedStepDto[];
  createdAt?: string;
}

// ==================== IMPACT ANALYSIS API ====================

export interface ComponentRequest {
  projectId: string;
  componentName: string;
  componentPath?: string;
  ownershipTeam?: string;
  ownershipContact?: string;
  metadata?: Record<string, unknown>;
}

export interface ComponentResponse {
  id: string;
  projectId: string;
  componentName: string;
  componentPath?: string;
  ownershipTeam?: string;
  ownershipContact?: string;
  metadata?: Record<string, unknown>;
  createdAt: string;
}

export interface TestComponentMappingRequest {
  testId: string;
  componentId: string;
  confidenceScore?: number;
  mappingType?: 'direct' | 'indirect' | 'ai-suggested';
}

export interface ImpactAnalysisRequest {
  projectId: string;
  triggerType?: 'commit' | 'pr' | 'manual' | 'schedule';
  commitSha?: string;
  commitMessage?: string;
  changedFiles?: string[];
  prId?: string;
  branch?: string;
}

export interface TestImpactDto {
  testId: string;
  testIssueKey?: string;
  testName?: string;
  impactLevel?: 'HIGH' | 'MEDIUM' | 'LOW';
  riskScore?: number;
  reason?: string;
}

export interface ImpactAnalysisResponse {
  id: string;
  projectId: string;
  triggerType: string;
  triggerId?: string;
  affectedTests: TestImpactDto[];
  suggestedSuites?: string[];
  riskScore?: number;
  confidenceScore?: number;
  analyzedBy?: string;
  createdAt: string;
}

// ==================== FLAKY TEST API ====================

export interface FlakyTestResponse {
  testId: string;
  testIssueKey?: string;
  testName?: string;
  totalExecutions: number;
  totalFailures: number;
  totalPasses: number;
  flakyScore: number;
  passRateTrend?: 'improving' | 'stable' | 'degrading';
  firstFlakyOccurrence?: string;
  lastFlakyOccurrence?: string;
  currentStatus?: 'stable' | 'flaky' | 'quarantine_candidate';
  confidenceLevel?: number;
  patterns?: FlakyPatternResponse[];
  recentExecutions?: ExecutionRecordResponse[];
}

export interface FlakyPatternResponse {
  id: string;
  testId: string;
  patternType: string;
  patternDescription?: string;
  frequencyScore?: number;
  affectedEnvironments?: string[];
  affectedBuilds?: string[];
  rootCauseCategory?: string;
  suggestedFix?: string;
  confidenceScore?: number;
  createdAt: string;
}

export interface ExecutionRecordResponse {
  id: string;
  executionId: string;
  testId: string;
  isFlakyExecution?: boolean;
  failureReason?: string;
  environmentId?: string;
  executionDurationMs?: number;
  retryAttempt?: number;
  lastStatus?: string;
  analyzedAt: string;
}

export interface FlakyDashboardResponse {
  totalTestsAnalyzed: number;
  stableCount: number;
  flakyCount: number;
  quarantineCandidateCount: number;
  averageFlakyScore?: number;
  topFlakyTests?: FlakyTestResponse[];
  patternsByType?: Record<string, number>;
}

// ==================== QUARANTINE API ====================

export interface QuarantineRequest {
  testId: string;
  status?: 'candidate' | 'quarantined' | 'investigation';
  quarantineReason?: string;
  triggerType?: 'auto_flaky' | 'auto_failing' | 'manual';
  autoRestoreEnabled?: boolean;
  autoRestoreConditions?: Record<string, unknown>;
}

export interface QuarantineResponse {
  id: string;
  testId: string;
  testIssueKey?: string;
  testName?: string;
  status: string;
  quarantineReason?: string;
  triggerType?: string;
  triggeredBy?: string;
  triggeredAt: string;
  autoRestoreEnabled?: boolean;
  autoRestoreConditions?: Record<string, unknown>;
  currentExecutionCount?: number;
  currentPassCount?: number;
  lastExecutionAt?: string;
  lastStatus?: string;
  restoredAt?: string;
  restoredBy?: string;
  restoreReason?: string;
  createdAt?: string;
}

export interface QuarantineDashboardResponse {
  totalQuarantined: number;
  quarantinedCount: number;
  investigationCount: number;
  candidateCount: number;
  restoredThisWeek: number;
  averageQuarantineDurationDays?: number;
  byTriggerType?: Record<string, number>;
  recentQuarantined?: QuarantineResponse[];
  readyForRestore?: QuarantineResponse[];
}

export interface QuarantineRuleRequest {
  projectId: string;
  ruleName: string;
  ruleType: 'flaky_threshold' | 'failure_streak' | 'environment';
  conditions: Record<string, unknown>;
  autoQuarantine?: boolean;
  notifyOnTrigger?: boolean;
}

export interface QuarantineRuleResponse {
  id: string;
  projectId: string;
  ruleName: string;
  ruleType: string;
  conditions: Record<string, unknown>;
  autoQuarantine?: boolean;
  notifyOnTrigger?: boolean;
  isActive?: boolean;
  createdBy?: string;
  createdAt: string;
}

// ==================== ADVANCED FEATURES API ====================

export interface TimelineEventResponse {
  id: string;
  eventType: string;
  eventTimestamp: string;
  stepIndex?: number;
  screenshotPath?: string;
  logEntries?: string[];
  eventData?: unknown;
}

export interface ReplaySessionResponse {
  sessionId: string;
  playbackPositionMs: number;
  isPlaying: boolean;
}

export interface VersionDiffResponse {
  versionA?: { number: number; date: string };
  versionB?: { number: number; date: string };
  changes: unknown[];
  summary: { added: number; removed: number; modified: number };
}

// ==================== EVIDENCE MANAGEMENT API ====================

export interface EvidenceUploadRequest {
  executionId: string;
  stepResultId?: string;
  evidenceType: string; // SCREENSHOT, VIDEO, LOG, HAR, PDF, FILE, COMMENT
  classificationLevel?: string; // STEP_LEVEL, RUN_LEVEL, ENVIRONMENT_LEVEL
  fileName?: string;
  fileSize?: number;
  mimeType?: string;
  url?: string;
  content?: string;
  metadata?: Record<string, string>;
  retentionPolicyId?: string;
  createdBy?: string;
}

export interface EvidenceResponse {
  id: string;
  executionId: string;
  stepResultId?: string;
  evidenceType: string;
  classificationLevel?: string;
  fileName?: string;
  filePath?: string;
  fileSize?: number;
  mimeType?: string;
  url: string;
  thumbnailUrl?: string;
  content?: string;
  metadata?: Record<string, string>;
  retentionPolicyId?: string;
  retentionPolicyName?: string;
  isArchived?: boolean;
  archivedAt?: string;
  createdBy?: string;
  createdAt: string;
}

export interface EvidenceClassificationRequest {
  evidenceId: string;
  classificationLevel: string;
  classificationReason?: string;
}

export interface RetentionPolicyRequest {
  projectId?: string;
  policyName: string;
  description?: string;
  evidenceType?: string;
  retentionDays?: number;
  compressionEnabled?: boolean;
  autoArchive?: boolean;
  moveToColdStorage?: boolean;
  coldStorageAfterDays?: number;
  permanentDelete?: boolean;
  deleteAfterDays?: number;
  createdBy?: string;
}

export interface RetentionPolicyResponse {
  id: string;
  projectId?: string;
  policyName: string;
  description?: string;
  evidenceType?: string;
  retentionDays?: number;
  compressionEnabled?: boolean;
  autoArchive?: boolean;
  moveToColdStorage?: boolean;
  coldStorageAfterDays?: number;
  permanentDelete?: boolean;
  deleteAfterDays?: number;
  isActive?: boolean;
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface EvidenceViewerData {
  executionId: string;
  executionKey?: string;
  evidenceGroups: EvidenceGroup[];
  totalCount: number;
  countByType: Record<string, number>;
  countByLevel: Record<string, number>;
}

export interface EvidenceGroup {
  groupKey: string;
  groupLabel: string;
  evidenceLevel: string;
  evidences: EvidenceResponse[];
}

// ==================== ENVIRONMENT MATRIX API ====================

export interface DimensionConfig {
  name: string;
  values: string[];
  type?: string; // SINGLE_SELECT, MULTI_SELECT
}

export interface FilterRule {
  type: string; // INCLUDE, EXCLUDE
  dimension: string;
  values: string[];
}

export interface ConflictRule {
  ruleName: string;
  type: string; // INCOMPATIBLE, MUTUALLY_EXCLUSIVE
  conflicts: Record<string, string[]>;
}

export interface MatrixConfigurationRequest {
  projectId: string;
  name: string;
  description?: string;
  dimensions: DimensionConfig[];
  filterRules?: FilterRule[];
  conflictRules?: ConflictRule[];
  createdBy?: string;
}

export interface MatrixConfigurationResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  dimensions: DimensionConfig[];
  filterRules?: FilterRule[];
  conflictRules?: ConflictRule[];
  totalCombinations: number;
  validCombinations: number;
  invalidCombinations: number;
  isActive?: boolean;
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ValidationError {
  rule: string;
  details: string;
  affectedDimensions?: string;
}

export interface CombinationResponse {
  id: string;
  matrixId: string;
  combinationIndex: number;
  combinationData: Record<string, string>;
  isValid: boolean;
  validationErrors?: ValidationError[];
  provisionedConfig?: Record<string, unknown>;
  provisioningStatus?: string;
  provisionedAt?: string;
  provisioningError?: string;
  createdAt: string;
}

export interface ProvisioningRuleRequest {
  projectId?: string;
  ruleName: string;
  description?: string;
  providerType: string; // BROWSERSTACK, SAUCELABS, KUBERNETES, DOCKER, LOCAL
  providerConfig?: Record<string, unknown>;
  provisioningScript?: string;
  capabilitiesTemplate?: Record<string, unknown>;
  environmentTemplate?: Record<string, string>;
  maxConcurrent?: number;
  timeoutSeconds?: number;
  retryCount?: number;
  priority?: number;
  createdBy?: string;
}

export interface ProvisioningRuleResponse {
  id: string;
  projectId?: string;
  ruleName: string;
  description?: string;
  providerType: string;
  providerConfig?: Record<string, unknown>;
  provisioningScript?: string;
  capabilitiesTemplate?: Record<string, unknown>;
  environmentTemplate?: Record<string, string>;
  maxConcurrent?: number;
  timeoutSeconds?: number;
  retryCount?: number;
  priority?: number;
  isActive?: boolean;
  createdBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface EnvironmentProvisionRequest {
  matrixId?: string;
  combinationId: string;
  provisioningRuleId?: string;
  testExecutionId?: string;
  requestedBy?: string;
}

export interface ProvisionResponse {
  combinationId: string;
  provisioningRuleId?: string;
  providerType?: string;
  provisionedConfig?: Record<string, unknown>;
  environmentVariables?: Record<string, string>;
  accessUrl?: string;
  credentials?: string;
  expiresAt?: string;
  provisionedAt?: string;
  status: string;
  errorMessage?: string;
}

// ==================== API METHODS ====================

const advancedApi = {
  // Dataset API
  datasets: {
    create: (data: CreateDatasetRequest): Promise<DatasetResponse> =>
      axiosClient.post('/api/datasets', data).then(r => r.data),
    get: (datasetId: string): Promise<DatasetResponse> =>
      axiosClient.get(`/api/datasets/${datasetId}`).then(r => r.data),
    list: (projectId: string): Promise<DatasetResponse[]> =>
      axiosClient.get(`/api/datasets/project/${projectId}`).then(r => r.data),
    update: (datasetId: string, data: UpdateDatasetRequest): Promise<DatasetResponse> =>
      axiosClient.put(`/api/datasets/${datasetId}`, data).then(r => r.data),
    delete: (datasetId: string): Promise<void> =>
      axiosClient.delete(`/api/datasets/${datasetId}`).then(r => r.data),
    createVersion: (datasetId: string, changeSummary?: string): Promise<DatasetVersionResponse> =>
      axiosClient.post(`/api/datasets/${datasetId}/versions?changeSummary=${changeSummary || ''}`).then(r => r.data),
    getVersions: (datasetId: string): Promise<DatasetVersionResponse[]> =>
      axiosClient.get(`/api/datasets/${datasetId}/versions`).then(r => r.data),
    bind: (data: BindDatasetRequest): Promise<DatasetBindingResponse> =>
      axiosClient.post('/api/datasets/bind', data).then(r => r.data),
    unbind: (testId: string, datasetId: string): Promise<void> =>
      axiosClient.delete(`/api/datasets/bind/${testId}/${datasetId}`).then(r => r.data),
    getBindingsForTest: (testId: string): Promise<DatasetBindingResponse[]> =>
      axiosClient.get(`/api/datasets/bind/test/${testId}`).then(r => r.data),
    expandParameters: (datasetId: string, testId: string): Promise<Record<string, string>[]> =>
      axiosClient.get(`/api/datasets/${datasetId}/expand?testId=${testId}`).then(r => r.data),
  },

  // Shared Steps API
  sharedSteps: {
    create: (data: CreateSharedStepRequest): Promise<SharedStepResponse> =>
      axiosClient.post('/api/shared-steps', data).then(r => r.data),
    get: (sharedStepId: string): Promise<SharedStepResponse> =>
      axiosClient.get(`/api/shared-steps/${sharedStepId}`).then(r => r.data),
    list: (projectId: string): Promise<SharedStepResponse[]> =>
      axiosClient.get(`/api/shared-steps/project/${projectId}`).then(r => r.data),
    update: (sharedStepId: string, data: CreateSharedStepRequest): Promise<SharedStepResponse> =>
      axiosClient.put(`/api/shared-steps/${sharedStepId}`, data).then(r => r.data),
    delete: (sharedStepId: string): Promise<void> =>
      axiosClient.delete(`/api/shared-steps/${sharedStepId}`).then(r => r.data),
    getVersions: (sharedStepId: string): Promise<SharedStepVersionResponse[]> =>
      axiosClient.get(`/api/shared-steps/${sharedStepId}/versions`).then(r => r.data),
    getImpact: (sharedStepId: string): Promise<SharedStepImpactResponse[]> =>
      axiosClient.get(`/api/shared-steps/${sharedStepId}/impact`).then(r => r.data),
    insert: (data: InsertSharedStepRequest): Promise<EmbeddedStepResponse> =>
      axiosClient.post('/api/shared-steps/insert', data).then(r => r.data),
    getEmbeddedSteps: (testId: string): Promise<EmbeddedStepResponse[]> =>
      axiosClient.get(`/api/shared-steps/test/${testId}`).then(r => r.data),
  },

  // Impact Analysis API
  impact: {
    analyze: (data: ImpactAnalysisRequest): Promise<ImpactAnalysisResponse> =>
      axiosClient.post('/api/impact/analyze', data).then(r => r.data),
    analyzeCommit: (data: ImpactAnalysisRequest): Promise<ImpactAnalysisResponse> =>
      axiosClient.post('/api/impact/analyze/commit', data).then(r => r.data),
    getResult: (analysisId: string): Promise<ImpactAnalysisResponse> =>
      axiosClient.get(`/api/impact/results/${analysisId}`).then(r => r.data),
    getHistory: (projectId: string): Promise<ImpactAnalysisResponse[]> =>
      axiosClient.get(`/api/impact/history/${projectId}`).then(r => r.data),
    registerComponent: (data: ComponentRequest): Promise<ComponentResponse> =>
      axiosClient.post('/api/impact/components', data).then(r => r.data),
    getComponents: (projectId: string): Promise<ComponentResponse[]> =>
      axiosClient.get(`/api/impact/components/project/${projectId}`).then(r => r.data),
    mapTestToComponent: (data: TestComponentMappingRequest): Promise<void> =>
      axiosClient.post('/api/impact/test-component', data).then(r => r.data),
    getComponentsForTest: (testId: string): Promise<ComponentResponse[]> =>
      axiosClient.get(`/api/impact/test/${testId}/components`).then(r => r.data),
    analyzeTestImpact: (testId: string, cascadeDepth?: number): Promise<unknown> =>
      axiosClient.get(`/api/impact/test/${testId}`, { params: { cascadeDepth } }).then(r => r.data),
    analyzeRequirementImpact: (requirementKey: string, fromVersion?: number, toVersion?: number): Promise<unknown> =>
      axiosClient.get(`/api/impact/requirement/${requirementKey}`, { params: { fromVersion, toVersion } }).then(r => r.data),
    getAffectedTests: (projectId: string, changeType?: string, changeKey?: string): Promise<unknown[]> =>
      axiosClient.get(`/api/impact/affected`, { params: { projectId, changeType, changeKey } }).then(r => r.data),
  },

  // Flaky Test API
  flakyTests: {
    getAll: (limit = 50): Promise<FlakyTestResponse[]> =>
      axiosClient.get('/api/flaky-tests', { params: { limit } }).then(r => r.data),
    getDetails: (testId: string): Promise<FlakyTestResponse> =>
      axiosClient.get(`/api/flaky-tests/${testId}`).then(r => r.data),
    getCandidates: (): Promise<FlakyTestResponse[]> =>
      axiosClient.get('/api/flaky-tests/quarantine-candidates').then(r => r.data),
    getDashboard: (projectId: string): Promise<FlakyDashboardResponse> =>
      axiosClient.get('/api/flaky-tests/dashboard', { params: { projectId } }).then(r => r.data),
  },

  // Quarantine API
  quarantine: {
    quarantine: (data: QuarantineRequest): Promise<QuarantineResponse> =>
      axiosClient.post('/api/quarantine', data).then(r => r.data),
    get: (testId: string): Promise<QuarantineResponse> =>
      axiosClient.get(`/api/quarantine/test/${testId}`).then(r => r.data),
    list: (projectId: string): Promise<QuarantineResponse[]> =>
      axiosClient.get(`/api/quarantine/project/${projectId}`).then(r => r.data),
    listByStatus: (status: string): Promise<QuarantineResponse[]> =>
      axiosClient.get(`/api/quarantine/status/${status}`).then(r => r.data),
    updateStatus: (quarantineId: string, status: string, reason?: string): Promise<QuarantineResponse> =>
      axiosClient.put(`/api/quarantine/${quarantineId}/status`, null, { params: { status, reason } }).then(r => r.data),
    restore: (quarantineId: string, reason?: string): Promise<QuarantineResponse> =>
      axiosClient.post(`/api/quarantine/${quarantineId}/restore`, null, { params: { reason } }).then(r => r.data),
    getDashboard: (projectId: string): Promise<QuarantineDashboardResponse> =>
      axiosClient.get('/api/quarantine/dashboard', { params: { projectId } }).then(r => r.data),
    createRule: (data: QuarantineRuleRequest): Promise<QuarantineRuleResponse> =>
      axiosClient.post('/api/quarantine/rules', data).then(r => r.data),
    getRules: (projectId: string): Promise<QuarantineRuleResponse[]> =>
      axiosClient.get(`/api/quarantine/rules/project/${projectId}`).then(r => r.data),
  },

  // Timeline & Replay API
  timeline: {
    getTimeline: (executionId: string): Promise<TimelineEventResponse[]> =>
      axiosClient.get(`/api/executions/${executionId}/timeline`).then(r => r.data),
    startReplay: (executionId: string): Promise<ReplaySessionResponse> =>
      axiosClient.post(`/api/executions/${executionId}/replay/start`).then(r => r.data),
    updatePosition: (sessionId: string, positionMs: number): Promise<void> =>
      axiosClient.put(`/api/replay/${sessionId}/position`, null, { params: { positionMs } }).then(r => r.data),
    pauseReplay: (sessionId: string): Promise<void> =>
      axiosClient.post(`/api/replay/${sessionId}/pause`).then(r => r.data),
    resumeReplay: (sessionId: string): Promise<void> =>
      axiosClient.post(`/api/replay/${sessionId}/resume`).then(r => r.data),
  },

  // Version Diff API
  versionDiff: {
    diffTest: (testId: string, v1: number, v2: number): Promise<VersionDiffResponse> =>
      axiosClient.get(`/api/tests/${testId}/versions/${v1}/diff/${v2}`).then(r => r.data),
    diffDataset: (datasetId: string, v1: number, v2: number): Promise<VersionDiffResponse> =>
      axiosClient.get(`/api/datasets/${datasetId}/versions/${v1}/diff/${v2}`).then(r => r.data),
    diffSharedStep: (sharedStepId: string, v1: number, v2: number): Promise<VersionDiffResponse> =>
      axiosClient.get(`/api/shared-steps/${sharedStepId}/versions/${v1}/diff/${v2}`).then(r => r.data),
  },

  // Requirement Impact API
  requirementImpact: {
    createVersion: (requirementId: string, title: string, description?: string): Promise<void> =>
      axiosClient.post(`/api/requirements/${requirementId}/versions`, null, { params: { title, description } }).then(r => r.data),
    getVersions: (requirementId: string): Promise<unknown[]> =>
      axiosClient.get(`/api/requirements/${requirementId}/versions`).then(r => r.data),
    analyzeImpact: (requirementId: string, fromVersion: number, toVersion: number): Promise<unknown> =>
      axiosClient.get(`/api/requirements/${requirementId}/impact`, { params: { fromVersion, toVersion } }).then(r => r.data),
    analyzeCoverageDrift: (requirementId: string): Promise<void> =>
      axiosClient.post(`/api/requirements/${requirementId}/coverage-drift`).then(r => r.data),
  },

  // Evidence Management API
  evidence: {
    upload: (data: EvidenceUploadRequest): Promise<EvidenceResponse> =>
      axiosClient.post('/api/evidence', data).then(r => r.data),
    get: (evidenceId: string): Promise<EvidenceResponse> =>
      axiosClient.get(`/api/evidence/${evidenceId}`).then(r => r.data),
    getByExecution: (executionId: string): Promise<EvidenceResponse[]> =>
      axiosClient.get(`/api/evidence/execution/${executionId}`).then(r => r.data),
    getViewerData: (executionId: string): Promise<EvidenceViewerData> =>
      axiosClient.get(`/api/evidence/execution/${executionId}/viewer`).then(r => r.data),
    getByStep: (stepResultId: string): Promise<EvidenceResponse[]> =>
      axiosClient.get(`/api/evidence/step/${stepResultId}`).then(r => r.data),
    classify: (data: EvidenceClassificationRequest): Promise<EvidenceResponse> =>
      axiosClient.put('/api/evidence/classify', data).then(r => r.data),
    delete: (evidenceId: string): Promise<void> =>
      axiosClient.delete(`/api/evidence/${evidenceId}`).then(r => r.data),
    archive: (evidenceId: string): Promise<void> =>
      axiosClient.post(`/api/evidence/${evidenceId}/archive`).then(r => r.data),
    // Retention policies
    createPolicy: (data: RetentionPolicyRequest): Promise<RetentionPolicyResponse> =>
      axiosClient.post('/api/evidence/policies', data).then(r => r.data),
    getPolicies: (projectId?: string): Promise<RetentionPolicyResponse[]> =>
      axiosClient.get('/api/evidence/policies', { params: { projectId } }).then(r => r.data),
    getPolicy: (policyId: string): Promise<RetentionPolicyResponse> =>
      axiosClient.get(`/api/evidence/policies/${policyId}`).then(r => r.data),
    applyPolicy: (policyId: string): Promise<void> =>
      axiosClient.post(`/api/evidence/policies/${policyId}/apply`).then(r => r.data),
  },

  // Environment Matrix API
  environmentMatrix: {
    create: (data: MatrixConfigurationRequest): Promise<MatrixConfigurationResponse> =>
      axiosClient.post('/api/environment-matrix', data).then(r => r.data),
    get: (matrixId: string): Promise<MatrixConfigurationResponse> =>
      axiosClient.get(`/api/environment-matrix/${matrixId}`).then(r => r.data),
    list: (projectId: string): Promise<MatrixConfigurationResponse[]> =>
      axiosClient.get('/api/environment-matrix', { params: { projectId } }).then(r => r.data),
    delete: (matrixId: string): Promise<void> =>
      axiosClient.delete(`/api/environment-matrix/${matrixId}`).then(r => r.data),
    // Combinations
    getCombinations: (matrixId: string): Promise<CombinationResponse[]> =>
      axiosClient.get(`/api/environment-matrix/${matrixId}/combinations`).then(r => r.data),
    getValidCombinations: (matrixId: string): Promise<CombinationResponse[]> =>
      axiosClient.get(`/api/environment-matrix/${matrixId}/combinations/valid`).then(r => r.data),
    validate: (matrixId: string): Promise<CombinationResponse[]> =>
      axiosClient.post(`/api/environment-matrix/${matrixId}/validate`).then(r => r.data),
    // Provisioning
    provision: (data: EnvironmentProvisionRequest): Promise<ProvisionResponse> =>
      axiosClient.post('/api/environment-matrix/provision', data).then(r => r.data),
    getProvisioned: (combinationId: string): Promise<ProvisionResponse> =>
      axiosClient.get(`/api/environment-matrix/combinations/${combinationId}/provisioned`).then(r => r.data),
    // Rules
    createRule: (data: ProvisioningRuleRequest): Promise<ProvisioningRuleResponse> =>
      axiosClient.post('/api/environment-matrix/rules', data).then(r => r.data),
    getRules: (projectId?: string): Promise<ProvisioningRuleResponse[]> =>
      axiosClient.get('/api/environment-matrix/rules', { params: { projectId } }).then(r => r.data),
    getRule: (ruleId: string): Promise<ProvisioningRuleResponse> =>
      axiosClient.get(`/api/environment-matrix/rules/${ruleId}`).then(r => r.data),
    deleteRule: (ruleId: string): Promise<void> =>
      axiosClient.delete(`/api/environment-matrix/rules/${ruleId}`).then(r => r.data),
  },
};

// ==================== TEST ADMIN CONFIG API ====================

export interface TestStatusConfigRequest {
  projectId?: string;
  name: string;
  displayName: string;
  color?: string;
  icon?: string;
  category?: 'TODO' | 'IN_PROGRESS' | 'DONE';
  isDefault?: boolean;
  isFinal?: boolean;
  sortOrder?: number;
}

export interface TestStatusConfigResponse {
  id: string;
  projectId?: string;
  name: string;
  displayName: string;
  color: string;
  icon?: string;
  category: string;
  isDefault: boolean;
  isFinal: boolean;
  sortOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ExecutionStatusConfigRequest {
  projectId?: string;
  name: string;
  displayName: string;
  color?: string;
  icon?: string;
  isPass?: boolean;
  isFail?: boolean;
  sortOrder?: number;
}

export interface ExecutionStatusConfigResponse {
  id: string;
  projectId?: string;
  name: string;
  displayName: string;
  color: string;
  icon?: string;
  isPass: boolean;
  isFail: boolean;
  sortOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TestTypeConfigRequest {
  projectId?: string;
  name: string;
  displayName: string;
  description?: string;
  icon?: string;
  color?: string;
  sortOrder?: number;
}

export interface TestTypeConfigResponse {
  id: string;
  projectId?: string;
  name: string;
  displayName: string;
  description?: string;
  icon?: string;
  color: string;
  sortOrder: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ExploratorySessionRequest {
  projectId: string;
  charter?: string;
  charterGoal?: string;
  sessionType?: 'CHARTER_BASED' | 'SESSION_BASED' | 'FREESTYLE';
  timeBoxMinutes?: number;
  environment?: string;
}

export interface ExploratorySessionResponse {
  id: string;
  projectId: string;
  charter?: string;
  charterGoal?: string;
  sessionType: string;
  timeBoxMinutes: number;
  actualDurationMinutes?: number;
  status: string;
  testerId?: string;
  environment?: string;
  startedAt?: string;
  completedAt?: string;
  notes?: string;
  bugs: string[];
  ideas: string[];
  questions: string[];
  evidenceLinks: string[];
  defectKeys: string[];
  createdAt: string;
  updatedAt: string;
}

export interface BulkTestRequest {
  testIds: string[];
  status?: string;
  ownerId?: string;
  folderId?: string;
  testSetId?: string;
  labels?: string[];
}

export interface BulkTestResponse {
  totalRequested: number;
  successCount: number;
  failedCount: number;
  errors: string[];
}

export interface XrayImportRequest {
  testExecutionKey?: string;
  info?: {
    summary: string;
    description?: string;
    startDate?: string;
    finishDate?: string;
    testPlanKey?: string;
    testEnvironments?: string[];
  };
  tests: {
    testKey?: string;
    status: string;
    comment?: string;
    start?: string;
    finish?: string;
    steps?: { status: string; comment?: string; actualResult?: string }[];
    defects?: string[];
  }[];
}

const testAdminApi = {
  // Test Status Config
  getTestStatuses: (): Promise<TestStatusConfigResponse[]> =>
    axiosClient.get('/api/test-admin/statuses').then(r => r.data),
  getTestStatusesByProject: (projectId: string): Promise<TestStatusConfigResponse[]> =>
    axiosClient.get(`/api/test-admin/statuses/project/${projectId}`).then(r => r.data),
  createTestStatus: (data: TestStatusConfigRequest): Promise<TestStatusConfigResponse> =>
    axiosClient.post('/api/test-admin/statuses', data).then(r => r.data),
  updateTestStatus: (id: string, data: TestStatusConfigRequest): Promise<TestStatusConfigResponse> =>
    axiosClient.put(`/api/test-admin/statuses/${id}`, data).then(r => r.data),
  deleteTestStatus: (id: string): Promise<void> =>
    axiosClient.delete(`/api/test-admin/statuses/${id}`).then(r => r.data),

  // Execution Status Config
  getExecutionStatuses: (): Promise<ExecutionStatusConfigResponse[]> =>
    axiosClient.get('/api/test-admin/execution-statuses').then(r => r.data),
  createExecutionStatus: (data: ExecutionStatusConfigRequest): Promise<ExecutionStatusConfigResponse> =>
    axiosClient.post('/api/test-admin/execution-statuses', data).then(r => r.data),
  updateExecutionStatus: (id: string, data: ExecutionStatusConfigRequest): Promise<ExecutionStatusConfigResponse> =>
    axiosClient.put(`/api/test-admin/execution-statuses/${id}`, data).then(r => r.data),
  deleteExecutionStatus: (id: string): Promise<void> =>
    axiosClient.delete(`/api/test-admin/execution-statuses/${id}`).then(r => r.data),

  // Test Type Config
  getTestTypes: (): Promise<TestTypeConfigResponse[]> =>
    axiosClient.get('/api/test-admin/test-types').then(r => r.data),
  createTestType: (data: TestTypeConfigRequest): Promise<TestTypeConfigResponse> =>
    axiosClient.post('/api/test-admin/test-types', data).then(r => r.data),
  updateTestType: (id: string, data: TestTypeConfigRequest): Promise<TestTypeConfigResponse> =>
    axiosClient.put(`/api/test-admin/test-types/${id}`, data).then(r => r.data),
  deleteTestType: (id: string): Promise<void> =>
    axiosClient.delete(`/api/test-admin/test-types/${id}`).then(r => r.data),

  // Clone Operations
  cloneTest: (testId: string, projectId: string): Promise<TestResponse> =>
    axiosClient.post(`/api/tests/${testId}/clone?projectId=${projectId}`).then(r => r.data),
  cloneTestSet: (setId: string, projectId: string): Promise<TestSetResponse> =>
    axiosClient.post(`/api/test-sets/${setId}/clone?projectId=${projectId}`).then(r => r.data),
  cloneTestPlan: (planId: string, projectId: string): Promise<TestPlanResponse> =>
    axiosClient.post(`/api/test-plans/${planId}/clone?projectId=${projectId}`).then(r => r.data),

  // Bulk Operations
  bulkUpdateStatus: (data: BulkTestRequest, projectId: string): Promise<BulkTestResponse> =>
    axiosClient.put(`/api/tests/bulk/status?projectId=${projectId}`, data).then(r => r.data),
  bulkAssign: (data: BulkTestRequest, projectId: string): Promise<BulkTestResponse> =>
    axiosClient.put(`/api/tests/bulk/assign?projectId=${projectId}`, data).then(r => r.data),
  bulkMoveToFolder: (data: BulkTestRequest, projectId: string): Promise<BulkTestResponse> =>
    axiosClient.put(`/api/tests/bulk/move-folder?projectId=${projectId}`, data).then(r => r.data),
  bulkAddToSet: (data: BulkTestRequest, projectId: string): Promise<BulkTestResponse> =>
    axiosClient.put(`/api/tests/bulk/add-to-set?projectId=${projectId}`, data).then(r => r.data),
  bulkAddLabels: (data: BulkTestRequest, projectId: string): Promise<BulkTestResponse> =>
    axiosClient.put(`/api/tests/bulk/labels?projectId=${projectId}`, data).then(r => r.data),
  bulkDeleteTests: (testIds: string[], projectId: string): Promise<BulkTestResponse> =>
    axiosClient.delete(`/api/tests/bulk?projectId=${projectId}`, { data: { testIds } }).then(r => r.data),

  // Exploratory Testing Sessions
  createExploratorySession: (data: ExploratorySessionRequest): Promise<ExploratorySessionResponse> =>
    axiosClient.post('/api/exploratory-sessions', data).then(r => r.data),
  getExploratorySession: (id: string): Promise<ExploratorySessionResponse> =>
    axiosClient.get(`/api/exploratory-sessions/${id}`).then(r => r.data),
  getExploratorySessions: (projectId: string): Promise<ExploratorySessionResponse[]> =>
    axiosClient.get(`/api/exploratory-sessions/project/${projectId}`).then(r => r.data),
  updateExploratorySession: (id: string, data: Partial<ExploratorySessionRequest>): Promise<ExploratorySessionResponse> =>
    axiosClient.put(`/api/exploratory-sessions/${id}`, data).then(r => r.data),
  startExploratorySession: (id: string): Promise<ExploratorySessionResponse> =>
    axiosClient.post(`/api/exploratory-sessions/${id}/start`).then(r => r.data),
  completeExploratorySession: (id: string): Promise<ExploratorySessionResponse> =>
    axiosClient.post(`/api/exploratory-sessions/${id}/complete`).then(r => r.data),
  abandonExploratorySession: (id: string): Promise<ExploratorySessionResponse> =>
    axiosClient.post(`/api/exploratory-sessions/${id}/abandon`).then(r => r.data),
  addSessionNote: (id: string, note: string): Promise<ExploratorySessionResponse> =>
    axiosClient.post(`/api/exploratory-sessions/${id}/notes`, { note }).then(r => r.data),
  addSessionBug: (id: string, bug: string): Promise<ExploratorySessionResponse> =>
    axiosClient.post(`/api/exploratory-sessions/${id}/bugs`, { bug }).then(r => r.data),

  // Xray REST API
  xrayImportExecution: (data: XrayImportRequest, projectId: string): Promise<{ testExecIssue: string }> =>
    axiosClient.post(`/api/raven/1.0/import/execution?projectId=${projectId}`, data).then(r => r.data),

  // Additional Import Formats
  importTestNg: (projectId: string, file: File): Promise<TestImportResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.post(`/api/import/testng/file?projectId=${projectId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },
  importNUnit: (projectId: string, file: File): Promise<TestImportResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.post(`/api/import/nunit/file?projectId=${projectId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },
  importRobot: (projectId: string, file: File): Promise<TestImportResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.post(`/api/import/robot/file?projectId=${projectId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },

  // Test Run Iterations
  getIterations: (runId: string): Promise<unknown[]> =>
    axiosClient.get(`/api/test-runs/${runId}/iterations`).then(r => r.data),
  updateIteration: (runId: string, iterationId: string, data: { status: string; comment?: string }): Promise<unknown> =>
    axiosClient.put(`/api/test-runs/${runId}/iterations/${iterationId}`, data).then(r => r.data),
  startIteration: (runId: string, iterationId: string): Promise<unknown> =>
    axiosClient.post(`/api/test-runs/${runId}/iterations/${iterationId}/start`).then(r => r.data),
  completeIteration: (runId: string, iterationId: string): Promise<unknown> =>
    axiosClient.post(`/api/test-runs/${runId}/iterations/${iterationId}/complete`).then(r => r.data),
};

// Combined API - merges basic testApi with all advanced features
const combinedApi = {
  ...testApi,
  ...advancedApi,
  ...testAdminApi,
};

// Export all APIs
export { testApi, advancedApi, testAdminApi, combinedApi };
export default combinedApi;