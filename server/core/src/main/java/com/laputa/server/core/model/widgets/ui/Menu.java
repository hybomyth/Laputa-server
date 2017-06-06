package com.laputa.server.core.model.widgets.ui;

import com.laputa.server.core.model.widgets.OnePinWidget;
import com.laputa.utils.StringUtils;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 28.03.16.
 */
public class Menu extends OnePinWidget {

    public volatile String[] labels;

    public String hint;

    @Override
    public String getModeType() {
        return "out";
    }

    @Override
    public int getPrice() {
        return 400;
    }

    @Override
    public void setProperty(String property, String propertyValue) {
        switch (property) {
            case "labels" :
                this.labels = propertyValue.split(StringUtils.BODY_SEPARATOR_STRING);
                break;
            default:
                super.setProperty(property, propertyValue);
                break;
        }
    }
}
