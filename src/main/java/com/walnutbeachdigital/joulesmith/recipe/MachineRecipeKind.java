package com.walnutbeachdigital.joulesmith.recipe;

import com.walnutbeachdigital.joulesmith.registry.JSRecipes;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Every kind of machine recipe. Each has its own recipe type (so each machine only sees its own recipes)
 * but they all share {@link MachineRecipe}'s JSON format. {@code energy} and {@code time} are the defaults
 * used when a recipe leaves them out.
 */
public enum MachineRecipeKind {
    CRUSHING("crushing", 2000, 100),
    ALLOY_SMELTING("alloy_smelting", 4000, 160),
    COMPRESSING("compressing", 2400, 80),
    WIRE_DRAWING("wire_drawing", 1600, 80),
    SAWING("sawing", 1200, 60),
    WASHING("washing", 2400, 120),
    MIXING("mixing", 2000, 80),
    CENTRIFUGING("centrifuging", 3200, 120),
    CIRCUIT_ASSEMBLING("circuit_assembling", 3000, 100),
    BIO_REACTING("bio_reacting", 800, 40),
    DISSOLVING("dissolving", 4000, 160),
    CRYSTALLIZING("crystallizing", 2400, 120),
    FERTILIZER_MIXING("fertilizer_mixing", 1600, 80);

    private final String id;
    private final int defaultEnergy;
    private final int defaultTime;

    MachineRecipeKind(String id, int defaultEnergy, int defaultTime) {
        this.id = id;
        this.defaultEnergy = defaultEnergy;
        this.defaultTime = defaultTime;
    }

    public String id() { return id; }
    public int defaultEnergy() { return defaultEnergy; }
    public int defaultTime() { return defaultTime; }

    public RecipeType<MachineRecipe> type() {
        return JSRecipes.TYPES_BY_KIND.get(this).get();
    }

    public RecipeSerializer<MachineRecipe> serializer() {
        return JSRecipes.SERIALIZERS_BY_KIND.get(this).get();
    }

    public String titleKey() {
        return "jei.joulesmith." + id;
    }
}
