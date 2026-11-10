package com.walnutbeachdigital.joulesmith.registry;

import java.util.EnumMap;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.material.Metal;

import com.walnutbeachdigital.joulesmith.energy.cable.EnergyCableBlock;
import com.walnutbeachdigital.joulesmith.machine.MachineBlock;
import com.walnutbeachdigital.joulesmith.machine.SimpleMachineBlock;
import com.walnutbeachdigital.joulesmith.machine.battery.BatteryBlockEntity;
import com.walnutbeachdigital.joulesmith.farming.HarvesterBlockEntity;
import com.walnutbeachdigital.joulesmith.farming.PlanterBlockEntity;
import com.walnutbeachdigital.joulesmith.farming.TreeFarmBlockEntity;
import com.walnutbeachdigital.joulesmith.farming.rubber.RubberLogBlock;
import com.walnutbeachdigital.joulesmith.machine.ProcessingMachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.generator.BiofuelGeneratorBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.generator.CombustionGeneratorBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.creative.CreativePowerCellBlock;
import com.walnutbeachdigital.joulesmith.machine.furnace.ElectricFurnaceBlock;
import com.walnutbeachdigital.joulesmith.machine.pump.PumpBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.steam.BoilerBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.steam.SteamTurbineBlockEntity;
import com.walnutbeachdigital.joulesmith.storage.bin.BinBlock;
import com.walnutbeachdigital.joulesmith.storage.crate.CrateBlock;
import com.walnutbeachdigital.joulesmith.storage.tank.FluidTankBlock;
import com.walnutbeachdigital.joulesmith.transport.fluid.FluidPipeBlock;
import com.walnutbeachdigital.joulesmith.transport.item.ItemDuctBlock;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;

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
    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> ORE_WASHER = processing("ore_washer", () -> JSBlockEntities.ORE_WASHER.get());
    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> FLUID_MIXER = processing("fluid_mixer", () -> JSBlockEntities.FLUID_MIXER.get());
    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> CENTRIFUGE = processing("centrifuge", () -> JSBlockEntities.CENTRIFUGE.get());
    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> CIRCUIT_ASSEMBLER = processing("circuit_assembler", () -> JSBlockEntities.CIRCUIT_ASSEMBLER.get());

    public static final DeferredBlock<SimpleMachineBlock<ProcessingMachineBlockEntity>> BIO_REACTOR = processing("bio_reactor", () -> JSBlockEntities.BIO_REACTOR.get());

    // ---- farming
    public static final DeferredBlock<SimpleMachineBlock<PlanterBlockEntity>> PLANTER = BLOCKS.register("planter",
            () -> new SimpleMachineBlock<>(machine(), JSBlockEntities.PLANTER::get, (p, s) -> new PlanterBlockEntity(JSBlockEntities.PLANTER.get(), p, s)));
    public static final DeferredBlock<SimpleMachineBlock<HarvesterBlockEntity>> HARVESTER = BLOCKS.register("harvester",
            () -> new SimpleMachineBlock<>(machine(), JSBlockEntities.HARVESTER::get, (p, s) -> new HarvesterBlockEntity(JSBlockEntities.HARVESTER.get(), p, s)));
    public static final DeferredBlock<SimpleMachineBlock<TreeFarmBlockEntity>> TREE_FARM = BLOCKS.register("tree_farm",
            () -> new SimpleMachineBlock<>(machine(), JSBlockEntities.TREE_FARM::get, (p, s) -> new TreeFarmBlockEntity(JSBlockEntities.TREE_FARM.get(), p, s)));
    public static final DeferredBlock<SimpleMachineBlock<BiofuelGeneratorBlockEntity>> BIOFUEL_GENERATOR = BLOCKS.register("biofuel_generator",
            () -> new SimpleMachineBlock<>(machine().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 11 : 0),
                    JSBlockEntities.BIOFUEL_GENERATOR::get, BiofuelGeneratorBlockEntity::new));

    // ---- rubber trees
    public static final TreeGrower RUBBER_TREE = new TreeGrower(Joulesmith.MODID + ":rubber", Optional.empty(),
            Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE, Joulesmith.id("rubber_tree"))), Optional.empty());
    public static final DeferredBlock<RubberLogBlock> RUBBER_LOG = BLOCKS.register("rubber_log",
            () -> new RubberLogBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.TERRACOTTA_BROWN)));
    public static final DeferredBlock<LeavesBlock> RUBBER_LEAVES = BLOCKS.register("rubber_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<SaplingBlock> RUBBER_SAPLING = BLOCKS.register("rubber_sapling",
            () -> new SaplingBlock(RUBBER_TREE, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<Block> RUBBER_PLANKS = BLOCKS.registerSimpleBlock("rubber_planks",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));

    // ---- storage
    public static final DeferredBlock<CrateBlock> BASIC_CRATE = BLOCKS.register("basic_crate",
            () -> new CrateBlock(6, BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5f)));
    public static final DeferredBlock<CrateBlock> REINFORCED_CRATE = BLOCKS.register("reinforced_crate",
            () -> new CrateBlock(9, BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(3.5f).sound(SoundType.METAL)));
    public static final DeferredBlock<BinBlock> BASIC_BULK_BIN = BLOCKS.register("basic_bulk_bin",
            () -> new BinBlock(4_096, BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(2.5f)));
    public static final DeferredBlock<BinBlock> REINFORCED_BULK_BIN = BLOCKS.register("reinforced_bulk_bin",
            () -> new BinBlock(16_384, BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(3.5f).sound(SoundType.METAL)));

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
            () -> new SimpleMachineBlock<>(machine(), JSBlockEntities.BASIC_BATTERY::get, (p, s) -> new BatteryBlockEntity(p, s, 1)));

    public static final DeferredBlock<EnergyCableBlock> ENERGY_CABLE = cable("energy_cable", 1);
    public static final DeferredBlock<EnergyCableBlock> REINFORCED_ENERGY_CABLE = cable("reinforced_energy_cable", 2);
    public static final DeferredBlock<EnergyCableBlock> ADVANCED_ENERGY_CABLE = cable("advanced_energy_cable", 3);

    private static DeferredBlock<EnergyCableBlock> cable(String name, int tier) {
        return BLOCKS.register(name, () -> new EnergyCableBlock(tier, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                .strength(0.5f).sound(SoundType.WOOL).noOcclusion()));
    }

    public static final DeferredBlock<SimpleMachineBlock<BatteryBlockEntity>> REINFORCED_BATTERY = battery("reinforced_battery", 2);
    public static final DeferredBlock<SimpleMachineBlock<BatteryBlockEntity>> ADVANCED_BATTERY = battery("advanced_battery", 3);

    private static DeferredBlock<SimpleMachineBlock<BatteryBlockEntity>> battery(String name, int tier) {
        return BLOCKS.register(name, () -> new SimpleMachineBlock<>(machine(), JSBlockEntities.BASIC_BATTERY::get,
                (p, s) -> new BatteryBlockEntity(p, s, tier)));
    }

    // ---- steam (tier 3)
    public static final DeferredBlock<SimpleMachineBlock<BoilerBlockEntity>> BOILER = BLOCKS.register("boiler",
            () -> new SimpleMachineBlock<>(machine().lightLevel(state -> state.getValue(MachineBlock.LIT) ? 12 : 0),
                    JSBlockEntities.BOILER::get, BoilerBlockEntity::new));
    public static final DeferredBlock<SimpleMachineBlock<SteamTurbineBlockEntity>> STEAM_TURBINE = BLOCKS.register("steam_turbine",
            () -> new SimpleMachineBlock<>(machine(), JSBlockEntities.STEAM_TURBINE::get, SteamTurbineBlockEntity::new));

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
    public static final DeferredBlock<ItemDuctBlock> ADVANCED_ITEM_DUCT = BLOCKS.register("advanced_item_duct",
            () -> new ItemDuctBlock(3, pipe(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<FluidPipeBlock> ADVANCED_FLUID_PIPE = BLOCKS.register("advanced_fluid_pipe",
            () -> new FluidPipeBlock(3, pipe(MapColor.METAL)));

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
