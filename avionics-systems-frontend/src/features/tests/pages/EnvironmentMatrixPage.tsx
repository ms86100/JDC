import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import advancedApi, {
  MatrixConfigurationResponse,
  CombinationResponse,
  DimensionConfig,
} from '../../../api/testApi';
import {
  Grid3X3,
  Plus,
  Search,
  Trash2,
  X,
  Eye,
  Download,
  Upload,
  AlertTriangle,
  Settings,
  Play,
  Copy,
  CheckCircle,
  XCircle,
  RefreshCw,
  Cloud,
  Server,
  GitBranch,
  BarChart3,
  PieChart as PieChartIcon,
  Activity,
  Zap,
  ChevronDown,
  ChevronRight,
  ExternalLink,
  Filter,
  Layers,
  Target,
  TrendingUp,
} from 'lucide-react';

// Heatmap Cell Component
interface HeatmapCellProps {
  xLabel: string;
  yLabel: string;
  value: number;
  isValid: boolean;
  onClick: () => void;
}

const HeatmapCell: React.FC<HeatmapCellProps> = ({ xLabel, yLabel, value, isValid, onClick }) => {
  const getColor = () => {
    if (!isValid) return 'bg-gray-200';
    if (value >= 1.5) return 'bg-green-500';
    if (value >= 1.0) return 'bg-green-400';
    if (value >= 0.5) return 'bg-yellow-400';
    return 'bg-yellow-300';
  };

  return (
    <div
      onClick={onClick}
      className={`w-12 h-12 flex items-center justify-center cursor-pointer transition-all hover:scale-105 ${getColor()}`}
      title={`${xLabel} x ${yLabel}: ${value.toFixed(1)} (${isValid ? 'Valid' : 'Invalid'})`}
    >
      {value > 0 && (
        <span className="text-xs font-bold text-white drop-shadow">
          {value.toFixed(1)}
        </span>
      )}
    </div>
  );
};

// Matrix Visualization Component
interface MatrixVisualizationProps {
  matrix: MatrixConfigurationResponse;
  combinations: CombinationResponse[];
}

