import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AuthStyles.css';

const PLATFORM_NAME = 'System & Avionics Platform';

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
      const base = import.meta.env.VITE_API_GATEWAY_URL ?? '';
      const res = await fetch(`${base}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      });
      if (!res.ok) {
        const data = await res.json();
        throw new Error(data.message || 'Registration failed');
      }
      await login(form.username, form.password);
      navigate('/projects');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Registration failed. Please try again.';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="ab-auth-page">
      <div className="ab-auth-shell">
        <aside className="ab-auth-hero">
          <div className="ab-auth-hero-inner">
            <div className="ab-auth-mark" aria-hidden="true">
              SA
            </div>
            <h1 className="ab-auth-hero-title">{PLATFORM_NAME}</h1>
            <p className="ab-auth-hero-tagline">
              Create an account to collaborate on programs, manage work items, and stay
              aligned with your team.
            </p>
            <ul className="ab-auth-hero-features">
              <li>One account for projects and administration</li>
              <li>Role-based access when your org enables it</li>
              <li>Same sign-in for all platform modules</li>
            </ul>
          </div>
        </aside>

        <main className="ab-auth-panel">
          <header className="ab-auth-panel-header">
            <h2 className="ab-auth-panel-title">Create account</h2>
            <p className="ab-auth-panel-subtitle">
              Join {PLATFORM_NAME} to manage your programs and work.
            </p>
          </header>

          <form onSubmit={handleSubmit} className="ab-auth-form" noValidate>
            {error && <div className="ab-alert-error" role="alert">{error}</div>}

            <div className="ab-form-group">
              <label className="ab-label" htmlFor="register-username">
                Username
              </label>
              <input
                id="register-username"
                type="text"
                className="ab-input"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                placeholder="Choose a username"
                required
                minLength={3}
                autoComplete="username"
              />
            </div>

            <div className="ab-form-group">
              <label className="ab-label" htmlFor="register-email">
                Email
              </label>
              <input
                id="register-email"
                type="email"
                className="ab-input"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="you@company.com"
                required
                autoComplete="email"
              />
            </div>

            <div className="ab-form-group">
              <label className="ab-label" htmlFor="register-password">
                Password
              </label>
              <input
                id="register-password"
                type="password"
                className="ab-input"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="At least 8 characters"
                required
                minLength={8}
                autoComplete="new-password"
              />
            </div>

            <button type="submit" className="ab-auth-submit" disabled={isLoading}>
              {isLoading ? 'Creating account…' : 'Create account'}
            </button>
          </form>

          <p className="ab-auth-switch">
            Already have an account? <Link to="/login">Sign in</Link>
          </p>

          <footer className="ab-auth-powered">
            Powered by <strong>Airbus Digital</strong>
          </footer>
        </main>
      </div>
    </div>
  );
}
