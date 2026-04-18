package com.bx.announcementGUI.model;

public enum TargetType {
    LOCAL,
    SERVER,
    SERVERS,
    GROUP,
    GLOBAL;

    public boolean requiresTargets() {
        return this == SERVER || this == SERVERS || this == GROUP;
    }

    public TargetType next() {
        TargetType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
