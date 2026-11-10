package com.walnutbeachdigital.joulesmith.machine;

/** What a machine slot is for: what automation may insert or take, and how it is drawn. */
public enum SlotRole {
    INPUT, OUTPUT, UPGRADE,
    /** Holds a copy of an item to show what is wanted (a recipe pattern); never a real item. */
    GHOST
}
