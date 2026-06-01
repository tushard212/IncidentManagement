export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: 'ADMIN' | 'MANAGER' | 'ENGINEER';
  teamId?: string;
  isOnCall: boolean;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
  userId: string;
}

export interface Incident {
  id: string;
  title: string;
  description: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'ACKNOWLEDGED' | 'INVESTIGATING' | 'RESOLVED' | 'CLOSED';
  assigneeName: string | null;
  assigneeId: string | null;
  reporterName: string;
  reporterId: string;
  teamName: string | null;
  teamId: string | null;
  service: string;
  createdAt: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
  closedAt: string | null;
  slaDeadline: string;
  slaBreached: boolean;
  escalationLevel: number;
  timeline: TimelineEntry[];
}

export interface TimelineEntry {
  id: string;
  action: string;
  message: string;
  performedByName: string;
  createdAt: string;
}

export interface DashboardStats {
  totalOpen: number;
  totalAcknowledged: number;
  totalInvestigating: number;
  totalResolved: number;
  totalClosed: number;
  slaBreachedCount: number;
  severityCounts: { severity: string; count: number }[];
}

export interface Team {
  id: string;
  name: string;
  description: string;
  memberCount: number;
  members: TeamMember[];
  createdAt: string;
}

export interface TeamMember {
  id: string;
  username: string;
  fullName: string;
  role: string;
  isOnCall: boolean;
}

export interface WebSocketMessage {
  type: 'INCIDENT_CREATED' | 'INCIDENT_UPDATED' | 'INCIDENT_ESCALATED' | 'SLA_BREACHED';
  data: Incident;
}
