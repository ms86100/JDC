import apiClient from './axiosClient';

export interface AttachmentResponse {
  id: string;
  issueId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  storagePath: string;
  downloadUrl?: string;
  uploadedBy?: string;
  uploadedAt: string;
}

export interface UploadAttachmentRequest {
  issueId: string;
  file: File;
}

export const attachmentApi = {
  upload: async (issueId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('issueId', issueId);

    return apiClient.post<AttachmentResponse>('/api/attachments', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  getByIssue: (issueId: string) =>
    apiClient.get<AttachmentResponse[]>(`/api/attachments/issue/${issueId}`),

  download: (attachmentId: string) =>
    apiClient.get(`/api/attachments/${attachmentId}/download`, { responseType: 'blob' }),

  delete: (attachmentId: string) =>
    apiClient.delete(`/api/attachments/${attachmentId}`),
};