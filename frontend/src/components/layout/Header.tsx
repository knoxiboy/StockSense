import React from 'react';
import Navbar from './Navbar';
import { PageId } from './Sidebar';
import { User } from '../../types/auth';

export interface HeaderProps {
  currentPage: PageId;
  totalProductsCount?: number;
  currentUser: User | null;
  onOpenAuth: () => void;
}

export const Header: React.FC<HeaderProps> = (props) => {
  return <Navbar {...props} />;
};

export default Header;
