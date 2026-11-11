package com.walnutbeachdigital.joulesmith.transport.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.transport.PipeBlock;
import com.walnutbeachdigital.joulesmith.transport.PipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** A fluid pipe: tier 1 moves 100 mB/t, tier 2 400 and tier 3 1,600 at each connection. */
public class FluidPipeBlock extends PipeBlock {
    public static final MapCodec<FluidPipeBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(FluidPipeBlock::tier),
            propertiesCodec()
    ).apply(i, FluidPipeBlock::new));

    public FluidPipeBlock(int tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean sameFamily(BlockState other) {
        return other.getBlock() instanceof FluidPipeBlock;
    }

    @Override
    protected boolean hasTarget(Level level, BlockPos other, Direction face) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, other, face) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, JSBlockEntities.FLUID_PIPE.get(), PipeBlockEntity::serverTick);
    }
}
