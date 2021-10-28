package com.hatechnology.apps.server_messages.impl;

import com.hatechnology.apps.core_messaging.*;
import com.hatechnology.apps.core_messaging.behaviors.OnMessageBehavior;
import com.hatechnology.apps.logger.DefaultLogger;
import com.hatechnology.apps.server_messages.SocketClientService;
import com.hatechnology.apps.server_messages.SocketClientSession;

import java.time.LocalDateTime;

public class AuthenticatorReceiver implements OnMessageBehavior {

    private SocketClientService socketClientService;

    public AuthenticatorReceiver(SocketClientService socketClientService) {
        this.socketClientService = socketClientService;
    }

    @Override
    public void onMessageReceived(SyncMessage message, boolean isAuthenticated) {
        //Get request
        SyncRequest request = message.getSyncRequest();

        //Get response
        SyncResponse response = message.getSyncResponse();

        if (request != null){

            DefaultLogger.logEvent("AuthenticatorReceiver.onMessage", "Getting request(MessageId=" + message.getId() + ", Request=" + request.getPath() + ")", DefaultLogger.DEBUG_LEVEL);

            //Get request path
            String path = request.getPath();

            //Create response to be sent
            SyncResponse mResponse = new SyncResponse();

            //Also the connection testing is included here
            if (path.equals(SyncMessagePaths.TEST_CONNECTION)){
                mResponse.setSuccess(true);

                socketClientService.reply(null, mResponse, message);

                return;
            }

            if ( !isAuthenticated){

                /*
                 * Authenticate the client.
                 * Please use your own logic to authenticate the client.
                 */
                if (path.equals(SyncMessagePaths.AUTHENTICATE)){
                    Authentication auth = request.getAuthentication();

                    //Authentication user
                    if (auth.getUser().equals("admin") && auth.getPassword().equals("admin")){

                        socketClientService.setAuthenticated(true);

                        //Set client id
                        socketClientService.setUniqueId(message.getSourceId());

                        //Reply
                        socketClientService.reply(null, mResponse, message);

                        //Lets say the session start when gets authenticated
                        SocketClientSession clientSession = new SocketClientSession(socketClientService.getmInternalId(), LocalDateTime.now());

                        socketClientService.startSession(clientSession);

                        //Notify the client the authentication was successfully
                        mResponse.setSuccess(true);
                        socketClientService.reply(null, mResponse, message);

                        return;
                    }else{
                        mResponse.setSuccess(false);
                        mResponse.setErrorMessage("Credentials are not correct!");

                        socketClientService.reply(null, mResponse, message);
                    }
                } else {
                    //We also could reply to the client letting it know that is not authenticated
                    /*

                    mResponse = new SyncResponse(false);
                    mResponse.setErrorMessage("Client is not authenticated!");

                    socketClientService.reply(null, mResponse, message);
                    */
                    return;
                }
            }
        }

        if (response != null){
            DefaultLogger.logEvent("AuthenticatorReceiver.onMessage", "Getting response(MessageId=" + message.getId() + ", Response=" + response.getPath() + ")", DefaultLogger.DEBUG_LEVEL);
        }
    }
}
