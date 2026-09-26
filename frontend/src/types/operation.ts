export type OperationType = 'RECEIPT' | 'DELIVERY' | 'ADJUSTMENT';

export interface StockOperation {
  id: number;
  operationType: OperationType;
  productId: number;
  productName: string;
  sku: string;
  unit: string;
  quantity: number;
  quantityChange: number;
  resultingQuantity: number;
  reference: string | null;
  notes: string | null;
  createdAt: string;
}

export type StockOperationResponse = StockOperation;

export interface CreateReceiptRequest {
  productId: number;
  quantity: number;
  reference?: string;
  notes?: string;
}

export interface CreateDeliveryRequest {
  productId: number;
  quantity: number;
  reference?: string;
  notes?: string;
}

export interface CreateAdjustmentRequest {
  productId: number;
  countedQuantity: number;
  reference?: string;
  notes?: string;
}

export interface OperationFilterParams {
  productId?: number;
  type?: OperationType;
  page?: number;
  size?: number;
}
