package com.walnutbeachdigital.joulesmith.material;

import java.util.Locale;

/**
 * Every metal Joulesmith adds forms for. Vanilla metals (iron, copper, gold) only get the
 * extra forms vanilla lacks; ore metals get ores and raw ore; alloys are made, never mined.
 * Keep this list in step with tools/gen_materials.py and the ramps in tools/make_textures.py.
 *
 * {@code ramp} is each metal's five-step colour gradient, darkest to brightest. It colours both
 * Joulesmith's own art (plates, gears, wires, ore spots) and, through
 * {@link com.walnutbeachdigital.joulesmith.client.MetalTexturePack}, the vanilla-shaped forms.
 */
public enum Metal {
    //        source         dust  plate  gear   wire   ramp: outline, dark, mid, light, highlight
    IRON    (Source.VANILLA, true, true,  true,  false, 0x3A3D4A, 0x6E7280, 0xA7ABB5, 0xD6D9DE, 0xFBFBF7),
    COPPER  (Source.VANILLA, true, true,  true,  true,  0x5C1F12, 0xA3401F, 0xDD7034, 0xF6A45B, 0xFFD79A),
    GOLD    (Source.VANILLA, true, true,  true,  true,  0x6E3D05, 0xB8780B, 0xEAB21E, 0xFBDC4F, 0xFFFBB5),
    TIN     (Source.ORE,     true, true,  true,  false, 0x2F3C5A, 0x5B7393, 0x93ACC6, 0xC7D9EA, 0xF3FBFF),
    LEAD    (Source.ORE,     true, true,  true,  false, 0x1B1838, 0x363A6E, 0x535D96, 0x7682B8, 0xA9B5DD),
    SILVER  (Source.ORE,     true, true,  true,  true,  0x34526A, 0x6F97B2, 0xA8CDE0, 0xD5ECF6, 0xFFFFFF),
    NICKEL  (Source.ORE,     true, true,  true,  false, 0x4F4628, 0x8D8458, 0xC3BB8A, 0xE3DCB0, 0xFDF9E0),
    URANIUM (Source.ORE,     true, false, false, false, 0x173416, 0x2F6A22, 0x4FA338, 0x84D656, 0xCAFB92),
    STEEL   (Source.ALLOY,   true, true,  true,  true,  0x22252E, 0x474E5A, 0x6E7783, 0x9AA3AE, 0xCCD4DC),
    BRONZE  (Source.ALLOY,   true, true,  true,  false, 0x5A2A0C, 0x9A5419, 0xD18A2D, 0xEFB352, 0xFFE39A),
    INVAR   (Source.ALLOY,   true, true,  true,  false, 0x333F3C, 0x5F6F69, 0x8FA098, 0xBCCBC3, 0xE9F3EE),
    ELECTRUM(Source.ALLOY,   true, true,  true,  true,  0x5B450A, 0xA88518, 0xDCBD34, 0xF4DD66, 0xFFF9BE);

    public enum Source { VANILLA, ORE, ALLOY }

    private final Source source;
    private final boolean dust, plate, gear, wire;
    private final int[] ramp;

    Metal(Source source, boolean dust, boolean plate, boolean gear, boolean wire, int... ramp) {
        this.source = source;
        this.dust = dust;
        this.plate = plate;
        this.gear = gear;
        this.wire = wire;
        this.ramp = ramp;
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

    /** RGB colour at {@code t} along the ramp, 0 = darkest, 1 = brightest. */
    public int rampColor(double t) {
        double p = Math.max(0, Math.min(1, t)) * (ramp.length - 1);
        int i = Math.min(ramp.length - 2, (int) p);
        double f = p - i;
        int a = ramp[i], b = ramp[i + 1];
        int r = (int) Math.round((a >> 16 & 0xFF) * (1 - f) + (b >> 16 & 0xFF) * f);
        int g = (int) Math.round((a >> 8 & 0xFF) * (1 - f) + (b >> 8 & 0xFF) * f);
        int bl = (int) Math.round((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
        return r << 16 | g << 8 | bl;
    }
}
