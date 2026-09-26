export interface Product {
  id: number;
  name: string;
  sku: string;
  category: string | null;
  description: string | null;
  unitOfMeasure: string;
  reorderLevel: number | string;
  price: number | string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductDto {
  name: string;
  sku: string;
  category?: string;
  description?: string;
  unitOfMeasure: string;
  reorderLevel: number | string;
  price?: number | string | null;
}

export interface UpdateProductDto {
  name: string;
  sku: string;
  category?: string;
  description?: string;
  unitOfMeasure: string;
  reorderLevel: number | string;
  price?: number | string | null;
  active?: boolean;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}
