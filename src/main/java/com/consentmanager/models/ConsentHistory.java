package com.consentmanager.models;

import java.time.LocalDateTime;

public class ConsentHistory {
    private Integer historyId;
    private Integer consentId;
    private String action; // e.g., "requested", "approved", "rejected", "accessed"
    private Integer actorId; // ID of the user (seeker, provider, admin) or system performing action
    private String performedByRole; // Role of the actor, e.g. "seeker", "provider", "system"
    private LocalDateTime timestamp;
    private String details; // Optional details, e.g., reason for rejection

    // Constructors
    public ConsentHistory() {
    }

    public ConsentHistory(Integer consentId, String action, Integer actorId, String performedByRole, String details) {
        this.consentId = consentId;
        this.action = action;
        this.actorId = actorId;
        this.performedByRole = performedByRole;
        // Timestamp will be set on creation by DAO or service
        this.details = details;
    }

    public ConsentHistory(Integer historyId, Integer consentId, String action, Integer actorId, String performedByRole, LocalDateTime timestamp, String details) {
        this.historyId = historyId;
        this.consentId = consentId;
        this.action = action;
        this.actorId = actorId;
        this.performedByRole = performedByRole;
        this.timestamp = timestamp;
        this.details = details;
    }

    // Getters and Setters
    public Integer getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Integer historyId) {
        this.historyId = historyId;
    }

    public Integer getConsentId() {
        return consentId;
    }

    public void setConsentId(Integer consentId) {
        this.consentId = consentId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getActorId() {
        return actorId;
    }

    public void setActorId(Integer actorId) {
        this.actorId = actorId;
    }

    public String getPerformedByRole() {
        return performedByRole;
    }

    public void setPerformedByRole(String performedByRole) {
        this.performedByRole = performedByRole;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "ConsentHistory{" +
                "historyId=" + historyId +
                ", consentId=" + consentId +
                ", action='" + action + '\'' +
                ", actorId=" + actorId +
                ", performedByRole='" + performedByRole + '\'' +
                ", timestamp=" + timestamp +
                ", details='" + details + '\'' +
                '}';
    }
}
