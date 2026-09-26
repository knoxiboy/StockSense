import React from 'react';
import {
  Boxes,
  LayoutDashboard,
  Package,
  ArrowDownLeft,
  ArrowUpRight,
  SlidersHorizontal,
  History,
} from 'lucide-react';

export type PageId = 'dashboard' | 'products' | 'receipts' | 'deliveries' | 'adjustments' | 'ledger';

interface SidebarProps {
  currentPage: PageId;
  onNavigate: (page: PageId) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentPage, onNavigate }) => {
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
          <span className={`nav-badge-pill ${currentPage === 'products' ? 'active-pill' : ''}`}>
            Core
          </span>
        </button>

        <div className="nav-section-title">Operations</div>
        <button
          type="button"
          className={`nav-item ${currentPage === 'receipts' ? 'active' : ''}`}
          onClick={() => onNavigate('receipts')}
        >
          <ArrowDownLeft size={18} />
          <span>Receipts</span>
        </button>

        <button
          type="button"
          className={`nav-item ${currentPage === 'deliveries' ? 'active' : ''}`}
          onClick={() => onNavigate('deliveries')}
        >
          <ArrowUpRight size={18} />
          <span>Deliveries</span>
        </button>

        <button
          type="button"
          className={`nav-item ${currentPage === 'adjustments' ? 'active' : ''}`}
          onClick={() => onNavigate('adjustments')}
        >
          <SlidersHorizontal size={18} />
          <span>Adjustments</span>
        </button>

        <div className="nav-section-title">Audit</div>
        <button
          type="button"
          className={`nav-item ${currentPage === 'ledger' ? 'active' : ''}`}
          onClick={() => onNavigate('ledger')}
        >
          <History size={18} />
          <span>Stock Ledger</span>
        </button>
      </nav>

      <div className="sidebar-footer">
        <div className="system-status">
          <span className="status-dot"></span>
          <span>Phase 1 • Core Online</span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
