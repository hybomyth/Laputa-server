package com.laputa.server.core.protocol.model.messages.common;

import com.laputa.server.core.protocol.model.messages.StringMessage;

import static com.laputa.server.core.protocol.enums.Command.*;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 */
public class HardwareMessage extends StringMessage {

    public HardwareMessage(int messageId, String body) {
        super(messageId, HARDWARE, body.length(), body);
    }

    @Override
    public String toString() {
        return "HardwareMessage{" + super.toString() + "}";
    }
}
