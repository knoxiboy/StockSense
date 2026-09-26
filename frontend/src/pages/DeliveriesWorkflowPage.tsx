import React, { useState, useEffect, useCallback } from 'react';
import { Plus, CheckCircle, Search, RefreshCw, Eye, Trash2, CheckSquare, Square } from 'lucide-react';
import { deliveryOrderApi } from '../api/orders';
import { productApi } from '../api/products';
import { locationApi } from '../api/warehouses';
import { DeliveryOrder, OrderStatus } from '../types/order';
import { Product } from '../types/product';
import { Location } from '../types/warehouse';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import Badge from '../components/ui/Badge';
import Modal from '../components/ui/Modal';
import { formatDate } from '../utils/formatters';

interface DeliveriesWorkflowPageProps {
  onSuccessToast?: (msg: string) => void;
  onErrorToast?: (msg: string) => void;
}

export const DeliveriesWorkflowPage: React.FC<DeliveriesWorkflowPageProps> = ({
  onSuccessToast = () => {},
  onErrorToast = () => {},
}) => {
  const [orders, setOrders] = useState<DeliveryOrder[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>('');
  const [search, setSearch] = useState<string>('');

  // New Order Modal
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [ref, setRef] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [customerContact, setCustomerContact] = useState('');
  const [deliveryAddress, setDeliveryAddress] = useState('');
  const [sourceLocId, setSourceLocId] = useState<number>(0);
  const [notes, setNotes] = useState('');
  const [lines, setLines] = useState<{ productId: number; orderedQuantity: number; deliveredQuantity: number }[]>([]);
  const [submitting, setSubmitting] = useState(false);

  // Detail Modal
  const [selectedOrder, setSelectedOrder] = useState<DeliveryOrder | null>(null);
  const [statusUpdating, setStatusUpdating] = useState(false);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const [orderList, productList, locList] = await Promise.all([
        deliveryOrderApi.getAll(statusFilter ? statusFilter : undefined),
        productApi.getAll(),
        locationApi.getAll(),
      ]);
      setOrders(orderList);
      setProducts(productList);
      setLocations(locList);
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to load delivery orders');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, onErrorToast]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleAddLine = () => {
    if (products.length === 0) return;
    setLines([...lines, { productId: products[0].id, orderedQuantity: 5, deliveredQuantity: 5 }]);
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
    if (!ref.trim() || !customerName.trim()) {
      onErrorToast('Reference and Customer Name are required');
      return;
    }
    if (lines.length === 0) {
      onErrorToast('Add at least one product line to the delivery order');
      return;
    }

    setSubmitting(true);
    try {
      await deliveryOrderApi.create({
        reference: ref.trim(),
        customerName: customerName.trim(),
        customerContact: customerContact.trim() || undefined,
        deliveryAddress: deliveryAddress.trim() || undefined,
        sourceLocationId: sourceLocId || undefined,
        notes: notes.trim() || undefined,
        lines,
      });
      onSuccessToast('Delivery order created successfully');
      setIsNewModalOpen(false);
      resetNewForm();
      fetchOrders();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to create delivery order');
    } finally {
      setSubmitting(false);
    }
  };

  const handleStatusTransition = async (orderId: number, targetStatus: OrderStatus) => {
    setStatusUpdating(true);
    try {
      const updated = await deliveryOrderApi.updateStatus(orderId, targetStatus);
      onSuccessToast(`Delivery order moved to ${targetStatus}`);
      setSelectedOrder(updated);
      fetchOrders();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to transition delivery order status. Check stock availability.');
    } finally {
      setStatusUpdating(false);
    }
  };

  const handleTogglePickPack = async (lineId: number, field: 'picked' | 'packed', currentVal: boolean) => {
    if (!selectedOrder) return;
    try {
      const payload: any = { [field]: !currentVal };
      const updated = await deliveryOrderApi.updateLineProgress(selectedOrder.id, lineId, payload);
      setSelectedOrder(updated);
      fetchOrders();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to update line progress');
    }
  };

  const resetNewForm = () => {
    setRef(`DEL-${new Date().getFullYear()}-${Math.floor(1000 + Math.random() * 9000)}`);
    setCustomerName('');
    setCustomerContact('');
    setDeliveryAddress('');
    setSourceLocId(locations[0]?.id || 0);
    setNotes('');
    setLines(products.length > 0 ? [{ productId: products[0].id, orderedQuantity: 5, deliveredQuantity: 5 }] : []);
  };

  const filteredOrders = orders.filter((o) => {
    return (
      o.reference.toLowerCase().includes(search.toLowerCase()) ||
      o.customerName.toLowerCase().includes(search.toLowerCase())
    );
  });

  return (
    <div className="deliveries-workflow-container">
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Outbound Delivery Orders</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Multi-line client order fulfillment: Draft → Waiting → Ready (Picked/Packed) → Done (stock is deducted)
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
          <span>New Delivery Order</span>
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
                placeholder="Search reference or customer..."
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
            <LoadingSpinner message="Loading outbound delivery orders..." />
          </div>
        ) : filteredOrders.length === 0 ? (
          <EmptyState
            title="No delivery documents found"
            description="Create client delivery orders with picking and packing tracking."
            actionText="Create Delivery Order"
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
                  <th>Customer</th>
                  <th>Source Location</th>
                  <th>Items</th>
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
                        <div style={{ fontWeight: 600 }}>{o.customerName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{o.deliveryAddress || o.customerContact || 'No address'}</div>
                      </td>
                      <td>
                        {o.sourceLocation ? (
                          <>
                            <div style={{ fontWeight: 600 }}>{o.sourceLocation.name}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{o.sourceLocation.warehouseName}</div>
                          </>
                        ) : (
                          'Default Stock'
                        )}
                      </td>
                      <td>
                        <span style={{ fontWeight: 600 }}>{o.lines?.length || 0} line item(s)</span>
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

      {/* New Delivery Order Modal */}
      {isNewModalOpen && (
        <Modal
          isOpen={isNewModalOpen}
          title="Create Outbound Delivery Order"
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
                <label className="form-label">Source Stock Location *</label>
                <select
                  className="form-control"
                  required
                  value={sourceLocId}
                  onChange={(e) => setSourceLocId(Number(e.target.value))}
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
                <label className="form-label">Customer Name *</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                  placeholder="e.g. Acme Corporation"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Customer Contact</label>
                <input
                  type="text"
                  className="form-control"
                  value={customerContact}
                  onChange={(e) => setCustomerContact(e.target.value)}
                  placeholder="e.g. logistics@acme.com"
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Delivery Address</label>
              <input
                type="text"
                className="form-control"
                value={deliveryAddress}
                onChange={(e) => setDeliveryAddress(e.target.value)}
                placeholder="e.g. 100 Logistics Blvd, Warehouse Bay 4"
              />
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
                    placeholder="Ordered"
                    value={line.orderedQuantity}
                    onChange={(e) => handleLineChange(idx, 'orderedQuantity', parseFloat(e.target.value) || 0)}
                  />
                  <input
                    type="number"
                    step="any"
                    min="0"
                    className="form-control"
                    placeholder="Deliver"
                    value={line.deliveredQuantity}
                    onChange={(e) => handleLineChange(idx, 'deliveredQuantity', parseFloat(e.target.value) || 0)}
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
                placeholder="Special delivery notes or gate instructions"
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
                {submitting ? 'Creating...' : 'Save Delivery Draft'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Order Detail & Workflow Transition Modal */}
      {selectedOrder && (
        <Modal
          isOpen={!!selectedOrder}
          title={`Delivery Workflow: ${selectedOrder.reference}`}
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
                Customer: <strong>{selectedOrder.customerName}</strong>
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

            {/* Line items with Picking & Packing tracking */}
            <h4 style={{ fontSize: '0.9rem', marginBottom: '8px' }}>Order Line Items (Picking & Packing)</h4>
            <div className="table-container" style={{ marginBottom: '20px' }}>
              <table className="custom-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Ordered</th>
                    <th>Picked</th>
                    <th>Packed</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedOrder.lines?.map((line) => (
                    <tr key={line.id}>
                      <td>
                        <div style={{ fontWeight: 600 }}>{line.productName}</div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>SKU: {line.productSku}</div>
                      </td>
                      <td>{line.orderedQuantity}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          style={{ padding: '2px 8px', display: 'flex', alignItems: 'center', gap: '4px' }}
                          disabled={selectedOrder.status === 'DONE' || selectedOrder.status === 'CANCELED'}
                          onClick={() => handleTogglePickPack(line.id, 'picked', line.picked)}
                        >
                          {line.picked ? <CheckSquare size={14} style={{ color: 'var(--success)' }} /> : <Square size={14} />}
                          <span>{line.picked ? 'Picked' : 'To Pick'}</span>
                        </button>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          style={{ padding: '2px 8px', display: 'flex', alignItems: 'center', gap: '4px' }}
                          disabled={selectedOrder.status === 'DONE' || selectedOrder.status === 'CANCELED'}
                          onClick={() => handleTogglePickPack(line.id, 'packed', line.packed)}
                        >
                          {line.packed ? <CheckSquare size={14} style={{ color: 'var(--success)' }} /> : <Square size={14} />}
                          <span>{line.packed ? 'Packed' : 'To Pack'}</span>
                        </button>
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
                  Cancel Order
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
                    <span>Dispatch & Deduct Stock</span>
                  </button>
                </div>
              </div>
            )}

            {selectedOrder.status === 'DONE' && (
              <div style={{ padding: '14px', background: 'rgba(34, 197, 94, 0.08)', borderRadius: '6px', border: '1px solid rgba(34, 197, 94, 0.2)', textAlign: 'center', color: 'var(--success)' }}>
                <CheckCircle size={20} style={{ margin: '0 auto 6px' }} />
                <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>Delivery Completed & Stock Deducted</div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  Inventory balances were debited atomically with non-negative protection at {formatDate(selectedOrder.completedAt || selectedOrder.updatedAt)}
                </div>
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
};

export default DeliveriesWorkflowPage;
