import React, { useState, useEffect, useCallback } from 'react';
import {
  Boxes,
  Package,
  AlertTriangle,
  ArrowDownLeft,
  ArrowUpRight,
  ArrowLeftRight,
  SlidersHorizontal,
  History,
  TrendingUp,
  RefreshCw,
  Warehouse,
} from 'lucide-react';
import { dashboardApi } from '../api/dashboard';
import { productApi } from '../api/products';
import { operationApi } from '../api/operations';
import { warehouseApi, locationApi } from '../api/warehouses';
import { Product, StockBalanceResponse } from '../types/product';
import { StockOperation } from '../types/operation';
import { Warehouse as WarehouseType, Location } from '../types/warehouse';
import { DashboardKpis } from '../types/dashboard';
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
  const [kpis, setKpis] = useState<DashboardKpis>({
    totalProducts: 0,
    totalProductsInStock: 0,
    totalStockUnits: 0,
    lowStockProducts: 0,
    outOfStockProducts: 0,
    pendingReceipts: 0,
    pendingDeliveries: 0,
    scheduledTransfers: 0,
  });

  const [productsWithStock, setProductsWithStock] = useState<ProductWithStock[]>([]);
  const [recentOperations, setRecentOperations] = useState<StockOperation[]>([]);
  const [warehouses, setWarehouses] = useState<WarehouseType[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);

  // Real-data Filters
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<string>('');
  const [selectedLocationId, setSelectedLocationId] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [categories, setCategories] = useState<string[]>([]);

  const fetchDashboardData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const whId = selectedWarehouseId ? Number(selectedWarehouseId) : undefined;
      const locId = selectedLocationId ? Number(selectedLocationId) : undefined;
      const cat = selectedCategory || undefined;

      const [kpiData, products, operations, whList, locList, catList] = await Promise.all([
        dashboardApi.getStats(whId, locId, cat),
        productApi.getAll(undefined, cat),
        operationApi.getAll({ page: 0, size: 6 }),
        warehouseApi.getAll(),
        locationApi.getAll(whId),
        productApi.getCategories(),
      ]);

      setKpis(kpiData);
      setWarehouses(whList);
      setLocations(locList);
      setCategories(catList);
      setRecentOperations(operations);

      // Fetch stock balances for catalog display
      const stockBalances = await Promise.all(
        products.slice(0, 15).map(async (p) => {
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
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load live dashboard data';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [selectedWarehouseId, selectedLocationId, selectedCategory]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  if (loading && productsWithStock.length === 0 && !error) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: '80px 0' }}>
        <LoadingSpinner message="Calculating live warehouse KPIs and stock metrics..." />
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

  const lowStockItems = productsWithStock.filter((item) => {
    const q = typeof item.stock.quantity === 'string' ? parseFloat(item.stock.quantity) : item.stock.quantity;
    const reorder = typeof item.product.reorderLevel === 'string' ? parseFloat(item.product.reorderLevel) : item.product.reorderLevel;
    return q <= reorder;
  });

  return (
    <div className="dashboard-container">
      {/* Real-data Filter Ribbon */}
      <div className="card" style={{ padding: '14px 20px', marginBottom: '22px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '14px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Warehouse size={18} style={{ color: 'var(--primary)' }} />
            <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Scope & Filters:</span>
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
            {/* Warehouse Filter */}
            <select
              className="form-control"
              style={{ width: '180px', height: '36px', fontSize: '0.82rem' }}
              value={selectedWarehouseId}
              onChange={(e) => {
                setSelectedWarehouseId(e.target.value);
                setSelectedLocationId('');
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
              className="form-control"
              style={{ width: '180px', height: '36px', fontSize: '0.82rem' }}
              value={selectedLocationId}
              onChange={(e) => setSelectedLocationId(e.target.value)}
            >
              <option value="">All Locations</option>
              {locations.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.name} ({l.code})
                </option>
              ))}
            </select>

            {/* Category Filter */}
            <select
              className="form-control"
              style={{ width: '160px', height: '36px', fontSize: '0.82rem' }}
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
            >
              <option value="">All Categories</option>
              {categories.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>

            <button type="button" className="btn btn-secondary btn-sm" onClick={fetchDashboardData} style={{ height: '36px' }}>
              <RefreshCw size={13} />
              <span>Update</span>
            </button>
          </div>
        </div>
      </div>

      {/* 6 Real-Data KPI Cards */}
      <div className="metric-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        {/* 1. Total Products */}
        <div className="metric-card" onClick={() => onNavigate('products')} style={{ cursor: 'pointer' }}>
          <div className="metric-icon-wrap metric-icon-blue">
            <Package size={22} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Total Products</span>
            <span className="metric-value">{kpis.totalProducts}</span>
            <span className="metric-subtext">{kpis.totalProductsInStock} in stock</span>
          </div>
        </div>

        {/* 2. Total Stock Units */}
        <div className="metric-card">
          <div className="metric-icon-wrap metric-icon-green">
            <Boxes size={22} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Total Stock Units</span>
            <span className="metric-value">{Number(kpis.totalStockUnits).toLocaleString(undefined, { maximumFractionDigits: 1 })}</span>
            <span className="metric-subtext">Sum across active bins</span>
          </div>
        </div>

        {/* 3. Replenishment Alert */}
        <div className="metric-card" onClick={() => onNavigate('products')} style={{ cursor: 'pointer' }}>
          <div className="metric-icon-wrap metric-icon-amber">
            <AlertTriangle size={22} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Needs Replenishment</span>
            <span className="metric-value" style={{ color: kpis.lowStockProducts > 0 ? '#d97706' : 'inherit' }}>
              {kpis.lowStockProducts}
            </span>
            <span className="metric-subtext">{kpis.outOfStockProducts} out of stock</span>
          </div>
        </div>

        {/* 4. Pending Receipts */}
        <div className="metric-card" onClick={() => onNavigate('receipts')} style={{ cursor: 'pointer' }}>
          <div className="metric-icon-wrap" style={{ background: 'rgba(34, 197, 94, 0.1)', color: 'var(--success)' }}>
            <ArrowDownLeft size={22} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Pending Receipts</span>
            <span className="metric-value">{kpis.pendingReceipts}</span>
            <span className="metric-subtext">In Waiting/Ready status</span>
          </div>
        </div>

        {/* 5. Pending Deliveries */}
        <div className="metric-card" onClick={() => onNavigate('deliveries')} style={{ cursor: 'pointer' }}>
          <div className="metric-icon-wrap" style={{ background: 'rgba(59, 130, 246, 0.1)', color: 'var(--primary)' }}>
            <ArrowUpRight size={22} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Pending Deliveries</span>
            <span className="metric-value">{kpis.pendingDeliveries}</span>
            <span className="metric-subtext">Awaiting picking/dispatch</span>
          </div>
        </div>

        {/* 6. Scheduled Transfers */}
        <div className="metric-card" onClick={() => onNavigate('transfers')} style={{ cursor: 'pointer' }}>
          <div className="metric-icon-wrap metric-icon-purple">
            <ArrowLeftRight size={22} />
          </div>
          <div className="metric-info">
            <span className="metric-label">Scheduled Transfers</span>
            <span className="metric-value">{kpis.scheduledTransfers}</span>
            <span className="metric-subtext">Internal location shifts</span>
          </div>
        </div>
      </div>

      {/* Quick Action Navigation Bar */}
      <div
        className="card"
        style={{
          padding: '16px 20px',
          marginBottom: '28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '14px',
        }}
      >
        <div>
          <h3 style={{ fontSize: '0.95rem', fontWeight: 600 }}>Quick Operational Links</h3>
          <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Direct access to document workflows, transfer orders, and location configurations
          </p>
        </div>
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => onNavigate('receipts')}>
            <ArrowDownLeft size={15} style={{ color: 'var(--success)' }} />
            <span>Receipts</span>
          </button>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => onNavigate('deliveries')}>
            <ArrowUpRight size={15} style={{ color: 'var(--primary)' }} />
            <span>Deliveries</span>
          </button>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => onNavigate('transfers')}>
            <ArrowLeftRight size={15} style={{ color: '#8b5cf6' }} />
            <span>Transfers</span>
          </button>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => onNavigate('adjustments')}>
            <SlidersHorizontal size={15} style={{ color: '#d97706' }} />
            <span>Adjustments</span>
          </button>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => onNavigate('warehouses')}>
            <Warehouse size={15} />
            <span>Warehouses</span>
          </button>
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => onNavigate('ledger')}>
            <History size={15} />
            <span>Stock Ledger</span>
          </button>
        </div>
      </div>

      {/* Two Column Layout: Watchlist and Movement History */}
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
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <AlertTriangle size={18} style={{ color: '#d97706' }} />
              <h3 style={{ fontSize: '0.95rem', fontWeight: 600 }}>Replenishment Watchlist</h3>
            </div>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => onNavigate('products')}
            >
              Catalog
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
              onClick={() => onNavigate('ledger')}
            >
              Audit Trail
            </button>
          </div>

          {recentOperations.length === 0 ? (
            <div style={{ padding: '40px 20px', textAlign: 'center' }}>
              <EmptyState
                title="No operations recorded yet"
                description="Use Receipts, Deliveries, or Transfers to record your first inventory transaction."
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

                    let badgeVariant: 'success' | 'info' | 'warning' | 'secondary' = 'info';
                    if (op.operationType === 'RECEIPT') badgeVariant = 'success';
                    if (op.operationType === 'ADJUSTMENT') badgeVariant = 'warning';
                    if (op.operationType === 'TRANSFER') badgeVariant = 'info';

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
