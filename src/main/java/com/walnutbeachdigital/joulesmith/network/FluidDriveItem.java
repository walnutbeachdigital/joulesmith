package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;

/** A Fluid Drive: holds up to {@link #capacity()} mB of up to {@link #TYPES} fluids, kept on the drive itself. */
public class FluidDriveItem extends Item {
    public static final int TYPES = 9;

    /** What a Fluid Drive holds. */
    public record Contents(List<FluidStack> fluids) {
        public static final Contents EMPTY = new Contents(List.of());
        public static final Codec<Contents> CODEC = FluidStack.CODEC.listOf().xmap(Contents::new, Contents::fluids);
        public static final StreamCodec<RegistryFriendlyByteBuf, Contents> STREAM_CODEC =
                FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()).map(Contents::new, Contents::fluids);

        public Contents {
            fluids = List.copyOf(fluids.stream().map(FluidStack::copy).toList());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Contents c) || c.fluids.size() != fluids.size()) return false;
            for (int i = 0; i < fluids.size(); i++) if (!FluidStack.matches(fluids.get(i), c.fluids.get(i))) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int h = 1;
            for (FluidStack f : fluids) h = h * 31 + FluidStack.hashFluidAndComponents(f) + f.getAmount();
            return h;
        }
    }

    private final int capacity;

    public FluidDriveItem(int capacity, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public static List<FluidStack> contents(ItemStack drive) {
        return drive.getOrDefault(JSDataComponents.FLUID_DRIVE.get(), Contents.EMPTY).fluids();
    }

    public static long stored(ItemStack drive) {
        long n = 0;
        for (FluidStack f : contents(drive)) n += f.getAmount();
        return n;
    }

    static int indexOf(List<FluidStack> list, FluidStack fluid) {
        for (int i = 0; i < list.size(); i++) if (FluidStack.isSameFluidSameComponents(list.get(i), fluid)) return i;
        return -1;
    }

    /** Puts up to {@code amount} mB of {@code fluid} in; returns how much went in. */
    public static int insert(ItemStack drive, FluidStack fluid, int amount, boolean simulate) {
        if (!(drive.getItem() instanceof FluidDriveItem d) || fluid.isEmpty() || amount <= 0) return 0;
        List<FluidStack> list = contents(drive);
        int room = (int) Math.min(Integer.MAX_VALUE, d.capacity - stored(drive));
        int index = indexOf(list, fluid);
        if (index < 0 && list.size() >= TYPES) return 0;
        int moved = Math.min(room, amount);
        if (moved <= 0 || simulate) return Math.max(0, moved);
        List<FluidStack> next = new ArrayList<>(list);
        if (index < 0) next.add(fluid.copyWithAmount(moved));
        else next.set(index, fluid.copyWithAmount(list.get(index).getAmount() + moved));
        drive.set(JSDataComponents.FLUID_DRIVE.get(), new Contents(next));
        return moved;
    }

    /** Takes up to {@code amount} mB of {@code fluid} out; returns how much came out. */
    public static int extract(ItemStack drive, FluidStack fluid, int amount, boolean simulate) {
        List<FluidStack> list = contents(drive);
        int index = indexOf(list, fluid);
        if (index < 0 || amount <= 0) return 0;
        int have = list.get(index).getAmount(), moved = Math.min(have, amount);
        if (simulate) return moved;
        List<FluidStack> next = new ArrayList<>(list);
        if (moved == have) next.remove(index);
        else next.set(index, fluid.copyWithAmount(have - moved));
        if (next.isEmpty()) drive.remove(JSDataComponents.FLUID_DRIVE.get());
        else drive.set(JSDataComponents.FLUID_DRIVE.get(), new Contents(next));
        return moved;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.drive.fluid", String.format("%,d", stored(stack)), String.format("%,d", capacity))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.joulesmith.drive.types", contents(stack).size(), TYPES).withStyle(ChatFormatting.GRAY));
        for (FluidStack f : contents(stack)) {
            tooltip.add(Component.literal("  ").append(f.getHoverName()).append(String.format(": %,d mB", f.getAmount())).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
