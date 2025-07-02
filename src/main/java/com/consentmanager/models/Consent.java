package com.consentmanager.models;

import java.time.LocalDateTime;

public class Consent {
    private Integer consentId;
    private Integer dataItemId;
    private Integer seekerId;
    private Integer providerId;
    private String status; // e.g., "pending", "approved", "rejected", "revoked", "expired", "exhausted"
    private String reEncryptedAesKey; // AES key of DataItem, re-encrypted with Seeker's public key
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime expiresAt;
    private Integer accessCount; // How many times data has been accessed
    private Integer maxAccessCount; // Maximum allowed accesses, null for unlimited

    // Constructors
    public Consent() {
    }

    public Consent(Integer dataItemId, Integer seekerId, Integer providerId, String status, String reEncryptedAesKey, LocalDateTime requestedAt, LocalDateTime approvedAt, LocalDateTime expiresAt, Integer accessCount, Integer maxAccessCount) {
        this.dataItemId = dataItemId;
        this.seekerId = seekerId;
        this.providerId = providerId;
        this.status = status;
        this.reEncryptedAesKey = reEncryptedAesKey;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.expiresAt = expiresAt;
        this.accessCount = accessCount;
        this.maxAccessCount = maxAccessCount;
    }

    public Consent(Integer consentId, Integer dataItemId, Integer seekerId, Integer providerId, String status, String reEncryptedAesKey, LocalDateTime requestedAt, LocalDateTime approvedAt, LocalDateTime expiresAt, Integer accessCount, Integer maxAccessCount) {
        this.consentId = consentId;
        this.dataItemId = dataItemId;
        this.seekerId = seekerId;
        this.providerId = providerId;
        this.status = status;
        this.reEncryptedAesKey = reEncryptedAesKey;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.expiresAt = expiresAt;
        this.accessCount = accessCount;
        this.maxAccessCount = maxAccessCount;
    }

    // Getters and Setters
    public Integer getConsentId() {
        return consentId;
    }

    public void setConsentId(Integer consentId) {
        this.consentId = consentId;
    }

    public Integer getDataItemId() {
        return dataItemId;
    }

    public void setDataItemId(Integer dataItemId) {
        this.dataItemId = dataItemId;
    }

    public Integer getSeekerId() {
        return seekerId;
    }

    public void setSeekerId(Integer seekerId) {
        this.seekerId = seekerId;
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReEncryptedAesKey() {
        return reEncryptedAesKey;
    }

    public void setReEncryptedAesKey(String reEncryptedAesKey) {
        this.reEncryptedAesKey = reEncryptedAesKey;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public Integer getMaxAccessCount() {
        return maxAccessCount;
    }

    public void setMaxAccessCount(Integer maxAccessCount) {
        this.maxAccessCount = maxAccessCount;
    }

    @Override
    public String toString() {
        return "Consent{" +
                "consentId=" + consentId +
                ", dataItemId=" + dataItemId +
                ", seekerId=" + seekerId +
                ", providerId=" + providerId +
                ", status='" + status + '\'' +
                ", reEncryptedAesKey='" + (reEncryptedAesKey != null ? reEncryptedAesKey.substring(0, Math.min(reEncryptedAesKey.length(),10)) + "..." : "null") + '\'' +
                ", requestedAt=" + requestedAt +
                ", approvedAt=" + approvedAt +
                ", expiresAt=" + expiresAt +
                ", accessCount=" + accessCount +
                ", maxAccessCount=" + maxAccessCount +
                '}';
    }
}
