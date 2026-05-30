import React, { useEffect, useState } from 'react';
import api from '../services/api';

interface AnalyticsData {
  mttrMinutes: number;
  mttaMinutes: number;
  totalIncidents: number;
  totalResolved: number;
  slaComplianceRate: number;
  incidentsPerDay: Record<string, number>;
  bySeverity: Record<string, number>;
  byStatus: Record<string, number>;
  resolutionBySeverity: Record<string, number>;
  periodDays: number;
}

const Analytics: React.FC = () => {
  const [data, setData] = useState<AnalyticsData | null>(null);
  const [days, setDays] = useState(30);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAnalytics();
  }, [days]);

  const loadAnalytics = async () => {
    setLoading(true);
    try {
      const response = await api.get('/v2/analytics?days=' + days);
      setData(response.data);
    } catch (err) {
      console.error('Failed to load analytics', err);
    } finally {
      setLoading(false);
    }
  };

  const formatMinutes = (mins: number) => {
    if (mins < 60) return mins.toFixed(1) + ' min';
    return (mins / 60).toFixed(1) + ' hrs';
  };

  if (loading) return <div style={ { padding: '40px', textAlign: 'center' } }> Loading analytics...</div>;
  if (!data) return <div style={ { padding: '40px', textAlign: 'center' } }> No data available </div>;

  const maxPerDay = Math.max(...Object.values(data.incidentsPerDay || {}), 1);
  const severityColors: Record<string, string> = { CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#eab308', LOW: '#22c55e' };

  return (
    <div>
    <div className= "page-header" >
    <h2>Analytics </h2>
    < select
  value = { days }
  onChange = {(e) => setDays(Number(e.target.value))}
style = {{ padding: '8px 12px', background: '#1a1f2e', border: '1px solid #374151', borderRadius: '8px', color: '#e7e9ea' }}
        >
  <option value={ 7 }> Last 7 days </option>
    < option value = { 14} > Last 14 days </option>
      < option value = { 30} > Last 30 days </option>
        < option value = { 90} > Last 90 days </option>
          </select>
          </div>

          < div className = "analytics-grid" >
            <div className="analytics-card" >
              <h4>Mean Time to Resolve(MTTR) </h4>
                < span className = "metric-value" > { formatMinutes(data.mttrMinutes) } </span>
                  </div>
                  < div className = "analytics-card" >
                    <h4>Mean Time to Acknowledge(MTTA) </h4>
                      < span className = "metric-value" > { formatMinutes(data.mttaMinutes) } </span>
                        </div>
                        < div className = "analytics-card" >
                          <h4>SLA Compliance Rate </h4>
                            < span className = "metric-value" style = {{ color: data.slaComplianceRate >= 95 ? '#22c55e' : data.slaComplianceRate >= 80 ? '#eab308' : '#ef4444' }}>
                              { data.slaComplianceRate } %
                              </span>
                              </div>
                              < div className = "analytics-card" >
                                <h4>Total Incidents({ days }d) </h4>
                                  < span className = "metric-value" > { data.totalIncidents } </span>
                                    </div>
                                    < div className = "analytics-card" >
                                      <h4>Resolved </h4>
                                      < span className = "metric-value" style = {{ color: '#22c55e' }}> { data.totalResolved } </span>
                                        </div>
                                        < div className = "analytics-card" >
                                          <h4>Resolution Rate </h4>
                                            < span className = "metric-value" >
                                              { data.totalIncidents > 0 ? Math.round((data.totalResolved / data.totalIncidents) * 100) : 0 } %
                                              </span>
                                              </div>
                                              </div>

                                              < div style = {{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px' }}>
                                                <div className="analytics-card" >
                                                  <h4>Incidents by Severity </h4>
                                                    < div style = {{ marginTop: '16px' }}>
                                                    {
                                                      Object.entries(data.bySeverity || {}).map(([sev, count]) => (
                                                        <div key= { sev } className = "analytics-row" >
                                                        <span style={{ color: severityColors[sev] || '#6b7280', fontWeight: 600 }} > { sev } </span>
                                                      < span style = {{ fontSize: '1.2rem', fontWeight: 700 }}> { count } </span>
                                                        </div>
            ))}
</div>
  </div>

  < div className = "analytics-card" >
    <h4>Incidents by Status </h4>
      < div style = {{ marginTop: '16px' }}>
      {
        Object.entries(data.byStatus || {}).map(([status, count]) => (
          <div key= { status } className = "analytics-row" >
          <span className={ "badge badge-" + status.toLowerCase() } > { status } </span>
        < span style = {{ fontSize: '1.2rem', fontWeight: 700 }} > { count } </span>
        </div>
            ))}
</div>
  </div>
  </div>

  < div className = "analytics-card" >
    <h4>Incidents Per Day </h4>
      < div className = "chart-bar" >
      {
        Object.entries(data.incidentsPerDay || {}).slice(-14).map(([date, count]) => (
          <div key= { date } className = "chart-bar-item" >
          <div className="bar" style = {{ height: (count / maxPerDay) * 100 + '%' }} > </div>
        < span className = "bar-label" > { date.slice(5) } </span>
          </div>
          ))}
</div>
  </div>

{
  Object.keys(data.resolutionBySeverity || {}).length > 0 && (
    <div className="analytics-card" style = {{ marginTop: '20px' }
}>
  <h4>Avg Resolution Time by Severity </h4>
    < div style = {{ marginTop: '16px' }}>
    {
      Object.entries(data.resolutionBySeverity).map(([sev, mins]) => (
        <div key= { sev } className = "analytics-row" >
        <span style={{ color: severityColors[sev] || '#6b7280', fontWeight: 600 }} > { sev } </span>
      < span > { formatMinutes(mins) } </span>
      </div>
            ))}
</div>
  </div>
      )}
</div>
  );
};

export default Analytics;
