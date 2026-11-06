import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { apiCall } from '../api';
import { CheckIcon } from '../icons';
import { DIFFICULTIES, filterProblems } from '../utils';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';

const ProblemsPage = ({ onSolve }) => {
  const [problems, setProblems] = useState([]);
  const [stats, setStats] = useState({});          // problemId -> {submissions, acceptanceRate}
  const [progress, setProgress] = useState(null);  // solved / attempted ids of the logged-in user
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [query, setQuery] = useState('');
  const [difficulty, setDifficulty] = useState('ALL');
  const [status, setStatus] = useState('ALL');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [problemList, statList, progressInfo] = await Promise.all([
        apiCall('/problems'),
        apiCall('/problems/stats').catch(() => []),   // nice-to-have, never blocks the page
        apiCall('/me/progress').catch(() => null),
      ]);
      setProblems(problemList || []);
      setStats(Object.fromEntries((statList || []).map((s) => [s.problemId, s])));
      setProgress(progressInfo);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const visible = useMemo(
    () => filterProblems(problems, { query, difficulty, status }, progress),
    [problems, query, difficulty, status, progress]
  );

  if (loading) return <LoadingSpinner />;
  if (error) return <div className="dashboard-container"><ErrorState message={error} onRetry={load} /></div>;

  const solved = new Set((progress && progress.solvedProblemIds) || []);
  const attempted = new Set((progress && progress.attemptedProblemIds) || []);

  return (
    <div className="dashboard-container fade-in">
      <div className="page-header">
        <div>
          <h2>Problem Set</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
            Challenge yourself with our curated list of algorithmic problems.
          </p>
        </div>
        {progress && (
          <div className="progress-pill" title="Problems you have solved">
            <CheckIcon /> {progress.solvedCount} / {progress.totalProblems} solved
          </div>
        )}
      </div>

      <div className="filter-bar">
        <input
          className="search-input"
          type="search"
          placeholder="Search problems..."
          aria-label="Search problems"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <div className="chip-group" role="group" aria-label="Difficulty">
          {['ALL', ...DIFFICULTIES].map((d) => (
            <button
              key={d}
              className={`chip ${difficulty === d ? 'chip-active' : ''}`}
              onClick={() => setDifficulty(d)}
            >
              {d === 'ALL' ? 'All' : d.charAt(0) + d.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
        <select
          className="select-styled"
          aria-label="Status"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          disabled={!progress}
        >
          <option value="ALL">Any status</option>
          <option value="SOLVED">Solved</option>
          <option value="ATTEMPTED">Attempted</option>
          <option value="TODO">Not started</option>
        </select>
      </div>

      {problems.length === 0 ? (
        <div className="empty-state">No problems available right now.</div>
      ) : visible.length === 0 ? (
        <div className="empty-state">No problems match your filters.</div>
      ) : (
        <div className="problems-grid">
          {visible.map((prob) => {
            const s = stats[prob.id];
            return (
              <div key={prob.id} className="problem-card" onClick={() => onSolve(prob)}>
                <div className="problem-header">
                  <span className="problem-title">
                    {solved.has(prob.id) && <span className="solved-mark" title="Solved"><CheckIcon /></span>}
                    {prob.title}
                  </span>
                  <span className={`difficulty-badge diff-${prob.difficulty}`}>{prob.difficulty}</span>
                </div>
                <div className="problem-footer problem-footer-split">
                  <span className="problem-meta">
                    {s ? `${s.acceptanceRate}% accepted · ${s.submissions} submissions` : 'No submissions yet'}
                    {!solved.has(prob.id) && attempted.has(prob.id) && ' · attempted'}
                  </span>
                  <button className="btn btn-secondary solve-btn">Solve Challenge</button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default ProblemsPage;
