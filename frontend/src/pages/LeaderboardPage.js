import React, { useCallback, useEffect, useState } from 'react';
import { apiCall } from '../api';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorState from '../components/ErrorState';

const LeaderboardPage = () => {
  const [problems, setProblems] = useState([]);
  const [selectedProblem, setSelectedProblem] = useState('');
  const [leaderboard, setLeaderboard] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadProblems = useCallback(async () => {
    try {
      const list = await apiCall('/problems');
      setProblems(list || []);
      if (list && list.length > 0) {
        setSelectedProblem((current) => current || String(list[0].id));
      } else {
        setLoading(false);
      }
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadProblems(); }, [loadProblems]);

  useEffect(() => {
    if (!selectedProblem) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError('');
      try {
        const data = await apiCall(`/leaderboard/${selectedProblem}`);
        if (!cancelled) setLeaderboard(data || []);
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [selectedProblem]);

  return (
    <div className="dashboard-container fade-in">
      <div className="page-header">
        <div>
          <h2>Leaderboard</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
            Best attempt per user: highest score first, then fastest run.
          </p>
        </div>
      </div>

      <div className="leaderboard-container">
        <div className="leaderboard-controls">
          <span style={{ fontWeight: 600 }}>Select Challenge:</span>
          <select className="select-styled" value={selectedProblem}
                  onChange={(e) => setSelectedProblem(e.target.value)}>
            {problems.map((p) => <option key={p.id} value={p.id}>{p.title}</option>)}
          </select>
        </div>

        {loading ? (
          <LoadingSpinner />
        ) : error ? (
          <ErrorState message={error} onRetry={loadProblems} />
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
                  <tr><td colSpan="4" className="empty-state">No submissions yet. Be the first!</td></tr>
                ) : (
                  leaderboard.map((entry, idx) => {
                    const rank = idx + 1;
                    const userName = entry.userName || 'Unknown';
                    return (
                      <tr key={idx}>
                        <td className={`rank-cell rank-${rank}`}>#{rank}</td>
                        <td>
                          <div className="user-cell">
                            <div className="avatar-placeholder">{userName.charAt(0).toUpperCase()}</div>
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

export default LeaderboardPage;
