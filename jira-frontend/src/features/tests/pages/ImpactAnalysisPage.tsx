import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import {
  AlertTriangle,
  GitBranch,
  TrendingUp,
  ChevronRight,
  Download,
  Filter,
  RefreshCw,
  Search,
  Layers,
  Shield,
  List,
  BarChart3,
} from 'lucide-react';
import advancedApi from '../../../api/testApi';
import DependencyGraph, { GraphNode, GraphEdge, DependencyGraphData } from '../components/DependencyGraph';

interface TestImpactDetail {
  testId: string;
  testIssueKey?: string;
  testName?: string;
  testType?: string;
  status?: string;
  impactLevel?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  riskScore?: number;
  reason?: string;
  cascadeLevel?: number;
  componentName?: string;
  requirementKey?: string;
  dependentTests?: string[];
  mitigationSuggestions?: string[];
}

interface RequirementImpact {
  requirementKey: string;
  requirementTitle?: string;
  fromVersion?: number;
  toVersion?: number;
  changeType?: string;
  changeSummary?: string;
  affectedTestsCount: number;
  affectedTests: TestImpactDetail[];
  riskLevel?: string;
  suggestedActions?: string[];
}

interface BatchImpactResult {
  totalAnalyzed: number;
  totalAffected: number;
  overallRiskScore: number;
  riskLevel?: string;
  allAffectedTests: TestImpactDetail[];
  graphData: { id: string; sourceType: string; sourceId: string; sourceLabel?: string; targetType: string; targetId: string; targetLabel?: string; impactType: string; weight: number; cascadeDepth?: number }[];
  suggestedSuites?: string[];
  mitigationSummary?: string[];
}

type ViewMode = 'test' | 'requirement' | 'affected';

const ImpactAnalysisPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [activeView, setActiveView] = useState<ViewMode>('test');
  const [selectedTestId, setSelectedTestId] = useState<string>('');
  const [selectedRequirement, setSelectedRequirement] = useState<string>('');
  const [testSearch, setTestSearch] = useState<string>('');
  const [cascadeDepth, setCascadeDepth] = useState<number>(3);
  const [isLoading, setIsLoading] = useState(false);
  const [testImpact, setTestImpact] = useState<TestImpactDetail | null>(null);
  const [requirementImpact, setRequirementImpact] = useState<RequirementImpact | null>(null);
  const [affectedTests, setAffectedTests] = useState<TestImpactDetail[]>([]);
  const [batchResult, setBatchResult] = useState<BatchImpactResult | null>(null);
  const [graphData, setGraphData] = useState<DependencyGraphData>({ nodes: [], edges: [] });
  const [selectedNodeId, setSelectedNodeId] = useState<string | undefined>();
  const [changeType, setChangeType] = useState<'COMPONENT' | 'REQUIREMENT' | 'FILE'>('COMPONENT');
  const [changeKey, setChangeKey] = useState<string>('');

  // Fetch affected tests when change type/key changes
  useEffect(() => {
    if (projectId && changeKey) {
      fetchAffectedTests();
    }
  }, [changeType, changeKey, projectId]);

  const fetchTestImpact = async () => {
    if (!selectedTestId) return;
    setIsLoading(true);
    try {
      const result = await advancedApi.impact.analyzeTestImpact(selectedTestId, cascadeDepth) as TestImpactDetail;
      setTestImpact(result);
      buildGraphFromImpact(result);
    } catch (error) {
      console.error('Failed to analyze test impact:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchRequirementImpact = async () => {
    if (!selectedRequirement) return;
    setIsLoading(true);
    try {
      const result = await advancedApi.impact.analyzeRequirementImpact(selectedRequirement, 1, 2) as RequirementImpact;
      setRequirementImpact(result);
    } catch (error) {
      console.error('Failed to analyze requirement impact:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchAffectedTests = async () => {
    if (!projectId || !changeKey) return;
    setIsLoading(true);
    try {
      const result = await advancedApi.impact.getAffectedTests(projectId, changeType, changeKey) as TestImpactDetail[];
      setAffectedTests(result);
    } catch (error) {
      console.error('Failed to fetch affected tests:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const buildGraphFromImpact = (impact: TestImpactDetail) => {
    const nodes: GraphNode[] = [
      {
        id: impact.testId,
        label: impact.testIssueKey || impact.testName || 'Test',
        type: 'TEST',
        impactLevel: impact.impactLevel as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL',
      },
    ];

    if (impact.componentName) {
      nodes.push({
        id: `comp-${impact.testId}`,
        label: impact.componentName,
        type: 'COMPONENT',
        impactLevel: impact.impactLevel as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL',
      });
    }

    if (impact.requirementKey) {
      nodes.push({
        id: `req-${impact.testId}`,
        label: impact.requirementKey,
        type: 'REQUIREMENT',
      });
    }

    // Add dependent tests as nodes
    impact.dependentTests?.forEach((testKey, index) => {
      nodes.push({
        id: `dep-${index}-${testKey}`,
        label: testKey,
        type: 'TEST',
        impactLevel: 'MEDIUM',
      });
    });

    const edges: GraphEdge[] = [];

    // Test -> Component edge
    if (impact.componentName) {
      edges.push({
        id: 'e1',
        source: impact.testId,
        target: `comp-${impact.testId}`,
        weight: 1.0,
        impactType: 'DIRECT',
      });
    }

    // Test -> Requirement edge
    if (impact.requirementKey) {
      edges.push({
        id: 'e2',
        source: impact.testId,
        target: `req-${impact.testId}`,
        weight: 0.8,
        impactType: 'DIRECT',
      });
    }

    // Test -> Dependent test edges
    impact.dependentTests?.forEach((testKey, index) => {
      edges.push({
        id: `e-dep-${index}`,
        source: impact.testId,
        target: `dep-${index}-${testKey}`,
        weight: 0.6,
        impactType: 'TRANSITIVE',
      });
    });

    setGraphData({ nodes, edges });
  };

  const buildGraphFromBatchResult = (result: BatchImpactResult) => {
    const nodeMap = new Map<string, GraphNode>();
    const edges: GraphEdge[] = [];

    result.allAffectedTests.forEach((test) => {
      if (!nodeMap.has(test.testId)) {
        nodeMap.set(test.testId, {
          id: test.testId,
          label: test.testIssueKey || test.testName || test.testId,
          type: 'TEST',
          impactLevel: test.impactLevel as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL',
        });
      }

      if (test.componentName) {
        const compId = `comp-${test.componentName}`;
        if (!nodeMap.has(compId)) {
          nodeMap.set(compId, {
            id: compId,
            label: test.componentName,
            type: 'COMPONENT',
            impactLevel: test.impactLevel as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL',
          });
        }
        edges.push({
          id: `e-${test.testId}-${compId}`,
          source: test.testId,
          target: compId,
          weight: 1.0,
          impactType: 'DIRECT',
        });
      }

      if (test.requirementKey) {
        const reqId = `req-${test.requirementKey}`;
        if (!nodeMap.has(reqId)) {
          nodeMap.set(reqId, {
            id: reqId,
            label: test.requirementKey,
            type: 'REQUIREMENT',
          });
        }
        edges.push({
          id: `e-${test.testId}-${reqId}`,
          source: test.testId,
          target: reqId,
          weight: 0.8,
          impactType: 'DIRECT',
        });
      }
    });

    // Add edges from graphData
    result.graphData.forEach((edge, index) => {
      edges.push({
        id: `ge-${index}`,
        source: edge.sourceId,
        target: edge.targetId,
        weight: edge.weight,
        impactType: edge.impactType as 'DIRECT' | 'TRANSITIVE' | 'CASCADING',
      });
    });

    setGraphData({ nodes: Array.from(nodeMap.values()), edges });
  };

  const handleNodeClick = (node: GraphNode) => {
    setSelectedNodeId(node.id);
    if (node.type === 'TEST' && node.id !== testImpact?.testId) {
      setSelectedTestId(node.id);
      // Fetch impact for selected node
      advancedApi.impact.analyzeTestImpact(node.id, cascadeDepth).then(setTestImpact);
    }
  };

  const exportImpactReport = () => {
    let reportData: Record<string, unknown>;

    if (activeView === 'test' && testImpact) {
      reportData = {
        type: 'Test Impact Analysis',
        test: testImpact,
        generatedAt: new Date().toISOString(),
        cascadeDepth,
      };
    } else if (activeView === 'requirement' && requirementImpact) {
      reportData = {
        type: 'Requirement Impact Analysis',
        requirement: requirementImpact,
        generatedAt: new Date().toISOString(),
      };
    } else {
      reportData = {
        type: 'Affected Tests Analysis',
        changeType,
        changeKey,
        affectedTests,
        generatedAt: new Date().toISOString(),
      };
    }

    const blob = new Blob([JSON.stringify(reportData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `impact-report-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const getRiskBadgeColor = (level?: string): string => {
    switch (level) {
      case 'CRITICAL': return 'bg-red-100 text-red-700 border-red-200';
      case 'HIGH': return 'bg-orange-100 text-orange-700 border-orange-200';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-700 border-yellow-200';
      case 'LOW': return 'bg-green-100 text-green-700 border-green-200';
      default: return 'bg-gray-100 text-gray-700 border-gray-200';
    }
  };

  const getRiskScoreColor = (score?: number): string => {
    if (!score) return 'text-gray-600';
    if (score >= 75) return 'text-red-600';
    if (score >= 50) return 'text-orange-600';
    if (score >= 25) return 'text-yellow-600';
    return 'text-green-600';
  };

  return (
    <div className="impact-analysis-page p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <AlertTriangle className="text-orange-500" />
            Impact Analysis
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            Analyze test impact and dependency relationships
          </p>
        </div>
        <button
          onClick={exportImpactReport}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Download size={18} />
          Export Report
        </button>
      </div>

      {/* View Mode Tabs */}
      <div className="tabs mb-6 flex gap-2 border-b">
        <button
          onClick={() => setActiveView('test')}
          className={`px-4 py-2 font-medium flex items-center gap-2 ${
            activeView === 'test'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          <GitBranch size={18} />
          Test Impact
        </button>
        <button
          onClick={() => setActiveView('requirement')}
          className={`px-4 py-2 font-medium flex items-center gap-2 ${
            activeView === 'requirement'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          <TrendingUp size={18} />
          Requirement Impact
        </button>
        <button
          onClick={() => setActiveView('affected')}
          className={`px-4 py-2 font-medium flex items-center gap-2 ${
            activeView === 'affected'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          <AlertTriangle size={18} />
          Affected Tests
        </button>
      </div>

      {/* Controls */}
      <div className="controls mb-6 p-4 bg-gray-50 rounded-lg">
        {activeView === 'test' && (
          <div className="flex gap-4 items-end flex-wrap">
            <div className="flex-1 min-w-[300px]">
              <label className="block text-sm font-medium text-gray-700 mb-1">Test ID</label>
              <input
                type="text"
                placeholder="Enter test ID (UUID)"
                value={selectedTestId}
                onChange={(e) => setSelectedTestId(e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Cascade Depth</label>
              <select
                value={cascadeDepth}
                onChange={(e) => setCascadeDepth(parseInt(e.target.value))}
                className="px-3 py-2 border rounded-lg"
              >
                <option value={1}>Level 1</option>
                <option value={2}>Level 2</option>
                <option value={3}>Level 3</option>
              </select>
            </div>
            <button
              onClick={fetchTestImpact}
              disabled={!selectedTestId || isLoading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <Search size={18} />
              Analyze
            </button>
          </div>
        )}

        {activeView === 'requirement' && (
          <div className="flex gap-4 items-end flex-wrap">
            <div className="flex-1 min-w-[300px]">
              <label className="block text-sm font-medium text-gray-700 mb-1">Requirement Key</label>
              <input
                type="text"
                placeholder="e.g., PROJ-123"
                value={selectedRequirement}
                onChange={(e) => setSelectedRequirement(e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <button
              onClick={fetchRequirementImpact}
              disabled={!selectedRequirement || isLoading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <Search size={18} />
              Analyze
            </button>
          </div>
        )}

        {activeView === 'affected' && (
          <div className="flex gap-4 items-end flex-wrap">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Change Type</label>
              <select
                value={changeType}
                onChange={(e) => setChangeType(e.target.value as 'COMPONENT' | 'REQUIREMENT' | 'FILE')}
                className="px-3 py-2 border rounded-lg"
              >
                <option value="COMPONENT">Component</option>
                <option value="REQUIREMENT">Requirement</option>
                <option value="FILE">File</option>
              </select>
            </div>
            <div className="flex-1 min-w-[300px]">
              <label className="block text-sm font-medium text-gray-700 mb-1">Change Identifier</label>
              <input
                type="text"
                placeholder={
                  changeType === 'COMPONENT'
                    ? 'Component ID (UUID)'
                    : changeType === 'REQUIREMENT'
                    ? 'Requirement key (e.g., PROJ-123)'
                    : 'File path'
                }
                value={changeKey}
                onChange={(e) => setChangeKey(e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
            <button
              onClick={fetchAffectedTests}
              disabled={!changeKey || isLoading}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <Filter size={18} />
              Filter
            </button>
          </div>
        )}
      </div>

      {/* Main Content */}
      <div className="grid grid-cols-3 gap-6">
        {/* Left Panel - Results */}
        <div className="col-span-2 space-y-6">
          {/* Risk Summary Card */}
          {(testImpact || requirementImpact || affectedTests.length > 0) && (
            <div className="risk-summary-card p-4 bg-white rounded-lg border shadow-sm">
              <h3 className="font-semibold mb-3 flex items-center gap-2">
                <Shield size={18} className="text-blue-600" />
                Risk Summary
              </h3>
              <div className="flex gap-6">
                {testImpact && (
                  <>
                    <div className="text-center">
                      <div className={`text-3xl font-bold ${getRiskScoreColor(testImpact.riskScore)}`}>
                        {testImpact.riskScore?.toFixed(1) || '0'}%
                      </div>
                      <div className="text-xs text-gray-500">Risk Score</div>
                    </div>
                    <div className="text-center">
                      <div className={`text-lg font-semibold ${getRiskScoreColor(testImpact.riskScore)}`}>
                        {testImpact.impactLevel || 'UNKNOWN'}
                      </div>
                      <div className="text-xs text-gray-500">Impact Level</div>
                    </div>
                    <div className="text-center">
                      <div className="text-lg font-semibold">
                        {testImpact.dependentTests?.length || 0}
                      </div>
                      <div className="text-xs text-gray-500">Affected Tests</div>
                    </div>
                  </>
                )}
                {requirementImpact && (
                  <>
                    <div className="text-center">
                      <div className={`text-lg font-semibold ${getRiskScoreColor(
                        requirementImpact.riskLevel === 'CRITICAL' ? 80 :
                        requirementImpact.riskLevel === 'HIGH' ? 60 :
                        requirementImpact.riskLevel === 'MEDIUM' ? 30 : 10
                      )}`}>
                        {requirementImpact.riskLevel}
                      </div>
                      <div className="text-xs text-gray-500">Risk Level</div>
                    </div>
                    <div className="text-center">
                      <div className="text-lg font-semibold">{requirementImpact.affectedTestsCount}</div>
                      <div className="text-xs text-gray-500">Affected Tests</div>
                    </div>
                  </>
                )}
                {activeView === 'affected' && (
                  <>
                    <div className="text-center">
                      <div className="text-lg font-semibold">{affectedTests.length}</div>
                      <div className="text-xs text-gray-500">Total Affected</div>
                    </div>
                    <div className="text-center">
                      <div className="text-lg font-semibold">
                        {affectedTests.filter(t => t.impactLevel === 'HIGH' || t.impactLevel === 'CRITICAL').length}
                      </div>
                      <div className="text-xs text-gray-500">High/Critical</div>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

          {/* Dependency Graph */}
          <div className="graph-panel bg-white rounded-lg border shadow-sm p-4">
            <h3 className="font-semibold mb-3 flex items-center gap-2">
              <GitBranch size={18} className="text-blue-600" />
              Dependency Graph
            </h3>
            <div className="h-[400px] relative">
              <DependencyGraph
                data={graphData}
                onNodeClick={handleNodeClick}
                selectedNodeId={selectedNodeId}
              />
            </div>
          </div>

          {/* Test List */}
          {(testImpact?.dependentTests?.length || 0) > 0 && activeView === 'test' && (
            <div className="test-list-panel bg-white rounded-lg border shadow-sm p-4">
              <h3 className="font-semibold mb-3 flex items-center gap-2">
                <Layers size={18} className="text-blue-600" />
                Affected Tests List
              </h3>
              <div className="space-y-2">
                {testImpact?.dependentTests?.map((testKey, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 cursor-pointer"
                    onClick={() => {
                      // Could trigger analysis for this test
                    }}
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-sm">{testKey}</span>
                    </div>
                    <ChevronRight size={16} className="text-gray-400" />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Requirement Impact Test List */}
          {requirementImpact?.affectedTests && requirementImpact.affectedTests.length > 0 && (
            <div className="test-list-panel bg-white rounded-lg border shadow-sm p-4">
              <h3 className="font-semibold mb-3 flex items-center gap-2">
                <List size={18} className="text-blue-600" />
                Tests Covering Requirement
              </h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-2 px-3">Test</th>
                      <th className="text-left py-2 px-3">Type</th>
                      <th className="text-left py-2 px-3">Impact</th>
                      <th className="text-left py-2 px-3">Risk</th>
                    </tr>
                  </thead>
                  <tbody>
                    {requirementImpact.affectedTests.map((test, index) => (
                      <tr key={index} className="border-b hover:bg-gray-50">
                        <td className="py-2 px-3">{test.testIssueKey || test.testId}</td>
                        <td className="py-2 px-3">{test.testType}</td>
                        <td className="py-2 px-3">
                          <span className={`px-2 py-1 rounded text-xs font-medium ${getRiskBadgeColor(test.impactLevel)}`}>
                            {test.impactLevel}
                          </span>
                        </td>
                        <td className={`py-2 px-3 font-medium ${getRiskScoreColor(test.riskScore)}`}>
                          {test.riskScore?.toFixed(1)}%
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Affected Tests List */}
          {activeView === 'affected' && affectedTests.length > 0 && (
            <div className="test-list-panel bg-white rounded-lg border shadow-sm p-4">
              <h3 className="font-semibold mb-3 flex items-center gap-2">
                <AlertTriangle size={18} className="text-orange-500" />
                Tests Affected by Change
              </h3>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-2 px-3">Test</th>
                      <th className="text-left py-2 px-3">Type</th>
                      <th className="text-left py-2 px-3">Status</th>
                      <th className="text-left py-2 px-3">Impact</th>
                      <th className="text-left py-2 px-3">Reason</th>
                    </tr>
                  </thead>
                  <tbody>
                    {affectedTests.map((test, index) => (
                      <tr key={index} className="border-b hover:bg-gray-50">
                        <td className="py-2 px-3 font-medium">
                          {test.testIssueKey || test.testName || test.testId}
                        </td>
                        <td className="py-2 px-3">{test.testType}</td>
                        <td className="py-2 px-3">{test.status}</td>
                        <td className="py-2 px-3">
                          <span className={`px-2 py-1 rounded text-xs font-medium ${getRiskBadgeColor(test.impactLevel)}`}>
                            {test.impactLevel}
                          </span>
                        </td>
                        <td className="py-2 px-3 text-gray-600 text-xs">{test.reason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        {/* Right Panel - Mitigation */}
        <div className="right-panel space-y-6">
          {/* Mitigation Suggestions */}
          {(testImpact?.mitigationSuggestions || requirementImpact?.suggestedActions) && (
            <div className="mitigation-panel bg-white rounded-lg border shadow-sm p-4">
              <h3 className="font-semibold mb-3 flex items-center gap-2">
                <Shield size={18} className="text-green-600" />
                Mitigation Suggestions
              </h3>
              <div className="space-y-2">
                {(testImpact?.mitigationSuggestions || requirementImpact?.suggestedActions)?.map((suggestion, index) => (
                  <div key={index} className="flex items-start gap-2 p-2 bg-green-50 rounded-lg">
                    <div className="w-5 h-5 rounded-full bg-green-500 text-white flex items-center justify-center text-xs flex-shrink-0">
                      {index + 1}
                    </div>
                    <span className="text-sm text-gray-700">{suggestion}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Suggested Test Suites */}
          {batchResult?.suggestedSuites && batchResult.suggestedSuites.length > 0 && (
            <div className="suites-panel bg-white rounded-lg border shadow-sm p-4">
              <h3 className="font-semibold mb-3 flex items-center gap-2">
                <BarChart3 size={18} className="text-blue-600" />
                Suggested Test Suites
              </h3>
              <div className="space-y-2">
                {batchResult.suggestedSuites.map((suite, index) => (
                  <div key={index} className="flex items-center gap-2 p-2 bg-blue-50 rounded-lg">
                    <span className="text-sm font-medium text-blue-700">{suite}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Requirement Summary */}
          {requirementImpact && (
            <div className="requirement-summary bg-white rounded-lg border shadow-sm p-4">
              <h3 className="font-semibold mb-3">Requirement Details</h3>
              <div className="space-y-3 text-sm">
                <div>
                  <span className="text-gray-500">Key:</span>
                  <span className="ml-2 font-medium">{requirementImpact.requirementKey}</span>
                </div>
                {requirementImpact.changeType && (
                  <div>
                    <span className="text-gray-500">Change Type:</span>
                    <span className="ml-2 px-2 py-0.5 bg-gray-100 rounded text-xs">
                      {requirementImpact.changeType}
                    </span>
                  </div>
                )}
                <div>
                  <span className="text-gray-500">Versions:</span>
                  <span className="ml-2">
                    v{requirementImpact.fromVersion} → v{requirementImpact.toVersion}
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Loading State */}
          {isLoading && (
            <div className="loading-state flex items-center justify-center p-8">
              <div className="flex items-center gap-3">
                <RefreshCw size={20} className="animate-spin text-blue-600" />
                <span className="text-gray-600">Analyzing impact...</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Empty State */}
      {!testImpact && !requirementImpact && affectedTests.length === 0 && !isLoading && (
        <div className="empty-state text-center py-16">
          <div className="text-6xl mb-4">🔍</div>
          <h3 className="text-lg font-medium text-gray-700 mb-2">No Impact Analysis Yet</h3>
          <p className="text-gray-500 mb-4">
            Enter a test ID, requirement key, or change identifier to analyze impact
          </p>
        </div>
      )}
    </div>
  );
};

export default ImpactAnalysisPage;