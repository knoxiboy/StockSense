export interface Product {
  id: number;
  name: string;
  sku: string;
  category: string;
  unit: string;
  unitOfMeasure?: string;
  description: string | null;
  reorderLevel: number | string;
  price: number | string | null;
  active?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductDto {
  name: string;
  sku: string;
  category: string;
  unit: string;
  unitOfMeasure?: string;
  description?: string;
  reorderLevel: number | string;
  price?: number | string | null;
}

export interface UpdateProductDto {
  name: string;
  sku: string;
  category: string;
  unit: string;
  unitOfMeasure?: string;
  description?: string;
  reorderLevel: number | string;
  price?: number | string | null;
  active?: boolean;
}

export interface StockBalanceResponse {
  id: number;
  productId: number;
  productName: string;
  sku: string;
  quantity: number | string;
  unit: string;
  reorderLevel: number | string;
  updatedAt: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}
