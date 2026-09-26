import React from 'react';
import {
  Boxes,
  LayoutDashboard,
  Package,
  Layers,
  ArrowDownLeft,
  ArrowUpRight,
  ArrowLeftRight,
  SlidersHorizontal,
  History,
  Warehouse,
  Settings as SettingsIcon,
  User as UserIcon,
} from 'lucide-react';
import { User } from '../../types/auth';

export type PageId =
  | 'dashboard'
  | 'products'
  | 'operations'
  | 'receipts'
  | 'deliveries'
  | 'transfers'
  | 'adjustments'
  | 'ledger'
  | 'warehouses'
  | 'settings';

interface SidebarProps {
  currentPage: PageId;
  onNavigate: (page: PageId) => void;
  currentUser: User | null;
  onOpenAuth: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentPage,
  onNavigate,
  currentUser,
  onOpenAuth,
}) => {
  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="brand-icon">
          <Boxes size={22} />
        </div>
        <div>
          <div className="brand-title">StockSense</div>
          <div className="brand-subtitle">Inventory Management</div>
        </div>
      </div>

      <nav className="sidebar-nav">
        <div className="nav-section-title">Overview</div>
        <button
          type="button"
          className={`nav-item ${currentPage === 'dashboard' ? 'active' : ''}`}
          onClick={() => onNavigate('dashboard')}
        >
          <LayoutDashboard size={18} />
          <span>Dashboard</span>
        </button>

        <div className="nav-section-title">Catalog</div>
        <button
          type="button"
          className={`nav-item ${currentPage === 'products' ? 'active' : ''}`}
          onClick={() => onNavigate('products')}
        >
          <Package size={18} />
          <span>Products</span>
        </button>

        <div className="nav-section-title">Inventory Operations</div>
        <button
          type="button"
          className={`nav-item ${currentPage === 'receipts' ? 'active' : ''}`}
          onClick={() => onNavigate('receipts')}
        >
          <ArrowDownLeft size={18} />
          <span>Receipt Orders</span>
        </button>

        <button
          type="button"
          className={`nav-item ${currentPage === 'deliveries' ? 'active' : ''}`}
          onClick={() => onNavigate('deliveries')}
        >
          <ArrowUpRight size={18} />
          <span>Delivery Orders</span>
        </button>

        <button
          type="button"
          className={`nav-item ${currentPage === 'transfers' ? 'active' : ''}`}
          onClick={() => onNavigate('transfers')}
        >
          <ArrowLeftRight size={18} />
          <span>Internal Transfers</span>
        </button>

        <button
          type="button"
          className={`nav-item ${currentPage === 'adjustments' ? 'active' : ''}`}
          onClick={() => onNavigate('adjustments')}
        >
          <SlidersHorizontal size={18} />
          <span>Adjustments</span>
        </button>

        <button
          type="button"
          className={`nav-item ${currentPage === 'operations' ? 'active' : ''}`}
          onClick={() => onNavigate('operations')}
        >
          <Layers size={18} />
          <span>Quick Operations</span>
        </button>

        <div className="nav-section-title">Locations & Audit</div>
        <button
          type="button"
          className={`nav-item ${currentPage === 'warehouses' ? 'active' : ''}`}
          onClick={() => onNavigate('warehouses')}
        >
          <Warehouse size={18} />
          <span>Warehouses & Locations</span>
        </button>

        <button
          type="button"
          className={`nav-item ${currentPage === 'ledger' ? 'active' : ''}`}
          onClick={() => onNavigate('ledger')}
        >
          <History size={18} />
          <span>Stock Ledger</span>
        </button>

        <div className="nav-section-title">Configuration</div>
        <button
          type="button"
          className={`nav-item ${currentPage === 'settings' ? 'active' : ''}`}
          onClick={() => onNavigate('settings')}
        >
          <SettingsIcon size={18} />
          <span>Settings</span>
        </button>
      </nav>

      <div className="sidebar-footer">
        <div
          onClick={onOpenAuth}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            padding: '8px 12px',
            marginBottom: '10px',
            borderRadius: '6px',
            background: 'var(--surface-sunken)',
            cursor: 'pointer',
            border: '1px solid var(--border-subtle)',
          }}
        >
          <div
            style={{
              width: '28px',
              height: '32px',
              borderRadius: '50%',
              background: currentUser ? (currentUser.role === 'MANAGER' ? '#2563eb' : '#059669') : 'var(--text-muted)',
              color: '#fff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '0.85rem',
              fontWeight: 700,
            }}
          >
            {currentUser ? currentUser.fullName.charAt(0).toUpperCase() : <UserIcon size={14} />}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: '0.82rem', fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {currentUser ? currentUser.fullName : 'Account / Login'}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '2px' }}>
              {currentUser ? (
                <span style={{
                  fontSize: '0.68rem',
                  fontWeight: 700,
                  padding: '1px 5px',
                  borderRadius: '4px',
                  background: currentUser.role === 'MANAGER' ? 'rgba(59, 130, 246, 0.2)' : 'rgba(16, 185, 129, 0.2)',
                  color: currentUser.role === 'MANAGER' ? '#60a5fa' : '#34d399',
                  letterSpacing: '0.04em'
                }}>
                  {currentUser.role}
                </span>
              ) : (
                <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Click to authenticate</span>
              )}
            </div>
          </div>
        </div>

        <div className="system-status">
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
