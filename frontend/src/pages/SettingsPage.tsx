import React from 'react';
import { Server, Mail, ShieldCheck } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  return (
    <div className="settings-page-container">
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>System Settings & Configuration</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
          Environment configuration, database connection, and security credentials
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '24px' }}>
        {/* Environment & Backend Architecture Card */}
        <div className="card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
            <Server size={20} style={{ color: 'var(--primary)' }} />
            <h3 style={{ fontSize: '1rem', fontWeight: 600 }}>Architecture & Runtime</h3>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.85rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
              <span style={{ color: 'var(--text-muted)' }}>Backend Framework</span>
              <strong>Java 21 / Spring Boot 3.3.4</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
              <span style={{ color: 'var(--text-muted)' }}>Frontend Framework</span>
              <strong>React 18 / TypeScript / Vite</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
              <span style={{ color: 'var(--text-muted)' }}>Database</span>
              <strong>PostgreSQL 16 (Dockerized)</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
              <span style={{ color: 'var(--text-muted)' }}>Port Mapping</span>
              <code>Frontend :5173 ➔ Backend :8080</code>
            </div>
          </div>
        </div>

        {/* Security & Token Authentication Card */}
        <div className="card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
            <ShieldCheck size={20} style={{ color: 'var(--success)' }} />
            <h3 style={{ fontSize: '1rem', fontWeight: 600 }}>Security & Password Management</h3>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '0.85rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
              <span style={{ color: 'var(--text-muted)' }}>Password Hashing</span>
              <strong>BCrypt (strength 10)</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
              <span style={{ color: 'var(--text-muted)' }}>Session Security</span>
              <strong>HMAC-SHA256 Signed Tokens</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
              <span style={{ color: 'var(--text-muted)' }}>Password Reset OTP</span>
              <strong>Hashed 6-digit one-time code</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-subtle)' }}>
              <span style={{ color: 'var(--text-muted)' }}>OTP Safety Limits</span>
              <strong>10m expiry • 60s cooldown • 5 attempts</strong>
            </div>
          </div>
        </div>
      </div>

      {/* SMTP Email Configuration Guide Card */}
      <div className="card" style={{ padding: '20px', marginTop: '24px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <Mail size={20} style={{ color: '#d97706' }} />
          <h3 style={{ fontSize: '1rem', fontWeight: 600 }}>Email & SMTP Integration Configuration</h3>
        </div>

        <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
          StockSense uses Spring Mail to dispatch password reset verification OTP emails. When SMTP credentials are not provided or unreachable, the system automatically engages the <strong>Fallback Logger Mode</strong>, printing verification codes directly to the server console log with security markers so you can test end-to-end authentication without external SMTP dependencies.
        </p>

        <div style={{ background: 'var(--surface-sunken)', padding: '14px 18px', borderRadius: '6px', fontSize: '0.8rem', fontFamily: 'monospace' }}>
          <div style={{ color: 'var(--primary)', fontWeight: 700, marginBottom: '6px' }}># Required Environment Variables for Live Production SMTP</div>
          <div>SPRING_MAIL_HOST=smtp.gmail.com</div>
          <div>SPRING_MAIL_PORT=587</div>
          <div>SPRING_MAIL_USERNAME=your-account@gmail.com</div>
          <div>SPRING_MAIL_PASSWORD=your-app-password</div>
          <div>SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true</div>
          <div>SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true</div>
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;
