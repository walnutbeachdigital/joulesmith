package com.walnutbeachdigital.joulesmith.gear.powered;

import java.lang.reflect.Field;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Servers without allow-flight kick a player who hangs in the air for four seconds. The Jetpack is allowed
 * flight, so while it's thrusting the server's airborne counter is kept at zero.
 */
final class NoFlyKick {
    private static Field counter;
    private static boolean failed;

    static void reset(ServerPlayer player) {
        if (failed || player.connection == null) return;
        try {
            if (counter == null) {
                counter = ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
                counter.setAccessible(true);
            }
            counter.setInt(player.connection, 0);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
        }
    }

    private NoFlyKick() {}
}
