import React from 'react';
import Navbar from './Navbar';
import { PageId } from './Sidebar';

export interface HeaderProps {
  currentPage: PageId;
  totalProductsCount?: number;
}

export const Header: React.FC<HeaderProps> = (props) => {
  return <Navbar {...props} />;
};

export default Header;
