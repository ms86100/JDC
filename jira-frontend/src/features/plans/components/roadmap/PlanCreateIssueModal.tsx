import { useCallback, useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { issueApi, CreateIssueRequest } from '../../../../api/issueApi';
import { planApi } from '../../../../api/planApi';
import { projectApi, ProjectResponse } from '../../../../api/projectApi';
import apiClient from '../../../../api/axiosClient';

interface IssueType {
  id: string;
  name: string;
}

interface PlanCreateIssueModalProps {
  planId: string;
  planName: string;
  defaultProjectId?: string;
  onClose: () => void;
  onSuccess: () => void;
}

export default function PlanCreateIssueModal({
  planId,
  planName,
  defaultProjectId,
  onClose,
  onSuccess,
}: PlanCreateIssueModalProps) {
  const [projectId, setProjectId] = useState(defaultProjectId || '');
  const [issueTypeId, setIssueTypeId] = useState('');
  const [issueTypeName, setIssueTypeName] = useState('Story');
  const [summary, setSummary] = useState('');
  const [epicName, setEpicName] = useState('');
  const [showExtraFields, setShowExtraFields] = useState(false);
  const [description, setDescription] = useState('');
  const [createAnother, setCreateAnother] = useState(false);

  const { data: projects = [] } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const res = await projectApi.getAll();
      return Array.isArray(res) ? res : (res as { data?: ProjectResponse[] }).data ?? [];
    },
  });

  const { data: issueTypes = [] } = useQuery<IssueType[]>({
    queryKey: ['issueTypesList'],
    queryFn: async () => {
      const res = await apiClient.get<IssueType[]>('/issue-types');
      return res.data ?? [];
    },
  });

  useEffect(() => {
    if (!projectId && projects.length) {
      setProjectId(defaultProjectId || projects[0].id);
    }
  }, [projects, projectId, defaultProjectId]);

  useEffect(() => {
    if (!issueTypeId && issueTypes.length) {
      const story = issueTypes.find((t) => t.name.toLowerCase() === 'story') ?? issueTypes[0];
      setIssueTypeId(story.id);
      setIssueTypeName(story.name);
    }
  }, [issueTypes, issueTypeId]);

  const createMutation = useMutation({
    mutationFn: async () => {
      const payload: CreateIssueRequest = {
        projectId,
        title: summary,
        description: description || undefined,
        issueTypeId,
      };
      const created = await issueApi.create(payload);
      const issue = created.data;
      const planType =
        issueTypeName.toUpperCase() === 'EPIC'
          ? 'EPIC'
          : issueTypeName.toUpperCase().includes('SUB')
            ? 'SUBTASK'
            : 'STORY';
      await planApi.addItemToBacklog(planId, {
        issueId: issue.id,
        issueType: planType,
        status: issue.status,
      });
      if (planType === 'EPIC' && epicName.trim()) {
        await planApi.updatePlanSettings(planId, {
          epicNames: { [issue.id]: epicName.trim() },
        });
      }
      return issue;
    },
  });

  const submit = useCallback(async () => {
    if (!summary.trim() || !projectId || !issueTypeId) return;
    await createMutation.mutateAsync();
    if (createAnother) {
      setSummary('');
      setEpicName('');
      setDescription('');
    } else {
      onSuccess();
      onClose();
    }
  }, [summary, projectId, issueTypeId, createMutation, createAnother, onSuccess, onClose]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.altKey && e.key.toLowerCase() === 's') {
        e.preventDefault();
        void submit();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [submit]);

  const isEpic = issueTypeName.toLowerCase() === 'epic';

  return (
    <div className="jdc-modal-overlay" onClick={onClose}>
      <div className="jdc-modal jdc-create-issue-modal" onClick={(e) => e.stopPropagation()}>
        <div className="jdc-modal-header">
          <h2>Create issue — {planName}</h2>
          <button type="button" className="jdc-btn" onClick={onClose} aria-label="Close">×</button>
        </div>
        <div className="jdc-modal-body">
          <div className="jdc-form-row">
            <label>Project *</label>
            <select value={projectId} onChange={(e) => setProjectId(e.target.value)} required>
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.key})
                </option>
              ))}
            </select>
          </div>
          <div className="jdc-form-row">
            <label>Issue type *</label>
            <select
              value={issueTypeId}
              onChange={(e) => {
                const t = issueTypes.find((x) => x.id === e.target.value);
                setIssueTypeId(e.target.value);
                if (t) setIssueTypeName(t.name);
              }}
            >
              {issueTypes.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </div>
          <div className="jdc-form-row">
            <label>Summary *</label>
            <input
              type="text"
              value={summary}
              onChange={(e) => setSummary(e.target.value)}
              placeholder="What needs to be done?"
              autoFocus
            />
          </div>
          {isEpic && (
            <div className="jdc-form-row">
              <label>Epic Name</label>
              <input
                type="text"
                value={epicName}
                onChange={(e) => setEpicName(e.target.value)}
                placeholder="Epic name (shown on boards)"
              />
            </div>
          )}
          {showExtraFields && (
            <div className="jdc-form-row">
              <label>Description</label>
              <textarea
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          )}
          <button
            type="button"
            className="jdc-link-btn"
            onClick={() => setShowExtraFields((v) => !v)}
          >
            {showExtraFields ? 'Hide' : 'Configure'} fields
          </button>
        </div>
        <div className="jdc-modal-footer">
          <label className="jdc-checkbox-inline">
            <input
              type="checkbox"
              checked={createAnother}
              onChange={(e) => setCreateAnother(e.target.checked)}
            />
            Create another
          </label>
          <span className="jdc-hint">Alt+s to submit</span>
          <button type="button" className="jdc-btn" onClick={onClose}>Cancel</button>
          <button
            type="button"
            className="jdc-btn jdc-btn-primary"
            disabled={createMutation.isPending || !summary.trim()}
            onClick={() => void submit()}
          >
            {createMutation.isPending ? 'Creating…' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  );
}
