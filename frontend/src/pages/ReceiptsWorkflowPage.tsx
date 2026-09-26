import React, { useState, useEffect, useCallback } from 'react';
import { Plus, CheckCircle, Search, RefreshCw, Eye, Trash2 } from 'lucide-react';
import { receiptOrderApi } from '../api/orders';
import { productApi } from '../api/products';
import { locationApi } from '../api/warehouses';
import { ReceiptOrder, OrderStatus } from '../types/order';
import { Product } from '../types/product';
import { Location } from '../types/warehouse';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import Badge from '../components/ui/Badge';
import Modal from '../components/ui/Modal';
import { formatDate } from '../utils/formatters';

interface ReceiptsWorkflowPageProps {
  onSuccessToast?: (msg: string) => void;
  onErrorToast?: (msg: string) => void;
}

export const ReceiptsWorkflowPage: React.FC<ReceiptsWorkflowPageProps> = ({
  onSuccessToast = () => {},
  onErrorToast = () => {},
}) => {
  const [orders, setOrders] = useState<ReceiptOrder[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>('');
  const [search, setSearch] = useState<string>('');

  // New Order Modal
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [ref, setRef] = useState('');
  const [supplierName, setSupplierName] = useState('');
  const [supplierContact, setSupplierContact] = useState('');
  const [destLocId, setDestLocId] = useState<number>(0);
  const [notes, setNotes] = useState('');
  const [lines, setLines] = useState<{ productId: number; expectedQuantity: number; receivedQuantity: number }[]>([]);
  const [submitting, setSubmitting] = useState(false);

  // Detail Modal
  const [selectedOrder, setSelectedOrder] = useState<ReceiptOrder | null>(null);
  const [statusUpdating, setStatusUpdating] = useState(false);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const [orderList, productList, locList] = await Promise.all([
        receiptOrderApi.getAll(statusFilter ? statusFilter : undefined),
        productApi.getAll(),
        locationApi.getAll(),
      ]);
      setOrders(orderList);
      setProducts(productList);
      setLocations(locList);
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to load receipt orders');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, onErrorToast]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleAddLine = () => {
    if (products.length === 0) return;
    setLines([...lines, { productId: products[0].id, expectedQuantity: 10, receivedQuantity: 10 }]);
  };

  const handleRemoveLine = (index: number) => {
    setLines(lines.filter((_, i) => i !== index));
  };

  const handleLineChange = (index: number, field: string, value: any) => {
    const updated = [...lines];
    updated[index] = { ...updated[index], [field]: value };
    setLines(updated);
  };

  const handleCreateOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!ref.trim() || !supplierName.trim()) {
      onErrorToast('Reference and Supplier Name are required');
      return;
    }
    if (lines.length === 0) {
      onErrorToast('Add at least one product line to the receipt order');
      return;
    }

    setSubmitting(true);
    try {
      await receiptOrderApi.create({
        reference: ref.trim(),
        supplierName: supplierName.trim(),
        supplierContact: supplierContact.trim() || undefined,
        destinationLocationId: destLocId || undefined,
        notes: notes.trim() || undefined,
        lines,
      });
      onSuccessToast('Receipt order created successfully');
      setIsNewModalOpen(false);
      resetNewForm();
      fetchOrders();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to create receipt order');
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusTransition = async (orderId: number, targetStatus: OrderStatus) => {
    setStatusUpdating(true);
    try {
      const updated = await receiptOrderApi.updateStatus(orderId, targetStatus);
      onSuccessToast(`Receipt order moved to ${targetStatus}`);
      setSelectedOrder(updated);
      fetchOrders();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to transition order status');
    } finally {
      setStatusUpdating(false);
    }
  };

  const resetNewForm = () => {
    setRef(`REC-${new Date().getFullYear()}-${Math.floor(1000 + Math.random() * 9000)}`);
    setSupplierName('');
    setSupplierContact('');
    setDestLocId(locations[0]?.id || 0);
    setNotes('');
    setLines(products.length > 0 ? [{ productId: products[0].id, expectedQuantity: 10, receivedQuantity: 10 }] : []);
  };

  const filteredOrders = orders.filter((o) => {
    return (
      o.reference.toLowerCase().includes(search.toLowerCase()) ||
      o.supplierName.toLowerCase().includes(search.toLowerCase())
    );
  });

  return (
    <div className="receipts-workflow-container">
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Incoming Receipts & Purchase Orders</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Multi-line supplier receipts workflow: Draft → Waiting → Ready → Done (stock is credited on completion)
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => {
            resetNewForm();
            setIsNewModalOpen(true);
          }}
        >
          <Plus size={16} />
          <span>New Receipt Order</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
            <div className="search-input-wrap" style={{ position: 'relative' }}>
              <Search size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input
                type="text"
                className="form-control"
                placeholder="Search reference or supplier..."
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
              <option value="DRAFT">Draft</option>
              <option value="WAITING">Waiting</option>
              <option value="READY">Ready</option>
              <option value="DONE">Completed</option>
              <option value="CANCELED">Canceled</option>
            </select>
          </div>
          <button type="button" className="btn btn-secondary btn-sm" onClick={fetchOrders}>
            <RefreshCw size={14} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Orders Table */}
      <div className="card">
        {loading ? (
          <div style={{ padding: '60px 0', textAlign: 'center' }}>
            <LoadingSpinner message="Loading incoming receipt orders..." />
          </div>
        ) : filteredOrders.length === 0 ? (
          <EmptyState
            title="No receipt documents found"
            description="Create purchase orders and track multi-line supplier deliveries."
            actionText="Create Receipt Order"
            onAction={() => {
              resetNewForm();
              setIsNewModalOpen(true);
            }}
          />
        ) : (
          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Supplier</th>
                  <th>Destination Location</th>
                  <th>Line Items</th>
                  <th>Status</th>
                  <th>Created Date</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredOrders.map((o) => {
                  let badgeVariant: 'info' | 'success' | 'warning' | 'secondary' = 'info';
                  if (o.status === 'DONE') badgeVariant = 'success';
                  if (o.status === 'READY') badgeVariant = 'warning';
                  if (o.status === 'CANCELED') badgeVariant = 'secondary';

                  return (
                    <tr key={o.id}>
                      <td>
                        <strong>{o.reference}</strong>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600 }}>{o.supplierName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{o.supplierContact || 'No contact'}</div>
                      </td>
                      <td>
                        {o.destinationLocation ? (
                          <>
                            <div style={{ fontWeight: 600 }}>{o.destinationLocation.name}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{o.destinationLocation.warehouseName}</div>
                          </>
                        ) : (
                          'Default Stock'
                        )}
                      </td>
                      <td>
                        <span style={{ fontWeight: 600 }}>{o.lines?.length || 0} product(s)</span>
                      </td>
                      <td>
                        <Badge variant={badgeVariant}>{o.status}</Badge>
                      </td>
                      <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                        {formatDate(o.createdAt)}
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => setSelectedOrder(o)}
                        >
                          <Eye size={14} />
                          <span>View Workflow</span>
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* New Receipt Order Modal */}
      {isNewModalOpen && (
        <Modal
          isOpen={isNewModalOpen}
          title="Create Inbound Receipt Order"
          onClose={() => setIsNewModalOpen(false)}
        >
          <form onSubmit={handleCreateOrder}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
              <div className="form-group">
                <label className="form-label">Reference Number *</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  value={ref}
                  onChange={(e) => setRef(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Destination Location *</label>
                <select
                  className="form-control"
                  required
                  value={destLocId}
                  onChange={(e) => setDestLocId(Number(e.target.value))}
                >
                  {locations.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.name} ({l.code}) - {l.warehouseName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
              <div className="form-group">
                <label className="form-label">Supplier Name *</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  value={supplierName}
                  onChange={(e) => setSupplierName(e.target.value)}
                  placeholder="e.g. Apex Industrial Supplies"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Supplier Contact (Email / Phone)</label>
                <input
                  type="text"
                  className="form-control"
                  value={supplierContact}
                  onChange={(e) => setSupplierContact(e.target.value)}
                  placeholder="e.g. dispatch@apexsupply.com"
                />
              </div>
            </div>

            {/* Line Items */}
            <div style={{ marginBottom: '18px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <label className="form-label" style={{ margin: 0 }}>Product Items ({lines.length})</label>
                <button type="button" className="btn btn-secondary btn-sm" onClick={handleAddLine}>
                  <Plus size={13} />
                  <span>Add Line</span>
                </button>
              </div>

              {lines.map((line, idx) => (
                <div key={idx} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr auto', gap: '8px', alignItems: 'center', marginBottom: '8px' }}>
                  <select
                    className="form-control"
                    value={line.productId}
                    onChange={(e) => handleLineChange(idx, 'productId', Number(e.target.value))}
                  >
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} ({p.sku})
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    step="any"
                    min="0.0001"
                    className="form-control"
                    placeholder="Expected"
                    value={line.expectedQuantity}
                    onChange={(e) => handleLineChange(idx, 'expectedQuantity', parseFloat(e.target.value) || 0)}
                  />
                  <input
                    type="number"
                    step="any"
                    min="0"
                    className="form-control"
                    placeholder="Received"
                    value={line.receivedQuantity}
                    onChange={(e) => handleLineChange(idx, 'receivedQuantity', parseFloat(e.target.value) || 0)}
                  />
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    style={{ color: 'var(--danger)' }}
                    onClick={() => handleRemoveLine(idx)}
                    disabled={lines.length === 1}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              ))}
            </div>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Notes</label>
              <textarea
                className="form-control"
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Optional supplier order notes"
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsNewModalOpen(false)}
                disabled={submitting}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Creating...' : 'Save as Draft'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Order Detail & Workflow Transition Modal */}
      {selectedOrder && (
        <Modal
          isOpen={!!selectedOrder}
          title={`Receipt Workflow: ${selectedOrder.reference}`}
          onClose={() => setSelectedOrder(null)}
        >
          <div style={{ marginBottom: '18px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <div>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Status: </span>
                <Badge variant={selectedOrder.status === 'DONE' ? 'success' : selectedOrder.status === 'READY' ? 'warning' : 'info'}>
                  {selectedOrder.status}
                </Badge>
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Supplier: <strong>{selectedOrder.supplierName}</strong>
              </div>
            </div>

            {/* Workflow Pipeline Indicator */}
            <div style={{ display: 'flex', gap: '6px', marginBottom: '20px' }}>
              {(['DRAFT', 'WAITING', 'READY', 'DONE'] as OrderStatus[]).map((st, i) => {
                const isActive = selectedOrder.status === st;
                return (
                  <div
                    key={st}
                    style={{
                      flex: 1,
                      padding: '8px 4px',
                      textAlign: 'center',
                      fontSize: '0.75rem',
                      fontWeight: 700,
                      borderRadius: '4px',
                      background: isActive ? 'var(--primary)' : 'var(--surface-sunken)',
                      color: isActive ? '#fff' : 'var(--text-muted)',
                      border: '1px solid var(--border-subtle)',
                    }}
                  >
                    {i + 1}. {st}
                  </div>
                );
              })}
            </div>

            {/* Line items table */}
            <h4 style={{ fontSize: '0.9rem', marginBottom: '8px' }}>Received Line Items</h4>
            <div className="table-container" style={{ marginBottom: '20px' }}>
              <table className="custom-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Expected</th>
                    <th>Received</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedOrder.lines?.map((line) => (
                    <tr key={line.id}>
                      <td>
                        <div style={{ fontWeight: 600 }}>{line.productName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>SKU: {line.productSku}</div>
                      </td>
                      <td>{line.expectedQuantity}</td>
                      <td>
                        <strong style={{ color: 'var(--success)' }}>{line.receivedQuantity}</strong>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Status Transition Actions */}
            {selectedOrder.status !== 'DONE' && selectedOrder.status !== 'CANCELED' && (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-subtle)', paddingTop: '16px' }}>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  style={{ color: 'var(--danger)' }}
                  disabled={statusUpdating}
                  onClick={() => handleStatusTransition(selectedOrder.id, 'CANCELED')}
                >
                  Cancel Receipt
                </button>

                <div style={{ display: 'flex', gap: '10px' }}>
                  {selectedOrder.status === 'DRAFT' && (
                    <button
                      type="button"
                      className="btn btn-secondary"
                      disabled={statusUpdating}
                      onClick={() => handleStatusTransition(selectedOrder.id, 'WAITING')}
                    >
                      Mark Waiting
                    </button>
                  )}

                  {selectedOrder.status === 'WAITING' && (
                    <button
                      type="button"
                      className="btn btn-secondary"
                      disabled={statusUpdating}
                      onClick={() => handleStatusTransition(selectedOrder.id, 'READY')}
                    >
                      Mark Ready
                    </button>
                  )}

                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={statusUpdating}
                    onClick={() => handleStatusTransition(selectedOrder.id, 'DONE')}
                  >
                    <CheckCircle size={16} />
                    <span>Complete & Credit Inventory</span>
                  </button>
                </div>
              </div>
            )}

            {selectedOrder.status === 'DONE' && (
              <div style={{ padding: '14px', background: 'rgba(34, 197, 94, 0.08)', borderRadius: '6px', border: '1px solid rgba(34, 197, 94, 0.2)', textAlign: 'center', color: 'var(--success)' }}>
                <CheckCircle size={20} style={{ margin: '0 auto 6px' }} />
                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>Receipt Fully Processed and Completed</div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  Stock balances and audit ledger entries were updated atomically at {formatDate(selectedOrder.completedAt || selectedOrder.updatedAt)}
                </div>
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
};

export default ReceiptsWorkflowPage;
