package com.walnutbeachdigital.joulesmith.logic;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A sensor: gives off a redstone signal from what it senses. Inventory, Tank and Energy sensors read the
 * block their face points at (place them against it), stronger the fuller it is. The Time sensor is on in
 * daytime. The Proximity sensor counts players, hostile mobs or animals within 8 blocks (right-click to
 * choose), one signal level each. Right-click any sensor to see its reading.
 */
public class SensorBlock extends BaseEntityBlock {
    public enum Kind {
        INVENTORY(Measure.ITEMS), TANK(Measure.FLUID), ENERGY(Measure.ENERGY), TIME(Measure.TIME), PROXIMITY(Measure.PLAYERS);

        public final Measure measure;

        Kind(Measure measure) {
            this.measure = measure;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<SensorBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.xmap(s -> Kind.valueOf(s.toUpperCase(Locale.ROOT)), Kind::id).fieldOf("kind").forGetter(SensorBlock::kind),
            propertiesCodec()
    ).apply(i, SensorBlock::new));
    /** The way the sensor's face points: at the block it reads. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private final Kind kind;

    public SensorBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof SensorBlockEntity sensor ? sensor.power() : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof SensorBlockEntity sensor) {
            if (kind == Kind.PROXIMITY && !player.isShiftKeyDown()) sensor.nextTarget();
            player.displayClientMessage(Component.translatable("message.joulesmith.sensor",
                    Component.translatable(sensor.measure().key()), sensor.measure().format(sensor.reading()), sensor.power()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SensorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, JSBlockEntities.SENSOR.get(), SensorBlockEntity::serverTick);
    }
}
