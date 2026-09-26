import React from 'react';
import { LayoutDashboard } from 'lucide-react';

export const DashboardPage: React.FC = () => {
  return (
    <div className="phase-placeholder">
      <div className="phase-placeholder-icon">
        <LayoutDashboard size={28} />
      </div>
      <h3>Inventory Dashboard</h3>
      <p>
        Real-time metrics, low-stock alerts, pending receipts, and pending deliveries will be enabled
        in the Dashboard Phase once inventory operations are active.
      </p>
      <span className="badge badge-primary">Scheduled for Phase 4</span>
    </div>
  );
};

export default DashboardPage;
