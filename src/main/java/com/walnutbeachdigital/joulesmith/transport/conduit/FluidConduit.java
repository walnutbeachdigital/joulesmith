package com.walnutbeachdigital.joulesmith.transport.conduit;

import com.walnutbeachdigital.joulesmith.transport.PipeMode;
import com.walnutbeachdigital.joulesmith.transport.PipeNetwork;
import com.walnutbeachdigital.joulesmith.transport.fluid.FluidNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A fluid pipe (100 mB/t at tier 1, then 400, 1,600, 6,400 and 25,600). While part of a network the fluid
 * lives in the network; this pipe only keeps its share between rebuilds and in the save file.
 */
public class FluidConduit extends PipeConduit {
    private FluidStack held = FluidStack.EMPTY;

    public FluidConduit(int tier) {
        super(ConduitType.FLUID, tier);
    }

    @Override
    protected boolean hasTarget(Level level, BlockPos other, Direction face) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, other, face) != null;
    }

    @Override
    protected PipeNetwork createNetwork() {
        return FluidNetwork.create(level(), pos());
    }

    public FluidStack takeHeld() {
        FluidStack out = held;
        held = FluidStack.EMPTY;
        if (!out.isEmpty() && host != null) host.changed(false);
        return out;
    }

    public void hold(FluidStack stack) {
        held = stack.copy();
        if (host != null) host.changed(false);
    }

    /** What other blocks see: the network's intake, which accepts fluid but never gives it out. */
    public IFluidHandler fluidHandler(Direction side) {
        if (side != null && mode(side) == PipeMode.DISABLED) return null;
        return network() instanceof FluidNetwork net ? net.intake() : null;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        super.save(tag, registries);
        FluidStack share = network != null && network.valid() && network instanceof FluidNetwork net ? net.shareFor(this) : held;
        if (!share.isEmpty()) tag.put("Fluid", share.save(registries));
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        super.load(tag, registries);
        held = tag.contains("Fluid") ? FluidStack.parseOptional(registries, tag.getCompound("Fluid")) : FluidStack.EMPTY;
    }
}
