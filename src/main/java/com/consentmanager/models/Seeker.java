package com.consentmanager.models;

public class Seeker {
    private Integer seekerId;
    private Integer credentialId; // FK
    private String name;
    private String type; // Enum-like: "Bank", "Government", "Private Company", "Other"
    private String registrationNo;
    private String email;
    private String contactNo;
    private String address;
    private String publicKey;
    private Boolean isActive; // In DB: INTEGER (0 or 1)

    // Default constructor
    public Seeker() {
    }

    // Constructor for creation
    public Seeker(Integer credentialId, String name, String type, String registrationNo, String email, String contactNo, String address, String publicKey, Boolean isActive) {
        this.credentialId = credentialId;
        this.name = name;
        this.type = type;
        this.registrationNo = registrationNo;
        this.email = email;
        this.contactNo = contactNo;
        this.address = address;
        this.publicKey = publicKey;
        this.isActive = isActive;
    }

    // Full constructor
    public Seeker(Integer seekerId, Integer credentialId, String name, String type, String registrationNo, String email, String contactNo, String address, String publicKey, Boolean isActive) {
        this.seekerId = seekerId;
        this.credentialId = credentialId;
        this.name = name;
        this.type = type;
        this.registrationNo = registrationNo;
        this.email = email;
        this.contactNo = contactNo;
        this.address = address;
        this.publicKey = publicKey;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Integer getSeekerId() {
        return seekerId;
    }

    public void setSeekerId(Integer seekerId) {
        this.seekerId = seekerId;
    }

    public Integer getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Integer credentialId) {
        this.credentialId = credentialId;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Seeker{" +
                "seekerId=" + seekerId +
                ", credentialId=" + credentialId +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
