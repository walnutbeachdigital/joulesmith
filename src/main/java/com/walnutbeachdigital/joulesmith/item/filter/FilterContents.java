package com.walnutbeachdigital.joulesmith.item.filter;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What a Filter Card lets through: up to nine items, as a whitelist (only these pass) or a blacklist
 * (everything but these passes). Stored on the card as a data component.
 */
public record FilterContents(List<ItemStack> items, boolean blacklist) {
    public static final int SIZE = 9;
    public static final FilterContents EMPTY = new FilterContents(List.of(), false);

    public static final Codec<FilterContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(FilterContents::items),
            Codec.BOOL.fieldOf("blacklist").forGetter(FilterContents::blacklist)
    ).apply(i, FilterContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, FilterContents::items,
            ByteBufCodecs.BOOL, FilterContents::blacklist,
            FilterContents::new);

    public FilterContents {
        List<ItemStack> copy = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) {
            copy.add(i < items.size() ? items.get(i).copyWithCount(1) : ItemStack.EMPTY);
        }
        items = List.copyOf(copy);
    }

    /** Whether an item may pass this filter. */
    public boolean test(ItemStack stack) {
        boolean listed = false;
        for (ItemStack item : items) {
            if (!item.isEmpty() && ItemStack.isSameItem(item, stack)) {
                listed = true;
                break;
            }
        }
        return blacklist != listed;
    }

    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    // ItemStack has no equals of its own, so compare the lists the way vanilla's item containers do.
    @Override
    public boolean equals(Object o) {
        return o instanceof FilterContents other && blacklist == other.blacklist && ItemStack.listMatches(items, other.items);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(items) * 31 + Boolean.hashCode(blacklist);
    }
}
