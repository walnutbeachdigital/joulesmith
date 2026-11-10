package com.walnutbeachdigital.joulesmith.network;

import java.util.Map;

import com.mojang.serialization.MapCodec;
import com.walnutbeachdigital.joulesmith.item.filter.FilterCardItem;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An Import or Export Bus: placed against an inventory, it moves items between that inventory and the
 * network it's cabled to. Use a Filter Card on it to fit one (sneak with an empty hand to take it back).
 */
public class BusBlock extends BaseEntityBlock implements NetworkPart {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Shapes.or(box(3, 3, 0, 13, 13, 3), box(5, 5, 3, 11, 11, 8)),
            Direction.SOUTH, Shapes.or(box(3, 3, 13, 13, 13, 16), box(5, 5, 8, 11, 11, 13)),
            Direction.WEST, Shapes.or(box(0, 3, 3, 3, 13, 13), box(3, 5, 5, 8, 11, 11)),
            Direction.EAST, Shapes.or(box(13, 3, 3, 16, 13, 13), box(8, 5, 5, 13, 11, 11)),
            Direction.DOWN, Shapes.or(box(3, 0, 3, 13, 3, 13), box(5, 3, 5, 11, 8, 11)),
            Direction.UP, Shapes.or(box(3, 13, 3, 13, 16, 13), box(5, 8, 5, 11, 13, 11)));

    private final boolean export, fluid;
    private final MapCodec<BusBlock> codec;

    public BusBlock(boolean export, Properties properties) {
        this(export, false, properties);
    }

    /** {@code fluid}: a Fluid Import or Export Bus, moving fluids instead of items. */
    public BusBlock(boolean export, boolean fluid, Properties properties) {
        super(properties);
        this.export = export;
        this.fluid = fluid;
        this.codec = simpleCodec(p -> new BusBlock(export, fluid, p));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public boolean export() {
        return export;
    }

    public boolean fluid() {
        return fluid;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Faces the block it was placed against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FilterCardItem) || !(level.getBlockEntity(pos) instanceof BusBlockEntity bus)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            ItemStack old = bus.filter();
            bus.setFilter(stack.split(1));
            if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
            player.displayClientMessage(Component.translatable("message.joulesmith.bus.filter"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BusBlockEntity bus)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (player.isShiftKeyDown() && !bus.filter().isEmpty()) {
                ItemStack old = bus.filter();
                bus.setFilter(ItemStack.EMPTY);
                if (!player.getInventory().add(old)) player.drop(old, false);
                player.displayClientMessage(Component.translatable("message.joulesmith.bus.filter_removed"), true);
            } else {
                player.displayClientMessage(Component.translatable(bus.filter().isEmpty()
                        ? (export ? "message.joulesmith.bus.export_empty" : "message.joulesmith.bus.import_all")
                        : "message.joulesmith.bus.filtered"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BusBlockEntity bus && !bus.filter().isEmpty()) {
            Block.popResource(level, pos, bus.filter());
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BusBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, JSBlockEntities.BUS.get(), BusBlockEntity::serverTick);
    }
}
