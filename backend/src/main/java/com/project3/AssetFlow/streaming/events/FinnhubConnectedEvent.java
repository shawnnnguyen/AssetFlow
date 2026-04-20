package com.project3.AssetFlow.streaming.events;

import org.springframework.context.ApplicationEvent;

public class FinnhubConnectedEvent extends ApplicationEvent {

    public FinnhubConnectedEvent(Object event) {
        super(event);
    }
}
