import React from 'react';
import { CodeIcon, LogOutIcon, TrophyIcon, UserIcon, ShieldIcon } from '../icons';

const Navbar = ({ userName, role, currentPage, setCurrentPage, onLogout }) => {
  const link = (page, label, icon) => (
    <button
      className={`nav-link ${currentPage === page ? 'active' : ''}`}
      onClick={() => setCurrentPage(page)}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        {icon} {label}
      </div>
    </button>
  );

  return (
    <nav className="navbar">
      <div className="logo-container" onClick={() => setCurrentPage('problems')}>
        <CodeIcon />
        <span className="logo">CodeArena</span>
      </div>
      <div className="nav-links">
        {link('problems', 'Problems')}
        {link('leaderboard', 'Leaderboard', <TrophyIcon />)}
        {link('profile', 'Profile', <UserIcon />)}
        {role === 'ADMIN' && link('admin', 'Admin', <ShieldIcon />)}
      </div>
      <div className="user-section">
        <div className="user-name">{userName}</div>
        <button className="btn btn-secondary btn-icon" onClick={onLogout} title="Logout">
          <LogOutIcon />
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
