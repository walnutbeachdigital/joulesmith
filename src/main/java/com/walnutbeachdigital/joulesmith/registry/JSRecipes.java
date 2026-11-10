package com.walnutbeachdigital.joulesmith.registry;

import java.util.EnumMap;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipe;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipeKind;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Joulesmith's machine recipe types, one per kind of machine. Recipes are JSON files under data/<namespace>/recipe. */
public final class JSRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Joulesmith.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Joulesmith.MODID);

    public static final Map<MachineRecipeKind, DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>>> TYPES_BY_KIND =
            new EnumMap<>(MachineRecipeKind.class);
    public static final Map<MachineRecipeKind, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>>> SERIALIZERS_BY_KIND =
            new EnumMap<>(MachineRecipeKind.class);

    static {
        for (MachineRecipeKind kind : MachineRecipeKind.values()) {
            TYPES_BY_KIND.put(kind, TYPES.register(kind.id(), () -> RecipeType.simple(Joulesmith.id(kind.id()))));
            SERIALIZERS_BY_KIND.put(kind, SERIALIZERS.register(kind.id(), () -> new MachineRecipe.Serializer(kind)));
        }
    }

    private JSRecipes() {}
}
