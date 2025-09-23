package com.example.model.dto;

import com.example.model.UserRole;
import com.example.model.UserStatus;

public class AdminUserUpdateDTO {
    private String fullName;
    private String phoneNumber;
    private UserRole role;
    private UserStatus status;

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}