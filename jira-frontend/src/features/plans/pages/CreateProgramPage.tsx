import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateProgram } from '../hooks/usePlans';
import '../styles/plans.css';

export default function CreateProgramPage() {
  const navigate = useNavigate();
  const createProgram = useCreateProgram();
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    accessType: 'OPEN' as 'OPEN' | 'RESTRICTED',
  });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
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
      await createProgram.mutateAsync(formData);
      navigate('/plans');
    } catch (error) {
      console.error('Failed to create program:', error);
    }
  };

  return (
    <div className="create-program-page">
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
              <div className="select-with-icon">
                <span className="select-icon">
                  {formData.accessType === 'OPEN' ? (
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <rect x="3" y="7" width="10" height="8" rx="1" stroke="currentColor" strokeWidth="1.5"/>
                      <path d="M5 7V5C5 3.34315 6.34315 2 8 2C9.65685 2 11 3.34315 11 5V7" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                    </svg>
                  ) : (
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                      <rect x="3" y="7" width="10" height="8" rx="1" stroke="currentColor" strokeWidth="1.5"/>
                      <path d="M5 7V5C5 3.34315 6.34315 2 8 2C9.65685 2 11 3.34315 11 5V7" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                      <circle cx="8" cy="11" r="1.5" fill="currentColor"/>
                    </svg>
                  )}
                </span>
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
            </div>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => navigate('/plans')}
            >
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
          <div className="info-banner-icon">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <circle cx="10" cy="10" r="9" stroke="#0066FF" strokeWidth="2"/>
              <line x1="10" y1="9" x2="10" y2="14" stroke="#0066FF" strokeWidth="2" strokeLinecap="round"/>
              <circle cx="10" cy="6.5" r="1" fill="#0066FF"/>
            </svg>
          </div>
          <div className="info-banner-content">
            <h4 className="info-banner-title">Can't find the plan you're looking for?</h4>
            <p className="info-banner-text">This could be for various reasons, including:</p>
            <ul className="info-banner-list">
              <li>You may not have the necessary permissions to view the plans in your roadmap.</li>
              <li>The plan you're looking for may be in another program.</li>
            </ul>
            <a href="https://docs.example.com/portfolio" target="_blank" rel="noopener noreferrer" className="info-banner-link">
              See the documentation for more details.
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}