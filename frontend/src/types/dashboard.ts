export interface DashboardKpis {
  totalProducts: number;
  totalProductsInStock: number;
  totalStockUnits: number;
  lowStockProducts: number;
  outOfStockProducts: number;
  pendingReceipts: number;
  pendingDeliveries: number;
  scheduledTransfers: number;
}
