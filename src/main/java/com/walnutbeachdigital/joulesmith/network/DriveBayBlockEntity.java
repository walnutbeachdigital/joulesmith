package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A Drive Bay: nine slots for Item Drives, whose contents join the network it's on. */
public class DriveBayBlockEntity extends BlockEntity {
    public static final int SLOTS = 9;

    private final SimpleContainer drives = new SimpleContainer(SLOTS) {
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem() instanceof DriveItem; }
        @Override public int getMaxStackSize() { return 1; }
        @Override public void setChanged() { super.setChanged(); DriveBayBlockEntity.this.setChanged(); }
    };

    public DriveBayBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.DRIVE_BAY.get(), pos, state);
    }

    public Container container() {
        return drives;
    }

    /** The drives in it (the stacks themselves, so changes to them are kept). */
    public List<ItemStack> drives() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = drives.getItem(i);
            if (s.getItem() instanceof DriveItem) list.add(s);
        }
        return list;
    }

    void changed() {
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) list.set(i, drives.getItem(i));
        ContainerHelper.saveAllItems(tag, list, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        NonNullList<ItemStack> list = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, list, registries);
        for (int i = 0; i < SLOTS; i++) drives.setItem(i, list.get(i));
    }
}
