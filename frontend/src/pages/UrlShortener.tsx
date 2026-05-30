import React, { useEffect, useState } from 'react';
import { shortenUrl, getMyUrls, deleteShortUrl } from '../services/api';

interface ShortUrl {
  id: number;
  shortCode: string;
  originalUrl: string;
  createdAt: string;
  expiresAt: string | null;
  clickCount: number;
}

const UrlShortener: React.FC = () => {
  const [urls, setUrls] = useState<ShortUrl[]>([]);
  const [originalUrl, setOriginalUrl] = useState('');
  const [expiresInDays, setExpiresInDays] = useState<number>(30);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [copied, setCopied] = useState<string | null>(null);

  useEffect(() => {
    loadUrls();
  }, []);

  const loadUrls = async () => {
    try {
      const response = await getMyUrls();
      setUrls(response.data);
    } catch (err) {
      console.error('Failed to load URLs', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!originalUrl.trim()) return;
    setCreating(true);
    try {
      await shortenUrl(originalUrl, expiresInDays);
      setOriginalUrl('');
      await loadUrls();
    } catch (err: any) {
      alert(err.response?.data?.error || 'Failed to create share link');
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (shortCode: string) => {
    if (!window.confirm('Delete this share link?')) return;
    try {
      await deleteShortUrl(shortCode);
      await loadUrls();
    } catch (err) {
      alert('Failed to delete link');
    }
  };

  const copyToClipboard = (shortCode: string) => {
    const shortUrl = window.location.origin.replace(':3000', ':8080') + '/s/' + shortCode;
    navigator.clipboard.writeText(shortUrl);
    setCopied(shortCode);
    setTimeout(() => setCopied(null), 2000);
  };

  const getShortUrl = (shortCode: string) => {
    return window.location.origin.replace(':3000', ':8080') + '/s/' + shortCode;
  };

  if (loading) return <div style={{ padding: '40px', textAlign: 'center' }}>Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h2>Share Links</h2>
        <span style={{ color: '#6b7280', fontSize: '0.9rem' }}>Create short links for incident reports to share on Slack, Teams & Email</span>
      </div>

      <div className="detail-card" style={{ marginBottom: '24px' }}>
        <h4 style={{ marginBottom: '12px' }}>Create Share Link</h4>
        <p style={{ color: '#6b7280', fontSize: '0.85rem', marginBottom: '12px' }}>
          Paste any incident URL or report link to generate a short, shareable link for team communication.
        </p>
        <form onSubmit={handleCreate} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <input
            type="url"
            value={originalUrl}
            onChange={(e) => setOriginalUrl(e.target.value)}
            placeholder="https://incidenthub.com/incidents/42 or any report URL"
            required
            style={{ flex: 2, minWidth: '300px', padding: '10px', background: '#0f1419', border: '1px solid #374151', borderRadius: '8px', color: '#e7e9ea' }}
          />
          <select
            value={expiresInDays}
            onChange={(e) => setExpiresInDays(Number(e.target.value))}
            style={{ padding: '10px', background: '#0f1419', border: '1px solid #374151', borderRadius: '8px', color: '#e7e9ea' }}
          >
            <option value={7}>Expires in 7 days</option>
            <option value={30}>Expires in 30 days</option>
            <option value={90}>Expires in 90 days</option>
            <option value={365}>Expires in 1 year</option>
          </select>
          <button type="submit" className="btn btn-primary" disabled={creating}>
            {creating ? 'Creating...' : 'Create Link'}
          </button>
        </form>
      </div>

      <div className="detail-card">
        <h4 style={{ marginBottom: '16px' }}>My Share Links ({urls.length})</h4>
        {urls.length === 0 ? (
          <p style={{ color: '#6b7280' }}>No share links yet. Create one above to share incident details with your team!</p>
        ) : (
          <div className="incidents-table">
            <table>
              <thead>
                <tr>
                  <th>Short Link</th>
                  <th>Original URL</th>
                  <th>Views</th>
                  <th>Created</th>
                  <th>Expires</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {urls.map((url) => (
                  <tr key={url.id}>
                    <td>
                      <a href={getShortUrl(url.shortCode)} target="_blank" rel="noopener noreferrer" style={{ color: '#60a5fa' }}>
                        /s/{url.shortCode}
                      </a>
                    </td>
                    <td style={{ maxWidth: '250px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {url.originalUrl}
                    </td>
                    <td><span style={{ fontWeight: 600, color: '#10b981' }}>{url.clickCount}</span></td>
                    <td style={{ fontSize: '0.8rem', color: '#6b7280' }}>{new Date(url.createdAt).toLocaleDateString()}</td>
                    <td style={{ fontSize: '0.8rem', color: '#6b7280' }}>{url.expiresAt ? new Date(url.expiresAt).toLocaleDateString() : 'Never'}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '6px' }}>
                        <button className="btn btn-sm" onClick={() => copyToClipboard(url.shortCode)} style={{ background: '#1d4ed8', color: '#fff', padding: '4px 10px' }}>
                          {copied === url.shortCode ? 'Copied!' : 'Copy'}
                        </button>
                        <button className="btn btn-sm" onClick={() => handleDelete(url.shortCode)} style={{ background: '#dc2626', color: '#fff', padding: '4px 10px' }}>
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default UrlShortener;
