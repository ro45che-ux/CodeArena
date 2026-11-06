import React, { useCallback, useEffect, useState } from 'react';
import { apiCall } from '../api';
import { statusLabel, timeAgo } from '../utils';
import ErrorState from '../components/ErrorState';

/** The logged-in user's earlier attempts at one problem, with the ability to reload old code. */
const SubmissionHistory = ({ problemId, refreshKey, onLoadCode }) => {
  const [items, setItems] = useState(null);   // null = loading
  const [error, setError] = useState('');
  const [openId, setOpenId] = useState(null);
  const [detail, setDetail] = useState(null);

  const load = useCallback(async () => {
    setError('');
    try {
      setItems(await apiCall(`/submissions/mine?problemId=${problemId}`));
    } catch (err) {
      setError(err.message);
    }
  }, [problemId]);

  useEffect(() => { load(); }, [load, refreshKey]);

  const toggle = async (id) => {
    if (openId === id) {
      setOpenId(null);
      setDetail(null);
      return;
    }
    setOpenId(id);
    setDetail(null);
    try {
      setDetail(await apiCall(`/submissions/${id}`));
    } catch (err) {
      setDetail({ code: `// Could not load this submission: ${err.message}` });
    }
  };

  if (error) return <ErrorState message={error} onRetry={load} />;
  if (items === null) return <div className="history-empty">Loading your submissions...</div>;
  if (items.length === 0) {
    return <div className="history-empty">You have not submitted anything for this problem yet.</div>;
  }

  return (
    <div className="history-list">
      {items.map((s) => (
        <div key={s.id} className="history-item">
          <button className="history-row" onClick={() => toggle(s.id)} aria-expanded={openId === s.id}>
            <span className={`history-status status-${s.status}`}>{statusLabel(s.status)}</span>
            <span className="history-meta">{s.passedCount}/{s.totalCount} tests</span>
            <span className="history-meta">{s.executionTimeMs} ms</span>
            <span className="history-time">{timeAgo(s.createdAt)}</span>
          </button>
          {openId === s.id && (
            <div className="history-detail fade-in">
              {detail === null ? (
                <div className="history-empty">Loading code...</div>
              ) : (
                <>
                  <pre className="history-code">{detail.code}</pre>
                  <button className="btn btn-small btn-secondary" onClick={() => onLoadCode(detail.code)}>
                    Load into editor
                  </button>
                </>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  );
};

export default SubmissionHistory;
