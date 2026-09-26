import React from 'react';
import OperationsPage from './OperationsPage';

interface DeliveriesPageProps {
  onSuccessToast?: (msg: string) => void;
  onErrorToast?: (msg: string) => void;
}

export const DeliveriesPage: React.FC<DeliveriesPageProps> = ({
  onSuccessToast = () => {},
  onErrorToast = () => {},
}) => {
  return (
    <OperationsPage
      initialType="DELIVERY"
      onSuccessToast={onSuccessToast}
      onErrorToast={onErrorToast}
    />
  );
};

export default DeliveriesPage;
