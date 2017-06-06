package com.laputa.server.notifications.push.android;

import com.laputa.server.notifications.push.GCMMessage;
import com.laputa.server.notifications.push.enums.Priority;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 26.06.15.
 */
public class AndroidGCMMessage implements GCMMessage {

    private static final ObjectWriter writer = new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .writerFor(AndroidGCMMessage.class);
    private final String to;
    private final Priority priority;
    private final GCMData data;

    public AndroidGCMMessage(String to, Priority priority, String message, int dashId) {
        this.to = to;
        this.priority = priority;
        this.data = new GCMData(message, dashId);
    }

    @Override
    public String getToken() {
        return to;
    }

    @Override
    public String toJson() throws JsonProcessingException {
        return writer.writeValueAsString(this);
    }

}
