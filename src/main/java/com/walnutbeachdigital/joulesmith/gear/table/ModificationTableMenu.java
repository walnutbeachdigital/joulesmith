package com.walnutbeachdigital.joulesmith.gear.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.walnutbeachdigital.joulesmith.gear.powered.Module;
import com.walnutbeachdigital.joulesmith.gear.powered.ModuleItem;
import com.walnutbeachdigital.joulesmith.gear.powered.PoweredItem;
import com.walnutbeachdigital.joulesmith.registry.JSBlocks;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;
import com.walnutbeachdigital.joulesmith.registry.JSItems;
import com.walnutbeachdigital.joulesmith.registry.JSMenus;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * The Modification Table: a slot for powered gear and two module slots. The module slots show what the
 * item in the gear slot has fitted; putting a module in fits it, taking one out removes it. Modules stay
 * with the item when it is taken out.
 */
public class ModificationTableMenu extends AbstractContainerMenu {
    public static final int TOOL_X = 44, MODULE_X = 98, SLOT_Y = 35, INVENTORY_Y = 84;
    private static final int TOOL = 0, FIRST_MODULE = 1, INV_START = 3, INV_END = 39;

    private final ContainerLevelAccess access;
    private final Player player;
    private final SimpleContainer tool = new SimpleContainer(1);
    private final SimpleContainer modules = new SimpleContainer(2);
    private boolean loading;

    public ModificationTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public ModificationTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(JSMenus.MODIFICATION_TABLE.get(), id);
        this.access = access;
        this.player = inventory.player;
        tool.addListener(c -> loadModules());
        modules.addListener(c -> saveModules());
        addSlot(new Slot(tool, 0, TOOL_X, SLOT_Y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof PoweredItem p && p != null && fitsAny(p); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < 2; i++) {
            final int index = i;
            addSlot(new Slot(modules, i, MODULE_X + i * 18, SLOT_Y) {
                @Override public boolean mayPlace(ItemStack stack) { return canFit(stack, index); }
                @Override public boolean mayPickup(Player p) { return !tool.getItem(0).isEmpty(); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    private static boolean fitsAny(PoweredItem item) {
        for (Module m : Module.values()) if (item.accepts(m)) return true;
        return false;
    }

    /** Whether a module can go in module slot {@code index} for the item in the gear slot. */
    private boolean canFit(ItemStack stack, int index) {
        ItemStack gear = tool.getItem(0);
        if (!(stack.getItem() instanceof ModuleItem module) || !(gear.getItem() instanceof PoweredItem powered)) return false;
        if (!powered.accepts(module.module)) return false;
        ItemStack other = modules.getItem(1 - index);
        return !(other.getItem() instanceof ModuleItem o) || !o.module.conflicts(module.module);
    }

    /** Shows the gear's modules in the module slots, or empties them when the gear is taken out. */
    private void loadModules() {
        loading = true;
        try {
            List<Module> fitted = PoweredItem.modules(tool.getItem(0));
            for (int i = 0; i < 2; i++) {
                modules.setItem(i, i < fitted.size() ? new ItemStack(JSItems.MODULES.get(fitted.get(i)).get()) : ItemStack.EMPTY);
            }
        } finally {
            loading = false;
        }
    }

    /** Records the module slots on the gear, and sets the enchantments its modules grant. */
    private void saveModules() {
        ItemStack gear = tool.getItem(0);
        if (loading || gear.isEmpty()) return;
        List<Module> fitted = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if (modules.getItem(i).getItem() instanceof ModuleItem m) fitted.add(m.module);
        }
        if (fitted.isEmpty()) gear.remove(JSDataComponents.MODULES.get());
        else gear.set(JSDataComponents.MODULES.get(), List.copyOf(fitted));
        var lookup = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Set<ResourceKey<Enchantment>> managed = new HashSet<>();
        for (Module m : Module.values()) if (m.enchantment != null) managed.add(m.enchantment);
        EnchantmentHelper.updateEnchantments(gear, mutable -> {
            mutable.removeIf(holder -> managed.stream().anyMatch(holder::is));
            for (Module m : fitted) {
                if (m.enchantment != null) lookup.get(m.enchantment).ifPresent(h -> mutable.set(h, m.level));
            }
        });
        tool.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < INV_START) {
            if (!moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PoweredItem && slots.get(TOOL).mayPlace(stack) && !slots.get(TOOL).hasItem()) {
            if (!moveItemStackTo(stack, TOOL, TOOL + 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ModuleItem) {
            boolean moved = false;
            for (int i = 0; i < 2 && !moved; i++) {
                Slot target = slots.get(FIRST_MODULE + i);
                if (!target.hasItem() && target.mayPlace(stack)) {
                    target.setByPlayer(stack.split(1));
                    moved = true;
                }
            }
            if (!moved) return ItemStack.EMPTY;
        } else if (index < INV_END - 9) {
            if (!moveItemStackTo(stack, INV_END - 9, INV_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, INV_START, INV_END - 9, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // the modules are recorded on the gear, so only the gear goes back to the player
        loading = true;
        modules.clearContent();
        loading = false;
        access.execute((level, pos) -> clearContainer(player, tool));
        if (access == ContainerLevelAccess.NULL && !player.level().isClientSide) clearContainer(player, tool);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, JSBlocks.MODIFICATION_TABLE.get());
    }
}
