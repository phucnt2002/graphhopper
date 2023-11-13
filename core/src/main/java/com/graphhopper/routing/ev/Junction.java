package com.graphhopper.routing.ev;

import com.graphhopper.util.Helper;

public enum Junction {
    ROUNDABOUT, YES, NO, CIRCULAR, SPUI, INTERSECTION, TRAFFIC_SIGNS, OTHER;
    public static final String KEY = "junction";

    public static EnumEncodedValue<Junction> create() {
        return new EnumEncodedValue<>(KEY, Junction.class);
    }

    @Override
    public String toString() {
        return Helper.toLowerCase(super.toString());
    }

    public static Junction find(String name) {
        if (name == null || name.isEmpty())
            return OTHER;

        try {
            return Junction.valueOf(Helper.toUpperCase(name));
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}
