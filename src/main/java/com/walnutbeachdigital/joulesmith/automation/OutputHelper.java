package com.walnutbeachdigital.joulesmith.automation;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Fitting several stacks into a machine's output slots, all or nothing. */
final class OutputHelper {
    static boolean fits(ItemStackHandler items, List<SlotDef> slots, List<ItemStack> drops) {
        List<ItemStack> space = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).role() == SlotRole.OUTPUT) space.add(items.getStackInSlot(i).copy());
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

    /** Stores what fits; returns what didn't. */
    static List<ItemStack> store(ItemStackHandler items, List<SlotDef> slots, List<ItemStack> drops) {
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack drop : drops) {
            ItemStack left = drop.copy();
            for (int i = 0; i < slots.size() && !left.isEmpty(); i++) {
                if (slots.get(i).role() == SlotRole.OUTPUT) left = items.insertItem(i, left, false);
            }
            if (!left.isEmpty()) rest.add(left);
        }
        return rest;
    }

    private OutputHelper() {}
}
