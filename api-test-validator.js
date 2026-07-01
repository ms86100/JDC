/**
 * Comprehensive API Test Validator
 * Tests all Jira Test Management Service endpoints to ensure they return 200
 *
 * Usage: node api-test-validator.js
 * Requires: npm install axios
 */

const axios = require('axios');

const BASE_URL = process.env.API_BASE_URL || 'http://localhost:8086';
const PROJECT_ID = process.env.TEST_PROJECT_ID || '00000000-0000-0000-0000-000000000001';
const AUTH_TOKEN = process.env.AUTH_TOKEN || 'Bearer test-token';

// Create axios instance with default config
const api = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Authorization': AUTH_TOKEN,
    'Content-Type': 'application/json',
  },
});

// Test result tracking
const results = {
  passed: [],
  failed: [],
  errors: [],
};

async function testEndpoint(method, path, data = null, params = null, description = '') {
  const fullPath = `${BASE_URL}${path}`;
  try {
    let response;
    const options = { params };

    switch (method.toUpperCase()) {
      case 'GET':
        response = await api.get(path, options);
        break;
      case 'POST':
        response = await api.post(path, data, options);
        break;
      case 'PUT':
        response = await api.put(path, data, options);
        break;
      case 'DELETE':
        response = await api.delete(path, options);
        break;
      case 'PATCH':
        response = await api.patch(path, data, options);
        break;
      default:
        throw new Error(`Unsupported method: ${method}`);
    }

    if (response.status >= 200 && response.status < 300) {
      results.passed.push({
        method: method.toUpperCase(),
        path,
        status: response.status,
        description,
        responseSize: JSON.stringify(response.data).length,
      });
      console.log(`✅ ${method.toUpperCase()} ${path} - ${response.status} (${description})`);
      return { success: true, data: response.data, status: response.status };
    } else {
      results.failed.push({
        method: method.toUpperCase(),
        path,
        status: response.status,
        description,
      });
      console.log(`❌ ${method.toUpperCase()} ${path} - ${response.status} (${description})`);
      return { success: false, status: response.status };
    }
  } catch (error) {
    results.errors.push({
      method: method.toUpperCase(),
      path,
      error: error.message,
      status: error.response?.status,
      description,
    });
    console.log(`❌ ${method.toUpperCase()} ${path} - ERROR: ${error.message} (${description})`);
    return { success: false, error: error.message };
  }
}

