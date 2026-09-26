import http from './http';
import { DashboardKpis } from '../types/dashboard';

export const dashboardApi = {
  async getStats(warehouseId?: number, locationId?: number, category?: string): Promise<DashboardKpis> {
    const params: Record<string, string> = {};
    if (warehouseId) params.warehouseId = warehouseId.toString();
    if (locationId) params.locationId = locationId.toString();
    if (category) params.category = category;

    const res = await http.get<DashboardKpis>('/dashboard/stats', { params });
    return res.data;
  },
};
