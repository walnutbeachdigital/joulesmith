package com.walnutbeachdigital.joulesmith.machine.creative;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Creative-only: pushes endless power into every block touching it. For testing machines. */
public class CreativePowerCellBlock extends BaseEntityBlock {
    public static final MapCodec<CreativePowerCellBlock> CODEC = simpleCodec(CreativePowerCellBlock::new);

    public CreativePowerCellBlock(Properties properties) {
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
        return new CreativePowerCellBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, JSBlockEntities.CREATIVE_POWER_CELL.get(), CreativePowerCellBlockEntity::serverTick);
    }
}
