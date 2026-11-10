package com.walnutbeachdigital.joulesmith.machine.creative;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class CreativePowerCellBlockEntity extends BlockEntity {
    /** How much it offers each neighbour every tick. */
    public static final int PUSH_PER_TICK = 100_000;

    /** An endless store: always full, gives whatever is asked, takes nothing. */
    public static final IEnergyStorage ENDLESS = new IEnergyStorage() {
        @Override public int receiveEnergy(int toReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int toExtract, boolean simulate) { return toExtract; }
        @Override public int getEnergyStored() { return Integer.MAX_VALUE; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };

    public CreativePowerCellBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.CREATIVE_POWER_CELL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativePowerCellBlockEntity cell) {
        for (Direction dir : Direction.values()) {
            IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(dir), dir.getOpposite());
            if (target != null && target.canReceive()) {
                target.receiveEnergy(PUSH_PER_TICK, false);
            }
        }
    }
}
