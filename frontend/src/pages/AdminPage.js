import React, { useCallback, useEffect, useState } from 'react';
import { apiCall } from '../api';
import { DIFFICULTIES } from '../utils';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

const emptyProblem = { title: '', difficulty: 'EASY', description: '', solutionTemplate: '', driverCode: '' };
const emptyCase = { input: '', expectedOutput: '', hidden: false };

/** Admin only (the server enforces this too): create problems and manage their test cases. */
const AdminPage = () => {
  const [problems, setProblems] = useState([]);
  const [form, setForm] = useState(emptyProblem);
  const [selected, setSelected] = useState('');
  const [cases, setCases] = useState([]);
  const [caseForm, setCaseForm] = useState(emptyCase);
  const [notice, setNotice] = useState(null);   // {type: 'ok' | 'error', text}

  const say = (type, text) => setNotice({ type, text });

  const loadProblems = useCallback(async () => {
    try {
      setProblems((await apiCall('/problems')) || []);
    } catch (err) {
      say('error', err.message);
    }
  }, []);

  const loadCases = useCallback(async (problemId) => {
    if (!problemId) { setCases([]); return; }
    try {
      setCases((await apiCall(`/testcases/${problemId}/all`)) || []);
    } catch (err) {
      say('error', err.message);
    }
  }, []);

  useEffect(() => { loadProblems(); }, [loadProblems]);
  useEffect(() => { loadCases(selected); }, [selected, loadCases]);

  const createProblem = async (e) => {
    e.preventDefault();
    try {
      const created = await apiCall('/problems', { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(form) });
      say('ok', `Created "${created.title}". Now add its test cases below.`);
      setForm(emptyProblem);
      await loadProblems();
      setSelected(String(created.id));
    } catch (err) {
      say('error', err.message);
    }
  };

  const addCase = async (e) => {
    e.preventDefault();
    try {
      await apiCall(`/testcases/${selected}`, { method: 'POST', headers: JSON_HEADERS, body: JSON.stringify(caseForm) });
      setCaseForm(emptyCase);
      say('ok', 'Test case added.');
      loadCases(selected);
    } catch (err) {
      say('error', err.message);
    }
  };

  const removeCase = async (id) => {
    if (!window.confirm('Delete this test case?')) return;
    try {
      await apiCall(`/testcases/case/${id}`, { method: 'DELETE' });
      loadCases(selected);
    } catch (err) {
      say('error', err.message);
    }
  };

  const field = (key) => ({ value: form[key], onChange: (e) => setForm({ ...form, [key]: e.target.value }) });

  return (
    <div className="dashboard-container fade-in">
      <div className="page-header">
        <div>
          <h2>Admin</h2>
          <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
            Create problems and manage their test cases.
          </p>
        </div>
      </div>

      {notice && (
        <div className={`notice notice-${notice.type}`} role="status">
          {notice.text}
          <button className="close-btn" onClick={() => setNotice(null)} aria-label="Dismiss">✕</button>
        </div>
      )}

      <form className="admin-card" onSubmit={createProblem}>
        <h3 className="section-title">New problem</h3>
        <div className="admin-grid">
          <label>Title<input className="admin-input" required maxLength={255} {...field('title')} /></label>
          <label>Difficulty
            <select className="select-styled" {...field('difficulty')}>
              {DIFFICULTIES.map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
          </label>
        </div>
        <label>Description (max 2000 characters; mention the input and output format)
          <textarea className="admin-input" rows={5} maxLength={2000} {...field('description')} />
        </label>
        <label>Starter template (what the user sees in the editor)
          <textarea className="admin-input mono" rows={5} maxLength={5000} {...field('solutionTemplate')} />
        </label>
        <label>
          <span>Driver code (a <code>main</code> method that reads stdin and prints the answer; never shown to users)</span>
          <textarea className="admin-input mono" rows={7} maxLength={5000} {...field('driverCode')} />
        </label>
        <button type="submit" className="btn">Create problem</button>
      </form>

      <div className="admin-card">
        <h3 className="section-title">Test cases</h3>
        <select className="select-styled" value={selected} onChange={(e) => setSelected(e.target.value)}
                aria-label="Problem">
          <option value="">Select a problem...</option>
          {problems.map((p) => <option key={p.id} value={p.id}>{p.title}</option>)}
        </select>

        {selected && (
          <>
            <table className="leaderboard-table" style={{ marginTop: '1rem' }}>
              <thead><tr><th>Input</th><th>Expected output</th><th>Hidden</th><th></th></tr></thead>
              <tbody>
                {cases.length === 0 ? (
                  <tr><td colSpan="4" className="empty-state">No test cases yet.</td></tr>
                ) : cases.map((c) => (
                  <tr key={c.id}>
                    <td><pre className="case-cell">{c.input}</pre></td>
                    <td><pre className="case-cell">{c.expectedOutput}</pre></td>
                    <td>{c.hidden ? 'Yes' : 'No'}</td>
                    <td><button className="btn btn-small btn-secondary" onClick={() => removeCase(c.id)}>Delete</button></td>
                  </tr>
                ))}
              </tbody>
            </table>

            <form onSubmit={addCase} style={{ marginTop: '1.5rem' }}>
              <div className="admin-grid">
                <label>Input (multiple lines allowed, max 255 characters)
                  <textarea className="admin-input mono" rows={3} maxLength={255} value={caseForm.input}
                            onChange={(e) => setCaseForm({ ...caseForm, input: e.target.value })} />
                </label>
                <label>Expected output
                  <textarea className="admin-input mono" rows={3} required maxLength={255} value={caseForm.expectedOutput}
                            onChange={(e) => setCaseForm({ ...caseForm, expectedOutput: e.target.value })} />
                </label>
              </div>
              <label className="checkbox-row">
                <input type="checkbox" checked={caseForm.hidden}
                       onChange={(e) => setCaseForm({ ...caseForm, hidden: e.target.checked })} />
                Hidden (used for grading only, never shown to users)
              </label>
              <button type="submit" className="btn btn-small">Add test case</button>
            </form>
          </>
        )}
      </div>
    </div>
  );
};

export default AdminPage;
