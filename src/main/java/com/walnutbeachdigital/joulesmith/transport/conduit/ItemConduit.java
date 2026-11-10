package com.walnutbeachdigital.joulesmith.transport.conduit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.walnutbeachdigital.joulesmith.item.filter.FilterContents;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;
import com.walnutbeachdigital.joulesmith.transport.PipeMode;
import com.walnutbeachdigital.joulesmith.transport.PipeNetwork;
import com.walnutbeachdigital.joulesmith.transport.RoutingMode;
import com.walnutbeachdigital.joulesmith.transport.item.ItemNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * An item duct (tier 1 moves 1 item, 2 moves 4, 3 moves 16, 4 moves 32, 5 a stack, every half second).
 * Keeps each side's routing mode and filter card. A hopper or another mod's pipe can push items straight in.
 */
public class ItemConduit extends PipeConduit {
    private final RoutingMode[] routing = new RoutingMode[6];
    private final ItemStack[] filters = new ItemStack[6];

    public ItemConduit(int tier) {
        super(ConduitType.ITEM, tier);
        Arrays.fill(routing, RoutingMode.ROUND_ROBIN);
        Arrays.fill(filters, ItemStack.EMPTY);
    }

    @Override
    protected boolean hasTarget(Level level, BlockPos other, Direction face) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, other, face) != null;
    }

    @Override
    protected PipeNetwork createNetwork() {
        return ItemNetwork.create(level(), pos());
    }

    public RoutingMode routing(Direction dir) {
        return routing[dir.get3DDataValue()];
    }

    public ItemStack filter(Direction dir) {
        return filters[dir.get3DDataValue()];
    }

    /** Whether an item may pass this side's filter card (always, with no card). */
    public boolean passes(Direction dir, ItemStack stack) {
        ItemStack card = filter(dir);
        return card.isEmpty() || card.getOrDefault(JSDataComponents.FILTER.get(), FilterContents.EMPTY).test(stack);
    }

    /** Puts a filter card on a side and returns the card that was there. */
    public ItemStack setFilter(Direction dir, ItemStack card) {
        ItemStack old = filters[dir.get3DDataValue()];
        filters[dir.get3DDataValue()] = card;
        if (host != null) host.changed(false);
        return old;
    }

    @Override
    public Component sneakWrench(Player player, Direction dir) {
        ItemStack card = filter(dir);
        if (!card.isEmpty()) {
            setFilter(dir, ItemStack.EMPTY);
            ItemHandlerHelper.giveItemToPlayer(player, card);
            return Component.translatable("message.joulesmith.filter.removed");
        }
        if (!mode(dir).pulls()) return Component.translatable("message.joulesmith.routing.pull_only");
        RoutingMode next = routing(dir).next();
        routing[dir.get3DDataValue()] = next;
        invalidateNetwork();
        if (host != null) host.changed(false);
        return Component.translatable("message.joulesmith.routing", Component.translatable(next.key()));
    }

    /** What a hopper or another mod's pipe sees: one slot that sends anything put in straight down the network. */
    public IItemHandler itemHandler(Direction side) {
        if (side != null && mode(side) == PipeMode.DISABLED) return null;
        return new IItemHandler() {
            @Override public int getSlots() { return 1; }
            @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
            @Override public int getSlotLimit(int slot) { return 64; }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return network() instanceof ItemNetwork net ? net.deliver(pos(), side, stack, simulate) : stack;
            }
        };
    }

    @Override
    public List<ItemStack> extras() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack card : filters) if (!card.isEmpty()) out.add(card);
        return out;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        super.save(tag, registries);
        int[] values = new int[6];
        for (int i = 0; i < 6; i++) values[i] = routing[i].ordinal();
        tag.putIntArray("Routing", values);
        ListTag list = new ListTag();
        for (ItemStack card : filters) list.add(card.saveOptional(registries));
        tag.put("Filters", list);
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        super.load(tag, registries);
        int[] values = tag.getIntArray("Routing");
        for (int i = 0; i < Math.min(6, values.length); i++) {
            routing[i] = RoutingMode.values()[Math.floorMod(values[i], RoutingMode.values().length)];
        }
        ListTag list = tag.getList("Filters", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(6, list.size()); i++) filters[i] = ItemStack.parseOptional(registries, list.getCompound(i));
    }
}
