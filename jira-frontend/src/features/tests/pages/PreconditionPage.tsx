import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import {
  Plus, Search, Filter, MoreVertical, Edit2, Trash2, Link2,
  Unlink, CheckCircle, XCircle, AlertTriangle, Clock, Tag,
  Copy, ArrowRight, ChevronDown, ChevronRight, Settings, Database
} from 'lucide-react';

// Types
interface Precondition {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  type: 'DATABASE' | 'API' | 'FILE' | 'CONFIG' | 'ENVIRONMENT' | 'CUSTOM';
  evaluationMode: 'ALWAYS' | 'IF_EXISTS' | 'OPTIONAL';
  expectedResult?: string;
  createdAt: string;
  updatedAt: string;
  linkedTestsCount?: number;
}

interface PreconditionTemplate {
  id: string;
  name: string;
  type: string;
  description: string;
  expectedResult?: string;
  usageCount: number;
}

interface TestPreconditionLink {
  id: string;
  testId: string;
  testName?: string;
  preconditionId: string;
  preconditionName: string;
  stepOrder?: number;
  isRequired: boolean;
}

interface EvaluationResult {
  preconditionId: string;
  preconditionName: string;
  status: 'PASSED' | 'FAILED' | 'SKIPPED' | 'ERROR';
  message?: string;
  actualValue?: string;
  evaluatedAt: string;
}

// Confirmation Dialog
const ConfirmDialog: React.FC<{
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  variant?: 'default' | 'danger';
}> = ({ open, title, message, confirmLabel = 'Confirm', onConfirm, onCancel, variant = 'default' }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onCancel}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-2">{title}</h3>
          <p className="text-gray-600 mb-6">{message}</p>
          <div className="flex justify-end gap-3">
            <button onClick={onCancel} className="btn btn-secondary">Cancel</button>
            <button
              onClick={onConfirm}
              className={`btn ${variant === 'danger' ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}
            >
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Stats Card
interface StatsCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  color?: string;
}

const StatsCard: React.FC<StatsCardProps> = ({ title, value, subtitle, icon, color = 'blue' }) => {
  const colorClasses: Record<string, { bg: string; icon: string }> = {
    blue: { bg: 'bg-blue-50', icon: 'text-blue-500' },
    green: { bg: 'bg-green-50', icon: 'text-green-500' },
    red: { bg: 'bg-red-50', icon: 'text-red-500' },
    yellow: { bg: 'bg-yellow-50', icon: 'text-yellow-500' },
    purple: { bg: 'bg-purple-50', icon: 'text-purple-500' },
  };

  const colors = colorClasses[color] || colorClasses.blue;

  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-2xl font-bold mt-1">{value}</p>
          {subtitle && <p className="text-xs text-gray-400 mt-1">{subtitle}</p>}
        </div>
        <div className={`${colors.bg} p-3 rounded-lg`}>
          <div className={colors.icon}>{icon}</div>
        </div>
      </div>
    </div>
  );
};

// Type Badge
const TypeBadge: React.FC<{ type: string }> = ({ type }) => {
  const typeConfig: Record<string, { bg: string; text: string }> = {
    DATABASE: { bg: 'bg-purple-100', text: 'text-purple-700' },
    API: { bg: 'bg-blue-100', text: 'text-blue-700' },
    FILE: { bg: 'bg-green-100', text: 'text-green-700' },
    CONFIG: { bg: 'bg-yellow-100', text: 'text-yellow-700' },
    ENVIRONMENT: { bg: 'bg-orange-100', text: 'text-orange-700' },
    CUSTOM: { bg: 'bg-gray-100', text: 'text-gray-700' },
  };

  const config = typeConfig[type] || typeConfig.CUSTOM;

  return (
    <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${config.bg} ${config.text}`}>
      {type}
    </span>
  );
};

