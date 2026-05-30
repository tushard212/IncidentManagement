import React, { useEffect, useState } from 'react';
import { getIncidents, createIncident } from '../services/api';
import { Incident } from '../types';
import { useNavigate } from 'react-router-dom';

const IncidentList: React.FC = () => {
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const navigate = useNavigate();

  const [newIncident, setNewIncident] = useState({
    title: '',
    description: '',
    severity: 'HIGH',
    service: '',
  });

  useEffect(() => {
    loadIncidents();
  }, [page]);

  const loadIncidents = async () => {
    try {
      const response = await getIncidents(page);
      setIncidents(response.data.content);
      setTotalPages(response.data.totalPages);
    } catch (err) {
      console.error('Failed to load incidents', err);
    } finally {
      setLoading(false);
    }
  };

  const severityOrder: Record<string, number> = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };

  const getSortedIncidents = () => {
    let filtered = [...incidents];

    if (statusFilter !== 'ALL') {
      filtered = filtered.filter((i) => i.status === statusFilter);
    }

    if (severityFilter !== 'ALL') {
      filtered = filtered.filter((i) => i.severity === severityFilter);
    }

    const closed = filtered.filter((i) => i.status === 'CLOSED');
    const active = filtered.filter((i) => i.status !== 'CLOSED');

    active.sort((a, b) => {
      const sevDiff = (severityOrder[a.severity] || 99) - (severityOrder[b.severity] || 99);
      if (sevDiff !== 0) return sevDiff;
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });

    closed.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

    return [...active, ...closed];
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createIncident(newIncident);
      setShowCreateModal(false);
      setNewIncident({ title: '', description: '', severity: 'HIGH', service: '' });
      loadIncidents();
    } catch (err) {
      console.error('Failed to create incident', err);
    }
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}>Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h2>Incidents</h2>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{ padding: '8px 12px', background: '#1a1f2e', border: '1px solid #374151', borderRadius: '8px', color: '#e7e9ea', fontSize: '0.85rem' }}
          >
            <option value="ALL">All Statuses</option>
            <option value="OPEN">Open</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="INVESTIGATING">Investigating</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
          </select>
          <select
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            style={{ padding: '8px 12px', background: '#1a1f2e', border: '1px solid #374151', borderRadius: '8px', color: '#e7e9ea', fontSize: '0.85rem' }}
          >
            <option value="ALL">All Severities</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
          <button className="btn btn-primary" onClick={() => setShowCreateModal(true)}>
            + Create Incident
          </button>
        </div>
      </div>

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
              <th>SLA</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {getSortedIncidents().map((incident) => (
              <tr key={incident.id} onClick={() => navigate("/incidents/" + incident.id)}
                style={incident.status === 'CLOSED' ? { opacity: 0.5 } : {}}>
                <td>#{incident.id}</td>
                <td>{incident.title}</td>
                <td><span className={"badge badge-" + incident.severity.toLowerCase()}>{incident.severity}</span></td>
                <td><span className={"badge badge-" + incident.status.toLowerCase()}>{incident.status}</span></td>
                <td>{incident.assigneeName || 'Unassigned'}</td>
                <td>{incident.service || '-'}</td>
                <td>
                  {incident.slaBreached ? (
                    <span className="sla-breached">BREACHED</span>
                  ) : (
                    <span style={{ color: '#22c55e', fontSize: '0.8rem' }}>Within SLA</span>
                  )}
                </td>
                <td style={{ fontSize: '0.8rem', color: '#6b7280' }}>
                  {new Date(incident.createdAt).toLocaleString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '20px' }}>
        <button className="btn btn-sm" onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0}
          style={{ background: '#374151', color: '#e7e9ea' }}>
          Previous
        </button>
        <span style={{ padding: '6px 12px', color: '#6b7280' }}>
          Page {page + 1} of {totalPages || 1}
        </span>
        <button className="btn btn-sm" onClick={() => setPage(page + 1)} disabled={page >= totalPages - 1}
          style={{ background: '#374151', color: '#e7e9ea' }}>
          Next
        </button>
      </div>

      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Create New Incident</h3>
            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label>Title *</label>
                <input
                  type="text"
                  value={newIncident.title}
                  onChange={(e) => setNewIncident({ ...newIncident, title: e.target.value })}
                  placeholder="Brief incident title"
                  required
                />
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea
                  value={newIncident.description}
                  onChange={(e) => setNewIncident({ ...newIncident, description: e.target.value })}
                  placeholder="Detailed description of the incident"
                  rows={4}
                />
              </div>
              <div className="form-group">
                <label>Severity *</label>
                <select
                  value={newIncident.severity}
                  onChange={(e) => setNewIncident({ ...newIncident, severity: e.target.value })}
                >
                  <option value="CRITICAL">Critical (SLA: 15 min)</option>
                  <option value="HIGH">High (SLA: 30 min)</option>
                  <option value="MEDIUM">Medium (SLA: 2 hours)</option>
                  <option value="LOW">Low (SLA: 24 hours)</option>
                </select>
              </div>
              <div className="form-group">
                <label>Affected Service</label>
                <input
                  type="text"
                  value={newIncident.service}
                  onChange={(e) => setNewIncident({ ...newIncident, service: e.target.value })}
                  placeholder="e.g., payment-service, api-gateway"
                />
              </div>
              <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
                <button type="submit" className="btn btn-primary">Create Incident</button>
                <button type="button" className="btn" onClick={() => setShowCreateModal(false)}
                  style={{ background: '#374151', color: '#e7e9ea' }}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default IncidentList;
