package com.walnutbeachdigital.joulesmith.machine;

/** One machine slot: where it sits in the screen and what it is for. {@code big} draws a larger output frame. */
public record SlotDef(int x, int y, SlotRole role, boolean big) {
    public static SlotDef input(int x, int y) { return new SlotDef(x, y, SlotRole.INPUT, false); }
    public static SlotDef output(int x, int y) { return new SlotDef(x, y, SlotRole.OUTPUT, true); }
}
