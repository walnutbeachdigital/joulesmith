package com.walnutbeachdigital.joulesmith.machine.furnace;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.joulesmith.machine.MachineBlock;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ElectricFurnaceBlock extends MachineBlock {
    public static final MapCodec<ElectricFurnaceBlock> CODEC = simpleCodec(ElectricFurnaceBlock::new);

    public ElectricFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, JSBlockEntities.ELECTRIC_FURNACE.get(), MachineBlockEntity::serverTick);
    }
}
