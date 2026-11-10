package com.walnutbeachdigital.joulesmith.farming.rubber;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A rubber tree log. Logs on wild trees may have a tap spot: full of latex ("wet") until a Tree Tap
 * draws it, then "dry" until it slowly refills. Logs a player places have no spot.
 */
public class RubberLogBlock extends RotatedPillarBlock {
    public static final MapCodec<RubberLogBlock> CODEC = simpleCodec(RubberLogBlock::new);

    public enum Latex implements StringRepresentable {
        NONE, DRY, WET;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Latex> LATEX = EnumProperty.create("latex", Latex.class);
    /** Chance on each random tick that a dry spot fills again. */
    private static final float REFILL_CHANCE = 0.12f;

    public RubberLogBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LATEX, Latex.NONE));
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LATEX);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LATEX) == Latex.DRY;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < REFILL_CHANCE) level.setBlock(pos, state.setValue(LATEX, Latex.WET), Block.UPDATE_ALL);
    }
}
