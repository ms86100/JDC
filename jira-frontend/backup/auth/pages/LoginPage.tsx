import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AuthStyles.css';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      await login(form.username, form.password);
      navigate('/projects');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid credentials. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="ab-auth-page">
      <div className="ab-auth-container">
        <div className="ab-auth-header">
          <div className="ab-auth-logo">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
              <rect width="48" height="48" rx="8" fill="#0066FF"/>
              <path d="M12 36V12L24 24L36 12V36" stroke="white" strokeWidth="4" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <h1 className="ab-auth-title">Jira Platform</h1>
          <p className="ab-auth-subtitle">Enterprise Project Management powered by Airbus</p>
        </div>

        <div className="ab-card ab-auth-card">
          <form onSubmit={handleSubmit} className="ab-auth-form">
            {error && (
              <div className="ab-alert ab-alert-error">
                {error}
              </div>
            )}

            <div className="ab-form-group">
              <label className="ab-label">Username or Email</label>
              <input
                type="text"
                className="ab-input"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                placeholder="Enter your username"
                required
                autoComplete="username"
              />
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Password</label>
              <input
                type="password"
                className="ab-input"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Enter your password"
                required
                autoComplete="current-password"
              />
            </div>

            <div className="ab-form-group ab-checkbox-group">
              <label className="ab-checkbox-label">
                <input type="checkbox" className="ab-checkbox" />
                <span>Remember me</span>
              </label>
              <a href="#" className="ab-link-forgot">Forgot password?</a>
            </div>

            <button
              type="submit"
              className="ab-btn ab-btn-primary ab-btn-full"
              disabled={isLoading}
            >
              {isLoading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>
        </div>

        <div className="ab-auth-footer">
          <p>
            Don't have an account?{' '}
            <Link to="/register" className="ab-link">Create one</Link>
          </p>
        </div>

        <div className="ab-auth-brand">
          <span>Powered by Airbus Digital</span>
        </div>
      </div>
    </div>
  );
}