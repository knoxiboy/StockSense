import React, { useState } from 'react';
import Sidebar, { PageId } from './components/layout/Sidebar';
import Header from './components/layout/Header';
import Toast from './components/ui/Toast';
import { useToast } from './hooks/useToast';

import ProductsPage from './pages/ProductsPage';
import DashboardPage from './pages/DashboardPage';
import OperationsPage from './pages/OperationsPage';
import ReceiptsPage from './pages/ReceiptsPage';
import DeliveriesPage from './pages/DeliveriesPage';
import AdjustmentsPage from './pages/AdjustmentsPage';
import LedgerPage from './pages/LedgerPage';

export const App: React.FC = () => {
  const [currentPage, setCurrentPage] = useState<PageId>('dashboard');
  const [totalProductsCount, setTotalProductsCount] = useState<number | undefined>(undefined);
  const [actionProductId, setActionProductId] = useState<number | undefined>(undefined);
  const { toasts, addToast, removeToast } = useToast();

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
      case 'operations':
        return (
          <OperationsPage
            initialType="ALL"
            initialProductId={actionProductId}
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'receipts':
        return (
          <ReceiptsPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
      case 'deliveries':
        return (
          <DeliveriesPage
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
      case 'ledger':
        return (
          <LedgerPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
          />
        );
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
      />
      <div className="main-wrapper">
        <Header currentPage={currentPage} totalProductsCount={totalProductsCount} />
        <main className="content-body">{renderCurrentPage()}</main>
      </div>
      <Toast toasts={toasts} onDismiss={removeToast} />
    </div>
  );
};

export default App;
