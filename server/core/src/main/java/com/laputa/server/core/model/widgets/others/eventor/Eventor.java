package com.laputa.server.core.model.widgets.others.eventor;

import com.laputa.server.core.model.enums.PinType;
import com.laputa.server.core.model.widgets.NoPinWidget;
import com.laputa.server.core.model.widgets.Widget;
import com.laputa.server.core.model.widgets.others.eventor.model.action.BaseAction;
import com.laputa.server.core.model.widgets.others.eventor.model.action.SetPinAction;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 01.08.16.
 */
public class Eventor extends NoPinWidget {

    public Rule[] rules;

    public int deviceId;

    public Eventor() {
        this.width = 2;
        this.height = 1;
    }

    public Eventor(Rule[] rules) {
        this();
        this.rules = rules;
    }

    @Override
    public boolean updateIfSame(int deviceId, byte pin, PinType type, String value) {
        return false;
    }

    @Override
    public void updateIfSame(Widget widget) {
        //do nothing
    }

    @Override
    public boolean isSame(int deviceId, byte pin, PinType type) {
        return false;
    }

    @Override
    public String getJsonValue() {
        return null;
    }

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public String getValue(byte pin, PinType type) {
        return null;
    }

    @Override
    public void append(StringBuilder sb, int deviceId) {
        if (rules != null && this.deviceId == deviceId) {
            for (Rule rule : rules) {
                if (rule.actions != null) {
                    for (BaseAction action : rule.actions) {
                        if (action instanceof SetPinAction) {
                            SetPinAction setPinActionAction = (SetPinAction) action;
                            if (setPinActionAction.pin != null) {
                                append(sb, setPinActionAction.pin.pin, setPinActionAction.pin.pinType, getModeType());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getPrice() {
        return 500;
    }
}
