package com.walnutbeachdigital.joulesmith.recipe;

import java.util.Map;

import com.walnutbeachdigital.joulesmith.registry.JSBlocks;

import net.minecraft.world.level.ItemLike;

/** Which block runs each kind of machine recipe (for JEI and the Engineer's Handbook). */
public final class MachineStations {
    public static Map<MachineRecipeKind, ItemLike> all() {
        return Map.ofEntries(Map.entry(MachineRecipeKind.BIO_REACTING, JSBlocks.BIO_REACTOR.get()),
                Map.entry(MachineRecipeKind.CRUSHING, JSBlocks.CRUSHER.get()),
                Map.entry(MachineRecipeKind.ALLOY_SMELTING, JSBlocks.ALLOY_SMELTER.get()),
                Map.entry(MachineRecipeKind.COMPRESSING, JSBlocks.COMPRESSOR.get()),
                Map.entry(MachineRecipeKind.WIRE_DRAWING, JSBlocks.WIRE_DRAWER.get()),
                Map.entry(MachineRecipeKind.SAWING, JSBlocks.SAWMILL.get()),
                Map.entry(MachineRecipeKind.WASHING, JSBlocks.ORE_WASHER.get()),
                Map.entry(MachineRecipeKind.MIXING, JSBlocks.FLUID_MIXER.get()),
                Map.entry(MachineRecipeKind.CENTRIFUGING, JSBlocks.CENTRIFUGE.get()),
                Map.entry(MachineRecipeKind.CIRCUIT_ASSEMBLING, JSBlocks.CIRCUIT_ASSEMBLER.get()),
                Map.entry(MachineRecipeKind.DISSOLVING, JSBlocks.CHEMICAL_DISSOLVER.get()),
                Map.entry(MachineRecipeKind.CRYSTALLIZING, JSBlocks.CRYSTALLIZER.get()),
                Map.entry(MachineRecipeKind.FERTILIZER_MIXING, JSBlocks.FERTILIZER_MIXER.get()),
                Map.entry(MachineRecipeKind.ENGRAVING, JSBlocks.LASER_ENGRAVER.get()),
                Map.entry(MachineRecipeKind.REFINING, JSBlocks.PLASMA_REFINERY.get()));
    }

    private MachineStations() {}
}
