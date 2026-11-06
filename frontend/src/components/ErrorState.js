import React from 'react';

/** Shown when a request fails, instead of pretending with fake data. */
const ErrorState = ({ message, onRetry }) => (
  <div className="empty-state error-state fade-in">
    <p>{message || 'Something went wrong.'}</p>
    <p style={{ fontSize: '0.85rem', marginTop: '0.5rem' }}>
      The server may be waking up (free hosting sleeps after inactivity). Wait a few seconds and try again.
    </p>
    {onRetry && (
      <button className="btn btn-secondary" style={{ marginTop: '1rem' }} onClick={onRetry}>
        Try again
      </button>
    )}
  </div>
);

export default ErrorState;
