package com.stocksense.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    public static final String ROLE_MANAGER = "MANAGER";
    public static final String ROLE_WORKER = "WORKER";

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_REJECTED = "REJECTED";

    @Column(nullable = false, length = 50)
    private String role = ROLE_WORKER;

    @Column(name = "requested_role", nullable = false, length = 50)
    private String requestedRole = ROLE_WORKER;

    @Column(name = "approval_status", nullable = false, length = 50)
    private String approvalStatus = STATUS_APPROVED;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User() {
    }

    public User(String email, String passwordHash, String fullName, String role) {
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.passwordHash = passwordHash;
        this.fullName = fullName != null ? fullName.trim() : null;
        this.role = role != null ? role.trim().toUpperCase() : ROLE_WORKER;
        this.requestedRole = this.role;
        this.approvalStatus = STATUS_APPROVED;
        this.enabled = true;
        this.emailVerified = false;
    }

    public User(String email, String passwordHash, String fullName, String role, String requestedRole, String approvalStatus) {
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.passwordHash = passwordHash;
        this.fullName = fullName != null ? fullName.trim() : null;
        this.role = role != null ? role.trim().toUpperCase() : ROLE_WORKER;
        this.requestedRole = requestedRole != null ? requestedRole.trim().toUpperCase() : ROLE_WORKER;
        this.approvalStatus = approvalStatus != null ? approvalStatus.trim().toUpperCase() : STATUS_APPROVED;
        this.enabled = true;
        this.emailVerified = false;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.email != null) this.email = this.email.trim().toLowerCase();
        if (this.fullName != null) this.fullName = this.fullName.trim();
        if (this.role == null) this.role = ROLE_WORKER;
        if (this.requestedRole == null) this.requestedRole = this.role;
        if (this.approvalStatus == null) this.approvalStatus = STATUS_APPROVED;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.email != null) this.email = this.email.trim().toLowerCase();
        if (this.fullName != null) this.fullName = this.fullName.trim();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
