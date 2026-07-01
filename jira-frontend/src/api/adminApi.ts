import apiClient from './axiosClient';

export interface SystemSettings {
  applicationTitle: string;
  baseUrl: string;
  adminEmail: string;
  dateFormat: string;
  timeZone: string;
  language: string;
  allowSignUp: boolean;
  requireEmailVerification: boolean;
  enableTwoFactor: boolean;
  passwordMinLength: number;
  sessionTimeout: number;
  enableApiTokens: boolean;
  emailEnabled: boolean;
  smtpHost: string;
  smtpPort: number;
  smtpUsername: string;
  smtpSsl: boolean;
  emailFrom: string;
  maxAttachmentSize: number;
  allowedAttachmentTypes: string[];
  apiEnabled: boolean;
  apiRateLimit: number;
  logLevel: string;
  auditLogging: boolean;
}

export interface User {
  id: string;
  username: string;
  email: string;
  displayName: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'PENDING';
  role: string;
  createdAt: string;
  lastLogin?: string;
  activeProjects: number;
  emailVerified: boolean;
}

export interface UserStatistics {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  suspendedUsers: number;
  newUsersThisMonth: number;
  newUsersThisWeek: number;
  usersByRole: Record<string, number>;
  usersByStatus: Record<string, number>;
}

export interface ProjectSettings {
  id: string;
  projectKey: string;
  name: string;
  description: string;
  type: string;
  status: string;
  leadUsername: string;
  allowSubTasks: boolean;
  allowAttachments: boolean;
  allowComments: boolean;
  maxAttachments: number;
  enableNotifications: boolean;
  notificationEvents: string[];
  createdAt: string;
  updatedAt: string;
}

export interface AppearanceSettings {
  logoUrl: string;
  faviconUrl: string;
  appName: string;
  useSystemFont: boolean;
  loginPageMessage: string;
  footerMessage: string;
  theme?: {
    mode: string;
    primaryColor: string;
    secondaryColor: string;
    accentColor: string;
    backgroundColor: string;
    textColor: string;
  };
  colorScheme?: {
    name: string;
    primary: string;
    secondary: string;
    success: string;
    warning: string;
    danger: string;
    info: string;
    light: string;
    dark: string;
  };
  fonts?: {
    primaryFont: string;
    monospaceFont: string;
    headingFont: string;
    baseFontSize: string;
  };
}

export interface License {
  licenseType: string;
  maxUsers: number;
  currentUsers: number;
  maxProjects: number;
  currentProjects: number;
  purchaseDate: string;
  expiryDate: string;
  isValid: boolean;
  isExpired: boolean;
  daysUntilExpiry: number;
  supportEntitlement: string;
}

export interface SystemHealth {
  isHealthy: boolean;
  uptime: number;
  metrics: {
    totalRequests: number;
    activeUsers: number;
    totalIssues: number;
    totalProjects: number;
    avgResponseTime: number;
    cpuUsage: number;
  };
  services: Array<{
    name: string;
    isRunning: boolean;
    status: string;
    lastChecked: string;
  }>;
  diskUsage: {
    total: number;
    used: number;
    available: number;
    percentageUsed: number;
  };
  memoryUsage: {
    total: number;
    used: number;
    available: number;
    percentageUsed: number;
  };
}

export const adminApi = {
  // System Settings
  getSettings: () => apiClient.get<SystemSettings>('/admin/settings'),
  updateSettings: (data: Partial<SystemSettings>) => apiClient.put<SystemSettings>('/admin/settings', data),

  // Users
  getUsers: (params?: { search?: string; status?: string; role?: string }) =>
    apiClient.get<User[]>('/admin/users', { params }),
  createUser: (data: { username: string; email: string; displayName?: string; role?: string }) =>
    apiClient.post<User>('/admin/users', data),
  updateUser: (userId: string, data: Partial<User>) =>
    apiClient.put<User>(`/admin/users/${userId}`, data),
  deleteUser: (userId: string) => apiClient.delete(`/admin/users/${userId}`),
  getUserStatistics: () => apiClient.get<UserStatistics>('/admin/users/statistics'),

  // Projects
  getProjects: () => apiClient.get<ProjectSettings[]>('/admin/projects'),
  getProject: (projectId: string) => apiClient.get<ProjectSettings>(`/admin/projects/${projectId}`),
  updateProject: (projectId: string, data: Partial<ProjectSettings>) =>
    apiClient.put<ProjectSettings>(`/admin/projects/${projectId}`, data),

  // Appearance
  getAppearance: () => apiClient.get<AppearanceSettings>('/admin/appearance'),
  updateAppearance: (data: Partial<AppearanceSettings>) =>
    apiClient.put<AppearanceSettings>('/admin/appearance', data),

  // Licensing
  getLicense: () => apiClient.get<License>('/admin/license'),

  // System Health
  getHealth: () => apiClient.get<SystemHealth>('/admin/health'),
};