package com.consentmanager.models;

import java.time.LocalDateTime;

public class DataItem {
    private Integer dataItemId;
    private Integer providerId;
    private String name;
    private String description;
    private String type; // "text", "file", etc.
    private String data; // Actual text data, or URL/path for file
    private String aesKeyEncrypted; // Encrypted AES key for the data (if applicable)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public DataItem() {
    }

    public DataItem(Integer providerId, String name, String description, String type, String data, String aesKeyEncrypted) {
        this.providerId = providerId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.data = data;
        this.aesKeyEncrypted = aesKeyEncrypted;
    }

    public DataItem(Integer dataItemId, Integer providerId, String name, String description, String type, String data, String aesKeyEncrypted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.dataItemId = dataItemId;
        this.providerId = providerId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.data = data;
        this.aesKeyEncrypted = aesKeyEncrypted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    // Getters and Setters
    public Integer getDataItemId() {
        return dataItemId;
    }

    public void setDataItemId(Integer dataItemId) {
        this.dataItemId = dataItemId;
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getAesKeyEncrypted() {
        return aesKeyEncrypted;
    }

    public void setAesKeyEncrypted(String aesKeyEncrypted) {
        this.aesKeyEncrypted = aesKeyEncrypted;
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

    @Override
    public String toString() {
        return "DataItem{" +
                "dataItemId=" + dataItemId +
                ", providerId=" + providerId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", data='" + (type != null && type.equals("text") && data != null && data.length() > 50 ? data.substring(0, 50) + "..." : data) + '\'' + // Avoid logging large text data
                ", aesKeyEncrypted='" + (aesKeyEncrypted != null ? aesKeyEncrypted.substring(0, Math.min(aesKeyEncrypted.length(),10)) + "..." : "null") + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
