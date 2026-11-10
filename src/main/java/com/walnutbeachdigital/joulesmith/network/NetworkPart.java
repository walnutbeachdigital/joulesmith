package com.walnutbeachdigital.joulesmith.network;

import com.walnutbeachdigital.joulesmith.transport.PipeMode;
import com.walnutbeachdigital.joulesmith.transport.conduit.BundleBlock;
import com.walnutbeachdigital.joulesmith.transport.conduit.BundleBlockEntity;
import com.walnutbeachdigital.joulesmith.transport.conduit.Conduit;
import com.walnutbeachdigital.joulesmith.transport.conduit.ConduitType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that is part of a storage network: network cables join every NetworkPart they touch into one
 * network. A conduit bundle is one only while it holds a network cable.
 */
public interface NetworkPart {
    static boolean at(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NetworkPart)) return false;
        return !(state.getBlock() instanceof BundleBlock) || BundleBlockEntity.conduit(level, pos, ConduitType.NETWORK) != null;
    }

    /** Whether the storage network passes from {@code pos} to its neighbour on side {@code dir}. */
    static boolean joins(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        if (!at(level, pos) || !at(level, other)) return false;
        Conduit a = BundleBlockEntity.conduit(level, pos, ConduitType.NETWORK);
        Conduit b = BundleBlockEntity.conduit(level, other, ConduitType.NETWORK);
        if (a != null && a.mode(dir) == PipeMode.DISABLED) return false;
        return b == null || b.mode(dir.getOpposite()) != PipeMode.DISABLED;
    }
}
