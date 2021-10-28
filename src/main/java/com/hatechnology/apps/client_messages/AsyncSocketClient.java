package com.hatechnology.apps.client_messages;

import com.hatechnology.apps.client_messages.impl.DefaultClientReceiver;
import com.hatechnology.apps.core_messaging.behaviors.OnMessageBehavior;
import com.hatechnology.apps.exceptions.BusinessException;
import com.hatechnology.apps.exceptions.ConnectionNotAvailableException;
import com.hatechnology.apps.exceptions.MessageTimeoutException;
import com.hatechnology.apps.logger.DefaultLogger;
import com.hatechnology.apps.core_messaging.*;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class should be implemented at the client/station side.
 * Basically this implementation keeps a persistent connection.
 * Which means that if the connection fails, so then I will try to reconnect until I get connected.
 * This class receives a list of hosts to connect, which allow you to have load balancing. Example, lets say you
 * have more than one server up to handle the connections, so if the current server fails, then I will try to connect
 * to the next host provided in the list.
 *
 * @author Yenfry Herrera Feliz
 */
public class AsyncSocketClient extends MessagingService{

    //Default values
    private static final boolean DEFAULT_KEEP_CONNECTION_ALIVE = true;
    private static final int DEFAULT_SECONDS_TO_RECONNECT = 10;
    private static final int DEFAULT_MAX_BYTES_TO_TRANSFER = 16400;
    private static final int DEFAULT_TIMEOUT_SECONDS = 60 * 5;
    private static final boolean DEFAULT_SECURE_MESSAGES = false;

    private int currentHostIndex;
    private List<SocketHost> hostList;
    private boolean connected;
    private volatile boolean reconnecting;
    private boolean connect;
    private boolean onLineMode;
    private boolean connectionStarted;
    private boolean keepConnectionAlive;
    private int secondsToReconnect;
    private List<OnMessageBehavior> receivers;

    /**
     *
     * @param clientId This paramenter is the unique identifier for this client/station.
     * @param hostList The hosts to connect the clients
     * @param keepConnectionAlive To reconnect if disconnected
     * @param secondsToReconnect The amount of seconds to wait to retry the reconnection
     * @param maxBytesToTransfer The number of bytes to be transfered when sending through the socket output stream.
     * @param timeoutSeconds The amount seconds to wait for a response
     * @param secureMessages If true, each message will be checked for integrity.
     * @param secureTokenPath The token to validate the messages.
     * @throws BusinessException
     */
    public AsyncSocketClient(String clientId, List<SocketHost> hostList, boolean keepConnectionAlive, int secondsToReconnect, int maxBytesToTransfer, int timeoutSeconds, boolean secureMessages, String secureTokenPath, List<OnMessageBehavior> receivers) throws BusinessException {
        super(maxBytesToTransfer, timeoutSeconds, secureMessages, secureTokenPath);
        this.hostList = hostList;
        this.keepConnectionAlive = keepConnectionAlive;
        this.secondsToReconnect = secondsToReconnect;
        this.receivers = receivers;

        this.setUniqueId(clientId);
    }

    public AsyncSocketClient(String clientId, List<SocketHost> hostList) throws BusinessException {
        this(clientId, hostList, DEFAULT_KEEP_CONNECTION_ALIVE, DEFAULT_SECONDS_TO_RECONNECT, DEFAULT_MAX_BYTES_TO_TRANSFER, DEFAULT_TIMEOUT_SECONDS, DEFAULT_SECURE_MESSAGES, "", null);
    }

    public AsyncSocketClient(String clientId, List<SocketHost> hostList, List<OnMessageBehavior> receivers) throws BusinessException {
        this(clientId, hostList, DEFAULT_KEEP_CONNECTION_ALIVE, DEFAULT_SECONDS_TO_RECONNECT, DEFAULT_MAX_BYTES_TO_TRANSFER, DEFAULT_TIMEOUT_SECONDS, DEFAULT_SECURE_MESSAGES, "", receivers);
    }

    public int getCurrentHostIndex() {
        return currentHostIndex;
    }

    public void setCurrentHostIndex(int currentHostIndex) {
        this.currentHostIndex = currentHostIndex;
    }

    public List<SocketHost> getHostList() {
        return hostList;
    }

