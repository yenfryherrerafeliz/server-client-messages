package com.hatechnology.apps.core_messaging.asynchronous_list;

public interface OnItemIteration<T> {

    /**
     * @param item The item of the list
     * @return When a true values is returned so, that means the iteration should be stopped.
     */
    boolean action(T item);
}
