package com.walnutbeachdigital.joulesmith.gear.powered;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLEnvironment;

/** Whether the local player is holding jump (the Jetpack's thrust). Movement is the client's, so this is read there. */
final class JetpackInput {
    static boolean thrusting(Player player) {
        return FMLEnvironment.dist.isClient() && Client.jumpHeld(player);
    }

    private static final class Client {
        static boolean jumpHeld(Player player) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            return player == mc.player && mc.options.keyJump.isDown();
        }
    }

    private JetpackInput() {}
}
