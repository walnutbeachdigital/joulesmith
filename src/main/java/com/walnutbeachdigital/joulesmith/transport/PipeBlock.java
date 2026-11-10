package com.walnutbeachdigital.joulesmith.transport;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared block for item ducts and fluid pipes. Joins to pipes of the same family and to blocks that hold
 * what it carries. The wrench sets each side to push, pull, both or off.
 */
public abstract class PipeBlock extends BaseEntityBlock {
    public static final Map<Direction, EnumProperty<PipeConnection>> CONNECTION = new EnumMap<>(Direction.class);

    static {
        for (Direction dir : Direction.values()) {
            CONNECTION.put(dir, EnumProperty.create(dir.getSerializedName(), PipeConnection.class));
        }
    }

    private static final VoxelShape CORE = box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> PLATES = new EnumMap<>(Direction.class);

    static {
        ARMS.put(Direction.NORTH, box(5, 5, 0, 11, 11, 5));
        ARMS.put(Direction.SOUTH, box(5, 5, 11, 11, 11, 16));
        ARMS.put(Direction.WEST, box(0, 5, 5, 5, 11, 11));
        ARMS.put(Direction.EAST, box(11, 5, 5, 16, 11, 11));
        ARMS.put(Direction.DOWN, box(5, 0, 5, 11, 5, 11));
        ARMS.put(Direction.UP, box(5, 11, 5, 11, 16, 11));
        PLATES.put(Direction.NORTH, box(4, 4, 0, 12, 12, 2));
        PLATES.put(Direction.SOUTH, box(4, 4, 14, 12, 12, 16));
        PLATES.put(Direction.WEST, box(0, 4, 4, 2, 12, 12));
        PLATES.put(Direction.EAST, box(14, 4, 4, 16, 12, 12));
        PLATES.put(Direction.DOWN, box(4, 0, 4, 12, 2, 12));
        PLATES.put(Direction.UP, box(4, 14, 4, 12, 16, 12));
    }

    protected final int tier;

    protected PipeBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (EnumProperty<PipeConnection> p : CONNECTION.values()) state = state.setValue(p, PipeConnection.NONE);
        registerDefaultState(state);
    }

    public int tier() {
        return tier;
    }

    /** Whether another block is a pipe this one joins (same family, any tier). */
    protected abstract boolean sameFamily(BlockState other);

    /** Whether the block at {@code other} holds what this pipe carries, on its face {@code face}. */
    protected abstract boolean hasTarget(Level level, BlockPos other, Direction face);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTION.values().forEach(builder::add);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            PipeConnection conn = state.getValue(CONNECTION.get(dir));
            if (conn != PipeConnection.NONE) shape = Shapes.or(shape, ARMS.get(dir));
            if (conn == PipeConnection.PUSH || conn == PipeConnection.PULL) shape = Shapes.or(shape, PLATES.get(dir));
        }
        return shape;
    }

    // ------------------------------------------------------------------ connections

    /** How this pipe should look on side {@code dir}, from its wrench setting and what is next to it. */
    public PipeConnection connectionTo(LevelAccessor level, BlockPos pos, Direction dir) {
        PipeMode mode = level.getBlockEntity(pos) instanceof PipeBlockEntity pipe ? pipe.mode(dir) : PipeMode.PUSH;
        if (mode == PipeMode.DISABLED) return PipeConnection.NONE;
        BlockPos other = pos.relative(dir);
        BlockState otherState = level.getBlockState(other);
        if (sameFamily(otherState)) {
            if (level.getBlockEntity(other) instanceof PipeBlockEntity pipe && pipe.mode(dir.getOpposite()) == PipeMode.DISABLED) {
                return PipeConnection.NONE;
            }
            return PipeConnection.PIPE;
        }
        if (otherState.getBlock() instanceof PipeBlock) return PipeConnection.NONE;    // a pipe of another kind
        return level instanceof Level l && hasTarget(l, other, dir.getOpposite()) ? mode.look() : PipeConnection.NONE;
    }

    private BlockState computeState(LevelAccessor level, BlockPos pos, BlockState state) {
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTION.get(dir), connectionTo(level, pos, dir));
        }
        return state;
    }

    /** Redraws every side, e.g. after a wrench change. */
    public void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState updated = computeState(level, pos, state);
        if (updated != state) level.setBlock(pos, updated, Block.UPDATE_ALL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) {
            pipe.invalidateNetwork();
        }
        return state.setValue(CONNECTION.get(dir), connectionTo(level, pos, dir));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!state.is(oldState.getBlock())) invalidateAround(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) invalidateAround(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    private static void invalidateAround(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof PipeBlockEntity pipe) pipe.invalidateNetwork();
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof PipeBlockEntity pipe) pipe.invalidateNetwork();
        }
    }

    // ------------------------------------------------------------------ wrench

    /** Which side of the pipe a click hit: the arm or plate under the cursor, or the core's face. */
    public static Direction sideFromHit(BlockPos pos, Vec3 hit, Direction face) {
        double dx = hit.x - pos.getX() - 0.5, dy = hit.y - pos.getY() - 0.5, dz = hit.z - pos.getZ() - 0.5;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        double max = Math.max(ax, Math.max(ay, az));
        if (max <= 3 / 16.0 + 1e-3) return face;                 // on the core
        if (max == ax) return dx < 0 ? Direction.WEST : Direction.EAST;
        if (max == ay) return dy < 0 ? Direction.DOWN : Direction.UP;
        return dz < 0 ? Direction.NORTH : Direction.SOUTH;
    }

    /**
     * Wrench on side {@code dir}. Next to another pipe it joins or splits them. Next to a block it steps
     * push, pull, both, off. Sneaking hands over to the pipe (filters and routing on item ducts).
     */
    public InteractionResult wrench(Level level, BlockPos pos, Player player, Direction dir, boolean sneaking) {
        if (!(level.getBlockEntity(pos) instanceof PipeBlockEntity pipe)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Component message;
        if (sneaking) {
            message = pipe.sneakWrench(player, dir);
        } else {
            BlockPos other = pos.relative(dir);
            BlockState otherState = level.getBlockState(other);
            if (sameFamily(otherState) && level.getBlockEntity(other) instanceof PipeBlockEntity otherPipe) {
                boolean join = pipe.mode(dir) == PipeMode.DISABLED || otherPipe.mode(dir.getOpposite()) == PipeMode.DISABLED;
                PipeMode mode = join ? PipeMode.PUSH : PipeMode.DISABLED;
                pipe.setMode(dir, mode);
                otherPipe.setMode(dir.getOpposite(), mode);
                if (otherState.getBlock() instanceof PipeBlock otherBlock) otherBlock.refresh(level, other);
                message = Component.translatable(join ? "message.joulesmith.pipe.joined" : "message.joulesmith.pipe.split");
            } else if (!(otherState.getBlock() instanceof PipeBlock) && hasTarget(level, other, dir.getOpposite())) {
                PipeMode mode = pipe.mode(dir).next();
                pipe.setMode(dir, mode);
                message = Component.translatable(mode.key());
            } else {
                message = Component.translatable("message.joulesmith.pipe.nothing");
            }
            refresh(level, pos);
        }
        if (player != null && message != null) player.displayClientMessage(message, true);
        return InteractionResult.SUCCESS;
    }
}
