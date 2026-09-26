import React from 'react';
import { History } from 'lucide-react';

export const LedgerPage: React.FC = () => {
  return (
    <div className="phase-placeholder">
      <div className="phase-placeholder-icon">
        <History size={28} />
      </div>
      <h3>Stock Movement Ledger</h3>
      <p>
        Double-entry audit log tracking every inventory movement, reference document, timestamp, quantity, and running balance.
      </p>
      <span className="badge badge-primary">Scheduled for Phase 3: Stock Ledger</span>
    </div>
  );
};

export default LedgerPage;
