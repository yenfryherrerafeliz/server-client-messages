package com.hatechnology.apps.server_messages;


import com.hatechnology.apps.exceptions.BusinessException;
import com.hatechnology.apps.logger.DefaultLogger;
import com.hatechnology.apps.core_messaging.*;
import com.hatechnology.apps.server_messages.impl.AuthenticatorReceiver;

import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * This implementation should be done at the server side.
 * Basically this implementation represents each client at the server side,
 * which means that this is the end point to exchange the messages.
 * @author Yenfry Herrera Feliz
 */
public class SocketClientService extends MessagingService {

    private Map properties = new HashMap();
    private SocketServerService socketServerService;
    private boolean behaviorsAreSet;
    private boolean authenticated;
    private SocketClientSession clientSession;

    /**
     *
     * @param mSocket The client socket created when the server receives an incoming connection.
     * @param maxBytesToTransfer The max numbers of bytes to be transferred when sending data through the socket output stream.
     * @param timeoutSeconds The amount of seconds to timeout when sending messages that waits for answers.
     * @param secureMessages If true, each message will be checked for integrity.
     * @param secureTokenPath The token to validate the messages.
     * @param socketServerService The instance for the server that manages the connections.
     * @throws BusinessException
     */
    public SocketClientService(Socket mSocket, int maxBytesToTransfer, int timeoutSeconds, boolean secureMessages, String secureTokenPath, SocketServerService socketServerService) throws BusinessException {
        super(mSocket, maxBytesToTransfer, timeoutSeconds, secureMessages, secureTokenPath);

        setBehaviors();

        this.socketServerService = socketServerService;
    }

    public Map getProperties() {
        return properties;
    }

    public void setProperties(Map properties) {
        this.properties = properties;
    }

    public SocketClientSession getClientSession() {
        return clientSession;
    }

    public void startSession(SocketClientSession clientSession) {
        this.clientSession = clientSession;

        onSessionStarted();
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    private void setBehaviors(){

        if (behaviorsAreSet)
            return;

        //Add the authenticator receiver per default
        addReceiver(new AuthenticatorReceiver(this));

        //Set forward message behavior
        setForwardMessageBehavior((clientIdTo, message) -> {
            try {
                return socketServerService.sendMessageToClient(clientIdTo, message);
            } catch (BusinessException e) {
                SyncResponse response = new SyncResponse(false);
                response.setErrorMessage(e.getMessage());

                reply(null, response, message);

                return false;
            }
        });

        behaviorsAreSet = true;
    }

    @Override
    protected void onDisconnected() {
        this.socketServerService.removeSocketClient(this);
    }

    @Override
    protected void onConnected() {

    }

    @Override
    protected void onSessionStarted() {
        //Load and send the pending messages
        loadAndSendPendingMessages();
    }

    @Override
    protected void setTransactionEvent(String event, LocalDateTime eventDateTime) {

        if (isAuthenticated()){
            SocketClientSession socketClientSession = getClientSession();

            if (socketClientSession != null){
                socketClientSession.setLastTransactionDate(eventDateTime);
            }
        }
    }

    /**
     * On server side, this should always return true.
     * This is because, when a connection is done is because the client is fully connected.
     * @return boolean.
     */
    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    protected String getTemporaryMessagesFolder() {
        return System.getProperty("user.dir") + "/server-messages/";
    }
}
