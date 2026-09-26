import React, { useState, useEffect, useCallback } from 'react';
import {
  RotateCcw,
  RefreshCw,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Eye,
  CheckCircle2,
  MapPin,
  ArrowRight,
} from 'lucide-react';
import { ledgerApi } from '../api/ledger';
import { productApi } from '../api/products';
import { warehouseApi, locationApi } from '../api/warehouses';
import { StockLedgerEntry, LedgerFilterParams } from '../types/ledger';
import { Product } from '../types/product';
import { Warehouse, Location } from '../types/warehouse';
import { OperationType } from '../types/operation';
import Badge from '../components/ui/Badge';
import Modal from '../components/ui/Modal';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import { formatQuantity, formatDate } from '../utils/formatters';

interface LedgerPageProps {
  onSuccessToast?: (msg: string) => void;
  onErrorToast?: (msg: string) => void;
}

export const LedgerPage: React.FC<LedgerPageProps> = ({
  onErrorToast = () => {},
}) => {
  const [entries, setEntries] = useState<StockLedgerEntry[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [selectedProductId, setSelectedProductId] = useState<number | ''>('');
  const [selectedType, setSelectedType] = useState<OperationType | ''>('');
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<number | ''>('');
  const [selectedLocationId, setSelectedLocationId] = useState<number | ''>('');
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');
  const [dateError, setDateError] = useState<string | null>(null);

  // Pagination (0-based)
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);
  const [totalCount, setTotalCount] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);

  // Detail Modal
  const [selectedEntry, setSelectedEntry] = useState<StockLedgerEntry | null>(null);

  // Load products, warehouses, and locations
  useEffect(() => {
    productApi.getAll().then(setProducts).catch(() => {});
    warehouseApi.getAll().then(setWarehouses).catch(() => {});
    locationApi.getAll().then(setLocations).catch(() => {});
  }, []);

  const loadLedgerEntries = useCallback(async () => {
    if (fromDate && toDate && fromDate > toDate) {
      setDateError("'From' date cannot be after 'To' date");
      return;
    }
    setDateError(null);
    setLoading(true);
    setError(null);

    try {
      const params: LedgerFilterParams = {
        page,
        size: pageSize,
      };
      if (selectedProductId !== '') params.productId = Number(selectedProductId);
      if (selectedType !== '') params.type = selectedType as OperationType;
      if (selectedWarehouseId !== '') params.warehouseId = Number(selectedWarehouseId);
      if (selectedLocationId !== '') params.locationId = Number(selectedLocationId);
      if (fromDate) params.from = fromDate;
      if (toDate) params.to = toDate;

      const result = await ledgerApi.getAll(params);
      setEntries(result.items);
      setTotalCount(result.totalCount);
      setTotalPages(result.totalPages);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load ledger records';
      setError(msg);
      onErrorToast(msg);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, selectedProductId, selectedType, selectedWarehouseId, selectedLocationId, fromDate, toDate, onErrorToast]);

  useEffect(() => {
    loadLedgerEntries();
  }, [loadLedgerEntries]);

  const handleResetFilters = () => {
    setSelectedProductId('');
    setSelectedType('');
    setSelectedWarehouseId('');
    setSelectedLocationId('');
    setFromDate('');
    setToDate('');
    setDateError(null);
    setPage(0);
  };

  const isFilterActive =
    selectedProductId !== '' ||
    selectedType !== '' ||
    selectedWarehouseId !== '' ||
    selectedLocationId !== '' ||
    fromDate !== '' ||
    toDate !== '';

  const startRecord = totalCount === 0 ? 0 : page * pageSize + 1;
  const endRecord = Math.min((page + 1) * pageSize, totalCount);

  return (
    <div className="ledger-page">
      {/* Filter and Query Toolbar */}
      <div className="card toolbar-card" style={{ padding: '16px 20px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          {/* Product Filter */}
          <select
            className="filter-select"
            value={selectedProductId}
            onChange={(e) => {
              setSelectedProductId(e.target.value ? Number(e.target.value) : '');
              setPage(0);
            }}
          >
            <option value="">All Products</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.sku})
              </option>
            ))}
          </select>

          {/* Operation Type Filter */}
          <select
            className="filter-select"
            value={selectedType}
            onChange={(e) => {
              setSelectedType(e.target.value as OperationType | '');
              setPage(0);
            }}
          >
            <option value="">All Operation Types</option>
            <option value="RECEIPT">Receipts (Inbound)</option>
            <option value="DELIVERY">Deliveries (Outbound)</option>
            <option value="TRANSFER">Internal Transfers (Shift)</option>
            <option value="ADJUSTMENT">Adjustments (Counts)</option>
          </select>

          {/* Warehouse Filter */}
          <select
            className="filter-select"
            value={selectedWarehouseId}
            onChange={(e) => {
              setSelectedWarehouseId(e.target.value ? Number(e.target.value) : '');
              setPage(0);
            }}
          >
            <option value="">All Warehouses</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name} ({w.code})
              </option>
            ))}
          </select>

          {/* Location Filter */}
          <select
            className="filter-select"
            value={selectedLocationId}
            onChange={(e) => {
              setSelectedLocationId(e.target.value ? Number(e.target.value) : '');
              setPage(0);
            }}
          >
            <option value="">All Locations</option>
            {locations.map((l) => (
              <option key={l.id} value={l.id}>
                {l.name} ({l.code})
              </option>
            ))}
          </select>

          {/* Date Range: From */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>From:</span>
            <input
              type="date"
              className="filter-select"
              style={{ padding: '7px 10px' }}
              value={fromDate}
              onChange={(e) => {
                setFromDate(e.target.value);
                setPage(0);
              }}
            />
          </div>

          {/* Date Range: To */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>To:</span>
            <input
              type="date"
              className="filter-select"
              style={{ padding: '7px 10px' }}
              value={toDate}
              onChange={(e) => {
                setToDate(e.target.value);
                setPage(0);
              }}
            />
          </div>

          {isFilterActive && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={handleResetFilters}
            >
              <RotateCcw size={14} />
              <span>Clear Filters</span>
            </button>
          )}

          {isFilterActive && (
            <span className="badge badge-info" style={{ fontSize: '0.75rem' }}>
              Filters Active
            </span>
          )}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginTop: '12px' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={loadLedgerEntries}
            title="Refresh Ledger"
          >
            <RefreshCw size={14} />
            <span>Refresh Ledger</span>
          </button>
        </div>
      </div>

      {dateError && (
        <div
          style={{
            background: 'var(--danger-bg)',
            border: '1px solid var(--danger-border)',
            borderRadius: 'var(--radius-md)',
            padding: '10px 16px',
            marginBottom: '16px',
            color: 'var(--danger)',
            fontSize: '0.85rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <AlertTriangle size={16} />
          <span>{dateError}</span>
        </div>
      )}

      {/* Ledger Table Card */}
      <div className="card">
        {loading ? (
          <div style={{ padding: '60px 0', display: 'flex', justifyContent: 'center' }}>
            <LoadingSpinner message="Querying immutable stock ledger..." />
          </div>
        ) : error ? (
          <div style={{ padding: '40px 20px', textAlign: 'center' }}>
            <div style={{ color: 'var(--danger)', marginBottom: '8px' }}>
              <AlertTriangle size={32} style={{ margin: '0 auto' }} />
            </div>
            <p style={{ fontWeight: 600 }}>Error loading stock ledger</p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px' }}>{error}</p>
            <button type="button" className="btn btn-secondary btn-sm" onClick={loadLedgerEntries}>
              <RefreshCw size={14} />
              <span>Retry Query</span>
            </button>
          </div>
        ) : entries.length === 0 ? (
          <div style={{ padding: '40px 20px' }}>
            <EmptyState
              title={
                isFilterActive
                  ? 'No ledger records match the selected filters'
                  : 'Immutable stock ledger is empty'
              }
              description={
                isFilterActive
                  ? 'Try clearing or widening your product, location, or date filters.'
                  : 'Every verified stock receipt, delivery, transfer, and adjustment will create an immutable audit record here.'
              }
              actionText={isFilterActive ? 'Clear Filters' : undefined}
              onAction={isFilterActive ? handleResetFilters : undefined}
            />
          </div>
        ) : (
          <>
            <div className="table-container">
              <table className="custom-table">
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>Product & SKU</th>
                    <th>Operation</th>
                    <th>Warehouse / Location</th>
                    <th>Previous Stock</th>
                    <th>Change</th>
                    <th>Resulting Stock</th>
                    <th>Reference</th>
                    <th style={{ textAlign: 'right' }}>Audit</th>
                  </tr>
                </thead>
                <tbody>
                  {entries.map((entry) => {
                    const isPositive = entry.quantityChange > 0;
                    const isZero = entry.quantityChange === 0;

                    let badgeVariant: 'success' | 'info' | 'warning' | 'secondary' = 'info';
                    if (entry.operationType === 'RECEIPT') badgeVariant = 'success';
                    if (entry.operationType === 'ADJUSTMENT') badgeVariant = 'warning';
                    if (entry.operationType === 'TRANSFER') badgeVariant = 'info';

                    return (
                      <tr key={entry.id}>
                        <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          {formatDate(entry.createdAt)}
                        </td>
                        <td>
                          <div style={{ fontWeight: 600 }}>{entry.productName}</div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                            SKU: {entry.sku}
                          </div>
                        </td>
                        <td>
                          <Badge variant={badgeVariant}>{entry.operationType}</Badge>
                        </td>
                        <td>
                          {entry.locationName ? (
                            <div>
                              <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px' }}>
                                <MapPin size={13} style={{ color: 'var(--primary)' }} />
                                {entry.locationName}
                              </div>
                              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>
                                {entry.warehouseName || 'Main Hub'}
                              </div>
                            </div>
                          ) : (
                            <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>General Stock</span>
                          )}

                          {entry.operationType === 'TRANSFER' && (entry.sourceLocationName || entry.destinationLocationName) && (
                            <div style={{ fontSize: '0.72rem', color: 'var(--primary)', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                              <span>{entry.sourceLocationName || 'Src'}</span>
                              <ArrowRight size={11} />
                              <span>{entry.destinationLocationName || 'Dst'}</span>
                            </div>
                          )}
                        </td>
                        <td style={{ fontWeight: 500, color: 'var(--text-muted)' }}>
                          {formatQuantity(entry.previousQuantity, entry.unit)}
                        </td>
                        <td>
                          <span
                            className={
                              isPositive
                                ? 'delta-positive'
                                : isZero
                                ? ''
                                : 'delta-negative'
                            }
                          >
                            {isPositive ? `+${entry.quantityChange}` : entry.quantityChange} {entry.unit}
                          </span>
                        </td>
                        <td style={{ fontWeight: 700, color: 'var(--primary)' }}>
                          {formatQuantity(entry.resultingQuantity, entry.unit)}
                        </td>
                        <td>
                          {entry.reference ? (
                            <span style={{ fontSize: '0.82rem', fontWeight: 500 }}>
                              {entry.reference}
                            </span>
                          ) : (
                            <span style={{ color: 'var(--text-light)' }}>—</span>
                          )}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => setSelectedEntry(entry)}
                            title="Inspect Audit Entry"
                          >
                            <Eye size={14} />
                            <span>Details</span>
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            <div className="pagination-bar">
              <div className="pagination-info">
                Showing <strong>{startRecord}</strong> to <strong>{endRecord}</strong> of{' '}
                <strong>{totalCount}</strong> ledger entries
              </div>

              <div className="pagination-actions">
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Per page:</span>
                <select
                  className="filter-select"
                  style={{ padding: '4px 8px', fontSize: '0.82rem' }}
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(Number(e.target.value));
                    setPage(0);
                  }}
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                  <option value={100}>100</option>
                </select>

                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  <ChevronLeft size={16} />
                  <span>Prev</span>
                </button>

                <span style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                  Page {page + 1} of {totalPages}
                </span>

                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                >
                  <span>Next</span>
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Entry Audit Detail Modal */}
      {selectedEntry && (
        <Modal
          isOpen={!!selectedEntry}
          title={`Audit Entry #${selectedEntry.id}`}
          onClose={() => setSelectedEntry(null)}
          footer={
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setSelectedEntry(null)}
            >
              Close
            </button>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {/* Arithmetic Formula Check Banner */}
            <div
              style={{
                background: '#ecfdf5',
                border: '1px solid #a7f3d0',
                borderRadius: 'var(--radius-md)',
                padding: '14px',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
              }}
            >
              <CheckCircle2 size={20} color="#059669" />
              <div style={{ fontSize: '0.86rem', color: '#065f46' }}>
                <strong>Invariant Verified:</strong> Previous Balance ({selectedEntry.previousQuantity}) + Movement Delta ({selectedEntry.quantityChange > 0 ? `+${selectedEntry.quantityChange}` : selectedEntry.quantityChange}) = Resulting Stock ({selectedEntry.resultingQuantity} {selectedEntry.unit})
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Product
                </label>
                <div style={{ fontWeight: 600, fontSize: '0.95rem' }}>{selectedEntry.productName}</div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  SKU: {selectedEntry.sku}
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Operation Type
                </label>
                <div>
                  <Badge
                    variant={
                      selectedEntry.operationType === 'RECEIPT'
                        ? 'success'
                        : selectedEntry.operationType === 'ADJUSTMENT'
                        ? 'warning'
                        : 'info'
                    }
                  >
                    {selectedEntry.operationType}
                  </Badge>
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Warehouse & Location
                </label>
                <div style={{ fontWeight: 600 }}>
                  {selectedEntry.locationName ? `${selectedEntry.locationName} (${selectedEntry.locationCode})` : 'General Stock'}
                </div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  {selectedEntry.warehouseName ? `${selectedEntry.warehouseName} [${selectedEntry.warehouseCode}]` : 'Central Warehouse'}
                </div>
              </div>

              {selectedEntry.operationType === 'TRANSFER' && (
                <div>
                  <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                    Transfer Routing
                  </label>
                  <div style={{ fontWeight: 600, color: 'var(--primary)', fontSize: '0.85rem' }}>
                    {selectedEntry.sourceLocationName || 'Source'} ➔ {selectedEntry.destinationLocationName || 'Destination'}
                  </div>
                </div>
              )}

              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Linked Operation ID
                </label>
                <div style={{ fontWeight: 500, fontFamily: 'monospace' }}>
                  #{selectedEntry.operationId}
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Recorded Timestamp
                </label>
                <div style={{ fontWeight: 500, fontSize: '0.85rem' }}>
                  {formatDate(selectedEntry.createdAt)}
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Reference Document
                </label>
                <div style={{ fontWeight: 500 }}>
                  {selectedEntry.reference || '—'}
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.75rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Operator Notes
                </label>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-main)' }}>
                  {selectedEntry.notes || '—'}
                </div>
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default LedgerPage;
