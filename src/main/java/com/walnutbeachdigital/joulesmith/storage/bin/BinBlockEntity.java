package com.walnutbeachdigital.joulesmith.storage.bin;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A Bulk Bin: a lot of one item (4,096 in the basic bin, 16,384 reinforced). It forgets the item once it
 * is empty. Pipes and hoppers see it as one big slot.
 */
public class BinBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    private int count;
    private boolean dirty;

    public BinBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.BULK_BIN.get(), pos, state);
    }

    public int capacity() {
        return getBlockState().getBlock() instanceof BinBlock bin ? bin.capacity() : 4096;
    }

    public ItemStack item() {
        return item;
    }

    public int count() {
        return count;
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (item.isEmpty() || ItemStack.isSameItemSameComponents(item, stack))
                && !(stack.getItem() instanceof BinItem);
    }

    /** Adds as much of {@code stack} as fits; returns what is left. */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) return stack;
        int moved = Math.min(stack.getCount(), capacity() - count);
        if (moved <= 0) return stack;
        if (!simulate) {
            if (item.isEmpty()) item = stack.copyWithCount(1);
            count += moved;
            changed();
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Takes up to {@code amount} (at most a stack) out. */
    public ItemStack extract(int amount, boolean simulate) {
        if (item.isEmpty() || count <= 0) return ItemStack.EMPTY;
        int moved = Math.min(Math.min(amount, count), item.getMaxStackSize());
        ItemStack out = item.copyWithCount(moved);
        if (!simulate) {
            count -= moved;
            if (count <= 0) {
                count = 0;
                item = ItemStack.EMPTY;
            }
            changed();
        }
        return out;
    }

    private void changed() {
        setChanged();
        dirty = true;
    }

    public final IItemHandler handler = new IItemHandler() {
        @Override public int getSlots() { return 1; }
        @Override public ItemStack getStackInSlot(int slot) { return item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(count); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return insert(stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return extract(amount, simulate); }
        @Override public int getSlotLimit(int slot) { return capacity(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return accepts(stack); }
    };

    /** Sends the count to nearby players a few times a second while it changes. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BinBlockEntity bin) {
        if (bin.dirty && level.getGameTime() % 4 == 0) {
            bin.dirty = false;
            level.sendBlockUpdated(pos, state, state, 3);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!item.isEmpty()) {
            tag.put("Item", item.save(registries));
            tag.putInt("Count", count);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = tag.contains("Item") ? ItemStack.parseOptional(registries, tag.getCompound("Item")) : ItemStack.EMPTY;
        count = item.isEmpty() ? 0 : tag.getInt("Count");
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!item.isEmpty()) components.set(JSDataComponents.BIN.get(), new BinContents(item, count));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        BinContents contents = input.getOrDefault(JSDataComponents.BIN.get(), BinContents.EMPTY);
        item = contents.isEmpty() ? ItemStack.EMPTY : contents.item().copyWithCount(1);
        count = contents.isEmpty() ? 0 : contents.count();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Item");
        tag.remove("Count");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
