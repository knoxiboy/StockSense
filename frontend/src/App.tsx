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
import { authApi } from './api/auth';
import { User } from './types/auth';

export const App: React.FC = () => {
  const [currentPage, setCurrentPage] = useState<PageId>('dashboard');
  const [totalProductsCount, setTotalProductsCount] = useState<number | undefined>(undefined);
  const [actionProductId, setActionProductId] = useState<number | undefined>(undefined);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const { toasts, addToast, removeToast } = useToast();

  useEffect(() => {
    const user = authApi.getCurrentUser();
    if (user) {
      setCurrentUser(user);
    }
  }, []);

  const handleOpenReceiptForProduct = (productId: number) => {
    setActionProductId(productId);
    setCurrentPage('receipts');
  };

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
        />
        <main className="content-body">{renderCurrentPage()}</main>
      </div>

      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        currentUser={currentUser}
        onUserChange={setCurrentUser}
        onSuccessToast={(msg) => addToast(msg, 'success')}
        onErrorToast={(msg) => addToast(msg, 'error')}
      />

      <Toast toasts={toasts} onDismiss={removeToast} />
    </div>
  );
};

export default App;
