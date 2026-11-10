package com.walnutbeachdigital.joulesmith.machine;

import net.minecraft.core.Direction;

/** A machine's faces relative to where it points, so side settings turn with the machine. */
public enum MachineFace {
    FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM;

    /** Which relative face a world direction is, for a machine facing {@code facing}. */
    public static MachineFace of(Direction facing, Direction world) {
        if (world == Direction.UP) return TOP;
        if (world == Direction.DOWN) return BOTTOM;
        if (world == facing) return FRONT;
        if (world == facing.getOpposite()) return BACK;
        return world == facing.getClockWise() ? LEFT : RIGHT;
    }

    public String key() { return "gui.joulesmith.face." + name().toLowerCase(java.util.Locale.ROOT); }
}
