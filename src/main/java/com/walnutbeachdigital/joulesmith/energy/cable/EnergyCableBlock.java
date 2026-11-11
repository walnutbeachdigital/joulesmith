package com.walnutbeachdigital.joulesmith.energy.cable;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * An energy cable. Joins to other cables and to anything that holds power; every connected cable
 * forms one network that carries up to 500 FE/t to the machines along it.
 */
public class EnergyCableBlock extends BaseEntityBlock {
    public static final MapCodec<EnergyCableBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(EnergyCableBlock::tier),
            propertiesCodec()
    ).apply(i, EnergyCableBlock::new));
    public static final Map<Direction, BooleanProperty> CONNECTED = PipeBlock.PROPERTY_BY_DIRECTION;

    private final int tier;

    public EnergyCableBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (BooleanProperty p : CONNECTED.values()) state = state.setValue(p, false);
        registerDefaultState(state.setValue(com.walnutbeachdigital.joulesmith.transport.PipeShapes.NODE, true));
    }

    /** 1 Basic (500 FE/t), 2 Reinforced (2,000), 3 Advanced (10,000). A network runs at its weakest cable's rate. */
    public int tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTED.values().forEach(builder::add);
        builder.add(com.walnutbeachdigital.joulesmith.transport.PipeShapes.NODE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
            link[dir.get3DDataValue()] = level.getBlockState(pos.relative(dir)).getBlock() instanceof EnergyCableBlock;
        }
        return state.setValue(com.walnutbeachdigital.joulesmith.transport.PipeShapes.NODE,
                com.walnutbeachdigital.joulesmith.transport.PipeShapes.node(any, link));
    }

    private static boolean connects(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        if (level.getBlockState(other).getBlock() instanceof EnergyCableBlock) return true;
        return level instanceof Level l && l.getCapability(Capabilities.EnergyStorage.BLOCK, other, dir.getOpposite()) != null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTED.get(dir), connects(context.getLevel(), context.getClickedPos(), dir));
        }
        return withNode(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnergyCableBlockEntity cable) {
            cable.invalidateNetwork();
        }
        return withNode(state.setValue(CONNECTED.get(dir), connects(level, pos, dir)), level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        invalidateAround(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) invalidateAround(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    private static void invalidateAround(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(pos.relative(dir)) instanceof EnergyCableBlockEntity cable) cable.invalidateNetwork();
        }
        if (level.getBlockEntity(pos) instanceof EnergyCableBlockEntity cable) cable.invalidateNetwork();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, JSBlockEntities.ENERGY_CABLE.get(), EnergyCableBlockEntity::serverTick);
    }
}
