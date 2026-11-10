package com.walnutbeachdigital.joulesmith.transport.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSItems;
import com.walnutbeachdigital.joulesmith.transport.PipeBlock;
import com.walnutbeachdigital.joulesmith.transport.PipeBlockEntity;
import com.walnutbeachdigital.joulesmith.transport.PipeConnection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** An item duct (tier 1 moves 1 item, tier 2 moves 4, every half second). Right-click a side with a Filter Card to fit it. */
public class ItemDuctBlock extends PipeBlock {
    public static final MapCodec<ItemDuctBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(ItemDuctBlock::tier),
            propertiesCodec()
    ).apply(i, ItemDuctBlock::new));

    public ItemDuctBlock(int tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean sameFamily(BlockState other) {
        return other.getBlock() instanceof ItemDuctBlock;
    }

    @Override
    protected boolean hasTarget(Level level, BlockPos other, Direction face) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, other, face) != null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(JSItems.FILTER_CARD.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Direction side = sideFromHit(pos, hit.getLocation(), hit.getDirection());
        PipeConnection conn = state.getValue(CONNECTION.get(side));
        if (conn != PipeConnection.PUSH && conn != PipeConnection.PULL) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.joulesmith.filter.no_inventory"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ItemDuctBlockEntity duct) {
            ItemStack old = duct.setFilter(side, stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!old.isEmpty()) ItemHandlerHelper.giveItemToPlayer(player, old);
            player.displayClientMessage(Component.translatable("message.joulesmith.filter.added"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ItemDuctBlockEntity duct) duct.dropFilters();
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemDuctBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, JSBlockEntities.ITEM_DUCT.get(), PipeBlockEntity::serverTick);
    }
}
