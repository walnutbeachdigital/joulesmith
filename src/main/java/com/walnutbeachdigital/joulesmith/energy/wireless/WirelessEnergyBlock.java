package com.walnutbeachdigital.joulesmith.energy.wireless;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Wireless Transmitter or Receiver. Use a dye on one to set its channel (its lamp shows the colour);
 * Receivers draw power from every Transmitter on the same channel within range in the same dimension.
 */
public class WirelessEnergyBlock extends BaseEntityBlock {
    public static final EnumProperty<DyeColor> CHANNEL = EnumProperty.create("channel", DyeColor.class);
    public static final MapCodec<WirelessEnergyBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("transmitter").forGetter(WirelessEnergyBlock::transmitter), propertiesCodec()).apply(i, WirelessEnergyBlock::new));

    private final boolean transmitter;

    public WirelessEnergyBlock(boolean transmitter, Properties properties) {
        super(properties);
        this.transmitter = transmitter;
        registerDefaultState(stateDefinition.any().setValue(CHANNEL, DyeColor.WHITE));
    }

    public boolean transmitter() {
        return transmitter;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHANNEL);
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
        if (!(stack.getItem() instanceof DyeItem dye)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(CHANNEL, dye.getDyeColor()), 3);
            player.displayClientMessage(Component.translatable("message.joulesmith.wireless.channel",
                    Component.translatable("color.minecraft." + dye.getDyeColor().getName())), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WirelessEnergyBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || transmitter ? null
                : createTickerHelper(type, JSBlockEntities.WIRELESS_ENERGY.get(), WirelessEnergyBlockEntity::receiverTick);
    }
}
