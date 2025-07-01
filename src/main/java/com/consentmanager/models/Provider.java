package com.consentmanager.models;

import java.time.LocalDate;

public class Provider {
    private Integer providerId;
    private Integer credentialId; // FK
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String mobileNo;
    private LocalDate dateOfBirth;
    private Integer age;
    private String publicKey;
    private Boolean isActive; // In DB: INTEGER (0 or 1)

    // Default constructor
    public Provider() {
    }

    // Constructor for creation
    public Provider(Integer credentialId, String firstName, String middleName, String lastName, String email, String mobileNo, LocalDate dateOfBirth, Integer age, String publicKey, Boolean isActive) {
        this.credentialId = credentialId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.mobileNo = mobileNo;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.publicKey = publicKey;
        this.isActive = isActive;
    }

    // Full constructor
    public Provider(Integer providerId, Integer credentialId, String firstName, String middleName, String lastName, String email, String mobileNo, LocalDate dateOfBirth, Integer age, String publicKey, Boolean isActive) {
        this.providerId = providerId;
        this.credentialId = credentialId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.mobileNo = mobileNo;
        this.dateOfBirth = dateOfBirth;
        this.age = age;
        this.publicKey = publicKey;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public Integer getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(Integer credentialId) {
        this.credentialId = credentialId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
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
        return "Provider{" +
                "providerId=" + providerId +
                ", credentialId=" + credentialId +
                ", firstName='" + firstName + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
