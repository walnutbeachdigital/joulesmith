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
    /** Shapes by which sides connect, built once: one per block state was slow to build at startup. */
    public NetworkCableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : CONNECTED.values()) state = state.setValue(p, false);
        registerDefaultState(state.setValue(com.walnutbeachdigital.joulesmith.transport.PipeShapes.NODE, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTED.values().forEach(builder::add);
        builder.add(com.walnutbeachdigital.joulesmith.transport.PipeShapes.NODE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int[] sides = new int[6];
        for (Direction dir : Direction.values()) sides[dir.ordinal()] = state.getValue(CONNECTED.get(dir)) ? 1 : 0;
        return com.walnutbeachdigital.joulesmith.transport.PipeShapes.shape(sides, state.getValue(com.walnutbeachdigital.joulesmith.transport.PipeShapes.NODE));
    }

    /** Sets the nub: hidden only in the middle of a straight run of cable. */
    private static BlockState withNode(BlockState state, LevelAccessor level, BlockPos pos) {
        boolean[] any = new boolean[6], link = new boolean[6];
        for (Direction dir : Direction.values()) {
            any[dir.get3DDataValue()] = state.getValue(CONNECTED.get(dir));
            link[dir.get3DDataValue()] = level.getBlockState(pos.relative(dir)).getBlock() instanceof NetworkCableBlock;
        }
        return state.setValue(com.walnutbeachdigital.joulesmith.transport.PipeShapes.NODE,
                com.walnutbeachdigital.joulesmith.transport.PipeShapes.node(any, link));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTED.get(dir),
                    context.getLevel().getBlockState(context.getClickedPos().relative(dir)).getBlock() instanceof NetworkPart);
        }
        return withNode(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return withNode(state.setValue(CONNECTED.get(dir), neighbor.getBlock() instanceof NetworkPart), level, pos);
    }
}
