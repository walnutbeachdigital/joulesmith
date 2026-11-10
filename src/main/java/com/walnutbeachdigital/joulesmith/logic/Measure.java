package com.walnutbeachdigital.joulesmith.logic;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Something sensors and the Logic Controller can read. Most read the block on one side (how full its power
 * store, inventory or tank is, or the redstone signal coming from it); the rest read the world around them.
 */
public enum Measure {
    ENERGY(100, 5, true), ITEMS(100, 5, true), FLUID(100, 5, true), REDSTONE(15, 1, true),
    TIME(23, 1, false), PLAYERS(16, 1, false), HOSTILES(16, 1, false), ANIMALS(16, 1, false);

    public static final int RANGE = 8;

    /** The highest value it reads, how far one click moves a setting, and whether it reads a side. */
    public final int max, step;
    public final boolean usesSide;

    Measure(int max, int step, boolean usesSide) {
        this.max = max;
        this.step = step;
        this.usesSide = usesSide;
    }

    public String key() {
        return "gui.joulesmith.measure." + name().toLowerCase(Locale.ROOT);
    }

    /** Percent readings show a %, the hour shows as a time, the rest as plain numbers. */
    public String format(int value) {
        return switch (this) {
            case ENERGY, ITEMS, FLUID -> value + "%";
            case TIME -> String.format("%02d:00", value);
            default -> Integer.toString(value);
        };
    }

    /** The reading for a block at {@code pos} looking out of its {@code side}. */
    public int read(Level level, BlockPos pos, Direction side) {
        BlockPos other = pos.relative(side);
        Direction face = side.getOpposite();
        switch (this) {
            case ENERGY -> {
                IEnergyStorage e = level.getCapability(Capabilities.EnergyStorage.BLOCK, other, face);
                return e == null || e.getMaxEnergyStored() <= 0 ? 0 : (int) (100L * e.getEnergyStored() / e.getMaxEnergyStored());
            }
            case ITEMS -> {
                IItemHandler items = level.getCapability(Capabilities.ItemHandler.BLOCK, other, face);
                if (items == null || items.getSlots() == 0) return 0;
                float fill = 0;
                for (int i = 0; i < items.getSlots(); i++) {
                    var s = items.getStackInSlot(i);
                    if (!s.isEmpty()) fill += s.getCount() / (float) Math.min(items.getSlotLimit(i), s.getMaxStackSize());
                }
                return Math.round(100 * fill / items.getSlots());
            }
            case FLUID -> {
                IFluidHandler tanks = level.getCapability(Capabilities.FluidHandler.BLOCK, other, face);
                if (tanks == null) return 0;
                long have = 0, room = 0;
                for (int i = 0; i < tanks.getTanks(); i++) {
                    have += tanks.getFluidInTank(i).getAmount();
                    room += tanks.getTankCapacity(i);
                }
                return room <= 0 ? 0 : (int) (100 * have / room);
            }
            case REDSTONE -> {
                return level.getSignal(other, side);
            }
            case TIME -> {
                return (int) ((level.getDayTime() + 6000) % 24000 / 1000);       // tick 0 is 06:00
            }
            case PLAYERS -> {
                return Math.min(max, level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(RANGE), p -> !p.isSpectator()).size());
            }
            case HOSTILES -> {
                return Math.min(max, level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(RANGE), m -> m instanceof Enemy).size());
            }
            case ANIMALS -> {
                return Math.min(max, level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(RANGE)).size());
            }
        }
        return 0;
    }

    /** A reading as a redstone strength: 0 only at zero, 15 at the top. */
    public int signal(int value) {
        if (value <= 0) return 0;
        return Math.max(1, Math.min(15, (int) Math.ceil(15.0 * value / max)));
    }
}
