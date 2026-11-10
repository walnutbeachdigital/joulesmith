package com.walnutbeachdigital.joulesmith.logic;

import net.minecraft.core.Direction;

/**
 * One Logic Controller rule: "if [measure] on [side] is [below / above] [value], power [output]".
 * Packed into an int so it syncs to the screen cheaply.
 */
public record Rule(boolean enabled, Measure measure, Direction side, boolean above, int value, Direction output) {
    public static final Rule DEFAULT = new Rule(false, Measure.ENERGY, Direction.NORTH, false, 20, Direction.SOUTH);

    public int pack() {
        return (enabled ? 1 : 0) | measure.ordinal() << 1 | side.get3DDataValue() << 4 | (above ? 1 : 0) << 7
                | value << 8 | output.get3DDataValue() << 15;
    }

    public static Rule unpack(int bits) {
        Measure[] measures = Measure.values();
        return new Rule((bits & 1) != 0, measures[Math.min(measures.length - 1, bits >> 1 & 7)],
                Direction.from3DDataValue(bits >> 4 & 7), (bits >> 7 & 1) != 0, bits >> 8 & 127, Direction.from3DDataValue(bits >> 15 & 7));
    }

    /** Whether the rule holds for a reading. */
    public boolean test(int reading) {
        return above ? reading > value : reading < value;
    }

    /** The rule with one field stepped forward or back (fields: 0 on/off, 1 reading, 2 side, 3 below/above, 4 value, 5 output). */
    public Rule step(int field, boolean back) {
        int d = back ? -1 : 1;
        return switch (field) {
            case 0 -> new Rule(!enabled, measure, side, above, value, output);
            case 1 -> {
                Measure m = Measure.values()[Math.floorMod(measure.ordinal() + d, Measure.values().length)];
                yield new Rule(enabled, m, side, above, Math.min(value, m.max), output);
            }
            case 2 -> new Rule(enabled, measure, Direction.from3DDataValue(Math.floorMod(side.get3DDataValue() + d, 6)), above, value, output);
            case 3 -> new Rule(enabled, measure, side, !above, value, output);
            case 4 -> new Rule(enabled, measure, side, above, Math.max(0, Math.min(measure.max, value + d * measure.step)), output);
            case 5 -> new Rule(enabled, measure, side, above, value, Direction.from3DDataValue(Math.floorMod(output.get3DDataValue() + d, 6)));
            default -> this;
        };
    }
}
