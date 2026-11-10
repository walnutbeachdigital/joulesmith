package com.walnutbeachdigital.joulesmith.registry;

import com.walnutbeachdigital.joulesmith.machine.creative.CreativePowerCellBlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes Joulesmith blocks as standard energy and item handlers, so any mod's cables and pipes work with them. */
public final class JSCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, JSBlockEntities.ELECTRIC_FURNACE.get(),
                (machine, side) -> machine.energy());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JSBlockEntities.ELECTRIC_FURNACE.get(),
                (machine, side) -> machine.itemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, JSBlockEntities.CREATIVE_POWER_CELL.get(),
                (cell, side) -> CreativePowerCellBlockEntity.ENDLESS);
    }

    private JSCapabilities() {}
}
