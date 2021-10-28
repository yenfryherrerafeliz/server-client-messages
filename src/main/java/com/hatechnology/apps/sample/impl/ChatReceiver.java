package com.hatechnology.apps.sample.impl;

import com.hatechnology.apps.core_messaging.SyncFileBase64;
import com.hatechnology.apps.core_messaging.SyncMessage;
import com.hatechnology.apps.core_messaging.SyncMessagePaths;
import com.hatechnology.apps.core_messaging.SyncRequest;
import com.hatechnology.apps.core_messaging.behaviors.OnMessageBehavior;
import com.hatechnology.apps.exceptions.BusinessException;
import com.hatechnology.apps.utilities.HAFileHelper;

import java.io.IOException;
import java.util.List;

public class ChatReceiver implements OnMessageBehavior {

    private String downloadDir;

    public ChatReceiver(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    @Override
    public void onMessageReceived(SyncMessage message, boolean isAuthenticated) {
        SyncRequest request = message.getSyncRequest();

        if (request != null) {
            if (request.getPath().equals(SyncMessagePaths.CHAT_MESSAGE)) {
                System.out.println("Message(From=" + message.getSourceId() + ")\n" + request.getText());

                if (request.getFiles() != null) {

                    try {
                        downloadFiles(request.getFiles());
                    } catch (IOException | BusinessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void downloadFiles(List<SyncFileBase64> files) throws IOException, BusinessException {

        if (downloadDir == null || downloadDir.equals(""))
            throw new BusinessException("Download folder is not defined");

        if ( !downloadDir.endsWith("/"))
            downloadDir = downloadDir + "/";

        for (SyncFileBase64 file: files){
            String filePath = downloadDir + file.getName();

            System.out.println("Downloading " + filePath + "...");
            HAFileHelper.decodeFileFromBase64(file.getEncodedContent(), filePath);
        }
    }
}
