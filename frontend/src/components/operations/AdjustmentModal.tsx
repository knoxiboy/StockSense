import React, { useState, useEffect } from 'react';
import { SlidersHorizontal } from 'lucide-react';
import { Product, StockBalanceResponse } from '../../types/product';
import { operationApi } from '../../api/operations';
import { productApi } from '../../api/products';
import Modal from '../ui/Modal';
import { formatQuantity } from '../../utils/formatters';

interface AdjustmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  products: Product[];
  initialProductId?: number;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}

export const AdjustmentModal: React.FC<AdjustmentModalProps> = ({
  isOpen,
  onClose,
  products,
  initialProductId,
  onSuccess,
  onError,
}) => {
  const [selectedProductId, setSelectedProductId] = useState<number | ''>(initialProductId || '');
  const [currentStock, setCurrentStock] = useState<StockBalanceResponse | null>(null);
  const [loadingStock, setLoadingStock] = useState<boolean>(false);
  const [countedQuantity, setCountedQuantity] = useState<string>('');
  const [reference, setReference] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (initialProductId) {
      setSelectedProductId(initialProductId);
    } else if (products.length > 0 && selectedProductId === '') {
      setSelectedProductId(products[0].id);
    }
  }, [initialProductId, products, selectedProductId]);

  useEffect(() => {
    if (!selectedProductId) {
      setCurrentStock(null);
      return;
    }
    let isMounted = true;
    setLoadingStock(true);
    productApi
      .getStock(Number(selectedProductId))
      .then((res) => {
        if (isMounted) {
          setCurrentStock(res);
          // If countedQuantity is empty, pre-populate with current stock
          setCountedQuantity((prev) => (prev === '' ? `${res.quantity}` : prev));
        }
      })
      .catch(() => {
        if (isMounted) setCurrentStock(null);
      })
      .finally(() => {
        if (isMounted) setLoadingStock(false);
      });
    return () => {
      isMounted = false;
    };
  }, [selectedProductId]);

  const selectedProduct = products.find((p) => p.id === Number(selectedProductId));

  const currQty = currentStock
    ? typeof currentStock.quantity === 'string'
      ? parseFloat(currentStock.quantity)
      : currentStock.quantity
    : 0;

  const numCounted = parseFloat(countedQuantity);
  const isValidCounted = !isNaN(numCounted) && numCounted >= 0;
  const quantityChange = isValidCounted ? numCounted - currQty : 0;

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    if (!selectedProductId) {
      errs.productId = 'Please select a product';
    }
    if (countedQuantity === '' || isNaN(numCounted) || numCounted < 0) {
      errs.countedQuantity = 'Counted quantity cannot be negative';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      const res = await operationApi.createAdjustment({
        productId: Number(selectedProductId),
        countedQuantity: parseFloat(countedQuantity),
        reference: reference.trim() || undefined,
        notes: notes.trim() || undefined,
      });

      const sign = res.quantityChange > 0 ? `+${res.quantityChange}` : `${res.quantityChange}`;
      onSuccess(
        `Stock adjustment applied: ${res.productName} adjusted by ${sign} ${res.unit}. Resulting Stock: ${res.resultingQuantity} ${res.unit}`
      );
      // Reset form
      setCountedQuantity('');
      setReference('');
      setNotes('');
      setErrors({});
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to record stock adjustment';
      onError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Stock Adjustment (Cycle Count Reconcile)"
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={isSubmitting || !selectedProductId || countedQuantity === ''}
          >
            <SlidersHorizontal size={16} />
            <span>{isSubmitting ? 'Applying Adjustment...' : 'Apply Count Adjustment'}</span>
          </button>
        </>
      }
    >
      <form onSubmit={handleSubmit}>
        <div className="form-group" style={{ marginBottom: '16px' }}>
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>
            Product <span style={{ color: 'var(--danger)' }}>*</span>
          </label>
          <select
            className="filter-select"
            style={{ width: '100%' }}
            value={selectedProductId}
            onChange={(e) => {
              setSelectedProductId(Number(e.target.value));
              setCountedQuantity('');
              setErrors((prev) => ({ ...prev, productId: '' }));
            }}
          >
            <option value="">Select a product to adjust...</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.sku}) — {p.category}
              </option>
            ))}
          </select>
          {errors.productId && <div style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '4px' }}>{errors.productId}</div>}
        </div>

        {/* Stock Balance Comparison Box */}
        {selectedProduct && (
          <div className="stock-preview-box">
            <div className="stock-preview-item">
              <span className="stock-preview-label">System Balance</span>
              <span className="stock-preview-val">
                {loadingStock ? 'Loading...' : formatQuantity(currQty, selectedProduct.unit)}
              </span>
            </div>
            <div className="stock-preview-item" style={{ textAlign: 'center' }}>
              <span className="stock-preview-label">Adjustment Delta</span>
              <span
                className={`stock-preview-val ${
                  quantityChange > 0
                    ? 'delta-positive'
                    : quantityChange < 0
                    ? 'delta-negative'
                    : ''
                }`}
              >
                {quantityChange > 0 ? `+${quantityChange}` : quantityChange} {selectedProduct.unit}
              </span>
            </div>
            <div className="stock-preview-item" style={{ textAlign: 'right' }}>
              <span className="stock-preview-label">New Balance (Counted)</span>
              <span className="stock-preview-val" style={{ color: 'var(--primary)' }}>
                {isValidCounted ? formatQuantity(numCounted, selectedProduct.unit) : '—'}
              </span>
            </div>
          </div>
        )}

        <div className="form-group" style={{ marginBottom: '16px' }}>
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>
            Counted Physical Stock <span style={{ color: 'var(--danger)' }}>*</span>
          </label>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <input
              type="number"
              step="any"
              min="0"
              placeholder="e.g. 24"
              className="form-control"
              style={{
                width: '100%',
                padding: '9px 12px',
                borderRadius: 'var(--radius-md)',
                border: errors.countedQuantity ? '1px solid var(--danger)' : '1px solid var(--border-subtle)',
              }}
              value={countedQuantity}
              onChange={(e) => {
                setCountedQuantity(e.target.value);
                setErrors((prev) => ({ ...prev, countedQuantity: '' }));
              }}
            />
            {selectedProduct && (
              <span style={{ fontSize: '0.88rem', color: 'var(--text-muted)', minWidth: '40px' }}>
                {selectedProduct.unit}
              </span>
            )}
          </div>
          {errors.countedQuantity && (
            <div style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '4px' }}>
              {errors.countedQuantity}
            </div>
          )}
          <span style={{ fontSize: '0.78rem', color: 'var(--text-light)', display: 'block', marginTop: '4px' }}>
            Enter the exact quantity counted during physical verification (0 or greater).
          </span>
        </div>

        <div className="form-group" style={{ marginBottom: '16px' }}>
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>
            Reference / Audit Batch (Optional)
          </label>
          <input
            type="text"
            placeholder="e.g. COUNT-2026-Q3, AUDIT-MISMATCH"
            className="form-control"
            style={{
              width: '100%',
              padding: '9px 12px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
            }}
            value={reference}
            onChange={(e) => setReference(e.target.value)}
          />
        </div>

        <div className="form-group">
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>
            Reason for Adjustment (Optional)
          </label>
          <textarea
            placeholder="e.g. Annual physical inventory audit, recount found 2 damaged items"
            className="form-control"
            style={{
              width: '100%',
              padding: '9px 12px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-subtle)',
              minHeight: '68px',
              resize: 'vertical',
            }}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </div>
      </form>
    </Modal>
  );
};

export default AdjustmentModal;
