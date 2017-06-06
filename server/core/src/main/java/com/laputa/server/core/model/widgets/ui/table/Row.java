package com.laputa.server.core.model.widgets.ui.table;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 07.09.16.
 */
public class Row {

    public int id;

    public volatile String name;

    public volatile String value;

    public boolean isSelected;

    public Row() {
    }

    public Row(int id, String name, String value) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.isSelected = true;
    }

    public void update(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
