import React from 'react';
import { ArrowUpRight } from 'lucide-react';

export const DeliveriesPage: React.FC = () => {
  return (
    <div className="phase-placeholder">
      <div className="phase-placeholder-icon">
        <ArrowUpRight size={28} />
      </div>
      <h3>Delivery Orders (Outbound Stock)</h3>
      <p>
        Manage customer sales orders, pick & pack validation, and atomic stock decrements with negative stock prevention.
      </p>
      <span className="badge badge-primary">Scheduled for Phase 2: Operations</span>
    </div>
  );
};

export default DeliveriesPage;
