import React, { useCallback, useEffect, useState } from 'react';
import { apiCall } from '../api';
import { DIFFICULTIES, statusLabel, timeAgo } from '../utils';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';

const ProfilePage = () => {
  const [progress, setProgress] = useState(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      setProgress(await apiCall('/me/progress'));
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (error) return <div className="dashboard-container"><ErrorState message={error} onRetry={load} /></div>;
  if (!progress) return <LoadingSpinner />;

  return (
    <div className="dashboard-container fade-in">
      <div className="page-header">
        <div>
          <h2>{progress.name}</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>Your progress on CodeArena.</p>
        </div>
      </div>

      <div className="stat-cards">
        <div className="stat-card">
          <span className="stat-card-value">{progress.solvedCount}<small> / {progress.totalProblems}</small></span>
          <span className="stat-card-label">Problems solved</span>
        </div>
        <div className="stat-card">
          <span className="stat-card-value">{progress.totalSubmissions}</span>
          <span className="stat-card-label">Submissions</span>
        </div>
        <div className="stat-card">
          <span className="stat-card-value">{progress.acceptanceRate}%</span>
          <span className="stat-card-label">Acceptance rate</span>
        </div>
      </div>

      <h3 className="section-title">By difficulty</h3>
      <div className="difficulty-bars">
        {DIFFICULTIES.map((d) => {
          const total = progress.totalByDifficulty[d] || 0;
          const solved = progress.solvedByDifficulty[d] || 0;
          const pct = total === 0 ? 0 : Math.round((100 * solved) / total);
          return (
            <div className="difficulty-row" key={d}>
              <span className={`difficulty-badge diff-${d}`}>{d}</span>
              <div className="bar" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}
                   aria-label={`${d} solved`}>
                <div className={`bar-fill bar-${d}`} style={{ width: `${pct}%` }} />
              </div>
              <span className="difficulty-count">{solved} / {total}</span>
            </div>
          );
        })}
      </div>

      <h3 className="section-title">Recent submissions</h3>
      {progress.recent.length === 0 ? (
        <div className="empty-state">No submissions yet. Pick a problem and give it a try!</div>
      ) : (
        <div className="leaderboard-table-container">
          <table className="leaderboard-table">
            <thead>
              <tr><th>Problem</th><th>Result</th><th>Tests</th><th>Runtime</th><th>When</th></tr>
            </thead>
            <tbody>
              {progress.recent.map((s) => (
                <tr key={s.id}>
                  <td>{s.problemTitle}</td>
                  <td><span className={`history-status status-${s.status}`}>{statusLabel(s.status)}</span></td>
                  <td>{s.passedCount}/{s.totalCount}</td>
                  <td className="time-cell">{s.executionTimeMs} ms</td>
                  <td>{timeAgo(s.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default ProfilePage;
