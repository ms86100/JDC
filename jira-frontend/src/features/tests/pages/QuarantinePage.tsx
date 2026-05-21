import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import advancedApi, {
  QuarantineResponse,
  QuarantineDashboardResponse,
  QuarantineRuleResponse
} from '../../../api/testApi';
import {
  Shield, Search, Filter, MoreVertical, Eye, Play, Pause, RotateCcw,
  Trash2, X, ChevronDown, ChevronRight, Clock, AlertTriangle, CheckCircle,
  XCircle, Plus, Settings, Bug, Calendar, AlertCircle as AlertIcon
} from 'lucide-react';

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
            <button onClick={onConfirm} className={`btn ${variant === 'danger' ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}>
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Stats Card Component
interface StatsCardProps {
  title: string;
  value: number | string;
  subtitle?: string;
  icon: React.ReactNode;
  color?: string;
}

const StatsCard: React.FC<StatsCardProps> = ({ title, value, subtitle, icon, color = 'blue' }) => {
  const colorClasses: Record<string, { bg: string; icon: string }> = {
    blue: { bg: 'bg-blue-50', icon: 'text-blue-500' },
    red: { bg: 'bg-red-50', icon: 'text-red-500' },
    green: { bg: 'bg-green-50', icon: 'text-green-500' },
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

// Quarantine Rules Panel
interface QuarantineRulesPanelProps {
  projectId: string;
  onClose: () => void;
}

const QuarantineRulesPanel: React.FC<QuarantineRulesPanelProps> = ({ projectId, onClose }) => {
  const { data: rules = [], isLoading } = useQuery({
    queryKey: ['quarantine-rules', projectId],
    queryFn: () => advancedApi.quarantine.getRules(projectId),
  });

  const createRuleMutation = useMutation({
    mutationFn: (rule: any) => advancedApi.quarantine.createRule(rule),
    onSuccess: () => {
      // Refresh rules
    },
  });

  const deleteRuleMutation = useMutation({
    mutationFn: (ruleId: string) => advancedApi.environmentMatrix.deleteRule(ruleId),
    onSuccess: () => {
      // Refresh rules
    },
  });

  return (
    <div className="w-[480px] border-l border-gray-200 bg-white flex flex-col">
      <div className="flex items-center justify-between p-4 border-b">
        <div>
          <h3 className="font-semibold">Quarantine Rules</h3>
          <p className="text-sm text-gray-500">Configure auto-quarantine rules</p>
        </div>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4">
        <div className="mb-4">
          <button className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg">
            <Plus className="w-4 h-4" />
            Add Rule
          </button>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center h-32">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
          </div>
        ) : rules.length === 0 ? (
          <div className="text-center py-8">
            <Shield className="w-10 h-10 text-gray-400 mx-auto mb-2" />
            <p className="text-gray-500">No quarantine rules configured</p>
            <p className="text-sm text-gray-400">Rules will auto-quarantine tests based on your criteria</p>
          </div>
        ) : (
          <div className="space-y-3">
            {rules.map((rule) => (
              <div key={rule.id} className={`p-3 border rounded ${rule.isActive ? 'border-green-200 bg-green-50' : 'border-gray-200'}`}>
                <div className="flex items-center justify-between">
                  <span className="font-medium">{rule.ruleName}</span>
                  <span className={`text-xs px-2 py-0.5 rounded ${rule.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}`}>
                    {rule.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <p className="text-sm text-gray-600 mt-1">
                  Type: {rule.ruleType.replace('_', ' ')}
                </p>
                <div className="flex items-center justify-between mt-2">
                  <span className="text-xs text-gray-500">
                    Created: {new Date(rule.createdAt).toLocaleDateString()}
                  </span>
                  <div className="flex gap-2">
                    <button className="text-xs text-blue-600 hover:text-blue-800">Edit</button>
                    <button className="text-xs text-red-600 hover:text-red-800">Delete</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

// Quarantine Detail Panel
interface QuarantineDetailPanelProps {
  quarantine: QuarantineResponse;
  onClose: () => void;
  onRestore: () => void;
  onExtend: () => void;
  onCreateBug: () => void;
}

const QuarantineDetailPanel: React.FC<QuarantineDetailPanelProps> = ({
  quarantine,
  onClose,
  onRestore,
  onExtend,
  onCreateBug,
}) => {
  const statusColors: Record<string, string> = {
    candidate: 'bg-yellow-100 text-yellow-800',
    quarantined: 'bg-red-100 text-red-800',
    investigation: 'bg-purple-100 text-purple-800',
    restored: 'bg-green-100 text-green-800',
  };

  return (
    <div className="w-[480px] border-l border-gray-200 bg-white flex flex-col">
      <div className="flex items-center justify-between p-4 border-b">
        <div>
          <h3 className="font-semibold">Quarantine Details</h3>
          <p className="text-sm text-gray-500">{quarantine.testIssueKey || quarantine.testId}</p>
        </div>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4">
        {/* Test Info */}
        <div className="mb-4">
          <h4 className="text-lg font-medium">{quarantine.testName}</h4>
          <span className={`inline-block px-2 py-0.5 rounded text-xs font-medium mt-2 ${statusColors[quarantine.status] || 'bg-gray-100'}`}>
            {quarantine.status?.toUpperCase() || 'UNKNOWN'}
          </span>
        </div>

        {/* Quarantine Info */}
        <div className="space-y-3 mb-4">
          <div className="p-3 bg-gray-50 rounded">
            <span className="text-xs text-gray-500">Quarantine Reason</span>
            <p className="text-sm mt-1">{quarantine.quarantineReason || 'No reason provided'}</p>
          </div>

          <div className="p-3 bg-gray-50 rounded">
            <span className="text-xs text-gray-500">Trigger Type</span>
            <p className="text-sm mt-1">{quarantine.triggerType?.replace('_', ' ') || 'Manual'}</p>
          </div>

          <div className="p-3 bg-gray-50 rounded">
            <span className="text-xs text-gray-500">Triggered By</span>
            <p className="text-sm mt-1">{quarantine.triggeredBy || 'System'}</p>
          </div>

          <div className="p-3 bg-gray-50 rounded">
            <span className="text-xs text-gray-500">Triggered At</span>
            <p className="text-sm mt-1">{new Date(quarantine.triggeredAt).toLocaleString()}</p>
          </div>

          {quarantine.autoRestoreEnabled && (
            <div className="p-3 bg-green-50 rounded">
              <span className="text-xs text-green-600">Auto-Restore Enabled</span>
              {quarantine.autoRestoreConditions && (
                <p className="text-sm mt-1">
                  Conditions: {JSON.stringify(quarantine.autoRestoreConditions)}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Execution Stats */}
        {quarantine.currentExecutionCount !== undefined && (
          <div className="grid grid-cols-2 gap-3 mb-4">
            <div className="p-3 bg-gray-50 rounded text-center">
              <p className="text-2xl font-bold">{quarantine.currentExecutionCount}</p>
              <p className="text-xs text-gray-500">Executions Since Quarantine</p>
            </div>
            <div className="p-3 bg-green-50 rounded text-center">
              <p className="text-2xl font-bold">{quarantine.currentPassCount || 0}</p>
              <p className="text-xs text-gray-500">Passes</p>
            </div>
          </div>
        )}

        {/* Last Execution */}
        {quarantine.lastExecutionAt && (
          <div className="p-3 bg-gray-50 rounded mb-4">
            <span className="text-xs text-gray-500">Last Execution</span>
            <p className="text-sm mt-1">
              {new Date(quarantine.lastExecutionAt).toLocaleString()}
              <span className={`ml-2 ${quarantine.lastStatus === 'PASSED' ? 'text-green-600' : 'text-red-600'}`}>
                ({quarantine.lastStatus || 'UNKNOWN'})
              </span>
            </p>
          </div>
        )}

        {/* Restore Info */}
        {quarantine.restoredAt && (
          <div className="p-3 bg-green-50 rounded mb-4">
            <span className="text-xs text-green-600">Restored</span>
            <p className="text-sm mt-1">
              {new Date(quarantine.restoredAt).toLocaleString()}
              {quarantine.restoredBy && ` by ${quarantine.restoredBy}`}
            </p>
            {quarantine.restoreReason && (
              <p className="text-sm text-gray-600 mt-1">Reason: {quarantine.restoreReason}</p>
            )}
          </div>
        )}

        {/* Actions */}
        <div className="mt-4 pt-4 border-t space-y-2">
          {quarantine.status !== 'restored' && (
            <>
              <button
                onClick={onRestore}
                className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg"
              >
                <RotateCcw className="w-4 h-4" />
                Restore Test
              </button>
              <button
                onClick={onExtend}
                className="w-full flex items-center justify-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                <Calendar className="w-4 h-4" />
                Extend Quarantine
              </button>
            </>
          )}
          <button
            onClick={onCreateBug}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            <Bug className="w-4 h-4" />
            Create Bug Ticket
          </button>
        </div>
      </div>
    </div>
  );
};

// Main Page Component
export const QuarantinePage: React.FC<{ projectId?: string }> = ({ projectId: propProjectId }) => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [expandedQuarantineId, setExpandedQuarantineId] = useState<string | null>(null);
  const [selectedQuarantine, setSelectedQuarantine] = useState<QuarantineResponse | null>(null);
  const [showRulesPanel, setShowRulesPanel] = useState(false);
  const [restoreConfirm, setRestoreConfirm] = useState<{ open: boolean; quarantineId: string | null }>({ open: false, quarantineId: null });

  // Fetch dashboard data
  const { data: dashboard, isLoading: dashboardLoading } = useQuery({
    queryKey: ['quarantine-dashboard', propProjectId],
    queryFn: () => advancedApi.quarantine.getDashboard(propProjectId || ''),
    enabled: !!propProjectId,
  });

  // Fetch all quarantined tests
  const { data: quarantinedTests = [], isLoading, refetch } = useQuery({
    queryKey: ['quarantine-list', propProjectId],
    queryFn: () => advancedApi.quarantine.list(propProjectId || ''),
    enabled: !!propProjectId,
  });

  // Restore mutation
  const restoreMutation = useMutation({
    mutationFn: (quarantineId: string) => advancedApi.quarantine.restore(quarantineId, 'Restored manually'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quarantine'] });
      queryClient.invalidateQueries({ queryKey: ['quarantine-dashboard'] });
      setRestoreConfirm({ open: false, quarantineId: null });
    },
  });

  // Update status mutation
  const updateStatusMutation = useMutation({
    mutationFn: ({ quarantineId, status }: { quarantineId: string; status: string }) =>
      advancedApi.quarantine.updateStatus(quarantineId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['quarantine'] });
    },
  });

  const filteredTests = useMemo(() => {
    return quarantinedTests
      .filter(test => {
        const matchesSearch = !searchQuery ||
          test.testName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          test.testIssueKey?.toLowerCase().includes(searchQuery.toLowerCase());
        const matchesStatus = !filterStatus || test.status === filterStatus;
        return matchesSearch && matchesStatus;
      })
      .sort((a, b) => new Date(b.triggeredAt).getTime() - new Date(a.triggeredAt).getTime());
  }, [quarantinedTests, searchQuery, filterStatus]);

  const handleRestore = () => {
    if (restoreConfirm.quarantineId) {
      restoreMutation.mutate(restoreConfirm.quarantineId);
    }
  };

  const getStatusIcon = (status?: string) => {
    switch (status) {
      case 'candidate': return <AlertTriangle className="w-4 h-4 text-yellow-500" />;
      case 'quarantined': return <Shield className="w-4 h-4 text-red-500" />;
      case 'investigation': return <Search className="w-4 h-4 text-purple-500" />;
      case 'restored': return <CheckCircle className="w-4 h-4 text-green-500" />;
      default: return <Clock className="w-4 h-4 text-gray-400" />;
    }
  };

  const statusColors: Record<string, string> = {
    candidate: 'bg-yellow-100 text-yellow-800',
    quarantined: 'bg-red-100 text-red-800',
    investigation: 'bg-purple-100 text-purple-800',
    restored: 'bg-green-100 text-green-800',
  };

  // Expiring soon tests (within 7 days)
  const expiringSoon = useMemo(() => {
    return quarantinedTests.filter(test => {
      // This would need actual expiry date field
      return false;
    });
  }, [quarantinedTests]);

  return (
    <div className="h-full flex">
      {/* Main Content */}
      <div className="flex-1 flex flex-col bg-gray-50">
        {/* Header */}
        <div className="bg-white px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Quarantine Management</h1>
              <p className="text-sm text-gray-500 mt-1">Manage quarantined tests and review workflows</p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => setShowRulesPanel(true)}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                <Settings className="w-4 h-4" />
                Configure Rules
              </button>
            </div>
          </div>
        </div>

        {/* Stats Cards */}
        {dashboard && (
          <div className="px-6 py-4 grid grid-cols-4 gap-4">
            <StatsCard
              title="Total Quarantined"
              value={dashboard.totalQuarantined}
              subtitle="Currently inactive"
              icon={<Shield className="w-5 h-5" />}
              color="red"
            />
            <StatsCard
              title="Quarantine"
              value={dashboard.quarantinedCount}
              subtitle="Active"
              icon={<Pause className="w-5 h-5" />}
              color="yellow"
            />
            <StatsCard
              title="Investigation"
              value={dashboard.investigationCount}
              subtitle="Being reviewed"
              icon={<Search className="w-5 h-5" />}
              color="purple"
            />
            <StatsCard
              title="Restored This Week"
              value={dashboard.restoredThisWeek}
              subtitle="Recently restored"
              icon={<RotateCcw className="w-5 h-5" />}
              color="green"
            />
          </div>
        )}

        {/* Expiring Soon Alert */}
        {expiringSoon.length > 0 && (
          <div className="px-6 pb-4">
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 flex items-center gap-3">
              <AlertIcon className="w-5 h-5 text-yellow-600" />
              <span className="text-sm text-yellow-800">
                {expiringSoon.length} test(s) expiring soon - review before auto-restore
              </span>
            </div>
          </div>
        )}

        {/* Filters */}
        <div className="px-6 pb-4 flex items-center gap-4">
          <div className="flex-1 relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search quarantined tests..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Statuses</option>
            <option value="candidate">Candidate</option>
            <option value="quarantined">Quarantined</option>
            <option value="investigation">Investigation</option>
            <option value="restored">Restored</option>
          </select>
        </div>

        {/* Table */}
        <div className="flex-1 overflow-auto px-6 pb-6">
          {isLoading || dashboardLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            </div>
          ) : filteredTests.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 text-center bg-white rounded-lg border">
              <Shield className="w-12 h-12 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No quarantined tests</h3>
              <p className="text-gray-500">All tests are active and running</p>
            </div>
          ) : (
            <div className="bg-white rounded-lg border overflow-hidden">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-gray-500 bg-gray-50 border-b">
                    <th className="p-4 w-10"></th>
                    <th className="p-4">Test</th>
                    <th className="p-4">Status</th>
                    <th className="p-4">Reason</th>
                    <th className="p-4">Trigger</th>
                    <th className="p-4">Quarantined</th>
                    <th className="p-4">Auto-Restore</th>
                    <th className="p-4 w-12"></th>
                  </tr>
                </thead>
                <tbody>
                  {filteredTests.map((quarantine) => (
                    <React.Fragment key={quarantine.id}>
                      <tr
                        className="border-b hover:bg-gray-50 cursor-pointer"
                        onClick={() => setExpandedQuarantineId(expandedQuarantineId === quarantine.id ? null : quarantine.id)}
                      >
                        <td className="p-4">
                          {expandedQuarantineId === quarantine.id ? (
                            <ChevronDown className="w-4 h-4 text-gray-400" />
                          ) : (
                            <ChevronRight className="w-4 h-4 text-gray-400" />
                          )}
                        </td>
                        <td className="p-4">
                          <div>
                            <p className="font-medium text-gray-900">{quarantine.testName}</p>
                            {quarantine.testIssueKey && (
                              <p className="text-sm text-gray-500">{quarantine.testIssueKey}</p>
                            )}
                          </div>
                        </td>
                        <td className="p-4">
                          <div className="flex items-center gap-2">
                            {getStatusIcon(quarantine.status)}
                            <span className={`px-2 py-0.5 rounded text-xs font-medium ${statusColors[quarantine.status] || 'bg-gray-100'}`}>
                              {quarantine.status?.replace('_', ' ').toUpperCase() || 'UNKNOWN'}
                            </span>
                          </div>
                        </td>
                        <td className="p-4 text-gray-600 text-sm max-w-xs truncate">
                          {quarantine.quarantineReason || 'No reason'}
                        </td>
                        <td className="p-4">
                          <span className="text-xs bg-gray-100 px-2 py-0.5 rounded">
                            {quarantine.triggerType?.replace('_', ' ') || 'Manual'}
                          </span>
                        </td>
                        <td className="p-4 text-gray-500 text-sm">
                          {new Date(quarantine.triggeredAt).toLocaleDateString()}
                        </td>
                        <td className="p-4">
                          {quarantine.autoRestoreEnabled ? (
                            <CheckCircle className="w-4 h-4 text-green-500" />
                          ) : (
                            <XCircle className="w-4 h-4 text-gray-400" />
                          )}
                        </td>
                        <td className="p-4" onClick={(e) => e.stopPropagation()}>
                          <QuarantineActionsMenu
                            quarantine={quarantine}
                            onView={() => setSelectedQuarantine(quarantine)}
                            onRestore={() => setRestoreConfirm({ open: true, quarantineId: quarantine.id })}
                            onInvestigate={() => updateStatusMutation.mutate({ quarantineId: quarantine.id, status: 'investigation' })}
                          />
                        </td>
                      </tr>
                      {expandedQuarantineId === quarantine.id && (
                        <tr>
                          <td colSpan={8} className="bg-gray-50 p-4">
                            <div className="grid grid-cols-3 gap-4 text-sm">
                              <div>
                                <span className="text-gray-500">Triggered By:</span>
                                <p className="font-medium mt-1">{quarantine.triggeredBy || 'System'}</p>
                              </div>
                              {quarantine.currentExecutionCount !== undefined && (
                                <>
                                  <div>
                                    <span className="text-gray-500">Executions Since Quarantine:</span>
                                    <p className="font-medium mt-1">{quarantine.currentExecutionCount}</p>
                                  </div>
                                  <div>
                                    <span className="text-gray-500">Passes:</span>
                                    <p className="font-medium mt-1">{quarantine.currentPassCount || 0}</p>
                                  </div>
                                </>
                              )}
                            </div>
                            {quarantine.autoRestoreConditions && (
                              <div className="mt-3 p-2 bg-green-50 rounded">
                                <span className="text-xs text-green-600">Auto-restore conditions: {JSON.stringify(quarantine.autoRestoreConditions)}</span>
                              </div>
                            )}
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
      {selectedQuarantine && !showRulesPanel && (
        <QuarantineDetailPanel
          quarantine={selectedQuarantine}
          onClose={() => setSelectedQuarantine(null)}
          onRestore={() => setRestoreConfirm({ open: true, quarantineId: selectedQuarantine.id })}
          onExtend={() => console.log('Extend quarantine')}
          onCreateBug={() => console.log('Create bug')}
        />
      )}

      {/* Rules Panel */}
      {showRulesPanel && propProjectId && (
        <QuarantineRulesPanel
          projectId={propProjectId}
          onClose={() => setShowRulesPanel(false)}
        />
      )}

      {/* Restore Confirmation */}
      <ConfirmDialog
        open={restoreConfirm.open}
        title="Restore Test"
        message="This will restore the test to active status and include it in future test execution cycles."
        confirmLabel="Restore"
        onConfirm={handleRestore}
        onCancel={() => setRestoreConfirm({ open: false, quarantineId: null })}
      />
    </div>
  );
};

// Quarantine Actions Menu
const QuarantineActionsMenu: React.FC<{
  quarantine: QuarantineResponse;
  onView: () => void;
  onRestore: () => void;
  onInvestigate: () => void;
}> = ({ quarantine, onView, onRestore, onInvestigate }) => {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} className="p-1 hover:bg-gray-200 rounded">
        <MoreVertical className="w-4 h-4 text-gray-400" />
      </button>
      {open && (
        <div className="absolute right-0 top-8 bg-white border border-gray-200 rounded-lg shadow-lg z-10 min-w-[160px]">
          <button onClick={() => { onView(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Eye className="w-4 h-4" /> View Details
          </button>
          {quarantine.status !== 'restored' && (
            <>
              <button onClick={() => { onInvestigate(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
                <Search className="w-4 h-4" /> Investigate
              </button>
              <button onClick={() => { onRestore(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 text-green-600 flex items-center gap-2">
                <RotateCcw className="w-4 h-4" /> Restore
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default QuarantinePage;