import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AuthStyles.css';

const PLATFORM_NAME = 'System & Avionics Platform';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ username: '', password: '' });
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    document.title = 'Systems';
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);
    try {
      await login(form.username, form.password);
      navigate('/projects');
    } catch (err: unknown) {
      const message =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(message || 'Invalid credentials. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="ab-auth-page">
      <div className="ab-auth-shell">
        <aside className="ab-auth-hero" aria-hidden="false">
          <div className="ab-auth-hero-inner">
            <div className="ab-auth-mark" aria-hidden="true">
              SA
            </div>
            <h1 className="ab-auth-hero-title">{PLATFORM_NAME}</h1>
            <p className="ab-auth-hero-tagline">
              Enterprise program and systems engineering — requirements, workflows, and
              traceability in one workspace.
            </p>
            <ul className="ab-auth-hero-features">
              <li>Integrated workflow and issue management</li>
              <li>Administration aligned with aerospace practices</li>
              <li>Secure access for distributed engineering teams</li>
            </ul>
          </div>
        </aside>

        <main className="ab-auth-panel">
          <header className="ab-auth-panel-header">
            <h2 className="ab-auth-panel-title">Sign in</h2>
            <p className="ab-auth-panel-subtitle">
              Enter your credentials to access {PLATFORM_NAME}.
            </p>
          </header>

          <form onSubmit={handleSubmit} className="ab-auth-form" noValidate>
            {error && <div className="ab-alert-error" role="alert">{error}</div>}

            <div className="ab-form-group">
              <label className="ab-label" htmlFor="login-username">
                Username or email
              </label>
              <input
                id="login-username"
                type="text"
                className="ab-input"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                placeholder="e.g. jane.engineer"
                required
                autoComplete="username"
              />
            </div>

            <div className="ab-form-group">
              <label className="ab-label" htmlFor="login-password">
                Password
              </label>
              <input
                id="login-password"
                type="password"
                className="ab-input"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                placeholder="Enter your password"
                required
                autoComplete="current-password"
              />
            </div>

            <div className="ab-auth-remember-row">
              <label className="ab-auth-remember-label" htmlFor="login-remember">
                <input
                  id="login-remember"
                  type="checkbox"
                  checked={remember}
                  onChange={(e) => setRemember(e.target.checked)}
                />
                <span>Remember me</span>
              </label>
              <a href="#" className="ab-link-forgot">
                Forgot password?
              </a>
            </div>

            <button type="submit" className="ab-auth-submit" disabled={isLoading}>
              {isLoading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p className="ab-auth-switch">
            Don&apos;t have an account? <Link to="/register">Create one</Link>
          </p>

          <footer className="ab-auth-powered">
            Powered by <strong>Airbus Digital</strong>
          </footer>
        </main>
      </div>
    </div>
  );
}
