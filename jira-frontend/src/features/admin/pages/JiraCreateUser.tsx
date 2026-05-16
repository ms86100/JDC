import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import JiraAdminLayout from '../components/JiraAdminLayout';
import { useCreateJiraUser } from '../hooks/useAdminApi';
import './JiraCreateUser.css';

export default function JiraCreateUser() {
  const navigate = useNavigate();
  const createUser = useCreateJiraUser();

  const [formData, setFormData] = useState({
    email: '',
    fullName: '',
    userName: '',
    password: '',
    sendNotification: false,
    createAnother: false,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const newValue = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;
    setFormData((prev) => ({ ...prev, [name]: newValue }));

    // Clear error when user starts typing
    if (errors[name]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[name];
        return next;
      });
    }
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Email validation
    if (!formData.email.trim()) {
      newErrors.email = 'Email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Invalid email format';
    }

    // Full name validation
    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Full name is required';
    }

    // Username validation
    if (!formData.userName.trim()) {
      newErrors.userName = 'Username is required';
    } else if (formData.userName.includes(' ')) {
      newErrors.userName = 'Username cannot contain spaces';
    } else if (formData.userName.length < 3) {
      newErrors.userName = 'Username must be at least 3 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    try {
      await createUser.mutateAsync({
        email: formData.email,
        fullName: formData.fullName,
        userName: formData.userName,
        password: formData.password || undefined,
        sendNotification: formData.sendNotification,
      });

      if (formData.createAnother) {
        // Reset form but keep email pattern for easy re-entry
        setFormData({
          email: '',
          fullName: '',
          userName: '',
          password: '',
          sendNotification: false,
          createAnother: false,
        });
        setErrors({});
      } else {
        // Navigate back to users list
        navigate('/admin/users');
      }
    } catch (error) {
      console.error('Failed to create user:', error);
      setErrors({ form: 'Failed to create user. Please try again.' });
    }
  };

  return (
    <JiraAdminLayout>
      <div className="create-user-page">
        {/* Breadcrumb */}
        <div className="create-user-breadcrumb">
          <Link to="/admin/users" className="breadcrumb-link">Users</Link>
          <span className="breadcrumb-sep">/</span>
          <span className="breadcrumb-current">Create new user</span>
        </div>

        {/* Form Card */}
        <div className="create-user-card">
          <h1 className="create-user-title">Create new user</h1>

          <form onSubmit={handleSubmit} className="create-user-form">
            {/* Email Address */}
            <div className="form-row">
              <label className="form-label form-label-required" htmlFor="email">
                Email address
              </label>
              <div className="form-input-wrapper">
                <input
                  type="email"
                  id="email"
                  name="email"
                  className={`form-input ${errors.email ? 'form-input-error' : ''}`}
                  placeholder="E.g. charlie@atlassian.com"
                  value={formData.email}
                  onChange={handleChange}
                  autoFocus
                />
                {errors.email && <span className="form-error">{errors.email}</span>}
              </div>
            </div>

            {/* Full Name */}
            <div className="form-row">
              <label className="form-label form-label-required" htmlFor="fullName">
                Full name
              </label>
              <div className="form-input-wrapper">
                <input
                  type="text"
                  id="fullName"
                  name="fullName"
                  className={`form-input ${errors.fullName ? 'form-input-error' : ''}`}
                  placeholder="User's full name"
                  value={formData.fullName}
                  onChange={handleChange}
                />
                {errors.fullName && <span className="form-error">{errors.fullName}</span>}
              </div>
            </div>

            {/* Username */}
            <div className="form-row">
              <label className="form-label form-label-required" htmlFor="userName">
                Username
              </label>
              <div className="form-input-wrapper">
                <input
                  type="text"
                  id="userName"
                  name="userName"
                  className={`form-input form-input-purple ${errors.userName ? 'form-input-error' : ''}`}
                  value={formData.userName}
                  onChange={handleChange}
                />
                {errors.userName && <span className="form-error">{errors.userName}</span>}
              </div>
            </div>

            {/* Password */}
            <div className="form-row">
              <label className="form-label" htmlFor="password">
                Password
              </label>
              <div className="form-input-wrapper">
                <input
                  type="password"
                  id="password"
                  name="password"
                  className="form-input form-input-purple"
                  value={formData.password}
                  onChange={handleChange}
                  autoComplete="new-password"
                />
                <span className="form-hint">
                  If you do not enter a password, one will be generated automatically.
                </span>
              </div>
            </div>

            {/* Send Notification */}
            <div className="form-row">
              <div className="form-label">&nbsp;</div>
              <div className="form-input-wrapper">
                <label className="form-checkbox">
                  <input
                    type="checkbox"
                    name="sendNotification"
                    checked={formData.sendNotification}
                    onChange={handleChange}
                  />
                  <span>Send notification email</span>
                </label>
                <span className="form-info-icon" title="Send welcome email to the new user">?</span>
              </div>
            </div>

            {/* Application Access */}
            <div className="form-row">
              <label className="form-label">Application access</label>
              <div className="form-input-wrapper">
                <div className="application-row">
                  <div className="application-icon">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                      <path d="M12 2L2 7L12 12L22 7L12 2Z" fill="#0052CC"/>
                      <path d="M2 17L12 22L22 17" stroke="#0052CC" strokeWidth="2"/>
                      <path d="M2 12L12 17L22 12" stroke="#0052CC" strokeWidth="2"/>
                    </svg>
                  </div>
                  <span className="application-name">Jira Software</span>
                </div>
              </div>
            </div>

            {/* Form Error */}
            {errors.form && (
              <div className="form-row">
                <div className="form-label">&nbsp;</div>
                <div className="form-input-wrapper">
                  <span className="form-error">{errors.form}</span>
                </div>
              </div>
            )}

            {/* Create Another */}
            <div className="form-row">
              <div className="form-label">&nbsp;</div>
              <div className="form-input-wrapper">
                <label className="form-checkbox">
                  <input
                    type="checkbox"
                    name="createAnother"
                    checked={formData.createAnother}
                    onChange={handleChange}
                  />
                  <span>Create another</span>
                </label>
              </div>
            </div>

            {/* Buttons */}
            <div className="form-row form-actions">
              <div className="form-label">&nbsp;</div>
              <div className="form-input-wrapper">
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={createUser.isPending}
                >
                  {createUser.isPending ? 'Creating...' : 'Create user'}
                </button>
                <Link to="/admin/users" className="btn-link">
                  Cancel
                </Link>
              </div>
            </div>
          </form>
        </div>
      </div>
    </JiraAdminLayout>
  );
}