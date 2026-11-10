package com.walnutbeachdigital.joulesmith.network;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Network Cable: joins storage network blocks together. */
public class NetworkCableBlock extends Block implements NetworkPart {
    public static final MapCodec<NetworkCableBlock> CODEC = simpleCodec(NetworkCableBlock::new);
    public static final Map<Direction, BooleanProperty> CONNECTED = PipeBlock.PROPERTY_BY_DIRECTION;
    private static final VoxelShape CORE = box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, box(6, 6, 0, 10, 10, 6), Direction.SOUTH, box(6, 6, 10, 10, 10, 16),
            Direction.WEST, box(0, 6, 6, 6, 10, 10), Direction.EAST, box(10, 6, 6, 16, 10, 10),
            Direction.DOWN, box(6, 0, 6, 10, 6, 10), Direction.UP, box(6, 10, 6, 10, 16, 10));

    public NetworkCableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : CONNECTED.values()) state = state.setValue(p, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTED.values().forEach(builder::add);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) if (state.getValue(CONNECTED.get(dir))) shape = Shapes.or(shape, ARMS.get(dir));
        return shape;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTED.get(dir),
                    context.getLevel().getBlockState(context.getClickedPos().relative(dir)).getBlock() instanceof NetworkPart);
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CONNECTED.get(dir), neighbor.getBlock() instanceof NetworkPart);
    }
}
