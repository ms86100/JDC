import apiClient from './axiosClient';

export interface CommentResponse {
  id: string; issueId: string; userId: string; content: string;
  parentCommentId?: string; createdAt: string; updatedAt: string; replies?: CommentResponse[];
}
export interface CreateCommentRequest { issueId: string; content: string; parentCommentId?: string }

export const commentApi = {
  create: (data: CreateCommentRequest) => apiClient.post<CommentResponse>('/comments', data),
  getByIssue: (issueId: string) => apiClient.get<CommentResponse[]>(`/api/comments/issue/${issueId}`),
  update: (id: string, content: string) => apiClient.put<CommentResponse>(`/api/comments/${id}`, { content }),
  delete: (id: string) => apiClient.delete(`/api/comments/${id}`),
};
