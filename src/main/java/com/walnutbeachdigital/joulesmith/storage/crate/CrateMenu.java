package com.walnutbeachdigital.joulesmith.storage.crate;

import com.walnutbeachdigital.joulesmith.registry.JSMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** A Crate's screen: its rows of nine slots above the player's inventory, like a big chest. */
public class CrateMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final int rows;

    public CrateMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), buf.readVarInt(), null);
    }

    public CrateMenu(int id, Inventory inventory, BlockPos pos, int rows, IItemHandler items) {
        super(JSMenus.CRATE.get(), id);
        this.pos = pos;
        this.rows = rows;
        IItemHandler handler = items != null ? items : new ItemStackHandler(rows * 9);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) addSlot(new SlotItemHandler(handler, c + r * 9, 8 + c * 18, 18 + r * 18));
        }
        int invY = inventoryY(rows);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c + r * 9 + 9, 8 + c * 18, invY + r * 18));
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c, 8 + c * 18, invY + 58));
    }

    public static int inventoryY(int rows) {
        return 18 + rows * 18 + 14;
    }

    public int rows() {
        return rows;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int crate = rows * 9;
        if (index < crate) {
            if (!moveItemStackTo(stack, crate, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, crate, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0 && player.level().getBlockEntity(pos) instanceof CrateBlockEntity;
    }
}
