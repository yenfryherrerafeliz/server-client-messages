package com.hatechnology.apps.sample.server;

import com.hatechnology.apps.core_messaging.behaviors.OnMessageBehavior;
import com.hatechnology.apps.sample.CommonMessages;
import com.hatechnology.apps.sample.impl.ChatReceiver;
import com.hatechnology.apps.server_messages.SocketClientService;
import com.hatechnology.apps.server_messages.SocketServerService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ServerMessaging {

    //To pick the sample documents
    private static final String DATA_FOLDER = "sample-server-data";

    private final SocketServerService socketServerService;
    private final CommonMessages commonMessages = new CommonMessages();

    public ServerMessaging(){
        socketServerService = new SocketServerService(1035, buildMyCustomReceivers());
        socketServerService.start();
    }

    public void waitForOneConnection() {
        System.out.println("\rWaiting to get a least one client connected...!");
        while (socketServerService.getmSocketClients().size() == 0){

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Client connected!");
    }

    public void sendTextMessageToAllClients(String text){
        socketServerService.sendMessageToAllClients(commonMessages.buildTextMessage(text));
    }

    public void sendTextMessageWithSharingFilesToAllClients(String text, File[] files) throws IOException {
        socketServerService.sendMessageToAllClients(commonMessages.buildTextMessageWithFiles(text, files));
    }

    private File[] buildTestFiles() {
        File photosDir = new File(DATA_FOLDER + "/photos");
        return photosDir.listFiles();
    }

    public void startChattingWithAllTheClients() {

        System.out.println("Start talking!");

        while (true){
            Scanner scanner = new Scanner(System.in);
            String read = scanner.nextLine();

            //Exit from loop
            if (read.toLowerCase().equals("exit")){
                break;
            }

            sendTextMessageToAllClients(read);
        }
    }

    private List<OnMessageBehavior> buildMyCustomReceivers(){
        List<OnMessageBehavior> mReceivers = new ArrayList<>();
        mReceivers.add(new ChatReceiver(DATA_FOLDER + "/download"));

        return mReceivers;
    }

    public static void main(String... args) {
        ServerMessaging serverMessaging = new ServerMessaging();
        serverMessaging.waitForOneConnection();

        try {
            //Test a simple text message
            serverMessaging.sendTextMessageToAllClients("Hi clients, welcome to the server!");

            //Test a simple text message and files
            serverMessaging.sendTextMessageWithSharingFilesToAllClients("Hi clients, let me share with you some files", serverMessaging.buildTestFiles());

            //Start a simple chat
            serverMessaging.startChattingWithAllTheClients();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
