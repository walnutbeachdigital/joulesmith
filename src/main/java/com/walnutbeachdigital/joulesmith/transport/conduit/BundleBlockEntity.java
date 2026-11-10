package com.walnutbeachdigital.joulesmith.transport.conduit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/** A conduit bundle: up to one item duct, fluid pipe, energy cable and network cable sharing a block. */
public class BundleBlockEntity extends BlockEntity {
    private final Map<ConduitType, Conduit> conduits = new EnumMap<>(ConduitType.class);
    private BundleView view = BundleView.EMPTY;

    public BundleBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.CONDUIT_BUNDLE.get(), pos, state);
    }

    /** The conduit of a kind at a position, if there's a bundle there holding one. */
    public static Conduit conduit(BlockGetter level, BlockPos pos, ConduitType type) {
        return level.getBlockEntity(pos) instanceof BundleBlockEntity bundle ? bundle.get(type) : null;
    }

    public Conduit get(ConduitType type) {
        return conduits.get(type);
    }

    public boolean has(ConduitType type) {
        return conduits.containsKey(type);
    }

    public Collection<Conduit> all() {
        return conduits.values();
    }

    public boolean isEmpty() {
        return conduits.isEmpty();
    }

    public BundleView view() {
        return view;
    }

    /** Puts a conduit in, joins it up and tells the neighbours. */
    public void add(Conduit conduit) {
        conduit.host = this;
        conduits.put(conduit.type, conduit);
        conduit.recompute();
        afterChange();
    }

    /** Takes a conduit out (removing the block if it was the last) and returns it. */
    public Conduit remove(ConduitType type) {
        Conduit conduit = conduits.remove(type);
        if (conduit == null) return null;
        conduit.invalidateNetwork();
        if (conduits.isEmpty()) {
            level.removeBlock(worldPosition, false);
        } else {
            afterChange();
        }
        return conduit;
    }

    private void afterChange() {
        if (level == null || level.isClientSide) return;
        level.invalidateCapabilities(worldPosition);
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof BundleBlockEntity other) other.refreshConnections();
        }
        refreshConnections();
        changed(true);
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    /** Works out how every conduit joins again (after a neighbour changed); sends the new look if it did. */
    public void refreshConnections() {
        if (level == null || level.isClientSide) return;
        boolean changed = false;
        for (Conduit c : conduits.values()) changed |= c.recompute();
        if (changed) changed(true);
    }

    /** Marks the bundle for saving, and if {@code redraw}, sends its new look to the players' games. */
    void changed(boolean redraw) {
        view = BundleView.of(conduits.values());
        setChanged();
        if (redraw && level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BundleBlockEntity bundle) {
        for (Conduit c : bundle.conduits.values()) c.tick(level);
    }

    // ------------------------------------------------------------------ what other blocks see

    public IItemHandler itemHandler(Direction side) {
        return get(ConduitType.ITEM) instanceof ItemConduit c ? c.itemHandler(side) : null;
    }

    public IFluidHandler fluidHandler(Direction side) {
        return get(ConduitType.FLUID) instanceof FluidConduit c ? c.fluidHandler(side) : null;
    }

    public IEnergyStorage energyHandler(Direction side) {
        return get(ConduitType.ENERGY) instanceof EnergyConduit c ? c.energyHandler(side) : null;
    }

    @Override
    public void setRemoved() {
        for (Conduit c : conduits.values()) c.invalidateNetwork();
        super.setRemoved();
    }

    // ------------------------------------------------------------------ saving and syncing

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Conduit c : conduits.values()) {
            CompoundTag t = new CompoundTag();
            c.save(t, registries);
            list.add(t);
        }
        tag.put("Conduits", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        List<Conduit> loaded = new ArrayList<>();
        ListTag list = tag.getList("Conduits", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Conduit c = Conduit.read(list.getCompound(i), registries);
            if (c != null) loaded.add(c);
        }
        for (Conduit old : conduits.values()) old.invalidateNetwork();
        conduits.clear();
        for (Conduit c : loaded) {
            c.host = this;
            conduits.put(c.type, c);
        }
        view = BundleView.of(conduits.values());
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(BundleView.PROPERTY, view).build();
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
