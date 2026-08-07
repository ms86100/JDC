import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import combinedApi, {
  ExploratorySessionRequest,
  ExploratorySessionResponse,
} from '../../../api/testApi';
import {
  Plus, X, Play, CheckCircle, XCircle, Clock, User, Calendar,
  ChevronDown, ChevronUp, Loader2, AlertCircle, Bug, Lightbulb, HelpCircle,
  FileText, Timer,
} from 'lucide-react';

// ─── Toast ─────────────────────────────────────────────────────────────────────

const Toast: React.FC<{ message: string; type: 'success' | 'error'; onClose: () => void }> = ({ message, type, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 3000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className={`fixed bottom-4 right-4 ${type === 'success' ? 'bg-green-500' : 'bg-red-500'} text-white px-4 py-3 rounded-lg shadow-lg flex items-center gap-2 z-50`}>
      {type === 'success' ? <CheckCircle className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}
      <span>{message}</span>
      <button onClick={onClose} className="ml-2 hover:opacity-80"><X className="w-4 h-4" /></button>
    </div>
  );
};

// ─── Status badge helper ───────────────────────────────────────────────────────

const statusColors: Record<string, string> = {
  NOT_STARTED: 'bg-gray-100 text-gray-700',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  COMPLETED: 'bg-green-100 text-green-700',
  ABANDONED: 'bg-red-100 text-red-700',
};

