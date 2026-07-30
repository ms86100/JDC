import axiosClient from './axiosClient';

const BASE_URL = '/boards';

export interface BoardColumn {
  id: string;
  name: string;
  sequence: number;
  statusCategory: string;
  isDone: boolean;
  maxIssues?: number;
  currentIssues: number;
  color: string;
  isCollapsible: boolean;
  isHidden: boolean;
}

export interface BoardIssue {
  id: string;
  issueKey: string;
  title: string;
  status: string;
  priority: string;
  projectId?: string;
  assigneeId?: string;
  assigneeName?: string;
  reporterId?: string;
  epicId?: string;
  epicName?: string;
  epicColor?: string;
  storyPoints?: number;
  labels: string[];
  issueType: string;
  created: string;
  updated: string;
  sprintId?: string;
  sprintName?: string;
  dueDate?: string;
  rank?: string;
}

export interface AgileBoard {
  id: string;
  name: string;
  description?: string;
  projectId: string;
  boardType: 'SCRUM' | 'KANBAN' | 'BADGE';
  filterId?: string;
  jqlQuery?: string;
  columnConfig?: string;
  cardLayout: 'FULL' | 'COMPACT' | 'MINI';
  estimationStatistic?: string;
  daysOnBoard: number;
  isDefault: boolean;
}

export interface QuickFilter {
  id: string;
  name: string;
  jql: string;
  icon?: string;
}

export interface SwimlaneConfig {
  enabled: boolean;
  field: 'none' | 'epic' | 'assignee' | 'priority' | 'labels' | 'sprint';
  collapsedSwimlanes: string[];
}

export interface BoardConfig {
  boardId: string;
  quickFilters: QuickFilter[];
  swimlane: SwimlaneConfig;
  workVsCapacity: boolean;
  cardColors: {
    enabled: boolean;
    field: 'none' | 'priority' | 'type' | 'labels' | 'epic';
  };
}

export interface CreateBoardRequest {
  name: string;
  projectId: string;
  boardType: 'SCRUM' | 'KANBAN' | 'BADGE';
  description?: string;
  jqlQuery?: string;
}

export interface BoardDataResponse {
  board: AgileBoard;
  columns: BoardColumn[];
  issues: BoardIssue[];
  activeSprint?: {
    id: string;
    name: string;
    startDate: string;
    endDate: string;
    capacity: number;
    committed: number;
  };
  velocity?: {
    average: number;
    points: { sprint: string; completed: number; planned: number }[];
  };
}

const boardApi = {
  // Get all boards for a project
  getBoardsByProject: async (projectId: string): Promise<AgileBoard[]> => {
    try {
      const response = await axiosClient.get(`${BASE_URL}/project/${projectId}`);
      const data = response.data;
      return Array.isArray(data) ? data : [];
    } catch {
      return [];
    }
  },

  // Get board by ID with full data
  getBoardData: async (boardId: string): Promise<BoardDataResponse> => {
    const response = await axiosClient.get(`${BASE_URL}/${boardId}/data`);
    return response.data;
  },

  // Get board configuration
  getBoardConfig: async (boardId: string): Promise<BoardConfig> => {
    const response = await axiosClient.get(`${BASE_URL}/${boardId}/config`);
    return response.data;
  },

  // Update board configuration
  updateBoardConfig: async (boardId: string, config: Partial<BoardConfig>): Promise<BoardConfig> => {
    const response = await axiosClient.put(`${BASE_URL}/${boardId}/config`, config);
    return response.data;
  },

  updateColumn: async (boardId: string, columnId: string, data: Partial<BoardColumn>): Promise<BoardColumn> => {
    const response = await axiosClient.put(`${BASE_URL}/${boardId}/columns/${columnId}`, data);
    return response.data;
  },

  // Create new board
  createBoard: async (data: CreateBoardRequest): Promise<AgileBoard> => {
    const response = await axiosClient.post(BASE_URL, data);
    return response.data;
  },

  // Update board
  updateBoard: async (boardId: string, data: Partial<AgileBoard>): Promise<AgileBoard> => {
    const response = await axiosClient.put(`${BASE_URL}/${boardId}`, data);
    return response.data;
  },

  // Delete board
  deleteBoard: async (boardId: string): Promise<void> => {
    await axiosClient.delete(`${BASE_URL}/${boardId}`);
  },

  // Quick filter results
  applyQuickFilter: async (boardId: string, quickFilterId: string): Promise<BoardIssue[]> => {
    const response = await axiosClient.post(`${BASE_URL}/${boardId}/quick-filter/${quickFilterId}`);
    return response.data;
  },

  // Get board issues with optional JQL
  getBoardIssues: async (boardId: string, jql?: string): Promise<BoardIssue[]> => {
    const params = jql ? { jql } : {};
    const response = await axiosClient.get(`${BASE_URL}/${boardId}/issues`, { params });
    return response.data;
  },

  // Move issue to column (status transition)
  moveIssue: async (boardId: string, issueId: string, toColumnStatus: string, rank?: string): Promise<BoardIssue> => {
    const response = await axiosClient.put(`${BASE_URL}/${boardId}/issues/${issueId}/move`, {
      status: toColumnStatus,
      rank,
    });
    return response.data;
  },

  // Get swimlane data
  getSwimlaneData: async (boardId: string, field: string): Promise<{ swimlanes: { key: string; label: string; issues: BoardIssue[] }[] }> => {
    const response = await axiosClient.get(`${BASE_URL}/${boardId}/swimlanes`, { params: { field } });
    return response.data;
  },

  // Get velocity data
  getVelocity: async (boardId: string): Promise<{ average: number; points: { sprint: string; completed: number; planned: number }[] }> => {
    const response = await axiosClient.get(`${BASE_URL}/${boardId}/velocity`);
    return response.data;
  },

  // Get sprint capacity
  getSprintCapacity: async (boardId: string, sprintId: string): Promise<{ capacity: number; committed: number; completed: number; remaining: number }> => {
    const response = await axiosClient.get(`${BASE_URL}/${boardId}/sprints/${sprintId}/capacity`);
    return response.data;
  },

  // Reorder issues within column
  reorderIssue: async (boardId: string, issueId: string, targetIndex: number, columnStatus: string): Promise<void> => {
    await axiosClient.post(`${BASE_URL}/${boardId}/issues/${issueId}/reorder`, {
      index: targetIndex,
      status: columnStatus,
    });
  },
};

export default boardApi;