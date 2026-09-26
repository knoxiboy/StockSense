import apiClient from './client';
import {
  StockLedgerEntry,
  LedgerFilterParams,
  PagedResult,
} from '../types/ledger';

export const ledgerApi = {
  async getById(id: number): Promise<StockLedgerEntry> {
    const response = await apiClient.get<StockLedgerEntry>(`/ledger/${id}`);
    return response.data;
  },

  async getAll(params?: LedgerFilterParams): Promise<PagedResult<StockLedgerEntry>> {
    const queryParams: Record<string, string | number> = {};
    if (params?.productId) queryParams.productId = params.productId;
    if (params?.type) queryParams.type = params.type;
    if (params?.from) queryParams.from = params.from;
    if (params?.to) queryParams.to = params.to;
    if (params?.page !== undefined) queryParams.page = params.page;
    if (params?.size !== undefined) queryParams.size = params.size;

    const response = await apiClient.get<StockLedgerEntry[]>('/ledger', {
      params: queryParams,
    });

    const headers = response.headers;
    const totalCount = parseInt(
      (headers['x-total-count'] as string) || (headers['X-Total-Count'] as string) || `${response.data.length}`,
      10
    );
    const totalPages = parseInt(
      (headers['x-total-pages'] as string) || (headers['X-Total-Pages'] as string) || '1',
      10
    );
    const currentPage = parseInt(
      (headers['x-current-page'] as string) || (headers['X-Current-Page'] as string) || `${params?.page ?? 0}`,
      10
    );
    const pageSize = parseInt(
      (headers['x-page-size'] as string) || (headers['X-Page-Size'] as string) || `${params?.size ?? 20}`,
      10
    );

    return {
      items: response.data,
      totalCount: isNaN(totalCount) ? response.data.length : totalCount,
      totalPages: isNaN(totalPages) ? 1 : Math.max(totalPages, 1),
      currentPage: isNaN(currentPage) ? 0 : currentPage,
      pageSize: isNaN(pageSize) ? 20 : pageSize,
    };
  },
};

export default ledgerApi;
