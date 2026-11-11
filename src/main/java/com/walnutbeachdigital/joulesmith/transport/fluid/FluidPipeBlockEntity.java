package com.walnutbeachdigital.joulesmith.transport.fluid;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.transport.PipeBlockEntity;
import com.walnutbeachdigital.joulesmith.transport.PipeMode;
import com.walnutbeachdigital.joulesmith.transport.PipeNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A fluid pipe. While part of a network the fluid lives in the network; this pipe only keeps its share
 * between rebuilds and in the save file.
 */
public class FluidPipeBlockEntity extends PipeBlockEntity {
    private FluidStack held = FluidStack.EMPTY;
    /** What the pipe shows through its glass (any amount; only the kind matters) and how full, in eighths. */
    private FluidStack shown = FluidStack.EMPTY;
    private int shownEighths;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.FLUID_PIPE.get(), pos, state);
    }

    @Override
    protected PipeNetwork createNetwork() {
        return FluidNetwork.create(level, worldPosition);
    }

    FluidStack takeHeld() {
        FluidStack out = held;
        held = FluidStack.EMPTY;
        if (!out.isEmpty()) setChanged();
        return out;
    }

    void hold(FluidStack stack) {
        held = stack.copy();
        setChanged();
    }

    void show(FluidStack fluid, int eighths) {
        if (eighths == shownEighths && FluidStack.isSameFluidSameComponents(fluid, shown)) return;
        shown = fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1);
        shownEighths = fluid.isEmpty() ? 0 : eighths;
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    public FluidStack shown() {
        return shown;
    }

    /** How full the pipe looks, 0 to 1. */
    public float shownFill() {
        return shownEighths / 8f;
    }

    /** What other blocks see: the network's intake, which accepts fluid but never gives it out. */
    public IFluidHandler fluidHandler(Direction side) {
        if (side != null && mode(side) == PipeMode.DISABLED) return null;
        return network() instanceof FluidNetwork net ? net.intake() : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        FluidStack share = network != null && network.valid() && network instanceof FluidNetwork net ? net.shareFor(this) : held;
        if (!share.isEmpty()) tag.put("Fluid", share.save(registries));
        if (!shown.isEmpty()) {
            tag.put("Shown", shown.save(registries));
            tag.putInt("ShownEighths", shownEighths);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        held = tag.contains("Fluid") ? FluidStack.parseOptional(registries, tag.getCompound("Fluid")) : FluidStack.EMPTY;
        shown = tag.contains("Shown") ? FluidStack.parseOptional(registries, tag.getCompound("Shown")) : FluidStack.EMPTY;
        shownEighths = tag.getInt("ShownEighths");
    }
}
