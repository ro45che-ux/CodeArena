// --- API helper and session storage -------------------------------------------------------------

// Override at build time with REACT_APP_API_URL (e.g. http://localhost:8080 for local development).
export const API_BASE = process.env.REACT_APP_API_URL || 'https://code-arena-backend-ve3a.onrender.com';

// The backend reports errors as {"error": "..."}; fall back to plain text for anything else.
const readErrorMessage = async (response) => {
  const text = await response.text();
  try {
    const data = JSON.parse(text);
    return data.error || data.detail || data.message || text;
  } catch (e) {
    return text;
  }
};

export const getSession = () => ({
  token: localStorage.getItem('token'),
  userName: localStorage.getItem('userName'),
  role: localStorage.getItem('role') || 'CANDIDATE',
});

export const saveSession = ({ token, userId, name, role }) => {
  localStorage.setItem('token', token);
  localStorage.setItem('userId', userId);
  localStorage.setItem('userName', name);
  localStorage.setItem('role', role || 'CANDIDATE');
};

export const clearSession = () => {
  ['token', 'userId', 'userName', 'role'].forEach((k) => localStorage.removeItem(k));
};

export const apiCall = async (endpoint, options = {}) => {
  const token = localStorage.getItem('token');
  const headers = { ...options.headers };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // Default content type for string bodies when the caller did not set one.
  if (options.body && !headers['Content-Type'] && typeof options.body === 'string') {
    headers['Content-Type'] = options.body.startsWith('{') ? 'application/json' : 'text/plain';
  }

  const response = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });

  // 401 on a normal call means the session expired. On the login/register calls it just means
  // "wrong credentials", which must be shown to the user instead of reloading the page.
  if (response.status === 401 && token && !endpoint.startsWith('/auth/')) {
    clearSession();
    window.location.reload();
    throw new Error('Your session has expired. Please log in again.');
  }

  if (!response.ok) {
    throw new Error((await readErrorMessage(response)) || 'Request failed');
  }

  try {
    return await response.json();
  } catch (e) {
    return null;
  }
};
