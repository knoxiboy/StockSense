import React, { useState, useRef, useEffect } from 'react';
import { PageId } from './Sidebar';
import { User } from '../../types/auth';
import { LogIn, User as UserIcon, ShieldCheck, LogOut, ChevronDown } from 'lucide-react';

interface NavbarProps {
  currentPage: PageId;
  totalProductsCount?: number;
  currentUser: User | null;
  onOpenAuth: () => void;
  onOpenProfile?: () => void;
  onOpenManagerApprovals?: () => void;
  onLogout?: () => void;
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
  onOpenProfile,
  onOpenManagerApprovals,
  onLogout,
}) => {
  const meta = pageTitles[currentPage] || pageTitles.dashboard;
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown on outside click or Escape key
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    };
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const isManager = currentUser?.role === 'MANAGER';

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

        {/* User Profile / Auth Button */}
        <div style={{ position: 'relative' }} ref={dropdownRef}>
          <button
            type="button"
            id="user-profile-menu-button"
            className="btn btn-secondary btn-sm"
            onClick={() => {
              if (!currentUser) {
                onOpenAuth();
              } else {
                setDropdownOpen(!dropdownOpen);
              }
            }}
            style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
            title={currentUser ? 'Click to view profile menu' : 'Sign in'}
          >
            {currentUser ? (
              <>
                <div
                  style={{
                    width: '22px',
                    height: '22px',
                    borderRadius: '50%',
                    background: isManager ? '#2563eb' : '#059669',
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
                    background: isManager ? 'rgba(59, 130, 246, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                    color: isManager ? '#60a5fa' : '#34d399',
                    border: `1px solid ${isManager ? 'rgba(59, 130, 246, 0.3)' : 'rgba(16, 185, 129, 0.3)'}`,
                    letterSpacing: '0.04em'
                  }}
                >
                  {currentUser.role}
                </span>
                <ChevronDown size={14} style={{ color: 'var(--text-muted)', marginLeft: '-2px' }} />
              </>
            ) : (
              <>
                <LogIn size={14} />
                <span>Sign In</span>
              </>
            )}
          </button>

          {/* Profile Dropdown Menu for Authenticated Users */}
          {currentUser && dropdownOpen && (
            <div
              id="user-profile-dropdown"
              style={{
                position: 'absolute',
                top: 'calc(100% + 8px)',
                right: 0,
                width: '240px',
                background: '#131b2e',
                borderRadius: '10px',
                border: '1px solid rgba(255, 255, 255, 0.12)',
                boxShadow: '0 12px 28px rgba(0, 0, 0, 0.5), 0 0 1px rgba(255, 255, 255, 0.1)',
                padding: '8px 0',
                zIndex: 1000,
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              {/* User Identity Info Header */}
              <div style={{
                padding: '10px 14px 12px 14px',
                borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
                marginBottom: '4px'
              }}>
                <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#f8fafc', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {currentUser.fullName}
                </div>
                <div style={{ fontSize: '0.78rem', color: '#94a3b8', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', marginTop: '2px' }}>
                  {currentUser.email}
                </div>
                <div style={{ marginTop: '6px' }}>
                  <span style={{
                    fontSize: '0.68rem',
                    fontWeight: 700,
                    padding: '2px 6px',
                    borderRadius: '8px',
                    background: isManager ? 'rgba(59, 130, 246, 0.2)' : 'rgba(16, 185, 129, 0.2)',
                    color: isManager ? '#93c5fd' : '#86efac',
                    border: `1px solid ${isManager ? 'rgba(59, 130, 246, 0.35)' : 'rgba(16, 185, 129, 0.35)'}`,
                  }}>
                    Assigned Role: {currentUser.role}
                  </span>
                </div>
              </div>

              {/* Menu Actions */}
              <button
                type="button"
                id="profile-dropdown-my-profile"
                onClick={() => {
                  setDropdownOpen(false);
                  onOpenProfile?.();
                }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  width: '100%',
                  padding: '9px 14px',
                  background: 'none',
                  border: 'none',
                  color: '#e2e8f0',
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'background 0.15s ease',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255, 255, 255, 0.06)')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
              >
                <UserIcon size={15} color="#60a5fa" />
                <span>My Profile</span>
              </button>

              {isManager && (
                <button
                  type="button"
                  id="profile-dropdown-manager-approvals"
                  onClick={() => {
                    setDropdownOpen(false);
                    onOpenManagerApprovals?.();
                  }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    width: '100%',
                    padding: '9px 14px',
                    background: 'none',
                    border: 'none',
                    color: '#e2e8f0',
                    fontSize: '0.85rem',
                    cursor: 'pointer',
                    textAlign: 'left',
                    transition: 'background 0.15s ease',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(255, 255, 255, 0.06)')}
                  onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
                >
                  <ShieldCheck size={15} color="#c084fc" />
                  <span>Manager Approvals</span>
                </button>
              )}

              <div style={{ height: '1px', background: 'rgba(255, 255, 255, 0.08)', margin: '4px 0' }} />

              <button
                type="button"
                id="profile-dropdown-logout"
                onClick={() => {
                  setDropdownOpen(false);
                  onLogout?.();
                }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  width: '100%',
                  padding: '9px 14px',
                  background: 'none',
                  border: 'none',
                  color: '#f87171',
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'background 0.15s ease',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(239, 68, 68, 0.1)')}
                onMouseLeave={(e) => (e.currentTarget.style.background = 'none')}
              >
                <LogOut size={15} color="#f87171" />
                <span>Logout</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;
