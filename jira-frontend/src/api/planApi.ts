import apiClient from './axiosClient';

// ============ Program Types ============
export interface ProgramResponse {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  ownerName?: string;
  accessType: 'OPEN' | 'RESTRICTED';
  isActive: boolean;
  planCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProgramRequest {
  name: string;
  description?: string;
  ownerId?: string;
  accessType?: 'OPEN' | 'RESTRICTED';
  linkedPlanIds?: string[];
}

export interface UpdateProgramRequest {
  name?: string;
  description?: string;
  accessType?: 'OPEN' | 'RESTRICTED';
  isActive?: boolean;
}

// ============ Plan Types ============
export interface PlanResponse {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  ownerName?: string;
  settings: Record<string, unknown>;
  startDate?: string;
  endDate?: string;
  isActive: boolean;
  itemCount: number;
  teamCount: number;
  releaseCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanRequest {
  name: string;
  description?: string;
  ownerId?: string;
  startDate?: string;
  endDate?: string;
  settings?: Record<string, unknown>;
}

export interface UpdatePlanRequest {
  name?: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  isActive?: boolean;
}

// ============ Plan Item Types ============
export interface PlanItemResponse {
  id: string;
  planId: string;
  issueId: string;
  issueKey?: string;
  issueType: 'EPIC' | 'STORY' | 'SUBTASK';
  summary?: string;
  status?: string;
  parentId?: string;
  parentKey?: string;
  sortOrder: string;
  targetDate?: string;
  targetEndDate?: string;
  assigneeId?: string;
  assigneeName?: string;
  storyPoints?: number;
  childCount?: number;
  progress?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlanItemRequest {
  issueId: string;
  issueType: 'EPIC' | 'STORY' | 'SUBTASK';
  parentId?: string;
  sortOrder?: string;
  targetDate?: string;
  targetEndDate?: string;
  status?: string;
}

export interface InitiativeResponse {
  id: string;
  name: string;
  description?: string;
  programId?: string;
  status?: string;
  targetDate?: string;
  epicCount?: number;
}

export interface ProgramAggregationResponse {
  programId: string;
  programName: string;
  planCount: number;
  plans: Array<{
    planId: string;
    planName: string;
    issuesByType?: Record<string, Array<{
      id: string;
      issueKey?: string;
      issueTitle?: string;
      issueType: string;
      targetDate?: string;
      targetEndDate?: string;
      status?: string;
    }>>;
    metrics?: { totalIssues: number; epicCount: number; storyCount: number };
  }>;
  releases?: Array<{ name: string; releaseDate?: string; issueCount: number; progress: number }>;
}

export interface PlanIssueSourceResponse {
  id: string;
  sourceType: string;
  sourceId: string;
  sourceName: string;
  issueCount?: number;
}

export interface ExclusionRuleResponse {
  id: string;
  fieldName: string;
  operator: string;
  fieldValue: string;
}

export interface ScheduleResultResponse {
  success: boolean;
  message?: string;
  scheduleDates: Record<string, { startDate: string; endDate: string; durationDays: number }>;
}

export interface ReorderRequest {
  itemId?: string;
  newSortOrder?: string;
  newParentId?: string;
  afterItemId?: string;
  beforeItemId?: string;
}

// ============ Backlog Types ============
export interface BacklogResponse {
  planId: string;
  planName: string;
  totalItems: number;
  epicCount: number;
  storyCount: number;
  subtaskCount: number;
  items: PlanItemResponse[];
}

// ============ Team Types ============
export interface TeamMemberResponse {
  id: string;
  teamId: string;
  userId: string;
  userName?: string;
  userEmail?: string;
  userAvatarUrl?: string;
  capacityHours: number;
  allocatedHours?: number;
  role?: string;
  joinedAt: string;
}

export interface TeamResponse {
  id: string;
  planId: string;
  name: string;
  description?: string;
  isActive: boolean;
  memberCount: number;
  totalCapacity: number;
  usedCapacity?: number;
  members: TeamMemberResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface AddTeamMemberRequest {
  userId: string;
  userName?: string;
  capacityHours?: number;
  role?: string;
}

// ============ Release Types ============
export interface ReleaseResponse {
  id: string;
  planId: string;
  name: string;
  version?: string;
  description?: string;
  releaseDate?: string;
  status: 'DRAFT' | 'APPROVED' | 'RELEASED';
  approvedBy?: string;
  approvedByName?: string;
  approvedAt?: string;
  itemCount?: number;
  completedCount?: number;
  progress?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReleaseRequest {
  name: string;
  version?: string;
  description?: string;
  releaseDate?: string;
}

// ============ Dependency Types ============
export interface DependencyResponse {
  id: string;
  planId: string;
  blockingIssueId: string;
  blockingIssueKey?: string;
  blockingIssueSummary?: string;
  blockingIssueStatus?: string;
  blockedIssueId: string;
  blockedIssueKey?: string;
  blockedIssueSummary?: string;
  blockedIssueStatus?: string;
  dependencyType: 'BLOCKS' | 'DEPENDENCY';
  isCircular?: boolean;
  blockingPath?: string;
  createdAt: string;
}

export interface CreateDependencyRequest {
  blockingIssueId: string;
  blockingIssueKey?: string;
  blockedIssueId: string;
  blockedIssueKey?: string;
  dependencyType?: 'BLOCKS' | 'DEPENDENCY';
}

// ============ Warning Types ============
export interface WarningResponse {
  id: string;
  planId: string;
  issueId: string;
  issueKey?: string;
  issueSummary?: string;
  warningType: string;
  message?: string;
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  isActive: boolean;
  dismissedAt?: string;
  createdAt: string;
}

// ============ Board Types ============
export interface BoardConfigResponse {
  id: string;
  planId: string;
  name: string;
  boardType: 'SCRUM' | 'KANBAN';
  columnConfigMode: string;
  constraintSource: string | null;
  isEnabled: boolean;
  cardLayoutMode: string;
  defaultSwimlane: string;
  columns: BoardColumnResponse[];
  quickFilters: BoardQuickFilterResponse[];
  swimlanes: BoardSwimlaneResponse[];
  cardColors: BoardCardColorResponse[];
  detailFields: BoardDetailFieldResponse[];
}

export interface BoardColumnResponse {
  id: string;
  name: string;
  sequence: number;
  statusMapping: string[];
  labelValues: string[];
  minWidth: number;
  maxWidth: number;
  color: string | null;
  maxIssues: number | null;
  constraintStatus: string | null;
}

export interface BoardQuickFilterResponse {
  id: string;
  name: string;
  filterQuery: string;
  sequence: number;
  isEnabled: boolean;
  icon: string | null;
}

export interface BoardSwimlaneResponse {
  id: string;
  name: string;
  groupingField: string;
  enabled: boolean;
  collapsedByDefault: boolean;
  sequence: number;
}

export interface BoardCardColorResponse {
  id: string;
  name: string;
  color: string;
  conditions: CardColorCondition[];
  sequence: number;
  enabled: boolean;
}

export interface CardColorCondition {
  field: string;
  operator: string;
  value: any;
}

export interface BoardDetailFieldResponse {
  id: string;
  fieldKey: string;
  fieldLabel: string | null;
  sequence: number;
  isVisible: boolean;
  fieldType: string;
}

export interface CreateBoardConfigRequest {
  name: string;
  boardType?: 'SCRUM' | 'KANBAN';
  columnConfigMode?: string;
  constraintSource?: string;
  cardLayoutMode?: string;
  defaultSwimlane?: string;
  isEnabled?: boolean;
}

export interface CreateBoardColumnRequest {
  name: string;
  sequence?: number;
  statusMapping?: string[];
  labelValues?: string[];
  minWidth?: number;
  maxWidth?: number;
  color?: string;
  maxIssues?: number;
  constraintStatus?: string;
}

// ============ Sprint Types ============
export interface SprintResponse {
  id: string;
  boardId: string;
  name: string;
  goal: string | null;
  startDate: string | null;
  endDate: string | null;
  completeDate: string | null;
  state: 'FUTURE' | 'ACTIVE' | 'CLOSED' | 'ABANDONED';
  sequence: number;
  velocity: number;
  committedPoints: number;
  completedPoints: number;
  totalIssues: number;
  completedIssues: number;
}

export interface SprintIssueResponse {
  id: string;
  sprintId: string;
  planItemId: string;
  issueId: string;
  rankValue: string | null;
  addedAt: string;
  addedBy: string | null;
  removedAt: string | null;
  completionStatus: 'UNCOMPLETED' | 'COMPLETED' | 'DROPPED';
  completedAt: string | null;
  assigneeId?: string;
  updatedAt?: string;
}

export interface SprintBurndownResponse {
  sprintId: string;
  sprintName: string;
  startDate: string | null;
  endDate: string | null;
  totalIssues: number;
  completedIssues: number;
  totalPoints: number;
  completedPoints: number;
  burndownPoints: BurndownPoint[];
}

export interface BurndownPoint {
  date: string;
  remainingIssues: number;
  completedIssues: number;
  remainingPoints: number;
  idealRemaining: number;
}

export interface CreateSprintRequest {
  name: string;
  goal?: string;
  startDate?: string;
  endDate?: string;
}

// ============ Working Days Types ============
export interface WorkingDaysResponse {
  id: string;
  name: string;
  monday: boolean;
  tuesday: boolean;
  wednesday: boolean;
  thursday: boolean;
  friday: boolean;
  saturday: boolean;
  sunday: boolean;
  hoursPerDay: number;
  isDefault: boolean;
  workingDaysPerWeek: number;
  holidays: NonWorkingDayResponse[];
}

export interface NonWorkingDayResponse {
  id: string;
  date: string;
  name: string;
}

export interface TeamAvailabilityResponse {
  id: string;
  teamId: string;
  userId: string | null;
  date: string;
  hours: number | null;
  reason: string | null;
}

export interface CapacityResponse {
  teamId: string;
  startDate: string;
  endDate: string;
  workingDays: number;
  totalCapacityHours: number;
  totalTimeOffHours: number;
  netCapacityHours: number;
  memberCount: number;
}

// ============ Permission Types ============
export interface BoardPermissionResponse {
  id: string;
  boardId: string;
  permissionType: 'VIEW' | 'EDIT' | 'ADMIN' | 'MANAGE_SPRINTS' | 'EDIT_SPRINTS';
  principalType: 'USER' | 'GROUP';
  principalId: string;
  grantedAt: string;
  grantedBy: string | null;
}

// ============ API Functions ============
export const planApi = {
  // Programs
  getPrograms: () => apiClient.get<ProgramResponse[]>('/plans/programs'),
  getProgramById: (id: string) => apiClient.get<ProgramResponse>(`/api/plans/programs/${id}`),
  createProgram: (data: CreateProgramRequest) =>
    apiClient.post<ProgramResponse>('/plans/programs', data),
  updateProgram: (id: string, data: UpdateProgramRequest) =>
    apiClient.put<ProgramResponse>(`/api/plans/programs/${id}`, data),
  deleteProgram: (id: string) => apiClient.delete(`/api/plans/programs/${id}`),
  linkPlanToProgram: (programId: string, planId: string) =>
    apiClient.post(`/api/plans/programs/${programId}/plans/${planId}`),
  unlinkPlanFromProgram: (programId: string, planId: string) =>
    apiClient.delete(`/api/plans/programs/${programId}/plans/${planId}`),
  getProgramAggregation: (programId: string) =>
    apiClient.get<ProgramAggregationResponse>(`/api/plans/programs/${programId}/aggregation`),

  // Plans
  getPlans: () => apiClient.get<PlanResponse[]>('/plans'),
  getPlanById: (id: string) => apiClient.get<PlanResponse>(`/api/plans/${id}`),
  getPlansByProgram: (programId: string) =>
    apiClient.get<PlanResponse[]>(`/api/plans/program/${programId}`),
  createPlan: (data: CreatePlanRequest) =>
    apiClient.post<PlanResponse>('/plans', data),
  updatePlan: (id: string, data: UpdatePlanRequest) =>
    apiClient.put<PlanResponse>(`/api/plans/${id}`, data),
  deletePlan: (id: string) => apiClient.delete(`/api/plans/${id}`),
  updatePlanSettings: (id: string, settings: Record<string, unknown>) =>
    apiClient.put<PlanResponse>(`/api/plans/${id}/settings`, settings),

  getIssueSources: (planId: string) =>
    apiClient.get<PlanIssueSourceResponse[]>(`/api/plans/${planId}/issue-sources`),
  addIssueSource: (planId: string, data: { sourceType: string; sourceId: string; sourceName: string }) =>
    apiClient.post<PlanIssueSourceResponse>(`/api/plans/${planId}/issue-sources`, data),
  removeIssueSource: (planId: string, sourceId: string, sourceType: string) =>
    apiClient.delete(`/api/plans/${planId}/issue-sources/${sourceId}`, { params: { sourceType } }),

  getExclusionRules: (planId: string) =>
    apiClient.get<ExclusionRuleResponse[]>(`/api/plans/${planId}/exclusion-rules`),
  createExclusionRule: (planId: string, data: { fieldName: string; operator: string; fieldValue: string }) =>
    apiClient.post<ExclusionRuleResponse>(`/api/plans/${planId}/exclusion-rules`, data),
  deleteExclusionRule: (planId: string, ruleId: string) =>
    apiClient.delete(`/api/plans/${planId}/exclusion-rules/${ruleId}`),

  runAutoSchedule: (planId: string, startDate?: string) =>
    apiClient.post<ScheduleResultResponse>(`/api/schedule/forward?planId=${planId}&startDate=${startDate || new Date().toISOString().slice(0, 10)}`),

  getInitiativesByProgram: (programId: string) =>
    apiClient.get<InitiativeResponse[]>(`/api/initiatives/program/${programId}`),

  // Backlog
  getBacklog: (planId: string) => apiClient.get<BacklogResponse>(`/api/plans/${planId}/backlog`),
  addItemToBacklog: (planId: string, data: CreatePlanItemRequest) =>
    apiClient.post<PlanItemResponse>(`/api/plans/${planId}/backlog`, data),
  updateBacklogItem: (planId: string, itemId: string, data: CreatePlanItemRequest) =>
    apiClient.put<PlanItemResponse>(`/api/plans/${planId}/backlog/${itemId}`, data),
  removeItemFromBacklog: (planId: string, itemId: string) =>
    apiClient.delete(`/api/plans/${planId}/backlog/${itemId}`),
  reorderBacklog: (planId: string, data: ReorderRequest) =>
    apiClient.put(`/api/plans/${planId}/backlog/reorder`, data),

  // Teams
  getTeams: (planId: string) => apiClient.get<TeamResponse[]>(`/api/plans/${planId}/teams`),
  getTeamById: (planId: string, teamId: string) =>
    apiClient.get<TeamResponse>(`/api/plans/${planId}/teams/${teamId}`),
  createTeam: (planId: string, data: CreateTeamRequest) =>
    apiClient.post<TeamResponse>(`/api/plans/${planId}/teams`, data),
  updateTeam: (planId: string, teamId: string, data: CreateTeamRequest) =>
    apiClient.put<TeamResponse>(`/api/plans/${planId}/teams/${teamId}`, data),
  deleteTeam: (planId: string, teamId: string) =>
    apiClient.delete(`/api/plans/${planId}/teams/${teamId}`),
  addTeamMember: (planId: string, teamId: string, data: AddTeamMemberRequest) =>
    apiClient.post<TeamMemberResponse>(`/api/plans/${planId}/teams/${teamId}/members`, data),
  removeTeamMember: (planId: string, teamId: string, memberId: string) =>
    apiClient.delete(`/api/plans/${planId}/teams/${teamId}/members/${memberId}`),

  // Releases
  getReleases: (planId: string) => apiClient.get<ReleaseResponse[]>(`/api/plans/${planId}/releases`),
  getReleaseById: (planId: string, releaseId: string) =>
    apiClient.get<ReleaseResponse>(`/api/plans/${planId}/releases/${releaseId}`),
  createRelease: (planId: string, data: CreateReleaseRequest) =>
    apiClient.post<ReleaseResponse>(`/api/plans/${planId}/releases`, data),
  updateRelease: (planId: string, releaseId: string, data: CreateReleaseRequest) =>
    apiClient.put<ReleaseResponse>(`/api/plans/${planId}/releases/${releaseId}`, data),
  approveRelease: (planId: string, releaseId: string, approvedBy: string) =>
    apiClient.post<ReleaseResponse>(`/api/plans/${planId}/releases/${releaseId}/approve?approvedBy=${approvedBy}`),
  releaseVersion: (planId: string, releaseId: string) =>
    apiClient.post<ReleaseResponse>(`/api/plans/${planId}/releases/${releaseId}/release`),
  deleteRelease: (planId: string, releaseId: string) =>
    apiClient.delete(`/api/plans/${planId}/releases/${releaseId}`),

  // Dependencies
  getDependencies: (planId: string) =>
    apiClient.get<DependencyResponse[]>(`/api/plans/${planId}/dependencies`),
  createDependency: (planId: string, data: CreateDependencyRequest) =>
    apiClient.post<DependencyResponse>(`/api/plans/${planId}/dependencies`, data),
  deleteDependency: (planId: string, dependencyId: string) =>
    apiClient.delete(`/api/plans/${planId}/dependencies/${dependencyId}`),

  // Warnings
  getWarnings: (planId: string) =>
    apiClient.get<WarningResponse[]>(`/api/plans/${planId}/warnings`),
  dismissWarning: (planId: string, warningId: string) =>
    apiClient.put<WarningResponse>(`/api/plans/${planId}/warnings/${warningId}/dismiss`),

  // Boards
  getBoards: (planId: string) =>
    apiClient.get<BoardConfigResponse[]>(`/api/plans/${planId}/boards`),
  getBoardById: (boardId: string) =>
    apiClient.get<BoardConfigResponse>(`/api/plans/boards/${boardId}`),
  createBoard: (planId: string, data: CreateBoardConfigRequest) =>
    apiClient.post<BoardConfigResponse>(`/api/plans/${planId}/boards`, data),
  updateBoard: (boardId: string, data: CreateBoardConfigRequest) =>
    apiClient.put<BoardConfigResponse>(`/api/plans/boards/${boardId}`, data),
  deleteBoard: (boardId: string) =>
    apiClient.delete(`/api/plans/boards/${boardId}`),

  // Sprints
  getSprints: (boardId: string) =>
    apiClient.get<SprintResponse[]>(`/api/plans/boards/${boardId}/sprints`),
  getSprintById: (sprintId: string) =>
    apiClient.get<SprintResponse>(`/api/plans/sprints/${sprintId}`),
  createSprint: (boardId: string, data: CreateSprintRequest) =>
    apiClient.post<SprintResponse>(`/api/plans/boards/${boardId}/sprints`, data),
  startSprint: (sprintId: string, userId?: string) =>
    apiClient.post<SprintResponse>(`/api/plans/sprints/${sprintId}/start${userId ? `?userId=${userId}` : ''}`),
  closeSprint: (sprintId: string, userId?: string) =>
    apiClient.post<SprintResponse>(`/api/plans/sprints/${sprintId}/close${userId ? `?userId=${userId}` : ''}`),
  getSprintIssues: (sprintId: string) =>
    apiClient.get<SprintIssueResponse[]>(`/api/plans/sprints/${sprintId}/issues`),
  getSprintBurndown: (sprintId: string) =>
    apiClient.get<SprintBurndownResponse>(`/api/plans/sprints/${sprintId}/burndown`),

  // Working Days
  getWorkingDaysConfigs: () =>
    apiClient.get<WorkingDaysResponse[]>('/plans/working-days'),
  getWorkingDaysConfig: (id: string) =>
    apiClient.get<WorkingDaysResponse>(`/api/plans/working-days/${id}`),
  getDefaultWorkingDays: () =>
    apiClient.get<WorkingDaysResponse>('/plans/working-days/default'),
  getHolidays: (configId: string) =>
    apiClient.get<NonWorkingDayResponse[]>(`/api/plans/working-days/${configId}/holidays`),
  getTeamAvailability: (teamId: string, start: string, end: string) =>
    apiClient.get<TeamAvailabilityResponse[]>(`/api/plans/teams/${teamId}/availability?start=${start}&end=${end}`),
  getTeamCapacity: (teamId: string, start: string, end: string) =>
    apiClient.get<CapacityResponse>(`/api/plans/teams/${teamId}/capacity?start=${start}&end=${end}`),

  // Permissions
  getBoardPermissions: (boardId: string) =>
    apiClient.get<BoardPermissionResponse[]>(`/api/plans/boards/${boardId}/permissions`),
  checkBoardPermission: (boardId: string, permission: string, userId: string) =>
    apiClient.get<boolean>(`/api/plans/boards/${boardId}/permissions/check?permission=${permission}&userId=${userId}`),
};
