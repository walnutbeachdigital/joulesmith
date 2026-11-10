package com.walnutbeachdigital.joulesmith.machine.generator;

import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Makes power from the wind: 50 FE/t at sea level (y 64) rising to 300 FE/t at y 192 and above, a quarter
 * more in rain and half again in a thunderstorm. Blocks in the 5x5 square around its rotor slow it down,
 * so it wants open air on every side.
 */
public class WindTurbineBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 40_000, OUTPUT = 600, MIN = 50, MAX = 300, LOW = 64, HIGH = 192;

    private int rate = -1;

    public WindTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.WIND_TURBINE.get(), pos, state, List.of(), CAPACITY, 0, OUTPUT);
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    /** FE/t at this spot right now. */
    private int rate(Level level, BlockPos pos) {
        float height = Mth.clamp((pos.getY() - LOW) / (float) (HIGH - LOW), 0f, 1f);
        float base = MIN + (MAX - MIN) * height;
        int open = 0, total = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0 && dy <= 0) continue;          // its own block and the mast below
                    total++;
                    if (level.getBlockState(pos.offset(dx, dy, dz)).isAir()) open++;
                }
            }
        }
        float weather = level.isThundering() ? 1.5f : level.isRaining() ? 1.25f : 1f;
        return Math.round(base * weather * open / total);
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (rate < 0 || level.getGameTime() % 40 == 0) rate = rate(level, pos);
        boolean running = false;
        if (allowed && rate > 0 && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
            energy.generate(rate);
            running = true;
        }
        pushEnergy(level, pos, OUTPUT);
        return running;
    }
}