const StatusBadge: React.FC<{ status: string }> = ({ status }) => (
  <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${statusColors[status] || 'bg-gray-100 text-gray-700'}`}>
    {status.replace(/_/g, ' ')}
  </span>
);

// ─── Countdown Timer ───────────────────────────────────────────────────────────

const CountdownTimer: React.FC<{ startedAt: string; timeBoxMinutes: number }> = ({ startedAt, timeBoxMinutes }) => {
  const [remaining, setRemaining] = useState<number>(0);

  useEffect(() => {
    const calc = () => {
      const start = new Date(startedAt).getTime();
      const end = start + timeBoxMinutes * 60 * 1000;
      const now = Date.now();
      return Math.max(0, Math.floor((end - now) / 1000));
    };
    setRemaining(calc());
    const interval = setInterval(() => setRemaining(calc()), 1000);
    return () => clearInterval(interval);
  }, [startedAt, timeBoxMinutes]);

  const minutes = Math.floor(remaining / 60);
  const seconds = remaining % 60;
  const isLow = remaining < 300; // under 5 min

  return (
    <div className={`flex items-center gap-2 text-lg font-mono ${isLow ? 'text-red-600' : 'text-blue-600'}`}>
      <Timer className="w-5 h-5" />
      <span>{String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}</span>
      {remaining === 0 && <span className="text-sm font-sans text-red-500 ml-2">Time is up</span>}
    </div>
  );
};

// ─── List editor for Notes/Bugs/Ideas/Questions in active session ──────────

const ListSection: React.FC<{
  title: string;
  icon: React.ReactNode;
  items: string[];
  onAdd: (item: string) => void;
}> = ({ title, icon, items, onAdd }) => {
  const [value, setValue] = useState('');

  const handleAdd = () => {
    const trimmed = value.trim();
    if (trimmed) {
      onAdd(trimmed);
      setValue('');
    }
  };

  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-center gap-2 mb-3">
        {icon}
        <h4 className="font-medium text-gray-900">{title}</h4>
        <span className="text-xs text-gray-400 ml-auto">{items.length} item(s)</span>
      </div>
      <div className="flex gap-2 mb-3">
        <input
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
          className="flex-1 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
          placeholder={`Add ${title.toLowerCase().slice(0, -1)}...`}
        />
        <button
          onClick={handleAdd}
          disabled={!value.trim()}
          className="px-3 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded text-sm disabled:opacity-50"
        >
          <Plus className="w-4 h-4" />
        </button>
      </div>
      {items.length > 0 ? (
        <ul className="space-y-1 max-h-48 overflow-auto">
          {items.map((item, i) => (
            <li key={i} className="text-sm text-gray-700 py-1 px-2 bg-gray-50 rounded">{item}</li>
          ))}
        </ul>
      ) : (
        <p className="text-sm text-gray-400 italic">No {title.toLowerCase()} yet</p>
      )}
    </div>
  );
};

// ─── Active Session Detail ─────────────────────────────────────────────────────

const ActiveSessionDetail: React.FC<{
  session: ExploratorySessionResponse;
  onComplete: () => void;
  onAbandon: () => void;
  isActioning: boolean;
}> = ({ session, onComplete, onAbandon, isActioning }) => {
  const queryClient = useQueryClient();
  const [notes, setNotes] = useState(session.notes || '');
  const [bugs, setBugs] = useState<string[]>(session.bugs || []);
  const [ideas, setIdeas] = useState<string[]>(session.ideas || []);
  const [questions, setQuestions] = useState<string[]>(session.questions || []);

  const addBugMutation = useMutation({
    mutationFn: (bug: string) => combinedApi.addSessionBug(session.id, bug),
    onSuccess: (data) => {
      setBugs(data.bugs || []);
      queryClient.invalidateQueries({ queryKey: ['exploratory-sessions'] });
    },
  });

  const addNoteMutation = useMutation({
    mutationFn: (note: string) => combinedApi.addSessionNote(session.id, note),
    onSuccess: (data) => {
      setNotes(data.notes || '');
      queryClient.invalidateQueries({ queryKey: ['exploratory-sessions'] });
    },
  });

  const handleAddBug = (bug: string) => {
    setBugs(prev => [...prev, bug]);
    addBugMutation.mutate(bug);
  };

  const handleAddIdea = (idea: string) => {
    setIdeas(prev => [...prev, idea]);
  };

  const handleAddQuestion = (question: string) => {
    setQuestions(prev => [...prev, question]);
  };

  const handleAddNote = (note: string) => {
    const updatedNotes = notes ? `${notes}\n${note}` : note;
    setNotes(updatedNotes);
    addNoteMutation.mutate(note);
  };

  return (
    <div className="space-y-4">
      {/* Session header */}
      <div className="bg-white rounded-lg border p-4">
        <div className="flex items-center justify-between mb-3">
          <div>
            <h3 className="font-semibold text-lg">{session.charterGoal || session.charter || 'Untitled Session'}</h3>
            <p className="text-sm text-gray-500">{session.sessionType?.replace(/_/g, ' ')} session</p>
          </div>
          {session.startedAt && (
            <CountdownTimer startedAt={session.startedAt} timeBoxMinutes={session.timeBoxMinutes} />
          )}
        </div>
        <div className="flex gap-3">
          <button
            onClick={onComplete}
            disabled={isActioning}
            className="flex items-center gap-2 px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg text-sm disabled:opacity-50"
          >
            {isActioning ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle className="w-4 h-4" />}
            Complete Session
          </button>
          <button
            onClick={onAbandon}
            disabled={isActioning}
            className="flex items-center gap-2 px-4 py-2 border border-red-300 text-red-600 hover:bg-red-50 rounded-lg text-sm disabled:opacity-50"
          >
            <XCircle className="w-4 h-4" />
            Abandon
          </button>
        </div>
      </div>

      {/* Four sections */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <ListSection
          title="Notes"
          icon={<FileText className="w-4 h-4 text-blue-500" />}
          items={notes ? notes.split('\n').filter(Boolean) : []}
          onAdd={handleAddNote}
        />
        <ListSection
          title="Bugs"
          icon={<Bug className="w-4 h-4 text-red-500" />}
          items={bugs}
          onAdd={handleAddBug}
        />
        <ListSection
          title="Ideas"
          icon={<Lightbulb className="w-4 h-4 text-yellow-500" />}
          items={ideas}
          onAdd={handleAddIdea}
        />
        <ListSection
          title="Questions"
          icon={<HelpCircle className="w-4 h-4 text-purple-500" />}
          items={questions}
          onAdd={handleAddQuestion}
        />
      </div>
    </div>
  );
};

// ─── Create Session Modal ──────────────────────────────────────────────────────

interface CreateModalProps {
  open: boolean;
  isSaving: boolean;
  onSave: (data: ExploratorySessionRequest) => void;
  onClose: () => void;
  projectId: string;
}

const CreateSessionModal: React.FC<CreateModalProps> = ({ open, isSaving, onSave, onClose, projectId }) => {
  const [charterGoal, setCharterGoal] = useState('');
  const [charter, setCharter] = useState('');
  const [sessionType, setSessionType] = useState<'CHARTER_BASED' | 'SESSION_BASED' | 'FREESTYLE'>('CHARTER_BASED');
  const [timeBoxMinutes, setTimeBoxMinutes] = useState(60);
  const [environment, setEnvironment] = useState('');

  if (!open) return null;

  const handleSubmit = () => {
    if (!charterGoal.trim()) return;
    onSave({
      projectId,
      charterGoal: charterGoal.trim(),
      charter: charter.trim() || undefined,
      sessionType,
      timeBoxMinutes,
      environment: environment.trim() || undefined,
    });
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold">New Exploratory Session</h3>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600"><X className="w-5 h-5" /></button>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Charter Goal <span className="text-red-500">*</span></label>
              <input
                type="text"
                value={charterGoal}
                onChange={(e) => setCharterGoal(e.target.value)}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="What is the goal of this testing session?"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">Charter Description</label>
              <textarea
                value={charter}
                onChange={(e) => setCharter(e.target.value)}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={3}
                placeholder="Describe the scope and approach for this session"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Session Type</label>
              <div className="flex gap-4">
                {(['CHARTER_BASED', 'SESSION_BASED', 'FREESTYLE'] as const).map((type) => (
                  <label key={type} className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="sessionType"
                      value={type}
                      checked={sessionType === type}
                      onChange={() => setSessionType(type)}
                      className="w-4 h-4"
                    />
                    <span className="text-sm">{type.replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase())}</span>
                  </label>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">Time Box (minutes)</label>
              <input
                type="number"
                value={timeBoxMinutes}
                onChange={(e) => setTimeBoxMinutes(parseInt(e.target.value, 10) || 60)}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                min={5}
                max={480}
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">Environment</label>
              <input
                type="text"
                value={environment}
                onChange={(e) => setEnvironment(e.target.value)}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g. Staging, Production, Local"
              />
            </div>
          </div>

          <div className="flex justify-end gap-3 mt-6">
            <button onClick={onClose} className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">Cancel</button>
            <button
              onClick={handleSubmit}
              disabled={!charterGoal.trim() || isSaving}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg disabled:opacity-50"
            >
              {isSaving && <Loader2 className="w-4 h-4 animate-spin" />}
              {isSaving ? 'Creating...' : 'Create Session'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// ─── Session Card ──────────────────────────────────────────────────────────────

const SessionCard: React.FC<{
  session: ExploratorySessionResponse;
  isExpanded: boolean;
  onToggle: () => void;
  onStart: () => void;
  onComplete: () => void;
  onAbandon: () => void;
  isActioning: boolean;
}> = ({ session, isExpanded, onToggle, onStart, onComplete, onAbandon, isActioning }) => {
  return (
    <div className="bg-white rounded-lg border shadow-sm overflow-hidden">
      <div className="p-4 cursor-pointer hover:bg-gray-50" onClick={onToggle}>
        <div className="flex items-start justify-between">
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-3 mb-1">
              <h3 className="font-semibold text-gray-900 truncate">{session.charterGoal || session.charter || 'Untitled Session'}</h3>
              <StatusBadge status={session.status} />
            </div>
            <div className="flex items-center gap-4 text-sm text-gray-500">
              <span className="flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" />
                {session.timeBoxMinutes} min
              </span>
              <span className="flex items-center gap-1">
                <User className="w-3.5 h-3.5" />
                {session.testerId || 'Unassigned'}
              </span>
              <span className="flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5" />
                {new Date(session.createdAt).toLocaleDateString()}
              </span>
              <span className="text-xs bg-gray-100 px-2 py-0.5 rounded">
                {session.sessionType?.replace(/_/g, ' ')}
              </span>
            </div>
          </div>
          <div className="flex items-center gap-2 ml-4">
            {session.status === 'NOT_STARTED' && (
              <button
                onClick={(e) => { e.stopPropagation(); onStart(); }}
                disabled={isActioning}
                className="flex items-center gap-1 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded text-sm disabled:opacity-50"
              >
                {isActioning ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Play className="w-3.5 h-3.5" />}
                Start
              </button>
            )}
            {session.status === 'IN_PROGRESS' && (
              <>
                <button
                  onClick={(e) => { e.stopPropagation(); onComplete(); }}
                  disabled={isActioning}
                  className="flex items-center gap-1 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white rounded text-sm disabled:opacity-50"
                >
                  <CheckCircle className="w-3.5 h-3.5" />
                  Complete
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); onAbandon(); }}
                  disabled={isActioning}
                  className="flex items-center gap-1 px-3 py-1.5 border border-red-300 text-red-600 hover:bg-red-50 rounded text-sm disabled:opacity-50"
                >
                  <XCircle className="w-3.5 h-3.5" />
                  Abandon
                </button>
              </>
            )}
            {isExpanded ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
          </div>
        </div>
      </div>

      {/* Expanded detail view */}
      {isExpanded && session.status === 'IN_PROGRESS' && (
        <div className="border-t p-4 bg-gray-50">
          <ActiveSessionDetail
            session={session}
            onComplete={onComplete}
            onAbandon={onAbandon}
            isActioning={isActioning}
          />
        </div>
      )}

      {isExpanded && session.status !== 'IN_PROGRESS' && (
        <div className="border-t p-4 bg-gray-50">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div>
              <span className="text-gray-500 block">Duration</span>
              <span className="font-medium">{session.actualDurationMinutes ? `${session.actualDurationMinutes} min` : '-'}</span>
            </div>
            <div>
              <span className="text-gray-500 block">Bugs Found</span>
              <span className="font-medium">{session.bugs?.length || 0}</span>
            </div>
            <div>
              <span className="text-gray-500 block">Environment</span>
              <span className="font-medium">{session.environment || '-'}</span>
            </div>
            <div>
              <span className="text-gray-500 block">Defects</span>
              <span className="font-medium">{session.defectKeys?.length || 0}</span>
            </div>
          </div>
          {session.notes && (
            <div className="mt-3">
              <span className="text-gray-500 text-sm block mb-1">Notes</span>
              <p className="text-sm text-gray-700 whitespace-pre-wrap bg-white rounded p-2 border">{session.notes}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// ─── Main Page ─────────────────────────────────────────────────────────────────

export const ExploratoryTestingPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [actioningId, setActioningId] = useState<string | null>(null);

  const { data: sessions = [], isLoading } = useQuery({
    queryKey: ['exploratory-sessions', projectId],
    queryFn: () => combinedApi.getExploratorySessions(projectId!),
    enabled: !!projectId,
  });

  const createMutation = useMutation({
    mutationFn: (data: ExploratorySessionRequest) => combinedApi.createExploratorySession(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['exploratory-sessions', projectId] });
      setToast({ message: 'Session created successfully', type: 'success' });
      setShowCreate(false);
    },
    onError: () => setToast({ message: 'Failed to create session', type: 'error' }),
  });

  const startMutation = useMutation({
    mutationFn: (id: string) => combinedApi.startExploratorySession(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['exploratory-sessions', projectId] });
      setToast({ message: 'Session started', type: 'success' });
      setExpandedId(id);
      setActioningId(null);
    },
    onError: () => {
      setToast({ message: 'Failed to start session', type: 'error' });
      setActioningId(null);
    },
  });

  const completeMutation = useMutation({
    mutationFn: (id: string) => combinedApi.completeExploratorySession(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['exploratory-sessions', projectId] });
      setToast({ message: 'Session completed', type: 'success' });
      setActioningId(null);
    },
    onError: () => {
      setToast({ message: 'Failed to complete session', type: 'error' });
      setActioningId(null);
    },
  });

  const abandonMutation = useMutation({
    mutationFn: (id: string) => combinedApi.abandonExploratorySession(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['exploratory-sessions', projectId] });
      setToast({ message: 'Session abandoned', type: 'success' });
      setActioningId(null);
    },
    onError: () => {
      setToast({ message: 'Failed to abandon session', type: 'error' });
      setActioningId(null);
    },
  });

  const handleStart = (id: string) => {
    setActioningId(id);
    startMutation.mutate(id);
  };

  const handleComplete = (id: string) => {
    setActioningId(id);
    completeMutation.mutate(id);
  };

  const handleAbandon = (id: string) => {
    setActioningId(id);
    abandonMutation.mutate(id);
  };

  if (!projectId) {
    return (
      <div className="flex flex-col items-center justify-center h-64 text-center">
        <AlertCircle className="w-12 h-12 text-gray-400 mb-4" />
        <h3 className="text-lg font-medium text-gray-900 mb-2">No project selected</h3>
        <p className="text-gray-500">Select a project to manage exploratory testing sessions</p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

      {/* Header */}
      <div className="bg-white px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Exploratory Testing</h1>
            <p className="text-sm text-gray-500 mt-1">Manage charter-based and freestyle testing sessions</p>
          </div>
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg"
          >
            <Plus className="w-4 h-4" /> New Session
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {isLoading ? (
          <div className="flex justify-center items-center py-12">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          </div>
        ) : sessions.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            <Clock className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <p className="text-lg font-medium">No sessions yet</p>
            <p className="text-sm mt-2">Create a new exploratory testing session to get started</p>
          </div>
        ) : (
          <div className="space-y-4">
            {sessions.map((session) => (
              <SessionCard
                key={session.id}
                session={session}
                isExpanded={expandedId === session.id}
                onToggle={() => setExpandedId(expandedId === session.id ? null : session.id)}
                onStart={() => handleStart(session.id)}
                onComplete={() => handleComplete(session.id)}
                onAbandon={() => handleAbandon(session.id)}
                isActioning={actioningId === session.id}
              />
            ))}
          </div>
        )}
      </div>

      {/* Create Modal */}
      <CreateSessionModal
        open={showCreate}
        isSaving={createMutation.isPending}
        onSave={(data) => createMutation.mutate(data)}
        onClose={() => setShowCreate(false)}
        projectId={projectId}
      />
    </div>
  );
};

export default ExploratoryTestingPage;
