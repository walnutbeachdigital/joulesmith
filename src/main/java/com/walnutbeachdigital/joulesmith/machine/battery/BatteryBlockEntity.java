package com.walnutbeachdigital.joulesmith.machine.battery;

import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.MachineFace;
import com.walnutbeachdigital.joulesmith.machine.SideMode;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Stores power. Faces set to input take power in; faces set to output push it out to whatever touches
 * them. By default the front gives out and every other face takes in. Its front lights while charged.
 */
public class BatteryBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 100_000, RATE = 200;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.BASIC_BATTERY.get(), pos, state, List.of(), CAPACITY, RATE, RATE);
        for (MachineFace face : MachineFace.values()) setDefaultSide(face, SideMode.INPUT);
        setDefaultSide(MachineFace.FRONT, SideMode.OUTPUT);
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (allowed) pushEnergy(level, pos, RATE);
        return energy.getEnergyStored() > 0;
    }

    @Override
    public IEnergyStorage energyHandler(Direction side) {
        if (side == null) return energy;
        SideMode mode = sideMode(MachineFace.of(facing(), side));
        return mode == SideMode.NONE ? null : new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) { return mode.accepts() ? energy.receiveEnergy(amount, simulate) : 0; }
            @Override public int extractEnergy(int amount, boolean simulate) { return mode.gives() ? energy.extractEnergy(amount, simulate) : 0; }
            @Override public int getEnergyStored() { return energy.getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
            @Override public boolean canExtract() { return mode.gives(); }
            @Override public boolean canReceive() { return mode.accepts(); }
        };
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }
}
