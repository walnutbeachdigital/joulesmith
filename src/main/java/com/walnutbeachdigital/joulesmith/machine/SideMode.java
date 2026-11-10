package com.walnutbeachdigital.joulesmith.machine;

/** What a machine face does with items. Colours match the side configuration overlay. */
public enum SideMode {
    NONE(0x8B8B8B), INPUT(0x3F76E4), OUTPUT(0xE48A3F), BOTH(0x4FB548);

    public final int color;

    SideMode(int color) {
        this.color = color;
    }

    public boolean accepts() { return this == INPUT || this == BOTH; }
    public boolean gives() { return this == OUTPUT || this == BOTH; }

    public SideMode next() { return values()[(ordinal() + 1) % values().length]; }
    public SideMode previous() { return values()[(ordinal() + values().length - 1) % values().length]; }

    public String key() { return "gui.joulesmith.side_mode." + name().toLowerCase(java.util.Locale.ROOT); }
}
