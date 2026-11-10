package com.walnutbeachdigital.joulesmith.storage.tank;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/** The Fluid Tank block. Use a bucket on it to fill or empty it; use an empty hand to see what it holds. */
public class FluidTankBlock extends BaseEntityBlock {
    public static final MapCodec<FluidTankBlock> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(i -> i.group(
            com.mojang.serialization.Codec.INT.fieldOf("capacity").forGetter(FluidTankBlock::capacity),
            propertiesCodec()).apply(i, FluidTankBlock::new));

    private final int capacity;

    public FluidTankBlock(int capacity, Properties properties) {
        super(properties);
        this.capacity = capacity;
    }

    /** mB this tank holds: 16,000, 64,000 or 256,000 by tier. */
    public int capacity() {
        return capacity;
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) {
            player.displayClientMessage(describe(tank.columnFluid(), tank.columnCapacity()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static Component describe(FluidStack fluid, int capacity) {
        String cap = String.format("%,d", capacity);
        return fluid.isEmpty()
                ? Component.translatable("message.joulesmith.fluid.empty", cap)
                : Component.translatable("message.joulesmith.fluid", fluid.getHoverName(), String.format("%,d", fluid.getAmount()), cap);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank) || tank.tank().isEmpty()) return 0;
        return 1 + (int) (14.0 * tank.tank().getFluidAmount() / tank.tank().getCapacity());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, JSBlockEntities.FLUID_TANK.get(), FluidTankBlockEntity::serverTick);
    }
}
