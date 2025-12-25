import apiClient from './client';
import type {
  Scale,
  ScaleConfig,
  CreateScaleRequest,
  UpdateScaleRequest,
  PaginatedResponse,
  ScaleRealtimeData,
} from '../types';

export const scalesApi = {
  getAllScales: async (params?: {
    page?: number;
    size?: number;
    sort?: string;
    locationId?: number;
    status?: string;
  }): Promise<Scale[]> => {
    const response = await apiClient.get('/scales', { params });
    return response.data.data || [];
  },

  getScaleById: async (id: number): Promise<Scale> => {
    const response = await apiClient.get(`/scales/${id}`);
    return response.data.data;
  },

  createScale: async (data: CreateScaleRequest): Promise<Scale> => {
    const response = await apiClient.post('/scales', data);
    return response.data.data;
  },

  updateScale: async (id: number, data: UpdateScaleRequest): Promise<Scale> => {
    const response = await apiClient.put(`/scales/${id}`, data);
    return response.data.data;
  },

  deleteScale: async (id: number): Promise<void> => {
    await apiClient.delete(`/scales/${id}`);
  },

  getScaleConfig: async (scaleId: number): Promise<ScaleConfig> => {
    const response = await apiClient.get(`/scales/${scaleId}/config`);
    return response.data.data;
  },

  updateScaleConfig: async (scaleId: number, data: Partial<ScaleConfig>): Promise<ScaleConfig> => {
    const response = await apiClient.put(`/scales/${scaleId}/config`, data);
    return response.data.data;
  },

  getScaleRealtimeData: async (scaleId: number): Promise<ScaleRealtimeData> => {
    const response = await apiClient.get(`/scales/${scaleId}/realtime`);
    return response.data.data;
  },

  startScale: async (scaleId: number): Promise<void> => {
    await apiClient.post(`/scales/${scaleId}/start`);
  },

  stopScale: async (scaleId: number): Promise<void> => {
    await apiClient.post(`/scales/${scaleId}/stop`);
  },

  getScalesCurrentStates: async (): Promise<any[]> => {
    const response = await apiClient.get('/scales/current-states');
    return response.data.data || [];
  },
};
