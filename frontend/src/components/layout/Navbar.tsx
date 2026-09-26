import React from 'react';
import { PageId } from './Sidebar';
import { User } from '../../types/auth';
import { LogIn } from 'lucide-react';

interface NavbarProps {
  currentPage: PageId;
  totalProductsCount?: number;
  currentUser: User | null;
  onOpenAuth: () => void;
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
    title: 'Quick Operations',
    subtitle: 'Process immediate receipts, deliveries, and stock adjustments with live movement history',
  },
  receipts: {
    title: 'Receipt Orders & Workflows',
    subtitle: 'Multi-line purchase orders from suppliers: Draft → Waiting → Ready → Done',
  },
  deliveries: {
    title: 'Delivery Orders & Workflows',
    subtitle: 'Multi-line client order fulfillment with picking and packing status tracking',
  },
  transfers: {
    title: 'Internal Transfers',
    subtitle: 'Move stock between warehouse locations with atomic zero-sum ledger updates',
  },
  adjustments: {
    title: 'Stock Adjustments',
    subtitle: 'Reconcile physical stock counts with system balances',
  },
  ledger: {
    title: 'Stock Ledger',
    subtitle: 'Immutable audit trail of all inventory balance transactions',
  },
  warehouses: {
    title: 'Warehouses & Locations',
    subtitle: 'Multi-warehouse facilities, storage zones, aisles, and bins',
  },
  settings: {
    title: 'System Settings',
    subtitle: 'Runtime configuration, database status, and SMTP email setup guide',
  },
};

export const Navbar: React.FC<NavbarProps> = ({
  currentPage,
  totalProductsCount,
  currentUser,
  onOpenAuth,
}) => {
  const meta = pageTitles[currentPage] || pageTitles.dashboard;

  return (
    <header className="navbar">
      <div className="navbar-title-wrap">
        <h1>{meta.title}</h1>
        <p>{meta.subtitle}</p>
      </div>

      <div className="navbar-actions" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {currentPage === 'products' && totalProductsCount !== undefined && (
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Total Items: <strong style={{ color: 'var(--text-main)' }}>{totalProductsCount}</strong>
          </span>
        )}

        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={onOpenAuth}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
          title="Click to view profile or sign out"
        >
          {currentUser ? (
            <>
              <div
                style={{
                  width: '22px',
                  height: '22px',
                  borderRadius: '50%',
                  background: currentUser.role === 'MANAGER' ? '#2563eb' : '#059669',
                  color: '#fff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                }}
              >
                {currentUser.fullName.charAt(0).toUpperCase()}
              </div>
              <span style={{ fontWeight: 600 }}>{currentUser.fullName}</span>
              <span
                style={{
                  fontSize: '0.7rem',
                  fontWeight: 700,
                  padding: '2px 6px',
                  borderRadius: '10px',
                  background: currentUser.role === 'MANAGER' ? 'rgba(59, 130, 246, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                  color: currentUser.role === 'MANAGER' ? '#60a5fa' : '#34d399',
                  border: `1px solid ${currentUser.role === 'MANAGER' ? 'rgba(59, 130, 246, 0.3)' : 'rgba(16, 185, 129, 0.3)'}`,
                  letterSpacing: '0.04em'
                }}
              >
                {currentUser.role}
              </span>
            </>
          ) : (
            <>
              <LogIn size={14} />
              <span>Sign In</span>
            </>
          )}
        </button>
      </div>
    </header>
  );
};

export default Navbar;
