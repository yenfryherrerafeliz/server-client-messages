package com.hatechnology.apps.core_messaging.tasks;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class ScheduledTimerTask {
    private final String id;
    private Timer timer;
    private TimerTask timerTask;

    public ScheduledTimerTask() {
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void schedule(Runnable command, LocalDateTime at){
        stop();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                command.run();
            }
        };

        timer = new Timer();
        timer.schedule(timerTask, Date.from(at.atZone(ZoneId.systemDefault()).toInstant()));

        System.out.println("Timer task with id " + getId() + " has been scheduled for " + at.toString() + "!");
    }

    public void stop(){
        if (timerTask != null && timer != null){

            System.out.println("Timer task with id " + getId() + " has been stopped!");

            timerTask.cancel();
            timer.cancel();
            timer.purge();
        }
    }
}
