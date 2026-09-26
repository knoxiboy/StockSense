import React from 'react';
import { ArrowDownLeft } from 'lucide-react';

export const ReceiptsPage: React.FC = () => {
  return (
    <div className="phase-placeholder">
      <div className="phase-placeholder-icon">
        <ArrowDownLeft size={28} />
      </div>
      <h3>Incoming Receipts (Inbound Stock)</h3>
      <p>
        Register incoming shipments from suppliers, record received quantities, and atomically increase stock levels.
      </p>
      <span className="badge badge-primary">Scheduled for Phase 2: Operations</span>
    </div>
  );
};

export default ReceiptsPage;
