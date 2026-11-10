package com.walnutbeachdigital.joulesmith.machine.reactor;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A Reactor Port: part of a Fission Reactor's shell that pipes, ducts and cables connect to. */
public class ReactorPortBlock extends BaseEntityBlock {
    public static final MapCodec<ReactorPortBlock> CODEC = simpleCodec(ReactorPortBlock::new);

    public ReactorPortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorPortBlockEntity(pos, state);
    }
}
