package com.hatechnology.apps.core_messaging.behaviors;

import com.hatechnology.apps.core_messaging.SyncMessage;

public interface ForwardMessageBehavior {

    boolean forwardMessage(String clientIdTo, SyncMessage message);
}
