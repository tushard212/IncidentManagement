import React, { useEffect, useState } from 'react';
import { getDashboardStats, getIncidents } from '../services/api';
import { DashboardStats, Incident } from '../types';
import { useNavigate } from 'react-router-dom';

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentIncidents, setRecentIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

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
              <div key={sc.severity} style={{ background: '#1a1f2e', border: '1px solid #2d3748', borderRadius: '8px', padding: '12px 20px' }}>
                <span className={"badge badge-" + sc.severity.toLowerCase()}>{sc.severity}</span>
                <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '8px' }}>{sc.count}</div>
              </div>
            ))}
          </div>
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
