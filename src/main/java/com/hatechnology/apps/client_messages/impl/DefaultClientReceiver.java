package com.hatechnology.apps.client_messages.impl;

import com.hatechnology.apps.client_messages.AsyncSocketClient;
import com.hatechnology.apps.core_messaging.SyncMessage;
import com.hatechnology.apps.core_messaging.SyncMessagePaths;
import com.hatechnology.apps.core_messaging.SyncRequest;
import com.hatechnology.apps.core_messaging.SyncResponse;
import com.hatechnology.apps.core_messaging.behaviors.OnMessageBehavior;
import com.hatechnology.apps.logger.DefaultLogger;

public class DefaultClientReceiver implements OnMessageBehavior {

    private AsyncSocketClient socketClient;

    public DefaultClientReceiver(AsyncSocketClient socketClient) {
        this.socketClient = socketClient;
    }

    @Override
    public void onMessageReceived(SyncMessage message, boolean isAuthenticated) {
        //Get request
        SyncRequest request = message.getSyncRequest();

        //Get response
        SyncResponse response = message.getSyncResponse();

        if (request != null){
            DefaultLogger.logEvent("DefaultClientReceiver.onMessage", "Getting request(MessageId="+ message.getId() + ", Request=" + request.getPath() + ")", DefaultLogger.DEBUG_LEVEL);

            String path = request.getPath();
            SyncResponse mResponse = new SyncResponse();

            switch (path){
                case SyncMessagePaths.REQUEST_CLIENT_ID:
                    //With sending back a success true the client id will be assigned based on the message source id property.
                    mResponse.setSuccess(true);
                    break;
                default:
                    return;
            }


            this.socketClient.reply(null, mResponse, message);
        }

        if (response != null){
            DefaultLogger.logEvent("AsyncSocketClient.onMessage", "Getting response(MessageId="+ message.getId() + ", Response=" + response.getPath() + ")", DefaultLogger.DEBUG_LEVEL);
        }
    }
}
