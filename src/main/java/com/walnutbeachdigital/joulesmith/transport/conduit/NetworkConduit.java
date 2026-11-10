package com.walnutbeachdigital.joulesmith.transport.conduit;

import com.walnutbeachdigital.joulesmith.network.NetworkPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * A network cable: joins storage network blocks together. It has no network object of its own; the storage
 * network is found by walking every joined part (see {@link com.walnutbeachdigital.joulesmith.network.Network}).
 */
public class NetworkConduit extends Conduit {
    public NetworkConduit() {
        super(ConduitType.NETWORK, 1);
    }

    @Override
    protected boolean hasTarget(Level level, BlockPos other, Direction face) {
        return level.getBlockState(other).getBlock() instanceof NetworkPart && !(level.getBlockState(other).getBlock() instanceof BundleBlock);
    }

    @Override
    public void invalidateNetwork() {
    }

    @Override
    public void tick(Level level) {
    }
}
