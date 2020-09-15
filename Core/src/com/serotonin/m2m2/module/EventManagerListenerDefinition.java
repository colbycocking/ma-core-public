/*
    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
    @author Matthew Lohbihler
 */
package com.serotonin.m2m2.module;

import com.serotonin.m2m2.i18n.TranslatableMessage;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.rt.event.type.EventType;

/**
 * Provides a hook for modules to "listen" for things that happen in the event manager.
 *
 * @author Matthew Lohbihler
 * @author Jared Wiltshire
 */
abstract public class EventManagerListenerDefinition extends ModuleElementDefinition {
    /**
     * Provides both notification of a new event, and the opportunity to automatically acknowledge it.
     *
     * @param eventType
     *            the type of event being raised.
     * @return the message with which to acknowledge the event, or null if it should not be acknowledged.
     */
    public TranslatableMessage autoAckEventWithMessage(EventType eventType) {
        return null;
    }


    /**
     * Provides a hook where the event instance can be modified by returning a new instance.
     * Can also be used to drop or ignore an event.
     *
     * This runs after the duplicate/recent checks but before the event is saved to the database.
     *
     * @param event
     * @return the new event or null to ignore/drop the event completely
     */
    public EventInstance modifyEvent(EventInstance event) {
        return event;
    }

}
