export type TransferStatus = 'DRAFT' | 'READY' | 'DONE' | 'CANCELED';

export interface InternalTransfer {
  id: number;
  reference: string;
  productId: number;
  productName: string;
  sku: string;
  unit: string;
  sourceLocationId: number;
  sourceLocationName: string;
  sourceLocationCode: string;
  sourceWarehouseName?: string;
  destinationLocationId: number;
  destinationLocationName: string;
  destinationLocationCode: string;
  destinationWarehouseName?: string;
  quantity: number | string;
  status: TransferStatus;
  notes: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTransferDto {
  reference?: string;
  productId: number;
  sourceLocationId: number;
  destinationLocationId: number;
  quantity: number;
  notes?: string;
  autoExecute?: boolean;
}
