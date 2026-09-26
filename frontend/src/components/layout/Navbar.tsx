import React from 'react';
import { PageId } from './Sidebar';

interface NavbarProps {
  currentPage: PageId;
  totalProductsCount?: number;
}

const pageTitles: Record<PageId, { title: string; subtitle: string }> = {
  dashboard: {
    title: 'Inventory Dashboard',
    subtitle: 'System KPIs, alerts, and operational overview',
  },
  products: {
    title: 'Product Catalog',
    subtitle: 'Manage products, SKUs, reorder levels, and categories',
  },
  receipts: {
    title: 'Incoming Receipts',
    subtitle: 'Receive purchase orders and inbound stock from suppliers',
  },
  deliveries: {
    title: 'Delivery Orders',
    subtitle: 'Pick, pack, and validate outgoing shipments to customers',
  },
  adjustments: {
    title: 'Stock Adjustments',
    subtitle: 'Reconcile physical stock counts with system balances',
  },
  ledger: {
    title: 'Stock Ledger',
    subtitle: 'Immutable audit trail of all inventory transactions',
  },
};

export const Navbar: React.FC<NavbarProps> = ({ currentPage, totalProductsCount }) => {
  const meta = pageTitles[currentPage];

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
