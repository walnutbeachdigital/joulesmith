package com.walnutbeachdigital.joulesmith.network;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** A crafting recipe recorded on a Pattern: the nine grid slots (one item each, or empty) and what it makes. */
public record PatternContents(List<ItemStack> grid, ItemStack output) {
    public static final Codec<PatternContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("grid").forGetter(PatternContents::grid),
            ItemStack.CODEC.fieldOf("output").forGetter(PatternContents::output)
    ).apply(i, PatternContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PatternContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(9)), PatternContents::grid,
            ItemStack.STREAM_CODEC, PatternContents::output,
            PatternContents::new);

    public PatternContents {
        List<ItemStack> copy = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = i < grid.size() ? grid.get(i) : ItemStack.EMPTY;
            copy.add(s.isEmpty() ? ItemStack.EMPTY : s.copyWithCount(1));
        }
        grid = List.copyOf(copy);
        output = output.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PatternContents p) || !ItemStack.matches(output, p.output)) return false;
        for (int i = 0; i < 9; i++) if (!ItemStack.matches(grid.get(i), p.grid.get(i))) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int h = ItemStack.hashItemAndComponents(output);
        for (ItemStack s : grid) h = h * 31 + (s.isEmpty() ? 0 : ItemStack.hashItemAndComponents(s));
        return h;
    }
}
