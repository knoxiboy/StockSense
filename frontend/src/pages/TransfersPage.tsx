import React, { useState, useEffect, useCallback } from 'react';
import { Plus, CheckCircle, Search, RefreshCw, ArrowRight } from 'lucide-react';
import { transferApi } from '../api/transfers';
import { productApi } from '../api/products';
import { locationApi } from '../api/warehouses';
import { InternalTransfer, TransferStatus } from '../types/transfer';
import { Product } from '../types/product';
import { Location } from '../types/warehouse';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import Badge from '../components/ui/Badge';
import Modal from '../components/ui/Modal';
import { formatDate, formatQuantity } from '../utils/formatters';

interface TransfersPageProps {
  onSuccessToast?: (msg: string) => void;
  onErrorToast?: (msg: string) => void;
}

export const TransfersPage: React.FC<TransfersPageProps> = ({
  onSuccessToast = () => {},
  onErrorToast = () => {},
}) => {
  const [transfers, setTransfers] = useState<InternalTransfer[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [statusFilter, setStatusFilter] = useState<TransferStatus | ''>('');
  const [search, setSearch] = useState<string>('');

  // Create Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [ref, setRef] = useState('');
  const [productId, setProductId] = useState<number>(0);
  const [sourceLocId, setSourceLocId] = useState<number>(0);
  const [destLocId, setDestLocId] = useState<number>(0);
  const [quantity, setQuantity] = useState<string>('');
  const [notes, setNotes] = useState('');
  const [autoExecute, setAutoExecute] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [executingId, setExecutingId] = useState<number | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [transferList, productList, locList] = await Promise.all([
        transferApi.getAll(statusFilter ? statusFilter : undefined),
        productApi.getAll(),
        locationApi.getAll(),
      ]);
      setTransfers(transferList);
      setProducts(productList);
      setLocations(locList);
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to load internal transfers');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, onErrorToast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreateTransfer = async (e: React.FormEvent) => {
    e.preventDefault();
    const qtyNum = parseFloat(quantity);
    if (!productId || !sourceLocId || !destLocId || isNaN(qtyNum) || qtyNum <= 0) {
      onErrorToast('Please select product, locations, and a valid quantity greater than zero');
      return;
    }
    if (sourceLocId === destLocId) {
      onErrorToast('Source location and destination location must be different');
      return;
    }

    setSubmitting(true);
    try {
      await transferApi.create({
        reference: ref.trim() || undefined,
        productId,
        sourceLocationId: sourceLocId,
        destinationLocationId: destLocId,
        quantity: qtyNum,
        notes: notes.trim() || undefined,
        autoExecute,
      });
      onSuccessToast(autoExecute ? 'Transfer created and executed successfully' : 'Transfer created successfully');
      setIsModalOpen(false);
      resetForm();
      fetchData();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to create transfer');
    } finally {
      setSubmitting(false);
    }
  };

  const handleExecute = async (id: number) => {
    setExecutingId(id);
    try {
      await transferApi.execute(id);
      onSuccessToast('Stock transferred successfully! Inventory balances updated.');
      fetchData();
    } catch (err: any) {
      onErrorToast(err.message || 'Transfer execution failed. Check source stock balance.');
    } finally {
      setExecutingId(null);
    }
  };

  const handleCancel = async (id: number) => {
    if (!confirm('Are you sure you want to cancel this transfer?')) return;
    try {
      await transferApi.cancel(id);
      onSuccessToast('Transfer canceled');
      fetchData();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to cancel transfer');
    }
  };

  const resetForm = () => {
    setRef('');
    setProductId(0);
    setSourceLocId(0);
    setDestLocId(0);
    setQuantity('');
    setNotes('');
    setAutoExecute(false);
  };

  const filteredTransfers = transfers.filter((t) => {
    const matchesSearch =
      t.reference.toLowerCase().includes(search.toLowerCase()) ||
      t.productName.toLowerCase().includes(search.toLowerCase()) ||
      t.sku.toLowerCase().includes(search.toLowerCase());
    return matchesSearch;
  });

  return (
    <div className="transfers-page-container">
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Internal Stock Transfers</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Execute atomic inventory transfers between warehouse locations with dual-ledger audit verification
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => {
            resetForm();
            if (products.length > 0) setProductId(products[0].id);
            if (locations.length > 1) {
              setSourceLocId(locations[0].id);
              setDestLocId(locations[1].id);
            }
            setIsModalOpen(true);
          }}
        >
          <Plus size={16} />
          <span>New Transfer</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
            <div className="search-input-wrap" style={{ position: 'relative' }}>
              <Search size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                className="form-control"
                placeholder="Search reference, product, SKU..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                style={{ paddingLeft: '32px', width: '260px' }}
              />
            </div>
            <select
              className="form-control"
              style={{ width: '160px' }}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as any)}
            >
              <option value="">All Statuses</option>
              <option value="READY">Ready</option>
              <option value="DRAFT">Draft</option>
              <option value="DONE">Completed</option>
              <option value="CANCELED">Canceled</option>
            </select>
          </div>

          <button type="button" className="btn btn-secondary btn-sm" onClick={fetchData}>
            <RefreshCw size={14} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Transfers Table */}
      <div className="card">
        {loading ? (
          <div style={{ padding: '60px 0', textAlign: 'center' }}>
            <LoadingSpinner message="Loading internal transfers..." />
          </div>
        ) : filteredTransfers.length === 0 ? (
          <EmptyState
            title="No internal transfers found"
            description="Move stock between warehouse zones, bins, or buildings."
            actionText="Create Transfer"
            onAction={() => setIsModalOpen(true)}
          />
        ) : (
          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Product</th>
                  <th>Source Location</th>
                  <th></th>
                  <th>Destination Location</th>
                  <th>Quantity</th>
                  <th>Status</th>
                  <th>Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredTransfers.map((t) => {
                  let badgeVariant: 'info' | 'success' | 'warning' | 'secondary' = 'info';
                  if (t.status === 'DONE') badgeVariant = 'success';
                  if (t.status === 'CANCELED') badgeVariant = 'secondary';
                  if (t.status === 'READY') badgeVariant = 'warning';

                  return (
                    <tr key={t.id}>
                      <td>
                        <span style={{ fontWeight: 600 }}>{t.reference}</span>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{t.productName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>SKU: {t.sku}</div>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{t.sourceLocationName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{t.sourceWarehouseName}</div>
                      </td>
                      <td style={{ textAlign: 'center', color: 'var(--primary)' }}>
                        <ArrowRight size={16} />
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{t.destinationLocationName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{t.destinationWarehouseName}</div>
                      </td>
                      <td>
                        <strong style={{ fontSize: '0.95rem' }}>{formatQuantity(t.quantity, t.unit)}</strong>
                      </td>
                      <td>
                        <Badge variant={badgeVariant}>{t.status}</Badge>
                      </td>
                      <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                        {formatDate(t.createdAt)}
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '8px' }}>
                          {(t.status === 'READY' || t.status === 'DRAFT') && (
                            <>
                              <button
                                type="button"
                                className="btn btn-primary btn-sm"
                                disabled={executingId === t.id}
                                onClick={() => handleExecute(t.id)}
                              >
                                {executingId === t.id ? 'Processing...' : 'Execute'}
                              </button>
                              <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                onClick={() => handleCancel(t.id)}
                              >
                                Cancel
                              </button>
                            </>
                          )}
                          {t.status === 'DONE' && (
                            <span style={{ fontSize: '0.78rem', color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                              <CheckCircle size={14} /> Completed
                            </span>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* New Transfer Modal */}
      {isModalOpen && (
        <Modal isOpen={isModalOpen}
          title="New Internal Stock Transfer"
          onClose={() => setIsModalOpen(false)}
        >
          <form onSubmit={handleCreateTransfer}>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Reference Number (Optional)</label>
              <input
                type="text"
                className="form-control"
                value={ref}
                onChange={(e) => setRef(e.target.value)}
                placeholder="e.g. TR-2026-0042 (Auto-generated if empty)"
              />
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Product *</label>
              <select
                className="form-control"
                required
                value={productId}
                onChange={(e) => setProductId(Number(e.target.value))}
              >
                <option value="">Select product to move</option>
                {products.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.sku})
                  </option>
                ))}
              </select>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '14px' }}>
              <div className="form-group">
                <label className="form-label">Source Location *</label>
                <select
                  className="form-control"
                  required
                  value={sourceLocId}
                  onChange={(e) => setSourceLocId(Number(e.target.value))}
                >
                  <option value="">Select source</option>
                  {locations.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.name} ({l.code})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Destination Location *</label>
                <select
                  className="form-control"
                  required
                  value={destLocId}
                  onChange={(e) => setDestLocId(Number(e.target.value))}
                >
                  <option value="">Select destination</option>
                  {locations.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.name} ({l.code})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Quantity to Transfer *</label>
              <input
                type="number"
                step="any"
                min="0.0001"
                required
                className="form-control"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                placeholder="e.g. 25"
              />
            </div>

            <div className="form-group" style={{ marginBottom: '16px' }}>
              <label className="form-label">Transfer Notes / Reason</label>
              <textarea
                className="form-control"
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="e.g. Replenishing retail picking shelf from bulk storage"
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '22px' }}>
              <input
                type="checkbox"
                id="autoExecuteCheck"
                checked={autoExecute}
                onChange={(e) => setAutoExecute(e.target.checked)}
              />
              <label htmlFor="autoExecuteCheck" style={{ fontSize: '0.85rem', cursor: 'pointer' }}>
                Execute immediately upon creation (deduct source & credit destination instantly)
              </label>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsModalOpen(false)}
                disabled={submitting}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Creating...' : autoExecute ? 'Create & Execute' : 'Save as Ready'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};

export default TransfersPage;
