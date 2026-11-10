package com.walnutbeachdigital.joulesmith.transport;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
 * How a pipe looks on one side: nothing, an arm joining another pipe, or an arm ending in a plate on a
 * block it pushes into, or in a servo on a block it pulls from.
 */
public enum PipeConnection implements StringRepresentable {
    NONE, PIPE, PUSH, PULL;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
