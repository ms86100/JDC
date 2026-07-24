import axiosClient from './axiosClient';

export const masterDataApi = {
  getPrograms: () => axiosClient.get('/api/admin/master-data/programs'),
  createProgram: (data: any) => axiosClient.post('/api/admin/master-data/programs', data),
  updateProgram: (id: string, data: any) => axiosClient.put(`/api/admin/master-data/programs/${id}`, data),
  deleteProgram: (id: string) => axiosClient.delete(`/api/admin/master-data/programs/${id}`),
  getTestMeans: (programId: string) => axiosClient.get(`/api/admin/master-data/programs/${programId}/test-means`),
  getSystems: (programId: string) => axiosClient.get(`/api/admin/master-data/programs/${programId}/systems`),
  getAtaChapters: (programId: string) => axiosClient.get(`/api/admin/master-data/programs/${programId}/ata-chapters`),
  getSuppliers: (programId: string, systemId: string) => axiosClient.get(`/api/admin/master-data/programs/${programId}/systems/${systemId}/suppliers`),
  getFunctions: (systemId: string) => axiosClient.get(`/api/admin/master-data/systems/${systemId}/functions`),
  getReporterTeams: (programId?: string) => axiosClient.get(`/api/admin/master-data/reporter-teams${programId ? '?programId=' + programId : ''}`),
  getDefectOrigins: () => axiosClient.get('/api/admin/master-data/defect-origins'),
  getDefectOriginSubItems: (parentId: string) => axiosClient.get(`/api/admin/master-data/defect-origins/${parentId}/sub-items`),
  // CRUD for all sub-types
  createTestMean: (data: any) => axiosClient.post('/api/admin/master-data/test-means', data),
  createSystem: (data: any) => axiosClient.post('/api/admin/master-data/systems', data),
  createSupplier: (data: any) => axiosClient.post('/api/admin/master-data/suppliers', data),
  createFunction: (data: any) => axiosClient.post('/api/admin/master-data/functions', data),
  createReporterTeam: (data: any) => axiosClient.post('/api/admin/master-data/reporter-teams', data),
};
