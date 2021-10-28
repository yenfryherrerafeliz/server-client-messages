package com.hatechnology.apps.core_messaging;

public class SyncRequest extends SyncCommonSharing{

    private Authentication authentication;
    private String text;

    public SyncRequest(String path) {
        super(path);
    }

    public SyncRequest(SyncRequest syncRequest) {
        setSuccess(syncRequest.isSuccess());
        setPath(syncRequest.getPath());
        setErrorMessage(syncRequest.getErrorMessage());
        setFiles(syncRequest.getFiles());
        authentication = syncRequest.authentication;
        text = syncRequest.text;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
