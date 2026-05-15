import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import { issueApi, IssueType, CreateIssueRequest } from '../../../api/issueApi';

interface CreateIssueModalProps {
  onClose: () => void;
  onSuccess: () => void;
}

export default function CreateIssueModal({ onClose, onSuccess }: CreateIssueModalProps) {
  const [form, setForm] = useState<CreateIssueRequest>({
    projectId: '',
    title: '',
    description: '',
    issueTypeId: '',
    priorityId: '',
  });

  const { data: projects } = useQuery<ProjectResponse[]>({
    queryKey: ['projects'],
    queryFn: async () => {
      const response = await projectApi.getAll();
      return response.data;
    },
  });

  const { data: types } = useQuery<IssueType[]>({
    queryKey: ['issueTypes'],
    queryFn: async () => {
      const response = await issueApi.getTypes();
      return response.data;
    },
  });

  const { data: priorities } = useQuery({
    queryKey: ['priorities'],
    queryFn: async () => {
      const response = await issueApi.getPriorities();
      return response.data;
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateIssueRequest) => issueApi.create(data),
    onSuccess: () => {
      onSuccess();
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.projectId || !form.title || !form.issueTypeId) {
      alert('Please fill in required fields');
      return;
    }
    createMutation.mutate(form);
  };

  return (
    <div className="ab-modal-overlay" onClick={onClose}>
      <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ab-modal-header">
          <h2 className="ab-modal-title">Create Issue</h2>
          <button className="ab-btn-icon" onClick={onClose}>
            <span className="ab-icon-close">✕</span>
          </button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="ab-modal-body">
            <div className="ab-form-group">
              <label className="ab-label">Project *</label>
              <select
                className="ab-select"
                value={form.projectId}
                onChange={(e) => setForm({ ...form, projectId: e.target.value })}
                required
              >
                <option value="">Select Project</option>
                {projects?.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name} ({project.projectKey})
                  </option>
                ))}
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Issue Type *</label>
              <select
                className="ab-select"
                value={form.issueTypeId}
                onChange={(e) => setForm({ ...form, issueTypeId: e.target.value })}
                required
              >
                <option value="">Select Type</option>
                {types?.map((type) => (
                  <option key={type.id} value={type.id}>
                    {type.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Summary *</label>
              <input
                type="text"
                className="ab-input"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="Brief description of the issue"
                required
              />
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Description</label>
              <textarea
                className="ab-textarea"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Detailed description of the issue"
                rows={5}
              />
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Priority</label>
              <select
                className="ab-select"
                value={form.priorityId || ''}
                onChange={(e) => setForm({ ...form, priorityId: e.target.value || undefined })}
              >
                <option value="">None</option>
                {priorities?.map((priority: any) => (
                  <option key={priority.id} value={priority.id}>
                    {priority.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="ab-modal-footer">
            <button type="button" className="ab-btn ab-btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button
              type="submit"
              className="ab-btn ab-btn-primary"
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? 'Creating...' : 'Create Issue'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}