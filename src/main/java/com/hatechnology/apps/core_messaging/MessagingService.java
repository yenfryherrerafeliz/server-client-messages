package com.hatechnology.apps.core_messaging;

import com.hatechnology.apps.exceptions.BusinessException;
import com.hatechnology.apps.exceptions.MessageTimeoutException;
import com.hatechnology.apps.logger.DefaultLogger;
import com.hatechnology.apps.core_messaging.behaviors.ForwardMessageBehavior;
import com.hatechnology.apps.core_messaging.behaviors.OnMessageBehavior;
import com.hatechnology.apps.utilities.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This class hold the core logic for managing the messages exchange.
 * Basically what I do is to read from the socket input stream in an independent thread,
 * so that we do not have to send/write to the other socket endpoint, in order to receive data from it.
 * We have some abstract methods that should be implemented, so that the clients can customize the process as they wish.
 * Here I also have a ping process that will guarantee the socket is connected, if this process find that there is not connection,
 * so then the disconnect method will be called. The disconnect method will also call the abstract method onDisconnected.
 * I have some methods to validate the integrity of the messages, but that implementation is not open to the public.
 * The reason why is not open to the public is because I have some customers using my own logic and I need to protect them.
 * @author Yenfry Herrera Feliz
 */
public abstract class MessagingService extends Thread{
    private static final int MAX_ALLOWED_BYTES_TO_TRANSFER = 65535;
    private static final String CONNECTION_TEST_REQUEST = "0000";
    private static final String CONNECTION_TEST_RESPONSE = "1111";
    private static final long SECONDS_TO_DISCONNECT = 30;

    //Internal use only
    private final String mInternalId = UUID.randomUUID().toString();
    protected Socket mSocket;
    private DataInputStream mDataInputStream;
    private DataOutputStream mDataOutputStream;
    private Map<String, SyncMessageQueue> syncMessageQueueList = new HashMap<>();
    private Map<String, SyncMessage> secureDeliveryMessages = new HashMap<>();
    private SyncMessage pingMessage;

    //General use
    private String uniqueId;
    private int maxBytesToTransfer;
    private int timeoutSeconds;
    private boolean secureMessages;
    private String secureTokenPath;
    private String secureToken;
    private final ConnectionCheckService connectionCheckService = new ConnectionCheckService(this);

    //Behaviors
    private List<OnMessageBehavior> receivers = new ArrayList<>();
    private ForwardMessageBehavior forwardMessageBehavior;

    public MessagingService(Socket mSocket, int maxBytesToTransfer, int timeoutSeconds, boolean secureMessages, String secureTokenPath) throws BusinessException {
        this.mSocket = mSocket;
        this.maxBytesToTransfer = maxBytesToTransfer;
        this.timeoutSeconds = timeoutSeconds;
        this.secureMessages = secureMessages;
        this.secureTokenPath = secureTokenPath;

        if (this.mSocket == null)
            throw new BusinessException("Socket can not be null!");
    }

    public MessagingService(int maxBytesToTransfer, int timeoutSeconds, boolean secureMessages, String secureTokenPath) throws BusinessException {
        this.maxBytesToTransfer = maxBytesToTransfer;
        this.timeoutSeconds = timeoutSeconds;
        this.secureMessages = secureMessages;
        this.secureTokenPath = secureTokenPath;

        loadSecuredToken();
    }

    public String getmInternalId() {
        return mInternalId;
    }

    public boolean isSecureMessages() {
        return secureMessages;
    }

    public void setSecureMessages(boolean secureMessages) {
        this.secureMessages = secureMessages;
    }

