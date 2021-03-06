package com.laputa.server.core.protocol.model.messages;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 */
public abstract class BinaryMessage extends MessageBase {

    private final byte[] data;

    public BinaryMessage(int messageId, short command, byte[] data) {
        super(messageId, command, data.length);
        this.data = data;
    }

    @Override
    public byte[] getBytes() {
        return data;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
