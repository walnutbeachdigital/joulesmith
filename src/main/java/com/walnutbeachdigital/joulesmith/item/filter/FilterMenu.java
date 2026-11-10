package com.walnutbeachdigital.joulesmith.item.filter;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;
import com.walnutbeachdigital.joulesmith.registry.JSItems;
import com.walnutbeachdigital.joulesmith.registry.JSMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Filter Card screen: nine "ghost" slots that hold a copy of an item rather than the item itself
 * (click with an item to set a slot, with an empty hand to clear it), and a whitelist / blacklist switch.
 */
public class FilterMenu extends AbstractContainerMenu {
    public static final int GRID_X = 44, GRID_Y = 17, INVENTORY_Y = 84;
    public static final int BUTTON_TOGGLE = 0;

    private final InteractionHand hand;
    private final SimpleContainer ghosts = new SimpleContainer(FilterContents.SIZE);
    private final int lockedSlot;
    private boolean blacklist;

    public FilterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readEnum(InteractionHand.class));
    }

    public FilterMenu(int id, Inventory inventory, InteractionHand hand) {
        super(JSMenus.FILTER.get(), id);
        this.hand = hand;
        FilterContents filter = inventory.player.getItemInHand(hand).getOrDefault(JSDataComponents.FILTER.get(), FilterContents.EMPTY);
        for (int i = 0; i < FilterContents.SIZE; i++) ghosts.setItem(i, filter.items().get(i).copy());
        blacklist = filter.blacklist();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(ghosts, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                    @Override public boolean mayPickup(Player player) { return false; }
                });
            }
        }
        // the card being edited stays put while its screen is open
        lockedSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            boolean locked = col == lockedSlot;
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58) {
                @Override public boolean mayPickup(Player player) { return !locked; }
                @Override public boolean mayPlace(ItemStack stack) { return !locked; }
            });
        }
        addDataSlot(new DataSlot() {
            @Override public int get() { return blacklist ? 1 : 0; }
            @Override public void set(int value) { blacklist = value == 1; }
        });
    }

    public boolean blacklist() {
        return blacklist;
    }

    private void save(Player player) {
        ItemStack card = player.getItemInHand(hand);
        if (!card.is(JSItems.FILTER_CARD.get())) return;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < FilterContents.SIZE; i++) items.add(ghosts.getItem(i));
        card.set(JSDataComponents.FILTER.get(), new FilterContents(items, blacklist));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FilterContents.SIZE) {
            if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
                ItemStack carried = getCarried();
                ghosts.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
                save(player);
            }
            return;
        }
        if (clickType == ClickType.SWAP && button == lockedSlot) return;
        super.clicked(slotId, button, clickType, player);
    }

    /** Shift-click in the inventory: copy that item into the first free ghost slot. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < FilterContents.SIZE) return ItemStack.EMPTY;
        ItemStack stack = slots.get(index).getItem();
        if (stack.isEmpty()) return ItemStack.EMPTY;
        for (int i = 0; i < FilterContents.SIZE; i++) {
            if (ItemStack.isSameItem(ghosts.getItem(i), stack)) return ItemStack.EMPTY;
        }
        for (int i = 0; i < FilterContents.SIZE; i++) {
            if (ghosts.getItem(i).isEmpty()) {
                ghosts.setItem(i, stack.copyWithCount(1));
                save(player);
                break;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_TOGGLE) return false;
        blacklist = !blacklist;
        save(player);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).is(JSItems.FILTER_CARD.get());
    }
}
