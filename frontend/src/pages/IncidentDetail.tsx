import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getIncident, acknowledgeIncident, updateIncidentStatus, addNote, deleteIncident } from '../services/api';
import { Incident } from '../types';

const IncidentDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [incident, setIncident] = useState<Incident | null>(null);
  const [loading, setLoading] = useState(true);
  const [note, setNote] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    loadIncident();
  }, [id]);

  const loadIncident = async () => {
    try {
      const response = await getIncident(Number(id));
      setIncident(response.data);
    } catch (err) {
      console.error('Failed to load incident', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAcknowledge = async () => {
    try {
      const response = await acknowledgeIncident(Number(id));
      setIncident(response.data);
    } catch (err: any) {
      alert(err.response?.data?.error || 'Failed to acknowledge');
    }
  };

  const handleStatusChange = async (status: string) => {
    try {
      const response = await updateIncidentStatus(Number(id), status);
      setIncident(response.data);
    } catch (err: any) {
      alert(err.response?.data?.error || 'Failed to update status');
    }
  };

  const handleAddNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!note.trim()) return;
    try {
      const response = await addNote(Number(id), note);
      setIncident(response.data);
      setNote('');
    } catch (err) {
      console.error('Failed to add note', err);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this incident?')) return;
    try {
      await deleteIncident(Number(id));
      navigate('/incidents');
    } catch (err: any) {
      alert(err.response?.data?.error || 'Failed to delete incident');
    }
  };

  const getUserRole = (): string => {
    const user = sessionStorage.getItem('user');
    if (user) {
      try { return JSON.parse(user).role || ''; } catch { return ''; }
    }
    return '';
  };

  if (loading) return <div style={ { padding: '40px', textAlign: 'center' } }> Loading...</div>;
  if (!incident) return <div>Incident not found </div>;

  const getNextActions = () => {
    switch (incident.status) {
      case 'OPEN':
        return [
          { label: 'Acknowledge', action: () => handleAcknowledge(), className: 'btn-warning' },
        ];
      case 'ACKNOWLEDGED':
        return [
          { label: 'Start Investigating', action: () => handleStatusChange('INVESTIGATING'), className: 'btn-warning' },
          { label: 'Resolve', action: () => handleStatusChange('RESOLVED'), className: 'btn-success' },
        ];
      case 'INVESTIGATING':
        return [
          { label: 'Resolve', action: () => handleStatusChange('RESOLVED'), className: 'btn-success' },
        ];
      case 'RESOLVED':
        return [
          { label: 'Close', action: () => handleStatusChange('CLOSED'), className: 'btn-primary' },
          { label: 'Reopen', action: () => handleStatusChange('INVESTIGATING'), className: 'btn-danger' },
        ];
      default:
        return [];
    }
  };

  return (
    <div>
    <div className= "page-header" >
    <div>
    <button onClick={ () => navigate('/incidents') } className = "btn btn-sm"
  style = {{ background: '#374151', color: '#e7e9ea', marginBottom: '12px' }
}>
  Back to Incidents
    </button>
    < h2 >#{ incident.id } - { incident.title } </h2>
      </div>
      </div>

      < div style = {{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '24px' }}>
        <div>
        <div style={ { background: '#1a1f2e', border: '1px solid #2d3748', borderRadius: '12px', padding: '24px', marginBottom: '20px' } }>
          <div style={ { display: 'flex', gap: '12px', marginBottom: '16px', flexWrap: 'wrap' } }>
            <span className={ "badge badge-" + incident.severity.toLowerCase() }> { incident.severity } </span>
              < span className = { "badge badge-"+incident.status.toLowerCase() } > { incident.status } </span>
{ incident.slaBreached && <span className="sla-breached" > SLA BREACHED </span> }
</div>

  < p style = {{ color: '#9ca3af', marginBottom: '20px' }}> { incident.description } </p>

    < div style = {{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', fontSize: '0.85rem' }}>
      <div><span style={ { color: '#6b7280' } }> Service: </span>{incident.service || '-'}</div >
        <div><span style={ { color: '#6b7280' } }> Assignee: </span>{incident.assigneeName || 'Unassigned'}</div >
          <div><span style={ { color: '#6b7280' } }> Reporter: </span>{incident.reporterName}</div >
            <div><span style={ { color: '#6b7280' } }> Team: </span>{incident.teamName || '-'}</div >
              <div><span style={ { color: '#6b7280' } }> Created: </span>{new Date(incident.createdAt).toLocaleString()}</div >
                <div><span style={ { color: '#6b7280' } }> SLA Deadline: </span>{new Date(incident.slaDeadline).toLocaleString()}</div >
                {
                  incident.acknowledgedAt && (
                    <div><span style={ { color: '#6b7280' } }> Acknowledged: </span>{new Date(incident.acknowledgedAt).toLocaleString()}</div >
              )}
{
  incident.resolvedAt && (
    <div><span style={ { color: '#6b7280' } }> Resolved: </span>{new Date(incident.resolvedAt).toLocaleString()}</div >
              )
}
<div><span style={ { color: '#6b7280' } }> Escalation Level: </span>{incident.escalationLevel}</div >
  </div>
  </div>

  < div style = {{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
  {
    getNextActions().map((action, idx) => (
      <button key= { idx } className = { "btn " + action.className } onClick = { action.action } >
      { action.label }
      </button>
    ))
  }
{
  (getUserRole() === 'ADMIN' || getUserRole() === 'MANAGER') && (
    <button className="btn btn-danger" onClick = { handleDelete } >
      Delete Incident
        </button>
            )
}
</div>

  < div style = {{ background: '#1a1f2e', border: '1px solid #2d3748', borderRadius: '12px', padding: '20px' }}>
    <h4 style={ { marginBottom: '12px' } }> Add Note </h4>
      < form onSubmit = { handleAddNote } style = {{ display: 'flex', gap: '8px' }}>
        <input
                type="text"
value = { note }
onChange = {(e) => setNote(e.target.value)}
placeholder = "Add a note or update..."
style = {{ flex: 1, padding: '10px', background: '#0f1419', border: '1px solid #374151', borderRadius: '8px', color: '#e7e9ea' }}
              />
  < button type = "submit" className = "btn btn-primary btn-sm" > Add </button>
    </form>
    </div>
    </div>

    < div style = {{ background: '#1a1f2e', border: '1px solid #2d3748', borderRadius: '12px', padding: '24px' }}>
      <h4 style={ { marginBottom: '16px' } }> Timeline </h4>
        < div className = "timeline" >
        {
          incident.timeline.map((entry) => (
            <div key= { entry.id } className = "timeline-item" >
            <div>
            <div className="timeline-action" > { entry.action } </div>
          < div className = "timeline-message" > { entry.message } </div>
          < div className = "timeline-meta" >
          { entry.performedByName } - { new Date(entry.createdAt).toLocaleString() }
          </div>
          </div>
          </div>
          ))
        }
          </div>
          </div>
          </div>
          </div>
  );
};

export default IncidentDetail;
