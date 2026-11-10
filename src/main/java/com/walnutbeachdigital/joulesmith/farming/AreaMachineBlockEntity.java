package com.walnutbeachdigital.joulesmith.farming;

import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared base for farm machines that work a square of ground in front of them (5x5 at first, 9x9 and 13x13
 * with Range Upgrades), level with the machine. Every half second (faster with Speed Upgrades) it looks at
 * the next spot in the square and does its job there, using power each time it does something.
 */
public abstract class AreaMachineBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, MAX_INPUT = 200, BASE_INTERVAL = 10, BASE_RADIUS = 2;

    private int cursor;
    private int cooldown;
    private int active;

    protected AreaMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SlotDef> slots) {
        super(type, pos, state, slots, CAPACITY, MAX_INPUT, 0, true);
    }

    /** How far the square reaches each way from its centre. */
    public int radius() {
        return BASE_RADIUS + 2 * Math.min(2, upgrades(UpgradeCardItem.Type.RANGE));
    }

    /** The middle of the square: just far enough in front of the machine that the square touches it. */
    public BlockPos centre() {
        return worldPosition.relative(facing(), radius() + 1);
    }

    /** Work one spot of the square. Returns the energy used (0 if there was nothing to do). */
    protected abstract int workAt(ServerLevel level, BlockPos spot, int offsetX, int offsetZ);

    /** Called every tick before the spot work, for jobs that span ticks (a tree being felled). Returns energy used. */
    protected int continueJob(ServerLevel level) {
        return 0;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (!allowed || !(level instanceof ServerLevel server)) return active-- > 0;
        int used = continueJob(server);
        int interval = Math.max(1, (int) Math.ceil(BASE_INTERVAL / (1 + 0.5 * upgrades(UpgradeCardItem.Type.SPEED))));
        if (used == 0 && ++cooldown >= interval) {
            cooldown = 0;
            int r = radius(), side = r * 2 + 1;
            cursor = Math.floorMod(cursor, side * side);
            int dx = cursor % side - r, dz = cursor / side - r;
            cursor++;
            BlockPos spot = centre().offset(dx, 0, dz);
            if (level.isLoaded(spot)) used = workAt(server, spot, dx, dz);
        }
        if (used > 0) {
            active = BASE_INTERVAL * 2;
            setChanged();
        }
        return active-- > 0;
    }

    /** Whether there is enough power for one action. */
    protected boolean hasEnergy(int amount) {
        double saving = Math.max(0.4, 1 - 0.2 * upgrades(UpgradeCardItem.Type.EFFICIENCY));
        return energy.getEnergyStored() >= (int) Math.ceil(amount * saving);
    }

    /** Spends power for one action (less with Efficiency Upgrades) and returns what was spent. */
    protected int spend(int amount) {
        int cost = (int) Math.ceil(amount * Math.max(0.4, 1 - 0.2 * upgrades(UpgradeCardItem.Type.EFFICIENCY)));
        energy.consume(cost);
        return Math.max(1, cost);
    }

    /** Whether all of {@code drops} would fit in the output slots. */
    protected boolean fits(List<ItemStack> drops) {
        // try them on a copy of the output slots
        List<ItemStack> space = new java.util.ArrayList<>();
        for (int i = 0; i < slotDefs().size(); i++) {
            if (slotDefs().get(i).role() == SlotRole.OUTPUT) space.add(items.getStackInSlot(i).copy());
        }
        for (ItemStack drop : drops) {
            ItemStack left = drop.copy();
            for (int i = 0; i < space.size() && !left.isEmpty(); i++) {
                ItemStack there = space.get(i);
                if (there.isEmpty()) {
                    space.set(i, left.copy());
                    left = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(there, left)) {
                    int moved = Math.min(there.getMaxStackSize() - there.getCount(), left.getCount());
                    there.grow(moved);
                    left.shrink(moved);
                }
            }
            if (!left.isEmpty()) return false;
        }
        return true;
    }

    /** Puts drops into the output slots (check {@link #fits} first). */
    protected void store(List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack left = drop.copy();
            for (int i = 0; i < slotDefs().size() && !left.isEmpty(); i++) {
                if (slotDefs().get(i).role() == SlotRole.OUTPUT) left = items.insertItem(i, left, false);
            }
        }
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Cursor", cursor);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        cursor = tag.getInt("Cursor");
    }
}
