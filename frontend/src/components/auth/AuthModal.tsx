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

type AuthTab = 'LOGIN' | 'REGISTER' | 'VERIFY_REGISTRATION' | 'FORGOT' | 'RESET' | 'PROFILE';

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
  const [confirmPassword, setConfirmPassword] = useState('');
  const [fullName, setFullName] = useState(currentUser?.fullName || '');
  const [selectedRole, setSelectedRole] = useState<'WORKER' | 'MANAGER'>('WORKER');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await authApi.login({ email: email.trim().toLowerCase(), password });
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
    if (password !== confirmPassword) {
      onErrorToast('Passwords do not match.');
      return;
    }
    if (password.length < 6) {
      onErrorToast('Password must be at least 6 characters.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await authApi.register({
        email: email.trim().toLowerCase(),
        password,
        fullName: fullName.trim(),
        requestedRole: selectedRole,
      });
      onSuccessToast(res.message);
      setOtp('');
      setTab('VERIFY_REGISTRATION');
    } catch (err: any) {
      onErrorToast(err.message || 'Registration failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyRegistrationOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await authApi.verifyEmailOtp(email.trim().toLowerCase(), otp.trim());
      if (res?.user && res?.token) {
        onUserChange(res.user);
        onSuccessToast(`Account activated! Welcome, ${res.user.fullName}!`);
        onClose();
      } else {
        onSuccessToast('Manager access request submitted for administrator approval.');
        setTab('LOGIN');
      }
    } catch (err: any) {
      const msg = err.message || 'Verification failed.';
      if (msg.includes('pending administrator approval')) {
        onSuccessToast('Manager access request submitted for administrator approval.');
        setTab('LOGIN');
      } else {
        onErrorToast(msg);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await authApi.forgotPassword(email.trim().toLowerCase());
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
    if (newPassword !== confirmNewPassword) {
      onErrorToast('New passwords do not match.');
      return;
    }
    if (newPassword.length < 6) {
      onErrorToast('New password must be at least 6 characters.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await authApi.resetPassword({
        email: email.trim().toLowerCase(),
        otp: otp.trim(),
        newPassword
      });
      onSuccessToast(res.message);
      setTab('LOGIN');
      setPassword('');
      setConfirmPassword('');
      setOtp('');
      setNewPassword('');
      setConfirmNewPassword('');
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
      const updated = await authApi.updateProfile(fullName.trim());
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
    } finally {
      onUserChange(null);
      onSuccessToast('Logged out successfully.');
      onClose();
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      title={
        currentUser
          ? 'User Profile & Account'
          : tab === 'REGISTER'
          ? 'Create StockSense Account'
          : tab === 'VERIFY_REGISTRATION'
          ? 'Verify Registration OTP'
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
              className={`btn btn-sm ${tab === 'REGISTER' || tab === 'VERIFY_REGISTRATION' ? 'btn-primary' : 'btn-secondary'}`}
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
              <span>Reset</span>
            </button>
          </div>
        )}

        {/* PROFILE TAB */}
        {currentUser && (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '18px', padding: '14px', background: 'var(--surface-sunken)', borderRadius: '6px' }}>
              <div style={{ width: '42px', height: '42px', borderRadius: '50%', background: currentUser.role === 'MANAGER' ? '#2563eb' : '#059669', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '1.1rem' }}>
                {currentUser.fullName.charAt(0).toUpperCase()}
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '1rem' }}>{currentUser.fullName}</div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{currentUser.email} • Role: <strong>{currentUser.role}</strong></div>
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
                placeholder="name@company.com"
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
            <div className="form-group" style={{ marginBottom: '12px' }}>
              <label className="form-label">Full Name *</label>
              <input
                type="text"
                className="form-control"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Jane Smith"
              />
            </div>

            <div className="form-group" style={{ marginBottom: '12px' }}>
              <label className="form-label">Work Email *</label>
              <input
                type="email"
                className="form-control"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
              />
            </div>

            {/* Role Selection */}
            <div className="form-group" style={{ marginBottom: '12px' }}>
              <label className="form-label" style={{ marginBottom: '6px' }}>Select Role *</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
                <div
                  onClick={() => setSelectedRole('WORKER')}
                  style={{
                    padding: '10px',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    background: selectedRole === 'WORKER' ? 'rgba(37, 99, 235, 0.15)' : 'var(--surface-sunken)',
                    border: `1.5px solid ${selectedRole === 'WORKER' ? '#3b82f6' : 'var(--border-subtle)'}`,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '3px' }}>
                    <strong style={{ fontSize: '0.85rem' }}>Worker</strong>
                    <input type="radio" checked={selectedRole === 'WORKER'} onChange={() => setSelectedRole('WORKER')} />
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Warehouse & Inventory ops</div>
                </div>

                <div
                  onClick={() => setSelectedRole('MANAGER')}
                  style={{
                    padding: '10px',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    background: selectedRole === 'MANAGER' ? 'rgba(147, 51, 234, 0.15)' : 'var(--surface-sunken)',
                    border: `1.5px solid ${selectedRole === 'MANAGER' ? '#a855f7' : 'var(--border-subtle)'}`,
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '3px' }}>
                    <strong style={{ fontSize: '0.85rem' }}>Manager</strong>
                    <input type="radio" checked={selectedRole === 'MANAGER'} onChange={() => setSelectedRole('MANAGER')} />
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>Requires admin approval</div>
                </div>
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '12px' }}>
              <label className="form-label">Password (min 6 chars) *</label>
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

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Confirm Password *</label>
              <input
                type="password"
                className="form-control"
                required
                minLength={6}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={submitting}>
              {submitting ? 'Creating Account...' : selectedRole === 'MANAGER' ? 'Request Manager Account' : 'Register Worker Account'}
            </button>
          </form>
        )}

        {/* VERIFY REGISTRATION OTP TAB */}
        {!currentUser && tab === 'VERIFY_REGISTRATION' && (
          <form onSubmit={handleVerifyRegistrationOtp}>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '14px' }}>
              Enter the 6-digit verification code sent to <strong>{email}</strong>
            </p>

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">6-Digit Verification Code *</label>
              <input
                type="text"
                className="form-control"
                required
                maxLength={6}
                value={otp}
                onChange={(e) => setOtp(e.target.value.trim())}
                placeholder="123456"
                style={{ fontSize: '1.2rem', letterSpacing: '6px', textAlign: 'center' }}
              />
            </div>

            <div style={{ display: 'flex', gap: '8px' }}>
              <button type="button" className="btn btn-secondary" onClick={() => setTab('REGISTER')}>
                Back
              </button>
              <button type="submit" className="btn btn-primary" style={{ flex: 1, justifyContent: 'center' }} disabled={submitting || otp.length !== 6}>
                {submitting ? 'Verifying...' : 'Verify Code & Activate'}
              </button>
            </div>
          </form>
        )}

        {/* FORGOT PASSWORD TAB */}
        {!currentUser && tab === 'FORGOT' && (
          <form onSubmit={handleForgotPassword}>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Enter your registered work email to receive a 6-digit verification code.
            </p>
            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Registered Work Email *</label>
              <input
                type="email"
                className="form-control"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={submitting}>
              {submitting ? 'Sending OTP...' : 'Send Verification Code'}
            </button>
          </form>
        )}

        {/* RESET PASSWORD TAB */}
        {!currentUser && tab === 'RESET' && (
          <form onSubmit={handleResetPassword}>
            <div className="form-group" style={{ marginBottom: '14px' }}>
              <label className="form-label">6-Digit Code sent to {email} *</label>
              <input
                type="text"
                className="form-control"
                required
                maxLength={6}
                value={otp}
                onChange={(e) => setOtp(e.target.value.trim())}
                placeholder="123456"
                style={{ textAlign: 'center', letterSpacing: '4px', fontWeight: 700 }}
              />
            </div>

            <div className="form-group" style={{ marginBottom: '14px' }}>
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

            <div className="form-group" style={{ marginBottom: '20px' }}>
              <label className="form-label">Confirm New Password *</label>
              <input
                type="password"
                className="form-control"
                required
                minLength={6}
                value={confirmNewPassword}
                onChange={(e) => setConfirmNewPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={submitting}>
              {submitting ? 'Updating...' : 'Save New Password & Sign In'}
            </button>
          </form>
        )}
      </div>
    </Modal>
  );
};

export default AuthModal;
