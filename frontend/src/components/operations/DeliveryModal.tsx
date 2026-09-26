import React, { useState, useEffect } from 'react';
import { ArrowUpRight, AlertTriangle } from 'lucide-react';
import { Product, StockBalanceResponse } from '../../types/product';
import { operationApi } from '../../api/operations';
import { productApi } from '../../api/products';
import Modal from '../ui/Modal';
import { formatQuantity } from '../../utils/formatters';

interface DeliveryModalProps {
  isOpen: boolean;
  onClose: () => void;
  products: Product[];
  initialProductId?: number;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}

export const DeliveryModal: React.FC<DeliveryModalProps> = ({
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
  const [quantity, setQuantity] = useState<string>('');
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
        if (isMounted) setCurrentStock(res);
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

  const numQty = parseFloat(quantity) || 0;
  const isExcessive = numQty > currQty;
  const isOutOfStock = currQty <= 0;

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    if (!selectedProductId) {
      errs.productId = 'Please select a product';
    }
    if (!quantity || isNaN(numQty) || numQty <= 0) {
      errs.quantity = 'Quantity must be strictly greater than zero';
    } else if (isExcessive) {
      errs.quantity = `Cannot deliver more than available stock (${currQty} ${selectedProduct?.unit || 'units'})`;
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      const res = await operationApi.createDelivery({
        productId: Number(selectedProductId),
        quantity: parseFloat(quantity),
        reference: reference.trim() || undefined,
        notes: notes.trim() || undefined,
      });

      onSuccess(
        `Delivery dispatched successfully: -${res.quantity} ${res.unit} of ${res.productName}. Remaining Balance: ${res.resultingQuantity} ${res.unit}`
      );
      // Reset form
      setQuantity('');
      setReference('');
      setNotes('');
      setErrors({});
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to record delivery';
      onError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Outbound Delivery (Ship Stock)"
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary"
            style={{ backgroundColor: isExcessive || isOutOfStock ? '#64748b' : undefined }}
            onClick={handleSubmit}
            disabled={isSubmitting || !selectedProductId || isExcessive || isOutOfStock}
          >
            <ArrowUpRight size={16} />
            <span>{isSubmitting ? 'Dispatching Delivery...' : 'Confirm Delivery'}</span>
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
              setErrors((prev) => ({ ...prev, productId: '' }));
            }}
          >
            <option value="">Select a product to deliver...</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.sku}) — {p.category}
              </option>
            ))}
          </select>
          {errors.productId && <div style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '4px' }}>{errors.productId}</div>}
        </div>

        {/* Out of Stock Warning */}
        {selectedProduct && isOutOfStock && !loadingStock && (
          <div
            style={{
              background: 'var(--danger-bg)',
              border: '1px solid var(--danger-border)',
              borderRadius: 'var(--radius-md)',
              padding: '12px 16px',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              marginBottom: '16px',
              color: 'var(--danger)',
              fontSize: '0.85rem',
            }}
          >
            <AlertTriangle size={18} />
            <span>
              <strong>Zero Available Stock:</strong> This product currently has 0 {selectedProduct.unit} on hand. An incoming receipt is required before delivering.
            </span>
          </div>
        )}

        {/* Stock Balance Preview Box */}
        {selectedProduct && !isOutOfStock && (
          <div className="stock-preview-box">
            <div className="stock-preview-item">
              <span className="stock-preview-label">Available Stock</span>
              <span className="stock-preview-val">
                {loadingStock ? 'Loading...' : formatQuantity(currQty, selectedProduct.unit)}
              </span>
            </div>
            <div className="stock-preview-item" style={{ textAlign: 'center' }}>
              <span className="stock-preview-label">Delivering</span>
              <span className="stock-preview-val delta-negative">
                -{numQty > 0 ? numQty : 0} {selectedProduct.unit}
              </span>
            </div>
            <div className="stock-preview-item" style={{ textAlign: 'right' }}>
              <span className="stock-preview-label">Resulting Stock</span>
              <span
                className="stock-preview-val"
                style={{ color: isExcessive ? 'var(--danger)' : 'var(--text-main)' }}
              >
                {formatQuantity(currQty - numQty, selectedProduct.unit)}
              </span>
            </div>
          </div>
        )}

        <div className="form-group" style={{ marginBottom: '16px' }}>
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>
            Delivery Quantity <span style={{ color: 'var(--danger)' }}>*</span>
          </label>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <input
              type="number"
              step="any"
              min="0.0001"
              max={currQty}
              placeholder="e.g. 10"
              className="form-control"
              style={{
                width: '100%',
                padding: '9px 12px',
                borderRadius: 'var(--radius-md)',
                border:
                  errors.quantity || isExcessive
                    ? '1px solid var(--danger)'
                    : '1px solid var(--border-subtle)',
              }}
              value={quantity}
              onChange={(e) => {
                setQuantity(e.target.value);
                setErrors((prev) => ({ ...prev, quantity: '' }));
              }}
              disabled={isOutOfStock}
            />
            {selectedProduct && (
              <span style={{ fontSize: '0.88rem', color: 'var(--text-muted)', minWidth: '40px' }}>
                {selectedProduct.unit}
              </span>
            )}
          </div>
          {errors.quantity && <div style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '4px' }}>{errors.quantity}</div>}
          {isExcessive && !errors.quantity && (
            <div style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '4px' }}>
              Insufficient stock! Requested {numQty} {selectedProduct?.unit}, but only {currQty} {selectedProduct?.unit} available.
            </div>
          )}
        </div>

        <div className="form-group" style={{ marginBottom: '16px' }}>
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>
            Reference / Delivery Order (Optional)
          </label>
          <input
            type="text"
            placeholder="e.g. DO-2026-4401, SO-8832"
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
            Notes / Destination (Optional)
          </label>
          <textarea
            placeholder="e.g. Dispatched to client warehouse, tracking #99401"
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

export default DeliveryModal;
