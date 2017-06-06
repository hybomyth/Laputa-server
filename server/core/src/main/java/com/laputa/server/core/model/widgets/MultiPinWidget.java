package com.laputa.server.core.model.widgets;

import com.laputa.server.core.model.Pin;
import com.laputa.server.core.model.enums.PinType;

import java.util.StringJoiner;

import static com.laputa.utils.StringUtils.BODY_SEPARATOR;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 02.11.15.
 */
public abstract class MultiPinWidget extends Widget implements AppSyncWidget {

    public int deviceId;

    public Pin[] pins;

    @Override
    public boolean updateIfSame(int deviceId, byte pinIn, PinType type, String value) {
        boolean isSame = false;
        if (this.pins != null && this.deviceId == deviceId) {
            for (Pin pin : this.pins) {
                if (pin.isSame(pinIn, type)) {
                    pin.value = value;
                    isSame = true;
                }
            }
        }
        return isSame;
    }

    @Override
    public void updateIfSame(Widget widget) {
        if (widget instanceof MultiPinWidget) {
            MultiPinWidget multiPinWidget = (MultiPinWidget) widget;
            if (multiPinWidget.pins != null && multiPinWidget.deviceId == this.deviceId) {
                for (Pin pin : multiPinWidget.pins) {
                    updateIfSame(multiPinWidget.deviceId, pin.pin, pin.pinType, pin.value);
                }
            }
        }
    }

    @Override
    public boolean isSame(int deviceId, byte pinIn, PinType pinType) {
        if (pins != null && this.deviceId == deviceId) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, pinType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public abstract boolean isSplitMode();

    public String makeHardwareBody(byte pinIn, PinType pinType) {
        if (pins == null) {
            return null;
        }
        if (isSplitMode()) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, pinType)) {
                    return pin.makeHardwareBody();
                }
            }
        } else {
            if (pins[0].notEmpty()) {
                StringBuilder sb = new StringBuilder(pins[0].makeHardwareBody());
                for (int i = 1; i < pins.length; i++) {
                    sb.append(BODY_SEPARATOR).append(pins[i].value);
                }
                return sb.toString();
            }
        }
        return null;
    }

    @Override
    public String getValue(byte pinIn, PinType pinType) {
        if (pins != null) {
            for (Pin pin : pins) {
                if (pin.isSame(pinIn, pinType)) {
                    return pin.value;
                }
            }
        }
        return null;
    }

    @Override
    public void append(StringBuilder sb, int deviceId) {
        if (pins != null && this.deviceId == deviceId) {
            for (Pin pin : pins) {
                append(sb, pin.pin, pin.pinType, getModeType());
            }
        }
    }

    @Override
    public String getJsonValue() {
        if (pins == null) {
            return "[]";
        }
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (Pin pin : pins) {
            if (pin.value == null) {
                sj.add("\"\"");
            } else {
                sj.add("\"" + pin.value + "\"");
            }
        }
        return sj.toString();
    }

}
