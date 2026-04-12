package com.project3.AssetFlow.streaming.events;

import org.springframework.context.ApplicationEvent;

public class FinnhubDisconnectedEvent extends ApplicationEvent {

    public FinnhubDisconnectedEvent(Object event) {
        super(event);
    }
}
