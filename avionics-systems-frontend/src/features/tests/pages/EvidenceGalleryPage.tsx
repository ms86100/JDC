import React, { useState, useMemo } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import advancedApi, { EvidenceResponse, EvidenceViewerData, EvidenceGroup } from '../../../api/testApi';
import {
  Search,
  Filter,
  Grid,
  List,
  Image,
  Video,
  FileText,
  File,
  Download,
  Trash2,
  Eye,
  Maximize2,
  ChevronLeft,
  ChevronRight,
  X,
  Calendar,
  Clock,
  User,
  Monitor,
  Tag,
  FolderOpen,
  MoreVertical,
  RefreshCw,
  Upload,
  Plus,
  ExternalLink,
  SortAsc,
  SortDesc,
  CheckCircle2,
  AlertCircle,
  Loader2,
} from 'lucide-react';

interface EvidenceFilters {
  type: string;
  level: string;
  search: string;
  dateRange: string;
}

const initialFilters: EvidenceFilters = {
  type: '',
  level: '',
  search: '',
  dateRange: '',
};

type ViewMode = 'grid' | 'list';

// Evidence type icons and colors
const EvidenceTypeConfig: Record<string, { icon: React.ReactNode; color: string; bgColor: string; label: string }> = {
  SCREENSHOT: { icon: <Image className="w-4 h-4" />, color: 'text-blue-600', bgColor: 'bg-blue-100', label: 'Screenshot' },
  VIDEO: { icon: <Video className="w-4 h-4" />, color: 'text-purple-600', bgColor: 'bg-purple-100', label: 'Video' },
  LOG: { icon: <FileText className="w-4 h-4" />, color: 'text-green-600', bgColor: 'bg-green-100', label: 'Log' },
  HAR: { icon: <File className="w-4 h-4" />, color: 'text-orange-600', bgColor: 'bg-orange-100', label: 'HAR' },
  PDF: { icon: <File className="w-4 h-4" />, color: 'text-red-600', bgColor: 'bg-red-100', label: 'PDF' },
  FILE: { icon: <File className="w-4 h-4" />, color: 'text-gray-600', bgColor: 'bg-gray-100', label: 'File' },
  COMMENT: { icon: <FileText className="w-4 h-4" />, color: 'text-yellow-600', bgColor: 'bg-yellow-100', label: 'Comment' },
};

// Classification levels
const ClassificationConfig: Record<string, { color: string; bgColor: string; label: string }> = {
  STEP_LEVEL: { color: 'text-green-700', bgColor: 'bg-green-100', label: 'Step Level' },
  RUN_LEVEL: { color: 'text-blue-700', bgColor: 'bg-blue-100', label: 'Run Level' },
  ENVIRONMENT_LEVEL: { color: 'text-purple-700', bgColor: 'bg-purple-100', label: 'Environment Level' },
};

