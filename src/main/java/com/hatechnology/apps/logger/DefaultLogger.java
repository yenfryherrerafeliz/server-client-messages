package com.hatechnology.apps.logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class DefaultLogger {
    public static final String DEBUG_LEVEL = "Debug";
    public static final String ERROR_LEVEL = "Error";
    private static final String loggerFileName = "DefaultLogger.log";

    public static synchronized void logEvent(String source, String event, String level) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(loggerFileName, true));
            String log = "{'Source': '" + source + "', 'Event': '" + event + "', 'Level': '" + level + "', 'EventDateTime': '" + LocalDateTime.now().toString() + "', 'Thread': '" + Thread.currentThread().getId() + "'}";
            writer.println(log);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
