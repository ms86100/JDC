import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import {
  Play, Pause, SkipBack, SkipForward, FastForward, Rewind,
  Clock, Calendar, Activity, Video, Download, Settings,
  ChevronLeft, ChevronRight, Maximize2, Volume2, VolumeX,
  Eye, Layers, GitBranch, Filter, Search
} from 'lucide-react';

// Types
interface TimelineEvent {
  id: string;
  timestamp: string;
  type: 'TEST_START' | 'TEST_COMPLETE' | 'STEP_PASS' | 'STEP_FAIL' | 'DEFECT_LINKED' | 'PRECONDITION_FAIL' | 'EVIDENCE_ADDED' | 'STATUS_CHANGE';
  testId: string;
  testName?: string;
  stepIndex?: number;
  result?: string;
  details?: string;
  userId?: string;
}

interface PlaybackSession {
  sessionId: string;
  executionId: string;
  name: string;
  playbackPositionMs: number;
  isPlaying: boolean;
  playbackSpeed: number;
  status: string;
  eventCount: number;
  createdBy?: string;
  sessionStart?: string;
  sessionEnd?: string;
}

interface TimelineSummary {
  executionId: string;
  totalEvents: number;
  duration: number;
  startTime: string;
  endTime: string;
  passCount: number;
  failCount: number;
  blockCount: number;
  skipCount: number;
}

interface Snapshot {
  id: string;
  executionId: string;
  name: string;
  description?: string;
  createdAt: string;
  eventCount: number;
}

// Confirm Dialog Component
const ConfirmDialog: React.FC<{
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  variant?: 'default' | 'danger';
}> = ({ open, title, message, confirmLabel = 'Confirm', onConfirm, onCancel, variant = 'default' }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onCancel}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-2">{title}</h3>
          <p className="text-gray-600 mb-6">{message}</p>
          <div className="flex justify-end gap-3">
            <button onClick={onCancel} className="btn btn-secondary">Cancel</button>
            <button
              onClick={onConfirm}
              className={`btn ${variant === 'danger' ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}
            >
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Stats Card Component
interface StatsCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  color?: string;
}

const StatsCard: React.FC<StatsCardProps> = ({ title, value, subtitle, icon, color = 'blue' }) => {
  const colorClasses: Record<string, { bg: string; icon: string }> = {
    blue: { bg: 'bg-blue-50', icon: 'text-blue-500' },
    green: { bg: 'bg-green-50', icon: 'text-green-500' },
    red: { bg: 'bg-red-50', icon: 'text-red-500' },
    yellow: { bg: 'bg-yellow-50', icon: 'text-yellow-500' },
    purple: { bg: 'bg-purple-50', icon: 'text-purple-500' },
  };

  const colors = colorClasses[color] || colorClasses.blue;

  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-2xl font-bold mt-1">{value}</p>
          {subtitle && <p className="text-xs text-gray-400 mt-1">{subtitle}</p>}
        </div>
        <div className={`${colors.bg} p-3 rounded-lg`}>
          <div className={colors.icon}>{icon}</div>
        </div>
      </div>
    </div>
  );
};

// Event Type Badge
const EventTypeBadge: React.FC<{ type: string }> = ({ type }) => {
  const typeConfig: Record<string, { bg: string; text: string; icon: string }> = {
    TEST_START: { bg: 'bg-blue-100', text: 'text-blue-700', icon: '▶' },
    TEST_COMPLETE: { bg: 'bg-green-100', text: 'text-green-700', icon: '✓' },
    STEP_PASS: { bg: 'bg-green-50', text: 'text-green-600', icon: '✓' },
    STEP_FAIL: { bg: 'bg-red-100', text: 'text-red-700', icon: '✗' },
    DEFECT_LINKED: { bg: 'bg-orange-100', text: 'text-orange-700', icon: '🐛' },
    PRECONDITION_FAIL: { bg: 'bg-yellow-100', text: 'text-yellow-700', icon: '⚠' },
    EVIDENCE_ADDED: { bg: 'bg-purple-100', text: 'text-purple-700', icon: '📎' },
    STATUS_CHANGE: { bg: 'bg-gray-100', text: 'text-gray-700', icon: '↻' },
  };

  const config = typeConfig[type] || typeConfig.STATUS_CHANGE;

  return (
    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${config.bg} ${config.text}`}>
      {config.icon} {type.replace(/_/g, ' ')}
    </span>
  );
};

