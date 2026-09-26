import React, { useState } from 'react';
import { CheckCircle2, Calendar, LogOut } from 'lucide-react';
import Modal from '../ui/Modal';
import { User } from '../../types/auth';
import { authApi } from '../../api/auth';

interface MyProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: User | null;
  onUserUpdate: (updatedUser: User) => void;
  onLogout: () => void;
  onSuccessToast: (msg: string) => void;
  onErrorToast: (msg: string) => void;
}

export const MyProfileModal: React.FC<MyProfileModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  onUserUpdate,
  onLogout,
  onSuccessToast,
  onErrorToast,
}) => {
  const [fullName, setFullName] = useState(currentUser?.fullName || '');
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen || !currentUser) return null;

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName.trim()) {
      onErrorToast('Full name cannot be empty.');
      return;
    }

    setSubmitting(true);
    try {
      const updated = await authApi.updateProfile(fullName.trim());
      onUserUpdate(updated);
      onSuccessToast('Profile updated successfully!');
      onClose();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to update profile.');
    } finally {
      setSubmitting(false);
    }
  };

  const isManager = currentUser.role === 'MANAGER';
  const memberSince = currentUser.createdAt
    ? new Date(currentUser.createdAt).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    : 'Active';

  return (
    <Modal isOpen={isOpen} title="My Profile" onClose={onClose}>
      <div style={{ padding: '4px 0' }}>
        {/* User Identity Card */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
          padding: '16px',
          background: 'var(--surface-sunken)',
          borderRadius: '10px',
          marginBottom: '20px',
          border: '1px solid var(--border-subtle)'
        }}>
          <div style={{
            width: '52px',
            height: '52px',
            borderRadius: '50%',
            background: isManager ? 'linear-gradient(135deg, #2563eb, #1d4ed8)' : 'linear-gradient(135deg, #059669, #047857)',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 800,
            fontSize: '1.3rem',
            boxShadow: isManager ? '0 4px 12px rgba(37, 99, 235, 0.3)' : '0 4px 12px rgba(5, 150, 105, 0.3)',
            flexShrink: 0
          }}>
            {currentUser.fullName.charAt(0).toUpperCase()}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
              <h3 style={{ margin: 0, fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-main)' }}>
                {currentUser.fullName}
              </h3>
              <span style={{
                fontSize: '0.72rem',
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '12px',
                background: isManager ? 'rgba(59, 130, 246, 0.15)' : 'rgba(16, 185, 129, 0.15)',
                color: isManager ? '#60a5fa' : '#34d399',
                border: `1px solid ${isManager ? 'rgba(59, 130, 246, 0.3)' : 'rgba(16, 185, 129, 0.3)'}`,
                letterSpacing: '0.04em'
              }}>
                {currentUser.role}
              </span>
            </div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginTop: '3px' }}>
              {currentUser.email}
            </div>
          </div>
        </div>

        {/* Account Details Metadata */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '10px',
          marginBottom: '20px'
        }}>
          <div style={{
            padding: '10px 12px',
            background: 'rgba(30, 41, 59, 0.4)',
            borderRadius: '8px',
            border: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            <CheckCircle2 size={16} color="#34d399" />
            <div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Email Status</div>
              <div style={{ fontSize: '0.82rem', fontWeight: 600, color: '#86efac' }}>Verified</div>
            </div>
          </div>

          <div style={{
            padding: '10px 12px',
            background: 'rgba(30, 41, 59, 0.4)',
            borderRadius: '8px',
            border: '1px solid var(--border-subtle)',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
          }}>
            <Calendar size={16} color="#60a5fa" />
            <div>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Member Since</div>
              <div style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-main)' }}>{memberSince}</div>
            </div>
          </div>
        </div>

        {/* Edit Profile Form */}
        <form onSubmit={handleUpdate}>
          <div className="form-group" style={{ marginBottom: '16px' }}>
            <label className="form-label" style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, marginBottom: '6px' }}>
              Full Name *
            </label>
            <input
              type="text"
              className="form-control"
              required
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Enter your name"
              style={{
                width: '100%',
                padding: '9px 12px',
                borderRadius: '6px',
                border: '1px solid var(--border-subtle)',
                background: 'var(--surface-sunken)',
                color: 'var(--text-main)',
                fontSize: '0.9rem',
                outline: 'none',
                boxSizing: 'border-box'
              }}
            />
          </div>

          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label className="form-label" style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, marginBottom: '6px' }}>
              Work Email Address
            </label>
            <input
              type="email"
              className="form-control"
              disabled
              value={currentUser.email}
              style={{
                width: '100%',
                padding: '9px 12px',
                borderRadius: '6px',
                border: '1px solid var(--border-subtle)',
                background: 'rgba(15, 23, 42, 0.6)',
                color: 'var(--text-muted)',
                fontSize: '0.9rem',
                boxSizing: 'border-box',
                cursor: 'not-allowed'
              }}
            />
            <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '4px', display: 'block' }}>
              Email address is linked to your account identity and verified via OTP.
            </span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '24px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              style={{ color: '#ef4444', display: 'flex', alignItems: 'center', gap: '6px' }}
              onClick={() => {
                onClose();
                onLogout();
              }}
            >
              <LogOut size={15} />
              <span>Sign Out</span>
            </button>

            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={onClose}
              >
                Close
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={submitting}
              >
                {submitting ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        </form>
      </div>
    </Modal>
  );
};

export default MyProfileModal;
