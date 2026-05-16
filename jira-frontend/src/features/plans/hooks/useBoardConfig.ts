import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';

// Types
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

// API functions
const boardConfigApi = {
  getByPlanId: (planId: string) =>
    apiClient.get<BoardConfigResponse[]>(`/api/plans/${planId}/boards`),
  getById: (boardId: string) =>
    apiClient.get<BoardConfigResponse>(`/api/plans/boards/${boardId}`),
  create: (planId: string, data: CreateBoardConfigRequest) =>
    apiClient.post<BoardConfigResponse>(`/api/plans/${planId}/boards`, data),
  update: (boardId: string, data: CreateBoardConfigRequest) =>
    apiClient.put<BoardConfigResponse>(`/api/plans/boards/${boardId}`, data),
  delete: (boardId: string) =>
    apiClient.delete(`/api/plans/boards/${boardId}`),

  addColumn: (boardId: string, data: CreateBoardColumnRequest) =>
    apiClient.post<BoardColumnResponse>(`/api/plans/boards/${boardId}/columns`, data),
  updateColumn: (columnId: string, data: CreateBoardColumnRequest) =>
    apiClient.put<BoardColumnResponse>(`/api/plans/boards/columns/${columnId}`, data),
  deleteColumn: (columnId: string) =>
    apiClient.delete(`/api/plans/boards/columns/${columnId}`),
  updateColumnsOrder: (boardId: string, columnIds: string[]) =>
    apiClient.put(`/api/plans/boards/${boardId}/columns`, columnIds),

  addQuickFilter: (boardId: string, data: any) =>
    apiClient.post(`/api/plans/boards/${boardId}/quick-filters`, data),
  deleteQuickFilter: (filterId: string) =>
    apiClient.delete(`/api/plans/boards/quick-filters/${filterId}`),

  addSwimlane: (boardId: string, data: any) =>
    apiClient.post(`/api/plans/boards/${boardId}/swimlanes`, data),
  deleteSwimlane: (swimlaneId: string) =>
    apiClient.delete(`/api/plans/boards/swimlanes/${swimlaneId}`),

  addCardColor: (boardId: string, data: any) =>
    apiClient.post(`/api/plans/boards/${boardId}/card-colors`, data),
  deleteCardColor: (colorId: string) =>
    apiClient.delete(`/api/plans/boards/card-colors/${colorId}`),

  addDetailField: (boardId: string, data: any) =>
    apiClient.post(`/api/plans/boards/${boardId}/detail-fields`, data),
  deleteDetailField: (fieldId: string) =>
    apiClient.delete(`/api/plans/boards/detail-fields/${fieldId}`),
};

// Hooks
export const useBoards = (planId: string) => {
  return useQuery({
    queryKey: ['boards', planId],
    queryFn: () => boardConfigApi.getByPlanId(planId),
    select: (res) => res.data,
    enabled: !!planId,
  });
};

export const useBoard = (boardId: string) => {
  return useQuery({
    queryKey: ['board', boardId],
    queryFn: () => boardConfigApi.getById(boardId),
    select: (res) => res.data,
    enabled: !!boardId,
  });
};

export const useCreateBoard = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ planId, data }: { planId: string; data: CreateBoardConfigRequest }) =>
      boardConfigApi.create(planId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['boards', variables.planId] });
    },
    onError: (error: Error) => {
      console.error('Failed to create board:', error);
      alert(error.message || 'Failed to create board');
    },
  });
};

export const useUpdateBoard = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ boardId, data }: { boardId: string; data: CreateBoardConfigRequest }) =>
      boardConfigApi.update(boardId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['board', variables.boardId] });
    },
    onError: (error: Error) => {
      console.error('Failed to update board:', error);
      alert(error.message || 'Failed to update board');
    },
  });
};

export const useDeleteBoard = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: boardConfigApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['boards'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete board:', error);
      alert(error.message || 'Failed to delete board');
    },
  });
};

export const useAddColumn = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ boardId, data }: { boardId: string; data: CreateBoardColumnRequest }) =>
      boardConfigApi.addColumn(boardId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['board', variables.boardId] });
    },
    onError: (error: Error) => {
      console.error('Failed to add column:', error);
      alert(error.message || 'Failed to add column');
    },
  });
};

export const useUpdateColumn = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ columnId, data }: { columnId: string; data: CreateBoardColumnRequest }) =>
      boardConfigApi.updateColumn(columnId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['board'] });
    },
    onError: (error: Error) => {
      console.error('Failed to update column:', error);
      alert(error.message || 'Failed to update column');
    },
  });
};

export const useDeleteColumn = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: boardConfigApi.deleteColumn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board'] });
    },
    onError: (error: Error) => {
      console.error('Failed to delete column:', error);
      alert(error.message || 'Failed to delete column');
    },
  });
};

export const useUpdateColumnsOrder = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ boardId, columnIds }: { boardId: string; columnIds: string[] }) =>
      boardConfigApi.updateColumnsOrder(boardId, columnIds),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['board', variables.boardId] });
    },
    onError: (error: Error) => {
      console.error('Failed to update columns order:', error);
      alert(error.message || 'Failed to update columns order');
    },
  });
};

export const useAddSwimlane = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ boardId, data }: { boardId: string; data: any }) =>
      boardConfigApi.addSwimlane(boardId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['board', variables.boardId] });
    },
    onError: (error: Error) => {
      console.error('Failed to add swimlane:', error);
      alert(error.message || 'Failed to add swimlane');
    },
  });
};

export const useAddCardColor = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ boardId, data }: { boardId: string; data: any }) =>
      boardConfigApi.addCardColor(boardId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['board', variables.boardId] });
    },
    onError: (error: Error) => {
      console.error('Failed to add card color:', error);
      alert(error.message || 'Failed to add card color');
    },
  });
};