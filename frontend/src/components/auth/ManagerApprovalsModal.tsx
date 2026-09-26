import React, { useState, useEffect } from 'react';
import { ShieldCheck, UserCheck, UserX, Clock } from 'lucide-react';
import Modal from '../ui/Modal';
import { User } from '../../types/auth';
import { authApi } from '../../api/auth';

interface ManagerApprovalsModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: User | null;
  onSuccessToast: (msg: string) => void;
  onErrorToast: (msg: string) => void;
}

export const ManagerApprovalsModal: React.FC<ManagerApprovalsModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  onSuccessToast,
  onErrorToast,
}) => {
  const [requests, setRequests] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionId, setActionId] = useState<number | null>(null);

  const fetchRequests = async () => {
    setLoading(true);
    try {
      const data = await authApi.getManagerRequests();
      setRequests(data);
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to load manager approval requests.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen && currentUser?.role === 'MANAGER') {
      fetchRequests();
    }
  }, [isOpen, currentUser]);

  if (!isOpen || currentUser?.role !== 'MANAGER') return null;

  const handleApprove = async (user: User) => {
    if (currentUser.id === user.id) {
      onErrorToast('You cannot approve your own Manager request.');
      return;
    }
    setActionId(user.id);
    try {
      await authApi.approveManagerRequest(user.id);
      onSuccessToast(`Manager access approved for ${user.fullName} (${user.email}).`);
      setRequests((prev) => prev.filter((r) => r.id !== user.id));
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to approve manager request.');
    } finally {
      setActionId(null);
    }
  };

  const handleReject = async (user: User) => {
    if (currentUser.id === user.id) {
      onErrorToast('You cannot reject your own Manager request.');
      return;
    }
    setActionId(user.id);
    try {
      await authApi.rejectManagerRequest(user.id);
      onSuccessToast(`Manager request rejected for ${user.fullName}. Retained as Worker.`);
      setRequests((prev) => prev.filter((r) => r.id !== user.id));
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to reject manager request.');
    } finally {
      setActionId(null);
    }
  };

  return (
    <Modal isOpen={isOpen} title="Manager Access Requests" onClose={onClose}>
      <div style={{ padding: '4px 0' }}>
        <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px', lineHeight: 1.4 }}>
          Users requesting privileged <strong>MANAGER</strong> role access must be authorized by an existing Manager before activation.
        </p>

        {loading ? (
          <div style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
            Loading pending requests...
          </div>
        ) : requests.length === 0 ? (
          <div style={{
            padding: '32px 20px',
            textAlign: 'center',
            background: 'var(--surface-sunken)',
            borderRadius: '8px',
            border: '1px solid var(--border-subtle)',
            color: 'var(--text-muted)'
          }}>
            <ShieldCheck size={32} color="#3b82f6" style={{ margin: '0 auto 8px auto' }} />
            <div style={{ fontWeight: 600, color: 'var(--text-main)', marginBottom: '4px' }}>No Pending Requests</div>
            <div style={{ fontSize: '0.8rem' }}>All manager access requests have been reviewed and approved.</div>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {requests.map((req) => (
              <div
                key={req.id}
                style={{
                  padding: '14px',
                  borderRadius: '8px',
                  background: 'var(--surface-sunken)',
                  border: '1px solid var(--border-subtle)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: '12px'
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-main)' }}>
                    {req.fullName}
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {req.email}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: '#f59e0b', display: 'flex', alignItems: 'center', gap: '4px', marginTop: '3px' }}>
                    <Clock size={12} />
                    <span>Requested: {new Date(req.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    style={{ color: '#ef4444' }}
                    disabled={actionId === req.id}
                    onClick={() => handleReject(req)}
                  >
                    <UserX size={14} />
                    <span>Reject</span>
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    style={{ background: '#9333ea', borderColor: '#9333ea' }}
                    disabled={actionId === req.id}
                    onClick={() => handleApprove(req)}
                  >
                    <UserCheck size={14} />
                    <span>{actionId === req.id ? 'Processing...' : 'Approve'}</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Modal>
  );
};

export default ManagerApprovalsModal;
