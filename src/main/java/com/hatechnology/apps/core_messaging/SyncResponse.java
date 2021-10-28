package com.hatechnology.apps.core_messaging;

public class SyncResponse extends SyncCommonSharing{

    public SyncResponse() {
        super();
    }

    public SyncResponse(boolean success) {
        setSuccess(success);
    }

    public SyncResponse(SyncResponse syncResponse) {
        setSuccess(syncResponse.isSuccess());
        setPath(syncResponse.getPath());
        setErrorMessage(syncResponse.getErrorMessage());
        setFiles(syncResponse.getFiles());
    }
}
