import { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { attachmentApi, AttachmentResponse } from '../../../api/attachmentApi';

interface AttachmentsTabProps {
  issueId: string;
}

function formatFileSize(bytes?: number | null): string {
  const size = bytes ?? 0;
  if (size === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(size) / Math.log(k));
  return parseFloat((size / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function getFileIcon(fileType?: string | null): string {
  const type = (fileType ?? '').toLowerCase();
  if (!type) return '📎';
  if (type.startsWith('image/')) return '🖼️';
  if (type.includes('pdf')) return '📄';
  if (type.includes('word') || type.includes('document')) return '📝';
  if (type.includes('excel') || type.includes('spreadsheet')) return '📊';
  if (type.includes('powerpoint') || type.includes('presentation')) return '📽️';
  if (type.includes('zip') || type.includes('archive')) return '📦';
  if (type.includes('text/')) return '📃';
  return '📎';
}

function getFileTypeLabel(fileType?: string | null): string {
  const type = (fileType ?? '').toLowerCase();
  if (!type) return 'File';
  if (type.startsWith('image/')) return 'Image';
  if (type.includes('pdf')) return 'PDF';
  if (type.includes('word') || type.includes('document')) return 'Word';
  if (type.includes('excel') || type.includes('spreadsheet')) return 'Excel';
  if (type.includes('powerpoint') || type.includes('presentation')) return 'PPT';
  if (type.includes('zip') || type.includes('archive')) return 'Archive';
  if (type.includes('text/')) return 'Text';
  return type.split('/')[1]?.toUpperCase() || 'FILE';
}

export default function AttachmentsTab({ issueId }: AttachmentsTabProps) {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [dragActive, setDragActive] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const { data: attachments, isLoading } = useQuery<AttachmentResponse[]>({
    queryKey: ['attachments', issueId],
    queryFn: async () => {
      const response = await attachmentApi.getByIssue(issueId);
      return response.data;
    },
    enabled: !!issueId,
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => attachmentApi.upload(issueId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attachments', issueId] });
      setUploadError(null);
    },
    onError: (error: any) => {
      setUploadError(error?.response?.data?.message || 'Upload failed');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (attachmentId: string) => attachmentApi.delete(attachmentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attachments', issueId] });
    },
  });

  const handleFileSelect = (files: FileList | null) => {
    if (!files || files.length === 0) return;

    const file = files[0];
    const maxSize = 10 * 1024 * 1024; // 10MB

    if (file.size > maxSize) {
      setUploadError('File size must be less than 10MB');
      return;
    }

    const allowedTypes = [
      'image/', 'application/pdf', 'application/msword',
      'application/vnd.openxmlformats-officedocument.',
      'application/zip', 'text/'
    ];

    const isAllowed = allowedTypes.some(type => file.type.startsWith(type.replace('/', '')));
    if (!isAllowed && !file.type) {
      setUploadError('Invalid file type');
      return;
    }

    uploadMutation.mutate(file);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    handleFileSelect(e.dataTransfer.files);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(true);
  };

  const handleDragLeave = () => {
    setDragActive(false);
  };

  const handleDownload = async (attachment: AttachmentResponse) => {
    try {
      const response = await attachmentApi.download(attachment.id);
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = attachment.fileName || 'download';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      console.error('Download failed:', error);
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  return (
    <div className="ab-attachments-tab">
      <div className="ab-section-header">
        <div className="ab-section-info">
          <h3>Attachments</h3>
          {attachments && attachments.length > 0 && (
            <span className="ab-attachment-count">{attachments.length} file{attachments.length !== 1 ? 's' : ''}</span>
          )}
        </div>
        <button
          className="ab-btn ab-btn-primary ab-btn-sm"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploadMutation.isPending}
        >
          {uploadMutation.isPending ? 'Uploading...' : 'Upload File'}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          style={{ display: 'none' }}
          onChange={(e) => handleFileSelect(e.target.files)}
          accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.txt"
        />
      </div>

      <div
        className={`ab-upload-zone ${dragActive ? 'ab-upload-zone-active' : ''}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={() => fileInputRef.current?.click()}
      >
        <div className="ab-upload-icon">📁</div>
        <p className="ab-upload-text">
          <strong>Drop files here</strong> or click to browse
        </p>
        <p className="ab-upload-hint">
          Images, PDF, Office documents, ZIP (max 10MB)
        </p>
      </div>

      {uploadError && (
        <div className="ab-upload-error">
          <span>⚠️ {uploadError}</span>
          <button onClick={() => setUploadError(null)}>×</button>
        </div>
      )}

      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
        </div>
      ) : attachments && attachments.length > 0 ? (
        <div className="ab-attachments-list">
          {attachments.map((attachment) => (
            <div key={attachment.id} className="ab-attachment-item">
              <div className="ab-attachment-icon">
                {getFileIcon(attachment.fileType)}
              </div>
              <div className="ab-attachment-info">
                <div className="ab-attachment-name">{attachment.fileName}</div>
                <div className="ab-attachment-meta">
                  <span className="ab-attachment-type">{getFileTypeLabel(attachment.fileType)}</span>
                  <span className="ab-attachment-size">{formatFileSize(attachment.fileSize)}</span>
                  <span className="ab-attachment-date">{formatDate(attachment.uploadedAt)}</span>
                </div>
              </div>
              <div className="ab-attachment-actions">
                <button
                  className="ab-btn ab-btn-ghost ab-btn-sm"
                  onClick={() => handleDownload(attachment)}
                  title="Download"
                >
                  ⬇️
                </button>
                <button
                  className="ab-btn-icon"
                  onClick={() => {
                    if (confirm('Delete this attachment?')) {
                      deleteMutation.mutate(attachment.id);
                    }
                  }}
                  title="Delete"
                >
                  ×
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="ab-empty-state">
          <div className="ab-empty-state-icon">📎</div>
          <p className="ab-empty-state-description">No attachments yet</p>
        </div>
      )}

      <style>{`
        .ab-attachments-tab {
          padding: var(--ab-spacing-md) 0;
        }

        .ab-section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-section-info {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
        }

        .ab-section-info h3 {
          font-size: var(--ab-font-size-base);
          font-weight: 600;
          margin: 0;
        }

        .ab-attachment-count {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
        }

        .ab-upload-zone {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          padding: var(--ab-spacing-xl);
          border: 2px dashed var(--ab-gray-300);
          border-radius: var(--ab-radius-lg);
          background: var(--ab-gray-50);
          cursor: pointer;
          transition: all var(--ab-transition-fast);
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-upload-zone:hover {
          border-color: var(--ab-primary-400);
          background: var(--ab-primary-50);
        }

        .ab-upload-zone-active {
          border-color: var(--ab-primary-500);
          background: var(--ab-primary-50);
        }

        .ab-upload-icon {
          font-size: 2rem;
          margin-bottom: var(--ab-spacing-sm);
        }

        .ab-upload-text {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-700);
          margin: 0 0 var(--ab-spacing-xs);
        }

        .ab-upload-hint {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-400);
          margin: 0;
        }

        .ab-upload-error {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: var(--ab-spacing-sm) var(--ab-spacing-md);
          background: var(--ab-danger-50);
          border: 1px solid var(--ab-danger-500);
          border-radius: var(--ab-radius-md);
          color: var(--ab-danger-700);
          font-size: var(--ab-font-size-sm);
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-attachments-list {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
        }

        .ab-attachment-item {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
          padding: var(--ab-spacing-md);
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-radius: var(--ab-radius-md);
          transition: background var(--ab-transition-fast);
        }

        .ab-attachment-item:hover {
          background: var(--ab-gray-50);
        }

        .ab-attachment-icon {
          font-size: 1.5rem;
          flex-shrink: 0;
        }

        .ab-attachment-info {
          flex: 1;
          min-width: 0;
        }

        .ab-attachment-name {
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-800);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .ab-attachment-meta {
          display: flex;
          gap: var(--ab-spacing-sm);
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-400);
          margin-top: 2px;
        }

        .ab-attachment-actions {
          display: flex;
          gap: var(--ab-spacing-xs);
          flex-shrink: 0;
        }
      `}</style>
    </div>
  );
}