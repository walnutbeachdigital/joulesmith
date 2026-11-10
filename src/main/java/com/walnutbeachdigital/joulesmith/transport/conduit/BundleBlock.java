package com.walnutbeachdigital.joulesmith.transport.conduit;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.joulesmith.network.NetworkPart;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSItems;
import com.walnutbeachdigital.joulesmith.transport.PipeConnection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The block every duct, pipe and cable is placed as. Holds one conduit of each kind side by side; breaking it
 * takes out the conduit you're looking at, and the block goes with the last one.
 */
public class BundleBlock extends BaseEntityBlock implements NetworkPart {
    public static final MapCodec<BundleBlock> CODEC = simpleCodec(BundleBlock::new);

    public BundleBlock(Properties properties) {
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

    private static BundleView view(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BundleBlockEntity bundle ? bundle.view() : BundleView.EMPTY;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ConduitShapes.shape(view(level, pos));
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    // ------------------------------------------------------------------ which conduit is being looked at

    /** The conduit a player is looking at in this bundle, or null. */
    public static ConduitType target(Player player, BlockPos pos) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1f, false);
        Vec3 at = hit instanceof BlockHitResult b && b.getBlockPos().equals(pos) ? b.getLocation() : Vec3.atCenterOf(pos);
        return target(player.level(), pos, at);
    }

    public static ConduitType target(BlockGetter level, BlockPos pos, Vec3 at) {
        return ConduitShapes.conduitAt(view(level, pos), at.subtract(pos.getX(), pos.getY(), pos.getZ()));
    }

    /** The side of a conduit a click at {@code at} on face {@code face} meant. */
    public static Direction side(BlockGetter level, BlockPos pos, ConduitType type, Vec3 at, Direction face) {
        BundleView.Entry entry = view(level, pos).get(type);
        return entry == null ? face : ConduitShapes.sideAt(entry, at.subtract(pos.getX(), pos.getY(), pos.getZ()), face);
    }

    // ------------------------------------------------------------------ neighbours

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BundleBlockEntity bundle) bundle.refreshConnections();
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        if (level instanceof Level l && !l.isClientSide && l.getBlockEntity(pos) instanceof BundleBlockEntity bundle) bundle.refreshConnections();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BundleBlockEntity bundle) {
            for (Conduit c : bundle.all()) {
                for (ItemStack extra : c.extras()) Block.popResource(level, pos, extra);
                c.invalidateNetwork();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    // ------------------------------------------------------------------ breaking one conduit at a time

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof BundleBlockEntity bundle && bundle.all().size() > 1) {
            ConduitType type = target(player, pos);
            if (type != null && bundle.has(type)) {
                if (!level.isClientSide) {
                    Conduit removed = bundle.remove(type);
                    if (removed != null) {
                        if (!player.getAbilities().instabuild) Block.popResource(level, pos, stackOf(removed));
                        for (ItemStack extra : removed.extras()) Block.popResource(level, pos, extra);
                    }
                }
                return false;
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    public static ItemStack stackOf(Conduit conduit) {
        return new ItemStack(JSItems.conduit(conduit.type, conduit.tier()));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BundleBlockEntity bundle) {
            for (Conduit c : bundle.all()) drops.add(stackOf(c));
        }
        return drops;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof BundleBlockEntity bundle) {
            ConduitType type = target(level, pos, target.getLocation());
            Conduit c = type == null ? null : bundle.get(type);
            if (c != null) return stackOf(c);
        }
        return ItemStack.EMPTY;
    }

    // ------------------------------------------------------------------ filter cards

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(JSItems.FILTER_CARD.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof BundleBlockEntity bundle) || !(bundle.get(ConduitType.ITEM) instanceof ItemConduit duct)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Direction side = side(level, pos, ConduitType.ITEM, hit.getLocation(), hit.getDirection());
        PipeConnection conn = duct.connection(side);
        if (conn != PipeConnection.PUSH && conn != PipeConnection.PULL) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.joulesmith.filter.no_inventory"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack old = duct.setFilter(side, stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (!old.isEmpty()) ItemHandlerHelper.giveItemToPlayer(player, old);
            player.displayClientMessage(Component.translatable("message.joulesmith.filter.added"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    // ------------------------------------------------------------------ block entity

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BundleBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, JSBlockEntities.CONDUIT_BUNDLE.get(), BundleBlockEntity::serverTick);
    }
}
