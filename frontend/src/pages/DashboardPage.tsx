import React, { useState, useEffect, useCallback } from 'react';
import {
  Boxes,
  Package,
  AlertTriangle,
  ArrowDownLeft,
  ArrowUpRight,
  SlidersHorizontal,
  History,
  TrendingUp,
  RefreshCw,
} from 'lucide-react';
import { productApi } from '../api/products';
import { operationApi } from '../api/operations';
import { Product, StockBalanceResponse } from '../types/product';
import { StockOperation } from '../types/operation';
import { PageId } from '../components/layout/Sidebar';
import Badge from '../components/ui/Badge';
import LoadingSpinner from '../components/common/LoadingSpinner';
import EmptyState from '../components/common/EmptyState';
import { formatQuantity, formatDate } from '../utils/formatters';

interface DashboardPageProps {
  onNavigate: (page: PageId) => void;
  onOpenReceiptForProduct?: (productId: number) => void;
}

interface ProductWithStock {
  product: Product;
  stock: StockBalanceResponse;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({
  onNavigate,
  onOpenReceiptForProduct,
}) => {
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [productsWithStock, setProductsWithStock] = useState<ProductWithStock[]>([]);
  const [recentOperations, setRecentOperations] = useState<StockOperation[]>([]);

  const fetchDashboardData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [products, operations] = await Promise.all([
        productApi.getAll(),
        operationApi.getAll({ page: 0, size: 6 }),
      ]);

      // Fetch stock for all products to compute verified inventory aggregates
      const stockBalances = await Promise.all(
        products.map(async (p) => {
          try {
            const stock = await productApi.getStock(p.id);
            return { product: p, stock };
          } catch {
            return {
              product: p,
              stock: {
                id: 0,
                productId: p.id,
                productName: p.name,
                sku: p.sku,
                unit: p.unit,
                quantity: 0,
                reorderLevel: p.reorderLevel,
                updatedAt: p.updatedAt,
              },
            };
          }
        })
      );

      setProductsWithStock(stockBalances);
      setRecentOperations(operations);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load dashboard data';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: '80px 0' }}>
        <LoadingSpinner message="Calculating inventory metrics from live database..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="card" style={{ padding: '32px', textAlign: 'center', borderColor: 'var(--danger-border)' }}>
        <div style={{ color: 'var(--danger)', marginBottom: '12px' }}>
          <AlertTriangle size={36} style={{ margin: '0 auto' }} />
        </div>
        <h3 style={{ fontSize: '1.1rem', marginBottom: '8px', color: 'var(--text-main)' }}>
          Unable to Load Dashboard
        </h3>
        <p style={{ color: 'var(--text-muted)', marginBottom: '20px', maxWidth: '460px', margin: '0 auto 20px' }}>
          {error}
        </p>
        <button type="button" className="btn btn-primary" onClick={fetchDashboardData}>
          <RefreshCw size={16} />
          <span>Retry Connection</span>
        </button>
      </div>
    );
  }

  // Derive verified statistics from complete dataset
  const totalProducts = productsWithStock.length;
  const totalQuantity = productsWithStock.reduce((sum, item) => {
    const q = typeof item.stock.quantity === 'string' ? parseFloat(item.stock.quantity) : item.stock.quantity;
    return sum + (isNaN(q) ? 0 : q);
  }, 0);

  const lowStockItems = productsWithStock.filter((item) => {
    const q = typeof item.stock.quantity === 'string' ? parseFloat(item.stock.quantity) : item.stock.quantity;
    const reorder = typeof item.product.reorderLevel === 'string' ? parseFloat(item.product.reorderLevel) : item.product.reorderLevel;
    return q <= reorder;
  });

  const outOfStockItems = productsWithStock.filter((item) => {
    const q = typeof item.stock.quantity === 'string' ? parseFloat(item.stock.quantity) : item.stock.quantity;
    return q === 0;
  });

  return (
    <div className="dashboard-container">
      {/* KPI Metric Cards */}
      <div className="metric-grid">
        <div className="metric-card">
          <div className="metric-icon-wrap metric-icon-blue">
            <Package size={24} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Total Catalog Products</span>
            <span className="metric-value">{totalProducts}</span>
            <span className="metric-subtext">Active inventory items</span>
          </div>
        </div>

        <div className="metric-card">
          <div className="metric-icon-wrap metric-icon-green">
            <Boxes size={24} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Total Stock Units</span>
            <span className="metric-value">{totalQuantity.toLocaleString(undefined, { maximumFractionDigits: 2 })}</span>
            <span className="metric-subtext">Sum of current on-hand units</span>
          </div>
        </div>

        <div className="metric-card">
          <div className="metric-icon-wrap metric-icon-amber">
            <AlertTriangle size={24} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Needs Replenishment</span>
            <span className="metric-value" style={{ color: lowStockItems.length > 0 ? '#d97706' : 'inherit' }}>
              {lowStockItems.length}
            </span>
            <span className="metric-subtext">
              {outOfStockItems.length} completely out of stock
            </span>
          </div>
        </div>

        <div className="metric-card">
          <div className="metric-icon-wrap metric-icon-purple">
            <TrendingUp size={24} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Recent Operations</span>
            <span className="metric-value">{recentOperations.length}</span>
            <span className="metric-subtext">Latest transactional events</span>
          </div>
        </div>
      </div>

      {/* Quick Actions Bar */}
      <div
        className="card"
        style={{
          padding: '18px 24px',
          marginBottom: '28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <div>
          <h3 style={{ fontSize: '0.98rem', fontWeight: 600 }}>Quick Inventory Actions</h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Execute stock transactions or review the immutable audit trail
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => onNavigate('receipts')}
          >
            <ArrowDownLeft size={16} style={{ color: 'var(--success)' }} />
            <span>New Receipt</span>
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => onNavigate('deliveries')}
          >
            <ArrowUpRight size={16} style={{ color: 'var(--primary)' }} />
            <span>New Delivery</span>
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => onNavigate('adjustments')}
          >
            <SlidersHorizontal size={16} style={{ color: '#d97706' }} />
            <span>Stock Adjustment</span>
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => onNavigate('ledger')}
          >
            <History size={16} />
            <span>Audit Ledger</span>
          </button>
        </div>
      </div>

      {/* Two Column Layout: Low Stock Alerts & Recent Operations */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))', gap: '24px' }}>
        {/* Low Stock Items Card */}
        <div className="card">
          <div
            style={{
              padding: '16px 20px',
              borderBottom: '1px solid var(--border-subtle)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignContent: 'center', gap: '8px' }}>
              <AlertTriangle size={18} style={{ color: '#d97706' }} />
              <h3 style={{ fontSize: '0.95rem', fontWeight: 600 }}>Replenishment Watchlist</h3>
            </div>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => onNavigate('products')}
            >
              View Catalog
            </button>
          </div>

          {lowStockItems.length === 0 ? (
            <div style={{ padding: '40px 20px', textAlign: 'center' }}>
              <div style={{ color: 'var(--success)', marginBottom: '8px' }}>
                <Boxes size={32} style={{ margin: '0 auto' }} />
              </div>
              <p style={{ fontWeight: 600, fontSize: '0.9rem' }}>All stock levels healthy</p>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                No products are currently at or below their reorder threshold.
              </p>
            </div>
          ) : (
            <div className="table-container">
              <table className="custom-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Current Stock</th>
                    <th>Reorder Level</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {lowStockItems.slice(0, 5).map(({ product, stock }) => {
                    const qty = typeof stock.quantity === 'string' ? parseFloat(stock.quantity) : stock.quantity;
                    const isOut = qty === 0;

                    return (
                      <tr key={product.id}>
                        <td>
                          <div style={{ fontWeight: 600 }}>{product.name}</div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            SKU: {product.sku}
                          </div>
                        </td>
                        <td>
                          <span style={{ fontWeight: 700, color: isOut ? 'var(--danger)' : '#b45309' }}>
                            {formatQuantity(stock.quantity, product.unit)}
                          </span>
                          {isOut && (
                            <Badge variant="danger" className="ml-1" style={{ marginLeft: '6px' }}>
                              Out of Stock
                            </Badge>
                          )}
                        </td>
                        <td>{formatQuantity(product.reorderLevel, product.unit)}</td>
                        <td>
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => {
                              if (onOpenReceiptForProduct) {
                                onOpenReceiptForProduct(product.id);
                              } else {
                                onNavigate('receipts');
                              }
                            }}
                          >
                            Receive
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

        {/* Recent Operations Card */}
        <div className="card">
          <div
            style={{
              padding: '16px 20px',
              borderBottom: '1px solid var(--border-subtle)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <TrendingUp size={18} style={{ color: 'var(--primary)' }} />
              <h3 style={{ fontSize: '0.95rem', fontWeight: 600 }}>Recent Stock Movements</h3>
            </div>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => onNavigate('operations')}
            >
              All Movements
            </button>
          </div>

          {recentOperations.length === 0 ? (
            <div style={{ padding: '40px 20px', textAlign: 'center' }}>
              <EmptyState
                title="No operations recorded yet"
                description="Use the Receipts, Deliveries, or Adjustments actions to record your first inventory transaction."
                actionText="Create Receipt"
                onAction={() => onNavigate('receipts')}
              />
            </div>
          ) : (
            <div className="table-container">
              <table className="custom-table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Product</th>
                    <th>Change</th>
                    <th>Resulting Stock</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {recentOperations.map((op) => {
                    const isPositive = op.quantityChange > 0;
                    const isZero = op.quantityChange === 0;

                    let badgeVariant: 'success' | 'info' | 'warning' = 'info';
                    if (op.operationType === 'RECEIPT') badgeVariant = 'success';
                    if (op.operationType === 'ADJUSTMENT') badgeVariant = 'warning';

                    return (
                      <tr key={op.id}>
                        <td>
                          <Badge variant={badgeVariant}>{op.operationType}</Badge>
                        </td>
                        <td>
                          <div style={{ fontWeight: 600 }}>{op.productName}</div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                            SKU: {op.sku}
                          </div>
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
                        <td style={{ fontWeight: 600 }}>
                          {formatQuantity(op.resultingQuantity, op.unit)}
                        </td>
                        <td style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                          {formatDate(op.createdAt)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
