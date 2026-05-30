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

export default api;
