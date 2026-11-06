import React, { useCallback, useEffect, useRef, useState } from 'react';
import Editor from '@monaco-editor/react';
import { apiCall } from '../api';
import { ArrowLeftIcon, PlayIcon, SendIcon } from '../icons';
import { statusLabel } from '../utils';
import SubmissionHistory from './SubmissionHistory';

const draftKey = (problemId) => `draft:${problemId}`;

const readDraft = (problemId) => {
  try { return localStorage.getItem(draftKey(problemId)); } catch (e) { return null; }
};
const writeDraft = (problemId, code) => {
  try { localStorage.setItem(draftKey(problemId), code); } catch (e) { /* storage may be unavailable */ }
};
const clearDraft = (problemId) => {
  try { localStorage.removeItem(draftKey(problemId)); } catch (e) { /* ignore */ }
};

const SolvePage = ({ problem, onBack }) => {
  const template = problem.solutionTemplate || '';
  const [code, setCode] = useState(() => readDraft(problem.id) ?? template);
  const [busy, setBusy] = useState(null);          // null | 'run' | 'submit'
  const [result, setResult] = useState(null);      // {kind: 'run' | 'submit', ...server fields}
  const [examples, setExamples] = useState([]);
  const [tab, setTab] = useState('description');
  const [historyVersion, setHistoryVersion] = useState(0);

  useEffect(() => {
    let cancelled = false;
    apiCall(`/testcases/${problem.id}`)
      .then((data) => { if (!cancelled) setExamples(data || []); })
      .catch((err) => console.error('Failed to fetch examples:', err));
    return () => { cancelled = true; };
  }, [problem.id]);

  // keep the draft so a refresh or accidental navigation does not lose the user's work
  useEffect(() => { writeDraft(problem.id, code); }, [problem.id, code]);

  const send = useCallback(async (kind) => {
    setBusy(kind);
    setResult(null);
    try {
      // The server identifies the user from the login token; no user id is sent.
      const path = kind === 'run' ? `/submissions/${problem.id}/run` : `/submissions/${problem.id}`;
      const res = await apiCall(path, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: code,
      });
      if (!res) throw new Error('The server returned an empty response. Please try again.');
      setResult({ kind, ...res });
      if (kind === 'submit') setHistoryVersion((v) => v + 1);
    } catch (err) {
      setResult({ kind, status: 'ERROR', score: 0, passedCount: 0, totalCount: 0, executionTimeMs: 0, message: err.message });
    } finally {
      setBusy(null);
    }
  }, [code, problem.id]);

  // Ctrl/Cmd+Enter runs the examples; the handler must always see the latest code
  const sendRef = useRef(send);
  useEffect(() => { sendRef.current = send; }, [send]);

  const handleEditorMount = (editor, monaco) => {
    editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.Enter, () => sendRef.current('run'));
  };

  const resetCode = () => {
    if (window.confirm('Reset your code to the starter template?')) {
      clearDraft(problem.id);
      setCode(template);
    }
  };

  return (
    <div className="solve-page fade-in">
      <div className="split-left">
        <div className="problem-details">
          <button className="back-btn" onClick={onBack}>
            <ArrowLeftIcon /> Back to Problems
          </button>

          <h2 className="problem-full-title">{problem.title}</h2>

          <div className="problem-stats">
            <div className="stat-item">
              <span className="stat-label">Difficulty</span>
              <span className={`difficulty-badge diff-${problem.difficulty}`} style={{ display: 'inline-block', marginTop: '4px' }}>
                {problem.difficulty}
              </span>
            </div>
          </div>

          <div className="tab-bar" role="tablist">
            <button role="tab" aria-selected={tab === 'description'}
                    className={`tab ${tab === 'description' ? 'tab-active' : ''}`}
                    onClick={() => setTab('description')}>Description</button>
            <button role="tab" aria-selected={tab === 'submissions'}
                    className={`tab ${tab === 'submissions' ? 'tab-active' : ''}`}
                    onClick={() => setTab('submissions')}>My Submissions</button>
          </div>

          {tab === 'description' ? (
            <>
              <div className="problem-description-full">{problem.description}</div>

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

              <div className="hint-box">
                💡 <strong>Implement the function in the editor.</strong> The class and driver code are handled
                automatically. <strong>Run</strong> checks the examples only; <strong>Submit</strong> grades
                against all test cases, including hidden ones.
              </div>
            </>
          ) : (
            <SubmissionHistory
              problemId={problem.id}
              refreshKey={historyVersion}
              onLoadCode={(c) => { setCode(c); }}
            />
          )}
        </div>
      </div>

      <div className="split-right">
        <div className="editor-header">
          <div className="editor-tabs">
            <div className="editor-tab">Solution.java</div>
          </div>
          <div className="editor-actions">
            <button className="btn btn-small btn-ghost" onClick={resetCode} disabled={busy !== null} title="Reset to the starter code">
              Reset
            </button>
            <button className="btn btn-small btn-secondary" onClick={() => send('run')} disabled={busy !== null}
                    title="Run the example test cases (Ctrl/Cmd + Enter)">
              {busy === 'run' ? <span className="run-spinner"></span> : <><PlayIcon /> Run</>}
            </button>
            <button className="btn btn-small" onClick={() => send('submit')} disabled={busy !== null}
                    title="Submit for grading against all test cases">
              {busy === 'submit' ? <span className="run-spinner"></span> : <><SendIcon /> Submit</>}
            </button>
          </div>
        </div>

        <div className="monaco-wrapper">
          <Editor
            height="100%"
            language="java"
            theme="vs-dark"
            value={code}
            onChange={(value) => setCode(value ?? '')}
            onMount={handleEditorMount}
            loading={<div className="history-empty">Loading editor...</div>}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              fontFamily: "'JetBrains Mono', 'Courier New', monospace",
              scrollBeyondLastLine: false,
              automaticLayout: true,
              tabSize: 4,
              padding: { top: 12 },
            }}
          />
        </div>

        {(result || busy) && (
          <div className="results-panel fade-in">
            <div className="results-header">
              {busy
                ? (busy === 'run' ? 'Running example test cases...' : 'Judging your submission...')
                : (result.kind === 'run' ? 'Run result · example test cases only' : 'Submission result · all test cases')}
              {!busy && (
                <button className="close-btn" onClick={() => setResult(null)} aria-label="Close results">✕</button>
              )}
            </div>
            {!busy && result && (
              <div className="results-content">
                <div className={`result-status status-${result.status}`}>{statusLabel(result.status)}</div>

                <div className="result-metrics">
                  <div className="metric">
                    <span className="metric-label">Test Cases</span>
                    <span className="metric-value">{result.passedCount} / {result.totalCount}</span>
                  </div>
                  {result.kind === 'submit' && (
                    <div className="metric">
                      <span className="metric-label">Score</span>
                      <span className="metric-value">{result.score} pts</span>
                    </div>
                  )}
                  <div className="metric">
                    <span className="metric-label">Runtime</span>
                    <span className="metric-value">{result.executionTimeMs} ms</span>
                  </div>
                </div>

                {result.message && <div className="result-message">{result.message}</div>}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default SolvePage;
