export type OperationType = 'RECEIPT' | 'DELIVERY' | 'ADJUSTMENT';
export type OperationStatus = 'DRAFT' | 'WAITING' | 'READY' | 'DONE' | 'CANCELLED';

export interface OperationItem {
  id?: number;
  productId: number;
  productName?: string;
  sku?: string;
  quantity: number | string;
}

export interface StockOperation {
  id: number;
  referenceNumber: string;
  type: OperationType;
  status: OperationStatus;
  notes?: string;
  items: OperationItem[];
  createdAt: string;
  completedAt?: string;
}
