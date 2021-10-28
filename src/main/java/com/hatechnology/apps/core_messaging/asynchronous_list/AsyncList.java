package com.hatechnology.apps.core_messaging.asynchronous_list;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class AsyncList<T> extends ArrayList<T> {
    public static final String LIST_ADD_ITEM = "addItem";
    public static final String LIST_REMOVE_ITEM = "removeItem";
    public static final String LIST_ITERATE = "iterate";

    private volatile boolean somethingInProgress;
    /**
     * With this we avoid the Concurrent Modification Exception
     * @param item The item to be manupilicated if needed
     * @param predicate The predicate to remove the items if needed
     * @param operation The operation to be done.
     * @param actionOnIteration When iterating over the items.
     * @return return true if the operation was successfully.
     */

    public synchronized boolean asyncOperation(T item, Predicate<T> predicate, String operation, OnItemIteration<T> actionOnIteration){
        while (somethingInProgress){
            try {
                System.out.println("Something in progress...");
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignored) {

            }
        }

        somethingInProgress = true;

        boolean returnValue = false;

        switch (operation) {
            case LIST_ADD_ITEM:
                add(item);

                returnValue = true;
                break;
            case LIST_REMOVE_ITEM:

                if (predicate != null) {
                    returnValue = removeIf(predicate);
                }else{
                    returnValue = remove(item);
                }

                break;
            case LIST_ITERATE:
                for (T iterItem : this) {
                    boolean breakSequence = actionOnIteration.action(iterItem);

                    if (breakSequence){
                        break;
                    }
                }

                returnValue = true;
                break;
        }

        somethingInProgress = false;

        return returnValue;
    }
}
