package com.walnutbeachdigital.joulesmith.energy.solar;

import com.walnutbeachdigital.joulesmith.energy.JSEnergyStorage;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Makes its tier's FE/t in full sun under open sky, less at dawn, dusk and in rain (it follows the light
 * the way a daylight detector does), and nothing at night. Pushes its power down and to the sides.
 */
public class SolarPanelBlockEntity extends BlockEntity {
    private final JSEnergyStorage energy;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.SOLAR_PANEL.get(), pos, state);
        int rate = rate(state);
        energy = new JSEnergyStorage(rate * 200, 0, rate * 4, this::setChanged);
    }

    private static int rate(BlockState state) {
        return state.getBlock() instanceof SolarPanelBlock panel ? panel.rate() : 10;
    }

    public IEnergyStorage energyHandler() {
        return energy;
    }

    /** FE/t right now. */
    public static int output(Level level, BlockPos pos, int rate) {
        if (!level.dimensionType().hasSkyLight() || !level.canSeeSky(pos.above())) return 0;
        int light = level.getBrightness(LightLayer.SKY, pos.above()) - level.getSkyDarken();
        return Math.max(0, rate * light / 15);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        int rate = rate(state);
        panel.energy.generate(output(level, pos, rate));
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || panel.energy.getEnergyStored() == 0) continue;
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null && target.canReceive()) {
                int taken = target.receiveEnergy(Math.min(rate * 4, panel.energy.getEnergyStored()), false);
                panel.energy.consume(taken);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
    }
}
