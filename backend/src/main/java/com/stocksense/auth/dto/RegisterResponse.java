package com.stocksense.auth.dto;

public class RegisterResponse {
    private String message;
    private String email;
    private String role;
    private String requestedRole;
    private String approvalStatus;
    private boolean requiresEmailVerification;

    public RegisterResponse() {
    }

    public RegisterResponse(String message, String email, String role, String requestedRole, String approvalStatus, boolean requiresEmailVerification) {
        this.message = message;
        this.email = email;
        this.role = role;
        this.requestedRole = requestedRole;
        this.approvalStatus = approvalStatus;
        this.requiresEmailVerification = requiresEmailVerification;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public boolean isRequiresEmailVerification() {
        return requiresEmailVerification;
    }

    public void setRequiresEmailVerification(boolean requiresEmailVerification) {
        this.requiresEmailVerification = requiresEmailVerification;
    }
}
