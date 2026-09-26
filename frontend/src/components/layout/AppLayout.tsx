import React from 'react';
import Sidebar, { PageId } from './Sidebar';
import Header from './Header';

import { User } from '../../types/auth';

interface AppLayoutProps {
  currentPage: PageId;
  onNavigate: (page: PageId) => void;
  totalProductsCount?: number;
  currentUser?: User | null;
  onOpenAuth?: () => void;
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({
  currentPage,
  onNavigate,
  totalProductsCount,
  currentUser = null,
  onOpenAuth = () => {},
  children,
}) => {
  return (
    <div className="app-container">
      <Sidebar
        currentPage={currentPage}
        onNavigate={onNavigate}
        currentUser={currentUser}
        onOpenAuth={onOpenAuth}
      />
      <div className="main-wrapper">
        <Header
          currentPage={currentPage}
          totalProductsCount={totalProductsCount}
          currentUser={currentUser}
          onOpenAuth={onOpenAuth}
        />
        <main className="content-body">{children}</main>
      </div>
    </div>
  );
};

export default AppLayout;
