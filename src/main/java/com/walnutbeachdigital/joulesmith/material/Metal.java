package com.walnutbeachdigital.joulesmith.material;

import java.util.Locale;

/**
 * Every metal Joulesmith adds forms for. Vanilla metals (iron, copper, gold) only get the
 * extra forms vanilla lacks; ore metals get ores and raw ore; alloys are made, never mined.
 * Keep this list in step with tools/gen_materials.py, which writes the matching JSON.
 */
public enum Metal {
    //        source         dust  plate gear  wire
    IRON    (Source.VANILLA, true, true, true, false),
    COPPER  (Source.VANILLA, true, true, true, true),
    GOLD    (Source.VANILLA, true, true, true, true),
    TIN     (Source.ORE,     true, true, true, false),
    LEAD    (Source.ORE,     true, true, true, false),
    SILVER  (Source.ORE,     true, true, true, true),
    NICKEL  (Source.ORE,     true, true, true, false),
    URANIUM (Source.ORE,     true, false, false, false),
    STEEL   (Source.ALLOY,   true, true, true, true),
    BRONZE  (Source.ALLOY,   true, true, true, false),
    INVAR   (Source.ALLOY,   true, true, true, false),
    ELECTRUM(Source.ALLOY,   true, true, true, true);

    public enum Source { VANILLA, ORE, ALLOY }

    private final Source source;
    private final boolean dust, plate, gear, wire;

    Metal(Source source, boolean dust, boolean plate, boolean gear, boolean wire) {
        this.source = source;
        this.dust = dust;
        this.plate = plate;
        this.gear = gear;
        this.wire = wire;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Ingot, nugget and storage block are added only where vanilla has none. */
    public boolean hasIngot() { return source != Source.VANILLA; }
    public boolean hasOre() { return source == Source.ORE; }
    public boolean hasDust() { return dust; }
    public boolean hasPlate() { return plate; }
    public boolean hasGear() { return gear; }
    public boolean hasWire() { return wire; }
}
