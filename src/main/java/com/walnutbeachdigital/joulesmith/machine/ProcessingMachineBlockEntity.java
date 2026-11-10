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

    public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, List<SlotDef> slots, MachineRecipeKind kind) {
        super(type, pos, state, slots, kind);
    }
}
