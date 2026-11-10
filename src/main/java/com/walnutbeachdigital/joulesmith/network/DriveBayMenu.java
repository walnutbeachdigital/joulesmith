package com.walnutbeachdigital.joulesmith.network;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Drive Bay's screen: vanilla's one-row chest, but its nine slots only take Item Drives, one each. */
public class DriveBayMenu extends ChestMenu {
    public DriveBayMenu(int id, Inventory inventory, Container drives) {
        this(id, inventory, drives, stack -> stack.getItem() instanceof DriveItem);
    }

    /** The same for any nine single-item slots that take only what {@code allowed} accepts (the CPU's patterns too). */
    public DriveBayMenu(int id, Inventory inventory, Container drives, java.util.function.Predicate<ItemStack> allowed) {
        super(MenuType.GENERIC_9x1, id, inventory, drives, 1);
        for (int i = 0; i < DriveBayBlockEntity.SLOTS; i++) {
            Slot old = slots.get(i);
            Slot slot = new Slot(drives, i, old.x, old.y) {
                @Override public boolean mayPlace(ItemStack stack) { return allowed.test(stack); }
                @Override public int getMaxStackSize() { return 1; }
            };
            slot.index = i;
            slots.set(i, slot);
        }
    }
}
