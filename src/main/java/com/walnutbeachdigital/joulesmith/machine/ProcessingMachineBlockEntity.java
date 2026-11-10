package com.walnutbeachdigital.joulesmith.machine;

import java.util.List;

import com.walnutbeachdigital.joulesmith.recipe.MachineRecipeKind;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A recipe machine with nothing special about it: the Crusher, Alloy Smelter, Compressor, Wire Drawer and
 * Sawmill are all this class with a different slot layout and recipe kind (see JSBlockEntities).
 */
public class ProcessingMachineBlockEntity extends RecipeMachineBlockEntity {
    /** One input, one main output. */
    public static final List<SlotDef> ONE_TO_ONE = List.of(SlotDef.input(56, 35), SlotDef.output(116, 35));
    /** One input, a main output and a byproduct. */
    public static final List<SlotDef> ONE_TO_TWO = List.of(SlotDef.input(56, 35), SlotDef.output(116, 35), SlotDef.byproduct(142, 35));
    /** Two inputs side by side, one main output. */
    public static final List<SlotDef> TWO_TO_ONE = List.of(SlotDef.input(35, 35), SlotDef.input(56, 35), SlotDef.output(116, 35));

    /** Ore Washer: water tank on the left, one input, a main output and a byproduct. */
    public static final List<SlotDef> WASHER = List.of(SlotDef.input(56, 35), SlotDef.output(116, 35), SlotDef.byproduct(142, 35));
    /** Bio Reactor: one item input beside its tank. */
    public static final List<SlotDef> MIXER = List.of(SlotDef.input(56, 35));
    /** Fluid Mixer: one item input between the two tanks, and an output under the arrow for solid results. */
    public static final List<SlotDef> FLUID_MIXER = List.of(SlotDef.input(56, 35), SlotDef.byproduct(83, 57));
    /** Centrifuge: one input, four outputs in a square. */
    public static final List<SlotDef> CENTRIFUGE = List.of(SlotDef.input(56, 35),
            SlotDef.byproduct(112, 26), SlotDef.byproduct(130, 26), SlotDef.byproduct(112, 44), SlotDef.byproduct(130, 44));
    /** Circuit Assembler: three inputs in a triangle, one output. */
    public static final List<SlotDef> ASSEMBLER = List.of(SlotDef.input(35, 26), SlotDef.input(53, 26), SlotDef.input(44, 44),
            SlotDef.output(116, 35));

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SlotDef> slots, MachineRecipeKind kind) {
        super(type, pos, state, slots, kind);
    }

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SlotDef> slots,
                                        MachineRecipeKind kind, int inputTankX, int outputTankX) {
        super(type, pos, state, slots, kind, inputTankX, outputTankX);
    }
}
