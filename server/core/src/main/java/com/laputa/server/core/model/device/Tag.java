package com.laputa.server.core.model.device;

import com.laputa.server.core.model.widgets.Target;
import com.laputa.utils.ArrayUtil;
import com.laputa.utils.JsonParser;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 16.11.16.
 */
public class Tag implements Target {

    public static final int START_TAG_ID = 100_000;

    public int id;

    public volatile String name;

    public volatile int[] deviceIds = ArrayUtil.EMPTY_INTS;

    public boolean isNotValid() {
        return name == null || name.isEmpty() || name.length() > 40 || id < START_TAG_ID || deviceIds.length > 100;
    }

    public Tag() {
    }

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    private Tag(int id, String name, int[] deviceIds) {
        this.id = id;
        this.name = name;
        this.deviceIds = deviceIds;
    }

    @Override
    public int[] getDeviceIds() {
        return deviceIds;
    }

    @Override
    public int getDeviceId() {
        return deviceIds[0];
    }

    @Override
    public boolean isTag() {
        return true;
    }

    public void update(Tag tag) {
        this.name = tag.name;
        this.deviceIds = tag.deviceIds;
    }

    public Tag copy() {
        return new Tag(id, name, deviceIds);
    }

    @Override
    public String toString() {
        return JsonParser.toJson(this);
    }
}
