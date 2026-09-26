import { Location } from './warehouse';

export type OrderStatus = 'DRAFT' | 'WAITING' | 'READY' | 'DONE' | 'CANCELED';

export interface ReceiptOrderLine {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  expectedQuantity: number | string;
  receivedQuantity: number | string;
}

export interface ReceiptOrder {
  id: number;
  reference: string;
  supplierName: string;
  supplierContact: string | null;
  destinationLocation: Location | null;
  status: OrderStatus;
  notes: string | null;
  completedAt: string | null;
  lines: ReceiptOrderLine[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateReceiptOrderDto {
  reference: string;
  supplierName: string;
  supplierContact?: string;
  destinationLocationId?: number;
  notes?: string;
  lines: {
    productId: number;
    expectedQuantity: number;
    receivedQuantity?: number;
  }[];
}

export interface DeliveryOrderLine {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  orderedQuantity: number | string;
  deliveredQuantity: number | string;
  picked: boolean;
  packed: boolean;
}

export interface DeliveryOrder {
  id: number;
  reference: string;
  customerName: string;
  customerContact: string | null;
  deliveryAddress: string | null;
  sourceLocation: Location | null;
  status: OrderStatus;
  notes: string | null;
  completedAt: string | null;
  lines: DeliveryOrderLine[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateDeliveryOrderDto {
  reference: string;
  customerName: string;
  customerContact?: string;
  deliveryAddress?: string;
  sourceLocationId?: number;
  notes?: string;
  lines: {
    productId: number;
    orderedQuantity: number;
    deliveredQuantity?: number;
    picked?: boolean;
    packed?: boolean;
  }[];
}
