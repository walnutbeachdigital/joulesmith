package com.walnutbeachdigital.joulesmith.storage.tank;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;
import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * A tier 1 Fluid Tank holding 16,000 mB. Tanks stacked on top of each other act as one: fluid fills the
 * bottom tank first and drains from the top. Keeps its fluid when broken.
 */
public class FluidTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16_000;
    private static final int MAX_COLUMN = 64;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            dirty = true;
        }
    };
    private boolean dirty;

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.FLUID_TANK.get(), pos, state);
    }

    public FluidTank tank() {
        return tank;
    }

    /** This tank and every tank stacked directly above and below it, bottom first. */
    private List<FluidTankBlockEntity> column() {
        List<FluidTankBlockEntity> column = new ArrayList<>();
        if (level == null) {
            column.add(this);
            return column;
        }
        BlockPos bottom = worldPosition;
        for (int i = 0; i < MAX_COLUMN && level.getBlockEntity(bottom.below()) instanceof FluidTankBlockEntity; i++) bottom = bottom.below();
        BlockPos pos = bottom;
        for (int i = 0; i < MAX_COLUMN * 2 && level.getBlockEntity(pos) instanceof FluidTankBlockEntity t; i++) {
            column.add(t);
            pos = pos.above();
        }
        return column;
    }

    /** The fluid in the whole column (one kind only) and how much there is. */
    public FluidStack columnFluid() {
        FluidStack total = FluidStack.EMPTY;
        for (FluidTankBlockEntity t : column()) {
            FluidStack f = t.tank.getFluid();
            if (f.isEmpty()) continue;
            if (total.isEmpty()) total = f.copy();
            else if (FluidStack.isSameFluidSameComponents(total, f)) total.grow(f.getAmount());
        }
        return total;
    }

    public int columnCapacity() {
        return column().size() * CAPACITY;
    }

    /** What pipes, pumps and buckets see: the whole column as one tank. */
    public IFluidHandler columnHandler() {
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int i) { return columnFluid(); }
            @Override public int getTankCapacity(int i) { return columnCapacity(); }
            @Override public boolean isFluidValid(int i, FluidStack stack) { return true; }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) return 0;
                FluidStack existing = columnFluid();
                if (!existing.isEmpty() && !FluidStack.isSameFluidSameComponents(existing, resource)) return 0;
                int left = resource.getAmount();
                for (FluidTankBlockEntity t : column()) {
                    if (left <= 0) break;
                    left -= t.tank.fill(resource.copyWithAmount(left), action);
                }
                return resource.getAmount() - left;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                FluidStack existing = columnFluid();
                if (resource.isEmpty() || !FluidStack.isSameFluidSameComponents(existing, resource)) return FluidStack.EMPTY;
                return drain(resource.getAmount(), action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                List<FluidTankBlockEntity> column = column();
                FluidStack out = FluidStack.EMPTY;
                for (int i = column.size() - 1; i >= 0 && maxDrain > 0; i--) {
                    FluidStack got = column.get(i).tank.drain(maxDrain, action);
                    if (got.isEmpty()) continue;
                    if (out.isEmpty()) out = got.copy();
                    else out.grow(got.getAmount());
                    maxDrain -= got.getAmount();
                }
                return out;
            }
        };
    }

    /** Sends the tank's contents to nearby players a few times a second while it changes. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity tank) {
        if (tank.dirty && level.getGameTime() % 4 == 0) {
            tank.dirty = false;
            level.sendBlockUpdated(pos, state, state, 3);
            level.updateNeighbourForOutputSignal(pos, state.getBlock());
        }
    }

    // ------------------------------------------------------------------ saving and item form

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) components.set(JSDataComponents.FLUID.get(), SimpleFluidContent.copyOf(tank.getFluid()));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        tank.setFluid(input.getOrDefault(JSDataComponents.FLUID.get(), SimpleFluidContent.EMPTY).copy());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Tank");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
