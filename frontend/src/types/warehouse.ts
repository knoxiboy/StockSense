export type LocationType = 'INTERNAL' | 'SUPPLIER' | 'CUSTOMER' | 'TRANSIT' | 'INVENTORY_LOSS';

export interface Warehouse {
  id: number;
  name: string;
  code: string;
  address: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Location {
  id: number;
  name: string;
  code: string;
  warehouseId: number;
  warehouseName?: string;
  warehouseCode?: string;
  locationType: LocationType;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWarehouseDto {
  name: string;
  code: string;
  address?: string;
}

export interface UpdateWarehouseDto {
  name: string;
  code: string;
  address?: string;
  active?: boolean;
}

export interface CreateLocationDto {
  name: string;
  code: string;
  warehouseId: number;
  locationType?: LocationType;
}

export interface UpdateLocationDto {
  name: string;
  code: string;
  warehouseId: number;
  locationType?: LocationType;
  active?: boolean;
}
