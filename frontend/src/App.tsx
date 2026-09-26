import React, { useState } from 'react';
import Sidebar, { PageId } from './components/layout/Sidebar';
import Navbar from './components/layout/Navbar';
import Toast from './components/ui/Toast';
import { useToast } from './hooks/useToast';

import ProductsPage from './pages/ProductsPage';
import DashboardPage from './pages/DashboardPage';
import ReceiptsPage from './pages/ReceiptsPage';
import DeliveriesPage from './pages/DeliveriesPage';
import AdjustmentsPage from './pages/AdjustmentsPage';
import LedgerPage from './pages/LedgerPage';

export const App: React.FC = () => {
  const [currentPage, setCurrentPage] = useState<PageId>('products');
  const [totalProductsCount, setTotalProductsCount] = useState<number | undefined>(undefined);
  const { toasts, addToast, removeToast } = useToast();

  const renderCurrentPage = () => {
    switch (currentPage) {
      case 'products':
        return (
          <ProductsPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
            onUpdateTotalCount={setTotalProductsCount}
          />
        );
      case 'dashboard':
        return <DashboardPage />;
      case 'receipts':
        return <ReceiptsPage />;
      case 'deliveries':
        return <DeliveriesPage />;
      case 'adjustments':
        return <AdjustmentsPage />;
      case 'ledger':
        return <LedgerPage />;
      default:
        return (
          <ProductsPage
            onSuccessToast={(msg) => addToast(msg, 'success')}
            onErrorToast={(msg) => addToast(msg, 'error')}
            onUpdateTotalCount={setTotalProductsCount}
          />
        );
    }
  };

  return (
    <div className="app-container">
      <Sidebar currentPage={currentPage} onNavigate={setCurrentPage} />
      <div className="main-wrapper">
        <Navbar currentPage={currentPage} totalProductsCount={totalProductsCount} />
        <main className="content-body">
          {renderCurrentPage()}
        </main>
      </div>
      <Toast toasts={toasts} onDismiss={removeToast} />
    </div>
  );
};

export default App;
