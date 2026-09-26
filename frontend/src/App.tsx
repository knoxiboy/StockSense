import React, { useState, useEffect } from 'react';
import Sidebar, { PageId } from './components/layout/Sidebar';
import Header from './components/layout/Header';
import Toast from './components/ui/Toast';
import { useToast } from './hooks/useToast';

import ProductsPage from './pages/ProductsPage';
import DashboardPage from './pages/DashboardPage';
import OperationsPage from './pages/OperationsPage';
import ReceiptsWorkflowPage from './pages/ReceiptsWorkflowPage';
import DeliveriesWorkflowPage from './pages/DeliveriesWorkflowPage';
import TransfersPage from './pages/TransfersPage';
import AdjustmentsPage from './pages/AdjustmentsPage';
import WarehousesPage from './pages/WarehousesPage';
import LedgerPage from './pages/LedgerPage';
import SettingsPage from './pages/SettingsPage';
import AuthModal from './components/auth/AuthModal';
import MyProfileModal from './components/auth/MyProfileModal';
import ManagerApprovalsModal from './components/auth/ManagerApprovalsModal';
import AuthScreen from './components/auth/AuthScreen';
import { authApi } from './api/auth';
import { User } from './types/auth';
import { Loader2 } from 'lucide-react';

export const App: React.FC = () => {
  const [currentPage, setCurrentPage] = useState<PageId>('dashboard');
  const [totalProductsCount, setTotalProductsCount] = useState<number | undefined>(undefined);
  const [actionProductId, setActionProductId] = useState<number | undefined>(undefined);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  const [isManagerApprovalsModalOpen, setIsManagerApprovalsModalOpen] = useState(false);
  const [isLoadingSession, setIsLoadingSession] = useState(true);
  const { toasts, addToast, removeToast } = useToast();

  // Validate or restore authentication session on initial load and page refresh
  useEffect(() => {
    const token = authApi.getToken();
    if (!token) {
      setCurrentUser(null);
      setIsLoadingSession(false);
      return;
    }

    // Verify token with backend /api/auth/me
    authApi
      .getProfile()
      .then((user) => {
        setCurrentUser(user);
      })
      .catch(() => {
        // Token was invalid or expired
        authApi.clearSession();
        setCurrentUser(null);
      })
      .finally(() => {
        setIsLoadingSession(false);
      });

    // Handle 401 Unauthorized globally from apiClient interceptor
    const handleUnauthorized = () => {
      setCurrentUser(null);
      setCurrentPage('dashboard');
      setIsAuthModalOpen(false);
      setIsProfileModalOpen(false);
      setIsManagerApprovalsModalOpen(false);
      addToast('Session expired or token invalid. Please sign in again.', 'error');
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, []);

  const handleLoginSuccess = (user: User) => {
    setCurrentUser(user);
    setCurrentPage('dashboard'); // Redirect to dashboard after successful login
  };

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } finally {
      authApi.clearSession();
      setCurrentUser(null);
      setCurrentPage('dashboard');
      setIsAuthModalOpen(false);
      setIsProfileModalOpen(false);
      setIsManagerApprovalsModalOpen(false);
      addToast('You have been logged out successfully.', 'success');
    }
  };

  const handleOpenReceiptForProduct = (productId: number) => {
    setActionProductId(productId);
    setCurrentPage('receipts');
  };

  // 1. Loading State on Initial Session Validation
  if (isLoadingSession) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#0f172a',
        color: '#94a3b8',
        gap: '16px',
        fontFamily: 'Inter, system-ui, sans-serif'
      }}>
        <Loader2 size={36} className="spin" color="#3b82f6" />
        <span style={{ fontSize: '0.9rem', letterSpacing: '0.05em' }}>Verifying StockSense session...</span>
      </div>
    );
  }

  // 2. Unauthenticated Gate: Show Login / Registration Screen
  if (!currentUser) {
    return (
      <>
        <AuthScreen
          onLoginSuccess={handleLoginSuccess}
          onSuccessToast={(msg) => addToast(msg, 'success')}
          onErrorToast={(msg) => addToast(msg, 'error')}
        />
        <Toast toasts={toasts} onDismiss={removeToast} />
      </>
    );
  }

  // 3. Authenticated App Flow: Render protected routes
  const renderCurrentPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return (
          <DashboardPage
            onNavigate={setCurrentPage}
            onOpenReceiptForProduct={handleOpenReceiptForProduct}
          />
        );
      case 'products':
        return (
          <ProductsPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
            onUpdateTotalCount={setTotalProductsCount}
          />
        );
      case 'receipts':
        return (
          <ReceiptsWorkflowPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'deliveries':
        return (
          <DeliveriesWorkflowPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'transfers':
        return (
          <TransfersPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'adjustments':
        return (
          <AdjustmentsPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'operations':
        return (
          <OperationsPage
            initialType="ALL"
            initialProductId={actionProductId}
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'warehouses':
        return (
          <WarehousesPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'ledger':
        return (
          <LedgerPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'settings':
        return <SettingsPage />;
      default:
        return (
          <DashboardPage
            onNavigate={setCurrentPage}
            onOpenReceiptForProduct={handleOpenReceiptForProduct}
          />
        );
    }
  };

  return (
    <div className="app-container">
      <Sidebar
        currentPage={currentPage}
        onNavigate={(p) => {
          setActionProductId(undefined);
          setCurrentPage(p);
        }}
        currentUser={currentUser}
        onOpenAuth={() => setIsAuthModalOpen(true)}
      />
      <div className="main-wrapper">
        <Header
          currentPage={currentPage}
          totalProductsCount={totalProductsCount}
          currentUser={currentUser}
          onOpenAuth={() => setIsAuthModalOpen(true)}
          onOpenProfile={() => setIsProfileModalOpen(true)}
          onOpenManagerApprovals={() => setIsManagerApprovalsModalOpen(true)}
          onLogout={handleLogout}
        />
        <main className="content-body">{renderCurrentPage()}</main>
      </div>

      <MyProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
        currentUser={currentUser}
        onUserUpdate={(updated) => setCurrentUser(updated)}
        onLogout={handleLogout}
        onSuccessToast={(msg) => addToast(msg, 'success')}
        onErrorToast={(msg) => addToast(msg, 'error')}
      />

      <ManagerApprovalsModal
        isOpen={isManagerApprovalsModalOpen}
        onClose={() => setIsManagerApprovalsModalOpen(false)}
        currentUser={currentUser}
        onSuccessToast={(msg) => addToast(msg, 'success')}
        onErrorToast={(msg) => addToast(msg, 'error')}
      />

      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        currentUser={currentUser}
        onUserChange={(user) => {
          if (!user) {
            handleLogout();
          } else {
            setCurrentUser(user);
          }
        }}
        onSuccessToast={(msg) => addToast(msg, 'success')}
        onErrorToast={(msg) => addToast(msg, 'error')}
      />

      <Toast toasts={toasts} onDismiss={removeToast} />
    </div>
  );
};

export default App;
