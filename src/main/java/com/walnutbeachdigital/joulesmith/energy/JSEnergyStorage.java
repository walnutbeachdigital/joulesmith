package com.walnutbeachdigital.joulesmith.energy;

import net.minecraft.util.Mth;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Standard Forge Energy (FE) storage, so other mods' cables and machines understand it,
 * plus helpers for Joulesmith's own machines. Calls {@code onChange} whenever the amount changes.
 */
public class JSEnergyStorage extends EnergyStorage {
    private final Runnable onChange;

    public JSEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    /** Adds energy a generator made, ignoring the receive limit. Returns how much fitted. */
    public int generate(int amount) {
        int added = Math.min(amount, capacity - energy);
        if (added > 0) {
            energy += added;
            made += added;
            onChange.run();
        }
        return added;
    }

    /** Removes energy a machine uses, ignoring the extract limit. Returns how much was taken. */
    public int consume(int amount) {
        int removed = Math.min(amount, energy);
        if (removed > 0) {
            energy -= removed;
            used += removed;
            onChange.run();
        }
        return removed;
    }

    /** FE made and used since the last {@link #sample}, for the FE/t shown on the energy bar. */
    private long made, used;

    /** Returns {made, used} since the last call and starts counting again. */
    public long[] sample() {
        long[] out = {made, used};
        made = used = 0;
        return out;
    }

    public void setEnergy(int amount) {
        energy = Mth.clamp(amount, 0, capacity);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) onChange.run();
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) onChange.run();
        return extracted;
    }
}
