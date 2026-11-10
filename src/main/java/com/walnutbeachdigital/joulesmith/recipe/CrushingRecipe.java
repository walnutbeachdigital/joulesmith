package com.walnutbeachdigital.joulesmith.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.walnutbeachdigital.joulesmith.registry.JSRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A Crusher recipe, written as JSON so modpacks can change it:
 * <pre>{"type": "joulesmith:crushing", "ingredient": {"tag": "c:raw_materials/tin"},
 *  "result": {"id": "joulesmith:tin_dust", "count": 2}, "energy": 2000, "time": 100}</pre>
 * {@code energy} (FE in total) and {@code time} (ticks) are optional.
 */
public class CrushingRecipe implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_ENERGY = 2000, DEFAULT_TIME = 100;

    private final Ingredient ingredient;
    private final ItemStack result;
    private final int energy;
    private final int time;

    public CrushingRecipe(Ingredient ingredient, ItemStack result, int energy, int time) {
        this.ingredient = ingredient;
        this.result = result;
        this.energy = energy;
        this.time = time;
    }

    public Ingredient ingredient() { return ingredient; }
    public ItemStack result() { return result; }
    public int energy() { return energy; }
    public int time() { return time; }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public boolean isSpecial() {
        return true;      // keeps it out of the vanilla recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return JSRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return JSRecipes.CRUSHING.get();
    }

    public static class Serializer implements RecipeSerializer<CrushingRecipe> {
        private static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrushingRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
                Codec.INT.optionalFieldOf("energy", DEFAULT_ENERGY).forGetter(CrushingRecipe::energy),
                Codec.INT.optionalFieldOf("time", DEFAULT_TIME).forGetter(CrushingRecipe::time)
        ).apply(i, CrushingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::ingredient,
                ItemStack.STREAM_CODEC, CrushingRecipe::result,
                ByteBufCodecs.VAR_INT, CrushingRecipe::energy,
                ByteBufCodecs.VAR_INT, CrushingRecipe::time,
                CrushingRecipe::new);

        @Override
        public MapCodec<CrushingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
