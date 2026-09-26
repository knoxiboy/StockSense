export interface StockBalance {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  quantityOnHand: number | string;
  reorderLevel: number | string;
  isLowStock: boolean;
  updatedAt: string;
}
