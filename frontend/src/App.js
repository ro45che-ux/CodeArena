import React, { useState } from 'react';
import './App.css';
import './index.css';
import './features.css';
import { clearSession, getSession } from './api';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import ProblemsPage from './pages/ProblemsPage';
import SolvePage from './pages/SolvePage';
import LeaderboardPage from './pages/LeaderboardPage';
import ProfilePage from './pages/ProfilePage';
import AdminPage from './pages/AdminPage';

function App() {
  const initial = getSession();
  const [userName, setUserName] = useState(initial.token ? initial.userName : null);
  const [role, setRole] = useState(initial.role);
  const [currentPage, setCurrentPage] = useState('problems'); // problems | solve | leaderboard | profile | admin
  const [currentProblem, setCurrentProblem] = useState(null);

  const handleLogin = ({ userName: name, role: userRole }) => {
    setUserName(name);
    setRole(userRole);
    setCurrentPage('problems');
  };

  const handleLogout = () => {
    clearSession();
    setUserName(null);
    setRole('CANDIDATE');
  };

  const navigateToSolve = (problem) => {
    setCurrentProblem(problem);
    setCurrentPage('solve');
  };

  if (!userName) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return (
    <div className="app-container">
      {currentPage !== 'solve' && (
        <Navbar
          userName={userName}
          role={role}
          currentPage={currentPage}
          setCurrentPage={setCurrentPage}
          onLogout={handleLogout}
        />
      )}

      {currentPage === 'problems' && <ProblemsPage onSolve={navigateToSolve} />}
      {currentPage === 'solve' && (
        <SolvePage problem={currentProblem} onBack={() => setCurrentPage('problems')} />
      )}
      {currentPage === 'leaderboard' && <LeaderboardPage />}
      {currentPage === 'profile' && <ProfilePage />}
      {currentPage === 'admin' && role === 'ADMIN' && <AdminPage />}
    </div>
  );
}

export default App;
