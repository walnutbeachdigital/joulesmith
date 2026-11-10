package com.walnutbeachdigital.joulesmith.storage.crate;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A Crate's inventory. Its contents go into the item when it is broken, like a shulker box. */
public class CrateBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler items;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.CRATE.get(), pos, state);
        int rows = state.getBlock() instanceof CrateBlock crate ? crate.rows() : 6;
        this.items = new ItemStackHandler(rows * 9) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return !(stack.getItem() instanceof CrateItem);       // no crates in crates
            }
        };
    }

    public ItemStackHandler items() {
        return items;
    }

    public int rows() {
        return items.getSlots() / 9;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CrateMenu(id, inventory, worldPosition, rows(), items);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            ItemStackHandler saved = new ItemStackHandler();
            saved.deserializeNBT(registries, tag.getCompound("Items"));
            for (int i = 0; i < Math.min(saved.getSlots(), items.getSlots()); i++) items.setStackInSlot(i, saved.getStackInSlot(i));
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        NonNullList<ItemStack> list = NonNullList.withSize(items.getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < items.getSlots(); i++) list.set(i, items.getStackInSlot(i));
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ItemContainerContents contents = input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> list = NonNullList.withSize(items.getSlots(), ItemStack.EMPTY);
        contents.copyInto(list);
        for (int i = 0; i < list.size(); i++) items.setStackInSlot(i, list.get(i));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Items");
    }
}
