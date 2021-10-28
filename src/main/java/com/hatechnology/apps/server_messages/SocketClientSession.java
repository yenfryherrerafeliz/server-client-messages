package com.hatechnology.apps.server_messages;

import java.time.LocalDateTime;

public class SocketClientSession {
    private String id;
    private String identifier;
    private LocalDateTime connectionDate;
    private LocalDateTime disconnectionDate;
    private LocalDateTime lastTransactionDate;

    public SocketClientSession(String id, LocalDateTime connectionDate) {
        this.id = id;
        this.connectionDate = connectionDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public LocalDateTime getConnectionDate() {
        return connectionDate;
    }

    public void setConnectionDate(LocalDateTime connectionDate) {
        this.connectionDate = connectionDate;
    }

    public LocalDateTime getDisconnectionDate() {
        return disconnectionDate;
    }

    public void setDisconnectionDate(LocalDateTime disconnectionDate) {
        this.disconnectionDate = disconnectionDate;
    }

    public LocalDateTime getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(LocalDateTime lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }
}
