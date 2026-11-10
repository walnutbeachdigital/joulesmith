package com.walnutbeachdigital.joulesmith.transport.conveyor;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * A flat belt that carries items, mobs and players the way it faces, about a block a second (no power
 * needed). Items reaching its far end are put into the inventory or machine in front of it if it has room;
 * belts in a row hand items along. Sneaking players aren't moved.
 */
public class ConveyorBeltBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ConveyorBeltBlock> CODEC = simpleCodec(ConveyorBeltBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);
    private static final double SPEED = 0.08, CENTRE_PULL = 0.05;

    public ConveyorBeltBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        move(level, pos, state, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity) move(level, pos, state, entity);   // items rest inside the belt's height
    }

    private static void move(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof Player player && player.isShiftKeyDown()) return;
        if (entity.getY() > pos.getY() + 0.5) return;
        Direction facing = state.getValue(FACING);
        Vec3 motion = entity.getDeltaMovement();
        double along = facing.getAxis() == Direction.Axis.X ? motion.x : motion.z;
        double sign = facing.getAxisDirection().getStep();
        double push = along * sign < SPEED ? SPEED * sign : along;
        // steer toward the middle of the belt, across the direction of travel
        double cx = pos.getX() + 0.5 - entity.getX(), cz = pos.getZ() + 0.5 - entity.getZ();
        if (facing.getAxis() == Direction.Axis.X) entity.setDeltaMovement(push, motion.y, cz * CENTRE_PULL * 4);
        else entity.setDeltaMovement(cx * CENTRE_PULL * 4, motion.y, push);
        if (entity instanceof ItemEntity item && !level.isClientSide) deliver(level, pos, facing, item);
    }

    /** At the far end of the belt, puts the item into whatever is in front, if it isn't another belt. */
    private static void deliver(Level level, BlockPos pos, Direction facing, ItemEntity item) {
        double progress = facing.getAxis() == Direction.Axis.X ? item.getX() - pos.getX() : item.getZ() - pos.getZ();
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) progress = 1 - progress;
        if (progress < 0.8) return;
        BlockPos front = pos.relative(facing);
        if (level.getBlockState(front).getBlock() instanceof ConveyorBeltBlock) return;
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, front, facing.getOpposite());
        if (target == null) return;
        ItemStack left = ItemHandlerHelper.insertItemStacked(target, item.getItem().copy(), false);
        if (left.isEmpty()) item.discard();
        else item.setItem(left);
    }
}
