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
  Server
} from 'lucide-react';
import { authApi } from '../../api/auth';
import { User } from '../../types/auth';

interface AuthScreenProps {
  onLoginSuccess: (user: User) => void;
  onSuccessToast: (msg: string) => void;
  onErrorToast: (msg: string) => void;
}

type AuthMode = 'LOGIN' | 'REGISTER' | 'FORGOT' | 'VERIFY' | 'RESET';

export const AuthScreen: React.FC<AuthScreenProps> = ({
  onLoginSuccess,
  onSuccessToast,
  onErrorToast,
}) => {
  const [mode, setMode] = useState<AuthMode>('LOGIN');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [fullName, setFullName] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
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
    setSubmitting(true);
    try {
      const res = await authApi.login({ email: email.trim(), password });
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
    setSubmitting(true);
    try {
      const res = await authApi.register({
        email: email.trim(),
        password,
        fullName: fullName.trim(),
      });
      onSuccessToast(`Account created successfully! Welcome, ${res.user.fullName}!`);
      onLoginSuccess(res.user);
    } catch (err: any) {
      const msg = err.message || 'Registration failed. Please check your details.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      const res = await authApi.forgotPassword(email.trim());
      setFormSuccess(res.message);
      onSuccessToast(res.message);
      setCooldown(60);
      setMode('VERIFY');
    } catch (err: any) {
      const msg = err.message || 'Failed to send OTP code. Please try again.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      const res = await authApi.verifyOtp(email.trim(), otp.trim());
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
    setSubmitting(true);
    try {
      const res = await authApi.resetPassword({
        email: email.trim(),
        otp: otp.trim(),
        newPassword,
      });
      onSuccessToast(res.message);
      setFormSuccess(res.message);
      switchMode('LOGIN');
      setPassword('');
      setOtp('');
      setNewPassword('');
    } catch (err: any) {
      const msg = err.message || 'Password reset failed. Invalid or expired code.';
      setFormError(msg);
      onErrorToast(msg);
    } finally {
      setSubmitting(false);
    }
  };

  // Helper to prefill demo accounts
  const fillCredentials = (demoEmail: string, demoPass: string) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setFormError(null);
  };

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
        maxWidth: '460px',
        background: 'rgba(17, 24, 39, 0.85)',
        backdropFilter: 'blur(16px)',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        borderRadius: '16px',
        padding: '36px 32px',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)'
      }}>
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '56px',
            height: '56px',
            borderRadius: '14px',
            background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
            color: '#fff',
            marginBottom: '16px',
            boxShadow: '0 10px 20px -5px rgba(59, 130, 246, 0.4)'
          }}>
            <Boxes size={30} />
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, margin: '0 0 6px 0', letterSpacing: '-0.025em' }}>
            StockSense
          </h1>
          <p style={{ fontSize: '0.875rem', color: '#94a3b8', margin: 0 }}>
            Enterprise Inventory & Warehouse Operations
          </p>
        </div>

        {/* Tab Switcher (Only if in Login, Register, or Forgot) */}
        {(mode === 'LOGIN' || mode === 'REGISTER' || mode === 'FORGOT') && (
          <div style={{
            display: 'flex',
            background: 'rgba(15, 23, 42, 0.7)',
            borderRadius: '10px',
            padding: '4px',
            marginBottom: '24px',
            border: '1px solid rgba(255, 255, 255, 0.05)'
          }}>
            <button
              type="button"
              onClick={() => switchMode('LOGIN')}
              style={{
                flex: 1,
                padding: '8px 12px',
                borderRadius: '8px',
                border: 'none',
                background: mode === 'LOGIN' ? '#3b82f6' : 'transparent',
                color: mode === 'LOGIN' ? '#fff' : '#94a3b8',
                fontWeight: mode === 'LOGIN' ? 600 : 500,
                fontSize: '0.85rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                transition: 'all 0.15s ease'
              }}
            >
              <LogIn size={15} />
              <span>Sign In</span>
            </button>

            <button
              type="button"
              onClick={() => switchMode('REGISTER')}
              style={{
                flex: 1,
                padding: '8px 12px',
                borderRadius: '8px',
                border: 'none',
                background: mode === 'REGISTER' ? '#3b82f6' : 'transparent',
                color: mode === 'REGISTER' ? '#fff' : '#94a3b8',
                fontWeight: mode === 'REGISTER' ? 600 : 500,
                fontSize: '0.85rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                transition: 'all 0.15s ease'
              }}
            >
              <UserPlus size={15} />
              <span>Register</span>
            </button>

            <button
              type="button"
              onClick={() => switchMode('FORGOT')}
              style={{
                flex: 1,
                padding: '8px 12px',
                borderRadius: '8px',
                border: 'none',
                background: mode === 'FORGOT' ? '#3b82f6' : 'transparent',
                color: mode === 'FORGOT' ? '#fff' : '#94a3b8',
                fontWeight: mode === 'FORGOT' ? 600 : 500,
                fontSize: '0.85rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
                transition: 'all 0.15s ease'
              }}
            >
              <KeyRound size={15} />
              <span>Reset</span>
            </button>
          </div>
        )}

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
            <div style={{ flex: 1 }}>{formError}</div>
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
            <div style={{ flex: 1 }}>{formSuccess}</div>
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
                placeholder="manager@stocksense.io"
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

            {/* Quick Demo Credentials */}
            <div style={{
              marginTop: '24px',
              padding: '14px',
              borderRadius: '8px',
              background: 'rgba(30, 41, 59, 0.6)',
              border: '1px dashed #334155'
            }}>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '8px' }}>
                Quick Fill Test Credentials
              </div>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  type="button"
                  onClick={() => fillCredentials('manager@stocksense.io', 'ManagerPassword123!')}
                  style={{
                    flex: 1,
                    padding: '6px 8px',
                    fontSize: '0.78rem',
                    background: '#1e293b',
                    border: '1px solid #475569',
                    borderRadius: '6px',
                    color: '#e2e8f0',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start'
                  }}
                >
                  <strong style={{ color: '#60a5fa' }}>Manager Role</strong>
                  <span style={{ fontSize: '0.7rem', color: '#94a3b8' }}>Full facility control</span>
                </button>

                <button
                  type="button"
                  onClick={() => fillCredentials('worker@stocksense.io', 'WorkerPassword123!')}
                  style={{
                    flex: 1,
                    padding: '6px 8px',
                    fontSize: '0.78rem',
                    background: '#1e293b',
                    border: '1px solid #475569',
                    borderRadius: '6px',
                    color: '#e2e8f0',
                    cursor: 'pointer',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start'
                  }}
                >
                  <strong style={{ color: '#34d399' }}>Worker Role</strong>
                  <span style={{ fontSize: '0.7rem', color: '#94a3b8' }}>Operations execution</span>
                </button>
              </div>
            </div>
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
                placeholder="operator@stocksense.io"
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
                Password (min 6 characters) *
              </label>
              <input
                id="register-password"
                type="password"
                required
                minLength={6}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
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
              <span style={{ fontSize: '0.75rem', color: '#94a3b8', marginTop: '4px', display: 'block' }}>
                Public registration assigns the <strong>WORKER</strong> role. Manager accounts are managed by administrators.
              </span>
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
                background: '#3b82f6',
                color: '#fff',
                fontWeight: 600,
                fontSize: '0.95rem',
                cursor: submitting ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                boxShadow: '0 4px 12px rgba(59, 130, 246, 0.3)'
              }}
            >
              {submitting ? 'Creating Account...' : 'Register Worker Account'}
              <ArrowRight size={16} />
            </button>
          </form>
        )}

        {/* 3. FORGOT PASSWORD (STEP 1: REQUEST OTP) */}
        {mode === 'FORGOT' && (
          <form onSubmit={handleForgotPassword}>
            <p style={{ fontSize: '0.85rem', color: '#94a3b8', marginBottom: '16px', lineHeight: 1.5 }}>
              Enter your registered work email to receive a 6-digit one-time password (OTP) verification code.
            </p>

            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#cbd5e1', marginBottom: '6px' }}>
                Registered Email Address *
              </label>
              <input
                id="forgot-email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="user@stocksense.io"
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

        {/* 4. VERIFY OTP (STEP 2: VALIDATE CODE) */}
        {mode === 'VERIFY' && (
          <form onSubmit={handleVerifyOtp}>
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

        {/* 5. RESET PASSWORD (STEP 3: ENTER NEW PASSWORD) */}
        {mode === 'RESET' && (
          <form onSubmit={handleResetPassword}>
            <div style={{ textAlign: 'center', marginBottom: '16px' }}>
              <UserCheck size={32} color="#34d399" style={{ margin: '0 auto 8px auto' }} />
              <h3 style={{ margin: '0 0 4px 0', fontSize: '1.1rem' }}>Set New Password</h3>
              <p style={{ fontSize: '0.8rem', color: '#94a3b8', margin: 0 }}>
                Code verified. Choose a strong new password.
              </p>
            </div>

            <div style={{ marginBottom: '20px' }}>
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