// Status Badge
const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const statusConfig: Record<string, { bg: string; text: string; icon: React.ReactNode }> = {
    PASSED: { bg: 'bg-green-100', text: 'text-green-700', icon: <CheckCircle size={12} /> },
    FAILED: { bg: 'bg-red-100', text: 'text-red-700', icon: <XCircle size={12} /> },
    SKIPPED: { bg: 'bg-gray-100', text: 'text-gray-700', icon: <Clock size={12} /> },
    ERROR: { bg: 'bg-orange-100', text: 'text-orange-700', icon: <AlertTriangle size={12} /> },
  };

  const config = statusConfig[status] || statusConfig.SKIPPED;

  return (
    <span className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs font-medium ${config.bg} ${config.text}`}>
      {config.icon} {status}
    </span>
  );
};

// Create/Edit Precondition Modal
interface PreconditionModalProps {
  open: boolean;
  precondition?: Precondition | null;
  projectId: string;
  onClose: () => void;
  onSuccess: () => void;
}

const PreconditionModal: React.FC<PreconditionModalProps> = ({
  open,
  precondition,
  projectId,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState({
    name: precondition?.name || '',
    description: precondition?.description || '',
    type: precondition?.type || 'CUSTOM' as string,
    evaluationMode: precondition?.evaluationMode || 'ALWAYS' as string,
    expectedResult: precondition?.expectedResult || '',
  });

  const createMutation = useMutation({
    mutationFn: async (data: typeof formData) => {
      const response = await apiClient.post(`/api/preconditions?projectId=${projectId}`, data);
      return response.data;
    },
    onSuccess: () => {
      onSuccess();
      onClose();
    },
  });

  const updateMutation = useMutation({
    mutationFn: async (data: typeof formData) => {
      const response = await apiClient.put(`/api/preconditions/${precondition?.id}`, data);
      return response.data;
    },
    onSuccess: () => {
      onSuccess();
      onClose();
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (precondition) {
      updateMutation.mutate(formData);
    } else {
      createMutation.mutate(formData);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
          <h3 className="text-lg font-semibold mb-4">
            {precondition ? 'Edit Precondition' : 'Create Precondition'}
          </h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Name *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg"
                rows={3}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Type
                </label>
                <select
                  value={formData.type}
                  onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg"
                >
                  <option value="DATABASE">Database</option>
                  <option value="API">API</option>
                  <option value="FILE">File</option>
                  <option value="CONFIG">Config</option>
                  <option value="ENVIRONMENT">Environment</option>
                  <option value="CUSTOM">Custom</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Evaluation Mode
                </label>
                <select
                  value={formData.evaluationMode}
                  onChange={(e) => setFormData({ ...formData, evaluationMode: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg"
                >
                  <option value="ALWAYS">Always</option>
                  <option value="IF_EXISTS">If Exists</option>
                  <option value="OPTIONAL">Optional</option>
                </select>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Expected Result
              </label>
              <input
                type="text"
                value={formData.expectedResult}
                onChange={(e) => setFormData({ ...formData, expectedResult: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg"
                placeholder="Expected value or condition"
              />
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <button type="button" onClick={onClose} className="btn btn-secondary">
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// Link Precondition Modal
interface LinkPreconditionModalProps {
  open: boolean;
  preconditionId: string;
  onClose: () => void;
  onSuccess: () => void;
}

const LinkPreconditionModal: React.FC<LinkPreconditionModalProps> = ({
  open,
  preconditionId,
  onClose,
  onSuccess,
}) => {
  const [testId, setTestId] = useState('');
  const [stepOrder, setStepOrder] = useState<number | undefined>();

  const linkMutation = useMutation({
    mutationFn: async (data: { testId: string; stepOrder?: number }) => {
      const response = await apiClient.post(
        `/api/preconditions/${preconditionId}/link/test/${data.testId}${data.stepOrder ? `?stepOrder=${data.stepOrder}` : ''}`
      );
      return response.data;
    },
    onSuccess: () => {
      onSuccess();
      onClose();
      setTestId('');
      setStepOrder(undefined);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    linkMutation.mutate({ testId, stepOrder });
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-4">Link Precondition to Test</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Test ID *
              </label>
              <input
                type="text"
                value={testId}
                onChange={(e) => setTestId(e.target.value)}
                className="w-full px-3 py-2 border rounded-lg"
                placeholder="Enter test UUID"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Step Order (optional)
              </label>
              <input
                type="number"
                value={stepOrder || ''}
                onChange={(e) => setStepOrder(e.target.value ? parseInt(e.target.value) : undefined)}
                className="w-full px-3 py-2 border rounded-lg"
                placeholder="Link at specific step"
                min={1}
              />
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <button type="button" onClick={onClose} className="btn btn-secondary">
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={linkMutation.isPending}>
                {linkMutation.isPending ? 'Linking...' : 'Link'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// Template Library Panel
interface TemplateLibraryProps {
  onSelect: (template: PreconditionTemplate) => void;
}

const TemplateLibrary: React.FC<TemplateLibraryProps> = ({ onSelect }) => {
  const { data: templates = [] } = useQuery({
    queryKey: ['precondition-templates'],
    queryFn: async () => {
      const response = await apiClient.get('/preconditions/templates');
      return response.data as PreconditionTemplate[];
    },
  });

  return (
    <div className="bg-white rounded-lg border p-4">
      <h3 className="font-semibold mb-4">Precondition Templates</h3>
      <div className="space-y-2">
        {templates.length === 0 ? (
          <p className="text-sm text-gray-500">No templates available</p>
        ) : (
          templates.map((template) => (
            <div
              key={template.id}
              className="p-3 border rounded hover:bg-gray-50 cursor-pointer"
              onClick={() => onSelect(template)}
            >
              <div className="flex items-center justify-between">
                <span className="font-medium text-sm">{template.name}</span>
                <TypeBadge type={template.type} />
              </div>
              <p className="text-xs text-gray-500 mt-1">{template.description}</p>
              <p className="text-xs text-gray-400 mt-1">Used {template.usageCount} times</p>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

// Evaluation Panel
interface EvaluationPanelProps {
  preconditionId: string;
  testId?: string;
}

const EvaluationPanel: React.FC<EvaluationPanelProps> = ({ preconditionId, testId }) => {
  const [result, setResult] = useState<EvaluationResult | null>(null);
  const [isEvaluating, setIsEvaluating] = useState(false);

  const evaluateMutation = useMutation({
    mutationFn: async () => {
      setIsEvaluating(true);
      const url = testId
        ? `/api/preconditions/evaluate/test/${testId}`
        : `/api/preconditions/${preconditionId}/evaluate`;
      const response = await apiClient.post(url);
      return response.data as EvaluationResult;
    },
    onSuccess: (data) => {
      setResult(data);
      setIsEvaluating(false);
    },
    onError: () => {
      setIsEvaluating(false);
    },
  });

  return (
    <div className="bg-white rounded-lg border p-4">
      <h3 className="font-semibold mb-4">Precondition Evaluation</h3>
      <button
        onClick={() => evaluateMutation.mutate()}
        className="btn btn-primary mb-4"
        disabled={isEvaluating}
      >
        {isEvaluating ? 'Evaluating...' : 'Evaluate Precondition'}
      </button>

      {result && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium">Status:</span>
            <StatusBadge status={result.status} />
          </div>
          {result.message && (
            <div>
              <span className="text-sm font-medium">Message:</span>
              <p className="text-sm text-gray-600 mt-1">{result.message}</p>
            </div>
          )}
          {result.actualValue && (
            <div>
              <span className="text-sm font-medium">Actual Value:</span>
              <p className="text-sm text-gray-600 mt-1 font-mono">{result.actualValue}</p>
            </div>
          )}
          <p className="text-xs text-gray-400">
            Evaluated at: {new Date(result.evaluatedAt).toLocaleString()}
          </p>
        </div>
      )}
    </div>
  );
};

// Main Component
const PreconditionPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showLinkModal, setShowLinkModal] = useState(false);
  const [editingPrecondition, setEditingPrecondition] = useState<Precondition | null>(null);
  const [selectedPrecondition, setSelectedPrecondition] = useState<Precondition | null>(null);
  const [selectedPreconditionId, setSelectedPreconditionId] = useState<string | null>(null);
  const [showTemplates, setShowTemplates] = useState(false);
  const [showEvaluation, setShowEvaluation] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    type: 'CUSTOM' as string,
    evaluationMode: 'ALWAYS' as string,
    expectedResult: '',
  });
  const [deleteConfirm, setDeleteConfirm] = useState<{ open: boolean; id: string | null }>({
    open: false,
    id: null,
  });

  // Fetch preconditions
  const { data: preconditions = [], isLoading } = useQuery({
    queryKey: ['preconditions'],
    queryFn: async () => {
      const response = await apiClient.get('/preconditions');
      return response.data as Precondition[];
    },
  });

  // Fetch linked tests
  const { data: linkedTests = [] } = useQuery({
    queryKey: ['precondition-links', selectedPreconditionId],
    queryFn: async () => {
      if (!selectedPreconditionId) return [];
      const response = await apiClient.get(`/api/preconditions/${selectedPreconditionId}/links`);
      return response.data as TestPreconditionLink[];
    },
    enabled: !!selectedPreconditionId,
  });

  // Mutations
  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      await apiClient.delete(`/api/preconditions/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['preconditions'] });
      setDeleteConfirm({ open: false, id: null });
      if (selectedPrecondition?.id === deleteConfirm.id) {
        setSelectedPrecondition(null);
      }
    },
  });

  // Filter preconditions
  const filteredPreconditions = preconditions.filter((precondition) => {
    if (filterType !== 'ALL' && precondition.type !== filterType) return false;
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      return (
        precondition.name.toLowerCase().includes(query) ||
        precondition.description?.toLowerCase().includes(query)
      );
    }
    return true;
  });

  // Stats
  const stats = {
    total: preconditions.length,
    database: preconditions.filter((p) => p.type === 'DATABASE').length,
    api: preconditions.filter((p) => p.type === 'API').length,
    linked: preconditions.filter((p) => (p.linkedTestsCount || 0) > 0).length,
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Precondition Management</h1>
            <p className="text-sm text-gray-500 mt-1">
              Create, manage, and link test preconditions
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setShowTemplates(!showTemplates)}
              className={`btn ${showTemplates ? 'btn-primary' : 'btn-secondary'}`}
            >
              <Database size={16} className="mr-1" />
              Templates
            </button>
            <button
              onClick={() => {
                setEditingPrecondition(null);
                setShowCreateModal(true);
              }}
              className="btn btn-primary"
            >
              <Plus size={16} className="mr-1" />
              Create Precondition
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <StatsCard
            title="Total Preconditions"
            value={stats.total}
            icon={<Tag size={20} />}
            color="blue"
          />
          <StatsCard
            title="Database Checks"
            value={stats.database}
            icon={<Database size={20} />}
            color="purple"
          />
          <StatsCard
            title="API Checks"
            value={stats.api}
            icon={<Link2 size={20} />}
            color="yellow"
          />
          <StatsCard
            title="Linked to Tests"
            value={stats.linked}
            icon={<Link2 size={20} />}
            color="green"
          />
        </div>

        {/* Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Precondition List */}
          <div className="lg:col-span-2 bg-white rounded-lg border">
            <div className="p-4 border-b flex items-center justify-between">
              <h2 className="font-semibold text-gray-900">Preconditions</h2>
              <div className="flex gap-2">
                <select
                  value={filterType}
                  onChange={(e) => setFilterType(e.target.value)}
                  className="px-3 py-1 border rounded text-sm"
                >
                  <option value="ALL">All Types</option>
                  <option value="DATABASE">Database</option>
                  <option value="API">API</option>
                  <option value="FILE">File</option>
                  <option value="CONFIG">Config</option>
                  <option value="ENVIRONMENT">Environment</option>
                  <option value="CUSTOM">Custom</option>
                </select>
                <div className="relative">
                  <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Search preconditions..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="pl-9 pr-3 py-1 border rounded text-sm w-48"
                  />
                </div>
              </div>
            </div>

            <div className="max-h-[500px] overflow-y-auto">
              {isLoading ? (
                <div className="p-4 text-center text-gray-500">Loading...</div>
              ) : filteredPreconditions.length === 0 ? (
                <div className="p-4 text-center text-gray-500">
                  No preconditions found
                </div>
              ) : (
                filteredPreconditions.map((precondition) => (
                  <div
                    key={precondition.id}
                    onClick={() => {
                      setSelectedPrecondition(precondition);
                      setSelectedPreconditionId(precondition.id);
                      setShowEvaluation(false);
                    }}
                    className={`p-4 border-b cursor-pointer transition-colors ${
                      selectedPrecondition?.id === precondition.id
                        ? 'bg-blue-50'
                        : 'hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <h3 className="font-medium text-gray-900">{precondition.name}</h3>
                          <TypeBadge type={precondition.type} />
                        </div>
                        {precondition.description && (
                          <p className="text-sm text-gray-500 mt-1">{precondition.description}</p>
                        )}
                        <div className="flex items-center gap-4 mt-2">
                          <span className="text-xs text-gray-400">
                            <Link2 size={12} className="inline mr-1" />
                            {precondition.linkedTestsCount || 0} linked tests
                          </span>
                          <span className="text-xs text-gray-400">
                            <Clock size={12} className="inline mr-1" />
                            {new Date(precondition.updatedAt).toLocaleDateString()}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setEditingPrecondition(precondition);
                            setShowCreateModal(true);
                          }}
                          className="p-1 rounded hover:bg-gray-200"
                          title="Edit"
                        >
                          <Edit2 size={16} className="text-gray-500" />
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedPreconditionId(precondition.id);
                            setShowLinkModal(true);
                          }}
                          className="p-1 rounded hover:bg-gray-200"
                          title="Link to test"
                        >
                          <Link2 size={16} className="text-gray-500" />
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setDeleteConfirm({ open: true, id: precondition.id });
                          }}
                          className="p-1 rounded hover:bg-red-100"
                          title="Delete"
                        >
                          <Trash2 size={16} className="text-red-500" />
                        </button>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Detail Panel */}
          <div className="space-y-4">
            {/* Template Library */}
            {showTemplates && (
              <TemplateLibrary
                onSelect={(template) => {
                  setFormData({
                    name: template.name,
                    description: template.description,
                    type: template.type,
                    evaluationMode: 'ALWAYS',
                    expectedResult: template.expectedResult || '',
                  });
                  setShowCreateModal(true);
                }}
              />
            )}

            {/* Evaluation Panel */}
            {selectedPrecondition && (
              <div className="bg-white rounded-lg border">
                <div className="p-4 border-b flex items-center justify-between">
                  <h3 className="font-semibold">Precondition Details</h3>
                  <button
                    onClick={() => setShowEvaluation(!showEvaluation)}
                    className={`btn btn-sm ${showEvaluation ? 'btn-primary' : 'btn-secondary'}`}
                  >
                    {showEvaluation ? 'Hide Evaluation' : 'Show Evaluation'}
                  </button>
                </div>
                <div className="p-4">
                  <h4 className="font-medium">{selectedPrecondition.name}</h4>
                  <p className="text-sm text-gray-500 mt-1">{selectedPrecondition.description}</p>
                  <div className="mt-4 space-y-2">
                    <div className="flex justify-between">
                      <span className="text-sm text-gray-500">Type:</span>
                      <TypeBadge type={selectedPrecondition.type} />
                    </div>
                    <div className="flex justify-between">
                      <span className="text-sm text-gray-500">Evaluation Mode:</span>
                      <span className="text-sm">{selectedPrecondition.evaluationMode}</span>
                    </div>
                    {selectedPrecondition.expectedResult && (
                      <div>
                        <span className="text-sm text-gray-500">Expected Result:</span>
                        <p className="text-sm font-mono mt-1">{selectedPrecondition.expectedResult}</p>
                      </div>
                    )}
                  </div>

                  {showEvaluation && (
                    <div className="mt-4">
                      <EvaluationPanel preconditionId={selectedPrecondition.id} />
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Linked Tests */}
            {selectedPreconditionId && linkedTests.length > 0 && (
              <div className="bg-white rounded-lg border">
                <div className="p-4 border-b">
                  <h3 className="font-semibold">Linked Tests</h3>
                </div>
                <div className="max-h-[300px] overflow-y-auto">
                  {linkedTests.map((link) => (
                    <div key={link.id} className="p-4 border-b hover:bg-gray-50">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="font-medium text-sm">{link.testName || link.testId}</p>
                          <p className="text-xs text-gray-500">
                            Step order: {link.stepOrder || 'Any'}
                          </p>
                        </div>
                        <button
                          onClick={() => {
                            apiClient.delete(`/api/preconditions/${selectedPreconditionId}/unlink/test/${link.testId}`);
                            queryClient.invalidateQueries({ queryKey: ['precondition-links'] });
                          }}
                          className="p-1 rounded hover:bg-red-100"
                          title="Unlink"
                        >
                          <Unlink size={16} className="text-red-500" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Modals */}
      <PreconditionModal
        open={showCreateModal}
        precondition={editingPrecondition}
        projectId="default"
        onClose={() => {
          setShowCreateModal(false);
          setEditingPrecondition(null);
        }}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['preconditions'] });
        }}
      />

      <LinkPreconditionModal
        open={showLinkModal}
        preconditionId={selectedPreconditionId || ''}
        onClose={() => setShowLinkModal(false)}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['preconditions'] });
          queryClient.invalidateQueries({ queryKey: ['precondition-links'] });
        }}
      />

      <ConfirmDialog
        open={deleteConfirm.open}
        title="Delete Precondition"
        message="Are you sure you want to delete this precondition? This action cannot be undone."
        confirmLabel="Delete"
        variant="danger"
        onConfirm={() => deleteMutation.mutate(deleteConfirm.id!)}
        onCancel={() => setDeleteConfirm({ open: false, id: null })}
      />
    </div>
  );
};

export default PreconditionPage;