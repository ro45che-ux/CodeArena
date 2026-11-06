import React from 'react';

const LoadingSpinner = () => (
  <div className="loading-container fade-in">
    <div className="spinner-large"></div>
    <div style={{ color: 'var(--text-secondary)', marginTop: '1rem' }}>Loading data...</div>
  </div>
);

export default LoadingSpinner;
