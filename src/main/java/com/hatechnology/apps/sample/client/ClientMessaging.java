package com.hatechnology.apps.sample.client;

import com.hatechnology.apps.client_messages.AsyncSocketClient;
import com.hatechnology.apps.core_messaging.*;
import com.hatechnology.apps.core_messaging.behaviors.OnMessageBehavior;
import com.hatechnology.apps.exceptions.BusinessException;
import com.hatechnology.apps.exceptions.MessageTimeoutException;
import com.hatechnology.apps.sample.CommonMessages;
import com.hatechnology.apps.sample.impl.ChatReceiver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ClientMessaging {

    private static final String CLIENT_ID = "b4e8d4bc-360d-11ec-9840-64006afadd13";

    //To pick the sample documents
    private static final String DATA_FOLDER = "sample-client-data";

    private final MessagingService messagingService;
    private final CommonMessages commonMessages = new CommonMessages();

    public ClientMessaging() throws BusinessException {
        messagingService = new AsyncSocketClient(CLIENT_ID, getMyHosts(), buildMyCustomReceivers());
        messagingService.start();
    }

    public void waitToBeConnected(){
        System.out.println("\rWaiting to get connected to the server...!");
        while ( !messagingService.isConnected()){

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Client connected!");
    }

    public void sendTextMessage(String text) throws BusinessException {
        messagingService.sendMessage(commonMessages.buildTextMessage(text));
    }

    public void sendTextMessageWithSharingFiles(String text, File[] files) throws IOException, BusinessException {
        //And send it
        messagingService.sendMessage(commonMessages.buildTextMessageWithFiles(text, files));
    }

    public void sendMessageAndWaitForAnAnswer() throws MessageTimeoutException, BusinessException {
        SyncRequest request = new SyncRequest(SyncMessagePaths.TEST_CONNECTION);

        SyncMessage message = new SyncMessage();
        message.setSyncRequest(request);

        message = messagingService.sendRequestMessage(message, true, 0);

        //Get the response
        SyncResponse response = message.getSyncResponse();
        System.out.println("Response(Success=" + (response.isSuccess()) + ", Message=" + response.getErrorMessage() + ")");
    }

    public void startChatting() throws BusinessException {

        System.out.println("Start talking!");

        while (true){

            Scanner scanner = new Scanner(System.in);
            String read = scanner.nextLine();

            //Exit from loop
            if (read.toLowerCase().equals("exit")){
                break;
            }

            sendTextMessage(read);
        }
    }

    private List<SocketHost> getMyHosts(){
        List<SocketHost> hosts = new ArrayList<>();
        hosts.add(new SocketHost("localhost", 1035));
        //hosts.add(new SocketHost("localhost", 1034));

        return hosts;
    }

    private File[] buildTestFiles() {
        File photosDir = new File(DATA_FOLDER + "/photos");
        return photosDir.listFiles();
    }


    private List<OnMessageBehavior> buildMyCustomReceivers(){
        List<OnMessageBehavior> mReceivers = new ArrayList<>();
        mReceivers.add(new ChatReceiver(DATA_FOLDER + "/download"));

        return mReceivers;
    }

    public static void main(String[] args){
        try {
            ClientMessaging messaging = new ClientMessaging();
            messaging.waitToBeConnected();

            //Test a message that includes some files
            messaging.sendTextMessageWithSharingFiles("Hi, I am sharing my pictures with you!", messaging.buildTestFiles());

            //Test a message that wait for a response
            messaging.sendMessageAndWaitForAnAnswer();

            //Test a simple chat
            messaging.startChatting();

        } catch (BusinessException | IOException | MessageTimeoutException e) {
            e.printStackTrace();
        }

    }
}
