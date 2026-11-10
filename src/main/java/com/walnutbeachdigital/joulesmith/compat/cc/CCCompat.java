package com.walnutbeachdigital.joulesmith.compat.cc;

import dan200.computercraft.api.ComputerCraftAPI;

/** ComputerCraft support; only loaded when CC: Tweaked is installed. */
public final class CCCompat {
    public static void register() {
        ComputerCraftAPI.registerGenericSource(new MachinePeripheralMethods());
    }

    private CCCompat() {}
}
