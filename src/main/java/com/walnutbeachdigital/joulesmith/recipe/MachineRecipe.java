package com.walnutbeachdigital.joulesmith.recipe;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One recipe for any Joulesmith machine, written as JSON so modpacks can change it:
 * <pre>{
 *   "type": "joulesmith:alloy_smelting",
 *   "inputs": [{"tag": "c:ingots/copper", "count": 3}, {"tag": "c:ingots/tin"}],
 *   "results": [{"id": "joulesmith:bronze_ingot", "count": 4}],
 *   "energy": 4000, "time": 160
 * }</pre>
 * Results may have a {@code chance} (0 to 1) for byproducts. {@code fluid} and {@code fluid_result}
 * ({@code {"id": "minecraft:water", "amount": 1000}}) are for machines with tanks. {@code energy} is the FE
 * for the whole run and {@code time} its length in ticks; both have defaults per machine.
 */
public class MachineRecipe implements Recipe<MachineInput> {
    private final MachineRecipeKind kind;
    private final List<CountedIngredient> inputs;
    private final FluidStack fluid;
    private final List<ChanceResult> results;
    private final FluidStack fluidResult;
    private final int energy;
    private final int time;

    public MachineRecipe(MachineRecipeKind kind, List<CountedIngredient> inputs, FluidStack fluid,
                         List<ChanceResult> results, FluidStack fluidResult, int energy, int time) {
        this.kind = kind;
        this.inputs = List.copyOf(inputs);
        this.fluid = fluid;
        this.results = List.copyOf(results);
        this.fluidResult = fluidResult;
        this.energy = energy;
        this.time = time;
    }

    public MachineRecipeKind kind() { return kind; }
    public List<CountedIngredient> inputs() { return inputs; }
    public FluidStack fluid() { return fluid; }
    public List<ChanceResult> results() { return results; }
    public FluidStack fluidResult() { return fluidResult; }
    public int energy() { return energy; }
    public int time() { return time; }

    /**
     * Which input slot each ingredient takes from (ingredient i uses slot {@code result[i]}), or null if the
     * slots don't hold this recipe. Every filled slot must be used, so a stray item blocks the recipe.
     */
    public int[] assign(List<ItemStack> slots) {
        int[] chosen = new int[inputs.size()];
        boolean[] used = new boolean[slots.size()];
        if (!assign(slots, 0, chosen, used)) return null;
        for (int s = 0; s < slots.size(); s++) {
            if (!used[s] && !slots.get(s).isEmpty()) return null;
        }
        return chosen;
    }

    private boolean assign(List<ItemStack> slots, int i, int[] chosen, boolean[] used) {
        if (i == inputs.size()) return true;
        for (int s = 0; s < slots.size(); s++) {
            if (used[s] || !inputs.get(i).test(slots.get(s))) continue;
            used[s] = true;
            chosen[i] = s;
            if (assign(slots, i + 1, chosen, used)) return true;
            used[s] = false;
        }
        return false;
    }

    /** Whether an item could be part of this recipe at all (for hoppers, pipes and shift-click). */
    public boolean accepts(ItemStack stack) {
        for (CountedIngredient input : inputs) {
            if (input.ingredient().test(stack)) return true;
        }
        return false;
    }

    @Override
    public boolean matches(MachineInput input, Level level) {
        if (!fluid.isEmpty() && (!FluidStack.isSameFluidSameComponents(fluid, input.fluid()) || input.fluid().getAmount() < fluid.getAmount())) {
            return false;
        }
        return assign(input.items()) != null;
    }

    @Override
    public ItemStack assemble(MachineInput input, HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        inputs.forEach(c -> list.add(c.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;      // keeps it out of the vanilla recipe book
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return kind.type();
    }

    /** One serializer per kind; the kind isn't in the JSON, it comes from the recipe's "type". */
    public static class Serializer implements RecipeSerializer<MachineRecipe> {
        private final MapCodec<MachineRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec;

        public Serializer(MachineRecipeKind kind) {
            this.codec = RecordCodecBuilder.mapCodec(i -> i.group(
                    CountedIngredient.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(MachineRecipe::inputs),
                    FluidStack.OPTIONAL_CODEC.optionalFieldOf("fluid", FluidStack.EMPTY).forGetter(MachineRecipe::fluid),
                    ChanceResult.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(MachineRecipe::results),
                    FluidStack.OPTIONAL_CODEC.optionalFieldOf("fluid_result", FluidStack.EMPTY).forGetter(MachineRecipe::fluidResult),
                    Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("energy", kind.defaultEnergy()).forGetter(MachineRecipe::energy),
                    Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("time", kind.defaultTime()).forGetter(MachineRecipe::time)
            ).apply(i, (inputs, fluid, results, fluidResult, energy, time) ->
                    new MachineRecipe(kind, inputs, fluid, results, fluidResult, energy, time)));
            this.streamCodec = StreamCodec.composite(
                    CountedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::inputs,
                    FluidStack.OPTIONAL_STREAM_CODEC, MachineRecipe::fluid,
                    ChanceResult.STREAM_CODEC.apply(ByteBufCodecs.list()), MachineRecipe::results,
                    FluidStack.OPTIONAL_STREAM_CODEC, MachineRecipe::fluidResult,
                    ByteBufCodecs.VAR_INT, MachineRecipe::energy,
                    ByteBufCodecs.VAR_INT, MachineRecipe::time,
                    (inputs, fluid, results, fluidResult, energy, time) ->
                            new MachineRecipe(kind, inputs, fluid, results, fluidResult, energy, time));
        }

        @Override
        public MapCodec<MachineRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
