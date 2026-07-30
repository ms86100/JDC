import React, { useState, useMemo } from 'react';
import { Download, X, ChevronLeft, ChevronRight, Maximize2, FileText, Image, Video, File, Eye, Link2, Clock, User } from 'lucide-react';
import { EvidenceResponse } from '../../../api/testApi';

// Evidence type configuration
const EvidenceTypeConfig: Record<string, { icon: React.ReactNode; color: string; bgColor: string; label: string }> = {
  SCREENSHOT: { icon: <Image className="w-5 h-5" />, color: 'text-blue-600', bgColor: 'bg-blue-100', label: 'Screenshot' },
  VIDEO: { icon: <Video className="w-5 h-5" />, color: 'text-purple-600', bgColor: 'bg-purple-100', label: 'Video' },
  LOG: { icon: <FileText className="w-5 h-5" />, color: 'text-green-600', bgColor: 'bg-green-100', label: 'Log' },
  HAR: { icon: <File className="w-5 h-5" />, color: 'text-orange-600', bgColor: 'bg-orange-100', label: 'HAR' },
  PDF: { icon: <File className="w-5 h-5" />, color: 'text-red-600', bgColor: 'bg-red-100', label: 'PDF' },
  FILE: { icon: <File className="w-5 h-5" />, color: 'text-gray-600', bgColor: 'bg-gray-100', label: 'File' },
};

interface EvidenceViewerProps {
  evidence: EvidenceResponse;
  onClose: () => void;
  onPrevious?: () => void;
  onNext?: () => void;
  hasPrevious?: boolean;
  hasNext?: boolean;
  allEvidence?: EvidenceResponse[];
}

