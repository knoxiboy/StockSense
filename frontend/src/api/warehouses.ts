import http from './http';
import {
  Warehouse,
  Location,
  CreateWarehouseDto,
  UpdateWarehouseDto,
  CreateLocationDto,
  UpdateLocationDto,
} from '../types/warehouse';

export const warehouseApi = {
  async getAll(): Promise<Warehouse[]> {
    const res = await http.get<Warehouse[]>('/warehouses');
    return res.data;
  },

  async getById(id: number): Promise<Warehouse> {
    const res = await http.get<Warehouse>(`/warehouses/${id}`);
    return res.data;
  },

  async create(data: CreateWarehouseDto): Promise<Warehouse> {
    const res = await http.post<Warehouse>('/warehouses', data);
    return res.data;
  },

  async update(id: number, data: UpdateWarehouseDto): Promise<Warehouse> {
    const res = await http.put<Warehouse>(`/warehouses/${id}`, data);
    return res.data;
  },

  async delete(id: number): Promise<void> {
    await http.delete(`/warehouses/${id}`);
  },
};

export const locationApi = {
  async getAll(warehouseId?: number): Promise<Location[]> {
    const params: Record<string, string> = {};
    if (warehouseId) params.warehouseId = warehouseId.toString();
    const res = await http.get<Location[]>('/locations', { params });
    return res.data;
  },

  async getById(id: number): Promise<Location> {
    const res = await http.get<Location>(`/locations/${id}`);
    return res.data;
  },

  async create(data: CreateLocationDto): Promise<Location> {
    const res = await http.post<Location>('/locations', data);
    return res.data;
  },

  async update(id: number, data: UpdateLocationDto): Promise<Location> {
    const res = await http.put<Location>(`/locations/${id}`, data);
    return res.data;
  },

  async delete(id: number): Promise<void> {
    await http.delete(`/locations/${id}`);
  },
};
