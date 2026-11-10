package com.walnutbeachdigital.joulesmith.material;

import java.util.Locale;

/**
 * Every metal Joulesmith adds forms for. Vanilla metals (iron, copper, gold) only get the
 * extra forms vanilla lacks; ore metals get ores and raw ore; alloys are made, never mined.
 * Keep this list in step with tools/gen_materials.py and the ramps in tools/make_textures.py.
 *
 * {@code ramp} is each metal's eight-step colour ramp, darkest to brightest. It colours both
 * Joulesmith's own art (plates, gears, wires, ore spots) and, through
 * {@link com.walnutbeachdigital.joulesmith.client.MetalTexturePack}, the vanilla-shaped forms.
 */
public enum Metal {
    //        source         dust  plate  gear   wire   ramp: eight steps, darkest to brightest
    IRON    (Source.VANILLA, true, true,  true,  false, 0x4A4A52, 0x5E5F68, 0x76777F, 0x8D8E96, 0xA8A9B0, 0xC4C5CB, 0xDCDDE1, 0xF4F4F6),
    COPPER  (Source.VANILLA, true, true,  true,  true,  0x772C15, 0xA0511F, 0xB8622A, 0xCE6E26, 0xDA7E2A, 0xF0A030, 0xFCBE2D, 0xFFE08A),
    GOLD    (Source.VANILLA, true, true,  true,  true,  0x8A5A12, 0xA06D14, 0xB8841A, 0xCF9C22, 0xE3B52E, 0xF1CD45, 0xFBE36E, 0xFFF4A8),
    TIN     (Source.ORE,     true, true,  true,  false, 0x314F6F, 0x476F81, 0x517C88, 0x6C8998, 0x88A2A7, 0x90AFAC, 0xA1C6C2, 0xBFD8DD),
    LEAD    (Source.ORE,     true, true,  true,  false, 0x13142F, 0x232457, 0x2F3061, 0x393C61, 0x454967, 0x575E79, 0x727989, 0x7D97A6),
    SILVER  (Source.ORE,     true, true,  true,  true,  0x2A3345, 0x3C4859, 0x4D5C6A, 0x647380, 0x758392, 0x9FADB4, 0xBECCD2, 0xFFFFFF),
    NICKEL  (Source.ORE,     true, true,  true,  false, 0x734C3B, 0x866444, 0x997857, 0xA28960, 0xB0A075, 0xC7B784, 0xE7D498, 0xF9F5AB),
    URANIUM (Source.ORE,     true, false, false, false, 0x1F3D17, 0x2C5520, 0x3B6E28, 0x4C8A31, 0x62A63C, 0x7FC24C, 0xA2DB66, 0xC8F08E),
    STEEL   (Source.ALLOY,   true, true,  true,  true,  0x22262E, 0x30353F, 0x3E4450, 0x4D5461, 0x5F6774, 0x767E8B, 0x939AA6, 0xB8BEC8),
    BRONZE  (Source.ALLOY,   true, true,  true,  false, 0x6A3412, 0x82441A, 0x9B5622, 0xB46A2C, 0xCA7F37, 0xDE9A48, 0xEEB764, 0xF9D58D),
    INVAR   (Source.ALLOY,   true, true,  true,  false, 0x4A666A, 0x5C7C7B, 0x768885, 0x859792, 0xA3B1A8, 0xC5CCC3, 0xD7DED9, 0xE1E7E5),
    ELECTRUM(Source.ALLOY,   true, true,  true,  true,  0x957C3D, 0xA19144, 0xBCA54F, 0xC9B255, 0xD9C25F, 0xE9DA75, 0xF8F796, 0xFBFBBC);

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

    /** Number of steps in the ramp. */
    public int rampSize() { return ramp.length; }

    /** RGB colour of ramp step {@code i}, 0 = darkest. */
    public int rampStep(int i) { return ramp[Math.max(0, Math.min(ramp.length - 1, i))]; }

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