export const EvidenceViewer: React.FC<EvidenceViewerProps> = ({
  evidence,
  onClose,
  onPrevious,
  onNext,
  hasPrevious,
  hasNext,
  allEvidence = [],
}) => {
  const [activeTab, setActiveTab] = useState<'preview' | 'details' | 'metadata'>('preview');
  const [isFullscreen, setIsFullscreen] = useState(false);

  const typeConfig = EvidenceTypeConfig[evidence.evidenceType] || EvidenceTypeConfig.FILE;

  const renderPreview = () => {
    const isImage = evidence.mimeType?.startsWith('image/');
    const isVideo = evidence.mimeType?.startsWith('video/');
    const isPdf = evidence.mimeType === 'application/pdf' || evidence.evidenceType === 'PDF';
    const isLog = evidence.evidenceType === 'LOG' || evidence.mimeType?.includes('text');

    if (isImage) {
      return (
        <div className="flex items-center justify-center h-full bg-gray-900">
          <img
            src={evidence.url || evidence.content}
            alt={evidence.fileName || 'Evidence'}
            className="max-h-full max-w-full object-contain"
          />
        </div>
      );
    }

    if (isVideo) {
      return (
        <div className="flex items-center justify-center h-full bg-gray-900">
          <video
            src={evidence.url || evidence.content}
            controls
            className="max-h-full max-w-full"
            autoPlay
          />
        </div>
      );
    }

    if (isPdf) {
      return (
        <div className="flex items-center justify-center h-full bg-gray-100">
          <div className="text-center">
            <File className="w-20 h-20 mx-auto text-red-500 mb-4" />
            <p className="text-gray-700 font-medium mb-2">{evidence.fileName}</p>
            <a
              href={evidence.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              <Eye className="w-4 h-4" />
              Open PDF
            </a>
          </div>
        </div>
      );
    }

    if (isLog) {
      return (
        <div className="h-full overflow-auto bg-gray-900 text-gray-100 p-4">
          <pre className="font-mono text-sm whitespace-pre-wrap">
            {evidence.content || 'No content available'}
          </pre>
        </div>
      );
    }

    // Default: show download option
    return (
      <div className="flex items-center justify-center h-full bg-gray-100">
        <div className="text-center">
          <div className={`w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center ${typeConfig.bgColor}`}>
            <span className={`${typeConfig.color}`}>{typeConfig.icon}</span>
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">{evidence.fileName || 'Evidence File'}</h3>
          <p className="text-sm text-gray-500 mb-4">
            {evidence.mimeType || evidence.evidenceType}
            {evidence.fileSize && ` • ${(evidence.fileSize / 1024).toFixed(1)} KB`}
          </p>
          <a
            href={evidence.url}
            download={evidence.fileName}
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            <Download className="w-4 h-4" />
            Download File
          </a>
        </div>
      </div>
    );
  };

  const formatDate = (dateString: string | null | undefined) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  const formatFileSize = (bytes: number | null | undefined) => {
    if (!bytes) return 'N/A';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div
      className={`fixed inset-0 z-50 bg-black flex flex-col ${
        isFullscreen ? '' : ''
      }`}
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-gray-900 text-white">
        <div className="flex items-center gap-4">
          <div className={`p-2 rounded-lg ${typeConfig.bgColor}`}>
            <span className={typeConfig.color}>{typeConfig.icon}</span>
          </div>
          <div>
            <h2 className="font-semibold">{evidence.fileName || 'Evidence Viewer'}</h2>
            <p className="text-sm text-gray-400">
              {typeConfig.label}
              {evidence.fileSize && ` • ${formatFileSize(evidence.fileSize)}`}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Navigation */}
          {onPrevious && (
            <button
              onClick={onPrevious}
              disabled={!hasPrevious}
              className={`p-2 rounded-lg hover:bg-gray-700 ${
                hasPrevious ? '' : 'opacity-50 cursor-not-allowed'
              }`}
              title="Previous"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
          )}
          {onNext && (
            <button
              onClick={onNext}
              disabled={!hasNext}
              className={`p-2 rounded-lg hover:bg-gray-700 ${
                hasNext ? '' : 'opacity-50 cursor-not-allowed'
              }`}
              title="Next"
            >
              <ChevronRight className="w-5 h-5" />
            </button>
          )}

          {/* Actions */}
          <button
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="p-2 rounded-lg hover:bg-gray-700"
            title={isFullscreen ? 'Exit Fullscreen' : 'Fullscreen'}
          >
            <Maximize2 className="w-5 h-5" />
          </button>

          {evidence.url && (
            <a
              href={evidence.url}
              download={evidence.fileName}
              className="p-2 rounded-lg hover:bg-gray-700"
              title="Download"
            >
              <Download className="w-5 h-5" />
            </a>
          )}

          <button
            onClick={onClose}
            className="p-2 rounded-lg hover:bg-gray-700"
            title="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 px-4 py-2 bg-gray-800 text-white">
        <button
          onClick={() => setActiveTab('preview')}
          className={`px-4 py-2 rounded-lg text-sm font-medium ${
            activeTab === 'preview' ? 'bg-gray-700' : 'hover:bg-gray-700'
          }`}
        >
          Preview
        </button>
        <button
          onClick={() => setActiveTab('details')}
          className={`px-4 py-2 rounded-lg text-sm font-medium ${
            activeTab === 'details' ? 'bg-gray-700' : 'hover:bg-gray-700'
          }`}
        >
          Details
        </button>
        <button
          onClick={() => setActiveTab('metadata')}
          className={`px-4 py-2 rounded-lg text-sm font-medium ${
            activeTab === 'metadata' ? 'bg-gray-700' : 'hover:bg-gray-700'
          }`}
        >
          Metadata {evidence.metadata && Object.keys(evidence.metadata).length > 0 && (
            <span className="ml-1 px-1.5 py-0.5 bg-blue-600 rounded text-xs">
              {Object.keys(evidence.metadata).length}
            </span>
          )}
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-hidden">
        {activeTab === 'preview' && renderPreview()}

        {activeTab === 'details' && (
          <div className="h-full overflow-auto p-6 bg-gray-100">
            <div className="max-w-2xl mx-auto bg-white rounded-lg shadow">
              <table className="w-full">
                <tbody>
                  <tr className="border-b">
                    <td className="px-4 py-3 text-gray-500 font-medium">File Name</td>
                    <td className="px-4 py-3">{evidence.fileName || 'N/A'}</td>
                  </tr>
                  <tr className="border-b">
                    <td className="px-4 py-3 text-gray-500 font-medium">Evidence Type</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center gap-1 px-2 py-1 rounded ${typeConfig.bgColor} ${typeConfig.color}`}>
                        {typeConfig.icon}
                        {typeConfig.label}
                      </span>
                    </td>
                  </tr>
                  <tr className="border-b">
                    <td className="px-4 py-3 text-gray-500 font-medium">Classification</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 rounded text-sm ${
                        evidence.classificationLevel === 'STEP_LEVEL' ? 'bg-green-100 text-green-800' :
                        evidence.classificationLevel === 'RUN_LEVEL' ? 'bg-blue-100 text-blue-800' :
                        'bg-purple-100 text-purple-800'
                      }`}>
                        {evidence.classificationLevel?.replace('_', ' ') || 'N/A'}
                      </span>
                    </td>
                  </tr>
                  <tr className="border-b">
                    <td className="px-4 py-3 text-gray-500 font-medium">MIME Type</td>
                    <td className="px-4 py-3">{evidence.mimeType || 'N/A'}</td>
                  </tr>
                  <tr className="border-b">
                    <td className="px-4 py-3 text-gray-500 font-medium">File Size</td>
                    <td className="px-4 py-3">{formatFileSize(evidence.fileSize)}</td>
                  </tr>
                  <tr className="border-b">
                    <td className="px-4 py-3 text-gray-500 font-medium">Created By</td>
                    <td className="px-4 py-3 flex items-center gap-2">
                      <User className="w-4 h-4 text-gray-400" />
                      {evidence.createdBy || 'System'}
                    </td>
                  </tr>
                  <tr className="border-b">
                    <td className="px-4 py-3 text-gray-500 font-medium">Created At</td>
                    <td className="px-4 py-3 flex items-center gap-2">
                      <Clock className="w-4 h-4 text-gray-400" />
                      {formatDate(evidence.createdAt)}
                    </td>
                  </tr>
                  {evidence.retentionPolicyName && (
                    <tr className="border-b">
                      <td className="px-4 py-3 text-gray-500 font-medium">Retention Policy</td>
                      <td className="px-4 py-3">{evidence.retentionPolicyName}</td>
                    </tr>
                  )}
                  {evidence.stepResultId && (
                    <tr className="border-b">
                      <td className="px-4 py-3 text-gray-500 font-medium">Linked Step</td>
                      <td className="px-4 py-3 flex items-center gap-2">
                        <Link2 className="w-4 h-4 text-gray-400" />
                        {evidence.stepResultId}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === 'metadata' && (
          <div className="h-full overflow-auto p-6 bg-gray-100">
            <div className="max-w-2xl mx-auto space-y-4">
              {/* Extracted Metadata */}
              {evidence.metadata && Object.keys(evidence.metadata).length > 0 && (
                <div className="bg-white rounded-lg shadow p-4">
                  <h3 className="font-semibold text-gray-700 mb-3">Extracted Metadata</h3>
                  <div className="grid grid-cols-2 gap-4">
                    {Object.entries(evidence.metadata).map(([key, value]) => (
                      <div key={key} className="flex flex-col">
                        <span className="text-xs text-gray-500 uppercase">{key.replace('_', ' ')}</span>
                        <span className="text-sm font-mono bg-gray-50 px-2 py-1 rounded mt-1">
                          {value}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Quick Actions */}
              <div className="bg-white rounded-lg shadow p-4">
                <h3 className="font-semibold text-gray-700 mb-3">Quick Actions</h3>
                <div className="space-y-2">
                  <button className="w-full flex items-center gap-3 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-lg text-left">
                    <Link2 className="w-4 h-4 text-blue-500" />
                    <div>
                      <p className="font-medium">Link to Test Step</p>
                      <p className="text-xs text-gray-500">Associate with a specific test step</p>
                    </div>
                  </button>
                  <button className="w-full flex items-center gap-3 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-lg text-left">
                    <FileText className="w-4 h-4 text-green-500" />
                    <div>
                      <p className="font-medium">Add Tags</p>
                      <p className="text-xs text-gray-500">Categorize this evidence</p>
                    </div>
                  </button>
                  <button className="w-full flex items-center gap-3 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-lg text-left">
                    <Eye className="w-4 h-4 text-purple-500" />
                    <div>
                      <p className="font-medium">View Chain of Custody</p>
                      <p className="text-xs text-gray-500">See evidence history and integrity</p>
                    </div>
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

// Thumbnail Grid Component for evidence selection
interface EvidenceThumbnailGridProps {
  evidence: EvidenceResponse[];
  selectedId?: string;
  onSelect: (evidence: EvidenceResponse) => void;
}

export const EvidenceThumbnailGrid: React.FC<EvidenceThumbnailGridProps> = ({
  evidence,
  selectedId,
  onSelect,
}) => {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  if (evidence.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        No evidence available
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* View Toggle */}
      <div className="flex items-center justify-end">
        <div className="flex items-center border border-gray-300 rounded-lg overflow-hidden">
          <button
            onClick={() => setViewMode('grid')}
            className={`p-2 ${viewMode === 'grid' ? 'bg-blue-100 text-blue-600' : 'hover:bg-gray-50'}`}
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
            </svg>
          </button>
          <button
            onClick={() => setViewMode('list')}
            className={`p-2 ${viewMode === 'list' ? 'bg-blue-100 text-blue-600' : 'hover:bg-gray-50'}`}
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
      </div>

      {/* Grid View */}
      {viewMode === 'grid' && (
        <div className="grid grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
          {evidence.map((item) => {
            const config = EvidenceTypeConfig[item.evidenceType] || EvidenceTypeConfig.FILE;
            const isSelected = item.id === selectedId;
            const isImage = item.mimeType?.startsWith('image/');

            return (
              <div
                key={item.id}
                onClick={() => onSelect(item)}
                className={`relative bg-white rounded-lg border overflow-hidden cursor-pointer transition-all ${
                  isSelected ? 'ring-2 ring-blue-500 shadow-lg' : 'hover:shadow-md'
                }`}
              >
                <div className="aspect-video bg-gray-100">
                  {isImage && item.thumbnailUrl ? (
                    <img
                      src={item.thumbnailUrl}
                      alt={item.fileName}
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center">
                      <div className={`p-4 rounded-full ${config.bgColor}`}>
                        <span className={config.color}>{config.icon}</span>
                      </div>
                    </div>
                  )}
                </div>
                <div className="p-2">
                  <p className="text-xs font-medium truncate">{item.fileName}</p>
                  <p className="text-xs text-gray-500">
                    {item.fileSize && `${(item.fileSize / 1024).toFixed(0)} KB`}
                  </p>
                </div>
                {isSelected && (
                  <div className="absolute top-2 right-2">
                    <div className="w-5 h-5 bg-blue-500 rounded-full flex items-center justify-center">
                      <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* List View */}
      {viewMode === 'list' && (
        <div className="bg-white rounded-lg border overflow-hidden">
          <div className="px-4 py-3 bg-gray-50 border-b flex items-center text-xs font-medium text-gray-500">
            <div className="w-10"></div>
            <div className="flex-1">File</div>
            <div className="w-24 text-right">Type</div>
            <div className="w-24 text-right">Size</div>
            <div className="w-32 text-right">Created</div>
          </div>
          {evidence.map((item) => {
            const config = EvidenceTypeConfig[item.evidenceType] || EvidenceTypeConfig.FILE;
            const isSelected = item.id === selectedId;

            return (
              <div
                key={item.id}
                onClick={() => onSelect(item)}
                className={`px-4 py-3 flex items-center gap-4 border-b cursor-pointer ${
                  isSelected ? 'bg-blue-50' : 'hover:bg-gray-50'
                }`}
              >
                <div className={`p-2 rounded ${config.bgColor}`}>
                  <span className={config.color}>{config.icon}</span>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{item.fileName}</p>
                </div>
                <div className="w-24 text-right">
                  <span className={`text-xs px-2 py-0.5 rounded ${config.bgColor} ${config.color}`}>
                    {config.label}
                  </span>
                </div>
                <div className="w-24 text-right text-sm text-gray-500">
                  {item.fileSize && `${(item.fileSize / 1024).toFixed(0)} KB`}
                </div>
                <div className="w-32 text-right text-sm text-gray-500">
                  {item.createdAt && new Date(item.createdAt).toLocaleDateString()}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default EvidenceViewer;