import apiClient from './axiosClient';

export interface SavedFilter {
  id: string;
  name: string;
  jql: string;
  owner: string;
  isShared: boolean;
  favorite: boolean;
  shareType: string;
  isSystem?: boolean;
  usageCount?: number;
  lastUsed?: string;
  createdAt?: string;
}

export interface FilterSubscription {
  id: string;
  userId: string;
  filterName: string;
  jqlQuery: string;
  frequency: 'INSTANT' | 'DAILY' | 'WEEKLY';
  isActive: boolean;
  emailNotification: boolean;
  lastNotified?: string;
  createdAt: string;
}

export const filterApi = {
  getSavedFilters: (tab: 'my' | 'shared' | 'system' = 'my') =>
    apiClient.get<SavedFilter[]>('/filters', { params: { tab } }),

  createFilter: (data: { name: string; jql: string; isShared?: boolean }) =>
    apiClient.post<SavedFilter>('/filters', data),

  deleteFilter: (filterId: string) =>
    apiClient.delete(`/filters/${filterId}`),

  toggleFavorite: (filterId: string) =>
    apiClient.post<SavedFilter>(`/filters/${filterId}/favorite`),

  getSubscriptions: () =>
    apiClient.get<FilterSubscription[]>('/filters/subscriptions'),

  createSubscription: (data: {
    filterName: string;
    jql: string;
    frequency?: 'INSTANT' | 'DAILY' | 'WEEKLY';
    emailNotification?: boolean;
  }) =>
    apiClient.post<FilterSubscription>('/filters/subscriptions', data),

  deleteSubscription: (subscriptionId: string) =>
    apiClient.delete(`/filters/subscriptions/${subscriptionId}`),

  toggleSubscription: (subscriptionId: string) =>
    apiClient.post<FilterSubscription>(`/filters/subscriptions/${subscriptionId}/toggle`),
};