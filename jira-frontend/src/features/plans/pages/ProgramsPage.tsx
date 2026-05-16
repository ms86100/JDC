import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { usePrograms, useCreateProgram, useDeleteProgram } from '../hooks/usePlans';
import { CreateProgramRequest } from '../../../api/planApi';
import '../styles/plans.css';

export default function ProgramsPage() {
  const navigate = useNavigate();
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<CreateProgramRequest>({ name: '', description: '' });

  const { data: programs, isLoading } = usePrograms();
  const createMutation = useCreateProgram();
  const deleteMutation = useDeleteProgram();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate(form, {
      onSuccess: () => {
        setShowCreate(false);
        setForm({ name: '', description: '' });
      },
      onError: (error: Error) => {
        alert(error.message || 'Failed to create program');
      },
    });
  };

  const handleDelete = (id: string) => {
    deleteMutation.mutate(id, {
      onError: (error: Error) => {
        alert(error.message || 'Failed to delete program');
      },
    });
  };

  return (
    <div className="ab-programs-page">
      <div className="ab-page-header">
        <div>
          <h1 className="ab-page-title">Programs</h1>
          <p className="ab-page-subtitle">Organize your plans into programs</p>
        </div>
        <button className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
          <span className="ab-icon-plus"></span>
          Create Program
        </button>
      </div>

      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
        </div>
      ) : programs && programs.length > 0 ? (
        <div className="ab-grid ab-grid-3">
          {programs.map((program) => (
            <div key={program.id} className="ab-card ab-program-card">
              <div className="ab-program-card-header">
                <div className="ab-program-icon">
                  {program.name.charAt(0).toUpperCase()}
                </div>
                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                  <span className={`ab-badge ${program.accessType === 'OPEN' ? 'ab-badge-success' : 'ab-badge-warning'}`}>
                    {program.accessType}
                  </span>
                </div>
              </div>
              <Link to={`/programs/${program.id}`}>
                <h3 className="ab-program-name">{program.name}</h3>
              </Link>
              <p className="ab-program-description">{program.description || 'No description'}</p>
              <div className="ab-program-meta">
                <span className="ab-text-sm ab-text-muted">
                  {program.planCount} {program.planCount === 1 ? 'plan' : 'plans'}
                </span>
              </div>
              <div className="ab-card-actions">
                <button className="ab-btn ab-btn-sm ab-btn-danger" onClick={() => handleDelete(program.id)}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="ab-card">
          <div className="ab-empty-state">
            <div className="ab-empty-state-icon">📊</div>
            <h3 className="ab-empty-state-title">No programs yet</h3>
            <p className="ab-empty-state-description">
              Create your first program to start organizing your plans and roadmaps.
            </p>
            <button className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
              Create Program
            </button>
          </div>
        </div>
      )}

      {showCreate && (
        <div className="ab-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Create Program</h2>
              <button className="ab-btn-icon" onClick={() => setShowCreate(false)}>
                <span className="ab-icon-close"></span>
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Program Name *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="Enter program name"
                    required
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Description</label>
                  <textarea
                    className="ab-textarea"
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    placeholder="Describe your program"
                    rows={4}
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Access Type</label>
                  <select
                    className="ab-select"
                    value={form.accessType || 'OPEN'}
                    onChange={(e) => setForm({ ...form, accessType: e.target.value as 'OPEN' | 'RESTRICTED' })}
                  >
                    <option value="OPEN">Open</option>
                    <option value="RESTRICTED">Restricted</option>
                  </select>
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowCreate(false)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Creating...' : 'Create Program'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
