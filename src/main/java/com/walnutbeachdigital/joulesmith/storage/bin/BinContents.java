package com.walnutbeachdigital.joulesmith.storage.bin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/** What a Bulk Bin holds: one kind of item and how many. Stored on the bin's item when it is broken. */
public record BinContents(ItemStack item, int count) {
    public static final BinContents EMPTY = new BinContents(ItemStack.EMPTY, 0);

    public static final Codec<BinContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(BinContents::item),
            Codec.INT.fieldOf("count").forGetter(BinContents::count)
    ).apply(i, BinContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BinContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, BinContents::item,
            ByteBufCodecs.VAR_INT, BinContents::count,
            BinContents::new);

    public BinContents {
        item = item.copyWithCount(1);
    }

    public boolean isEmpty() {
        return item.isEmpty() || count <= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BinContents other && count == other.count && ItemStack.isSameItemSameComponents(item, other.item);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(item) * 31 + count;
    }
}
