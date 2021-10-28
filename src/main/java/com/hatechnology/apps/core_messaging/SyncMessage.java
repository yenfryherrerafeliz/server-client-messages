package com.hatechnology.apps.core_messaging;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

/***
 * Here you must define every class you want share by messages
 * for the server and for the client as well
***/
public class SyncMessage implements Serializable {

    //Message id is automatically calculated
    private String id;

    //Message properties
    private String sourceId;
    private String to;
    private String validationToken;
    private boolean waitResponse;
    private boolean splitMessage;
    private int messageParts;
    private boolean encryptMessage;
    private String sessionId;
    private boolean isPingingServer;
    private boolean secureDelivery;
    private LocalDateTime messageDateTime;
    private boolean confirmingDelivery;


    //Messaging main objects
    private SyncRequest syncRequest;
    private SyncResponse syncResponse;

    public SyncMessage() {
        _generateId();
    }

    private SyncMessage(boolean isPingingServer) {
        this.isPingingServer = isPingingServer;

        _generateId();

        _setDateTime();
    }

    protected SyncMessage(SyncMessage message) {
        this.id = message.getId();
        this.sourceId = message.sourceId;
        this.to = message.to;
        this.validationToken = message.getValidationToken();
        this.waitResponse = message.waitResponse;
        this.splitMessage = message.splitMessage;
        this.messageParts = message.messageParts;
        this.encryptMessage = message.encryptMessage;
        this.sessionId = message.sessionId;
        this.isPingingServer = message.isPingingServer;
        this.secureDelivery = message.secureDelivery;
        this.messageDateTime = message.messageDateTime;
        this.confirmingDelivery = message.confirmingDelivery;
        this.syncResponse = message.getSyncResponse();
        this.syncRequest = message.getSyncRequest();
    }

    public String getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

    public boolean isWaitResponse() {
        return waitResponse;
    }

    public void setWaitResponse(boolean waitResponse) {
        this.waitResponse = waitResponse;
    }

    public boolean isSplitMessage() {
        return splitMessage;
    }

    public void setSplitMessage(boolean splitMessage) {
        this.splitMessage = splitMessage;
    }

    public int getMessageParts() {
        return messageParts;
    }

    public void setMessageParts(int messageParts) {
        this.messageParts = messageParts;
    }

    public boolean isEncryptMessage() {
        return encryptMessage;
    }

    public void setEncryptMessage(boolean encryptMessage) {
        this.encryptMessage = encryptMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isPingingServer() {
        return isPingingServer;
    }

    public boolean isSecureDelivery() {
        return secureDelivery;
    }

    public void setSecureDelivery(boolean secureDelivery) {
        this.secureDelivery = secureDelivery;
    }

    public LocalDateTime getMessageDateTime() {
        return messageDateTime;
    }

    public SyncRequest getSyncRequest() {
        return syncRequest;
    }

    public void setSyncRequest(SyncRequest syncRequest) {
        this.syncRequest = syncRequest;
    }

    public SyncResponse getSyncResponse() {
        return syncResponse;
    }

    public void setSyncResponse(SyncResponse syncResponse) {
        this.syncResponse = syncResponse;
    }

    protected boolean isConfirmingDelivery() {
        return confirmingDelivery;
    }

    protected void setConfirmingDelivery(boolean confirmingDelivery) {
        this.confirmingDelivery = confirmingDelivery;
    }

    public static SyncMessage createPingMessage(){

        return new SyncMessage(true);
    }

    protected void _generateId(){
        id = UUID.randomUUID().toString();
    }

    protected void _setDateTime(){
        this.messageDateTime = LocalDateTime.now();
    }

    public SyncMessage createCopy() {
        return new SyncMessage(this);
    }
}
