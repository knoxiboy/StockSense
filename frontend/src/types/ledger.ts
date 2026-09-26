import { OperationType } from './operation';

export interface StockLedgerEntry {
  id: number;
  operationId: number;
  productId: number;
  productName: string;
  sku: string;
  unit: string;
  operationType: OperationType;
  quantityChange: number;
  previousQuantity: number;
  resultingQuantity: number;
  reference: string | null;
  notes: string | null;
  createdAt: string;
}

export type StockLedgerEntryResponse = StockLedgerEntry;

export interface LedgerFilterParams {
  productId?: number;
  type?: OperationType;
  from?: string; // YYYY-MM-DD
  to?: string;   // YYYY-MM-DD
  page?: number;
  size?: number;
}

export interface PagedResult<T> {
  items: T[];
  totalCount: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}
