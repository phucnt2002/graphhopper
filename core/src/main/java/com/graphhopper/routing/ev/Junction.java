package com.graphhopper.routing.ev;

import com.graphhopper.util.Helper;

public enum Junction {
    ROUNDABOUT,YES, CIRCULAR, JUGHANDLE, APPPROACH, OTHER;
    public static final String KEY = "junction";
    public static EnumEncodedValue<Junction> create() {
        return new EnumEncodedValue<>(Junction.KEY, Junction.class);
    }
    @Override
    public String toString() {
        return Helper.toLowerCase(super.toString());
    }
    public static Junction find(String name) {
        if (name == null)
            return OTHER;
        try {
            return Junction.valueOf(Helper.toUpperCase(name));
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}
