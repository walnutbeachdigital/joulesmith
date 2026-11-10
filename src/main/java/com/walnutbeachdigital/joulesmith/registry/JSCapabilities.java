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
        for (var type : JSBlockEntities.processingMachines()) {
            machine(event, type.get());
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type.get(), (m, side) -> m.fluidHandler(side));
        }
        machine(event, JSBlockEntities.COMBUSTION_GENERATOR.get());
        machine(event, JSBlockEntities.BASIC_BATTERY.get());
        machine(event, JSBlockEntities.PUMP.get());
        machine(event, JSBlockEntities.STEAM_TURBINE.get());
        machine(event, JSBlockEntities.GEOTHERMAL_GENERATOR.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.GEOTHERMAL_GENERATOR.get(), (g, side) -> g.fluidHandler(side));
        machine(event, JSBlockEntities.WIND_TURBINE.get());
        machine(event, JSBlockEntities.FISSION_REACTOR.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.FISSION_REACTOR.get(), (r, side) -> r.fluidHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, JSBlockEntities.REACTOR_PORT.get(), (p, side) -> p.energy);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.REACTOR_PORT.get(), (p, side) -> p.fluids);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JSBlockEntities.REACTOR_PORT.get(), (p, side) -> p.items);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.SPRINKLER.get(), (s, side) -> s.fluidHandler());
        for (var type : java.util.List.of(JSBlockEntities.ANIMAL_RANCHER, JSBlockEntities.HYDROPONIC_BED, JSBlockEntities.MOB_GRINDER,
                JSBlockEntities.AUTO_ENCHANTER)) {
            machine(event, type.get());
        }
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.HYDROPONIC_BED.get(), (m, side) -> m.fluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.MOB_GRINDER.get(), (m, side) -> m.fluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.AUTO_ENCHANTER.get(), (m, side) -> m.fluidHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, JSBlockEntities.SOLAR_PANEL.get(), (panel, side) -> panel.energyHandler());
        machine(event, JSBlockEntities.BLOCK_PLACER.get());
        machine(event, JSBlockEntities.BLOCK_BREAKER.get());
        machine(event, JSBlockEntities.AUTOCRAFTER.get());
        machine(event, JSBlockEntities.FLUID_PLACER.get());
        machine(event, JSBlockEntities.FLUID_DRAIN.get());
        machine(event, JSBlockEntities.MECHANICAL_USER.get());
        machine(event, JSBlockEntities.QUARRY.get());
        machine(event, JSBlockEntities.MOB_FIGHTER.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.FLUID_PLACER.get(), (m, side) -> m.fluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.FLUID_DRAIN.get(), (m, side) -> m.fluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.STEAM_TURBINE.get(), (t, side) -> t.fluidHandler(side));
        // the Boiler holds no power, so cables don't connect to it
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JSBlockEntities.BOILER.get(), (b, side) -> b.itemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.BOILER.get(), (b, side) -> b.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JSBlockEntities.CRATE.get(), (crate, side) -> crate.items());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JSBlockEntities.BULK_BIN.get(), (bin, side) -> bin.handler);
        machine(event, JSBlockEntities.PLANTER.get());
        machine(event, JSBlockEntities.HARVESTER.get());
        machine(event, JSBlockEntities.TREE_FARM.get());
        machine(event, JSBlockEntities.BIOFUEL_GENERATOR.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.BIOFUEL_GENERATOR.get(), (g, side) -> g.fluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.PUMP.get(), (pump, side) -> pump.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, JSBlockEntities.ITEM_DUCT.get(), (duct, side) -> duct.itemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.FLUID_PIPE.get(), (pipe, side) -> pipe.fluidHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, JSBlockEntities.FLUID_TANK.get(), (tank, side) -> tank.columnHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, JSBlockEntities.ENERGY_CABLE.get(),
                (cable, side) -> cable.energyHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, JSBlockEntities.CHARGING_PAD.get(), (pad, side) -> pad.energyHandler());
        for (var holder : JSGear.POWERED) {
            var item = holder.get();
            var powered = (com.walnutbeachdigital.joulesmith.gear.powered.PoweredItem) item;
            event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, context) -> new net.neoforged.neoforge.energy.ComponentEnergyStorage(
                    stack, JSDataComponents.ENERGY.get(), powered.capacity(), powered.transferRate(), powered.transferRate()), item);
        }
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
