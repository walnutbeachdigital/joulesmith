package com.walnutbeachdigital.joulesmith.material;

import java.util.Locale;

/**
 * Every metal Joulesmith adds forms for. Vanilla metals (iron, copper, gold) only get the
 * extra forms vanilla lacks; ore metals get ores and raw ore; alloys are made, never mined.
 * Keep this list in step with tools/gen_materials.py, which writes the matching JSON.
 *
 * {@code tint} colours Minecraft's own iron ingot, nugget, raw iron, iron block, raw iron block
 * and sugar textures so each metal keeps vanilla's exact shapes.
 */
public enum Metal {
    //        source         dust  plate  gear   wire   tint
    IRON    (Source.VANILLA, true, true,  true,  false, 0xE8E8EF),
    COPPER  (Source.VANILLA, true, true,  true,  true,  0xFFA465),
    GOLD    (Source.VANILLA, true, true,  true,  true,  0xFFEF51),
    TIN     (Source.ORE,     true, true,  true,  false, 0xDEF1FF),
    LEAD    (Source.ORE,     true, true,  true,  false, 0x8C96D1),
    SILVER  (Source.ORE,     true, true,  true,  true,  0xF0FFFF),
    NICKEL  (Source.ORE,     true, true,  true,  false, 0xFDF9C7),
    URANIUM (Source.ORE,     true, false, false, false, 0x9BF060),
    STEEL   (Source.ALLOY,   true, true,  true,  true,  0xABB5C1),
    BRONZE  (Source.ALLOY,   true, true,  true,  false, 0xFFBC57),
    INVAR   (Source.ALLOY,   true, true,  true,  false, 0xD1E0D8),
    ELECTRUM(Source.ALLOY,   true, true,  true,  true,  0xFFF371);

    public enum Source { VANILLA, ORE, ALLOY }

    private final Source source;
    private final boolean dust, plate, gear, wire;
    private final int tint;

    Metal(Source source, boolean dust, boolean plate, boolean gear, boolean wire, int tint) {
        this.source = source;
        this.dust = dust;
        this.plate = plate;
        this.gear = gear;
        this.wire = wire;
        this.tint = tint;
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

    /** Opaque ARGB colour multiplied over the vanilla-shaped textures. */
    public int tint() { return 0xFF000000 | tint; }

    /**
     * Tint for raw ore and raw blocks. Vanilla's raw iron is a warm brown, so green and blue are
     * boosted to cancel that warmth, then the colour is scaled back into range keeping its hue.
     */
    public int rawTint() {
        double r = (tint >> 16 & 0xFF), g = (tint >> 8 & 0xFF) * 1.17, b = (tint & 0xFF) * 1.42;
        double scale = Math.min(1.0, 255.0 / Math.max(r, Math.max(g, b)));
        return 0xFF000000 | (int) Math.round(r * scale) << 16 | (int) Math.round(g * scale) << 8 | (int) Math.round(b * scale);
    }
}
