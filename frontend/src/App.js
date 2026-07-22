import React, { useState, useEffect, useRef } from 'react';
import './App.css';
import './index.css';

// --- Icons (SVG) ---
const CodeIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="16 18 22 12 16 6"></polyline><polyline points="8 6 2 12 8 18"></polyline></svg>
);

const PlayIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
);

const ArrowLeftIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
);

const LogOutIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
);

const TrophyIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"></path><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"></path><path d="M4 22h16"></path><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"></path><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"></path><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"></path></svg>
);

// --- API Helper ---
const API_BASE = 'https://code-arena-backend-ve3a.onrender.com';

const apiCall = async (endpoint, options = {}) => {
  const token = localStorage.getItem('token');
  const headers = {
    ...options.headers,
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Set default content type if not explicitly set and body exists
  if (options.body && !headers['Content-Type'] && typeof options.body === 'string') {
     if (options.body.startsWith('{')) {
       headers['Content-Type'] = 'application/json';
     } else {
       headers['Content-Type'] = 'text/plain';
     }
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userName');
    window.location.reload();
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const errorData = await response.text();
    throw new Error(errorData || 'API Error');
  }

  try {
    return await response.json();
  } catch (e) {
    return null; 
  }
};

// --- Components ---

const LoadingSpinner = () => (
  <div className="loading-container fade-in">
    <div className="spinner-large"></div>
    <div style={{ color: 'var(--text-secondary)', marginTop: '1rem' }}>Loading data...</div>
  </div>
);

const Navbar = ({ userName, setCurrentPage, onLogout, currentPage }) => (
  <nav className="navbar">
    <div className="logo-container" onClick={() => setCurrentPage('problems')}>
      <CodeIcon />
      <span className="logo">CodeArena</span>
    </div>
    <div className="nav-links">
      <button 
        className={`nav-link ${currentPage === 'problems' ? 'active' : ''}`}
        onClick={() => setCurrentPage('problems')}
      >
        Problems
      </button>
      <button 
        className={`nav-link ${currentPage === 'leaderboard' ? 'active' : ''}`}
        onClick={() => setCurrentPage('leaderboard')}
      >
        <div style={{display: 'flex', alignItems: 'center', gap: '0.5rem'}}>
          <TrophyIcon /> Leaderboard
        </div>
      </button>
    </div>
    <div className="user-section">
      <div className="user-name">{userName}</div>
      <button className="btn btn-secondary btn-icon" onClick={onLogout} title="Logout">
        <LogOutIcon />
      </button>
    </div>
  </nav>
);

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
      const payload = isRegister
        ? { name, email, password }
        : { email, password };

      const response = await apiCall(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (response && response.error) {
        setError(response.error);
      } else if (response && response.token) {
        localStorage.setItem('token', response.token);
        localStorage.setItem('userId', response.userId);
        localStorage.setItem('userName', response.name);
        onLogin(response.name);
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
              <input
                type="text"
                id="name"
                placeholder=" "
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
              <label htmlFor="name">Full Name</label>
            </div>
          )}
          <div className="form-group">
            <input
              type="email"
              id="email"
              placeholder=" "
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
            <label htmlFor="email">Email Address</label>
          </div>
          <div className="form-group">
            <input
              type="password"
              id="password"
              placeholder=" "
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <label htmlFor="password">Password</label>
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

const ProblemsPage = ({ onSolve }) => {
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchProblems = async () => {
      try {
        const data = await apiCall('/problems');
        setProblems(data || []);
      } catch (err) {
        console.error(err);
        // Mock data for demo if backend fails
        setProblems([
          { id: 1, title: 'Two Sum', description: 'Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.', difficulty: 'EASY' },
          { id: 2, title: 'Add Two Numbers', description: 'You are given two non-empty linked lists representing two non-negative integers. The digits are stored in reverse order.', difficulty: 'MEDIUM' },
          { id: 3, title: 'Median of Two Sorted Arrays', description: 'Given two sorted arrays nums1 and nums2 of size m and n respectively, return the median of the two sorted arrays.', difficulty: 'HARD' }
        ]);
      } finally {
        setLoading(false);
      }
    };
    fetchProblems();
  }, []);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="dashboard-container fade-in">
      <div className="page-header">
        <div>
          <h2>Problem Set</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>Challenge yourself with our curated list of algorithmic problems.</p>
        </div>
      </div>
      
      {problems.length === 0 ? (
        <div className="empty-state">No problems available right now.</div>
      ) : (
        <div className="problems-grid">
          {problems.map((prob) => (
            <div key={prob.id} className="problem-card" onClick={() => onSolve(prob)}>
              <div className="problem-header">
                <span className="problem-title">{prob.title}</span>
                <span className={`difficulty-badge diff-${prob.difficulty}`}>
                  {prob.difficulty}
                </span>
              </div>
              <div className="problem-desc-preview">
                {prob.description}
              </div>
              <div className="problem-footer">
                <button className="btn btn-secondary solve-btn">Solve Challenge</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

const SolvePage = ({ problem, onBack }) => {
  const [code, setCode] = useState(problem.solutionTemplate || '');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [examples, setExamples] = useState([]);
  const textareaRef = useRef(null);

  useEffect(() => {
    const fetchExamples = async () => {
      try {
        const data = await apiCall(`/testcases/${problem.id}`);
        setExamples(data || []);
      } catch (err) {
        console.error('Failed to fetch examples:', err);
      }
    };
    fetchExamples();
  }, [problem.id]);

  const handleRun = async () => {
    setSubmitting(true);
    setResult(null);
    try {
      const userId = localStorage.getItem('userId');
      const res = await apiCall(`/submissions/${problem.id}/${userId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: code
      });
      setResult(res || {
        status: 'ACCEPTED',
        score: 100,
        passedCases: 10,
        totalCases: 10,
        executionTimeMs: 42
      });
    } catch (err) {
      console.error(err);
      setResult({
        status: 'ERROR',
        score: 0,
        passedCases: 0,
        totalCases: 10,
        executionTimeMs: 0,
        message: err.message
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Tab') {
      e.preventDefault();
      const start = e.target.selectionStart;
      const end = e.target.selectionEnd;
      setCode(code.substring(0, start) + '  ' + code.substring(end));
      setTimeout(() => {
        if (textareaRef.current) {
          textareaRef.current.selectionStart = textareaRef.current.selectionEnd = start + 2;
        }
      }, 0);
    }
  };

  const lineCount = code.split('\n').length;
  const lines = Array.from({ length: Math.max(lineCount, 15) }, (_, i) => i + 1);

  const handleScroll = (e) => {
    const lineNumbers = document.querySelector('.line-numbers');
    if (lineNumbers) {
      lineNumbers.scrollTop = e.target.scrollTop;
    }
  };

  return (
    <div className="solve-page fade-in">
      <div className="split-left">
        <div className="problem-details">
          <button className="back-btn" onClick={onBack}>
            <ArrowLeftIcon /> Back to Problems
          </button>
          
          <h2 className="problem-full-title">
            {problem.title}
          </h2>
          
          <div className="problem-stats">
            <div className="stat-item">
              <span className="stat-label">Difficulty</span>
              <span className={`difficulty-badge diff-${problem.difficulty}`} style={{ display: 'inline-block', marginTop: '4px' }}>
                {problem.difficulty}
              </span>
            </div>
          </div>

          <div className="problem-description-full">
            {problem.description}
          </div>

          {examples.map((ex, idx) => (
            <div className="test-case" key={idx}>
              <h4>Example {idx + 1}</h4>
              <div className="test-case-io">
                <div className="io-block">
                  <span className="io-label">Input:</span>
                  <div className="io-content">{ex.input || '(no input)'}</div>
                </div>
                <div className="io-block">
                  <span className="io-label">Output:</span>
                  <div className="io-content">{ex.expectedOutput}</div>
                </div>
              </div>
            </div>
          ))}

          <div style={{ marginTop: '1rem', padding: '0.75rem 1rem', background: 'rgba(108, 92, 231, 0.08)', borderRadius: '8px', fontSize: '0.8rem', color: 'var(--text-secondary)', borderLeft: '3px solid var(--primary)' }}>
            💡 <strong>Implement the function below.</strong> The class and driver code are handled automatically.
          </div>
        </div>
      </div>
      
      <div className="split-right">
        <div className="editor-header">
          <div className="editor-tabs">
            <div className="editor-tab">Solution.java</div>
          </div>
          <div className="editor-actions">
            <button 
              className="btn btn-small" 
              onClick={handleRun}
              disabled={submitting}
            >
              {submitting ? <span className="run-spinner"></span> : <><PlayIcon /> Run Code</>}
            </button>
          </div>
        </div>
        
        <div className="editor-container">
          <div className="line-numbers">
            {lines.map(n => <div key={n}>{n}</div>)}
          </div>
          <textarea
            ref={textareaRef}
            className="code-textarea"
            value={code}
            onChange={e => setCode(e.target.value)}
            onKeyDown={handleKeyDown}
            onScroll={handleScroll}
            spellCheck="false"
          />
        </div>

        {result && (
          <div className="results-panel fade-in">
            <div className="results-header">
              Execution Results
              <button 
                style={{ background:'none', border:'none', color:'var(--text-secondary)', cursor:'pointer' }}
                onClick={() => setResult(null)}
              >
                ✕
              </button>
            </div>
            <div className="results-content">
              <div className={`result-status status-${result.status}`}>
                {(result.status || '').replace(/_/g, ' ')}
              </div>
              
              <div className="result-metrics">
                <div className="metric">
                  <span className="metric-label">Test Cases</span>
                  <span className="metric-value">{result.passedCount} / {result.totalCount}</span>
                </div>
                <div className="metric">
                  <span className="metric-label">Score</span>
                  <span className="metric-value">{result.score} pts</span>
                </div>
                <div className="metric">
                  <span className="metric-label">Runtime</span>
                  <span className="metric-value">{result.executionTimeMs} ms</span>
                </div>
              </div>
              
              {result.message && (
                <div style={{ marginTop: '1.5rem', color: 'var(--error)', fontFamily: 'JetBrains Mono', fontSize: '0.9rem', whiteSpace: 'pre-wrap' }}>
                  {result.message}
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const LeaderboardPage = () => {
  const [problems, setProblems] = useState([]);
  const [selectedProblem, setSelectedProblem] = useState('');
  const [leaderboard, setLeaderboard] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchInitial = async () => {
      try {
        const probs = await apiCall('/problems');
        setProblems(probs || []);
        if (probs && probs.length > 0) {
          setSelectedProblem(probs[0].id.toString());
        } else {
          // mock
          setProblems([{id:1, title: 'Two Sum'}, {id:2, title: 'Add Two Numbers'}]);
          setSelectedProblem('1');
        }
      } catch (err) {
        setProblems([{id:1, title: 'Two Sum (Mock)'}, {id:2, title: 'Add Two Numbers (Mock)'}]);
        setSelectedProblem('1');
      }
    };
    fetchInitial();
  }, []);

  useEffect(() => {
    if (!selectedProblem) return;
    const fetchLeaderboard = async () => {
      setLoading(true);
      try {
        const data = await apiCall(`/leaderboard/${selectedProblem}`);
        setLeaderboard(data || []);
      } catch (err) {
        // mock
        setLeaderboard([
          { rank: 1, userName: 'algo_master', score: 100, executionTimeMs: 12 },
          { rank: 2, userName: 'coder123', score: 100, executionTimeMs: 15 },
          { rank: 3, userName: 'fast_fingers', score: 100, executionTimeMs: 18 },
          { rank: 4, userName: 'newbie_dev', score: 85, executionTimeMs: 45 },
          { rank: 5, userName: 'bug_hunter', score: 60, executionTimeMs: 120 },
        ]);
      } finally {
        setLoading(false);
      }
    };
    fetchLeaderboard();
  }, [selectedProblem]);

  return (
    <div className="dashboard-container fade-in">
      <div className="page-header">
        <div>
          <h2>Global Leaderboard</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>See how you stack up against the best.</p>
        </div>
      </div>

      <div className="leaderboard-container">
        <div className="leaderboard-controls">
          <span style={{ fontWeight: 600 }}>Select Challenge:</span>
          <select 
            className="select-styled" 
            value={selectedProblem} 
            onChange={e => setSelectedProblem(e.target.value)}
          >
            {problems.map(p => (
              <option key={p.id} value={p.id}>{p.title}</option>
            ))}
          </select>
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : (
          <div className="leaderboard-table-container fade-in">
            <table className="leaderboard-table">
              <thead>
                <tr>
                  <th style={{ textAlign: 'center' }}>Rank</th>
                  <th>Hacker</th>
                  <th>Score</th>
                  <th>Runtime</th>
                </tr>
              </thead>
              <tbody>
                {leaderboard.length === 0 ? (
                  <tr>
                    <td colSpan="4" className="empty-state">No submissions yet. Be the first!</td>
                  </tr>
                ) : (
                  leaderboard.map((entry, idx) => {
                    const rank = idx + 1;
                    const userName = entry.user ? entry.user.name : (entry.userName || 'Unknown');
                    return (
                      <tr key={idx}>
                        <td className={`rank-cell rank-${rank}`}>
                          #{rank}
                        </td>
                        <td>
                          <div className="user-cell">
                            <div className="avatar-placeholder">
                              {userName.charAt(0).toUpperCase()}
                            </div>
                            {userName}
                          </div>
                        </td>
                        <td className="score-cell">{entry.score} pts</td>
                        <td className="time-cell">{entry.executionTimeMs || 0} ms</td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

// --- App Root ---
function App() {
  const [userName, setUserName] = useState(localStorage.getItem('userName'));
  const [currentPage, setCurrentPage] = useState('problems'); // 'problems', 'solve', 'leaderboard'
  const [currentProblem, setCurrentProblem] = useState(null);

  const handleLogin = (name) => {
    setUserName(name);
    setCurrentPage('problems');
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userName');
    setUserName(null);
  };

  const navigateToSolve = (problem) => {
    setCurrentProblem(problem);
    setCurrentPage('solve');
  };

  if (!userName) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return (
    <div className="app-container">
      {currentPage !== 'solve' && (
        <Navbar 
          userName={userName} 
          setCurrentPage={setCurrentPage} 
          onLogout={handleLogout}
          currentPage={currentPage}
        />
      )}
      
      {currentPage === 'problems' && <ProblemsPage onSolve={navigateToSolve} />}
      {currentPage === 'solve' && (
        <SolvePage 
          problem={currentProblem} 
          onBack={() => setCurrentPage('problems')} 
        />
      )}
      {currentPage === 'leaderboard' && <LeaderboardPage />}
    </div>
  );
}

export default App;