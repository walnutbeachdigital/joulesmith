package com.walnutbeachdigital.joulesmith.registry;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.creative.CreativePowerCellBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes Joulesmith blocks as standard energy, item and fluid handlers, so any mod's cables and pipes work with them. */
public final class JSCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        machine(event, JSBlockEntities.ELECTRIC_FURNACE.get());
        machine(event, JSBlockEntities.CRUSHER.get());
        machine(event, JSBlockEntities.COMBUSTION_GENERATOR.get());
        machine(event, JSBlockEntities.BASIC_BATTERY.get());
        machine(event, JSBlockEntities.PUMP.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.PUMP.get(), (pump, side) -> pump.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JSBlockEntities.ITEM_DUCT.get(), (duct, side) -> duct.itemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.FLUID_PIPE.get(), (pipe, side) -> pipe.fluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.FLUID_TANK.get(), (tank, side) -> tank.columnHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, JSBlockEntities.ENERGY_CABLE.get(),
                (cable, side) -> cable.energyHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, JSBlockEntities.CREATIVE_POWER_CELL.get(),
                (cell, side) -> CreativePowerCellBlockEntity.ENDLESS);
    }

    /** Every machine: its energy by face, and its slots by face for pipes and hoppers. */
    private static <T extends MachineBlockEntity> void machine(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (machine, side) -> machine.energyHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (machine, side) -> machine.itemHandler(side));
    }

    private JSCapabilities() {}
}
