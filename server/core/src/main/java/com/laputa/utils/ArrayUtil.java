package com.laputa.utils;

import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.App;
import com.laputa.server.core.model.device.Device;
import com.laputa.server.core.model.device.Tag;
import com.laputa.server.core.model.widgets.Widget;

import java.lang.reflect.Array;

/**
 * The Laputa Project.
 * Created by Sommer
 * Created on 04.01.16.
 */
public class ArrayUtil {

    public static final int[] EMPTY_INTS = {};
    public static final DashBoard[] EMPTY_DASHBOARDS = {};
    public static final Tag[] EMPTY_TAGS = {};
    public static final Device[] EMPTY_DEVICES = {};
    public static final Widget[] EMPTY_WIDGETS = {};
    public static final byte[] EMPTY_BYTES = {};
    public static final App[] EMPTY_APPS = {};

    public static <T> T[] add(T[] array, T element, Class<T> type) {
        T[] newArray = copyArrayGrow1(array, type);
        newArray[newArray.length - 1] = element;
        return newArray;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] copyArrayGrow1(final T[] array, Class<T> type) {
        T[] newArray = (T[]) Array.newInstance(type, array.length + 1);
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] remove(final T[] array, final int index, Class<T> type) {
        final T[] result = (T[]) Array.newInstance(type, array.length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        }

        return result;
    }

    public static boolean contains(final int[] ar, final int val) {
        for (final int arVal : ar) {
            if (arVal == val) {
                return true;
            }
        }
        return false;
    }

}
