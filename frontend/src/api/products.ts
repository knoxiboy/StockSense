import http from './http';
import { Product, CreateProductDto, UpdateProductDto } from '../types/product';

export const productApi = {
  async getAll(search?: string, category?: string): Promise<Product[]> {
    const params: Record<string, string> = {};
    if (search) params.search = search;
    if (category) params.category = category;
    const response = await http.get<Product[]>('/products', { params });
    return response.data;
  },

  async getById(id: number): Promise<Product> {
    const response = await http.get<Product>(`/products/${id}`);
    return response.data;
  },

  async getBySku(sku: string): Promise<Product> {
    const response = await http.get<Product>(`/products/sku/${encodeURIComponent(sku)}`);
    return response.data;
  },

  async getCategories(): Promise<string[]> {
    const response = await http.get<string[]>('/products/categories');
    return response.data;
  },

  async create(data: CreateProductDto): Promise<Product> {
    const response = await http.post<Product>('/products', data);
    return response.data;
  },

  async update(id: number, data: UpdateProductDto): Promise<Product> {
    const response = await http.put<Product>(`/products/${id}`, data);
    return response.data;
  },

  async delete(id: number): Promise<void> {
    await http.delete(`/products/${id}`);
  },
};
