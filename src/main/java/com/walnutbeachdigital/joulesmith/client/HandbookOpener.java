package com.walnutbeachdigital.joulesmith.client;

import net.minecraft.client.Minecraft;

/** Opens the Handbook screen (client only; the item calls this on the client side). */
public final class HandbookOpener {
    public static void open() {
        Minecraft.getInstance().setScreen(new HandbookScreen());
    }

    private HandbookOpener() {}
}
