package com.walnutbeachdigital.joulesmith.recipe;

import java.util.Arrays;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** A recipe input and how many of it: {@code {"tag": "c:ingots/copper", "count": 3}}. Count defaults to 1. */
public record CountedIngredient(Ingredient ingredient, int count) {
    public static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(i -> i.group(
            Ingredient.MAP_CODEC_NONEMPTY.forGetter(CountedIngredient::ingredient),
            Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
    ).apply(i, CountedIngredient::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CountedIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CountedIngredient::ingredient,
            ByteBufCodecs.VAR_INT, CountedIngredient::count,
            CountedIngredient::new);

    /** Whether {@code stack} is this ingredient and there is enough of it. */
    public boolean test(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    /** Every item this ingredient accepts, at the right count (for JEI). */
    public List<ItemStack> displayStacks() {
        return Arrays.stream(ingredient.getItems()).map(s -> s.copyWithCount(count)).toList();
    }
}
