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
  warehouseId?: number | null;
  warehouseName?: string | null;
  warehouseCode?: string | null;
  locationId?: number | null;
  locationName?: string | null;
  locationCode?: string | null;
  sourceLocationId?: number | null;
  sourceLocationName?: string | null;
  sourceLocationCode?: string | null;
  destinationLocationId?: number | null;
  destinationLocationName?: string | null;
  destinationLocationCode?: string | null;
  createdAt: string;
}

export type StockLedgerEntryResponse = StockLedgerEntry;

export interface LedgerFilterParams {
  productId?: number;
  type?: OperationType;
  from?: string; // YYYY-MM-DD
  to?: string;   // YYYY-MM-DD
  locationId?: number;
  warehouseId?: number;
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
