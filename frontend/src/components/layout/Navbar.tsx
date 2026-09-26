import React from 'react';
import { PageId } from './Sidebar';

interface NavbarProps {
  currentPage: PageId;
  totalProductsCount?: number;
}

const pageTitles: Record<PageId, { title: string; subtitle: string }> = {
  dashboard: {
    title: 'Inventory Dashboard',
    subtitle: 'System KPIs, replenishment alerts, and operational overview',
  },
  products: {
    title: 'Product Catalog',
    subtitle: 'Manage products, SKUs, reorder levels, and categories',
  },
  operations: {
    title: 'Inventory Operations',
    subtitle: 'Process receipts, deliveries, and stock adjustments with live movement history',
  },
  receipts: {
    title: 'Incoming Receipts',
    subtitle: 'Receive purchase orders and inbound stock from suppliers',
  },
  deliveries: {
    title: 'Delivery Orders',
    subtitle: 'Validate outgoing client deliveries and maintain non-negative stock',
  },
  adjustments: {
    title: 'Stock Adjustments',
    subtitle: 'Reconcile physical stock counts with system balances',
  },
  ledger: {
    title: 'Stock Ledger',
    subtitle: 'Immutable audit trail of all inventory balance transactions',
  },
};

export const Navbar: React.FC<NavbarProps> = ({ currentPage, totalProductsCount }) => {
  const meta = pageTitles[currentPage] || pageTitles.dashboard;

  return (
    <header className="navbar">
      <div className="navbar-title-wrap">
        <h1>{meta.title}</h1>
        <p>{meta.subtitle}</p>
      </div>

      <div className="navbar-actions">
        {currentPage === 'products' && totalProductsCount !== undefined && (
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Total Items: <strong style={{ color: 'var(--text-main)' }}>{totalProductsCount}</strong>
          </span>
        )}
      </div>
    </header>
  );
};

export default Navbar;
