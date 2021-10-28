package com.hatechnology.apps.core_messaging.behaviors;

import com.hatechnology.apps.core_messaging.SyncMessage;

public interface OnMessageBehavior {

    void onMessageReceived(SyncMessage message, boolean isAuthenticated);
}
