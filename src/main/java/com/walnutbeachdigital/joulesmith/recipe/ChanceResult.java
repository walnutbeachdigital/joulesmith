package com.walnutbeachdigital.joulesmith.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * A recipe output that may only come out some of the time:
 * {@code {"id": "joulesmith:sawdust", "count": 1, "chance": 0.5}}. Count and chance default to 1.
 */
public record ChanceResult(ItemStack stack, float chance) {
    public static final Codec<ChanceResult> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(r -> r.stack().getItemHolder()),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(r -> r.stack().getCount()),
            Codec.floatRange(0f, 1f).optionalFieldOf("chance", 1f).forGetter(ChanceResult::chance)
    ).apply(i, (item, count, chance) -> new ChanceResult(new ItemStack(item, count), chance)));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChanceResult> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, ChanceResult::stack,
            ByteBufCodecs.FLOAT, ChanceResult::chance,
            ChanceResult::new);

    public static ChanceResult always(ItemStack stack) {
        return new ChanceResult(stack, 1f);
    }

    public boolean certain() {
        return chance >= 1f;
    }
}
