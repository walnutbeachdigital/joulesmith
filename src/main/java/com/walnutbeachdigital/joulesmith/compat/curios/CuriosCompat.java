package com.walnutbeachdigital.joulesmith.compat.curios;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.walnutbeachdigital.joulesmith.Joulesmith;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Curios support: Energy Cells fit a belt slot (data/curios/tags/item/belt.json) and keep charging your gear
 * from there, and powered gear worn in Curios slots charges on a Charging Pad. Reached by reflection, so
 * Joulesmith needs nothing from Curios to build or run.
 */
public final class CuriosCompat {
    private static Boolean loaded;
    private static Method getInventory, getEquipped;

    /** The stacks a player wears in Curios slots, or none without Curios. */
    public static List<ItemStack> equipped(LivingEntity entity) {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("curios");
            if (loaded) {
                try {
                    getInventory = Class.forName("top.theillusivec4.curios.api.CuriosApi").getMethod("getCuriosInventory", LivingEntity.class);
                    getEquipped = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler").getMethod("getEquippedCurios");
                } catch (ReflectiveOperationException | LinkageError e) {
                    Joulesmith.LOGGER.warn("Curios is installed but its API could not be reached", e);
                    loaded = false;
                }
            }
        }
        if (!loaded) return List.of();
        try {
            Optional<?> inventory = (Optional<?>) getInventory.invoke(null, entity);
            if (inventory.isEmpty()) return List.of();
            IItemHandler handler = (IItemHandler) getEquipped.invoke(inventory.get());
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) out.add(stack);
            }
            return out;
        } catch (ReflectiveOperationException | ClassCastException e) {
            return List.of();
        }
    }

    private CuriosCompat() {}
}