    /*
     *I recommend to use a unique identifier for each client.
     * So basically that id could be based on a activation code generated, etc.
     */

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public int getMaxBytesToTransfer() {
        return maxBytesToTransfer;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void setMaxBytesToTransfer(int maxBytesToTransfer) {
        this.maxBytesToTransfer = maxBytesToTransfer;
    }

    public String getSecureTokenPath() {
        return secureTokenPath;
    }

    public void setSecureTokenPath(String secureTokenPath) {
        this.secureTokenPath = secureTokenPath;
    }

    public void addReceiver(OnMessageBehavior receiver){
        receivers.add(receiver);
    }

    public ForwardMessageBehavior getForwardMessageBehavior() {
        return forwardMessageBehavior;
    }

    public void setForwardMessageBehavior(ForwardMessageBehavior forwardMessageBehavior) {
        this.forwardMessageBehavior = forwardMessageBehavior;
    }

    protected abstract void onDisconnected();

    protected abstract void onConnected();

    protected abstract void onSessionStarted();

    protected abstract void setTransactionEvent(String event, LocalDateTime eventDateTime);

    public abstract boolean isConnected();

    public abstract boolean isAuthenticated();

    protected abstract String getTemporaryMessagesFolder();

    /**
     * This method send a message. It receives as parameter a message object.
     * @param message
     * @throws BusinessException
     */
    public void sendMessage(SyncMessage message) throws BusinessException {
        if (mSocket != null){
            try {
                if (message == null)
                    throw new BusinessException("Please specify a valid message");

                //Check if is pinging the server, so then we just need to write a byte
                if (message.isPingingServer()){
                    try {

                        this.pingMessage = message;

                        writeUTF(CONNECTION_TEST_REQUEST);
                    } catch (IOException e) {
                        DefaultLogger.logEvent("MessagingService.sendMessage(ClientId=" + getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);
                    }

                    return;
                }

                //Set message properties
                String uniqueId = getUniqueId();

                message.setSourceId(uniqueId);
                message._setDateTime();
                message.setValidationToken(generateValidationToken());

                //
                String writeUTF = JsonUtils.convertToJson(message);

                /*
                * To make the message have a secure delivery.
                **/
                if (message.isSecureDelivery() && !message.isConfirmingDelivery()){

                    if (uniqueId == null || uniqueId.equals("")){
                        throw new BusinessException("Client id must not be null!");
                    }

                    String messageFile = getTemporaryMessagesFolder() + uniqueId + "/SyncMessage." + message.getId() + ".smsg";
                    String encodedMessage = HAStringUtils.encodeStringToBase64(writeUTF);

                    HAFileHelper.createFile(messageFile, encodedMessage);


                    secureDeliveryMessages.put(message.getId(), message);
                }

                if (maxBytesToTransfer > MAX_ALLOWED_BYTES_TO_TRANSFER)
                    throw new BusinessException("The amount of bytes allowed to transfer are " + MAX_ALLOWED_BYTES_TO_TRANSFER + " and you are trying " + maxBytesToTransfer + ". \nPlease fix this in order to have the messaging service working properly.");

                int bytesLength = writeUTF.getBytes().length;

                if (bytesLength > maxBytesToTransfer){

                    int messagesCount = bytesLength / maxBytesToTransfer;

                    if (bytesLength % maxBytesToTransfer != 0)
                        messagesCount = messagesCount + 1;

                    //Sending the message to prepare the receiver
                    //to receive the message by separated parts
                    SyncMessage splitSyncMessage = new SyncMessage();
                    splitSyncMessage.setSplitMessage(true);
                    splitSyncMessage.setMessageParts(messagesCount);

                    String writeSplitMessage = JsonUtils.convertToJson(splitSyncMessage);

                    writeUTF(writeSplitMessage);

                    // ** //

                    String[] strParts = new String[messagesCount];

                    for (int i = 0; i < messagesCount; i++){
                        int begin = (i * maxBytesToTransfer);
                        int end = begin + (maxBytesToTransfer);

                        if (end > bytesLength)
                            end = bytesLength;

                        strParts[i] = writeUTF.substring(begin , end);
                    }

                    for (String str: strParts) {
                        writeUTF(str);
                    }
                }else{
                    //Else send the complete message without dividing it
                    writeUTF(writeUTF);
                }

            } catch (IOException e) {

                DefaultLogger.logEvent("MessagingService.sendMessage(ClientId=" + getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);

                disconnect(true);
            }

        }
    }

    private synchronized void writeUTF(String content) throws IOException {
        this.mDataOutputStream.writeUTF(content);
        this.mDataOutputStream.flush();
    }
    /**
     * Here we send a message and we wait for a response.
     * If we do not get a response in the specified timeout, so then a exception will be thrown.
     * @param message
     * @param reconnectOnTimeOut
     * @param customTimeoutSeconds
     * @return
     * @throws BusinessException
     * @throws MessageTimeoutException
     */
    public SyncMessage sendRequestMessage(SyncMessage message,
                                          boolean reconnectOnTimeOut,
                                          int customTimeoutSeconds) throws BusinessException, MessageTimeoutException {

        //Place the message into the queue
        SyncMessageQueue newMessageQueue = addNewMessageToTheQueue(message);

        //Send the message
        sendMessage(message);

        //Message must wait for a response
        message.setWaitResponse(true);

        try {
            waitUntilMyQueueMessageIsReceived(newMessageQueue, customTimeoutSeconds);
        } catch (MessageTimeoutException e) {

            DefaultLogger.logEvent("MessagingService.sendRequestMessage(ClientId=" + getmInternalId() + ", MessageId=" + message.getId() +")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);

            if (reconnectOnTimeOut){
                //Disconnected
                disconnect(true);
            }

            throw new MessageTimeoutException(e.getMessage());
        }

        return newMessageQueue.getSyncMessage();
    }

    private SyncMessageQueue addNewMessageToTheQueue(SyncMessage message) {

        SyncMessageQueue syncMessageQueue = new SyncMessageQueue();
        syncMessageQueue.setId(message.getId());
        syncMessageQueue.setSyncMessage(message);
        syncMessageQueue.setSent(true);

        syncMessageQueueList.put(message.getId(), syncMessageQueue);

        return syncMessageQueue;
    }

    private void setResponseToMessageQueue(String messageId, SyncResponse response){

        SyncMessageQueue messageQueue = syncMessageQueueList.get(messageId);

        if (messageQueue != null){
            messageQueue.setResponded(true);
            SyncMessage message = messageQueue.getSyncMessage();
            message.setSyncResponse(response);

            syncMessageQueueList.remove(messageId);
        }
    }

    private void setMessageDelivered(String messageId){
        SyncMessage message = secureDeliveryMessages.remove(messageId);

        if (message != null){
            releaseTempMessage(messageId);
        }
    }

    /**
     * This method wait a response for messages that requires it.
     * @param syncMessageQueue
     * @param customWaitingSeconds
     * @throws MessageTimeoutException
     */
    private void waitUntilMyQueueMessageIsReceived(SyncMessageQueue syncMessageQueue, int customWaitingSeconds) throws MessageTimeoutException {
        //Wait for a response
        int millisecondsAwaiting = 0;
        int timeoutSeconds = getTimeoutSeconds();

        if (customWaitingSeconds != 0)
            timeoutSeconds = customWaitingSeconds;

        while (isConnected() && !syncMessageQueue.isResponded()){

            long seconds = millisecondsAwaiting / 1000L;

            if ( seconds > timeoutSeconds){
                throw new MessageTimeoutException("Time exceeded for a response. DefinedTimeout=" + getTimeoutSeconds());
            }else{
                try {
                    TimeUnit.MILLISECONDS.sleep(1);
                    millisecondsAwaiting++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //If a connection problem occurred
        if ( !syncMessageQueue.isResponded())
        {
            SyncResponse syncResponse = new SyncResponse();
            syncResponse.setSuccess(false);
            syncResponse.setErrorMessage("Not connection available");

            syncMessageQueue.getSyncMessage().setSyncResponse(syncResponse);
        }

    }

    /**
     * This is the message listener.
     * We basically started reading our socket input stream in a thread(background).
     */
    protected void startListener(){

        BackgroundProcessHelper.startThread(()->{

            if (mSocket != null){
                try{
                    this.mDataInputStream = new DataInputStream(mSocket.getInputStream());
                    this.mDataOutputStream = new DataOutputStream(mSocket.getOutputStream());

                    mSocket.setKeepAlive(true);

                    connectionCheckService.start();

                    while (true){

                        String readUTF = readUTF();

                        String firstFourthChars = readUTF.substring(0, 4);

                        //If this result is equal to 1, so then this means a ping was done from the client
                        if (firstFourthChars.equals(CONNECTION_TEST_REQUEST)){
                            //Send the answer back to confirm the connection
                            writeUTF(CONNECTION_TEST_RESPONSE);

                            continue;
                        }

                        //We got a successfully ping response
                        if (firstFourthChars.equals(CONNECTION_TEST_RESPONSE)){

                            setResponseToMessageQueue(pingMessage.getId(), new SyncResponse(true));

                            continue;
                        }

                        SyncMessage syncMessage = (SyncMessage) JsonUtils.convertFromJsonToObject(readUTF, SyncMessage.class);

                        String clientSourceId = syncMessage.getSourceId();

                        //DefaultLogger.logEvent("MessagingService.startListener", "Getting a message. (Message=" + readUTF+")", DefaultLogger.DEBUG_LEVEL);

                        String validationToken = syncMessage.getValidationToken();

                        if ( !isTrustedMessage(clientSourceId, validationToken)){

                            replyNotTrustedMessage(syncMessage);

                            continue;
                        }

                        if (syncMessage.isSplitMessage()){

                            StringBuilder builder = new StringBuilder();

                            int messagesCount = syncMessage.getMessageParts();

                            for (int count = 0; count < messagesCount; count++){
                                readUTF = readUTF();

                                //DefaultLogger.logEvent("MessagingService.startListener", "{MessageId: " + syncMessage.getId() +", MessageNumber: " + (count) + ", MessageContent: " + readUTF + "}", DefaultLogger.DEBUG_LEVEL);

                                builder.append(readUTF);
                            }

                            syncMessage = (SyncMessage) JsonUtils.convertFromJsonToObject(builder.toString(), SyncMessage.class);
                        }

                        //Check if the message will be forward
                        String messageTo = syncMessage.getTo();
                        if ( messageTo != null && !messageTo.equals("")){

                            if (forwardMessageBehavior == null){
                                SyncResponse response = new SyncResponse();
                                response.setSuccess(false);
                                response.setErrorMessage("Forwarding message is not implemented for this client!");

                                reply(null, response, syncMessage);
                                continue;
                            }

                            boolean sent = forwardMessageBehavior.forwardMessage(messageTo, syncMessage);

                            if ( !sent)
                                continue;

                            //To avoid onMessage behavior
                            syncMessage.setWaitResponse(true);
                        }

                        String messageId = syncMessage.getId();

                        //Confirm that a previous message sent was delivered
                        if (syncMessage.isConfirmingDelivery()){
                            setMessageDelivered(messageId);
                            continue;
                        }

                        if (messageId == null)
                            messageId = "";

                        setResponseToMessageQueue(messageId, syncMessage.getSyncResponse());

                        //Set last transaction date
                        setTransactionEvent("Message received", LocalDateTime.now());

                        //if ( !syncMessage.isWaitResponse()) {
                            SyncMessage finalSyncMessage = syncMessage;
                            BackgroundProcessHelper.startThread(()->{

                                Object[] currentReceivers = receivers.toArray();

                                for (Object object: currentReceivers){
                                    OnMessageBehavior receiver = (OnMessageBehavior) object;

                                    receiver.onMessageReceived(finalSyncMessage, isAuthenticated());
                                }
                            });
                        //}

                        //Notify the sender that a message was received/delivered
                        if (syncMessage.isSecureDelivery()){
                            confirmDelivery(syncMessage);
                        }
                    }
                }catch (Exception e){
                    DefaultLogger.logEvent("MessagingService.startListener(ClientId=" + getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);

                    disconnect(true);
                }
            }
        });
    }

    private String readUTF() throws IOException {
        return mDataInputStream.readUTF();
    }

    /**
     * This method will check the integrity of each received message.
     * @param sourceId
     * @param validationToken
     * @return
     */
    private boolean isTrustedMessage(String sourceId, String validationToken){
        if ( !isSecureMessages())
            return true;

        //TODO: Please add your own logic or use not secure messages

        return false;
    }

    private void replyNotTrustedMessage(SyncMessage syncMessage) throws BusinessException {
        SyncResponse syncResponse = new SyncResponse();
        syncResponse.setSuccess(false);
        syncResponse.setErrorMessage("The integrity of this message is not trusted. \n This could be either or because Client not match server identity or the token is not valid.");

        syncMessage.setSyncResponse(syncResponse);
        syncMessage.setSyncRequest(null);

        sendMessage(syncMessage);
    }

    private void loadSecuredToken() throws BusinessException {

        if ( !isSecureMessages()){
            return;
        }

        try {
            String token = HAFileHelper.getFileContent(this.secureTokenPath);
            this.secureToken = HAStringUtils.decodeBase64ToString(token);
        } catch (IOException e) {
            DefaultLogger.logEvent("MessagingService.loadSecuredToken(ClientId=" + getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);

            throw new BusinessException("Secure token could not be load!");
        }
    }

    private String generateValidationToken(){
        //TODO: Please add your own logic
        return "";
    }

    /**
     * This method will load and send the messages that were not confirmed as delivered.
     * We should call this method when an Id is assigned to the client.
     */
    protected void loadAndSendPendingMessages(){
        BackgroundProcessHelper.startThread(()->{

            String uniqueId = getUniqueId();

            if (uniqueId == null || uniqueId.equals("")){
                try {
                    throw new BusinessException("Client id is null!");
                } catch (BusinessException e) {
                    DefaultLogger.logEvent("MessagingService.loadAndSendPendingMessages(ClientId=" + getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);

                    return;
                }
            }

            File[] messageFiles = HAFileHelper.listFilesFromDir(getTemporaryMessagesFolder() + uniqueId + "/");

            if (messageFiles == null)
                return;

            for (File messageFile: messageFiles){
                try {
                    String strMessage = HAFileHelper.getFileContent(messageFile);
                    strMessage = HAStringUtils.decodeBase64ToString(strMessage);

                    SyncMessage message = (SyncMessage) JsonUtils.convertFromJsonToObject(strMessage, SyncMessage.class);

                    sendMessage(message);

                } catch (IOException | BusinessException e) {
                    DefaultLogger.logEvent("MessagingService.loadAndSendPendingMessages(ClientId=" + getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);
                }
            }
        });
    }

    private void releaseTempMessage(String messageId){
        String messageFile = getTemporaryMessagesFolder() + getUniqueId() + "/SyncMessage." + messageId + ".smsg";

        HAFileHelper.deleteFile(messageFile);
    }

    public void disconnect(boolean callOnDisconnected){

        if (mSocket != null){
            try {
                mSocket.close();
            } catch (IOException e) {
                DefaultLogger.logEvent("MessagingService.disconnect(ClientId=" + getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);
            }
        }

        if (callOnDisconnected) {
            onDisconnected();
        }
    }

    @Override
    public void run(){
        startListener();
    }

    /**
     * This method is used to reply back
     * @param request
     * @param response
     * @param message
     */
    public void reply(SyncRequest request, SyncResponse response, SyncMessage message){
        message.setSyncRequest(request);
        message.setSyncResponse(response);

        try {
            sendMessage(message);
        } catch (BusinessException e) {
            DefaultLogger.logEvent("MessagingService.reply(ClientId=" + getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);
        }
    }

    private void confirmDelivery(SyncMessage message){
        SyncMessage confirmingMessage = new SyncMessage(message);
        confirmingMessage.setSyncResponse(null);
        confirmingMessage.setSyncRequest(null);
        confirmingMessage.setConfirmingDelivery(true);

        reply(null, null, confirmingMessage);
    }
}