// Test categories
const testSuites = {
  // ============================================
  // TEST MODULE
  // ============================================
  testModule: async () => {
    console.log('\n📋 TEST MODULE\n' + '─'.repeat(50));
    let testId;

    // Create test
    const createResult = await testEndpoint('POST', '/api/tests', {
      projectId: PROJECT_ID,
      name: 'API Validation Test',
      description: 'Test created by API validator',
      testType: 'MANUAL',
      labels: ['api-test', 'automated'],
    }, null, 'Create test');
    if (createResult.success && createResult.data?.id) {
      testId = createResult.data.id;
    }

    // Get test
    if (testId) {
      await testEndpoint('GET', `/api/tests/${testId}`, null, null, 'Get test by ID');
    }

    // Search tests
    await testEndpoint('GET', '/api/tests', null, { projectId: PROJECT_ID }, 'List tests by project');

    // Update test
    if (testId) {
      await testEndpoint('PUT', `/api/tests/${testId}`, {
        name: 'Updated API Validation Test',
        labels: ['api-test', 'automated', 'updated'],
      }, null, 'Update test');
    }

    // Delete test
    if (testId) {
      await testEndpoint('DELETE', `/api/tests/${testId}`, null, null, 'Delete test');
    }

    return testId;
  },

  // ============================================
  // TEST STEP MODULE
  // ============================================
  testStepModule: async () => {
    console.log('\n📋 TEST STEP MODULE\n' + '─'.repeat(50));
    let testId;

    // Create test with steps
    const createResult = await testEndpoint('POST', '/api/tests', {
      projectId: PROJECT_ID,
      name: 'Test with Steps',
      description: 'Test for step validation',
      testType: 'MANUAL',
      testSteps: [
        { index: 1, description: 'Given I am logged in', expectedResult: 'Login successful' },
        { index: 2, description: 'When I navigate to dashboard', expectedResult: 'Dashboard visible' },
        { index: 3, description: 'Then I should see my projects', expectedResult: 'Projects listed' },
      ],
    }, null, 'Create test with steps');

    if (createResult.success && createResult.data?.id) {
      testId = createResult.data.id;
    }

    // Get test with steps
    if (testId) {
      await testEndpoint('GET', `/api/tests/${testId}`, null, null, 'Get test with steps');
    }

    // Update test steps
    if (testId) {
      await testEndpoint('PUT', `/api/tests/${testId}`, {
        testSteps: [
          { index: 1, description: 'Given I am on the login page', expectedResult: 'Login form visible' },
          { index: 2, description: 'When I enter valid credentials', expectedResult: 'Credentials accepted' },
        ],
      }, null, 'Update test steps');
    }

    return testId;
  },

  // ============================================
  // TEST SET MODULE
  // ============================================
  testSetModule: async () => {
    console.log('\n📋 TEST SET MODULE\n' + '─'.repeat(50));
    let testSetId;

    // Create test set
    const createResult = await testEndpoint('POST', '/api/test-sets', {
      projectId: PROJECT_ID,
      name: 'API Validation Test Set',
      description: 'Test set created by API validator',
    }, null, 'Create test set');
    if (createResult.success && createResult.data?.id) {
      testSetId = createResult.data.id;
    }

    // Get test set
    if (testSetId) {
      await testEndpoint('GET', `/api/test-sets/${testSetId}`, null, null, 'Get test set by ID');
    }

    // List test sets
    await testEndpoint('GET', '/api/test-sets', null, { projectId: PROJECT_ID }, 'List test sets');

    // Update test set
    if (testSetId) {
      await testEndpoint('PUT', `/api/test-sets/${testSetId}`, {
        name: 'Updated Test Set',
        description: 'Test set updated by API validator',
      }, null, 'Update test set');
    }

    // Delete test set
    if (testSetId) {
      await testEndpoint('DELETE', `/api/test-sets/${testSetId}`, null, null, 'Delete test set');
    }

    return testSetId;
  },

  // ============================================
  // TEST PLAN MODULE
  // ============================================
  testPlanModule: async () => {
    console.log('\n📋 TEST PLAN MODULE\n' + '─'.repeat(50));
    let testPlanId;

    // Create test plan
    const createResult = await testEndpoint('POST', '/api/test-plans', {
      projectId: PROJECT_ID,
      name: 'API Validation Test Plan',
      description: 'Test plan created by API validator',
      testCycle: 'SPRINT-1',
      testEnv: 'STAGING',
    }, null, 'Create test plan');
    if (createResult.success && createResult.data?.id) {
      testPlanId = createResult.data.id;
    }

    // Get test plan
    if (testPlanId) {
      await testEndpoint('GET', `/api/test-plans/${testPlanId}`, null, null, 'Get test plan by ID');
    }

    // List test plans
    await testEndpoint('GET', '/api/test-plans', null, { projectId: PROJECT_ID }, 'List test plans');

    // Update test plan
    if (testPlanId) {
      await testEndpoint('PUT', `/api/test-plans/${testPlanId}`, {
        name: 'Updated Test Plan',
        status: 'IN_PROGRESS',
      }, null, 'Update test plan');
    }

    // Delete test plan
    if (testPlanId) {
      await testEndpoint('DELETE', `/api/test-plans/${testPlanId}`, null, null, 'Delete test plan');
    }

    return testPlanId;
  },

  // ============================================
  // TEST EXECUTION MODULE
  // ============================================
  testExecutionModule: async () => {
    console.log('\n📋 TEST EXECUTION MODULE\n' + '─'.repeat(50));
    let executionId;

    // Create execution
    const createResult = await testEndpoint('POST', '/api/test-executions', {
      projectId: PROJECT_ID,
      name: 'API Validation Execution',
      testEnv: 'STAGING',
    }, null, 'Create test execution');
    if (createResult.success && createResult.data?.id) {
      executionId = createResult.data.id;
    }

    // Get execution
    if (executionId) {
      await testEndpoint('GET', `/api/test-executions/${executionId}`, null, null, 'Get execution by ID');
    }

    // List executions
    await testEndpoint('GET', '/api/test-executions', null, { projectId: PROJECT_ID }, 'List executions');

    // Start execution
    if (executionId) {
      await testEndpoint('PUT', `/api/test-executions/${executionId}/start`, {}, null, 'Start execution');
    }

    // Complete execution
    if (executionId) {
      await testEndpoint('PUT', `/api/test-executions/${executionId}/complete`, {
        status: 'PASSED',
        comment: 'Execution completed successfully',
      }, null, 'Complete execution');
    }

    return executionId;
  },

  // ============================================
  // SHARED STEPS MODULE
  // ============================================
  sharedStepsModule: async () => {
    console.log('\n📋 SHARED STEPS MODULE\n' + '─'.repeat(50));
    let sharedStepId;

    // Create shared step
    const createResult = await testEndpoint('POST', '/api/shared-steps', {
      projectId: PROJECT_ID,
      name: 'Common Login Step',
      description: 'Shared step for login',
      steps: [
        { description: 'Enter username', expectedResult: 'Username entered' },
        { description: 'Enter password', expectedResult: 'Password entered' },
      ],
    }, null, 'Create shared step');
    if (createResult.success && createResult.data?.id) {
      sharedStepId = createResult.data.id;
    }

    // Get shared step
    if (sharedStepId) {
      await testEndpoint('GET', `/api/shared-steps/${sharedStepId}`, null, { projectId: PROJECT_ID }, 'Get shared step');
    }

    // List shared steps
    await testEndpoint('GET', '/api/shared-steps', null, { projectId: PROJECT_ID }, 'List shared steps');

    // Search shared steps
    await testEndpoint('GET', '/api/shared-steps/search', null, { projectId: PROJECT_ID, search: 'login' }, 'Search shared steps');

    // Get version history
    if (sharedStepId) {
      await testEndpoint('GET', `/api/shared-steps/${sharedStepId}/versions`, null, { projectId: PROJECT_ID }, 'Get version history');
    }

    // Update shared step
    if (sharedStepId) {
      await testEndpoint('PUT', `/api/shared-steps/${sharedStepId}`, {
        name: 'Updated Login Step',
        steps: [
          { description: 'Enter username', expectedResult: 'Username entered' },
          { description: 'Enter password', expectedResult: 'Password entered' },
          { description: 'Click login', expectedResult: 'Logged in' },
        ],
      }, { projectId: PROJECT_ID }, 'Update shared step');
    }

    // Get impact analysis
    if (sharedStepId) {
      await testEndpoint('GET', `/api/shared-steps/${sharedStepId}/impact`, null, { projectId: PROJECT_ID }, 'Get impact analysis');
    }

    // Delete shared step
    if (sharedStepId) {
      await testEndpoint('DELETE', `/api/shared-steps/${sharedStepId}`, null, { projectId: PROJECT_ID }, 'Delete shared step');
    }

    return sharedStepId;
  },

  // ============================================
  // PRECONDITION MODULE
  // ============================================
  preconditionModule: async () => {
    console.log('\n📋 PRECONDITION MODULE\n' + '─'.repeat(50));
    let preconditionId;

    // Create precondition
    const createResult = await testEndpoint('POST', '/api/preconditions', {
      name: 'Database Connection Check',
      description: 'Verify database is accessible',
      type: 'DATABASE',
      evaluationMode: 'ALWAYS',
      expectedResult: 'Connection successful',
    }, { projectId: PROJECT_ID }, 'Create precondition');
    if (createResult.success && createResult.data?.id) {
      preconditionId = createResult.data.id;
    }

    // Get precondition
    if (preconditionId) {
      await testEndpoint('GET', `/api/preconditions/${preconditionId}`, null, null, 'Get precondition');
    }

    // List preconditions
    await testEndpoint('GET', '/api/preconditions', null, { projectId: PROJECT_ID }, 'List preconditions by project');

    // List by type
    await testEndpoint('GET', `/api/preconditions/project/${PROJECT_ID}/type/DATABASE`, null, null, 'List preconditions by type');

    // Update precondition
    if (preconditionId) {
      await testEndpoint('PUT', `/api/preconditions/${preconditionId}`, {
        name: 'Updated DB Check',
        expectedResult: 'Connection verified',
      }, null, 'Update precondition');
    }

    // Delete precondition
    if (preconditionId) {
      await testEndpoint('DELETE', `/api/preconditions/${preconditionId}`, null, null, 'Delete precondition');
    }

    return preconditionId;
  },

  // ============================================
  // QUARANTINE MODULE
  // ============================================
  quarantineModule: async () => {
    console.log('\n📋 QUARANTINE MODULE\n' + '─'.repeat(50));
    let testId;

    // First create a test
    const createTest = await testEndpoint('POST', '/api/tests', {
      projectId: PROJECT_ID,
      name: 'Quarantine Test',
      testType: 'MANUAL',
    }, null, 'Create test for quarantine');

    if (createTest.success && createTest.data?.id) {
      testId = createTest.data.id;
    }

    // Quarantine a test
    if (testId) {
      await testEndpoint('POST', '/api/quarantine', {
        testId: testId,
        reason: 'Flaky test - intermittent failures',
        quarantineType: 'FLAKY',
      }, null, 'Quarantine test');
    }

    // Get quarantine status
    if (testId) {
      await testEndpoint('GET', `/api/quarantine/test/${testId}`, null, null, 'Get quarantine status');
    }

    // List quarantined tests
    await testEndpoint('GET', '/api/quarantine', null, { projectId: PROJECT_ID }, 'List quarantined tests');

    // Get quarantine dashboard
    await testEndpoint('GET', '/api/quarantine/dashboard', null, { projectId: PROJECT_ID }, 'Get quarantine dashboard');

    // Get quarantine rules
    await testEndpoint('GET', '/api/quarantine/rules', null, { projectId: PROJECT_ID }, 'List quarantine rules');

    return testId;
  },

  // ============================================
  // COVERAGE MODULE
  // ============================================
  coverageModule: async () => {
    console.log('\n📋 COVERAGE MODULE\n' + '─'.repeat(50));

    // Get project coverage
    await testEndpoint('GET', '/api/coverage', null, { projectId: PROJECT_ID }, 'Get project coverage');

    // Get coverage trend
    await testEndpoint('GET', `/api/coverage/${PROJECT_ID}/trend`, null, { range: '30d' }, 'Get coverage trend');

    // Get requirements coverage
    await testEndpoint('GET', `/api/coverage/${PROJECT_ID}/requirements`, null, null, 'Get requirements coverage');

    // Get coverage matrix
    await testEndpoint('GET', `/api/coverage/${PROJECT_ID}/matrix`, null, null, 'Get coverage matrix');

    // Get coverage rules
    await testEndpoint('GET', '/api/coverage/rules', null, { projectId: PROJECT_ID }, 'List coverage rules');

    // Create coverage rule
    await testEndpoint('POST', '/api/coverage/rules', {
      name: 'High Priority Coverage',
      type: 'REQUIREMENT',
      targetPercent: 90,
      isActive: true,
    }, null, 'Create coverage rule');

    // Get coverage alerts
    await testEndpoint('GET', `/api/coverage/${PROJECT_ID}/alerts`, null, null, 'Get coverage alerts');

    // Get coverage violations
    await testEndpoint('GET', `/api/coverage/${PROJECT_ID}/violations`, null, null, 'Get coverage violations');
  },

  // ============================================
  // REQUIREMENT & TRACEABILITY MODULE
  // ============================================
  requirementTraceabilityModule: async () => {
    console.log('\n📋 REQUIREMENT & TRACEABILITY MODULE\n' + '─'.repeat(50));
    let testId;
    let requirementKey = 'PROJ-001';

    // Create test linked to requirement
    const createTest = await testEndpoint('POST', '/api/tests', {
      projectId: PROJECT_ID,
      name: 'Requirement Linked Test',
      testType: 'MANUAL',
      requirementKeys: [requirementKey],
    }, null, 'Create test with requirement link');

    if (createTest.success && createTest.data?.id) {
      testId = createTest.data.id;
    }

    // Get traceability matrix
    await testEndpoint('GET', '/api/traceability/matrix', null, { projectId: PROJECT_ID }, 'Get traceability matrix');

    // Get coverage for requirement
    await testEndpoint('GET', `/api/requirements/${requirementKey}/coverage/${PROJECT_ID}`, null, null, 'Get requirement coverage');

    // Link requirement to test
    if (testId) {
      await testEndpoint('POST', '/api/requirements/links', {
        testId: testId,
        requirementKey: requirementKey,
        coverageStatus: 'COVERED',
      }, null, 'Link requirement to test');
    }

    // Get requirement versions
    await testEndpoint('GET', `/api/requirements/${requirementKey}/versions`, null, null, 'Get requirement versions');

    // Get drift analysis
    await testEndpoint('GET', `/api/requirements/${requirementKey}/drift`, null, null, 'Get drift analysis');

    return testId;
  },

  // ============================================
  // TIMELINE & REPLAY MODULE
  // ============================================
  timelineModule: async () => {
    console.log('\n📋 TIMELINE & REPLAY MODULE\n' + '─'.repeat(50));
    let executionId;

    // Create test execution
    const createExec = await testEndpoint('POST', '/api/test-executions', {
      projectId: PROJECT_ID,
      name: 'Timeline Test Execution',
    }, null, 'Create execution for timeline');

    if (createExec.success && createExec.data?.id) {
      executionId = createExec.data.id;
    }

    // Start replay session
    if (executionId) {
      await testEndpoint('POST', '/api/timeline/sessions', {}, { executionId }, 'Start replay session');
    }

    // Get timeline summary
    if (executionId) {
      await testEndpoint('GET', `/api/timeline/summary/${executionId}`, null, null, 'Get timeline summary');
    }

    // Get timeline events
    if (executionId) {
      await testEndpoint('GET', '/api/timeline/events', null, { executionId }, 'Get timeline events');
    }

    // Get snapshots
    if (executionId) {
      await testEndpoint('GET', '/api/timeline/snapshots', null, { executionId }, 'Get timeline snapshots');
    }
  },

  // ============================================
  // FLaky TEST MODULE
  // ============================================
  flakyTestModule: async () => {
    console.log('\n📋 FLAKY TEST MODULE\n' + '─'.repeat(50));

    // Get flaky tests
    await testEndpoint('GET', '/api/flaky-tests', null, { projectId: PROJECT_ID }, 'Get flaky tests');

    // Get flaky test dashboard
    await testEndpoint('GET', '/api/flaky-tests/dashboard', null, { projectId: PROJECT_ID }, 'Get flaky dashboard');

    // Get quarantine candidates
    await testEndpoint('GET', '/api/flaky-tests/quarantine-candidates', null, { projectId: PROJECT_ID }, 'Get quarantine candidates');
  },

  // ============================================
  // WORKFLOW MODULE
  // ============================================
  workflowModule: async () => {
    console.log('\n📋 WORKFLOW MODULE\n' + '─'.repeat(50));
    let workflowId;

    // Create workflow definition
    const createResult = await testEndpoint('POST', '/api/workflows/definitions', {
      name: 'API Validation Workflow',
      description: 'Workflow for API validation',
      projectId: PROJECT_ID,
      initialStatus: 'DRAFT',
    }, null, 'Create workflow definition');
    if (createResult.success && createResult.data?.id) {
      workflowId = createResult.data.id;
    }

    // Get workflow definition
    if (workflowId) {
      await testEndpoint('GET', `/api/workflows/definitions/${workflowId}`, null, null, 'Get workflow definition');
    }

    // List workflow definitions
    await testEndpoint('GET', '/api/workflows/definitions', null, { projectId: PROJECT_ID }, 'List workflows');

    // Update workflow
    if (workflowId) {
      await testEndpoint('PUT', `/api/workflows/definitions/${workflowId}`, {
        name: 'Updated Workflow',
        description: 'Workflow updated via API',
      }, null, 'Update workflow');
    }

    // Get workflow transitions
    if (workflowId) {
      await testEndpoint('GET', `/api/workflows/definitions/${workflowId}/transitions`, null, null, 'Get workflow transitions');
    }

    return workflowId;
  },

  // ============================================
  // DATASET MODULE
  // ============================================
  datasetModule: async () => {
    console.log('\n📋 DATASET MODULE\n' + '─'.repeat(50));
    let datasetId;

    // Create dataset
    const createResult = await testEndpoint('POST', '/api/datasets', {
      projectId: PROJECT_ID,
      name: 'API Validation Dataset',
      description: 'Dataset for API testing',
      columns: ['username', 'password', 'expectedResult'],
      rows: [
        ['user1', 'pass1', 'success'],
        ['user2', 'pass2', 'success'],
      ],
    }, null, 'Create dataset');
    if (createResult.success && createResult.data?.id) {
      datasetId = createResult.data.id;
    }

    // Get dataset
    if (datasetId) {
      await testEndpoint('GET', `/api/datasets/${datasetId}`, null, null, 'Get dataset');
    }

    // List datasets
    await testEndpoint('GET', '/api/datasets', null, { projectId: PROJECT_ID }, 'List datasets');

    // Get dataset templates
    await testEndpoint('GET', '/api/datasets/templates', null, { projectId: PROJECT_ID }, 'Get dataset templates');

    // Update dataset
    if (datasetId) {
      await testEndpoint('PUT', `/api/datasets/${datasetId}`, {
        name: 'Updated Dataset',
        rows: [
          ['user1', 'pass1', 'success'],
          ['user2', 'pass2', 'success'],
          ['user3', 'pass3', 'fail'],
        ],
      }, null, 'Update dataset');
    }

    // Get dataset sharing
    if (datasetId) {
      await testEndpoint('GET', `/api/datasets/${datasetId}/sharing`, null, null, 'Get dataset sharing');
    }

    return datasetId;
  },

  // ============================================
  // ENVIRONMENT MATRIX MODULE
  // ============================================
  environmentMatrixModule: async () => {
    console.log('\n📋 ENVIRONMENT MATRIX MODULE\n' + '─'.repeat(50));

    // Get environment matrix
    await testEndpoint('GET', '/api/environment-matrix', null, { projectId: PROJECT_ID }, 'Get environment matrix');

    // Get matrix combinations
    await testEndpoint('GET', '/api/environment-matrix/combinations', null, { projectId: PROJECT_ID }, 'Get combinations');

    // Get provisioning rules
    await testEndpoint('GET', '/api/environment-matrix/rules', null, { projectId: PROJECT_ID }, 'Get provisioning rules');
  },

  // ============================================
  // REPORTING MODULE
  // ============================================
  reportingModule: async () => {
    console.log('\n📋 REPORTING MODULE\n' + '─'.repeat(50));

    // Get report summary
    await testEndpoint('GET', '/api/reports/summary', null, { projectId: PROJECT_ID }, 'Get report summary');

    // Get test trends
    await testEndpoint('GET', '/api/reports/trend', null, { projectId: PROJECT_ID }, 'Get test trends');

    // Get coverage report
    await testEndpoint('GET', '/api/reports/coverage', null, { projectId: PROJECT_ID }, 'Get coverage report');

    // Get defect density
    await testEndpoint('GET', '/api/reports/defect-density', null, { projectId: PROJECT_ID }, 'Get defect density');
  },

  // ============================================
  // IMPORT MODULE
  // ============================================
  importModule: async () => {
    console.log('\n📋 IMPORT MODULE\n' + '─'.repeat(50));

    // Get import history
    await testEndpoint('GET', '/api/import/history', null, { projectId: PROJECT_ID }, 'Get import history');

    // Get CI sources
    await testEndpoint('GET', '/api/import/ci-source', null, null, 'Get CI sources');
  },

  // ============================================
  // EVIDENCE MODULE
  // ============================================
  evidenceModule: async () => {
    console.log('\n📋 EVIDENCE MODULE\n' + '─'.repeat(50));
    let evidenceId;

    // Get evidence gallery
    await testEndpoint('GET', '/api/evidence', null, { projectId: PROJECT_ID }, 'Get evidence gallery');

    // Get evidence viewer data
    await testEndpoint('GET', '/api/evidence/viewer', null, { testId: '00000000-0000-0000-0000-000000000001' }, 'Get evidence viewer');

    // Get evidence statistics
    await testEndpoint('GET', '/api/evidence/stats', null, { projectId: PROJECT_ID }, 'Get evidence stats');

    return evidenceId;
  },

  // ============================================
  // AUDIT MODULE
  // ============================================
  auditModule: async () => {
    console.log('\n📋 AUDIT MODULE\n' + '─'.repeat(50));

    // Get audit logs
    await testEndpoint('GET', '/api/audit/logs', null, { projectId: PROJECT_ID }, 'Get audit logs');

    // Get audit summary
    await testEndpoint('GET', '/api/audit/summary', null, { projectId: PROJECT_ID }, 'Get audit summary');

    // Get audit exports
    await testEndpoint('GET', '/api/audit/exports', null, null, 'Get audit exports');
  },

  // ============================================
  // COMPLIANCE MODULE
  // ============================================
  complianceModule: async () => {
    console.log('\n📋 COMPLIANCE MODULE\n' + '─'.repeat(50));

    // Get compliance report
    await testEndpoint('GET', '/api/compliance/report', null, { projectId: PROJECT_ID }, 'Get compliance report');

    // Get compliance dashboard
    await testEndpoint('GET', '/api/compliance/dashboard', null, null, 'Get compliance dashboard');
  },
};

