package com.walnutbeachdigital.joulesmith.machine.generator;

import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Burns anything a furnace burns and turns it into power: 40 FE every tick for as long as the fuel would
 * burn in a furnace. Pushes its power out of every face set to give out (all but the front by default).
 */
public class CombustionGeneratorBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 20_000, OUTPUT = 80, GENERATION = 40;

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.COMBUSTION_GENERATOR.get(), pos, state, List.of(SlotDef.input(80, 53)), CAPACITY, 0, OUTPUT);
    }

    private static int burnTime(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        boolean burning = false;
        if (progress <= 0 && allowed && energy.getEnergyStored() < energy.getMaxEnergyStored()) {
            ItemStack fuel = items.getStackInSlot(0);
            int time = burnTime(fuel);
            if (time > 0) {
                progress = maxProgress = time;
                ItemStack remainder = fuel.getCraftingRemainingItem();      // a lava bucket leaves its bucket
                fuel.shrink(1);
                if (fuel.isEmpty() && !remainder.isEmpty()) items.setStackInSlot(0, remainder);
                setChanged();
            }
        }
        if (progress > 0) {
            progress--;
            energy.generate(GENERATION);
            burning = true;
            setChanged();
        }
        pushEnergy(level, pos, OUTPUT);
        return burning;
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return burnTime(stack) > 0;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.FLAME;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTotal", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        maxProgress = tag.getInt("BurnTotal");
    }
}
