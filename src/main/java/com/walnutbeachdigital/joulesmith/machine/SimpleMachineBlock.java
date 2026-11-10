package com.walnutbeachdigital.joulesmith.machine;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine block that needs nothing beyond the shared machine behaviour: give it the block entity type
 * and constructor and it handles placement, ticking and its codec.
 */
public class SimpleMachineBlock<T extends MachineBlockEntity> extends MachineBlock {
    private final Supplier<BlockEntityType<T>> type;
    private final BiFunction<BlockPos, BlockState, T> factory;
    private final MapCodec<SimpleMachineBlock<T>> codec;

    public SimpleMachineBlock(Properties properties, Supplier<BlockEntityType<T>> type, BiFunction<BlockPos, BlockState, T> factory) {
        super(properties);
        this.type = type;
        this.factory = factory;
        this.codec = simpleCodec(p -> new SimpleMachineBlock<>(p, type, factory));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, type.get(), MachineBlockEntity::serverTick);
    }
}
