package com.walnutbeachdigital.joulesmith.transport;

import java.util.Locale;

/**
 * What a pipe does on one side, set with the wrench. On a side touching a block: push into it, pull out of
 * it, both, or off. On a side touching another pipe only "off" matters: it splits the two networks.
 */
public enum PipeMode {
    PUSH, PULL, BOTH, DISABLED;

    public boolean pushes() { return this == PUSH || this == BOTH; }
    public boolean pulls() { return this == PULL || this == BOTH; }

    public PipeMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** How a side in this mode is drawn when it touches a block. */
    public PipeConnection look() {
        return switch (this) {
            case PUSH -> PipeConnection.PUSH;
            case PULL, BOTH -> PipeConnection.PULL;
            case DISABLED -> PipeConnection.NONE;
        };
    }

    public String key() {
        return "message.joulesmith.pipe.mode." + name().toLowerCase(Locale.ROOT);
    }
}
