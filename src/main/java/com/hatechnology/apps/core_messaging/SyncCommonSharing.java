package com.hatechnology.apps.core_messaging;

import java.util.List;

public class SyncCommonSharing {
    private String path;
    private boolean success;
    private String errorMessage;
    private List<SyncFileBase64> files;

    public SyncCommonSharing() {
    }

    public SyncCommonSharing(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<SyncFileBase64> getFiles() {
        return files;
    }

    public void setFiles(List<SyncFileBase64> files) {
        this.files = files;
    }
}
