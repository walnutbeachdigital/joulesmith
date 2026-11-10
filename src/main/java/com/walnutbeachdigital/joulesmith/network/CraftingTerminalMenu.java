package com.walnutbeachdigital.joulesmith.network;

import java.util.Optional;

import com.walnutbeachdigital.joulesmith.registry.JSBlocks;
import com.walnutbeachdigital.joulesmith.registry.JSMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

/**
 * The Crafting Terminal: the Network Terminal with a crafting grid. After each craft, every grid slot
 * that ran out is topped up with one more of the same from the network, so shift-clicking the result
 * crafts as many as the network has ingredients for (up to a stack). What's left in the grid goes back
 * into the network when the screen closes.
 */
public class CraftingTerminalMenu extends TerminalMenu {
    public static final int CRAFT_X = 26, CRAFT_Y = 96, RESULT_X = 120, RESULT_Y = 114;
    private final TransientCraftingContainer grid = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer result = new ResultContainer();
    private final int resultIndex;

    public CraftingTerminalMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos());
    }

    public CraftingTerminalMenu(int id, Inventory inventory, BlockPos pos) {
        super(JSMenus.CRAFTING_TERMINAL.get(), id, inventory, pos, CRAFTING_ROWS, CRAFTING_INVENTORY_Y);
        resultIndex = slots.size();
        addSlot(new ResultSlot(inventory.player, grid, result, 0, RESULT_X, RESULT_Y) {
            @Override
            public void onTake(Player p, ItemStack stack) {
                ItemStack[] before = new ItemStack[9];
                for (int i = 0; i < 9; i++) before[i] = grid.getItem(i).copy();
                super.onTake(p, stack);
                if (p.level().isClientSide) return;
                Network net = network();
                for (int i = 0; i < 9; i++) {
                    if (grid.getItem(i).isEmpty() && !before[i].isEmpty()) {
                        grid.setItem(i, net.extract(before[i], 1, false));
                    }
                }
                resend();
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) addSlot(new Slot(grid, col + row * 3, CRAFT_X + col * 18, CRAFT_Y + row * 18));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == grid && player instanceof ServerPlayer sp) {
            var input = grid.asCraftInput();
            Optional<RecipeHolder<CraftingRecipe>> recipe = sp.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, sp.level());
            ItemStack out = recipe.map(r -> r.value().assemble(input, sp.level().registryAccess())).orElse(ItemStack.EMPTY);
            result.setRecipeUsed(recipe.orElse(null));
            result.setItem(0, out);
        }
        super.slotsChanged(container);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (index == resultIndex) {
            // craft again and again into the inventory, up to a stack's worth
            Slot slot = slots.get(index);
            int made = 0;
            while (slot.hasItem() && made < 64) {
                ItemStack stack = slot.getItem();
                ItemStack copy = stack.copy();
                if (room(stack) < stack.getCount() || !moveItemStackTo(stack, 0, 36, true)) break;
                slot.onTake(p, copy);
                made += copy.getCount();
            }
            return ItemStack.EMPTY;
        }
        if (index > resultIndex) {                          // a grid slot: back into the network
            Slot slot = slots.get(index);
            if (slot.hasItem() && !p.level().isClientSide) {
                slot.set(network().insert(slot.getItem(), false));
                resend();
            }
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(p, index);
    }

    /** How many of {@code stack} the player's inventory slots could take. */
    private int room(ItemStack stack) {
        int room = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack in = slots.get(i).getItem();
            if (in.isEmpty()) room += stack.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(in, stack)) room += in.getMaxStackSize() - in.getCount();
        }
        return room;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p.level().isClientSide) return;
        Network net = network();
        for (int i = 0; i < 9; i++) {
            ItemStack left = net.insert(grid.removeItemNoUpdate(i), false);
            if (!left.isEmpty() && !p.getInventory().add(left)) p.drop(left, false);
        }
    }

    @Override
    protected Block block() {
        return JSBlocks.CRAFTING_TERMINAL.get();
    }
}
