import React, { useState, useEffect, useCallback } from 'react';
import {
  ArrowDownLeft,
  ArrowUpRight,
  SlidersHorizontal,
  RotateCcw,
  RefreshCw,
  AlertTriangle,
  Layers,
} from 'lucide-react';
import { operationApi } from '../api/operations';
import { productApi } from '../api/products';
import { StockOperation, OperationType } from '../types/operation';
import { Product } from '../types/product';
import ReceiptModal from '../components/operations/ReceiptModal';
import DeliveryModal from '../components/operations/DeliveryModal';
import AdjustmentModal from '../components/operations/AdjustmentModal';
import Badge from '../components/ui/Badge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import { formatQuantity, formatDate } from '../utils/formatters';

interface OperationsPageProps {
  initialType?: OperationType | 'ALL';
  initialProductId?: number;
  onSuccessToast: (msg: string) => void;
  onErrorToast: (msg: string) => void;
}

export const OperationsPage: React.FC<OperationsPageProps> = ({
  initialType = 'ALL',
  initialProductId,
  onSuccessToast,
  onErrorToast,
}) => {
  const [activeTab, setActiveTab] = useState<OperationType | 'ALL'>(initialType);
  const [selectedProductId, setSelectedProductId] = useState<number | ''>(initialProductId || '');
  const [operations, setOperations] = useState<StockOperation[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Modal controls
  const [isReceiptOpen, setIsReceiptOpen] = useState<boolean>(false);
  const [isDeliveryOpen, setIsDeliveryOpen] = useState<boolean>(false);
  const [isAdjustmentOpen, setIsAdjustmentOpen] = useState<boolean>(false);
  const [actionProductId, setActionProductId] = useState<number | undefined>(initialProductId);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const typeFilter = activeTab === 'ALL' ? undefined : activeTab;
      const productFilter = selectedProductId ? Number(selectedProductId) : undefined;

      const [ops, prods] = await Promise.all([
        operationApi.getAll({ productId: productFilter, type: typeFilter, size: 50 }),
        productApi.getAll(),
      ]);

      setOperations(ops);
      setProducts(prods);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load operations history';
      setError(msg);
      onErrorToast(msg);
    } finally {
      setLoading(false);
    }
  }, [activeTab, selectedProductId, onErrorToast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Sync initial prop updates if caller changes tabs
  useEffect(() => {
    if (initialType) {
      setActiveTab(initialType);
    }
  }, [initialType]);

  const handleOpenReceipt = (prodId?: number) => {
    setActionProductId(prodId);
    setIsReceiptOpen(true);
  };

  const handleOpenDelivery = (prodId?: number) => {
    setActionProductId(prodId);
    setIsDeliveryOpen(true);
  };

  const handleOpenAdjustment = (prodId?: number) => {
    setActionProductId(prodId);
    setIsAdjustmentOpen(true);
  };

  const handleOperationSuccess = (msg: string) => {
    onSuccessToast(msg);
    loadData();
  };

  return (
    <div className="operations-page">
      {/* Action and Filter Toolbar */}
      <div className="card toolbar-card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          {/* Product Filter */}
          <select
            className="filter-select"
            value={selectedProductId}
            onChange={(e) => setSelectedProductId(e.target.value ? Number(e.target.value) : '')}
          >
            <option value="">All Products</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.sku})
              </option>
            ))}
          </select>

          {selectedProductId !== '' && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setSelectedProductId('')}
            >
              <RotateCcw size={14} />
              <span>Reset Product</span>
            </button>
          )}
        </div>

        {/* Primary Action Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleOpenReceipt()}
            style={{ borderColor: 'var(--success-border)', color: 'var(--success)' }}
          >
            <ArrowDownLeft size={16} />
            <span>Receive Stock</span>
          </button>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleOpenDelivery()}
            style={{ borderColor: '#bfdbfe', color: '#1e40af' }}
          >
            <ArrowUpRight size={16} />
            <span>Deliver Stock</span>
          </button>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleOpenAdjustment()}
            style={{ borderColor: '#fde68a', color: '#92400e' }}
          >
            <SlidersHorizontal size={16} />
            <span>Adjust Stock</span>
          </button>
        </div>
      </div>

      {/* Tabs Header */}
      <div className="tabs-container">
        <button
          type="button"
          className={`tab-btn ${activeTab === 'ALL' ? 'active' : ''}`}
          onClick={() => setActiveTab('ALL')}
        >
          <Layers size={16} />
          <span>All Movements</span>
        </button>

        <button
          type="button"
          className={`tab-btn ${activeTab === 'RECEIPT' ? 'active' : ''}`}
          onClick={() => setActiveTab('RECEIPT')}
        >
          <ArrowDownLeft size={16} style={{ color: 'var(--success)' }} />
          <span>Receipts (Inbound)</span>
        </button>

        <button
          type="button"
          className={`tab-btn ${activeTab === 'DELIVERY' ? 'active' : ''}`}
          onClick={() => setActiveTab('DELIVERY')}
        >
          <ArrowUpRight size={16} style={{ color: 'var(--primary)' }} />
          <span>Deliveries (Outbound)</span>
        </button>

        <button
          type="button"
          className={`tab-btn ${activeTab === 'ADJUSTMENT' ? 'active' : ''}`}
          onClick={() => setActiveTab('ADJUSTMENT')}
        >
          <SlidersHorizontal size={16} style={{ color: '#d97706' }} />
          <span>Count Adjustments</span>
        </button>
      </div>

      {/* Operations Table Card */}
      <div className="card">
        {loading ? (
          <div style={{ padding: '60px 0', display: 'flex', justifyContent: 'center' }}>
            <LoadingSpinner message="Fetching stock operations history..." />
          </div>
        ) : error ? (
          <div style={{ padding: '40px 20px', textAlign: 'center' }}>
            <div style={{ color: 'var(--danger)', marginBottom: '8px' }}>
              <AlertTriangle size={32} style={{ margin: '0 auto' }} />
            </div>
            <p style={{ fontWeight: 600 }}>Failed to load operations</p>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px' }}>{error}</p>
            <button type="button" className="btn btn-secondary btn-sm" onClick={loadData}>
              <RefreshCw size={14} />
              <span>Retry</span>
            </button>
          </div>
        ) : operations.length === 0 ? (
          <div style={{ padding: '40px 20px' }}>
            <EmptyState
              title={
                activeTab === 'ALL'
                  ? 'No inventory movements recorded yet'
                  : `No ${activeTab.toLowerCase()} operations found`
              }
              description="Record receipts to bring stock in, deliveries to dispatch items, or adjustments to reconcile physical counts."
              actionText="New Movement"
              onAction={() => handleOpenReceipt()}
            />
          </div>
        ) : (
          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Type</th>
                  <th>Product & SKU</th>
                  <th>Operation Quantity</th>
                  <th>Net Change</th>
                  <th>Resulting Stock</th>
                  <th>Reference & Notes</th>
                </tr>
              </thead>
              <tbody>
                {operations.map((op) => {
                  const isPositive = op.quantityChange > 0;
                  const isZero = op.quantityChange === 0;

                  let badgeVariant: 'success' | 'info' | 'warning' = 'info';
                  if (op.operationType === 'RECEIPT') badgeVariant = 'success';
                  if (op.operationType === 'ADJUSTMENT') badgeVariant = 'warning';

                  return (
                    <tr key={op.id}>
                      <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {formatDate(op.createdAt)}
                      </td>
                      <td>
                        <Badge variant={badgeVariant}>{op.operationType}</Badge>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{op.productName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontFamily: 'monospace' }}>
                          SKU: {op.sku}
                        </div>
                      </td>
                      <td>
                        <span style={{ fontWeight: 500 }}>
                          {formatQuantity(op.quantity, op.unit)}
                        </span>
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
                          {isPositive ? `+${op.quantityChange}` : op.quantityChange} {op.unit}
                        </span>
                      </td>
                      <td>
                        <span style={{ fontWeight: 600, color: 'var(--primary)' }}>
                          {formatQuantity(op.resultingQuantity, op.unit)}
                        </span>
                      </td>
                      <td>
                        {op.reference && (
                          <div style={{ fontWeight: 500, fontSize: '0.82rem' }}>
                            Ref: {op.reference}
                          </div>
                        )}
                        {op.notes && (
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            {op.notes}
                          </div>
                        )}
                        {!op.reference && !op.notes && <span style={{ color: 'var(--text-light)' }}>—</span>}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modals for Operations */}
      <ReceiptModal
        isOpen={isReceiptOpen}
        onClose={() => setIsReceiptOpen(false)}
        products={products}
        initialProductId={actionProductId}
        onSuccess={handleOperationSuccess}
        onError={onErrorToast}
      />

      <DeliveryModal
        isOpen={isDeliveryOpen}
        onClose={() => setIsDeliveryOpen(false)}
        products={products}
        initialProductId={actionProductId}
        onSuccess={handleOperationSuccess}
        onError={onErrorToast}
      />

      <AdjustmentModal
        isOpen={isAdjustmentOpen}
        onClose={() => setIsAdjustmentOpen(false)}
        products={products}
        initialProductId={actionProductId}
        onSuccess={handleOperationSuccess}
        onError={onErrorToast}
      />
    </div>
  );
};

export default OperationsPage;
