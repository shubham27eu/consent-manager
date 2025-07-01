package com.consentmanager.models;

import java.time.LocalDateTime;

public class SeekerBacklog {
    private Integer backlogId;
    private String username;
    private String password; // Hashed
    private String role; // Should always be "seeker"
    private String name;
    private String type; // "Bank", "Government", "Private Company", "Other"
    private String registrationNo;
    private String email;
    private String contactNo;
    private String address;
    private String publicKey;
    private String status; // "pending", "rejected", "approved"
    private LocalDateTime createdAt; // Stored as TEXT (ISO8601) in DB

    // Default constructor
    public SeekerBacklog() {
    }

    // Constructor for creation
    public SeekerBacklog(String username, String password, String role, String name, String type, String registrationNo, String email, String contactNo, String address, String publicKey, String status) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
        this.type = type;
        this.registrationNo = registrationNo;
        this.email = email;
        this.contactNo = contactNo;
        this.address = address;
        this.publicKey = publicKey;
        this.status = status;
    }

    // Full constructor
    public SeekerBacklog(Integer backlogId, String username, String password, String role, String name, String type, String registrationNo, String email, String contactNo, String address, String publicKey, String status, LocalDateTime createdAt) {
        this.backlogId = backlogId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
        this.type = type;
        this.registrationNo = registrationNo;
        this.email = email;
        this.contactNo = contactNo;
        this.address = address;
        this.publicKey = publicKey;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Integer getBacklogId() {
        return backlogId;
    }

    public void setBacklogId(Integer backlogId) {
        this.backlogId = backlogId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SeekerBacklog{" +
                "backlogId=" + backlogId +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
