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
    IRON    (Source.VANILLA, true, true,  true,  false, 0x1D2028, 0x4B505C, 0x8C919C, 0xCDD1D8, 0xFFFFFF),
    COPPER  (Source.VANILLA, true, true,  true,  true,  0x4A120A, 0x8F2B14, 0xD0561F, 0xF39A45, 0xFFE2A8),
    GOLD    (Source.VANILLA, true, true,  true,  true,  0x5A2C02, 0xA76408, 0xE7A817, 0xFCDF55, 0xFFFBD0),
    TIN     (Source.ORE,     true, true,  true,  false, 0x1E3442, 0x3F6878, 0x7FA6B0, 0xB7D4D8, 0xEFFCFC),
    LEAD    (Source.ORE,     true, true,  true,  false, 0x08081F, 0x161B48, 0x2E3A78, 0x5866A6, 0xA0ADDD),
    SILVER  (Source.ORE,     true, true,  true,  true,  0x1B2836, 0x45596D, 0x8597A9, 0xC6D3DE, 0xFFFFFF),
    NICKEL  (Source.ORE,     true, true,  true,  false, 0x3E3020, 0x7A6648, 0xB49F78, 0xDCCAA3, 0xFFF6DC),
    URANIUM (Source.ORE,     true, false, false, false, 0x0F2A0E, 0x25601B, 0x47A02F, 0x86D855, 0xD8FFA8),
    STEEL   (Source.ALLOY,   true, true,  true,  true,  0x15181E, 0x363D48, 0x626C79, 0x97A1AD, 0xD9E0E8),
    BRONZE  (Source.ALLOY,   true, true,  true,  false, 0x4B1F0A, 0x8A4418, 0xC9802F, 0xECB865, 0xFFF0C8),
    INVAR   (Source.ALLOY,   true, true,  true,  false, 0x2A3332, 0x5A6866, 0x94A3A0, 0xC6D1CF, 0xF4F9F8),
    ELECTRUM(Source.ALLOY,   true, true,  true,  true,  0x5A4304, 0xA78812, 0xDCC033, 0xF6E47A, 0xFFFDE0);

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
