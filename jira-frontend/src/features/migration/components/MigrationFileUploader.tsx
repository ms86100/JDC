import React, { useCallback, useRef, useState } from 'react';
import type { ValidationResult, CsvTemplate } from '../types/migration';
import VirusScanStatusBadge from './VirusScanStatusBadge';

const MIME_ALLOWLIST = [
  'text/csv',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/xml',
  'text/xml',
  'application/octet-stream',
];

interface MigrationFileUploaderProps {
  onFileSelect: (file: File) => void | Promise<void>;
  onUploadCancel?: () => void;
  onTemplateDownload?: (templateId: string) => void;
  templates?: CsvTemplate[];
  isLoading?: boolean;
  uploadProgress?: number | null;
  validationResult?: ValidationResult | null;
  accept?: string;
  maxSize?: number;
  importTypeLabel?: string;
  virusScanStatus?: string | null;
}

export default function MigrationFileUploader({
  onFileSelect,
  onUploadCancel,
  onTemplateDownload,
  templates,
  isLoading = false,
  uploadProgress = null,
  validationResult,
  accept = '.csv,.xlsx,.xml',
  maxSize = 500 * 1024 * 1024,
  importTypeLabel = 'CSV, Excel (.xlsx), or Jira DC XML',
  virusScanStatus = null,
}: MigrationFileUploaderProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateFile = useCallback(
    (file: File): string | null => {
      const extension = file.name.toLowerCase().split('.').pop();
      const acceptedExtensions = accept.split(',').map((ext) => ext.trim().toLowerCase().replace(/^\./, ''));

      if (extension && !acceptedExtensions.includes(extension)) {
        return `Invalid file type. Accepted: ${acceptedExtensions.join(', ')}`;
      }

      if (file.type && !MIME_ALLOWLIST.some((m) => file.type.includes(m.split('/')[1]) || file.type === m)) {
        const extOk = extension && acceptedExtensions.includes(extension);
        if (!extOk && file.type !== 'application/octet-stream') {
          return `Unsupported file type (${file.type}). Use ${importTypeLabel}.`;
        }
      }

      if (file.size > maxSize) {
        return `File too large. Maximum size: ${(maxSize / (1024 * 1024)).toFixed(0)}MB`;
      }

      return null;
    },
    [accept, maxSize, importTypeLabel]
  );

  const handleFile = useCallback(
    async (file: File) => {
      const validationError = validateFile(file);
      if (validationError) {
        setError(validationError);
        return;
      }

      setError(null);
      setSelectedFile(file);
      try {
        await onFileSelect(file);
      } catch (e) {
        const cancelled =
          (e instanceof Error && (e.name === 'CanceledError' || e.name === 'AbortError')) ||
          (typeof e === 'object' && e !== null && 'code' in e && (e as { code?: string }).code === 'ERR_CANCELED');
        if (cancelled) {
          setSelectedFile(null);
          setError('Upload cancelled');
        } else {
          setError(e instanceof Error ? e.message : 'Upload failed');
        }
      }
    },
    [validateFile, onFileSelect]
  );

  const handleCancelUpload = useCallback(() => {
    setSelectedFile(null);
    setError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
    onUploadCancel?.();
  }, [onUploadCancel]);

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDragging(false);
      const files = e.dataTransfer.files;
      if (files.length > 0) handleFile(files[0]);
    },
    [handleFile]
  );

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const showProgress = isLoading && uploadProgress !== null && uploadProgress >= 0;

  return (
    <div className="space-y-6">
      {templates && templates.length > 0 && (
        <div className="bg-white rounded-lg border p-4">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-medium text-gray-700">Available Templates</h3>
            <span className="text-xs text-gray-500">{templates.length} templates</span>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {templates.map((template) => (
              <div
                key={template.id}
                className="bg-gray-50 rounded-lg p-3 border border-gray-200 hover:border-jira-blue transition-colors"
              >
                <h4 className="text-sm font-medium text-gray-900 truncate">{template.templateName}</h4>
                <p className="text-xs text-gray-500">{template.entityType}</p>
                <button
                  type="button"
                  onClick={() => onTemplateDownload?.(template.id)}
                  className="w-full mt-2 px-2 py-1.5 text-xs bg-white border border-gray-300 rounded hover:bg-gray-50"
                >
                  Download
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      <div
        className={`relative border-2 border-dashed rounded-lg p-8 text-center transition-all cursor-pointer
          ${isDragging ? 'border-jira-blue bg-blue-50' : 'border-gray-300 hover:border-gray-400'}
          ${error ? 'border-red-400 bg-red-50' : ''}
          ${selectedFile && !isLoading ? 'border-green-400 bg-green-50' : ''}
          ${isLoading ? 'pointer-events-none opacity-80' : ''}`}
        onDragEnter={(e) => { e.preventDefault(); setIsDragging(true); }}
        onDragLeave={(e) => { e.preventDefault(); setIsDragging(false); }}
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDrop}
        onClick={() => !isLoading && fileInputRef.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => e.key === 'Enter' && fileInputRef.current?.click()}
      >
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept={accept}
          onChange={(e) => {
            const files = e.target.files;
            if (files?.[0]) handleFile(files[0]);
          }}
        />

        {showProgress ? (
          <div className="flex flex-col items-center w-full max-w-md mx-auto">
            <p className="text-gray-700 font-medium mb-3">Uploading… {uploadProgress}%</p>
            <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
              <div
                className="h-full bg-jira-blue transition-all duration-200"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); handleCancelUpload(); }}
              className="mt-4 px-4 py-2 text-sm text-red-600 border border-red-200 rounded-lg hover:bg-red-50"
            >
              Cancel upload
            </button>
          </div>
        ) : isLoading ? (
          <div className="flex flex-col items-center">
            <div className="w-12 h-12 border-4 border-jira-blue border-t-transparent rounded-full animate-spin mb-4" />
            <p className="text-gray-600 font-medium">Processing file…</p>
          </div>
        ) : selectedFile ? (
          <div className="flex flex-col items-center">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mb-4 text-2xl text-green-600">✓</div>
            <p className="text-gray-900 font-medium">{selectedFile.name}</p>
            <p className="text-sm text-gray-500 mt-1">{formatFileSize(selectedFile.size)}</p>
            <VirusScanStatusBadge status={virusScanStatus} />
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); handleCancelUpload(); }}
              className="mt-3 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 rounded"
            >
              Remove file
            </button>
          </div>
        ) : (
          <div className="flex flex-col items-center">
            <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mb-4 text-2xl">📤</div>
            <p className="text-gray-900 font-medium">
              {isDragging ? 'Drop file here' : 'Drag and drop your migration file'}
            </p>
            <p className="text-gray-500 text-sm mt-1">or click to browse</p>
            <p className="text-xs text-gray-400 mt-2">{importTypeLabel} · up to {formatFileSize(maxSize)}</p>
          </div>
        )}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex gap-3" role="alert">
          <span className="text-red-500">⚠</span>
          <div>
            <p className="text-red-800 font-medium">Upload Error</p>
            <p className="text-red-600 text-sm mt-1">{error}</p>
          </div>
        </div>
      )}

      {validationResult && validationResult.previewRows?.length > 0 && (
        <div className="bg-white rounded-lg border overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 border-b flex justify-between">
            <h3 className="text-sm font-medium text-gray-700">File Preview</h3>
            <span className="text-xs text-gray-500">{validationResult.totalRows} rows</span>
          </div>
          <div className="overflow-x-auto max-h-64">
            <table className="min-w-full divide-y divide-gray-200 text-sm">
              <thead className="bg-gray-50 sticky top-0">
                <tr>
                  <th className="px-3 py-2 text-left text-xs text-gray-500">#</th>
                  {validationResult.headers.map((h, i) => (
                    <th key={i} className="px-3 py-2 text-left text-xs text-gray-500">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {validationResult.previewRows.map((row, ri) => (
                  <tr key={ri} className={ri % 2 === 0 ? 'bg-white' : 'bg-gray-50'}>
                    <td className="px-3 py-2 text-xs text-gray-400">{ri + 1}</td>
                    {row.map((cell, ci) => (
                      <td key={ci} className="px-3 py-2 max-w-xs truncate" title={cell}>{cell || '—'}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
