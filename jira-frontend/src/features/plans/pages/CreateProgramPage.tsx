import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateProgram, usePlans } from '../hooks/usePlans';
import { asArray } from '../../../utils/apiList';
import '../styles/plans.css';

export default function CreateProgramPage() {
  const navigate = useNavigate();
  const createProgram = useCreateProgram();
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    accessType: 'OPEN' as 'OPEN' | 'RESTRICTED',
    linkedPlanIds: [] as string[],
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { data: plansRaw } = usePlans();
  const plans = asArray(plansRaw);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: '' }));
    }
  };

  const togglePlan = (planId: string) => {
    setFormData((prev) => ({
      ...prev,
      linkedPlanIds: prev.linkedPlanIds.includes(planId)
        ? prev.linkedPlanIds.filter((id) => id !== planId)
        : [...prev.linkedPlanIds, planId],
    }));
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};
    if (!formData.name.trim()) {
      newErrors.name = 'Program name is required';
    } else if (formData.name.length > 255) {
      newErrors.name = 'Name must not exceed 255 characters';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;

    try {
      const res = await createProgram.mutateAsync({
        name: formData.name,
        description: formData.description,
        accessType: formData.accessType,
        linkedPlanIds: formData.linkedPlanIds.length ? formData.linkedPlanIds : undefined,
      });
      const programId = res.data?.id;
      navigate(programId ? `/programs/${programId}` : '/programs');
    } catch (error) {
      console.error('Failed to create program:', error);
    }
  };

  return (
    <div className="create-program-page jdc-create-program-dc">
      <div className="create-page-container">
        <div className="create-page-header">
          <h1 className="create-page-title">Create a program.</h1>
          <p className="create-page-description">
            Group plans together in an aggregated view of all your related work, including status,
            progress, and schedule of the selected plans.
          </p>
        </div>

        <form className="create-form" onSubmit={handleSubmit}>
          <div className="form-section">
            <div className="form-group">
              <label className="form-label" htmlFor="name">
                Program Name <span className="required">*</span>
              </label>
              <input
                type="text"
                id="name"
                name="name"
                className={`form-input ${errors.name ? 'input-error' : ''}`}
                value={formData.name}
                onChange={handleChange}
                placeholder="Enter program name"
                maxLength={255}
                autoFocus
              />
              {errors.name && <span className="error-message">{errors.name}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="accessType">
                Privacy
              </label>
              <select
                id="accessType"
                name="accessType"
                className="form-select"
                value={formData.accessType}
                onChange={handleChange}
              >
                <option value="OPEN">No restrictions</option>
                <option value="RESTRICTED">Restricted</option>
              </select>
            </div>

            <div className="form-group jdc-connected-plans">
              <label className="form-label">Connected plans</label>
              <p className="form-hint">Select plans to include in this program (DC connected plans checkboxes).</p>
              <div className="jdc-plan-checkbox-list">
                {plans.map((plan) => (
                  <label key={plan.id} className="jdc-plan-checkbox">
                    <input
                      type="checkbox"
                      checked={formData.linkedPlanIds.includes(plan.id)}
                      onChange={() => togglePlan(plan.id)}
                    />
                    {plan.name}
                    <span className="jdc-muted">({plan.itemCount ?? 0} issues)</span>
                  </label>
                ))}
                {plans.length === 0 && (
                  <p className="jdc-muted">No plans available. Create a plan first.</p>
                )}
              </div>
            </div>
          </div>

          <div className="form-actions">
            <button type="button" className="btn-secondary" onClick={() => navigate('/programs')}>
              Cancel
            </button>
            <button
              type="submit"
              className="btn-primary"
              disabled={createProgram.isPending || !formData.name.trim()}
            >
              {createProgram.isPending ? 'Creating...' : 'Create'}
            </button>
          </div>
        </form>

        <div className="info-banner">
          <div className="info-banner-content">
            <h4 className="info-banner-title">Can&apos;t find the plan you&apos;re looking for?</h4>
            <p className="info-banner-text">You may not have permission to view some plans, or they may already be in another program.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