    public void setHostList(List<SocketHost> hostList) {
        this.hostList = hostList;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isReconnecting() {
        return reconnecting;
    }

    public void setReconnecting(boolean reconnecting) {
        this.reconnecting = reconnecting;
    }

    public boolean isConnect() {
        return connect;
    }

    public void setConnect(boolean connect) {
        this.connect = connect;
    }

    public boolean isOnLineMode() {
        return onLineMode;
    }

    public void setOnLineMode(boolean onLineMode) {
        this.onLineMode = onLineMode;
    }

    public boolean isKeepConnectionAlive() {
        return keepConnectionAlive;
    }

    public void setKeepConnectionAlive(boolean keepConnectionAlive) {
        this.keepConnectionAlive = keepConnectionAlive;
    }

    public int getSecondsToReconnect() {
        return secondsToReconnect;
    }

    public void setSecondsToReconnect(int secondsToReconnect) {
        this.secondsToReconnect = secondsToReconnect;
    }

    private void startSocketClientConnection() throws BusinessException, IOException, ConnectionNotAvailableException {


        DefaultLogger.logEvent("AsyncSocketClient.startSocketClientConnection", "Starting socket connection with server", DefaultLogger.DEBUG_LEVEL);

        if ( !onLineMode)
            throw new BusinessException("Offline mode is enabled");

        disconnect(false);

        /*if (hostList.size() == 0)
            throw new BusinessException(Strings.NOT_HOST_PROVIDED_TO_CONNECT);*/

        SocketHost socketHost = getCurrentHost();

        try {
            mSocket = new Socket(socketHost.getHost(), socketHost.getPort());

            //Listener for the incoming messages
            startListener();

            //Set reconnecting
            setReconnecting(false);

            //Set connected status
            setConnected(true);

            //Call on connected method
            onConnected();

        } catch (IOException e) {
            moveToOtherSocketHost();
            //Set reconnecting
            setReconnecting(false);

            //Try the reconnection
            reconnectSocketClient(e, "startSocketClientConnection");
        }
    }

    private SocketHost getCurrentHost() throws BusinessException {

        if (hostList.size() == 0)
            throw new BusinessException("Not host available to connect!");

        return getHostList().get(currentHostIndex);
    }

    private void moveToOtherSocketHost(){
        int hostListSize = hostList.size();

        //Just one socket host available
        if (hostListSize == 1)
        {
            currentHostIndex = 0;
            return;
        }

        currentHostIndex++;
        hostListSize = hostListSize - 1;

        if (currentHostIndex > hostListSize)
            currentHostIndex = 0;
    }

    private void reconnectSocketClient(Throwable throwable, String requester) throws IOException, ConnectionNotAvailableException, BusinessException {

        if (isReconnecting())
            return;

        if ( !isKeepConnectionAlive())
            throw new IOException(throwable);

        setReconnecting(true);

        DefaultLogger.logEvent("AsyncSocketClient.reconnectSocketClient", "Reconnection requested by " + requester, DefaultLogger.DEBUG_LEVEL);

        setConnected(false);

        try {
            TimeUnit.SECONDS.sleep(secondsToReconnect);
        } catch (InterruptedException e) {
            DefaultLogger.logEvent("AsyncSocketClient.reconnectSocketClient", e.getMessage(), DefaultLogger.ERROR_LEVEL);
        }

        startSocketClientConnection();
    }

    protected void setBehaviors(){

        //Set the default receiver
        this.addReceiver(new DefaultClientReceiver(this));

        //Add the custom receivers
        if (this.receivers != null){
            for (OnMessageBehavior receiver: this.receivers){
                addReceiver(receiver);
            }
        }
    }

    @Override
    protected void onDisconnected() {
        try {
            reconnectSocketClient(null, "AsyncSocketClient.onDisconnected");
        } catch (IOException | ConnectionNotAvailableException | BusinessException e) {
            DefaultLogger.logEvent("AsyncSocketClient.onDisconnected", e.getMessage(), DefaultLogger.ERROR_LEVEL);
        }
    }

    @Override
    protected void onConnected() {
        try {
            authenticateClient();

            onSessionStarted();

        } catch (BusinessException | ConnectionNotAvailableException | MessageTimeoutException e) {
            DefaultLogger.logEvent("AsyncSocketClient.onConnected", e.getMessage(), DefaultLogger.ERROR_LEVEL);
        }
    }

    @Override
    protected void onSessionStarted() {
        loadAndSendPendingMessages();
    }

    @Override
    protected void setTransactionEvent(String event, LocalDateTime eventDateTime) {

    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isAuthenticated() {
        //Default is true since we do not need authentication for this implementation
        return true;
    }

    @Override
    protected String getTemporaryMessagesFolder() {
        return System.getProperty("user.dir") + "/client-messages/";
    }

    @Override
    public void run(){
        if ( connectionStarted)
            return;

        try {

            //Set the current host
            currentHostIndex = 0;

            setOnLineMode(true);

            setBehaviors();

            startSocketClientConnection();

        } catch (IOException | BusinessException | ConnectionNotAvailableException e) {
            DefaultLogger.logEvent("AsyncSocketClient.connectSocketClientToServer", e.getMessage(), DefaultLogger.ERROR_LEVEL);
        }
    }

    private void authenticateClient() throws BusinessException, ConnectionNotAvailableException, MessageTimeoutException {
        Authentication authentication = new Authentication();
        authentication.setUser("admin");
        authentication.setPassword("admin");

        SyncRequest request = new SyncRequest(SyncMessagePaths.AUTHENTICATE);
        request.setAuthentication(authentication);

        SyncMessage message = new SyncMessage();
        message.setSyncRequest(request);

        message = sendRequestMessage(message, false, 0);

        SyncResponse response = message.getSyncResponse();

        if ( !response.isSuccess()){
            System.out.println("Client is not authenticated!!\n" + (response.getErrorMessage()));
        }
    }
}
