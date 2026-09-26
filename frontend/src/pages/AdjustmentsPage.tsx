import React from 'react';
import { SlidersHorizontal } from 'lucide-react';

export const AdjustmentsPage: React.FC = () => {
  return (
    <div className="phase-placeholder">
      <div className="phase-placeholder-icon">
        <SlidersHorizontal size={28} />
      </div>
      <h3>Inventory Adjustments</h3>
      <p>
        Perform physical stock takes, audit discrepancies between physical counts and system balances, and log corrections.
      </p>
      <span className="badge badge-primary">Scheduled for Phase 2: Operations</span>
    </div>
  );
};

export default AdjustmentsPage;
