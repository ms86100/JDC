import React, { useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import combinedApi, {
  TestPlanResponse,
  TestExecutionResponse,
} from '../../../api/testApi';
import {
  BarChart3, Users, LayoutGrid, Loader2, AlertCircle, CheckCircle, XCircle,
  Ban, SkipForward, ClipboardList,
} from 'lucide-react';

// ─── Types ─────────────────────────────────────────────────────────────────────

interface TestPlanBoardProps {
  projectId: string;
  testPlanId?: string;
}

type GroupBy = 'status' | 'assignee';

const STATUS_COLUMNS = ['TODO', 'IN_PROGRESS', 'PASSED', 'FAILED', 'BLOCKED'] as const;

const STATUS_CONFIG: Record<string, { label: string; color: string; bgColor: string; icon: React.ReactNode }> = {
  TODO: { label: 'To Do', color: 'text-gray-700', bgColor: 'bg-gray-100', icon: <ClipboardList className="w-4 h-4" /> },
  IN_PROGRESS: { label: 'In Progress', color: 'text-blue-700', bgColor: 'bg-blue-100', icon: <Loader2 className="w-4 h-4" /> },
  PASSED: { label: 'Passed', color: 'text-green-700', bgColor: 'bg-green-100', icon: <CheckCircle className="w-4 h-4" /> },
  FAILED: { label: 'Failed', color: 'text-red-700', bgColor: 'bg-red-100', icon: <XCircle className="w-4 h-4" /> },
  BLOCKED: { label: 'Blocked', color: 'text-orange-700', bgColor: 'bg-orange-100', icon: <Ban className="w-4 h-4" /> },
  SKIPPED: { label: 'Skipped', color: 'text-gray-500', bgColor: 'bg-gray-50', icon: <SkipForward className="w-4 h-4" /> },
};

const PRIORITY_COLORS: Record<string, string> = {
  CRITICAL: 'bg-red-500 text-white',
  HIGH: 'bg-orange-500 text-white',
  MEDIUM: 'bg-yellow-400 text-yellow-900',
  LOW: 'bg-green-500 text-white',
};

// ─── Helper: normalize execution status to a column ────────────────────────────

function normalizeStatus(status: string): string {
  const upper = (status || '').toUpperCase().replace(/[\s-]/g, '_');
  if (['PASS', 'PASSED'].includes(upper)) return 'PASSED';
  if (['FAIL', 'FAILED'].includes(upper)) return 'FAILED';
  if (['BLOCK', 'BLOCKED'].includes(upper)) return 'BLOCKED';
  if (['SKIP', 'SKIPPED', 'UNTESTED', 'NOT_RUN'].includes(upper)) return 'TODO';
  if (['IN_PROGRESS', 'EXECUTING', 'RUNNING'].includes(upper)) return 'IN_PROGRESS';
  if (['TODO', 'PENDING', 'QUEUED', 'CREATED'].includes(upper)) return 'TODO';
  return 'TODO';
}

// ─── Helper: get initials from an ID / name string ─────────────────────────────

function getInitials(value?: string | null): string {
  if (!value) return '?';
  const parts = value.split(/[\s._@-]+/).filter(Boolean);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return value.substring(0, 2).toUpperCase();
}

// ─── Stats Bar ─────────────────────────────────────────────────────────────────

const StatsBar: React.FC<{ executions: TestExecutionResponse[] }> = ({ executions }) => {
  const total = executions.length;
  const passed = executions.filter(e => normalizeStatus(e.status) === 'PASSED').length;
  const failed = executions.filter(e => normalizeStatus(e.status) === 'FAILED').length;
  const blocked = executions.filter(e => normalizeStatus(e.status) === 'BLOCKED').length;
  const skipped = executions.filter(e => normalizeStatus(e.status) === 'TODO').length;
  const passRate = total > 0 ? Math.round((passed / total) * 100) : 0;

  return (
    <div className="grid grid-cols-2 md:grid-cols-6 gap-3 mb-6">
      <div className="bg-white rounded-lg border p-3 text-center">
        <div className="text-2xl font-bold text-gray-900">{total}</div>
        <div className="text-xs text-gray-500">Total Tests</div>
      </div>
      <div className="bg-white rounded-lg border p-3 text-center">
        <div className="text-2xl font-bold text-blue-600">{passRate}%</div>
        <div className="text-xs text-gray-500">Pass Rate</div>
      </div>
      <div className="bg-white rounded-lg border p-3 text-center">
        <div className="text-2xl font-bold text-green-600">{passed}</div>
        <div className="text-xs text-gray-500">Passed</div>
      </div>
      <div className="bg-white rounded-lg border p-3 text-center">
        <div className="text-2xl font-bold text-red-600">{failed}</div>
        <div className="text-xs text-gray-500">Failed</div>
      </div>
      <div className="bg-white rounded-lg border p-3 text-center">
        <div className="text-2xl font-bold text-orange-600">{blocked}</div>
        <div className="text-xs text-gray-500">Blocked</div>
      </div>
      <div className="bg-white rounded-lg border p-3 text-center">
        <div className="text-2xl font-bold text-gray-500">{skipped}</div>
        <div className="text-xs text-gray-500">To Do</div>
      </div>
    </div>
  );
};

// ─── Execution Card ────────────────────────────────────────────────────────────

const ExecutionCard: React.FC<{ execution: TestExecutionResponse; onClick: () => void }> = ({ execution, onClick }) => {
  const priorityClass = PRIORITY_COLORS[(execution as any).priority?.toUpperCase()] || 'bg-gray-200 text-gray-700';

  return (
    <div
      onClick={onClick}
      className="bg-white rounded border shadow-sm p-3 cursor-pointer hover:shadow-md transition-shadow"
    >
      <p className="text-sm font-medium text-gray-900 truncate mb-2" title={execution.name}>
        {execution.name || execution.issueKey}
      </p>
      <div className="flex items-center justify-between">
        {(execution as any).priority && (
          <span className={`inline-flex px-1.5 py-0.5 text-xs font-medium rounded ${priorityClass}`}>
            {(execution as any).priority}
          </span>
        )}
        {execution.assigneeId && (
          <span
            className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-blue-500 text-white text-xs font-medium"
            title={execution.assigneeId}
          >
            {getInitials(execution.assigneeId)}
          </span>
        )}
      </div>
    </div>
  );
};

// ─── Board Column ──────────────────────────────────────────────────────────────

const BoardColumn: React.FC<{
  columnKey: string;
  label: string;
  icon: React.ReactNode;
  bgColor: string;
  color: string;
  executions: TestExecutionResponse[];
  onCardClick: (execution: TestExecutionResponse) => void;
}> = ({ columnKey, label, icon, bgColor, color, executions, onCardClick }) => {
  return (
    <div className="flex flex-col min-w-[220px] flex-1">
      <div className={`flex items-center gap-2 px-3 py-2 rounded-t-lg ${bgColor}`}>
        <span className={color}>{icon}</span>
        <span className={`text-sm font-medium ${color}`}>{label}</span>
        <span className={`ml-auto text-xs font-medium ${color} opacity-70`}>{executions.length}</span>
      </div>
      <div className="flex-1 bg-gray-50 rounded-b-lg p-2 space-y-2 min-h-[100px] border border-t-0">
        {executions.length === 0 ? (
          <p className="text-xs text-gray-400 text-center py-4">No tests</p>
        ) : (
          executions.map((exec) => (
            <ExecutionCard key={exec.id} execution={exec} onClick={() => onCardClick(exec)} />
          ))
        )}
      </div>
    </div>
  );
};

// ─── Main Board Component ──────────────────────────────────────────────────────

export const TestPlanBoard: React.FC<TestPlanBoardProps> = ({ projectId, testPlanId }) => {
  const navigate = useNavigate();
  const [groupBy, setGroupBy] = useState<GroupBy>('status');
  const [selectedPlanId, setSelectedPlanId] = useState<string | undefined>(testPlanId);

  // Fetch test plans for project
  const { data: plans = [], isLoading: plansLoading } = useQuery({
    queryKey: ['test-plans', projectId],
    queryFn: () => combinedApi.getTestPlansByProject(projectId),
    enabled: !!projectId,
  });

  // Determine active plan ID
  const activePlanId = selectedPlanId || (plans.length > 0 ? plans[0].id : undefined);

  // Fetch executions for the active plan
  const { data: executions = [], isLoading: executionsLoading } = useQuery({
    queryKey: ['test-plan-executions', activePlanId],
    queryFn: () => combinedApi.getExecutionsByPlan(activePlanId!),
    enabled: !!activePlanId,
  });

  // Group by status
  const statusGroups = useMemo(() => {
    const groups: Record<string, TestExecutionResponse[]> = {};
    STATUS_COLUMNS.forEach(col => { groups[col] = []; });
    executions.forEach(exec => {
      const normalized = normalizeStatus(exec.status);
      if (!groups[normalized]) groups[normalized] = [];
      groups[normalized].push(exec);
    });
    return groups;
  }, [executions]);

  // Group by assignee
  const assigneeGroups = useMemo(() => {
    const groups: Record<string, TestExecutionResponse[]> = {};
    executions.forEach(exec => {
      const key = exec.assigneeId || 'Unassigned';
      if (!groups[key]) groups[key] = [];
      groups[key].push(exec);
    });
    return groups;
  }, [executions]);

  const handleCardClick = (execution: TestExecutionResponse) => {
    navigate(`/tests/${execution.testId}`);
  };

  const isLoading = plansLoading || executionsLoading;

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (plans.length === 0) {
    return (
      <div className="text-center py-12 text-gray-500">
        <BarChart3 className="w-12 h-12 text-gray-300 mx-auto mb-4" />
        <p className="text-lg font-medium">No test plans found</p>
        <p className="text-sm mt-2">Create a test plan to see the execution board</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Plan selector and group-by toggle */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <select
            value={activePlanId || ''}
            onChange={(e) => setSelectedPlanId(e.target.value)}
            className="px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
          >
            {plans.map(plan => (
              <option key={plan.id} value={plan.id}>{plan.name}</option>
            ))}
          </select>
          <span className="text-sm text-gray-500">
            {executions.length} test(s)
          </span>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500">Group by:</span>
          <button
            onClick={() => setGroupBy('status')}
            className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm ${
              groupBy === 'status' ? 'bg-blue-100 text-blue-700 font-medium' : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            <LayoutGrid className="w-3.5 h-3.5" /> By Status
          </button>
          <button
            onClick={() => setGroupBy('assignee')}
            className={`flex items-center gap-1 px-3 py-1.5 rounded text-sm ${
              groupBy === 'assignee' ? 'bg-blue-100 text-blue-700 font-medium' : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            <Users className="w-3.5 h-3.5" /> By Assignee
          </button>
        </div>
      </div>

      {/* Stats bar */}
      <StatsBar executions={executions} />

      {/* Board */}
      {groupBy === 'status' ? (
        <div className="flex gap-4 overflow-x-auto pb-4">
          {STATUS_COLUMNS.map(status => {
            const config = STATUS_CONFIG[status];
            return (
              <BoardColumn
                key={status}
                columnKey={status}
                label={config.label}
                icon={config.icon}
                bgColor={config.bgColor}
                color={config.color}
                executions={statusGroups[status] || []}
                onCardClick={handleCardClick}
              />
            );
          })}
        </div>
      ) : (
        <div className="flex gap-4 overflow-x-auto pb-4">
          {Object.entries(assigneeGroups).map(([assignee, execs]) => (
            <BoardColumn
              key={assignee}
              columnKey={assignee}
              label={assignee}
              icon={<Users className="w-4 h-4" />}
              bgColor="bg-blue-50"
              color="text-blue-700"
              executions={execs}
              onCardClick={handleCardClick}
            />
          ))}
        </div>
      )}

      {/* Empty state for no executions */}
      {executions.length === 0 && (
        <div className="text-center py-8 text-gray-500">
          <AlertCircle className="w-10 h-10 text-gray-300 mx-auto mb-3" />
          <p className="font-medium">No test executions in this plan</p>
          <p className="text-sm mt-1">Start the test plan to generate executions</p>
        </div>
      )}
    </div>
  );
};

export default TestPlanBoard;
