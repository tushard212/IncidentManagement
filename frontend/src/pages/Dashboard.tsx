import React, { useEffect, useState } from 'react';
import { getDashboardStats, getIncidents, getNotificationStats, getNotifications } from '../services/api';
import { DashboardStats, Incident } from '../types';
import { useNavigate } from 'react-router-dom';

interface NotificationLog {
  id: number;
  recipientEmail: string;
  recipientName: string;
  subject: string;
  type: string;
  status: string;
  incidentId: number;
  incidentTitle: string;
  sentAt: string;
  errorMessage: string | null;
}

interface NotifStats {
  totalSent: number;
  totalFailed: number;
  last24h: number;
  failedLast7d: number;
}

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentIncidents, setRecentIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [notifStats, setNotifStats] = useState<NotifStats | null>(null);
  const [notifications, setNotifications] = useState<NotificationLog[]>([]);
  const navigate = useNavigate();

  const user = JSON.parse(sessionStorage.getItem('user') || '{}');
  const isAdminOrManager = user.role === 'ADMIN' || user.role === 'MANAGER';

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [statsRes, incidentsRes] = await Promise.all([
        getDashboardStats(),
        getIncidents(0, 5),
      ]);
      setStats(statsRes.data);
      setRecentIncidents(incidentsRes.data.content);

      if (isAdminOrManager) {
        try {
          const [nStatsRes, nLogsRes] = await Promise.all([
            getNotificationStats(),
            getNotifications(0, 5),
          ]);
          setNotifStats(nStatsRes.data);
          setNotifications(nLogsRes.data.content);
        } catch (e) {
          console.error('Failed to load notification data', e);
        }
      }
    } catch (err) {
      console.error('Failed to load dashboard data', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}>Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h2>Dashboard</h2>
        <span style={{ color: '#6b7280', fontSize: '0.9rem' }}>Real-time incident overview</span>
      </div>

      <div className="stats-grid">
        <div className="stat-card critical">
          <div className="stat-value">{stats?.totalOpen || 0}</div>
          <div className="stat-label">Open Incidents</div>
        </div>
        <div className="stat-card high">
          <div className="stat-value">{stats?.totalAcknowledged || 0}</div>
          <div className="stat-label">Acknowledged</div>
        </div>
        <div className="stat-card medium">
          <div className="stat-value">{stats?.totalInvestigating || 0}</div>
          <div className="stat-label">Investigating</div>
        </div>
        <div className="stat-card resolved">
          <div className="stat-value">{stats?.totalResolved || 0}</div>
          <div className="stat-label">Resolved</div>
        </div>
        <div className="stat-card breached">
          <div className="stat-value">{stats?.slaBreachedCount || 0}</div>
          <div className="stat-label">SLA Breached</div>
        </div>
      </div>

      {stats?.severityCounts && stats.severityCounts.length > 0 && (
        <div style={{ marginBottom: '30px' }}>
          <h3 style={{ marginBottom: '12px', fontSize: '1rem' }}>Severity Distribution</h3>
          <div style={{ display: 'flex', gap: '12px' }}>
            {stats.severityCounts.map((sc) => (
              <div key={sc.severity} className="severity-card">
                <span className={"badge badge-" + sc.severity.toLowerCase()}>{sc.severity}</span>
                <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '8px' }}>{sc.count}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {isAdminOrManager && notifStats && (
        <div style={{ marginBottom: '30px' }}>
          <h3 style={{ marginBottom: '12px', fontSize: '1rem' }}>Email Notifications</h3>
          <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(4, 1fr)' }}>
            <div className="stat-card" style={{ borderLeft: '4px solid #10b981' }}>
              <div className="stat-value">{notifStats.totalSent}</div>
              <div className="stat-label">Total Sent</div>
            </div>
            <div className="stat-card" style={{ borderLeft: '4px solid #ef4444' }}>
              <div className="stat-value">{notifStats.totalFailed}</div>
              <div className="stat-label">Failed</div>
            </div>
            <div className="stat-card" style={{ borderLeft: '4px solid #3b82f6' }}>
              <div className="stat-value">{notifStats.last24h}</div>
              <div className="stat-label">Last 24h</div>
            </div>
            <div className="stat-card" style={{ borderLeft: '4px solid #f59e0b' }}>
              <div className="stat-value">{notifStats.failedLast7d}</div>
              <div className="stat-label">Failed (7d)</div>
            </div>
          </div>

          {notifications.length > 0 && (
            <div className="incidents-table" style={{ marginTop: '12px' }}>
              <table>
                <thead>
                  <tr>
                    <th>Recipient</th>
                    <th>Type</th>
                    <th>Incident</th>
                    <th>Status</th>
                    <th>Sent At</th>
                  </tr>
                </thead>
                <tbody>
                  {notifications.map((n) => (
                    <tr key={n.id}>
                      <td>{n.recipientName}</td>
                      <td><span className="badge" style={{ background: n.type === 'INCIDENT_ESCALATED' ? '#ef4444' : n.type === 'INCIDENT_CREATED' ? '#3b82f6' : '#10b981', color: '#fff', padding: '2px 8px', borderRadius: '4px', fontSize: '0.75rem' }}>{n.type.replace('INCIDENT_', '')}</span></td>
                      <td style={{ cursor: 'pointer', color: '#60a5fa' }} onClick={() => navigate('/incidents/' + n.incidentId)}>#{n.incidentId} {n.incidentTitle}</td>
                      <td><span style={{ color: n.status === 'SENT' ? '#10b981' : '#ef4444', fontWeight: 600 }}>{n.status}</span></td>
                      <td style={{ fontSize: '0.8rem', color: '#6b7280' }}>{new Date(n.sentAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      <h3 style={{ marginBottom: '12px', fontSize: '1rem' }}>Recent Incidents</h3>
      <div className="incidents-table">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Title</th>
              <th>Severity</th>
              <th>Status</th>
              <th>Assignee</th>
              <th>Service</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {recentIncidents.map((incident) => (
              <tr key={incident.id} onClick={() => navigate("/incidents/" + incident.id)}>
                <td>#{incident.id}</td>
                <td>
                  {incident.title}
                  {incident.slaBreached && <span className="sla-breached" style={{ marginLeft: '8px' }}>SLA</span>}
                </td>
                <td><span className={"badge badge-" + incident.severity.toLowerCase()}>{incident.severity}</span></td>
                <td><span className={"badge badge-" + incident.status.toLowerCase()}>{incident.status}</span></td>
                <td>{incident.assigneeName || '-'}</td>
                <td>{incident.service || '-'}</td>
                <td style={{ fontSize: '0.8rem', color: '#6b7280' }}>
                  {new Date(incident.createdAt).toLocaleString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Dashboard;
