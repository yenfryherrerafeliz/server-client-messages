package com.hatechnology.apps.core_messaging;

public class SyncMessageQueue {
    private String id;
    private SyncMessage syncMessage;
    private boolean sent;
    private boolean responded;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SyncMessage getSyncMessage() {
        return syncMessage;
    }

    public void setSyncMessage(SyncMessage syncMessage) {
        this.syncMessage = syncMessage;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public boolean isResponded() {
        return responded;
    }

    public void setResponded(boolean responded) {
        this.responded = responded;
    }
}
