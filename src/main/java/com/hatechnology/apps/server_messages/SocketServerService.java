package com.hatechnology.apps.server_messages;

import com.hatechnology.apps.core_messaging.behaviors.OnMessageBehavior;
import com.hatechnology.apps.exceptions.BusinessException;
import com.hatechnology.apps.logger.DefaultLogger;
import com.hatechnology.apps.core_messaging.SyncMessage;
import com.hatechnology.apps.server_messages.impl.AuthenticatorReceiver;
import com.hatechnology.apps.utilities.BackgroundProcessHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * This is the implementation that will manage the connections.
 * @author Yenfry Herrera Feliz
 */
public class SocketServerService extends Thread {

    //Default
    private static final int DEFAULT_MAX_CONNECTIONS = 100;
    private static final int DEFAULT_MAX_BYTES_TO_TRANSFER = 16400;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;
    private static final boolean DEFAULT_SECURE_MESSAGES = false;

    //Settings
    private static final int SOCKET_CLIENT_CLEAN_UP_MINUTES = 5;

    //Client list
    private HashMap<String, SocketClientService> mSocketClients = new HashMap<>();
    private ServerSocket mServerSocket;
    private int port;
    private int maxNumberOfConnections;
    private int maxBytesToTransfer;
    private String secureTokenPath;
    private boolean acceptConnections;
    private int timeoutSeconds;
    private List<OnMessageBehavior> receivers;

    public SocketServerService(int port){
        this(port, DEFAULT_MAX_CONNECTIONS, DEFAULT_MAX_BYTES_TO_TRANSFER, DEFAULT_TIMEOUT_SECONDS, DEFAULT_SECURE_MESSAGES, "", null);
    }

    public SocketServerService(int port, List<OnMessageBehavior> receivers){
        this(port, DEFAULT_MAX_CONNECTIONS, DEFAULT_MAX_BYTES_TO_TRANSFER, DEFAULT_TIMEOUT_SECONDS, DEFAULT_SECURE_MESSAGES, "", receivers);
    }

    public SocketServerService(int port, int maxNumberOfConnections, int maxBytesToTransfer, int timeoutSeconds, boolean secureMessages, String secureTokenPath, List<OnMessageBehavior> receivers) {
        this.port = port;
        this.maxNumberOfConnections = maxNumberOfConnections;
        this.maxBytesToTransfer = maxBytesToTransfer;
        this.timeoutSeconds = timeoutSeconds;
        this.secureTokenPath = secureTokenPath;
        this.receivers = receivers;
    }

    public HashMap<String, SocketClientService> getmSocketClients() {
        return mSocketClients;
    }

    private void startServer(){

        try {
            //Start server socket
            mServerSocket = new ServerSocket(port, maxNumberOfConnections);

            DefaultLogger.logEvent("SocketServerService.startServer", "Starting server at port " + port + " on " + LocalDateTime.now().toString(), DefaultLogger.DEBUG_LEVEL);

            //Accept connections
            acceptConnections = true;
            while (acceptConnections){

                Socket socket = mServerSocket.accept();

                DefaultLogger.logEvent("SocketServerService.startServer", "Accepting connection from " + socket.toString(), DefaultLogger.DEBUG_LEVEL);

                SocketClientService socketClientService = new SocketClientService(socket, maxBytesToTransfer, timeoutSeconds,false, secureTokenPath, this);

                //Add the custom receivers
                if (receivers != null) {
                    for (OnMessageBehavior receiver : receivers) {
                        socketClientService.addReceiver(receiver);
                    }
                }

                socketClientService.start();

                mSocketClients.put(socketClientService.getmInternalId(), socketClientService);

                //Schedule a task to check if in 5 minutes the client gets authenticated,
                // if not will be disconnected
                scheduleServerClientsCleanUp(socketClientService);
            }
        } catch (IOException | BusinessException e) {
            e.printStackTrace();
        }
    }

    public void stopServer(){
        try {
            acceptConnections = false;

            //Clean up all the connections
            for (Map.Entry<String, SocketClientService> longSocketClientServiceEntry : mSocketClients.entrySet()) {

                SocketClientService socketClientService = longSocketClientServiceEntry.getValue();

                //Remove the client
                removeSocketClient(socketClientService);
            }

            if ( mServerSocket != null)
                mServerSocket.close();
        } catch (IOException e) {
            DefaultLogger.logEvent("SocketServerService.stopServer", e.getMessage(), DefaultLogger.ERROR_LEVEL);
        }
    }

    public void restartServer(){
        stopServer();
        startServer();
    }

    public void removeSocketClient(SocketClientService socketClientService) {
        if (socketClientService == null)
            return;

        //Do the disconnection
        socketClientService.disconnect(false);

        this.mSocketClients.remove(socketClientService.getmInternalId());
    }

    public void sendMessageToAllClients(SyncMessage syncMessage) {

        for (Map.Entry<String, SocketClientService> entry: mSocketClients.entrySet()){
            SocketClientService socketClientService = entry.getValue();

            BackgroundProcessHelper.startThread(()-> {
                try {
                    socketClientService.sendMessage(syncMessage);
                } catch (BusinessException e) {
                    DefaultLogger.logEvent("SocketServerService.sendMessageToAllClients", e.getMessage(), DefaultLogger.ERROR_LEVEL);
                }
            });
        }
    }

    public boolean sendMessageToClient(String clientIdTo, SyncMessage message) throws BusinessException {

        if (message.isWaitResponse()){
            throw new BusinessException("Not implemented yet!");
        }

        Map.Entry<String, SocketClientService> entry = mSocketClients.entrySet().stream().filter(item->{
            String uniqueId = item.getValue().getUniqueId();

            return uniqueId != null && uniqueId.equals(clientIdTo);
        }).findFirst().orElse(null);

        if (entry != null){
            SocketClientService socketClientService = entry.getValue();
            socketClientService.sendMessage(message);

            return true;
        }

        return false;
    }

    private void scheduleServerClientsCleanUp(SocketClientService socketClientService){
        //This method will check in 5 minutes if is authenticated
        //If is not, so then we will disconnect it

        Timer cleanUpTimer = new Timer();

        Calendar cleanUpDateTime = Calendar.getInstance();

        cleanUpDateTime.setTime(Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));

        cleanUpDateTime.add(Calendar.MINUTE, SOCKET_CLIENT_CLEAN_UP_MINUTES);

        cleanUpTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if ( !socketClientService.isAuthenticated()){
                    DefaultLogger.logEvent("scheduleServerClientsCleanUp", "SocketClientId=" + socketClientService.getmInternalId()  + " will be disconnected due that is not authenticated", DefaultLogger.DEBUG_LEVEL);
                    removeSocketClient(socketClientService);
                }

                //Finish scheduled task
                cleanUpTimer.cancel();
                cleanUpTimer.purge();

                return;
            }
        }, cleanUpDateTime.getTime());
    }

    public void removeSocketClientByProp(Map props, String key) {
        //Close sessions

        for (Map.Entry<String, SocketClientService> socketClientServiceEntry:
                this.mSocketClients.entrySet()){

            SocketClientService clientService = socketClientServiceEntry.getValue();

            Map<String, Object> properties = clientService.getProperties();

            if (properties == null){
                continue;
            }

            if (props.get(key).equals(properties.get(key))){
                this.removeSocketClient(clientService);
            }
        }
    }


    @Override
    public void run(){
        startServer();
    }
}
