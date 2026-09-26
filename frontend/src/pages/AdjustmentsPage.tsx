import React from 'react';
import OperationsPage from './OperationsPage';

interface AdjustmentsPageProps {
  onSuccessToast?: (msg: string) => void;
  onErrorToast?: (msg: string) => void;
}

export const AdjustmentsPage: React.FC<AdjustmentsPageProps> = ({
  onSuccessToast = () => {},
  onErrorToast = () => {},
}) => {
  return (
    <OperationsPage
      initialType="ADJUSTMENT"
      onSuccessToast={onSuccessToast}
      onErrorToast={onErrorToast}
    />
  );
};

export default AdjustmentsPage;