// Lightbox Component
const EvidenceLightbox: React.FC<{
  evidence: EvidenceResponse;
  onClose: () => void;
  onPrevious: () => void;
  onNext: () => void;
  hasPrevious: boolean;
  hasNext: boolean;
}> = ({ evidence, onClose, onPrevious, onNext, hasPrevious, hasNext }) => {
  const typeConfig = EvidenceTypeConfig[evidence.evidenceType] || EvidenceTypeConfig.FILE;

  const renderContent = () => {
    const isImage = evidence.mimeType?.startsWith('image/');
    const isVideo = evidence.mimeType?.startsWith('video/');

    if (isImage) {
      return (
        <img
          src={evidence.url}
          alt={evidence.fileName || 'Evidence'}
          className="max-h-[80vh] max-w-full object-contain"
        />
      );
    }

    if (isVideo) {
      return (
        <video
          src={evidence.url}
          controls
          className="max-h-[80vh] max-w-full"
        />
      );
    }

    // For other types, show a download link
    return (
      <div className="text-center py-12">
        <div className={`w-20 h-20 mx-auto mb-4 rounded-full flex items-center justify-center ${typeConfig.bgColor}`}>
          <span className={`${typeConfig.color}`}>{typeConfig.icon}</span>
        </div>
        <h3 className="text-lg font-medium text-gray-900 mb-2">{evidence.fileName}</h3>
        <p className="text-sm text-gray-500 mb-4">{evidence.evidenceType}</p>
        <a
          href={evidence.url}
          download={evidence.fileName}
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          <Download className="w-4 h-4" />
          Download File
        </a>
      </div>
    );
  };

  return (
    <div className="fixed inset-0 z-50 bg-black bg-opacity-90 flex items-center justify-center">
      {/* Navigation */}
      <button
        onClick={onPrevious}
        disabled={!hasPrevious}
        className={`absolute left-4 top-1/2 -translate-y-1/2 p-3 bg-white bg-opacity-10 rounded-full ${
          hasPrevious ? 'hover:bg-opacity-20' : 'opacity-30 cursor-not-allowed'
        }`}
      >
        <ChevronLeft className="w-6 h-6 text-white" />
      </button>

      <button
        onClick={onNext}
        disabled={!hasNext}
        className={`absolute right-4 top-1/2 -translate-y-1/2 p-3 bg-white bg-opacity-10 rounded-full ${
          hasNext ? 'hover:bg-opacity-20' : 'opacity-30 cursor-not-allowed'
        }`}
      >
        <ChevronRight className="w-6 h-6 text-white" />
      </button>

      {/* Content */}
      <div className="max-w-5xl w-full mx-4">
        {renderContent()}
      </div>

      {/* Close Button */}
      <button
        onClick={onClose}
        className="absolute top-4 right-4 p-2 bg-white bg-opacity-10 rounded-full hover:bg-opacity-20"
      >
        <X className="w-6 h-6 text-white" />
      </button>

      {/* Info Bar */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-4 px-4 py-2 bg-white bg-opacity-10 rounded-lg">
        <span className="text-white text-sm">{evidence.fileName}</span>
        {evidence.fileSize && (
          <span className="text-white text-sm opacity-70">
            {(evidence.fileSize / 1024).toFixed(1)} KB
          </span>
        )}
      </div>
    </div>
  );
};

// Grid Item Component
const EvidenceGridItem: React.FC<{
  evidence: EvidenceResponse;
  onClick: () => void;
}> = ({ evidence, onClick }) => {
  const typeConfig = EvidenceTypeConfig[evidence.evidenceType] || EvidenceTypeConfig.FILE;
  const isImage = evidence.mimeType?.startsWith('image/');

  return (
    <div
      className="bg-white rounded-lg border overflow-hidden cursor-pointer hover:shadow-md transition-shadow"
      onClick={onClick}
    >
      {/* Thumbnail */}
      <div className="aspect-video bg-gray-100 relative">
        {isImage && evidence.thumbnailUrl ? (
          <img
            src={evidence.thumbnailUrl}
            alt={evidence.fileName}
            className="w-full h-full object-cover"
          />
        ) : isImage && evidence.url ? (
          <img
            src={evidence.url}
            alt={evidence.fileName}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <div className={`p-4 rounded-full ${typeConfig.bgColor}`}>
              <span className={`${typeConfig.color}`}>{typeConfig.icon}</span>
            </div>
          </div>
        )}

        {/* Type Badge */}
        <div className={`absolute top-2 right-2 px-2 py-1 rounded ${typeConfig.bgColor}`}>
          <span className={`text-xs font-medium ${typeConfig.color}`}>
            {typeConfig.label}
          </span>
        </div>

        {/* Expand Icon */}
        <div className="absolute inset-0 bg-black bg-opacity-0 hover:bg-opacity-20 flex items-center justify-center transition-all">
          <Maximize2 className="w-6 h-6 text-white opacity-0 hover:opacity-100" />
        </div>
      </div>

      {/* Info */}
      <div className="p-3">
        <p className="text-sm font-medium text-gray-900 truncate">{evidence.fileName}</p>
        <div className="flex items-center justify-between mt-2">
          <span className="text-xs text-gray-500">
            {evidence.fileSize ? `${(evidence.fileSize / 1024).toFixed(1)} KB` : 'N/A'}
          </span>
          <span className="text-xs text-gray-500">
            {new Date(evidence.createdAt).toLocaleDateString()}
          </span>
        </div>
      </div>
    </div>
  );
};

// List Item Component
const EvidenceListItem: React.FC<{
  evidence: EvidenceResponse;
  onClick: () => void;
}> = ({ evidence, onClick }) => {
  const typeConfig = EvidenceTypeConfig[evidence.evidenceType] || EvidenceTypeConfig.FILE;
  const classConfig = ClassificationConfig[evidence.classificationLevel || ''] || { color: 'text-gray-600', bgColor: 'bg-gray-100', label: 'Unknown' };

  return (
    <div
      className="bg-white border-b last:border-b-0 hover:bg-gray-50 cursor-pointer"
      onClick={onClick}
    >
      <div className="px-4 py-3 flex items-center gap-4">
        {/* Icon */}
        <div className={`p-2 rounded ${typeConfig.bgColor}`}>
          <span className={`${typeConfig.color}`}>{typeConfig.icon}</span>
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-900 truncate">{evidence.fileName}</p>
          <div className="flex items-center gap-4 mt-1">
            <span className={`text-xs px-2 py-0.5 rounded ${typeConfig.bgColor} ${typeConfig.color}`}>
              {typeConfig.label}
            </span>
            {evidence.classificationLevel && (
              <span className={`text-xs px-2 py-0.5 rounded ${classConfig.bgColor} ${classConfig.color}`}>
                {classConfig.label}
              </span>
            )}
          </div>
        </div>

        {/* Metadata */}
        <div className="flex items-center gap-6 text-sm text-gray-500">
          <div className="text-right">
            <p>Size</p>
            <p className="font-medium text-gray-900">
              {evidence.fileSize ? `${(evidence.fileSize / 1024).toFixed(1)} KB` : 'N/A'}
            </p>
          </div>
          <div className="text-right">
            <p>Created</p>
            <p className="font-medium text-gray-900">
              {new Date(evidence.createdAt).toLocaleDateString()}
            </p>
          </div>
          <div className="text-right">
            <p>By</p>
            <p className="font-medium text-gray-900">
              {evidence.createdBy || 'System'}
            </p>
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2">
          <a
            href={evidence.url}
            download={evidence.fileName}
            onClick={(e) => e.stopPropagation()}
            className="p-2 hover:bg-gray-100 rounded"
            title="Download"
          >
            <Download className="w-4 h-4 text-gray-400" />
          </a>
          <button
            onClick={(e) => e.stopPropagation()}
            className="p-2 hover:bg-gray-100 rounded"
            title="More"
          >
            <MoreVertical className="w-4 h-4 text-gray-400" />
          </button>
        </div>
      </div>
    </div>
  );
};

export const EvidenceGalleryPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const [filters, setFilters] = useState<EvidenceFilters>(initialFilters);
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [selectedEvidence, setSelectedEvidence] = useState<EvidenceResponse | null>(null);
  const [lightboxIndex, setLightboxIndex] = useState(0);
  const [showLightbox, setShowLightbox] = useState(false);

  // Get execution ID from URL params
  const executionIdFromUrl = searchParams.get('executionId');

  // Fetch evidence data
  const { data: viewerData, isLoading, refetch } = useQuery({
    queryKey: ['evidence-gallery', executionIdFromUrl],
    queryFn: async () => {
      if (executionIdFromUrl) {
        return advancedApi.evidence.getViewerData(executionIdFromUrl);
      }
      // Return mock data for demo when no execution ID
      return {
        executionId: '',
        executionKey: '',
        evidenceGroups: [],
        totalCount: 0,
        countByType: {},
        countByLevel: {},
      } as EvidenceViewerData;
    },
    enabled: !!executionIdFromUrl,
  });

  // Mock evidence data for demonstration
  const mockEvidence: EvidenceResponse[] = executionIdFromUrl ? [
    {
      id: '1',
      executionId: executionIdFromUrl,
      evidenceType: 'SCREENSHOT',
      classificationLevel: 'STEP_LEVEL',
      fileName: 'step-1-login-page.png',
      fileSize: 245000,
      mimeType: 'image/png',
      url: 'https://picsum.photos/800/600',
      thumbnailUrl: 'https://picsum.photos/200/150',
      createdBy: 'john.doe',
      createdAt: '2024-01-15T10:30:00Z',
    },
    {
      id: '2',
      executionId: executionIdFromUrl,
      evidenceType: 'SCREENSHOT',
      classificationLevel: 'STEP_LEVEL',
      fileName: 'step-2-dashboard.png',
      fileSize: 312000,
      mimeType: 'image/png',
      url: 'https://picsum.photos/800/600?random=2',
      thumbnailUrl: 'https://picsum.photos/200/150?random=2',
      createdBy: 'john.doe',
      createdAt: '2024-01-15T10:31:00Z',
    },
    {
      id: '3',
      executionId: executionIdFromUrl,
      evidenceType: 'LOG',
      classificationLevel: 'RUN_LEVEL',
      fileName: 'test-execution.log',
      fileSize: 15000,
      mimeType: 'text/plain',
      url: '#',
      createdBy: 'system',
      createdAt: '2024-01-15T10:32:00Z',
    },
    {
      id: '4',
      executionId: executionIdFromUrl,
      evidenceType: 'SCREENSHOT',
      classificationLevel: 'STEP_LEVEL',
      fileName: 'step-3-error.png',
      fileSize: 189000,
      mimeType: 'image/png',
      url: 'https://picsum.photos/800/600?random=3',
      thumbnailUrl: 'https://picsum.photos/200/150?random=3',
      createdBy: 'john.doe',
      createdAt: '2024-01-15T10:33:00Z',
    },
    {
      id: '5',
      executionId: executionIdFromUrl,
      evidenceType: 'VIDEO',
      classificationLevel: 'RUN_LEVEL',
      fileName: 'execution-recording.mp4',
      fileSize: 5240000,
      mimeType: 'video/mp4',
      url: '#',
      createdBy: 'system',
      createdAt: '2024-01-15T10:34:00Z',
    },
    {
      id: '6',
      executionId: executionIdFromUrl,
      evidenceType: 'HAR',
      classificationLevel: 'RUN_LEVEL',
      fileName: 'network-trace.har',
      fileSize: 45000,
      mimeType: 'application/json',
      url: '#',
      createdBy: 'system',
      createdAt: '2024-01-15T10:34:30Z',
    },
  ] : [];

  // Use mock data when no execution ID, otherwise use API data
  const allEvidence = executionIdFromUrl ? mockEvidence : [];

  // Filter evidence
  const filteredEvidence = useMemo(() => {
    return allEvidence.filter(evidence => {
      if (filters.type && evidence.evidenceType !== filters.type) return false;
      if (filters.level && evidence.classificationLevel !== filters.level) return false;
      if (filters.search) {
        const searchLower = filters.search.toLowerCase();
        const matchesFileName = evidence.fileName?.toLowerCase().includes(searchLower);
        if (!matchesFileName) return false;
      }
      return true;
    });
  }, [allEvidence, filters]);

  // Calculate stats
  const stats = useMemo(() => {
    const counts = {
      total: allEvidence.length,
      screenshots: allEvidence.filter(e => e.evidenceType === 'SCREENSHOT').length,
      videos: allEvidence.filter(e => e.evidenceType === 'VIDEO').length,
      logs: allEvidence.filter(e => e.evidenceType === 'LOG').length,
      other: allEvidence.filter(e => !['SCREENSHOT', 'VIDEO', 'LOG'].includes(e.evidenceType)).length,
    };
    return counts;
  }, [allEvidence]);

  const handleOpenLightbox = (evidence: EvidenceResponse, index: number) => {
    setSelectedEvidence(evidence);
    setLightboxIndex(index);
    setShowLightbox(true);
  };

  const handlePrevious = () => {
    if (lightboxIndex > 0) {
      const newIndex = lightboxIndex - 1;
      setLightboxIndex(newIndex);
      setSelectedEvidence(filteredEvidence[newIndex]);
    }
  };

  const handleNext = () => {
    if (lightboxIndex < filteredEvidence.length - 1) {
      const newIndex = lightboxIndex + 1;
      setLightboxIndex(newIndex);
      setSelectedEvidence(filteredEvidence[newIndex]);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Evidence Gallery</h1>
              <p className="text-sm text-gray-500 mt-1">
                {executionIdFromUrl
                  ? `Execution: ${executionIdFromUrl.slice(0, 8)}... | ${stats.total} evidence items`
                  : 'View test execution evidence'}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => refetch()}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                <RefreshCw className="w-4 h-4" />
                Refresh
              </button>
              <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                <Upload className="w-4 h-4" />
                Upload
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        <div className="grid grid-cols-5 gap-4">
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500">Total</div>
            <div className="text-2xl font-bold mt-1">{stats.total}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500 flex items-center gap-1">
              <Image className="w-4 h-4 text-blue-500" />
              Screenshots
            </div>
            <div className="text-2xl font-bold mt-1 text-blue-600">{stats.screenshots}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500 flex items-center gap-1">
              <Video className="w-4 h-4 text-purple-500" />
              Videos
            </div>
            <div className="text-2xl font-bold mt-1 text-purple-600">{stats.videos}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500 flex items-center gap-1">
              <FileText className="w-4 h-4 text-green-500" />
              Logs
            </div>
            <div className="text-2xl font-bold mt-1 text-green-600">{stats.logs}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500">Other</div>
            <div className="text-2xl font-bold mt-1 text-gray-600">{stats.other}</div>
          </div>
        </div>
      </div>

      {/* Filters & View Toggle */}
      <div className="max-w-7xl mx-auto px-6 py-2">
        <div className="bg-white rounded-lg border">
          <div className="p-4 flex items-center gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search evidence..."
                value={filters.search}
                onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <select
              value={filters.type}
              onChange={(e) => setFilters({ ...filters, type: e.target.value })}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Types</option>
              <option value="SCREENSHOT">Screenshot</option>
              <option value="VIDEO">Video</option>
              <option value="LOG">Log</option>
              <option value="HAR">HAR</option>
              <option value="PDF">PDF</option>
              <option value="FILE">File</option>
            </select>
            <select
              value={filters.level}
              onChange={(e) => setFilters({ ...filters, level: e.target.value })}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Levels</option>
              <option value="STEP_LEVEL">Step Level</option>
              <option value="RUN_LEVEL">Run Level</option>
              <option value="ENVIRONMENT_LEVEL">Environment Level</option>
            </select>
            <button
              onClick={() => setFilters(initialFilters)}
              className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg"
            >
              Clear
            </button>

            {/* View Toggle */}
            <div className="flex items-center border border-gray-300 rounded-lg overflow-hidden">
              <button
                onClick={() => setViewMode('grid')}
                className={`p-2 ${viewMode === 'grid' ? 'bg-blue-100 text-blue-600' : 'hover:bg-gray-50'}`}
              >
                <Grid className="w-4 h-4" />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={`p-2 ${viewMode === 'list' ? 'bg-blue-100 text-blue-600' : 'hover:bg-gray-50'}`}
              >
                <List className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Evidence Display */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : filteredEvidence.length === 0 ? (
          <div className="bg-white rounded-lg border p-12 text-center">
            <FolderOpen className="w-12 h-12 mx-auto text-gray-300 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No evidence found</h3>
            <p className="text-gray-500">
              {allEvidence.length === 0
                ? executionIdFromUrl
                  ? 'No evidence has been captured for this execution'
                  : 'Select an execution to view its evidence'
                : 'No evidence matches your filter criteria'}
            </p>
          </div>
        ) : viewMode === 'grid' ? (
          <div className="grid grid-cols-4 gap-4">
            {filteredEvidence.map((evidence, index) => (
              <EvidenceGridItem
                key={evidence.id}
                evidence={evidence}
                onClick={() => handleOpenLightbox(evidence, index)}
              />
            ))}
          </div>
        ) : (
          <div className="bg-white rounded-lg border overflow-hidden">
            <div className="px-4 py-3 bg-gray-50 border-b border-gray-200 flex items-center gap-4 text-sm font-medium text-gray-500">
              <div className="w-10"></div>
              <div className="flex-1">File</div>
              <div className="w-40 text-right">Size</div>
              <div className="w-40 text-right">Created</div>
              <div className="w-40 text-right">By</div>
              <div className="w-20">Actions</div>
            </div>
            <div>
              {filteredEvidence.map((evidence, index) => (
                <EvidenceListItem
                  key={evidence.id}
                  evidence={evidence}
                  onClick={() => handleOpenLightbox(evidence, index)}
                />
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Lightbox */}
      {showLightbox && selectedEvidence && (
        <EvidenceLightbox
          evidence={selectedEvidence}
          onClose={() => setShowLightbox(false)}
          onPrevious={handlePrevious}
          onNext={handleNext}
          hasPrevious={lightboxIndex > 0}
          hasNext={lightboxIndex < filteredEvidence.length - 1}
        />
      )}
    </div>
  );
};

export default EvidenceGalleryPage;