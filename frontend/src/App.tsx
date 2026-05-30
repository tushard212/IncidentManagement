import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import IncidentList from './pages/IncidentList';
import IncidentDetail from './pages/IncidentDetail';
import Analytics from './pages/Analytics';
import UrlShortener from './pages/UrlShortener';
import Sidebar from './components/Sidebar';
import { wsService } from './services/websocket';
import { WebSocketMessage } from './types';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(!!sessionStorage.getItem('token'));
  const [notifications, setNotifications] = useState<WebSocketMessage[]>([]);

  useEffect(() => {
    if (isAuthenticated) {
      wsService.connect();
      const unsubscribe = wsService.subscribe((msg) => {
        setNotifications((prev) => [msg, ...prev.slice(0, 9)]);
      });
      return () => {
        unsubscribe();
        wsService.disconnect();
      };
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (notifications.length > 0) {
      const timer = setTimeout(() => {
        setNotifications((prev) => prev.slice(1));
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [notifications]);

  const handleLogin = () => setIsAuthenticated(true);
  const handleLogout = () => {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    setIsAuthenticated(false);
  };

  if (!isAuthenticated) {
    return <Login onLogin={ handleLogin } />;
  }

  return (
    <Router>
    <div className= "app" >
    <Sidebar onLogout={ handleLogout } />
      < main className = "main-content" >
      {
        notifications.length > 0 && notifications[0] && (
          <div className={ "toast toast-" + (notifications[0].type === 'SLA_BREACHED' ? 'critical' : 'info') }>
            <strong>{ notifications[0].type.replace(/_/g, ' ') } </strong>
            < p style = {{ fontSize: '0.85rem', marginTop: '4px' }
}>
  { notifications[0].data?.title }
  </p>
  </div>
          )}
<Routes>
  <Route path="/" element = {< Dashboard />} />
    < Route path = "/incidents" element = {< IncidentList />} />
      < Route path = "/incidents/:id" element = {< IncidentDetail />} />
        < Route path = "/analytics" element = {< Analytics />} />
          < Route path = "/urls" element = {< UrlShortener />} />
            < Route path = "*" element = {< Navigate to = "/" />} />
              </Routes>
              </main>
              </div>
              </Router>
  );
}

export default App;
