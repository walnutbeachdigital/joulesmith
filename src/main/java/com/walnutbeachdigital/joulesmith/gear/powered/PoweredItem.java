package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * An item that holds Forge Energy instead of durability: it shows a charge bar, and stops working when
 * empty instead of breaking. Charge it on a Charging Pad, or by carrying an Energy Cell.
 */
public interface PoweredItem {
    int BAR_COLOR = 0x3FC8F0;

    int capacity();

    /** The most FE per tick it takes in or gives out. */
    int transferRate();

    /** Which kinds of module fit this item; none for the Energy Cell. */
    default boolean accepts(Module module) {
        return false;
    }

    static int energy(ItemStack stack) {
        return stack.getOrDefault(JSDataComponents.ENERGY.get(), 0);
    }

    static void setEnergy(ItemStack stack, int energy) {
        int max = stack.getItem() instanceof PoweredItem p ? p.capacity() : energy;
        stack.set(JSDataComponents.ENERGY.get(), Mth.clamp(energy, 0, max));
    }

    /** Whether the item has at least {@code amount} FE; always true for creative players. */
    static boolean has(ItemStack stack, int amount, LivingEntity user) {
        return creative(user) || energy(stack) >= amount;
    }

    /** Uses {@code amount} FE if the item has it (creative players use none). */
    static boolean use(ItemStack stack, int amount, LivingEntity user) {
        if (creative(user)) return true;
        int energy = energy(stack);
        if (energy < amount) return false;
        setEnergy(stack, energy - amount);
        return true;
    }

    private static boolean creative(LivingEntity user) {
        return user instanceof Player player && player.getAbilities().instabuild;
    }

    /** How many modules fit (the Modification Table shows this many slots). */
    default int moduleSlots() {
        return 2;
    }

    static List<Module> modules(ItemStack stack) {
        return stack.getOrDefault(JSDataComponents.MODULES.get(), List.of());
    }

    static boolean hasModule(ItemStack stack, Module module) {
        return modules(stack).contains(module);
    }

    static int barWidth(ItemStack stack) {
        int cap = stack.getItem() instanceof PoweredItem p ? p.capacity() : 1;
        return Math.round(13.0f * energy(stack) / Math.max(1, cap));
    }

    /** "Energy: x / y FE" and the fitted modules. */
    static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        int cap = stack.getItem() instanceof PoweredItem p ? p.capacity() : 0;
        tooltip.add(Component.translatable("tooltip.joulesmith.energy", String.format("%,d", energy(stack)), String.format("%,d", cap))
                .withStyle(ChatFormatting.GRAY));
        for (Module module : modules(stack)) {
            tooltip.add(Component.literal(" + ").append(Component.translatable("item.joulesmith." + module.id() + "_module"))
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    /**
     * Gives up to {@code budget} FE to the powered items the player carries and wears (not to Energy Cells
     * unless {@code cells}). Returns how much was given.
     */
    static int chargeInventory(Player player, int budget, boolean cells, ItemStack except) {
        int given = 0;
        var inv = player.getInventory();
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) stacks.add(inv.getItem(i));
        stacks.addAll(com.walnutbeachdigital.joulesmith.compat.curios.CuriosCompat.equipped(player));   // gear worn in Curios slots
        for (ItemStack stack : stacks) {
            if (given >= budget) break;
            if (stack.isEmpty() || stack == except) continue;
            if (!cells && stack.getItem() instanceof EnergyCellItem) continue;
            IEnergyStorage storage = stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage == null || !storage.canReceive()) continue;
            given += storage.receiveEnergy(budget - given, false);
        }
        return given;
    }
}
