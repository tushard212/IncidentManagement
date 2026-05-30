import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT token to every request
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401, clear token and reload to force login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('user');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

// Auth
export const login = (username: string, password: string) =>
  api.post('/auth/login', { username, password });

export const register = (data: any) =>
  api.post('/auth/register', data);

// Incidents
export const getIncidents = (page = 0, size = 20) =>
  api.get(`/incidents?page=${page}&size=${size}&sort=createdAt,desc`);

export const getIncident = (id: number) =>
  api.get(`/incidents/${id}`);

export const createIncident = (data: any) =>
  api.post('/incidents', data);

export const updateIncident = (id: number, data: any) =>
  api.put(`/incidents/${id}`, data);

export const acknowledgeIncident = (id: number) =>
  api.post(`/incidents/${id}/acknowledge`);

export const updateIncidentStatus = (id: number, status: string) =>
  api.post(`/incidents/${id}/status?status=${status}`);

export const addNote = (id: number, message: string) =>
  api.post(`/incidents/${id}/notes`, { message });

export const deleteIncident = (id: number) =>
  api.delete(`/incidents/${id}`);

export const getDashboardStats = () =>
  api.get('/incidents/dashboard/stats');

export const getIncidentsByStatus = (status: string, page = 0) =>
  api.get(`/incidents/status/${status}?page=${page}&size=20`);

// Teams
export const getTeams = () => api.get('/teams');
export const getTeam = (id: number) => api.get(`/teams/${id}`);
export const createTeam = (data: any) => api.post('/teams', data);

// On-Call
export const getTeamSchedules = (teamId: number) =>
  api.get(`/oncall/team/${teamId}`);

export const getCurrentOnCall = (teamId: number) =>
  api.get(`/oncall/team/${teamId}/current`);

// Notifications (Admin/Manager)
export const getNotifications = (page = 0, size = 5) =>
  api.get(`/notifications?page=${page}&size=${size}`);

export const getNotificationStats = () =>
  api.get('/notifications/stats');

// Attachments
export const getAttachments = (incidentId: number) =>
  api.get(`/incidents/${incidentId}/attachments`);

export const uploadAttachment = (incidentId: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post(`/incidents/${incidentId}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const downloadAttachment = (incidentId: number, attachmentId: number) =>
  api.get(`/incidents/${incidentId}/attachments/${attachmentId}/download`, { responseType: 'blob' });

export const deleteAttachment = (incidentId: number, attachmentId: number) =>
  api.delete(`/incidents/${incidentId}/attachments/${attachmentId}`);

// URL Shortener
export const shortenUrl = (originalUrl: string, expiresInDays?: number) =>
  api.post('/urls/shorten', { url: originalUrl, expiryDays: expiresInDays });

export const getMyUrls = () =>
  api.get('/urls');

export const getUrlStats = (shortCode: string) =>
  api.get(`/urls/${shortCode}/stats`);

export const deleteShortUrl = (shortCode: string) =>
  api.delete(`/urls/${shortCode}`);

export default api;
