package com.hatechnology.apps.utilities;

import java.util.ArrayList;
import java.util.List;

public class BackgroundProcessHelper {

    public static void startThread(Runnable task){
        Thread thread = new Thread(task);
        thread.start();
    }
}
