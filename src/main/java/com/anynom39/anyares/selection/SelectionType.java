package com.anynom39.anyares.selection;

import org.jetbrains.annotations.Nullable;

public enum SelectionType {
    CUBOID("Cuboid"),
    SPHERE("Sphere"),
    CYLINDER("Cylinder"),
    POLYGON("Polygon"),
    ELLIPSOID("Ellipsoid");

    public static final boolean PYRAMID = true;
    private final String displayName;

    SelectionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public static SelectionType fromString(String name) {
        if (name == null) return null;
        for (SelectionType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.getDisplayName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}