// Small pure helpers (kept free of React so they are easy to unit-test).

export const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

/**
 * Filters the problem list.
 *
 * @param problems   array of {id, title, description, difficulty}
 * @param filters    {query, difficulty ('ALL'|EASY|MEDIUM|HARD), status ('ALL'|'SOLVED'|'ATTEMPTED'|'TODO')}
 * @param progress   {solvedProblemIds: number[], attemptedProblemIds: number[]} or null
 */
export const filterProblems = (problems, { query = '', difficulty = 'ALL', status = 'ALL' } = {}, progress = null) => {
  const q = query.trim().toLowerCase();
  const solved = new Set((progress && progress.solvedProblemIds) || []);
  const attempted = new Set((progress && progress.attemptedProblemIds) || []);

  return problems.filter((p) => {
    if (difficulty !== 'ALL' && p.difficulty !== difficulty) return false;
    if (q && !(`${p.title} ${p.description || ''}`.toLowerCase().includes(q))) return false;
    if (status === 'SOLVED') return solved.has(p.id);
    if (status === 'ATTEMPTED') return attempted.has(p.id) && !solved.has(p.id);
    if (status === 'TODO') return !attempted.has(p.id);
    return true;
  });
};

/** "just now", "5 min ago", "3 h ago", "2 d ago", or a date for anything older than a month. */
export const timeAgo = (isoString, now = Date.now()) => {
  if (!isoString) return '';
  const seconds = Math.max(0, Math.floor((now - new Date(isoString).getTime()) / 1000));
  if (seconds < 60) return 'just now';
  if (seconds < 3600) return `${Math.floor(seconds / 60)} min ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)} h ago`;
  if (seconds < 30 * 86400) return `${Math.floor(seconds / 86400)} d ago`;
  return new Date(isoString).toLocaleDateString();
};

export const statusLabel = (status) => (status || '').replace(/_/g, ' ');