// Main test runner
async function runAllTests() {
  console.log('╔════════════════════════════════════════════════════════════╗');
  console.log('║     SYSTEMS TEST MANAGEMENT - API VALIDATION SUITE          ║');
  console.log('╚════════════════════════════════════════════════════════════╝');
  console.log(`\nBase URL: ${BASE_URL}`);
  console.log(`Project ID: ${PROJECT_ID}`);
  console.log('─'.repeat(60));

  const startTime = Date.now();

  // Run all test suites
  try {
    await testSuites.testModule();
    await testSuites.testStepModule();
    await testSuites.testSetModule();
    await testSuites.testPlanModule();
    await testSuites.testExecutionModule();
    await testSuites.sharedStepsModule();
    await testSuites.preconditionModule();
    await testSuites.quarantineModule();
    await testSuites.coverageModule();
    await testSuites.requirementTraceabilityModule();
    await testSuites.timelineModule();
    await testSuites.flakyTestModule();
    await testSuites.workflowModule();
    await testSuites.datasetModule();
    await testSuites.environmentMatrixModule();
    await testSuites.reportingModule();
    await testSuites.importModule();
    await testSuites.evidenceModule();
    await testSuites.auditModule();
    await testSuites.complianceModule();
  } catch (error) {
    console.error('\n❌ Test suite execution failed:', error.message);
  }

  const duration = ((Date.now() - startTime) / 1000).toFixed(2);

  // Print summary
  console.log('\n' + '═'.repeat(60));
  console.log('                    TEST SUMMARY');
  console.log('═'.repeat(60));
  console.log(`\n✅ Passed:  ${results.passed.length}`);
  console.log(`❌ Failed:  ${results.failed.length}`);
  console.log(`⚠️  Errors:  ${results.errors.length}`);
  console.log(`⏱️  Duration: ${duration}s`);
  console.log(`📊 Total:   ${results.passed.length + results.failed.length + results.errors.length}`);

  if (results.passed.length > 0) {
    console.log('\n✅ PASSED ENDPOINTS:');
    results.passed.forEach((p) => {
      console.log(`   ${p.method.padEnd(6)} ${p.path} (${p.status})`);
    });
  }

  if (results.failed.length > 0) {
    console.log('\n❌ FAILED ENDPOINTS:');
    results.failed.forEach((f) => {
      console.log(`   ${f.method.padEnd(6)} ${f.path} (${f.status})`);
    });
  }

  if (results.errors.length > 0) {
    console.log('\n⚠️  ERROR ENDPOINTS:');
    results.errors.forEach((e) => {
      console.log(`   ${e.method.padEnd(6)} ${e.path} - ${e.error}`);
    });
  }

  // Calculate pass rate
  const totalTests = results.passed.length + results.failed.length;
  const passRate = totalTests > 0 ? ((results.passed.length / totalTests) * 100).toFixed(2) : 0;

  console.log('\n' + '─'.repeat(60));
  console.log(`📈 Pass Rate: ${passRate}%`);
  console.log('═'.repeat(60));

  // Exit with appropriate code
  process.exit(results.failed.length + results.errors.length > 0 ? 1 : 0);
}

// Run tests
runAllTests();