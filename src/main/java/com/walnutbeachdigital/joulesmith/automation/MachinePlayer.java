package com.walnutbeachdigital.joulesmith.automation;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * The stand-in player that Joulesmith machines act as when they need one (using items, attacking).
 * Shows as "[Joulesmith]" in logs and protection mods.
 */
public final class MachinePlayer {
    private static final GameProfile PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("joulesmith:machine".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "[Joulesmith]");

    /** The machine player, standing in the machine at {@code pos} and looking out of its {@code facing} side. */
    public static FakePlayer at(ServerLevel level, BlockPos pos, Direction facing) {
        FakePlayer player = FakePlayerFactory.get(level, PROFILE);
        // eyes at the middle of the machine, looking straight out of its front
        player.moveTo(pos.getX() + 0.5, pos.getY() + 0.5 - player.getEyeHeight(), pos.getZ() + 0.5, facing.toYRot(), 0);
        player.setYHeadRot(facing.toYRot());
        return player;
    }

    private MachinePlayer() {}
}
