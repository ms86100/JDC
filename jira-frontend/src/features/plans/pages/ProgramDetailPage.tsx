import { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useProgram, useUpdateProgram, useDeleteProgram } from '../hooks/usePlans';
import { usePlans, useCreatePlan } from '../hooks/usePlans';
import { planApi, CreatePlanRequest } from '../../../api/planApi';
import '../styles/plans.css';

export default function ProgramDetailPage() {
  const { programId } = useParams<{ programId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showCreatePlan, setShowCreatePlan] = useState(false);
  const [showEdit, setShowEdit] = useState(false);
  const [form, setForm] = useState<CreatePlanRequest>({ name: '', description: '' });

  const { data: program, isLoading: programLoading } = useProgram(programId || '');
  const { data: plans, isLoading: plansLoading } = usePlans();
  const createPlanMutation = useCreatePlan();
  const updateMutation = useUpdateProgram();
  const deleteProgramMutation = useDeleteProgram();

  const filteredPlans = plans?.filter(p => program?.planCount === undefined || program?.planCount > 0);

  const handleCreatePlan = (e: React.FormEvent) => {
    e.preventDefault();
    if (!programId) return;
    createPlanMutation.mutate(
      { ...form, ownerId: program?.ownerId },
      {
        onSuccess: (data) => {
          planApi.linkPlanToProgram(programId, data.data.id);
          setShowCreatePlan(false);
          setForm({ name: '', description: '' });
          queryClient.invalidateQueries({ queryKey: ['programs', programId] });
          navigate(`/plans/${data.data.id}`);
        },
      }
    );
  };

  const handleDelete = () => {
    if (!programId) return;
    if (confirm('Are you sure you want to delete this program?')) {
      deleteProgramMutation.mutate(programId, {
        onSuccess: () => navigate('/programs'),
      });
    }
  };

  if (programLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  if (!program) {
    return (
      <div className="ab-empty-state">
        <h3>Program not found</h3>
        <Link to="/programs" className="ab-btn ab-btn-primary">Back to Programs</Link>
      </div>
    );
  }

  return (
    <div className="ab-program-detail-page">
      <div className="ab-page-header">
        <div>
          <Link to="/programs" className="ab-breadcrumb">Programs</Link>
          <h1 className="ab-page-title">{program.name}</h1>
          <p className="ab-page-subtitle">{program.description || 'No description'}</p>
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <button className="ab-btn ab-btn-secondary" onClick={() => setShowEdit(true)}>
            Edit
          </button>
          <button className="ab-btn ab-btn-primary" onClick={() => setShowCreatePlan(true)}>
            <span className="ab-icon-plus"></span>
            Add Plan
          </button>
        </div>
      </div>

      <div className="ab-tabs">
        <div className="ab-tab ab-tab-active">Overview</div>
      </div>

      <div className="ab-content">
        <div className="ab-grid">
          <div className="ab-card">
            <h3 className="ab-card-title">Program Details</h3>
            <div className="ab-detail-grid">
              <div className="ab-detail-item">
                <span className="ab-detail-label">Access Type</span>
                <span className={`ab-badge ${program.accessType === 'OPEN' ? 'ab-badge-success' : 'ab-badge-warning'}`}>
                  {program.accessType}
                </span>
              </div>
              <div className="ab-detail-item">
                <span className="ab-detail-label">Status</span>
                <span className="ab-badge ab-badge-secondary">{program.isActive ? 'Active' : 'Inactive'}</span>
              </div>
              <div className="ab-detail-item">
                <span className="ab-detail-label">Plans</span>
                <span>{program.planCount}</span>
              </div>
              <div className="ab-detail-item">
                <span className="ab-detail-label">Created</span>
                <span>{new Date(program.createdAt).toLocaleDateString()}</span>
              </div>
            </div>
          </div>

          <div className="ab-card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 className="ab-card-title" style={{ marginBottom: 0 }}>Plans</h3>
              <button className="ab-btn ab-btn-sm ab-btn-primary" onClick={() => setShowCreatePlan(true)}>
                Create Plan
              </button>
            </div>
            {plansLoading ? (
              <div className="ab-loading"><div className="ab-spinner"></div></div>
            ) : filteredPlans && filteredPlans.length > 0 ? (
              <div className="ab-list">
                {filteredPlans.map((plan) => (
                  <Link to={`/plans/${plan.id}`} key={plan.id} className="ab-list-item">
                    <div>
                      <div className="ab-list-item-title">{plan.name}</div>
                      <div className="ab-text-sm ab-text-muted">{plan.description || 'No description'}</div>
                    </div>
                    <span className="ab-icon-chevron-right"></span>
                  </Link>
                ))}
              </div>
            ) : (
              <div className="ab-empty-state-sm">
                <p>No plans in this program yet</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {showCreatePlan && (
        <div className="ab-modal-overlay" onClick={() => setShowCreatePlan(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Create Plan</h2>
              <button className="ab-btn-icon" onClick={() => setShowCreatePlan(false)}>
                <span className="ab-icon-close"></span>
              </button>
            </div>
            <form onSubmit={handleCreatePlan}>
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Plan Name *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="Enter plan name"
                    required
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Description</label>
                  <textarea
                    className="ab-textarea"
                    value={form.description}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    placeholder="Describe your plan"
                    rows={4}
                  />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowCreatePlan(false)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={createPlanMutation.isPending}>
                  {createPlanMutation.isPending ? 'Creating...' : 'Create Plan'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showEdit && (
        <div className="ab-modal-overlay" onClick={() => setShowEdit(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Edit Program</h2>
              <button className="ab-btn-icon" onClick={() => setShowEdit(false)}>
                <span className="ab-icon-close"></span>
              </button>
            </div>
            <form onSubmit={(e) => {
              e.preventDefault();
              if (!programId) return;
              updateMutation.mutate(
                { id: programId, data: { name: program.name, description: program.description } },
                { onSuccess: () => {
                  setShowEdit(false);
                  queryClient.invalidateQueries({ queryKey: ['programs', programId] });
                }}
              );
            }}>
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Program Name</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={program.name}
                    onChange={(e) => {}}
                  />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowEdit(false)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary">
                  Save Changes
                </button>
                <button type="button" className="ab-btn ab-btn-danger" onClick={handleDelete}>
                  Delete Program
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
