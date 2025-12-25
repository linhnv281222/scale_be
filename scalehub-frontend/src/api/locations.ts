import apiClient from './client';
import type { Location, LocationTree, CreateLocationRequest, UpdateLocationRequest } from '../types';

export const locationsApi = {
  getAllLocations: async (): Promise<Location[]> => {
    const response = await apiClient.get('/locations');
    return response.data.data;
  },

  getLocationsTree: async (): Promise<LocationTree[]> => {
    const response = await apiClient.get('/locations/tree');
    return response.data.data;
  },

  getLocationById: async (id: number): Promise<Location> => {
    const response = await apiClient.get(`/locations/${id}`);
    return response.data.data;
  },

  createLocation: async (data: CreateLocationRequest): Promise<Location> => {
    const response = await apiClient.post('/locations', data);
    return response.data.data;
  },

  updateLocation: async (id: number, data: UpdateLocationRequest): Promise<Location> => {
    const response = await apiClient.put(`/locations/${id}`, data);
    return response.data.data;
  },

  deleteLocation: async (id: number): Promise<void> => {
    await apiClient.delete(`/locations/${id}`);
  },
};