// Timeline Event Item
interface TimelineEventItemProps {
  event: TimelineEvent;
  isActive: boolean;
  onClick: () => void;
}

const TimelineEventItem: React.FC<TimelineEventItemProps> = ({ event, isActive, onClick }) => {
  const time = new Date(event.timestamp);
  const formattedTime = time.toLocaleTimeString('en-US', { hour12: false });

  return (
    <div
      onClick={onClick}
      className={`p-3 border-b cursor-pointer transition-colors ${
        isActive ? 'bg-blue-50 border-l-4 border-l-blue-500' : 'hover:bg-gray-50'
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="text-xs font-mono text-gray-500">{formattedTime}</span>
          <EventTypeBadge type={event.type} />
        </div>
        {event.stepIndex !== undefined && (
          <span className="text-xs text-gray-400">Step {event.stepIndex}</span>
        )}
      </div>
      <p className="text-sm mt-1 text-gray-700">{event.details || `Event: ${event.type}`}</p>
      {event.testName && (
        <p className="text-xs text-gray-500 mt-1">{event.testName}</p>
      )}
    </div>
  );
};

// Playback Controls
interface PlaybackControlsProps {
  isPlaying: boolean;
  playbackSpeed: number;
  currentPosition: number;
  totalDuration: number;
  onPlay: () => void;
  onPause: () => void;
  onSeek: (position: number) => void;
  onSpeedChange: (speed: number) => void;
}

const PlaybackControls: React.FC<PlaybackControlsProps> = ({
  isPlaying,
  playbackSpeed,
  currentPosition,
  totalDuration,
  onPlay,
  onPause,
  onSeek,
  onSpeedChange,
}) => {
  const speedOptions = [0.5, 1, 1.5, 2, 4];

  const formatTime = (ms: number) => {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  return (
    <div className="bg-white rounded-lg border p-4">
      {/* Progress Bar */}
      <div className="mb-4">
        <input
          type="range"
          min="0"
          max={totalDuration}
          value={currentPosition}
          onChange={(e) => onSeek(Number(e.target.value))}
          className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
          style={{
            background: `linear-gradient(to right, #3b82f6 0%, #3b82f6 ${(currentPosition / totalDuration) * 100}%, #e5e7eb ${(currentPosition / totalDuration) * 100}%, #e5e7eb 100%)`,
          }}
        />
        <div className="flex justify-between text-xs text-gray-500 mt-1">
          <span>{formatTime(currentPosition)}</span>
          <span>{formatTime(totalDuration)}</span>
        </div>
      </div>

      {/* Control Buttons */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            onClick={() => onSeek(Math.max(0, currentPosition - 5000))}
            className="p-2 rounded-lg hover:bg-gray-100"
            title="Rewind 5s"
          >
            <Rewind size={20} />
          </button>
          <button
            onClick={() => onSeek(Math.max(0, currentPosition - 1000))}
            className="p-2 rounded-lg hover:bg-gray-100"
            title="Step back"
          >
            <SkipBack size={20} />
          </button>
          <button
            onClick={isPlaying ? onPause : onPlay}
            className="p-3 rounded-full bg-blue-600 text-white hover:bg-blue-700"
            title={isPlaying ? 'Pause' : 'Play'}
          >
            {isPlaying ? <Pause size={24} /> : <Play size={24} />}
          </button>
          <button
            onClick={() => onSeek(Math.min(totalDuration, currentPosition + 1000))}
            className="p-2 rounded-lg hover:bg-gray-100"
            title="Step forward"
          >
            <SkipForward size={20} />
          </button>
          <button
            onClick={() => onSeek(Math.min(totalDuration, currentPosition + 5000))}
            className="p-2 rounded-lg hover:bg-gray-100"
            title="Forward 5s"
          >
            <FastForward size={20} />
          </button>
        </div>

        {/* Speed Control */}
        <div className="flex items-center gap-2">
          <span className="text-sm text-gray-500">Speed:</span>
          <select
            value={playbackSpeed}
            onChange={(e) => onSpeedChange(Number(e.target.value))}
            className="px-3 py-1 border rounded-lg text-sm"
          >
            {speedOptions.map((speed) => (
              <option key={speed} value={speed}>{speed}x</option>
            ))}
          </select>
        </div>
      </div>
    </div>
  );
};

// Main Component
const TimelinePage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedExecution, setSelectedExecution] = useState<string | null>(null);
  const [currentSession, setCurrentSession] = useState<PlaybackSession | null>(null);
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [activeEvent, setActiveEvent] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const [playbackPosition, setPlaybackPosition] = useState(0);
  const [filterType, setFilterType] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showSnapshots, setShowSnapshots] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const playbackRef = useRef<NodeJS.Timeout | null>(null);

  // Fetch recent executions
  const { data: executions = [], isLoading: executionsLoading } = useQuery({
    queryKey: ['timeline-executions'],
    queryFn: async () => {
      const response = await apiClient.get('/api/test-runs', {
        params: { limit: 50, sort: 'startedAt_desc' },
      });
      return response.data;
    },
  });

  // Fetch timeline summary for selected execution
  const { data: timelineSummary } = useQuery({
    queryKey: ['timeline-summary', selectedExecution],
    queryFn: async () => {
      if (!selectedExecution) return null;
      const response = await apiClient.get(`/api/timeline/summary/${selectedExecution}`);
      return response.data as TimelineSummary;
    },
    enabled: !!selectedExecution,
  });

  // Fetch events for playback
  const { data: playbackData } = useQuery({
    queryKey: ['playback-data', currentSession?.sessionId],
    queryFn: async () => {
      if (!currentSession?.sessionId) return null;
      const response = await apiClient.get(`/api/timeline/playback/${currentSession.sessionId}/data`);
      return response.data;
    },
    enabled: !!currentSession?.sessionId,
  });

  // Fetch snapshots
  const { data: snapshots = [] } = useQuery({
    queryKey: ['timeline-snapshots', selectedExecution],
    queryFn: async () => {
      if (!selectedExecution) return [];
      const response = await apiClient.get('/api/timeline/snapshots', {
        params: { executionId: selectedExecution },
      });
      return response.data as Snapshot[];
    },
    enabled: !!selectedExecution && showSnapshots,
  });

  // Start session mutation
  const startSessionMutation = useMutation({
    mutationFn: async (executionId: string) => {
      const response = await apiClient.post(`/api/timeline/sessions?executionId=${executionId}`);
      return response.data as PlaybackSession;
    },
    onSuccess: (session) => {
      setCurrentSession(session);
      queryClient.invalidateQueries({ queryKey: ['playback-data'] });
    },
  });

  // Control mutations
  const controlMutation = useMutation({
    mutationFn: async ({ action, sessionId }: { action: string; sessionId: string }) => {
      const response = await apiClient.post(`/api/timeline/sessions/${sessionId}/${action}`);
      return response.data;
    },
  });

  // Snapshot mutation
  const createSnapshotMutation = useMutation({
    mutationFn: async (data: { name: string; description?: string }) => {
      const response = await apiClient.post('/api/timeline/snapshots', {
        executionId: selectedExecution,
        ...data,
      });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['timeline-snapshots'] });
    },
  });

  // Handle playback
  useEffect(() => {
    if (isPlaying && playbackData?.events?.length) {
      const interval = 100 / playbackSpeed;
      playbackRef.current = setInterval(() => {
        setPlaybackPosition((prev) => {
          const next = prev + 100;
          if (next >= (timelineSummary?.duration || 0)) {
            setIsPlaying(false);
            return timelineSummary?.duration || 0;
          }
          return next;
        });

        // Update active event
        const currentEvents = playbackData.events as TimelineEvent[];
        const elapsed = playbackPosition;
        const active = currentEvents.find((e: TimelineEvent) => {
          const eventTime = new Date(e.timestamp).getTime();
          return eventTime <= elapsed;
        });
        if (active) setActiveEvent(active.id);
      }, interval);
    }

    return () => {
      if (playbackRef.current) {
        clearInterval(playbackRef.current);
      }
    };
  }, [isPlaying, playbackSpeed, playbackData]);

  // Handlers
  const handleStartPlayback = (executionId: string) => {
    setSelectedExecution(executionId);
    startSessionMutation.mutate(executionId);
  };

  const handlePlay = () => {
    if (currentSession) {
      controlMutation.mutate({ action: 'play', sessionId: currentSession.sessionId });
      setIsPlaying(true);
    }
  };

  const handlePause = () => {
    if (currentSession) {
      controlMutation.mutate({ action: 'pause', sessionId: currentSession.sessionId });
      setIsPlaying(false);
    }
  };

  const handleSeek = (position: number) => {
    if (currentSession) {
      apiClient.post(`/api/timeline/sessions/${currentSession.sessionId}/seek`, { positionMs: position });
      setPlaybackPosition(position);
    }
  };

  const handleSpeedChange = (speed: number) => {
    if (currentSession) {
      apiClient.put(`/api/timeline/sessions/${currentSession.sessionId}/speed`, { speed });
      setPlaybackSpeed(speed);
    }
  };

  const handleCreateSnapshot = (name: string, description?: string) => {
    createSnapshotMutation.mutate({ name, description });
  };

  // Filter events
  const filteredEvents = events.filter((event) => {
    if (filterType !== 'ALL' && event.type !== filterType) return false;
    if (searchQuery && !event.details?.toLowerCase().includes(searchQuery.toLowerCase())) {
      return false;
    }
    return true;
  });

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Timeline & Replay</h1>
            <p className="text-sm text-gray-500 mt-1">
              Visualize and replay test execution events
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setShowSnapshots(!showSnapshots)}
              className={`btn ${showSnapshots ? 'btn-primary' : 'btn-secondary'}`}
            >
              <Layers size={16} className="mr-1" />
              Snapshots
            </button>
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="btn btn-secondary"
            >
              <Settings size={16} className="mr-1" />
              Settings
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <StatsCard
            title="Total Executions"
            value={executions.length}
            subtitle="last 50"
            icon={<Activity size={20} />}
            color="blue"
          />
          <StatsCard
            title="Total Events"
            value={timelineSummary?.totalEvents || 0}
            subtitle="in selected"
            icon={<Layers size={20} />}
            color="purple"
          />
          <StatsCard
            title="Duration"
            value={timelineSummary ? `${Math.round(timelineSummary.duration / 1000)}s` : '0s'}
            subtitle="test run"
            icon={<Clock size={20} />}
            color="yellow"
          />
          <StatsCard
            title="Pass Rate"
            value={timelineSummary ? `${Math.round((timelineSummary.passCount / (timelineSummary.passCount + timelineSummary.failCount)) * 100)}%` : 'N/A'}
            subtitle="current session"
            icon={<Activity size={20} />}
            color="green"
          />
        </div>

        {/* Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Execution List */}
          <div className="lg:col-span-1 bg-white rounded-lg border">
            <div className="p-4 border-b">
              <h2 className="font-semibold text-gray-900">Recent Executions</h2>
            </div>
            <div className="max-h-[500px] overflow-y-auto">
              {executionsLoading ? (
                <div className="p-4 text-center text-gray-500">Loading...</div>
              ) : executions.length === 0 ? (
                <div className="p-4 text-center text-gray-500">No executions found</div>
              ) : (
                executions.map((execution: any) => (
                  <div
                    key={execution.id}
                    onClick={() => setSelectedExecution(execution.id)}
                    className={`p-4 border-b cursor-pointer hover:bg-gray-50 ${
                      selectedExecution === execution.id ? 'bg-blue-50' : ''
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-medium text-sm">{execution.name || execution.testId}</span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleStartPlayback(execution.id);
                        }}
                        className="p-1 rounded hover:bg-blue-100"
                        title="Start replay"
                      >
                        <Play size={16} className="text-blue-600" />
                      </button>
                    </div>
                    <p className="text-xs text-gray-500 mt-1">
                      {execution.startedAt ? new Date(execution.startedAt).toLocaleString() : 'N/A'}
                    </p>
                    <div className="flex gap-2 mt-2">
                      {execution.status === 'PASSED' && (
                        <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">Passed</span>
                      )}
                      {execution.status === 'FAILED' && (
                        <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded">Failed</span>
                      )}
                      {execution.status === 'IN_PROGRESS' && (
                        <span className="text-xs bg-yellow-100 text-yellow-700 px-2 py-0.5 rounded">In Progress</span>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Playback Area */}
          <div className="lg:col-span-2 space-y-4">
            {currentSession ? (
              <>
                {/* Session Info */}
                <div className="bg-white rounded-lg border p-4">
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h3 className="font-semibold">{currentSession.name}</h3>
                      <p className="text-sm text-gray-500">
                        Session: {currentSession.sessionId.slice(0, 8)}...
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <span className="text-sm px-3 py-1 bg-gray-100 rounded-full">
                        {currentSession.eventCount} events
                      </span>
                      <span className={`text-sm px-3 py-1 rounded-full ${
                        currentSession.status === 'PLAYING' ? 'bg-green-100 text-green-700' :
                        currentSession.status === 'PAUSED' ? 'bg-yellow-100 text-yellow-700' :
                        'bg-gray-100 text-gray-700'
                      }`}>
                        {currentSession.status}
                      </span>
                    </div>
                  </div>

                  {/* Playback Controls */}
                  <PlaybackControls
                    isPlaying={isPlaying}
                    playbackSpeed={playbackSpeed}
                    currentPosition={playbackPosition}
                    totalDuration={timelineSummary?.duration || 0}
                    onPlay={handlePlay}
                    onPause={handlePause}
                    onSeek={handleSeek}
                    onSpeedChange={handleSpeedChange}
                  />
                </div>

                {/* Event Timeline */}
                <div className="bg-white rounded-lg border">
                  <div className="p-4 border-b flex items-center justify-between">
                    <h3 className="font-semibold">Event Timeline</h3>
                    <div className="flex gap-2">
                      <select
                        value={filterType}
                        onChange={(e) => setFilterType(e.target.value)}
                        className="px-3 py-1 border rounded text-sm"
                      >
                        <option value="ALL">All Events</option>
                        <option value="TEST_START">Test Start</option>
                        <option value="TEST_COMPLETE">Test Complete</option>
                        <option value="STEP_PASS">Step Pass</option>
                        <option value="STEP_FAIL">Step Fail</option>
                        <option value="DEFECT_LINKED">Defect Linked</option>
                      </select>
                      <div className="relative">
                        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                        <input
                          type="text"
                          placeholder="Search events..."
                          value={searchQuery}
                          onChange={(e) => setSearchQuery(e.target.value)}
                          className="pl-9 pr-3 py-1 border rounded text-sm w-40"
                        />
                      </div>
                    </div>
                  </div>
                  <div className="max-h-[400px] overflow-y-auto">
                    {filteredEvents.length === 0 ? (
                      <div className="p-4 text-center text-gray-500">
                        No events to display. Start playback to see events.
                      </div>
                    ) : (
                      filteredEvents.map((event) => (
                        <TimelineEventItem
                          key={event.id}
                          event={event}
                          isActive={activeEvent === event.id}
                          onClick={() => setActiveEvent(event.id)}
                        />
                      ))
                    )}
                  </div>
                </div>

                {/* Snapshots Panel */}
                {showSnapshots && (
                  <div className="bg-white rounded-lg border">
                    <div className="p-4 border-b flex items-center justify-between">
                      <h3 className="font-semibold">Snapshots</h3>
                      <button
                        onClick={() => {
                          const name = prompt('Enter snapshot name:');
                          if (name) handleCreateSnapshot(name);
                        }}
                        className="btn btn-sm btn-primary"
                      >
                        Create Snapshot
                      </button>
                    </div>
                    <div className="max-h-[300px] overflow-y-auto">
                      {snapshots.length === 0 ? (
                        <div className="p-4 text-center text-gray-500">
                          No snapshots available
                        </div>
                      ) : (
                        snapshots.map((snapshot) => (
                          <div
                            key={snapshot.id}
                            className="p-4 border-b hover:bg-gray-50"
                          >
                            <div className="flex items-center justify-between">
                              <div>
                                <p className="font-medium text-sm">{snapshot.name}</p>
                                <p className="text-xs text-gray-500">
                                  {snapshot.eventCount} events • {new Date(snapshot.createdAt).toLocaleString()}
                                </p>
                              </div>
                              <button className="btn btn-sm btn-secondary">
                                Restore
                              </button>
                            </div>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div className="bg-white rounded-lg border p-8 text-center">
                <Video size={48} className="mx-auto text-gray-300 mb-4" />
                <h3 className="text-lg font-semibold text-gray-900">No Active Session</h3>
                <p className="text-gray-500 mt-2">
                  Select an execution from the list and click play to start replay
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default TimelinePage;