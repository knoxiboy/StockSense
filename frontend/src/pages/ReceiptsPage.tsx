import React from 'react';
import OperationsPage from './OperationsPage';

interface ReceiptsPageProps {
  onSuccessToast?: (msg: string) => void;
  onErrorToast?: (msg: string) => void;
}

export const ReceiptsPage: React.FC<ReceiptsPageProps> = ({
  onSuccessToast = () => {},
  onErrorToast = () => {},
}) => {
  return (
    <OperationsPage
      initialType="RECEIPT"
      onSuccessToast={onSuccessToast}
      onErrorToast={onErrorToast}
    />
  );
};

export default ReceiptsPage;
