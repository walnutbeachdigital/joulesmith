package com.walnutbeachdigital.joulesmith.registry;

import java.util.EnumMap;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.material.Metal;

import com.walnutbeachdigital.joulesmith.energy.cable.EnergyCableBlock;
import com.walnutbeachdigital.joulesmith.machine.MachineBlock;
import com.walnutbeachdigital.joulesmith.machine.SimpleMachineBlock;
import com.walnutbeachdigital.joulesmith.machine.battery.BatteryBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.ProcessingMachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.generator.CombustionGeneratorBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.creative.CreativePowerCellBlock;
import com.walnutbeachdigital.joulesmith.machine.furnace.ElectricFurnaceBlock;
import com.walnutbeachdigital.joulesmith.machine.pump.PumpBlockEntity;
import com.walnutbeachdigital.joulesmith.storage.tank.FluidTankBlock;
import com.walnutbeachdigital.joulesmith.transport.fluid.FluidPipeBlock;
import com.walnutbeachdigital.joulesmith.transport.item.ItemDuctBlock;

import java.util.function.Supplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JSBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Joulesmith.MODID);

    public static final Map<Metal, DeferredBlock<Block>> ORE = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredBlock<Block>> DEEPSLATE_ORE = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredBlock<Block>> RAW_BLOCK = new EnumMap<>(Metal.class);
    public static final Map<Metal, DeferredBlock<Block>> STORAGE_BLOCK = new EnumMap<>(Metal.class);

    static {
        for (Metal metal : Metal.values()) {
            String id = metal.id();
            if (metal.hasOre()) {
                ORE.put(metal, BLOCKS.registerSimpleBlock(id + "_ore",
                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE)));
                DEEPSLATE_ORE.put(metal, BLOCKS.registerSimpleBlock("deepslate_" + id + "_ore",
                        BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)));
                RAW_BLOCK.put(metal, BLOCKS.registerSimpleBlock("raw_" + id + "_block",
                        BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK)));
            }
            if (metal.hasIngot()) {
                STORAGE_BLOCK.put(metal, BLOCKS.registerSimpleBlock(id + "_block",
                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
            }
        }
    }

    // ---- machines
    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f, 6.0f)
                .requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    public static final DeferredBlock<ElectricFurnaceBlock> ELECTRIC_FURNACE = BLOCKS.register("electric_furnace",
            () -> new ElectricFurnaceBlock(machine().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0)));

    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> CRUSHER = processing("crusher", () -> JSBlockEntities.CRUSHER.get());
    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> ALLOY_SMELTER = processing("alloy_smelter", () -> JSBlockEntities.ALLOY_SMELTER.get());
    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> COMPRESSOR = processing("compressor", () -> JSBlockEntities.COMPRESSOR.get());
    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> WIRE_DRAWER = processing("wire_drawer", () -> JSBlockEntities.WIRE_DRAWER.get());
    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> SAWMILL = processing("sawmill", () -> JSBlockEntities.SAWMILL.get());

    /** A recipe machine block; its block entity type knows the slots and recipes. */
    private static DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> processing(
            String name, Supplier<BlockEntityType<ProcessingMachineBlockEntity>> type) {
        return BLOCKS.register(name, () -> new SimpleMachineBlock<>(machine().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 9 : 0),
                type, (pos, state) -> type.get().create(pos, state)));
    }

    public static final DeferredBlock<SimpleMachineBlock<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR = BLOCKS.register("combustion_generator",
            () -> new SimpleMachineBlock<>(machine().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0),
                    JSBlockEntities.COMBUSTION_GENERATOR::get, CombustionGeneratorBlockEntity::new));

    public static final DeferredBlock<SimpleMachineBlock<BatteryBlockEntity>> BASIC_BATTERY = BLOCKS.register("basic_battery",
            () -> new SimpleMachineBlock<>(machine(), JSBlockEntities.BASIC_BATTERY::get, BatteryBlockEntity::new));

    public static final DeferredBlock<EnergyCableBlock> ENERGY_CABLE = BLOCKS.register("energy_cable",
            () -> new EnergyCableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.5f)
                    .sound(SoundType.WOOL).noOcclusion()));

    // ---- transport and fluid storage
    private static BlockBehaviour.Properties pipe(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(0.6f).sound(SoundType.METAL).noOcclusion();
    }

    public static final DeferredBlock<ItemDuctBlock> ITEM_DUCT = BLOCKS.register("item_duct",
            () -> new ItemDuctBlock(1, pipe(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<ItemDuctBlock> REINFORCED_ITEM_DUCT = BLOCKS.register("reinforced_item_duct",
            () -> new ItemDuctBlock(2, pipe(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<FluidPipeBlock> FLUID_PIPE = BLOCKS.register("fluid_pipe",
            () -> new FluidPipeBlock(1, pipe(MapColor.METAL)));
    public static final DeferredBlock<FluidPipeBlock> REINFORCED_FLUID_PIPE = BLOCKS.register("reinforced_fluid_pipe",
            () -> new FluidPipeBlock(2, pipe(MapColor.METAL)));

    public static final DeferredBlock<FluidTankBlock> FLUID_TANK = BLOCKS.register("fluid_tank",
            () -> new FluidTankBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f)
                    .requiresCorrectToolForDrops().sound(SoundType.GLASS).noOcclusion()
                    .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));

    public static final DeferredBlock<SimpleMachineBlock<PumpBlockEntity>> PUMP = BLOCKS.register("pump",
            () -> new SimpleMachineBlock<>(machine(), JSBlockEntities.PUMP::get, PumpBlockEntity::new));

    public static final DeferredBlock<CreativePowerCellBlock> CREATIVE_POWER_CELL = BLOCKS.register("creative_power_cell",
            () -> new CreativePowerCellBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3_600_000.0f).noLootTable().sound(SoundType.METAL).lightLevel(state -> 7)));

    private JSBlocks() {}
}
