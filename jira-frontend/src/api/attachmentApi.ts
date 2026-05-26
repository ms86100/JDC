import apiClient from './axiosClient';

export interface AttachmentResponse {
  id: string;
  issueId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  storagePath?: string;
  downloadUrl?: string;
  uploadedBy?: string;
  uploadedAt: string;
}

/** Map attachment-service DTO (mimeType, filename, createdAt) to UI shape. */
export function normalizeAttachment(raw: Record<string, unknown>): AttachmentResponse {
  const fileName =
    (raw.fileName as string) ||
    (raw.filename as string) ||
    (raw.originalFilename as string) ||
    'attachment';
  let fileType =
    (raw.fileType as string) ||
    (raw.mimeType as string) ||
    (raw.mimeTypeDetected as string) ||
    '';
  if (!fileType && fileName.includes('.')) {
    const ext = fileName.split('.').pop()?.toLowerCase() ?? '';
    const byExt: Record<string, string> = {
      png: 'image/png',
      jpg: 'image/jpeg',
      jpeg: 'image/jpeg',
      gif: 'image/gif',
      webp: 'image/webp',
      pdf: 'application/pdf',
      doc: 'application/msword',
      docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      xls: 'application/vnd.ms-excel',
      xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      zip: 'application/zip',
      txt: 'text/plain',
    };
    fileType = byExt[ext] ?? 'application/octet-stream';
  }
  const uploadedAt =
    (raw.uploadedAt as string) ||
    (raw.createdAt as string) ||
    new Date().toISOString();

  return {
    id: String(raw.id ?? ''),
    issueId: String(raw.issueId ?? ''),
    fileName,
    fileType,
    fileSize: Number(raw.fileSize ?? 0),
    storagePath: raw.storagePath as string | undefined,
    downloadUrl: raw.downloadUrl as string | undefined,
    uploadedBy: (raw.uploadedBy as string) || (raw.uploaderName as string),
    uploadedAt,
  };
}

function normalizeList(data: unknown): AttachmentResponse[] {
  if (!Array.isArray(data)) return [];
  return data.map((row) => normalizeAttachment(row as Record<string, unknown>));
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

    const response = await apiClient.post<AttachmentResponse>('/attachments', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    response.data = normalizeAttachment(response.data as unknown as Record<string, unknown>);
    return response;
  },

  getByIssue: async (issueId: string) => {
    const response = await apiClient.get<AttachmentResponse[]>(`/api/attachments/issue/${issueId}`);
    response.data = normalizeList(response.data) as unknown as AttachmentResponse[];
    return response;
  },

  download: (attachmentId: string) =>
    apiClient.get(`/api/attachments/${attachmentId}/download`, { responseType: 'blob' }),

  delete: (attachmentId: string) =>
    apiClient.delete(`/api/attachments/${attachmentId}`),
};