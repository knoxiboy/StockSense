import React, { useState, useEffect } from 'react';
import { ArrowDownLeft } from 'lucide-react';
import { Product, StockBalanceResponse } from '../../types/product';
import { operationApi } from '../../api/operations';
import { productApi } from '../../api/products';
import Modal from '../ui/Modal';
import { formatQuantity } from '../../utils/formatters';

interface ReceiptModalProps {
  isOpen: boolean;
  onClose: () => void;
  products: Product[];
  initialProductId?: number;
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}

export const ReceiptModal: React.FC<ReceiptModalProps> = ({
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

  const validate = (): boolean => {
    const errs: Record<string, string> = {};
    if (!selectedProductId) {
      errs.productId = 'Please select a product';
    }
    const numQty = parseFloat(quantity);
    if (!quantity || isNaN(numQty) || numQty <= 0) {
      errs.quantity = 'Quantity must be strictly greater than zero';
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      const res = await operationApi.createReceipt({
        productId: Number(selectedProductId),
        quantity: parseFloat(quantity),
        reference: reference.trim() || undefined,
        notes: notes.trim() || undefined,
      });

      onSuccess(
        `Receipt recorded successfully: +${res.quantity} ${res.unit} of ${res.productName}. New Balance: ${res.resultingQuantity} ${res.unit}`
      );
      // Reset form
      setQuantity('');
      setReference('');
      setNotes('');
      setErrors({});
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to record receipt';
      onError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const numQty = parseFloat(quantity) || 0;
  const currQty = currentStock
    ? typeof currentStock.quantity === 'string'
      ? parseFloat(currentStock.quantity)
      : currentStock.quantity
    : 0;
  const projectedQty = currQty + numQty;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Incoming Receipt (Inbound Stock)"
      footer={
        <>
          <button type="button" className="btn btn-secondary" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={isSubmitting || !selectedProductId}
          >
            <ArrowDownLeft size={16} />
            <span>{isSubmitting ? 'Recording Receipt...' : 'Confirm Receipt'}</span>
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
            <option value="">Select a product...</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.sku}) — {p.category}
              </option>
            ))}
          </select>
          {errors.productId && <div style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '4px' }}>{errors.productId}</div>}
        </div>

        {/* Stock Balance Preview Box */}
        {selectedProduct && (
          <div className="stock-preview-box">
            <div className="stock-preview-item">
              <span className="stock-preview-label">Current Balance</span>
              <span className="stock-preview-val">
                {loadingStock ? 'Loading...' : formatQuantity(currQty, selectedProduct.unit)}
              </span>
            </div>
            <div className="stock-preview-item" style={{ textAlign: 'center' }}>
              <span className="stock-preview-label">Quantity Received</span>
              <span className="stock-preview-val delta-positive">
                +{numQty > 0 ? numQty : 0} {selectedProduct.unit}
              </span>
            </div>
            <div className="stock-preview-item" style={{ textAlign: 'right' }}>
              <span className="stock-preview-label">Projected Balance</span>
              <span className="stock-preview-val" style={{ color: 'var(--primary)' }}>
                {formatQuantity(projectedQty, selectedProduct.unit)}
              </span>
            </div>
          </div>
        )}

        <div className="form-group" style={{ marginBottom: '16px' }}>
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>
            Received Quantity <span style={{ color: 'var(--danger)' }}>*</span>
          </label>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <input
              type="number"
              step="any"
              min="0.0001"
              placeholder="e.g. 50"
              className="form-control"
              style={{
                width: '100%',
                padding: '9px 12px',
                borderRadius: 'var(--radius-md)',
                border: errors.quantity ? '1px solid var(--danger)' : '1px solid var(--border-subtle)',
              }}
              value={quantity}
              onChange={(e) => {
                setQuantity(e.target.value);
                setErrors((prev) => ({ ...prev, quantity: '' }));
              }}
            />
            {selectedProduct && (
              <span style={{ fontSize: '0.88rem', color: 'var(--text-muted)', minWidth: '40px' }}>
                {selectedProduct.unit}
              </span>
            )}
          </div>
          {errors.quantity && <div style={{ color: 'var(--danger)', fontSize: '0.8rem', marginTop: '4px' }}>{errors.quantity}</div>}
        </div>

        <div className="form-group" style={{ marginBottom: '16px' }}>
          <label className="form-label" style={{ fontWeight: 600, display: 'block', marginBottom: '6px' }}>
            Reference / Document Number (Optional)
          </label>
          <input
            type="text"
            placeholder="e.g. PO-2026-089, BL-7819"
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
            Notes / Vendor Details (Optional)
          </label>
          <textarea
            placeholder="e.g. Delivery truck arrived on time, batch #4402 inspected"
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

export default ReceiptModal;
