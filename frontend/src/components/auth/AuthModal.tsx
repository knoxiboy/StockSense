import React, { useState } from 'react';
import { LogIn, UserPlus, KeyRound, LogOut } from 'lucide-react';
import { authApi } from '../../api/auth';
import { User as UserType } from '../../types/auth';
import Modal from '../ui/Modal';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentUser: UserType | null;
  onUserChange: (user: UserType | null) => void;
  onSuccessToast: (msg: string) => void;
  onErrorToast: (msg: string) => void;
}

type AuthTab = 'LOGIN' | 'REGISTER' | 'FORGOT' | 'RESET' | 'PROFILE';

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  currentUser,
  onUserChange,
  onSuccessToast,
  onErrorToast,
}) => {
  const [tab, setTab] = useState<AuthTab>(currentUser ? 'PROFILE' : 'LOGIN');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState(currentUser?.fullName || '');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await authApi.login({ email, password });
      onUserChange(res.user);
      onSuccessToast(`Welcome back, ${res.user.fullName}!`);
      onClose();
    } catch (err: any) {
      onErrorToast(err.message || 'Login failed. Invalid credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await authApi.register({ email, password, fullName });
      onUserChange(res.user);
      onSuccessToast(`Account created successfully! Welcome, ${res.user.fullName}!`);
      onClose();
    } catch (err: any) {
      onErrorToast(err.message || 'Registration failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await authApi.forgotPassword(email);
      onSuccessToast(res.message);
      setTab('RESET');
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to send OTP verification code.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await authApi.resetPassword({ email, otp, newPassword });
      onSuccessToast(res.message);
      setTab('LOGIN');
      setPassword('');
      setOtp('');
      setNewPassword('');
    } catch (err: any) {
      onErrorToast(err.message || 'Password reset failed. Invalid or expired OTP.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const updated = await authApi.updateProfile(fullName);
      onUserChange(updated);
      onSuccessToast('Profile updated successfully!');
      onClose();
    } catch (err: any) {
      onErrorToast(err.message || 'Failed to update profile.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = async () => {
    try {
      await authApi.logout();
      onUserChange(null);
      onSuccessToast('Logged out successfully.');
      onClose();
    } catch (err: any) {
      onErrorToast(err.message || 'Error during logout.');
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      title={
        tab === 'PROFILE'
          ? 'User Profile & Account'
          : tab === 'REGISTER'
          ? 'Create StockSense Account'
          : tab === 'FORGOT'
          ? 'Reset Password (OTP Verification)'
          : tab === 'RESET'
          ? 'Enter OTP & New Password'
          : 'StockSense Authentication'
      }
      onClose={onClose}
    >
      <div style={{ padding: '4px 0' }}>
        {/* Navigation tabs if not logged in */}
        {!currentUser && (
          <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '10px' }}>
            <button
              type="button"
              className={`btn btn-sm ${tab === 'LOGIN' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setTab('LOGIN')}
            >
              <LogIn size={14} />
              <span>Sign In</span>
            </button>
            <button
              type="button"
              className={`btn btn-sm ${tab === 'REGISTER' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setTab('REGISTER')}
            >
              <UserPlus size={14} />
              <span>Register</span>
            </button>
            <button
              type="button"
              className={`btn btn-sm ${tab === 'FORGOT' || tab === 'RESET' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setTab('FORGOT')}
            >
              <KeyRound size={14} />
              <span>Forgot Password</span>
            </button>
          </div>
        )}

        {/* PROFILE TAB */}
        {currentUser && tab === 'PROFILE' && (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '18px', padding: '14px', background: 'var(--surface-sunken)', borderRadius: '6px' }}>
              <div style={{ width: '42px', height: '42px', borderRadius: '50%', background: 'var(--primary)', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '1.1rem' }}>
                {currentUser.fullName.charAt(0).toUpperCase()}
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '1rem' }}>{currentUser.fullName}</div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{currentUser.email} • Role: {currentUser.role}</div>
              </div>
            </div>

            <form onSubmit={handleUpdateProfile}>
              <div className="form-group" style={{ marginBottom: '16px' }}>
                <label className="form-label">Full Name</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '24px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  style={{ color: 'var(--danger)' }}
                  onClick={handleLogout}
                >
                  <LogOut size={16} />
                  <span>Log Out</span>
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Updating...' : 'Save Profile'}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* LOGIN TAB */}
        {!currentUser && tab === 'LOGIN' && (
          <form onSubmit={handleLogin}>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Work Email *</label>
              <input
                type="email"
                className="form-control"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="manager@stocksense.io"
              />
            </div>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                <label className="form-label" style={{ margin: 0 }}>Password *</label>
                <button
                  type="button"
                  style={{ background: 'none', border: 'none', color: 'var(--primary)', fontSize: '0.75rem', cursor: 'pointer', padding: 0 }}
                  onClick={() => setTab('FORGOT')}
                >
                  Forgot password?
                </button>
              </div>
              <input
                type="password"
                className="form-control"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={submitting}>
              {submitting ? 'Authenticating...' : 'Sign In'}
            </button>
          </form>
        )}

        {/* REGISTER TAB */}
        {!currentUser && tab === 'REGISTER' && (
          <form onSubmit={handleRegister}>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Full Name *</label>
              <input
                type="text"
                className="form-control"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="John Doe"
              />
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Work Email *</label>
              <input
                type="email"
                className="form-control"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="operator@stocksense.io"
              />
            </div>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Secure Password (min 6 chars) *</label>
              <input
                type="password"
                className="form-control"
                required
                minLength={6}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={submitting}>
              {submitting ? 'Creating Account...' : 'Register New Account'}
            </button>
          </form>
        )}

        {/* FORGOT PASSWORD TAB */}
        {!currentUser && tab === 'FORGOT' && (
          <form onSubmit={handleForgotPassword}>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Enter your email address to receive a 6-digit one-time password (OTP). The code expires in 10 minutes.
            </p>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Registered Email *</label>
              <input
                type="email"
                className="form-control"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="user@stocksense.io"
              />
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              <button type="button" className="btn btn-secondary" onClick={() => setTab('LOGIN')}>
                Back to Sign In
              </button>
              <button type="submit" className="btn btn-primary" style={{ flex: 1, justifyContent: 'center' }} disabled={submitting}>
                {submitting ? 'Sending OTP...' : 'Send OTP Verification Code'}
              </button>
            </div>
          </form>
        )}

        {/* RESET PASSWORD TAB */}
        {!currentUser && tab === 'RESET' && (
          <form onSubmit={handleResetPassword}>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">Registered Email *</label>
              <input
                type="email"
                className="form-control"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">6-Digit OTP Verification Code *</label>
              <input
                type="text"
                className="form-control"
                required
                maxLength={6}
                value={otp}
                onChange={(e) => setOtp(e.target.value.trim())}
                placeholder="e.g. 123456"
                style={{ letterSpacing: '4px', fontSize: '1.1rem', fontWeight: 700 }}
              />
            </div>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">New Password (min 6 chars) *</label>
              <input
                type="password"
                className="form-control"
                required
                minLength={6}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              <button type="button" className="btn btn-secondary" onClick={() => setTab('LOGIN')}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" style={{ flex: 1, justifyContent: 'center' }} disabled={submitting}>
                {submitting ? 'Resetting Password...' : 'Verify OTP & Reset Password'}
              </button>
            </div>
          </form>
        )}
      </div>
    </Modal>
  );
};

export default AuthModal;
