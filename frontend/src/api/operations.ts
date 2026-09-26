import apiClient from './client';
import {
  StockOperationResponse,
  CreateReceiptRequest,
  CreateDeliveryRequest,
  CreateAdjustmentRequest,
  OperationFilterParams,
} from '../types/operation';

export const operationApi = {
  async createReceipt(data: CreateReceiptRequest): Promise<StockOperationResponse> {
    const response = await apiClient.post<StockOperationResponse>('/operations/receipts', data);
    return response.data;
  },

  async createDelivery(data: CreateDeliveryRequest): Promise<StockOperationResponse> {
    const response = await apiClient.post<StockOperationResponse>('/operations/deliveries', data);
    return response.data;
  },

  async createAdjustment(data: CreateAdjustmentRequest): Promise<StockOperationResponse> {
    const response = await apiClient.post<StockOperationResponse>('/operations/adjustments', data);
    return response.data;
  },

  async getById(id: number): Promise<StockOperationResponse> {
    const response = await apiClient.get<StockOperationResponse>(`/operations/${id}`);
    return response.data;
  },

  async getAll(params?: OperationFilterParams): Promise<StockOperationResponse[]> {
    const queryParams: Record<string, string | number> = {};
    if (params?.productId) queryParams.productId = params.productId;
    if (params?.type) queryParams.type = params.type;
    if (params?.page !== undefined) queryParams.page = params.page;
    if (params?.size !== undefined) queryParams.size = params.size;

    const response = await apiClient.get<StockOperationResponse[]>('/operations', {
      params: queryParams,
    });
    return response.data;
  },
};

export default operationApi;
