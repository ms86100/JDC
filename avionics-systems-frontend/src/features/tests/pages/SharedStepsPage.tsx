import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import advancedApi from '../../../api/testApi';
import apiClient from '../../../api/axiosClient';
import {
  Plus, Search, Filter, MoreVertical, Edit2, Trash2, GitBranch,
  ChevronRight, ChevronDown, Copy, ArrowUpDown, X, Eye, Clock, Users
} from 'lucide-react';

// Types
interface SharedStepDto {
  order?: number;
  stepType?: string;
  description: string;
  expectedResult?: string;
  parameters?: Record<string, string>;
}

interface SharedStepResponse {
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

interface SharedStepVersionResponse {
  id: string;
  sharedStepId: string;
  versionNumber: number;
  steps: SharedStepDto[];
  changeSummary?: string;
  createdBy?: string;
  isCurrent: boolean;
  createdAt: string;
}

interface SharedStepImpactResponse {
  testId: string;
  testIssueKey?: string;
  testName?: string;
  usageCount: number;
  lastUsedAt?: string;
  status?: string;
}

interface CreateSharedStepModalProps {
  projectId: string;
  onClose: () => void;
  onSuccess: (step: SharedStepResponse) => void;
}

interface VersionHistorySidebarProps {
  sharedStepId: string;
  onClose: () => void;
  onRestore: (versionId: string) => void;
}

interface SharedStepDetailPanelProps {
  step: SharedStepResponse;
  onClose: () => void;
}

// Confirmation Dialog Component
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

// Create Shared Step Modal
const CreateSharedStepModal: React.FC<CreateSharedStepModalProps> = ({ projectId, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    steps: [] as SharedStepDto[],
  });
  const [newStep, setNewStep] = useState({ stepType: 'GIVEN', description: '', expectedResult: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleAddStep = () => {
    if (!newStep.description.trim()) return;
    setFormData({
      ...formData,
      steps: [...formData.steps, { ...newStep, order: formData.steps.length + 1 }],
    });
    setNewStep({ stepType: 'GIVEN', description: '', expectedResult: '' });
  };

  const handleRemoveStep = (index: number) => {
    setFormData({
      ...formData,
      steps: formData.steps.filter((_, i) => i !== index).map((s, i) => ({ ...s, order: i + 1 })),
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setError('Step name is required');
      return;
    }
    if (formData.steps.length === 0) {
      setError('At least one step is required');
      return;
    }

    setSubmitting(true);
    try {
      const response = await advancedApi.sharedSteps.create({
        projectId,
        name: formData.name,
        description: formData.description,
        steps: formData.steps,
      });
      onSuccess(response);
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create shared step');
    } finally {
      setSubmitting(false);
    }
  };

  const stepTypeColors: Record<string, string> = {
    GIVEN: 'bg-green-100 text-green-800',
    WHEN: 'bg-blue-100 text-blue-800',
    THEN: 'bg-purple-100 text-purple-800',
    AND: 'bg-gray-100 text-gray-800',
    BUT: 'bg-yellow-100 text-yellow-800',
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          <div className="flex items-center justify-between p-4 border-b">
            <h3 className="text-lg font-semibold">Create Shared Step</h3>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
              <X className="w-5 h-5" />
            </button>
          </div>

          <form onSubmit={handleSubmit} className="p-4">
            {error && (
              <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-600 text-sm">
                {error}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">
                  Step Name <span className="text-red-600">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., Login to application"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Description</label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={2}
                  placeholder="Describe what this step does..."
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-2">Steps</label>
                <div className="space-y-2 mb-3">
                  {formData.steps.map((step, index) => (
                    <div key={index} className="flex items-center gap-2 p-2 bg-gray-50 rounded">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${stepTypeColors[step.stepType || 'GIVEN']}`}>
                        {step.stepType || 'GIVEN'}
                      </span>
                      <span className="flex-1 text-sm">{step.description}</span>
                      <button type="button" onClick={() => handleRemoveStep(index)} className="text-gray-400 hover:text-red-600">
                        <X className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>

                <div className="flex gap-2 items-end">
                  <select
                    value={newStep.stepType}
                    onChange={(e) => setNewStep({ ...newStep, stepType: e.target.value })}
                    className="px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="GIVEN">GIVEN</option>
                    <option value="WHEN">WHEN</option>
                    <option value="THEN">THEN</option>
                    <option value="AND">AND</option>
                    <option value="BUT">BUT</option>
                  </select>
                  <input
                    type="text"
                    value={newStep.description}
                    onChange={(e) => setNewStep({ ...newStep, description: e.target.value })}
                    className="flex-1 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Step description..."
                  />
                  <button type="button" onClick={handleAddStep} className="btn btn-secondary">
                    Add Step
                  </button>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6 pt-4 border-t">
              <button type="button" onClick={onClose} className="btn btn-secondary">Cancel</button>
              <button type="submit" disabled={submitting} className="btn btn-primary">
                {submitting ? 'Creating...' : 'Create Step'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// Version History Sidebar
const VersionHistorySidebar: React.FC<VersionHistorySidebarProps> = ({ sharedStepId, onClose, onRestore }) => {
  const { data: versions, isLoading } = useQuery({
    queryKey: ['shared-step-versions', sharedStepId],
    queryFn: () => advancedApi.sharedSteps.getVersions(sharedStepId),
  });

  return (
    <div className="w-80 border-l border-gray-200 bg-white flex flex-col">
      <div className="flex items-center justify-between p-4 border-b">
        <h3 className="font-semibold">Version History</h3>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4">
        {isLoading ? (
          <div className="flex items-center justify-center h-32">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
          </div>
        ) : versions && versions.length > 0 ? (
          <div className="space-y-3">
            {versions.map((version) => (
              <div key={version.id} className={`p-3 border rounded ${version.isCurrent ? 'border-blue-500 bg-blue-50' : 'border-gray-200'}`}>
                <div className="flex items-center justify-between mb-1">
                  <span className="font-medium">Version {version.versionNumber}</span>
                  {version.isCurrent && <span className="text-xs bg-blue-500 text-white px-2 py-0.5 rounded">Current</span>}
                </div>
                {version.changeSummary && <p className="text-sm text-gray-600 mb-2">{version.changeSummary}</p>}
                <div className="flex items-center justify-between text-xs text-gray-500">
                  <span>{version.createdBy || 'System'}</span>
                  <span>{new Date(version.createdAt).toLocaleDateString()}</span>
                </div>
                {!version.isCurrent && (
                  <button
                    onClick={() => onRestore(version.id)}
                    className="mt-2 text-sm text-blue-600 hover:text-blue-800"
                  >
                    Restore this version
                  </button>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center text-gray-500 py-8">No version history available</div>
        )}
      </div>
    </div>
  );
};

// Shared Step Detail Panel
const SharedStepDetailPanel: React.FC<SharedStepDetailPanelProps> = ({ step, onClose }) => {
  const { data: impact } = useQuery({
    queryKey: ['shared-step-impact', step.id],
    queryFn: () => advancedApi.sharedSteps.getImpact(step.id),
  });

  return (
    <div className="w-96 border-l border-gray-200 bg-white flex flex-col">
      <div className="flex items-center justify-between p-4 border-b">
        <h3 className="font-semibold">Step Details</h3>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4">
        <div className="mb-4">
          <h4 className="text-lg font-medium">{step.name}</h4>
          {step.description && <p className="text-sm text-gray-600 mt-1">{step.description}</p>}
          <div className="flex items-center gap-4 mt-2 text-xs text-gray-500">
            <span className="flex items-center gap-1">
              <Clock className="w-3 h-3" />
              v{step.currentVersion}
            </span>
            <span className="flex items-center gap-1">
              <Users className="w-3 h-3" />
              Used in {step.usageCount} tests
            </span>
          </div>
        </div>

        <div className="mb-4">
          <h5 className="text-sm font-medium mb-2">Steps</h5>
          <div className="space-y-2">
            {step.steps.map((s, i) => (
              <div key={i} className="flex items-start gap-2 p-2 bg-gray-50 rounded">
                <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                  s.stepType === 'GIVEN' ? 'bg-green-100 text-green-800' :
                  s.stepType === 'WHEN' ? 'bg-blue-100 text-blue-800' :
                  s.stepType === 'THEN' ? 'bg-purple-100 text-purple-800' :
                  'bg-gray-100 text-gray-800'
                }`}>
                  {s.stepType || 'GIVEN'}
                </span>
                <div>
                  <p className="text-sm">{s.description}</p>
                  {s.expectedResult && <p className="text-xs text-gray-500 mt-1">Expected: {s.expectedResult}</p>}
                </div>
              </div>
            ))}
          </div>
        </div>

        {Array.isArray(impact) && impact.length > 0 && (
          <div>
            <h5 className="text-sm font-medium mb-2">Used In</h5>
            <div className="space-y-2">
              {impact.slice(0, 5).map((item) => (
                <div key={item.testId} className="p-2 border rounded">
                  <p className="text-sm font-medium">{item.testName || item.testIssueKey}</p>
                  <p className="text-xs text-gray-500">
                    {item.usageCount} use{item.usageCount !== 1 ? 's' : ''} - Last used: {item.lastUsedAt ? new Date(item.lastUsedAt).toLocaleDateString() : 'Never'}
                  </p>
                </div>
              ))}
              {impact.length > 5 && (
                <p className="text-sm text-gray-500">+{impact.length - 5} more tests</p>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

// Main Page Component
export const SharedStepsPage: React.FC<{ projectId?: string }> = ({ projectId: propProjectId }) => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [sortField, setSortField] = useState<'name' | 'updatedAt' | 'usageCount'>('updatedAt');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [expandedStepId, setExpandedStepId] = useState<string | null>(null);
  const [selectedStep, setSelectedStep] = useState<SharedStepResponse | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showVersionHistory, setShowVersionHistory] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<{ open: boolean; stepId: string | null }>({ open: false, stepId: null });

  const { data: sharedSteps = [], isLoading, refetch } = useQuery({
    queryKey: ['shared-steps', propProjectId],
    queryFn: () => advancedApi.sharedSteps.list(propProjectId || ''),
    enabled: !!propProjectId,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => advancedApi.sharedSteps.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shared-steps'] });
      setDeleteConfirm({ open: false, stepId: null });
    },
  });

  const duplicateMutation = useMutation({
    mutationFn: (step: SharedStepResponse) => advancedApi.sharedSteps.create({
      projectId: propProjectId || '',
      name: `${step.name} (Copy)`,
      description: step.description,
      steps: step.steps,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shared-steps'] });
    },
  });

  const restoreMutation = useMutation({
    mutationFn: async (versionId: string) => {
      // Implementation would call restore endpoint
      console.log('Restoring version:', versionId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shared-steps'] });
      setShowVersionHistory(false);
    },
  });

  const filteredSteps = useMemo(() => {
    return sharedSteps
      .filter(step => step.name.toLowerCase().includes(searchQuery.toLowerCase()))
      .sort((a, b) => {
        const aVal = a[sortField];
        const bVal = b[sortField];
        const direction = sortDirection === 'asc' ? 1 : -1;
        if (typeof aVal === 'string' && typeof bVal === 'string') {
          return aVal.localeCompare(bVal) * direction;
        }
        return ((aVal as number) < (bVal as number) ? -1 : 1) * direction;
      });
  }, [sharedSteps, searchQuery, sortField, sortDirection]);

  const handleSort = (field: typeof sortField) => {
    if (sortField === field) {
      setSortDirection(d => d === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('desc');
    }
  };

  const handleDelete = () => {
    if (deleteConfirm.stepId) {
      deleteMutation.mutate(deleteConfirm.stepId);
    }
  };

  const handleRestore = (versionId: string) => {
    restoreMutation.mutate(versionId);
  };

  const stepTypeColors: Record<string, string> = {
    GIVEN: 'bg-green-900/50 text-green-400',
    WHEN: 'bg-blue-900/50 text-blue-400',
    THEN: 'bg-purple-900/50 text-purple-400',
    AND: 'bg-slate-700 text-slate-300',
    BUT: 'bg-yellow-900/50 text-yellow-400',
  };

  return (
    <div className="h-full flex">
      {/* Main Content */}
      <div className="flex-1 flex flex-col bg-gray-50">
        {/* Header */}
        <div className="bg-white px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Shared Steps</h1>
              <p className="text-sm text-gray-500 mt-1">Reusable test steps across multiple tests</p>
            </div>
            {propProjectId && (
              <button
                onClick={() => setShowCreateModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg"
              >
                <Plus className="w-4 h-4" />
                Create Shared Step
              </button>
            )}
          </div>

          {/* Search */}
          <div className="mt-4 relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search shared steps..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Table */}
        <div className="flex-1 overflow-auto p-6">
          {isLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            </div>
          ) : filteredSteps.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 text-center bg-white rounded-lg border">
              <GitBranch className="w-12 h-12 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No shared steps yet</h3>
              <p className="text-gray-500 mb-4">Create reusable steps to share across multiple tests</p>
              {propProjectId && (
                <button
                  onClick={() => setShowCreateModal(true)}
                  className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg"
                >
                  <Plus className="w-4 h-4" />
                  Create Shared Step
                </button>
              )}
            </div>
          ) : (
            <div className="bg-white rounded-lg border overflow-hidden">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-gray-500 bg-gray-50 border-b">
                    <th className="p-4 w-10"></th>
                    <th className="p-4 cursor-pointer" onClick={() => handleSort('name')}>
                      <span className="flex items-center gap-2">
                        Name
                        {sortField === 'name' && (
                          <ArrowUpDown className={`w-4 h-4 ${sortDirection === 'asc' ? '' : 'rotate-180'}`} />
                        )}
                      </span>
                    </th>
                    <th className="p-4">Steps</th>
                    <th className="p-4 cursor-pointer" onClick={() => handleSort('usageCount')}>
                      <span className="flex items-center gap-2">
                        Used In
                        {sortField === 'usageCount' && (
                          <ArrowUpDown className={`w-4 h-4 ${sortDirection === 'asc' ? '' : 'rotate-180'}`} />
                        )}
                      </span>
                    </th>
                    <th className="p-4">Version</th>
                    <th className="p-4 cursor-pointer" onClick={() => handleSort('updatedAt')}>
                      <span className="flex items-center gap-2">
                        Last Modified
                        {sortField === 'updatedAt' && (
                          <ArrowUpDown className={`w-4 h-4 ${sortDirection === 'asc' ? '' : 'rotate-180'}`} />
                        )}
                      </span>
                    </th>
                    <th className="p-4 w-12"></th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSteps.map((step) => (
                    <React.Fragment key={step.id}>
                      <tr
                        className="border-b hover:bg-gray-50 cursor-pointer"
                        onClick={() => setExpandedStepId(expandedStepId === step.id ? null : step.id)}
                      >
                        <td className="p-4">
                          {expandedStepId === step.id ? (
                            <ChevronDown className="w-4 h-4 text-gray-400" />
                          ) : (
                            <ChevronRight className="w-4 h-4 text-gray-400" />
                          )}
                        </td>
                        <td className="p-4">
                          <span className="font-medium text-gray-900">{step.name}</span>
                        </td>
                        <td className="p-4 text-gray-600">
                          {step.steps.length} step{step.steps.length !== 1 ? 's' : ''}
                        </td>
                        <td className="p-4">
                          <span className="text-blue-600 hover:text-blue-800">
                            {step.usageCount} test{step.usageCount !== 1 ? 's' : ''}
                          </span>
                        </td>
                        <td className="p-4">
                          <span className="text-sm bg-gray-100 px-2 py-0.5 rounded">v{step.currentVersion}</span>
                        </td>
                        <td className="p-4 text-gray-500 text-sm">
                          {new Date(step.updatedAt).toLocaleDateString()}
                        </td>
                        <td className="p-4" onClick={(e) => e.stopPropagation()}>
                          <StepActionsMenu
                            step={step}
                            onEdit={() => setSelectedStep(step)}
                            onDelete={() => setDeleteConfirm({ open: true, stepId: step.id })}
                            onDuplicate={() => duplicateMutation.mutate(step)}
                            onVersionHistory={() => { setSelectedStep(step); setShowVersionHistory(true); }}
                          />
                        </td>
                      </tr>
                      {expandedStepId === step.id && (
                        <tr>
                          <td colSpan={7} className="bg-gray-50 p-4">
                            <div className="space-y-2">
                              {step.steps.map((s, i) => (
                                <div key={i} className="flex items-start gap-2">
                                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${stepTypeColors[s.stepType || 'GIVEN']}`}>
                                    {s.stepType || 'GIVEN'}
                                  </span>
                                  <span className="text-gray-700">{s.description}</span>
                                </div>
                              ))}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Detail Panel */}
      {selectedStep && !showVersionHistory && (
        <SharedStepDetailPanel step={selectedStep} onClose={() => setSelectedStep(null)} />
      )}

      {/* Version History */}
      {showVersionHistory && selectedStep && (
        <VersionHistorySidebar
          sharedStepId={selectedStep.id}
          onClose={() => setShowVersionHistory(false)}
          onRestore={handleRestore}
        />
      )}

      {/* Create Modal */}
      {showCreateModal && propProjectId && (
        <CreateSharedStepModal
          projectId={propProjectId}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            refetch();
          }}
        />
      )}

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={deleteConfirm.open}
        title="Delete Shared Step"
        message="Are you sure you want to delete this shared step? This action cannot be undone."
        confirmLabel="Delete"
        onConfirm={handleDelete}
        onCancel={() => setDeleteConfirm({ open: false, stepId: null })}
        variant="danger"
      />
    </div>
  );
};

// Step Actions Menu
const StepActionsMenu: React.FC<{
  step: SharedStepResponse;
  onEdit: () => void;
  onDelete: () => void;
  onDuplicate: () => void;
  onVersionHistory: () => void;
}> = ({ onEdit, onDelete, onDuplicate, onVersionHistory }) => {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} className="p-1 hover:bg-gray-200 rounded">
        <MoreVertical className="w-4 h-4 text-gray-400" />
      </button>
      {open && (
        <div className="absolute right-0 top-8 bg-white border border-gray-200 rounded-lg shadow-lg z-10 min-w-[160px]">
          <button onClick={() => { onEdit(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Edit2 className="w-4 h-4" /> Edit
          </button>
          <button onClick={() => { onDuplicate(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Copy className="w-4 h-4" /> Duplicate
          </button>
          <button onClick={() => { onVersionHistory(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Clock className="w-4 h-4" /> Version History
          </button>
          <div className="border-t border-gray-200 my-1" />
          <button onClick={() => { onDelete(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 text-red-600 flex items-center gap-2">
            <Trash2 className="w-4 h-4" /> Delete
          </button>
        </div>
      )}
    </div>
  );
};

export default SharedStepsPage;