import React from 'react';
import Sidebar, { PageId } from './Sidebar';
import Header from './Header';

interface AppLayoutProps {
  currentPage: PageId;
  onNavigate: (page: PageId) => void;
  totalProductsCount?: number;
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({
  currentPage,
  onNavigate,
  totalProductsCount,
  children,
}) => {
  return (
    <div className="app-container">
      <Sidebar currentPage={currentPage} onNavigate={onNavigate} />
      <div className="main-wrapper">
        <Header currentPage={currentPage} totalProductsCount={totalProductsCount} />
        <main className="content-body">{children}</main>
      </div>
    </div>
  );
};

export default AppLayout;
