package com.hatechnology.apps.core_messaging;

import com.hatechnology.apps.exceptions.BusinessException;
import com.hatechnology.apps.exceptions.MessageTimeoutException;
import com.hatechnology.apps.logger.DefaultLogger;
import com.hatechnology.apps.utilities.HAGeneralUtils;

import java.util.concurrent.TimeUnit;

public class ConnectionCheckService extends Thread {
    private static final int SECONDS_TO_PING = 30;

    private MessagingService messagingService;
    private volatile boolean active;

    public ConnectionCheckService(MessagingService messagingService){
        this.messagingService = messagingService;
    }

    public void stopService(){
        active = false;
    }


    public void startService(){
        active = true;
        checkConnection();
    }

    /**
     * This method will check if there is connection with the client/server endpoint.
     * For this we send a byte and we will receive a byte as response to confirm there is connection.
     * If there is not connection so then a disconnection will occurs.
     */
    private void checkConnection(){
        while (active){

            //Just if it is connected we will check connection
            if (messagingService.isConnected()) {

                SyncMessage message = SyncMessage.createPingMessage();

                try {
                    //DefaultLogger.logEvent("ConnectionCheckService.checkConnection(ClientId=" + this.messagingService.getmInternalId() + ")", "Pinging endpoint...", DefaultLogger.DEBUG_LEVEL);

                    messagingService.sendRequestMessage(message, true, 0);

                    //DefaultLogger.logEvent("ConnectionCheckService.checkConnection(ClientId=" + this.messagingService.getmInternalId() + ")", message.getSyncResponse().isSuccess() ? "Ping successfully!" : "Ping not successfully!", DefaultLogger.DEBUG_LEVEL);

                } catch (BusinessException | MessageTimeoutException e) {
                    DefaultLogger.logEvent("ConnectionCheckService.checkConnection(ClientId=" + this.messagingService.getmInternalId() + ")", HAGeneralUtils.getStackTrace(e), DefaultLogger.ERROR_LEVEL);
                }
            }

            try {
                TimeUnit.SECONDS.sleep(SECONDS_TO_PING);
            } catch (InterruptedException ignored) {

            }
        }
    }

    @Override
    public void run() {
        startService();
    }
}
