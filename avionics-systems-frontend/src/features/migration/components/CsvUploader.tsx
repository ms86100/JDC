import React, { useCallback, useState, useRef } from 'react';
import type { ValidationResult, CsvTemplate } from '../types/migration';

interface CsvUploaderProps {
  onFileSelect: (file: File) => void;
  onTemplateDownload?: (templateId: string) => void;
  templates?: CsvTemplate[];
  isLoading?: boolean;
  validationResult?: ValidationResult | null;
  accept?: string;
  maxSize?: number;
}

export default function CsvUploader({
  onFileSelect,
  onTemplateDownload,
  templates,
  isLoading = false,
  validationResult,
  accept = '.csv',
  maxSize = 50 * 1024 * 1024, // 50MB
}: CsvUploaderProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragEnter = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);

  const validateFile = useCallback(
    (file: File): string | null => {
      // Check file extension
      const extension = file.name.toLowerCase().split('.').pop();
      const acceptedExtensions = accept.split(',').map((ext) => ext.trim().toLowerCase().replace(/^\./, ''));

      if (extension && !acceptedExtensions.includes(extension)) {
        return `Invalid file type. Accepted: ${acceptedExtensions.join(', ')}`;
      }

      // Check file size
      if (file.size > maxSize) {
        const maxSizeMB = (maxSize / (1024 * 1024)).toFixed(0);
        return `File too large. Maximum size: ${maxSizeMB}MB`;
      }

      return null;
    },
    [accept, maxSize]
  );

  const handleFile = useCallback(
    (file: File) => {
      const validationError = validateFile(file);
      if (validationError) {
        setError(validationError);
        return;
      }

      setError(null);
      setSelectedFile(file);
      onFileSelect(file);
    },
    [validateFile, onFileSelect]
  );

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      e.stopPropagation();
      setIsDragging(false);

      const files = e.dataTransfer.files;
      if (files.length > 0) {
        handleFile(files[0]);
      }
    },
    [handleFile]
  );

  const handleFileInputChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const files = e.target.files;
      if (files && files.length > 0) {
        handleFile(files[0]);
      }
    },
    [handleFile]
  );

  const handleClick = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="space-y-6">
      {/* Templates Section */}
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
                className="bg-gray-50 rounded-lg p-3 border border-gray-200 hover:border-avisys-blue hover:bg-blue-50 transition-colors"
              >
                <div className="flex items-start gap-2">
                  <span className="text-lg">📋</span>
                  <div className="flex-1 min-w-0">
                    <h4 className="text-sm font-medium text-gray-900 truncate" title={template.templateName}>
                      {template.templateName}
                    </h4>
                    <p className="text-xs text-gray-500">{template.entityType}</p>
                  </div>
                </div>
                <button
                  onClick={() => onTemplateDownload?.(template.id)}
                  className="w-full mt-2 px-2 py-1.5 text-xs bg-white border border-gray-300 rounded hover:bg-gray-50 transition-colors"
                >
                  Download
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Drop Zone */}
      <div
        className={`
          relative border-2 border-dashed rounded-lg p-8 text-center transition-all cursor-pointer
          ${isDragging ? 'border-avisys-blue bg-blue-50' : 'border-gray-300 hover:border-gray-400'}
          ${error ? 'border-red-400 bg-red-50' : ''}
          ${selectedFile ? 'border-green-400 bg-green-50' : ''}
          ${isLoading ? 'pointer-events-none opacity-60' : ''}
        `}
        onDragEnter={handleDragEnter}
        onDragLeave={handleDragLeave}
        onDragOver={handleDragOver}
        onDrop={handleDrop}
        onClick={handleClick}
      >
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept={accept}
          onChange={handleFileInputChange}
        />

        {isLoading ? (
          <div className="flex flex-col items-center">
            <div className="w-12 h-12 border-4 border-avisys-blue border-t-transparent rounded-full animate-spin mb-4" />
            <p className="text-gray-600 font-medium">Processing file...</p>
          </div>
        ) : selectedFile ? (
          <div className="flex flex-col items-center">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mb-4">
              <span className="text-2xl text-green-600">✓</span>
            </div>
            <p className="text-gray-900 font-medium">{selectedFile.name}</p>
            <p className="text-sm text-gray-500 mt-1">{formatFileSize(selectedFile.size)}</p>
            <button
              onClick={(e) => {
                e.stopPropagation();
                setSelectedFile(null);
                setError(null);
                if (fileInputRef.current) fileInputRef.current.value = '';
              }}
              className="mt-3 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50 rounded transition-colors"
            >
              Remove file
            </button>
          </div>
        ) : (
          <div className="flex flex-col items-center">
            <div className="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mb-4">
              <span className="text-2xl">📤</span>
            </div>
            <p className="text-gray-900 font-medium">
              {isDragging ? 'Drop file here' : 'Drag and drop your CSV file here'}
            </p>
            <p className="text-gray-500 text-sm mt-1">or click to browse</p>
            <p className="text-xs text-gray-400 mt-2">
              Supports CSV files up to {formatFileSize(maxSize)}
            </p>
          </div>
        )}
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start gap-3">
          <span className="text-red-500 text-lg">⚠️</span>
          <div>
            <p className="text-red-800 font-medium">Upload Error</p>
            <p className="text-red-600 text-sm mt-1">{error}</p>
          </div>
        </div>
      )}

      {/* File Preview */}
      {validationResult && (
        <div className="bg-white rounded-lg border overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 border-b flex items-center justify-between">
            <h3 className="text-sm font-medium text-gray-700">File Preview</h3>
            <span className="text-xs text-gray-500">{validationResult.totalRows} rows</span>
          </div>
          <div className="overflow-x-auto max-h-64">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50 sticky top-0">
                <tr>
                  <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-12">
                    #
                  </th>
                  {validationResult.headers.map((header, index) => (
                    <th
                      key={index}
                      className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      {header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {validationResult.previewRows.map((row, rowIndex) => (
                  <tr key={rowIndex} className={rowIndex % 2 === 0 ? 'bg-white' : 'bg-gray-50'}>
                    <td className="px-3 py-2 text-xs text-gray-400">{rowIndex + 1}</td>
                    {row.map((cell, cellIndex) => (
                      <td
                        key={cellIndex}
                        className="px-3 py-2 text-sm text-gray-900 max-w-xs truncate"
                        title={cell}
                      >
                        {cell || <span className="text-gray-300 italic">empty</span>}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {validationResult.totalRows > validationResult.previewRows.length && (
            <div className="px-4 py-2 bg-gray-50 border-t text-xs text-gray-500 text-center">
              Showing {validationResult.previewRows.length} of {validationResult.totalRows} rows
            </div>
          )}
        </div>
      )}
    </div>
  );
}
