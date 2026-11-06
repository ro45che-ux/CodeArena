import { filterProblems, timeAgo, statusLabel } from './utils';

const problems = [
  { id: 1, title: 'Two Sum', description: 'find two numbers', difficulty: 'EASY' },
  { id: 2, title: 'Coin Change', description: 'fewest coins', difficulty: 'MEDIUM' },
  { id: 3, title: 'Trapping Rain Water', description: 'elevation map', difficulty: 'HARD' },
];
const progress = { solvedProblemIds: [1], attemptedProblemIds: [1, 2] };

describe('filterProblems', () => {
  test('returns everything without filters', () => {
    expect(filterProblems(problems, {}, progress)).toHaveLength(3);
  });
  test('filters by difficulty', () => {
    expect(filterProblems(problems, { difficulty: 'HARD' }).map((p) => p.id)).toEqual([3]);
  });
  test('searches the title, ignoring case', () => {
    expect(filterProblems(problems, { query: 'COIN' }).map((p) => p.id)).toEqual([2]);
    expect(filterProblems(problems, { query: 'two' }).map((p) => p.id)).toEqual([1]);
    expect(filterProblems(problems, { query: 'elevation' })).toHaveLength(0); // descriptions are not searched
  });
  test('filters by progress status', () => {
    expect(filterProblems(problems, { status: 'SOLVED' }, progress).map((p) => p.id)).toEqual([1]);
    expect(filterProblems(problems, { status: 'ATTEMPTED' }, progress).map((p) => p.id)).toEqual([2]);
    expect(filterProblems(problems, { status: 'TODO' }, progress).map((p) => p.id)).toEqual([3]);
  });
  test('treats everything as not started when progress is unknown', () => {
    expect(filterProblems(problems, { status: 'TODO' }, null)).toHaveLength(3);
    expect(filterProblems(problems, { status: 'SOLVED' }, null)).toHaveLength(0);
  });
  test('combines filters', () => {
    expect(filterProblems(problems, { difficulty: 'MEDIUM', query: 'coin', status: 'ATTEMPTED' }, progress)).toHaveLength(1);
  });
});

describe('timeAgo', () => {
  const now = new Date('2026-01-10T12:00:00Z').getTime();
  test('formats recent times', () => {
    expect(timeAgo('2026-01-10T11:59:50Z', now)).toBe('just now');
    expect(timeAgo('2026-01-10T11:55:00Z', now)).toBe('5 min ago');
    expect(timeAgo('2026-01-10T09:00:00Z', now)).toBe('3 h ago');
    expect(timeAgo('2026-01-08T12:00:00Z', now)).toBe('2 d ago');
  });
  test('handles missing values', () => {
    expect(timeAgo(null, now)).toBe('');
  });
});

test('statusLabel makes enum names readable', () => {
  expect(statusLabel('TIME_LIMIT_EXCEEDED')).toBe('TIME LIMIT EXCEEDED');
  expect(statusLabel(undefined)).toBe('');
});
