import React, { useState, useEffect } from 'react';
import {
  Boxes,
  LogIn,
  UserPlus,
  KeyRound,
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
  Eye,
  EyeOff,
  UserCheck,
  Server,
  Users,
  Info
} from 'lucide-react';
import { authApi } from '../../api/auth';
import { User } from '../../types/auth';

interface AuthScreenProps {
  onLoginSuccess: (user: User) => void;
  onSuccessToast: (msg: string) => void;
  onErrorToast: (msg: string) => void;
}

type AuthMode = 'LOGIN' | 'REGISTER' | 'VERIFY_REGISTRATION' | 'FORGOT' | 'VERIFY_RESET' | 'RESET';

export const AuthScreen: React.FC<AuthScreenProps> = ({
  onLoginSuccess,
  onSuccessToast,
  onErrorToast,
}) => {
  const [mode, setMode] = useState<AuthMode>('LOGIN');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [fullName, setFullName] = useState('');
  const [selectedRole, setSelectedRole] = useState<'WORKER' | 'MANAGER'>('WORKER');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  // Resend cooldown timer for OTP
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    let timer: any;
    if (cooldown > 0) {
      timer = setTimeout(() => setCooldown(cooldown - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [cooldown]);

  const switchMode = (newMode: AuthMode) => {
    setMode(newMode);
    setFormError(null);
    setFormSuccess(null);
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);
    setSubmitting(true);
    try {
      const res = await authApi.login({ email: email.trim().toLowerCase(), password });
      onSuccessToast(`Welcome back, ${res.user.fullName}!`);
      onLoginSuccess(res.user);
    } catch (err: any) {
      const msg = err.message || 'Invalid email or password. Please verify your credentials.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);

    if (password !== confirmPassword) {
      const msg = 'Passwords do not match. Please re-enter your password.';
      setFormError(msg);
      onErrorToast(msg);
      return;
    }

    if (password.length < 6) {
      const msg = 'Password must be at least 6 characters.';
      setFormError(msg);
      onErrorToast(msg);
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

      setFormSuccess(res.message);
      onSuccessToast('Verification code sent to your email.');
      setCooldown(60);
      setOtp('');
      setMode('VERIFY_REGISTRATION');
    } catch (err: any) {
      const msg = err.message || 'Registration failed. Please check your details.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyRegistrationOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);
    setSubmitting(true);
    try {
      const res = await authApi.verifyEmailOtp(email.trim().toLowerCase(), otp.trim());
      if (res?.user && res?.token) {
        onSuccessToast(`Account activated! Welcome to StockSense, ${res.user.fullName}!`);
        onLoginSuccess(res.user);
      } else {
        // Manager registration pending approval
        const successMsg = 'Email verified successfully! Your Manager access request is submitted and pending administrator approval. You will be able to log in once an existing manager approves your request.';
        setFormSuccess(successMsg);
        onSuccessToast('Manager access request submitted for administrator approval.');
        switchMode('LOGIN');
      }
    } catch (err: any) {
      const msg = err.message || 'Invalid or expired verification code.';
      if (msg.includes('pending administrator approval')) {
        setFormSuccess(msg);
        onSuccessToast('Manager access request submitted for administrator approval.');
        switchMode('LOGIN');
      } else {
        setFormError(msg);
        onErrorToast(msg);
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleResendRegistrationOtp = async () => {
    if (cooldown > 0 || submitting) return;
    setFormError(null);
    setFormSuccess(null);
    setSubmitting(true);
    try {
      const res = await authApi.resendVerificationOtp(email.trim().toLowerCase());
      setFormSuccess(res.message);
      onSuccessToast(res.message);
      setCooldown(60);
    } catch (err: any) {
      const msg = err.message || 'Failed to resend verification code. Please try again.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);
    setSubmitting(true);
    try {
      const res = await authApi.forgotPassword(email.trim().toLowerCase());
      setFormSuccess(res.message);
      onSuccessToast(res.message);
      setCooldown(60);
      setMode('VERIFY_RESET');
    } catch (err: any) {
      const msg = err.message || 'Failed to send OTP code. Please try again.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyResetOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);
    setSubmitting(true);
    try {
      const res = await authApi.verifyOtp(email.trim().toLowerCase(), otp.trim());
      setFormSuccess(res.message);
      onSuccessToast('Code verified! Enter your new password below.');
      setMode('RESET');
    } catch (err: any) {
      const msg = err.message || 'Invalid or expired verification code.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);

    if (newPassword !== confirmNewPassword) {
      const msg = 'New passwords do not match. Please re-enter.';
      setFormError(msg);
      onErrorToast(msg);
      return;
    }

    if (newPassword.length < 6) {
      const msg = 'Password must be at least 6 characters.';
      setFormError(msg);
      onErrorToast(msg);
      return;
    }

    setSubmitting(true);
    try {
      const res = await authApi.resetPassword({
        email: email.trim().toLowerCase(),
        otp: otp.trim(),
        newPassword,
      });
      onSuccessToast(res.message);
      setFormSuccess(res.message);
      switchMode('LOGIN');
      setPassword('');
      setConfirmPassword('');
      setOtp('');
      setNewPassword('');
      setConfirmNewPassword('');
    } catch (err: any) {
      const msg = err.message || 'Password reset failed. Invalid or expired code.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const isResetActive = mode === 'FORGOT' || mode === 'VERIFY_RESET' || mode === 'RESET';
  const isRegisterActive = mode === 'REGISTER' || mode === 'VERIFY_REGISTRATION';

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #090d16 0%, #111827 50%, #0f172a 100%)',
      padding: '24px',
      color: '#f8fafc',
      fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
    }}>
      <div style={{
        width: '100%',
        maxWidth: '480px',
        background: '#131b2e',
        borderRadius: '16px',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        padding: '36px 32px',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.6), 0 0 40px rgba(59, 130, 246, 0.08)',
        boxSizing: 'border-box'
      }}>
        {/* Header Branding */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '54px',
            height: '54px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
            boxShadow: '0 8px 16px rgba(37, 99, 235, 0.35)',
            marginBottom: '14px'
          }}>
            <Boxes size={28} color="#fff" />
          </div>
          <h1 style={{
            fontSize: '1.75rem',
            fontWeight: 800,
            letterSpacing: '-0.025em',
            margin: '0 0 6px 0',
            background: 'linear-gradient(to right, #ffffff, #93c5fd)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent'
          }}>
            StockSense
          </h1>
          <p style={{
            fontSize: '0.85rem',
            color: '#94a3b8',
            margin: 0,
            fontWeight: 500,
            letterSpacing: '0.02em'
          }}>
            Enterprise Inventory & Warehouse Operations
          </p>
        </div>

        {/* Tab Navigation */}
        <div style={{
          display: 'flex',
          background: 'rgba(15, 23, 42, 0.7)',
          borderRadius: '10px',
          padding: '4px',
          marginBottom: '24px',
          border: '1px solid #1e293b'
        }}>
          <button
            type="button"
            id="tab-signin"
            onClick={() => switchMode('LOGIN')}
            style={{
              flex: 1,
              padding: '8px 12px',
              fontSize: '0.85rem',
              fontWeight: mode === 'LOGIN' ? 700 : 500,
              borderRadius: '7px',
              border: 'none',
              cursor: 'pointer',
              background: mode === 'LOGIN' ? '#2563eb' : 'transparent',
              color: mode === 'LOGIN' ? '#fff' : '#94a3b8',
              transition: 'all 0.15s ease',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px'
            }}
          >
            <LogIn size={15} />
            <span>Sign In</span>
          </button>

          <button
            type="button"
            id="tab-register"
            onClick={() => switchMode('REGISTER')}
            style={{
              flex: 1,
              padding: '8px 12px',
              fontSize: '0.85rem',
              fontWeight: isRegisterActive ? 700 : 500,
              borderRadius: '7px',
              border: 'none',
              cursor: 'pointer',
              background: isRegisterActive ? '#2563eb' : 'transparent',
              color: isRegisterActive ? '#fff' : '#94a3b8',
              transition: 'all 0.15s ease',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px'
            }}
          >
            <UserPlus size={15} />
            <span>Register</span>
          </button>

          <button
            type="button"
            id="tab-reset"
            onClick={() => switchMode('FORGOT')}
            style={{
              flex: 1,
              padding: '8px 12px',
              fontSize: '0.85rem',
              fontWeight: isResetActive ? 700 : 500,
              borderRadius: '7px',
              border: 'none',
              cursor: 'pointer',
              background: isResetActive ? '#2563eb' : 'transparent',
              color: isResetActive ? '#fff' : '#94a3b8',
              transition: 'all 0.15s ease',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px'
            }}
          >
            <KeyRound size={15} />
            <span>Reset</span>
          </button>
        </div>

        {/* Global Error Alert */}
        {formError && (
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px',
            padding: '12px 14px',
            background: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: '8px',
            marginBottom: '18px',
            color: '#fca5a5',
            fontSize: '0.85rem'
          }}>
            <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
            <div style={{ flex: 1, lineHeight: 1.4 }}>{formError}</div>
          </div>
        )}

        {/* Global Success Alert */}
        {formSuccess && (
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px',
            padding: '12px 14px',
            background: 'rgba(34, 197, 94, 0.12)',
            border: '1px solid rgba(34, 197, 94, 0.3)',
            borderRadius: '8px',
            marginBottom: '18px',
            color: '#86efac',
            fontSize: '0.85rem'
          }}>
            <CheckCircle2 size={18} style={{ flexShrink: 0, marginTop: '1px' }} />
            <div style={{ flex: 1, lineHeight: 1.4 }}>{formSuccess}</div>
          </div>
        )}

        {/* 1. SIGN IN FORM */}
        {mode === 'LOGIN' && (
          <form onSubmit={handleLogin}>
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                Work Email
              </label>
              <input
                id="login-email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #334155',
                  background: '#1e293b',
                  color: '#fff',
                  fontSize: '0.9rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label style={{ fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1' }}>
                  Password
                </label>
                <button
                  type="button"
                  onClick={() => switchMode('FORGOT')}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: '#60a5fa',
                    fontSize: '0.78rem',
                    cursor: 'pointer',
                    padding: 0
                  }}
                >
                  Forgot password?
                </button>
              </div>
              <div style={{ position: 'relative' }}>
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  style={{
                    width: '100%',
                    padding: '10px 40px 10px 14px',
                    borderRadius: '8px',
                    border: '1px solid #334155',
                    background: '#1e293b',
                    color: '#fff',
                    fontSize: '0.9rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: '#64748b',
                    cursor: 'pointer',
                    padding: 0
                  }}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button
              id="login-submit-btn"
              type="submit"
              disabled={submitting}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '8px',
                border: 'none',
                background: '#3b82f6',
                color: '#fff',
                fontWeight: 600,
                fontSize: '0.95rem',
                cursor: submitting ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                transition: 'background 0.15s ease',
                boxShadow: '0 4px 12px rgba(59, 130, 246, 0.3)'
              }}
            >
              {submitting ? 'Authenticating...' : 'Sign In to Dashboard'}
              <ArrowRight size={16} />
            </button>
          </form>
        )}

        {/* 2. REGISTER FORM */}
        {mode === 'REGISTER' && (
          <form onSubmit={handleRegister}>
            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                Full Name *
              </label>
              <input
                id="register-fullname"
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Jane Smith"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #334155',
                  background: '#1e293b',
                  color: '#fff',
                  fontSize: '0.9rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                Work Email *
              </label>
              <input
                id="register-email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #334155',
                  background: '#1e293b',
                  color: '#fff',
                  fontSize: '0.9rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            {/* Role Selection Cards */}
            <div style={{ marginBottom: '16px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px' }}>
                Select Role *
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                {/* Worker Option */}
                <div
                  id="role-select-worker"
                  onClick={() => setSelectedRole('WORKER')}
                  style={{
                    padding: '12px',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    background: selectedRole === 'WORKER' ? 'rgba(37, 99, 235, 0.15)' : '#1e293b',
                    border: `2px solid ${selectedRole === 'WORKER' ? '#3b82f6' : '#334155'}`,
                    transition: 'all 0.15s ease',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '4px'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Users size={16} color={selectedRole === 'WORKER' ? '#60a5fa' : '#94a3b8'} />
                      <strong style={{ fontSize: '0.9rem', color: selectedRole === 'WORKER' ? '#fff' : '#cbd5e1' }}>
                        Worker
                      </strong>
                    </div>
                    <input
                      type="radio"
                      name="role"
                      checked={selectedRole === 'WORKER'}
                      onChange={() => setSelectedRole('WORKER')}
                      style={{ accentColor: '#3b82f6' }}
                    />
                  </div>
                  <span style={{ fontSize: '0.73rem', color: '#94a3b8', lineHeight: 1.3 }}>
                    Perform day-to-day warehouse and inventory operations.
                  </span>
                </div>

                {/* Manager Option */}
                <div
                  id="role-select-manager"
                  onClick={() => setSelectedRole('MANAGER')}
                  style={{
                    padding: '12px',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    background: selectedRole === 'MANAGER' ? 'rgba(147, 51, 234, 0.15)' : '#1e293b',
                    border: `2px solid ${selectedRole === 'MANAGER' ? '#a855f7' : '#334155'}`,
                    transition: 'all 0.15s ease',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '4px'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <ShieldCheck size={16} color={selectedRole === 'MANAGER' ? '#c084fc' : '#94a3b8'} />
                      <strong style={{ fontSize: '0.9rem', color: selectedRole === 'MANAGER' ? '#fff' : '#cbd5e1' }}>
                        Manager
                      </strong>
                    </div>
                    <input
                      type="radio"
                      name="role"
                      checked={selectedRole === 'MANAGER'}
                      onChange={() => setSelectedRole('MANAGER')}
                      style={{ accentColor: '#a855f7' }}
                    />
                  </div>
                  <span style={{ fontSize: '0.73rem', color: '#94a3b8', lineHeight: 1.3 }}>
                    Manage inventory, warehouses, users, and authorized operations.
                  </span>
                </div>
              </div>

              {selectedRole === 'MANAGER' && (
                <div style={{
                  marginTop: '8px',
                  padding: '8px 10px',
                  borderRadius: '6px',
                  background: 'rgba(168, 85, 247, 0.1)',
                  border: '1px solid rgba(168, 85, 247, 0.25)',
                  fontSize: '0.75rem',
                  color: '#d8b4fe',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px'
                }}>
                  <Info size={14} style={{ flexShrink: 0 }} />
                  <span>
                    <strong>Manager Access Notice:</strong> Manager accounts require administrator approval before activation.
                  </span>
                </div>
              )}
            </div>

            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                Password (min 6 characters) *
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  id="register-password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  style={{
                    width: '100%',
                    padding: '10px 40px 10px 14px',
                    borderRadius: '8px',
                    border: '1px solid #334155',
                    background: '#1e293b',
                    color: '#fff',
                    fontSize: '0.9rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: '#64748b',
                    cursor: 'pointer',
                    padding: 0
                  }}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                Confirm Password *
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  id="register-confirm-password"
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  minLength={6}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  style={{
                    width: '100%',
                    padding: '10px 40px 10px 14px',
                    borderRadius: '8px',
                    border: '1px solid #334155',
                    background: '#1e293b',
                    color: '#fff',
                    fontSize: '0.9rem',
                    outline: 'none',
                    boxSizing: 'border-box'
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: '#64748b',
                    cursor: 'pointer',
                    padding: 0
                  }}
                >
                  {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button
              id="register-submit-btn"
              type="submit"
              disabled={submitting}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '8px',
                border: 'none',
                background: selectedRole === 'MANAGER' ? '#9333ea' : '#3b82f6',
                color: '#fff',
                fontWeight: 600,
                fontSize: '0.95rem',
                cursor: submitting ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: selectedRole === 'MANAGER'
                  ? '0 4px 12px rgba(147, 51, 234, 0.3)'
                  : '0 4px 12px rgba(59, 130, 246, 0.3)'
              }}
            >
              {submitting
                ? 'Creating Account...'
                : selectedRole === 'MANAGER'
                ? 'Request Manager Account'
                : 'Register Worker Account'}
              <ArrowRight size={16} />
            </button>
          </form>
        )}

        {/* 3. VERIFY REGISTRATION OTP */}
        {mode === 'VERIFY_REGISTRATION' && (
          <form onSubmit={handleVerifyRegistrationOtp}>
            <div style={{ textAlign: 'center', marginBottom: '18px' }}>
              <ShieldCheck size={36} color="#3b82f6" style={{ margin: '0 auto 8px auto' }} />
              <h3 style={{ margin: '0 0 6px 0', fontSize: '1.15rem' }}>Verify Your Email</h3>
              <p style={{ fontSize: '0.82rem', color: '#94a3b8', margin: 0, lineHeight: 1.4 }}>
                A 6-digit verification code has been sent to <strong>{email}</strong>
              </p>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '8px', textAlign: 'center' }}>
                Enter 6-Digit OTP Code *
              </label>
              <input
                id="verify-reg-otp-input"
                type="text"
                required
                maxLength={6}
                value={otp}
                onChange={(e) => setOtp(e.target.value.trim())}
                placeholder="123456"
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: '8px',
                  border: '1px solid #3b82f6',
                  background: '#0f172a',
                  color: '#fff',
                  fontSize: '1.4rem',
                  fontWeight: 700,
                  letterSpacing: '8px',
                  textAlign: 'center',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
              <button
                type="button"
                onClick={() => switchMode('REGISTER')}
                style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #475569',
                  background: 'transparent',
                  color: '#94a3b8',
                  fontSize: '0.85rem',
                  cursor: 'pointer'
                }}
              >
                Back
              </button>
              <button
                id="verify-reg-otp-submit-btn"
                type="submit"
                disabled={submitting || otp.length !== 6}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderRadius: '8px',
                  border: 'none',
                  background: '#3b82f6',
                  color: '#fff',
                  fontWeight: 600,
                  fontSize: '0.9rem',
                  cursor: submitting || otp.length !== 6 ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px'
                }}
              >
                {submitting ? 'Verifying...' : 'Verify Code & Activate'}
              </button>
            </div>

            {/* Resend Cooldown */}
            <div style={{ textAlign: 'center' }}>
              <button
                type="button"
                disabled={cooldown > 0 || submitting}
                onClick={handleResendRegistrationOtp}
                style={{
                  background: 'none',
                  border: 'none',
                  color: cooldown > 0 ? '#64748b' : '#60a5fa',
                  fontSize: '0.8rem',
                  cursor: cooldown > 0 ? 'not-allowed' : 'pointer'
                }}
              >
                {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend verification code'}
              </button>
            </div>
          </form>
        )}

        {/* 4. FORGOT PASSWORD (STEP 1: REQUEST OTP) */}
        {mode === 'FORGOT' && (
          <form onSubmit={handleForgotPassword}>
            <p style={{ fontSize: '0.85rem', color: '#94a3b8', marginBottom: '16px', lineHeight: 1.5 }}>
              Enter your registered work email to receive a 6-digit one-time password (OTP) verification code.
            </p>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                Registered Work Email *
              </label>
              <input
                id="forgot-email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="name@company.com"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #334155',
                  background: '#1e293b',
                  color: '#fff',
                  fontSize: '0.9rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <button
              id="forgot-submit-btn"
              type="submit"
              disabled={submitting}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '8px',
                border: 'none',
                background: '#3b82f6',
                color: '#fff',
                fontWeight: 600,
                fontSize: '0.95rem',
                cursor: submitting ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px'
              }}
            >
              {submitting ? 'Sending OTP...' : 'Send 6-Digit Verification Code'}
            </button>
          </form>
        )}

        {/* 5. VERIFY RESET OTP (STEP 2: VALIDATE CODE) */}
        {mode === 'VERIFY_RESET' && (
          <form onSubmit={handleVerifyResetOtp}>
            <div style={{ textAlign: 'center', marginBottom: '16px' }}>
              <ShieldCheck size={32} color="#60a5fa" style={{ margin: '0 auto 8px auto' }} />
              <h3 style={{ margin: '0 0 4px 0', fontSize: '1.1rem' }}>Enter Verification Code</h3>
              <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: 0 }}>
                Sent to <strong>{email}</strong>
              </p>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px', textAlign: 'center' }}>
                6-Digit OTP Code *
              </label>
              <input
                id="verify-otp-input"
                type="text"
                required
                maxLength={6}
                value={otp}
                onChange={(e) => setOtp(e.target.value.trim())}
                placeholder="123456"
                style={{
                  width: '100%',
                  padding: '12px',
                  borderRadius: '8px',
                  border: '1px solid #3b82f6',
                  background: '#0f172a',
                  color: '#fff',
                  fontSize: '1.4rem',
                  fontWeight: 700,
                  letterSpacing: '8px',
                  textAlign: 'center',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
              <button
                type="button"
                onClick={() => switchMode('FORGOT')}
                style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #475569',
                  background: 'transparent',
                  color: '#94a3b8',
                  fontSize: '0.85rem',
                  cursor: 'pointer'
                }}
              >
                Back
              </button>
              <button
                id="verify-otp-submit-btn"
                type="submit"
                disabled={submitting || otp.length !== 6}
                style={{
                  flex: 1,
                  padding: '10px',
                  borderRadius: '8px',
                  border: 'none',
                  background: '#3b82f6',
                  color: '#fff',
                  fontWeight: 600,
                  fontSize: '0.9rem',
                  cursor: submitting || otp.length !== 6 ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px'
                }}
              >
                {submitting ? 'Verifying Code...' : 'Verify Code & Proceed'}
              </button>
            </div>

            {/* Resend Cooldown */}
            <div style={{ textAlign: 'center' }}>
              <button
                type="button"
                disabled={cooldown > 0 || submitting}
                onClick={handleForgotPassword}
                style={{
                  background: 'none',
                  border: 'none',
                  color: cooldown > 0 ? '#64748b' : '#60a5fa',
                  fontSize: '0.8rem',
                  cursor: cooldown > 0 ? 'not-allowed' : 'pointer'
                }}
              >
                {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend verification code'}
              </button>
            </div>
          </form>
        )}

        {/* 6. RESET PASSWORD (STEP 3: ENTER NEW PASSWORD) */}
        {mode === 'RESET' && (
          <form onSubmit={handleResetPassword}>
            <div style={{ textAlign: 'center', marginBottom: '16px' }}>
              <UserCheck size={32} color="#34d399" style={{ margin: '0 auto 8px auto' }} />
              <h3 style={{ margin: '0 0 4px 0', fontSize: '1.1rem' }}>Set New Password</h3>
              <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: 0 }}>
                Code verified. Choose a strong new password.
              </p>
            </div>

            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                New Password (min 6 characters) *
              </label>
              <input
                id="reset-new-password"
                type="password"
                required
                minLength={6}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="••••••••"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #334155',
                  background: '#1e293b',
                  color: '#fff',
                  fontSize: '0.9rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                Confirm New Password *
              </label>
              <input
                id="reset-confirm-password"
                type="password"
                required
                minLength={6}
                value={confirmNewPassword}
                onChange={(e) => setConfirmNewPassword(e.target.value)}
                placeholder="••••••••"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  border: '1px solid #334155',
                  background: '#1e293b',
                  color: '#fff',
                  fontSize: '0.9rem',
                  outline: 'none',
                  boxSizing: 'border-box'
                }}
              />
            </div>

            <button
              id="reset-password-submit-btn"
              type="submit"
              disabled={submitting}
              style={{
                width: '100%',
                padding: '12px',
                borderRadius: '8px',
                border: 'none',
                background: '#10b981',
                color: '#fff',
                fontWeight: 600,
                fontSize: '0.95rem',
                cursor: submitting ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)'
              }}
            >
              {submitting ? 'Updating Password...' : 'Save New Password & Sign In'}
            </button>
          </form>
        )}

        {/* Footer Security Badges */}
        <div style={{
          marginTop: '32px',
          paddingTop: '20px',
          borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '16px',
          color: '#64748b',
          fontSize: '0.75rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <ShieldCheck size={13} color="#34d399" />
            <span>BCrypt Encrypted</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <Server size={13} color="#60a5fa" />
            <span>HMAC-SHA256 Auth</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthScreen;