const MatrixVisualization: React.FC<MatrixVisualizationProps> = ({ matrix, combinations }) => {
  const [selectedCell, setSelectedCell] = useState<CombinationResponse | null>(null);

  if (matrix.dimensions.length < 2) {
    return (
      <div className="p-8 text-center text-gray-500">
        <Grid3X3 className="w-12 h-12 mx-auto mb-4 text-gray-300" />
        <p>Matrix visualization requires at least 2 dimensions</p>
      </div>
    );
  }

  const dimX = matrix.dimensions[0];
  const dimY = matrix.dimensions[1];

  // Generate heatmap data
  const heatmapData = useMemo(() => {
    const data: Record<string, Record<string, { value: number; valid: boolean; combo: CombinationResponse | null }>> = {};

    for (const combo of combinations) {
      const xVal = combo.combinationData[dimX.name];
      const yVal = combo.combinationData[dimY.name];

      if (xVal && yVal) {
        if (!data[xVal]) data[xVal] = {};
        const existing = data[xVal][yVal];
        data[xVal][yVal] = {
          value: (existing?.value || 0) + (combo.isValid ? 1.5 : 0.5),
          valid: combo.isValid,
          combo: combo,
        };
      }
    }

    return data;
  }, [combinations, dimX.name, dimY.name]);

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-700 mb-4 flex items-center gap-2">
        <BarChart3 className="w-5 h-5 text-blue-500" />
        Matrix Heatmap
      </h3>

      {/* Heatmap Grid */}
      <div className="overflow-x-auto">
        <div className="inline-block min-w-full">
          {/* Y-axis labels */}
          <div className="flex">
            <div className="w-24"></div>
            {dimY.values.map((yVal) => (
              <div key={yVal} className="w-12 text-center text-xs text-gray-500 transform -rotate-45 origin-center">
                {yVal}
              </div>
            ))}
          </div>

          {/* Grid with X labels */}
          {dimX.values.map((xVal) => (
            <div key={xVal} className="flex items-center">
              <div className="w-24 text-sm font-medium text-gray-700 pr-2 text-right truncate">
                {xVal}
              </div>
              {dimY.values.map((yVal) => {
                const cell = heatmapData[xVal]?.[yVal];
                return (
                  <HeatmapCell
                    key={`${xVal}-${yVal}`}
                    xLabel={xVal}
                    yLabel={yVal}
                    value={cell?.value || 0}
                    isValid={cell?.valid !== false}
                    onClick={() => cell?.combo && setSelectedCell(cell.combo)}
                  />
                );
              })}
            </div>
          ))}
        </div>
      </div>

      {/* Legend */}
      <div className="flex items-center gap-6 mt-4 pt-4 border-t">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 bg-green-500 rounded"></div>
          <span className="text-sm text-gray-600">Fully Valid</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 bg-yellow-300 rounded"></div>
          <span className="text-sm text-gray-600">Partial</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 bg-gray-200 rounded"></div>
          <span className="text-sm text-gray-600">Invalid</span>
        </div>
      </div>

      {/* Selected Cell Details */}
      {selectedCell && (
        <div className="mt-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
          <div className="flex items-center justify-between">
            <div>
              <h4 className="font-semibold">Selected Combination</h4>
              <p className="text-sm text-gray-600">
                {Object.entries(selectedCell.combinationData).map(([k, v]) => `${k}: ${v}`).join(' | ')}
              </p>
            </div>
            <div className="flex items-center gap-2">
              {selectedCell.isValid ? (
                <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs font-medium flex items-center gap-1">
                  <CheckCircle className="w-3 h-3" /> Valid
                </span>
              ) : (
                <span className="px-2 py-1 bg-red-100 text-red-700 rounded text-xs font-medium flex items-center gap-1">
                  <XCircle className="w-3 h-3" /> Invalid
                </span>
              )}
              <button
                onClick={() => setSelectedCell(null)}
                className="p-1 hover:bg-blue-200 rounded"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {selectedCell.validationErrors && selectedCell.validationErrors.length > 0 && (
            <div className="mt-2 text-sm text-red-600">
              {selectedCell.validationErrors.map((err, i) => (
                <p key={i}>• {err.details}</p>
              ))}
            </div>
          )}

          <div className="mt-3 flex gap-2">
            <button className="px-3 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700">
              Provision
            </button>
            <button className="px-3 py-1.5 border border-gray-300 rounded text-sm hover:bg-gray-50">
              View Details
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

// Distribution Chart Component
interface DistributionChartProps {
  matrix: MatrixConfigurationResponse;
  combinations: CombinationResponse[];
}

const DistributionChart: React.FC<DistributionChartProps> = ({ matrix, combinations }) => {
  const distribution = useMemo(() => {
    const stats: Record<string, Record<string, number>> = {};

    for (const combo of combinations) {
      for (const dim of matrix.dimensions) {
        const value = combo.combinationData[dim.name];
        if (value) {
          if (!stats[dim.name]) stats[dim.name] = {};
          stats[dim.name][value] = (stats[dim.name][value] || 0) + 1;
        }
      }
    }

    return stats;
  }, [matrix.dimensions, combinations]);

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-700 mb-4 flex items-center gap-2">
        <PieChartIcon className="w-5 h-5 text-purple-500" />
        Value Distribution
      </h3>

      <div className="space-y-4">
        {matrix.dimensions.map((dim) => (
          <div key={dim.name}>
            <h4 className="text-sm font-medium text-gray-600 mb-2">{dim.name}</h4>
            <div className="space-y-1">
              {Object.entries(distribution[dim.name] || {}).map(([value, count]) => (
                <div key={value} className="flex items-center gap-3">
                  <div className="w-24 text-sm text-gray-700 truncate">{value}</div>
                  <div className="flex-1 h-4 bg-gray-100 rounded overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-blue-400 to-blue-600 rounded"
                      style={{ width: `${(count / combinations.length) * 100}%` }}
                    />
                  </div>
                  <div className="w-12 text-sm text-gray-500 text-right">{count}</div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// Cloud Provider Panel
const CloudProviderPanel: React.FC = () => {
  const [selectedProvider, setSelectedProvider] = useState<string | null>(null);

  const providers = [
    { id: 'AWS', name: 'Amazon Web Services', color: 'bg-orange-500', regions: 25 },
    { id: 'AZURE', name: 'Microsoft Azure', color: 'bg-blue-500', regions: 60 },
    { id: 'GCP', name: 'Google Cloud Platform', color: 'bg-red-500', regions: 28 },
    { id: 'BROWSERSTACK', name: 'BrowserStack', color: 'bg-blue-400', regions: 0 },
    { id: 'SAUCELABS', name: 'Sauce Labs', color: 'bg-green-500', regions: 0 },
  ];

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-700 mb-4 flex items-center gap-2">
        <Cloud className="w-5 h-5 text-cyan-500" />
        Cloud Providers
      </h3>

      <div className="space-y-3">
        {providers.map((provider) => (
          <div
            key={provider.id}
            onClick={() => setSelectedProvider(selectedProvider === provider.id ? null : provider.id)}
            className={`p-4 border rounded-lg cursor-pointer transition-all ${
              selectedProvider === provider.id
                ? 'border-blue-500 bg-blue-50'
                : 'border-gray-200 hover:border-gray-300'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 ${provider.color} rounded-lg flex items-center justify-center`}>
                  <Server className="w-5 h-5 text-white" />
                </div>
                <div>
                  <h4 className="font-medium text-gray-900">{provider.name}</h4>
                  <p className="text-xs text-gray-500">
                    {provider.regions > 0 ? `${provider.regions} regions` : 'Cloud-based testing'}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs">Active</span>
                {selectedProvider === provider.id ? (
                  <ChevronDown className="w-4 h-4 text-gray-400" />
                ) : (
                  <ChevronRight className="w-4 h-4 text-gray-400" />
                )}
              </div>
            </div>

            {selectedProvider === provider.id && (
              <div className="mt-4 pt-4 border-t">
                <div className="grid grid-cols-3 gap-4">
                  <div className="text-center p-3 bg-white rounded border">
                    <p className="text-2xl font-bold text-gray-900">
                      {provider.id === 'AWS' ? '50+' : provider.id === 'AZURE' ? '100+' : provider.id === 'GCP' ? '40+' : 'N/A'}
                    </p>
                    <p className="text-xs text-gray-500">Instance Types</p>
                  </div>
                  <div className="text-center p-3 bg-white rounded border">
                    <p className="text-2xl font-bold text-gray-900">
                      {provider.id === 'AWS' ? '24' : provider.id === 'AZURE' ? '60' : provider.id === 'GCP' ? '35' : 'N/A'}
                    </p>
                    <p className="text-xs text-gray-500">Regions</p>
                  </div>
                  <div className="text-center p-3 bg-white rounded border">
                    <p className="text-2xl font-bold text-gray-900">
                      {provider.id === 'AWS' ? '99.99' : provider.id === 'AZURE' ? '99.95' : provider.id === 'GCP' ? '99.97' : 'N/A'}
                    </p>
                    <p className="text-xs text-gray-500">Uptime %</p>
                  </div>
                </div>
                <button className="mt-4 w-full flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                  <Zap className="w-4 h-4" />
                  Configure Provider
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// Execution Plan Panel
interface ExecutionPlanPanelProps {
  matrix: MatrixConfigurationResponse;
}

const ExecutionPlanPanel: React.FC<ExecutionPlanPanelProps> = ({ matrix }) => {
  const [testCount, setTestCount] = useState(10);

  const calculatePlan = () => {
    const totalTests = matrix.validCombinations * testCount;
    const estimatedMinutes = Math.ceil(totalTests / 5); // 5 tests parallel
    return {
      totalCombinations: matrix.validCombinations,
      totalTests,
      estimatedMinutes,
      groups: Math.ceil(totalTests / 50),
    };
  };

  const plan = calculatePlan();

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-700 mb-4 flex items-center gap-2">
        <Target className="w-5 h-5 text-red-500" />
        Execution Plan
      </h3>

      <div className="space-y-4">
        <div className="flex items-center gap-4">
          <label className="text-sm text-gray-600">Test Cases per Combination:</label>
          <input
            type="number"
            value={testCount}
            onChange={(e) => setTestCount(parseInt(e.target.value) || 1)}
            className="w-20 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            min="1"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="p-4 bg-blue-50 rounded-lg text-center">
            <p className="text-3xl font-bold text-blue-600">{plan.totalCombinations}</p>
            <p className="text-sm text-gray-600">Combinations</p>
          </div>
          <div className="p-4 bg-green-50 rounded-lg text-center">
            <p className="text-3xl font-bold text-green-600">{plan.totalTests}</p>
            <p className="text-sm text-gray-600">Total Tests</p>
          </div>
          <div className="p-4 bg-purple-50 rounded-lg text-center">
            <p className="text-3xl font-bold text-purple-600">{plan.groups}</p>
            <p className="text-sm text-gray-600">Execution Groups</p>
          </div>
          <div className="p-4 bg-yellow-50 rounded-lg text-center">
            <p className="text-3xl font-bold text-yellow-600">~{plan.estimatedMinutes}m</p>
            <p className="text-sm text-gray-600">Est. Duration</p>
          </div>
        </div>

        <button className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
          <Play className="w-4 h-4" />
          Generate Execution Plan
        </button>
      </div>
    </div>
  );
};

// Compatibility Checker
const CompatibilityChecker: React.FC = () => {
  const [requirements, setRequirements] = useState<Record<string, string>>({});
  const [result, setResult] = useState<{ compatible: number; issues: string[] } | null>(null);

  const handleCheck = () => {
    // Mock result
    setResult({
      compatible: 12,
      issues: [],
    });
  };

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6">
      <h3 className="text-lg font-semibold text-gray-700 mb-4 flex items-center gap-2">
        <Activity className="w-5 h-5 text-green-500" />
        Compatibility Checker
      </h3>

      <div className="space-y-3">
        <div className="flex items-center gap-2">
          <input
            type="text"
            placeholder="Browser: Chrome"
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            onChange={(e) => {
              const [key, value] = e.target.value.split(':');
              if (key && value) {
                setRequirements({ ...requirements, [key.trim()]: value.trim() });
              }
            }}
          />
          <button
            onClick={handleCheck}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
          >
            Check
          </button>
        </div>

        {result && (
          <div className="p-4 bg-green-50 rounded-lg border border-green-200">
            <div className="flex items-center gap-2 mb-2">
              <CheckCircle className="w-5 h-5 text-green-600" />
              <span className="font-semibold text-green-800">
                {result.compatible} Compatible Combinations
              </span>
            </div>
            {result.issues.length === 0 ? (
              <p className="text-sm text-green-600">All requirements are compatible with existing matrices.</p>
            ) : (
              <ul className="text-sm text-red-600">
                {result.issues.map((issue, i) => (
                  <li key={i}>• {issue}</li>
                ))}
              </ul>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

// Main Page Component
export const EnvironmentMatrixPage: React.FC<{ projectId?: string }> = ({ projectId: propProjectId }) => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedMatrix, setSelectedMatrix] = useState<MatrixConfigurationResponse | null>(null);
  const [activeTab, setActiveTab] = useState<'visualization' | 'providers' | 'execution'>('visualization');

  // Fetch matrices
  const { data: matrices = [], isLoading, refetch } = useQuery({
    queryKey: ['environment-matrices', propProjectId],
    queryFn: () => advancedApi.environmentMatrix.list(propProjectId || ''),
    enabled: !!propProjectId,
  });

  // Fetch combinations for selected matrix
  const { data: combinations = [] } = useQuery({
    queryKey: ['matrix-combinations', selectedMatrix?.id],
    queryFn: () => selectedMatrix ? advancedApi.environmentMatrix.getCombinations(selectedMatrix.id) : Promise.resolve([]),
    enabled: !!selectedMatrix,
  });

  // Create matrix mutation
  const createMatrixMutation = useMutation({
    mutationFn: (data: any) => advancedApi.environmentMatrix.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['environment-matrices'] });
    },
  });

  // Delete matrix mutation
  const deleteMutation = useMutation({
    mutationFn: (id: string) => advancedApi.environmentMatrix.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['environment-matrices'] });
      if (selectedMatrix) setSelectedMatrix(null);
    },
  });

  const filteredMatrices = useMemo(() => {
    return matrices.filter((m) => m.name.toLowerCase().includes(searchQuery.toLowerCase()));
  }, [matrices, searchQuery]);

  const handleCreateMatrix = () => {
    if (!propProjectId) return;

    createMatrixMutation.mutate({
      projectId: propProjectId,
      name: `Matrix ${matrices.length + 1}`,
      description: 'New environment matrix',
      dimensions: [
        { name: 'Browser', values: ['Chrome', 'Firefox', 'Safari'], type: 'SINGLE_SELECT' },
        { name: 'OS', values: ['Windows', 'Mac', 'Linux'], type: 'SINGLE_SELECT' },
      ],
    });
  };

  return (
    <div className="h-full flex bg-gray-50">
      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <div className="bg-white px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-3">
                <Grid3X3 className="w-7 h-7 text-blue-600" />
                Environment Matrix
              </h1>
              <p className="text-sm text-gray-500 mt-1">Create, visualize, and manage test environment combinations</p>
            </div>
            {propProjectId && (
              <button
                onClick={handleCreateMatrix}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg"
              >
                <Plus className="w-4 h-4" />
                Create Matrix
              </button>
            )}
          </div>

          {/* Search */}
          <div className="mt-4 flex items-center gap-4">
            <div className="flex-1 relative max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search matrices..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <button onClick={() => refetch()} className="p-2 hover:bg-gray-100 rounded">
              <RefreshCw className="w-5 h-5 text-gray-400" />
            </button>
          </div>
        </div>

        <div className="flex-1 flex">
          {/* Matrix List Sidebar */}
          <div className="w-80 border-r border-gray-200 bg-white overflow-auto p-4">
            {isLoading ? (
              <div className="flex items-center justify-center h-32">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
              </div>
            ) : filteredMatrices.length === 0 ? (
              <div className="text-center py-8">
                <Grid3X3 className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">No matrices</h3>
                <p className="text-gray-500">Create a matrix to get started</p>
              </div>
            ) : (
              <div className="space-y-3">
                {filteredMatrices.map((matrix) => (
                  <div
                    key={matrix.id}
                    onClick={() => setSelectedMatrix(matrix)}
                    className={`p-4 border rounded-lg cursor-pointer transition-all ${
                      selectedMatrix?.id === matrix.id
                        ? 'border-blue-500 bg-blue-50 shadow-md'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <h3 className="font-semibold text-gray-900">{matrix.name}</h3>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          if (confirm('Delete this matrix?')) deleteMutation.mutate(matrix.id);
                        }}
                        className="p-1 hover:bg-red-100 rounded text-red-500"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    {/* Stats */}
                    <div className="grid grid-cols-3 gap-2 text-center">
                      <div className="p-2 bg-gray-50 rounded">
                        <p className="text-lg font-bold text-gray-900">{matrix.totalCombinations}</p>
                        <p className="text-xs text-gray-500">Total</p>
                      </div>
                      <div className="p-2 bg-green-50 rounded">
                        <p className="text-lg font-bold text-green-600">{matrix.validCombinations}</p>
                        <p className="text-xs text-gray-500">Valid</p>
                      </div>
                      <div className="p-2 bg-red-50 rounded">
                        <p className="text-lg font-bold text-red-600">{matrix.invalidCombinations}</p>
                        <p className="text-xs text-gray-500">Invalid</p>
                      </div>
                    </div>

                    {/* Dimensions */}
                    <div className="mt-2 flex flex-wrap gap-1">
                      {(Array.isArray(matrix.dimensions) ? matrix.dimensions : []).slice(0, 3).map((dim, i) => (
                        <span key={i} className="px-2 py-0.5 bg-gray-100 rounded text-xs">
                          {dim.name}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Main Content Area */}
          <div className="flex-1 overflow-auto p-6">
            {selectedMatrix ? (
              <div className="space-y-6">
                {/* Matrix Header */}
                <div className="bg-white rounded-xl border border-gray-200 p-6">
                  <div className="flex items-center justify-between">
                    <div>
                      <h2 className="text-xl font-bold text-gray-900">{selectedMatrix.name}</h2>
                      {selectedMatrix.description && (
                        <p className="text-gray-500 mt-1">{selectedMatrix.description}</p>
                      )}
                    </div>
                    <div className="flex items-center gap-2">
                      <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">
                        <Download className="w-4 h-4" />
                        Export
                      </button>
                      <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                        <Play className="w-4 h-4" />
                        Execute
                      </button>
                    </div>
                  </div>

                  {/* Quick Stats */}
                  <div className="grid grid-cols-4 gap-4 mt-6">
                    <div className="p-4 bg-blue-50 rounded-lg text-center">
                      <p className="text-3xl font-bold text-blue-600">{selectedMatrix.totalCombinations}</p>
                      <p className="text-sm text-gray-600">Total Combinations</p>
                    </div>
                    <div className="p-4 bg-green-50 rounded-lg text-center">
                      <p className="text-3xl font-bold text-green-600">{selectedMatrix.validCombinations}</p>
                      <p className="text-sm text-gray-600">Valid</p>
                    </div>
                    <div className="p-4 bg-red-50 rounded-lg text-center">
                      <p className="text-3xl font-bold text-red-600">{selectedMatrix.invalidCombinations}</p>
                      <p className="text-sm text-gray-600">Invalid</p>
                    </div>
                    <div className="p-4 bg-purple-50 rounded-lg text-center">
                      <p className="text-3xl font-bold text-purple-600">
                        {selectedMatrix.totalCombinations > 0
                          ? Math.round((selectedMatrix.validCombinations / selectedMatrix.totalCombinations) * 100)
                          : 0}%
                      </p>
                      <p className="text-sm text-gray-600">Coverage</p>
                    </div>
                  </div>
                </div>

                {/* Tabs */}
                <div className="flex items-center gap-4 border-b border-gray-200">
                  <button
                    onClick={() => setActiveTab('visualization')}
                    className={`px-4 py-2 font-medium border-b-2 ${
                      activeTab === 'visualization'
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  >
                    Visualization
                  </button>
                  <button
                    onClick={() => setActiveTab('providers')}
                    className={`px-4 py-2 font-medium border-b-2 ${
                      activeTab === 'providers'
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  >
                    Cloud Providers
                  </button>
                  <button
                    onClick={() => setActiveTab('execution')}
                    className={`px-4 py-2 font-medium border-b-2 ${
                      activeTab === 'execution'
                        ? 'border-blue-500 text-blue-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  >
                    Execution Plan
                  </button>
                </div>

                {/* Tab Content */}
                {activeTab === 'visualization' && (
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <MatrixVisualization matrix={selectedMatrix} combinations={combinations} />
                    <DistributionChart matrix={selectedMatrix} combinations={combinations} />
                  </div>
                )}

                {activeTab === 'providers' && (
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <CloudProviderPanel />
                    <CompatibilityChecker />
                  </div>
                )}

                {activeTab === 'execution' && (
                  <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <ExecutionPlanPanel matrix={selectedMatrix} />
                    <div className="bg-white rounded-xl border border-gray-200 p-6">
                      <h3 className="text-lg font-semibold text-gray-700 mb-4 flex items-center gap-2">
                        <GitBranch className="w-5 h-5 text-blue-500" />
                        Provisioning Rules
                      </h3>
                      <div className="space-y-3">
                        {[
                          { name: 'AWS Auto-Scale', type: 'AWS', status: 'Active' },
                          { name: 'BrowserStack CI', type: 'BROWSERSTACK', status: 'Active' },
                          { name: 'Docker Local', type: 'DOCKER', status: 'Active' },
                        ].map((rule, i) => (
                          <div key={i} className="p-4 border border-gray-200 rounded-lg flex items-center justify-between">
                            <div className="flex items-center gap-3">
                              <Server className="w-5 h-5 text-gray-400" />
                              <div>
                                <p className="font-medium">{rule.name}</p>
                                <p className="text-xs text-gray-500">{rule.type}</p>
                              </div>
                            </div>
                            <span className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs">{rule.status}</span>
                          </div>
                        ))}
                        <button className="w-full flex items-center justify-center gap-2 px-4 py-2 border border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-blue-500 hover:text-blue-600">
                          <Plus className="w-4 h-4" />
                          Add Rule
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center h-full text-center">
                <Layers className="w-16 h-16 text-gray-300 mb-4" />
                <h3 className="text-xl font-medium text-gray-900 mb-2">Select a Matrix</h3>
                <p className="text-gray-500">Choose a matrix from the sidebar to view and manage its configurations</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default EnvironmentMatrixPage;