package com.hatechnology.apps.sample;

import com.hatechnology.apps.core_messaging.SyncFileBase64;
import com.hatechnology.apps.core_messaging.SyncMessage;
import com.hatechnology.apps.core_messaging.SyncMessagePaths;
import com.hatechnology.apps.core_messaging.SyncRequest;
import com.hatechnology.apps.utilities.HAFileHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CommonMessages {

    public SyncMessage buildTextMessage(String text){
        SyncRequest request = new SyncRequest(SyncMessagePaths.CHAT_MESSAGE);
        request.setText(text);

        SyncMessage message = new SyncMessage();
        message.setSyncRequest(request);

        return message;
    }

    //I do not recommend to send big files since you system will run into out of memory
    public SyncMessage buildTextMessageWithFiles(String text, File[] files) throws IOException {
        //First lets prepare our files
        List<SyncFileBase64> fileBase64List = new ArrayList<>();
        for (File file: files){
            String fileName = file.getName();

            SyncFileBase64 fileBase64 = new SyncFileBase64();
            fileBase64.setName(fileName);
            fileBase64.setType(HAFileHelper.getFileExtension(fileName));
            fileBase64.setEncodedContent(HAFileHelper.encodeFileToBase64(file));

            fileBase64List.add(fileBase64);
        }

        //Then lets build our request container
        SyncRequest request = new SyncRequest(SyncMessagePaths.CHAT_MESSAGE);
        request.setText(text);
        request.setFiles(fileBase64List);

        //Then our message
        SyncMessage message = new SyncMessage();
        message.setSyncRequest(request);

        return message;
    }
}
