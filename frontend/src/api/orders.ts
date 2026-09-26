import http from './http';
import {
  ReceiptOrder,
  CreateReceiptOrderDto,
  DeliveryOrder,
  CreateDeliveryOrderDto,
  OrderStatus,
} from '../types/order';

export const receiptOrderApi = {
  async getAll(status?: OrderStatus, page = 0, size = 20): Promise<ReceiptOrder[]> {
    const params: Record<string, string> = {
      page: page.toString(),
      size: size.toString(),
    };
    if (status) params.status = status;
    const res = await http.get<ReceiptOrder[]>('/receipt-orders', { params });
    return res.data;
  },

  async getById(id: number): Promise<ReceiptOrder> {
    const res = await http.get<ReceiptOrder>(`/receipt-orders/${id}`);
    return res.data;
  },

  async create(data: CreateReceiptOrderDto): Promise<ReceiptOrder> {
    const res = await http.post<ReceiptOrder>('/receipt-orders', data);
    return res.data;
  },

  async updateStatus(id: number, status: OrderStatus): Promise<ReceiptOrder> {
    const res = await http.put<ReceiptOrder>(`/receipt-orders/${id}/status`, { status });
    return res.data;
  },

  async updateLineQuantity(id: number, lineId: number, receivedQuantity: number): Promise<ReceiptOrder> {
    const res = await http.put<ReceiptOrder>(`/receipt-orders/${id}/lines/${lineId}`, { receivedQuantity });
    return res.data;
  },
};

export const deliveryOrderApi = {
  async getAll(status?: OrderStatus, page = 0, size = 20): Promise<DeliveryOrder[]> {
    const params: Record<string, string> = {
      page: page.toString(),
      size: size.toString(),
    };
    if (status) params.status = status;
    const res = await http.get<DeliveryOrder[]>('/delivery-orders', { params });
    return res.data;
  },

  async getById(id: number): Promise<DeliveryOrder> {
    const res = await http.get<DeliveryOrder>(`/delivery-orders/${id}`);
    return res.data;
  },

  async create(data: CreateDeliveryOrderDto): Promise<DeliveryOrder> {
    const res = await http.post<DeliveryOrder>('/delivery-orders', data);
    return res.data;
  },

  async updateStatus(id: number, status: OrderStatus): Promise<DeliveryOrder> {
    const res = await http.put<DeliveryOrder>(`/delivery-orders/${id}/status`, { status });
    return res.data;
  },

  async updateLineProgress(
    id: number,
    lineId: number,
    data: { picked?: boolean; packed?: boolean; deliveredQuantity?: number }
  ): Promise<DeliveryOrder> {
    const res = await http.put<DeliveryOrder>(`/delivery-orders/${id}/lines/${lineId}`, data);
    return res.data;
  },
};
