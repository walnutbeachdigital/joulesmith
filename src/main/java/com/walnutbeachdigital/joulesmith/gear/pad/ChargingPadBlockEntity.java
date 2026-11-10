package com.walnutbeachdigital.joulesmith.gear.pad;

import com.walnutbeachdigital.joulesmith.energy.JSEnergyStorage;
import com.walnutbeachdigital.joulesmith.gear.powered.PoweredItem;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Holds a buffer of power and pours up to {@link #RATE} FE per tick into the gear of whoever stands on it. */
public class ChargingPadBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000, RATE = 4000;

    private final JSEnergyStorage energy = new JSEnergyStorage(CAPACITY, RATE, 0, this::setChanged);

    public ChargingPadBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.CHARGING_PAD.get(), pos, state);
    }

    public JSEnergyStorage energyHandler() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChargingPadBlockEntity pad) {
        boolean charging = false;
        if (pad.energy.getEnergyStored() > 0) {
            for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).expandTowards(0, 1, 0))) {
                int budget = Math.min(RATE, pad.energy.getEnergyStored());
                if (budget <= 0) break;
                int given = PoweredItem.chargeInventory(player, budget, true, null);
                if (given > 0) {
                    pad.energy.consume(given);
                    charging = true;
                }
            }
        }
        if (state.getValue(ChargingPadBlock.LIT) != charging) {
            level.setBlock(pos, state.setValue(ChargingPadBlock.LIT, charging), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
    }
}
