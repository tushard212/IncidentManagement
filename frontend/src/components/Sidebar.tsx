import React, { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';

interface SidebarProps {
  onLogout: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ onLogout }) => {
  const user = JSON.parse(sessionStorage.getItem('user') || '{}');
  const [theme, setTheme] = useState<string>(sessionStorage.getItem('theme') || 'dark');

  useEffect(() => {
    document.body.className = theme === 'light' ? 'light-theme' : '';
    sessionStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(theme === 'dark' ? 'light' : 'dark');
  };

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <h1>IncidentHub</h1>
        <span>Real-time Incident Management</span>
      </div>
      <nav className="sidebar-nav">
        <NavLink to="/" className={({ isActive }) => isActive ? 'active' : ''}>
          <span className="icon">Dashboard</span>
        </NavLink>
        <NavLink to="/incidents" className={({ isActive }) => isActive ? 'active' : ''}>
          <span className="icon">Incidents</span>
        </NavLink>
        <NavLink to="/analytics" className={({ isActive }) => isActive ? 'active' : ''}>
          <span className="icon">Analytics</span>
        </NavLink>
      </nav>
      <button className="theme-toggle" onClick={toggleTheme}>
        {theme === 'dark' ? '\u2600\uFE0F Light' : '\u{1F319} Dark'}
      </button>
      <div style={{ position: 'absolute', bottom: '20px', left: '20px', right: '20px' }}>
        <div style={{ fontSize: '0.8rem', color: '#6b7280', marginBottom: '8px' }}>
          Logged in as: <strong style={{ color: theme === 'dark' ? '#e7e9ea' : '#1e293b' }}>{user.username}</strong>
          <br />
          Role: <span className="badge badge-open">{user.role}</span>
        </div>
        <button onClick={onLogout} className="btn btn-danger btn-sm" style={{ width: '100%' }}>
          Logout
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
