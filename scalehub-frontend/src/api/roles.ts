import apiClient from './client';
import type { Role, CreateRoleRequest, UpdateRoleRequest, Permission } from '../types';

export const rolesApi = {
  getAllRoles: async (): Promise<Role[]> => {
    const response = await apiClient.get('/roles');
    return response.data.data;
  },

  getRoleById: async (id: number): Promise<Role> => {
    const response = await apiClient.get(`/roles/${id}`);
    return response.data.data;
  },

  createRole: async (data: CreateRoleRequest): Promise<Role> => {
    const response = await apiClient.post('/roles', data);
    return response.data.data;
  },

  updateRole: async (id: number, data: UpdateRoleRequest): Promise<Role> => {
    const response = await apiClient.put(`/roles/${id}`, data);
    return response.data.data;
  },

  deleteRole: async (id: number): Promise<void> => {
    await apiClient.delete(`/roles/${id}`);
  },
};

export const permissionsApi = {
  getAllPermissions: async (): Promise<Permission[]> => {
    const response = await apiClient.get('/permissions');
    return response.data.data;
  },
};
