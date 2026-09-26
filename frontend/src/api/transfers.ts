import http from './http';
import { InternalTransfer, CreateTransferDto, TransferStatus } from '../types/transfer';

export const transferApi = {
  async getAll(status?: TransferStatus, productId?: number, locationId?: number): Promise<InternalTransfer[]> {
    const params: Record<string, string> = {};
    if (status) params.status = status;
    if (productId) params.productId = productId.toString();
    if (locationId) params.locationId = locationId.toString();

    const res = await http.get<InternalTransfer[]>('/transfers', { params });
    return res.data;
  },

  async getById(id: number): Promise<InternalTransfer> {
    const res = await http.get<InternalTransfer>(`/transfers/${id}`);
    return res.data;
  },

  async create(data: CreateTransferDto): Promise<InternalTransfer> {
    const res = await http.post<InternalTransfer>('/transfers', data);
    return res.data;
  },

  async execute(id: number): Promise<InternalTransfer> {
    const res = await http.post<InternalTransfer>(`/transfers/${id}/execute`);
    return res.data;
  },

  async cancel(id: number): Promise<InternalTransfer> {
    const res = await http.post<InternalTransfer>(`/transfers/${id}/cancel`);
    return res.data;
  },
};
