import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AuthStyles.css';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ username: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      }).then(async (res) => {
        if (!res.ok) {
          const data = await res.json();
          throw new Error(data.message || 'Registration failed');
        }
        // Auto login after registration
        await login(form.username, form.password);
        navigate('/projects');
      });
    } catch (err: any) {
      setError(err.message || 'Registration failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="ab-auth-page">
      <div className="ab-auth-container">
        <div className="ab-auth-header">
          <div className="ab-logo-large">JP</div>
          <h1 className="ab-auth-title">Create Account</h1>
          <p className="ab-auth-subtitle">Join Jira Platform to manage your projects</p>
        </div>

        <div className="ab-card">
          <form onSubmit={handleSubmit} className="ab-auth-form">
            {error && (
              <div className="ab-alert ab-alert-error">
                {error}
              </div>
            )}

            <div className="ab-form-group">
              <label className="ab-label">Username</label>
              <input
                type="text"
                className="ab-input"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                placeholder="Choose a username"
                required
                minLength={3}
              />
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Email</label>
              <input
                type="email"
                className="ab-input"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="Enter your email"
                required
              />
            </div>

            <div className="ab-form-group">
              <label className="ab-label">Password</label>
              <input
                type="password"
                className="ab-input"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Create a password (min 8 characters)"
                required
                minLength={8}
              />
            </div>

            <button
              type="submit"
              className="ab-btn ab-btn-primary ab-btn-full"
              disabled={isLoading}
            >
              {isLoading ? 'Creating account...' : 'Create Account'}
            </button>
          </form>
        </div>

        <div className="ab-auth-footer">
          <p>
            Already have an account?{' '}
            <Link to="/login" className="ab-link">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}