import React, { useState } from 'react';
import { apiCall, saveSession } from '../api';
import { CodeIcon } from '../icons';

const LoginPage = ({ onLogin }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [isRegister, setIsRegister] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const endpoint = isRegister ? '/auth/register' : '/auth/login';
      const payload = isRegister ? { name, email, password } : { email, password };

      const response = await apiCall(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (response && response.token) {
        saveSession(response);
        onLogin({ userName: response.name, role: response.role || 'CANDIDATE' });
      } else {
        setError('Unexpected response from server');
      }
    } catch (err) {
      console.error(err);
      setError(err.message || 'Something went wrong. Is the backend running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card fade-in">
        <div className="login-header">
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem', color: 'var(--primary-glow)' }}>
            <CodeIcon />
          </div>
          <h1>CodeArena</h1>
          <p>{isRegister ? 'Create your account' : 'Sign in to continue your coding journey'}</p>
        </div>
        {error && <div className="error-message">{error}</div>}
        <form onSubmit={handleSubmit}>
          {isRegister && (
            <div className="form-group">
              <input type="text" id="name" placeholder=" " value={name}
                     onChange={(e) => setName(e.target.value)} required maxLength={50} />
              <label htmlFor="name">Full Name</label>
            </div>
          )}
          <div className="form-group">
            <input type="email" id="email" placeholder=" " value={email}
                   onChange={(e) => setEmail(e.target.value)} required />
            <label htmlFor="email">Email Address</label>
          </div>
          <div className="form-group">
            <input type="password" id="password" placeholder=" " value={password}
                   onChange={(e) => setPassword(e.target.value)} required
                   minLength={isRegister ? 8 : undefined} />
            <label htmlFor="password">{isRegister ? 'Password (min. 8 characters)' : 'Password'}</label>
          </div>
          <button type="submit" className="btn login-btn" disabled={loading}>
            {loading ? <span className="run-spinner"></span> : (isRegister ? 'Create Account' : 'Access Platform')}
          </button>
        </form>
        <div style={{ textAlign: 'center', marginTop: '1.5rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
          <span
            style={{ color: 'var(--primary-glow)', cursor: 'pointer', fontWeight: 600 }}
            onClick={() => { setIsRegister(!isRegister); setError(''); }}
          >
            {isRegister ? 'Sign In' : 'Sign Up'}
          </span>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
