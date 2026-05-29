import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function LoginPage() {
  const [mode, setMode]             = useState<'login' | 'register'>('login');
  const [loginEmail, setLoginEmail] = useState('');
  const [username, setUsername]     = useState('');
  const [email, setEmail]           = useState('');
  const [password, setPassword]     = useState('');
  const [error, setError]           = useState('');
  const [loading, setLoading]       = useState(false);

  const { login, register } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError('');
    if (mode === 'login' && !EMAIL_RE.test(loginEmail)) {
      setError('Please enter a valid email address.');
      return;
    }
    setLoading(true);
    try {
      const res = mode === 'login'
        ? await login({ email: loginEmail, password })
        : await register({ username, email, password });
      if (res.token) {
        void navigate('/dashboard');
      } else {
        setError('Authentication failed');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <div className="brand-mark">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="3 17 9 11 13 15 21 7" />
              <polyline points="14 7 21 7 21 14" />
            </svg>
          </div>
          <span className="brand-name">AssetFlow</span>
        </div>

        <h2>{mode === 'login' ? 'Welcome back' : 'Create account'}</h2>

        {error && <div className="login-error">{error}</div>}

        <form onSubmit={handleSubmit}>
          {mode === 'login' ? (
            <div className="login-field">
              <label>Email</label>
              <input
                type="email"
                value={loginEmail}
                onChange={e => setLoginEmail(e.target.value)}
                placeholder="you@example.com"
                required
                autoFocus
              />
            </div>
          ) : (
            <>
              <div className="login-field">
                <label>Username</label>
                <input
                  type="text"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="your username"
                  required
                  autoFocus
                />
              </div>
              <div className="login-field">
                <label>Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  required
                />
              </div>
            </>
          )}
          <div className="login-field">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>
          <button className="login-submit" type="submit" disabled={loading}>
            {loading ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>

        <div className="login-toggle">
          {mode === 'login' ? (
            <>No account?{' '}<button onClick={() => { setMode('register'); setError(''); setLoginEmail(''); }}>Register</button></>
          ) : (
            <>Already have an account?{' '}<button onClick={() => { setMode('login'); setError(''); setEmail(''); setUsername(''); }}>Sign in</button></>
          )}
        </div>
      </div>
    </div>
  );
}
