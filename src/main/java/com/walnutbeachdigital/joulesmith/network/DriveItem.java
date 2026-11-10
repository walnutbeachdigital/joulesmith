package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;
import com.walnutbeachdigital.joulesmith.storage.bin.BinContents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * An Item Drive: holds up to {@link #capacity()} items of up to {@link #TYPES} kinds, recorded on the drive
 * itself, so a drive keeps its contents when taken out of a Drive Bay.
 */
public class DriveItem extends Item {
    public static final int TYPES = 63;
    private final int capacity;

    public DriveItem(int capacity, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public static List<BinContents> contents(ItemStack drive) {
        return drive.getOrDefault(JSDataComponents.DRIVE.get(), List.of());
    }

    public static long stored(ItemStack drive) {
        long n = 0;
        for (BinContents c : contents(drive)) n += c.count();
        return n;
    }

    /** Puts up to {@code amount} of {@code item} in; returns how many went in. */
    public static int insert(ItemStack drive, ItemStack item, int amount, boolean simulate) {
        if (!(drive.getItem() instanceof DriveItem d) || item.isEmpty() || amount <= 0) return 0;
        List<BinContents> list = contents(drive);
        int room = (int) Math.min(Integer.MAX_VALUE, d.capacity - stored(drive));
        int index = indexOf(list, item);
        if (index < 0 && list.size() >= TYPES) return 0;
        int moved = Math.min(room, amount);
        if (moved <= 0 || simulate) return Math.max(0, moved);
        List<BinContents> next = new ArrayList<>(list);
        if (index < 0) next.add(new BinContents(item, moved));
        else next.set(index, new BinContents(item, list.get(index).count() + moved));
        drive.set(JSDataComponents.DRIVE.get(), List.copyOf(next));
        return moved;
    }

    /** Takes up to {@code amount} of {@code item} out; returns how many came out. */
    public static int extract(ItemStack drive, ItemStack item, int amount, boolean simulate) {
        List<BinContents> list = contents(drive);
        int index = indexOf(list, item);
        if (index < 0 || amount <= 0) return 0;
        int have = list.get(index).count(), moved = Math.min(have, amount);
        if (simulate) return moved;
        List<BinContents> next = new ArrayList<>(list);
        if (moved == have) next.remove(index);
        else next.set(index, new BinContents(item, have - moved));
        if (next.isEmpty()) drive.remove(JSDataComponents.DRIVE.get());
        else drive.set(JSDataComponents.DRIVE.get(), List.copyOf(next));
        return moved;
    }

    static int indexOf(List<BinContents> list, ItemStack item) {
        for (int i = 0; i < list.size(); i++) if (ItemStack.isSameItemSameComponents(list.get(i).item(), item)) return i;
        return -1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.drive.items", String.format("%,d", stored(stack)), String.format("%,d", capacity))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.joulesmith.drive.types", contents(stack).size(), TYPES).withStyle(ChatFormatting.GRAY));
    }
}
