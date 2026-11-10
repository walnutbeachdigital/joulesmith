package com.walnutbeachdigital.joulesmith.transport;

import java.util.Locale;

/** Where an item duct's pulling side sends items first. Set by sneaking with the wrench. */
public enum RoutingMode {
    ROUND_ROBIN, NEAREST, FURTHEST;

    public RoutingMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String key() {
        return "message.joulesmith.routing." + name().toLowerCase(Locale.ROOT);
    }
}
