package com.project3.AssetFlow.streaming.handler;

import org.springframework.context.ApplicationEvent;

public class FinnhubDisconnectedEvent extends ApplicationEvent {

    public FinnhubDisconnectedEvent(Object event) {
        super(event);
    }
}
