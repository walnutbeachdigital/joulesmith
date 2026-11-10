package com.walnutbeachdigital.joulesmith.registry;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.energy.cable.EnergyCableBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.battery.BatteryBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.creative.CreativePowerCellBlockEntity;
import com.walnutbeachdigital.joulesmith.farming.HarvesterBlockEntity;
import com.walnutbeachdigital.joulesmith.farming.PlanterBlockEntity;
import com.walnutbeachdigital.joulesmith.farming.TreeFarmBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.ProcessingMachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.generator.BiofuelGeneratorBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.recipe.MachineRecipeKind;
import com.walnutbeachdigital.joulesmith.machine.generator.CombustionGeneratorBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.furnace.ElectricFurnaceBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.pump.PumpBlockEntity;
import com.walnutbeachdigital.joulesmith.storage.bin.BinBlockEntity;
import com.walnutbeachdigital.joulesmith.storage.crate.CrateBlockEntity;
import com.walnutbeachdigital.joulesmith.storage.tank.FluidTankBlockEntity;
import com.walnutbeachdigital.joulesmith.transport.fluid.FluidPipeBlockEntity;
import com.walnutbeachdigital.joulesmith.transport.item.ItemDuctBlockEntity;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class JSBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Joulesmith.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("electric_furnace", () -> BlockEntityType.Builder
                    .of(ElectricFurnaceBlockEntity::new, JSBlocks.ELECTRIC_FURNACE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativePowerCellBlockEntity>> CREATIVE_POWER_CELL =
            BLOCK_ENTITIES.register("creative_power_cell", () -> BlockEntityType.Builder
                    .of(CreativePowerCellBlockEntity::new, JSBlocks.CREATIVE_POWER_CELL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator", () -> BlockEntityType.Builder
                    .of(CombustionGeneratorBlockEntity::new, JSBlocks.COMBUSTION_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BASIC_BATTERY =
            BLOCK_ENTITIES.register("basic_battery", () -> BlockEntityType.Builder
                    .of(BatteryBlockEntity::new, JSBlocks.BASIC_BATTERY.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> CRUSHER =
            processing("crusher", JSBlocks.CRUSHER, ProcessingMachineBlockEntity.ONE_TO_TWO, MachineRecipeKind.CRUSHING);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> ALLOY_SMELTER =
            processing("alloy_smelter", JSBlocks.ALLOY_SMELTER, ProcessingMachineBlockEntity.TWO_TO_ONE, MachineRecipeKind.ALLOY_SMELTING);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> COMPRESSOR =
            processing("compressor", JSBlocks.COMPRESSOR, ProcessingMachineBlockEntity.ONE_TO_ONE, MachineRecipeKind.COMPRESSING);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> WIRE_DRAWER =
            processing("wire_drawer", JSBlocks.WIRE_DRAWER, ProcessingMachineBlockEntity.ONE_TO_ONE, MachineRecipeKind.WIRE_DRAWING);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> SAWMILL =
            processing("sawmill", JSBlocks.SAWMILL, ProcessingMachineBlockEntity.ONE_TO_TWO, MachineRecipeKind.SAWING);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyCableBlockEntity>> ENERGY_CABLE =
            BLOCK_ENTITIES.register("energy_cable", () -> BlockEntityType.Builder
                    .of(EnergyCableBlockEntity::new, JSBlocks.ENERGY_CABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemDuctBlockEntity>> ITEM_DUCT =
            BLOCK_ENTITIES.register("item_duct", () -> BlockEntityType.Builder
                    .of(ItemDuctBlockEntity::new, JSBlocks.ITEM_DUCT.get(), JSBlocks.REINFORCED_ITEM_DUCT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPipeBlockEntity>> FLUID_PIPE =
            BLOCK_ENTITIES.register("fluid_pipe", () -> BlockEntityType.Builder
                    .of(FluidPipeBlockEntity::new, JSBlocks.FLUID_PIPE.get(), JSBlocks.REINFORCED_FLUID_PIPE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTankBlockEntity>> FLUID_TANK =
            BLOCK_ENTITIES.register("fluid_tank", () -> BlockEntityType.Builder
                    .of(FluidTankBlockEntity::new, JSBlocks.FLUID_TANK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP =
            BLOCK_ENTITIES.register("pump", () -> BlockEntityType.Builder
                    .of(PumpBlockEntity::new, JSBlocks.PUMP.get()).build(null));

    // tier 2
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> ORE_WASHER =
            processing("ore_washer", JSBlocks.ORE_WASHER, ProcessingMachineBlockEntity.WASHER, MachineRecipeKind.WASHING, 30, -1);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> FLUID_MIXER =
            processing("fluid_mixer", JSBlocks.FLUID_MIXER, ProcessingMachineBlockEntity.MIXER, MachineRecipeKind.MIXING, 30, 116);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> CENTRIFUGE =
            processing("centrifuge", JSBlocks.CENTRIFUGE, ProcessingMachineBlockEntity.CENTRIFUGE, MachineRecipeKind.CENTRIFUGING);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> CIRCUIT_ASSEMBLER =
            processing("circuit_assembler", JSBlocks.CIRCUIT_ASSEMBLER, ProcessingMachineBlockEntity.ASSEMBLER, MachineRecipeKind.CIRCUIT_ASSEMBLING);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> BIO_REACTOR =
            processing("bio_reactor", JSBlocks.BIO_REACTOR, ProcessingMachineBlockEntity.MIXER, MachineRecipeKind.BIO_REACTING, -1, 116);

    // farming
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlanterBlockEntity>> PLANTER =
            BLOCK_ENTITIES.register("planter", () -> BlockEntityType.Builder
                    .of((pos, state) -> new PlanterBlockEntity(JSBlockEntities.PLANTER.get(), pos, state), JSBlocks.PLANTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HarvesterBlockEntity>> HARVESTER =
            BLOCK_ENTITIES.register("harvester", () -> BlockEntityType.Builder
                    .of((pos, state) -> new HarvesterBlockEntity(JSBlockEntities.HARVESTER.get(), pos, state), JSBlocks.HARVESTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TreeFarmBlockEntity>> TREE_FARM =
            BLOCK_ENTITIES.register("tree_farm", () -> BlockEntityType.Builder
                    .of((pos, state) -> new TreeFarmBlockEntity(JSBlockEntities.TREE_FARM.get(), pos, state), JSBlocks.TREE_FARM.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BiofuelGeneratorBlockEntity>> BIOFUEL_GENERATOR =
            BLOCK_ENTITIES.register("biofuel_generator", () -> BlockEntityType.Builder
                    .of(BiofuelGeneratorBlockEntity::new, JSBlocks.BIOFUEL_GENERATOR.get()).build(null));

    // storage
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrateBlockEntity>> CRATE =
            BLOCK_ENTITIES.register("crate", () -> BlockEntityType.Builder
                    .of(CrateBlockEntity::new, JSBlocks.BASIC_CRATE.get(), JSBlocks.REINFORCED_CRATE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BinBlockEntity>> BULK_BIN =
            BLOCK_ENTITIES.register("bulk_bin", () -> BlockEntityType.Builder
                    .of(BinBlockEntity::new, JSBlocks.BASIC_BULK_BIN.get(), JSBlocks.REINFORCED_BULK_BIN.get()).build(null));

    /** Every recipe machine's type, for registering their capabilities together. */
    public static List<DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>>> processingMachines() {
        return List.of(CRUSHER, ALLOY_SMELTER, COMPRESSOR, WIRE_DRAWER, SAWMILL, ORE_WASHER, FLUID_MIXER, CENTRIFUGE,
                CIRCUIT_ASSEMBLER, BIO_REACTOR);
    }

    /** A recipe machine's block entity type: its block, its slot layout and the recipes it runs. */
    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> processing(
            String name, Supplier<? extends Block> block, List<SlotDef> slots, MachineRecipeKind kind) {
        return processing(name, block, slots, kind, -1, -1);
    }

    /** The same with tanks: where the input and output tank gauges go (-1 for none). */
    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<ProcessingMachineBlockEntity>> processing(
            String name, Supplier<? extends Block> block, List<SlotDef> slots, MachineRecipeKind kind, int inputTankX, int outputTankX) {
        Holder<BlockEntityType<ProcessingMachineBlockEntity>> self = new Holder<>();
        self.value = BLOCK_ENTITIES.register(name, () -> BlockEntityType.Builder
                .of((pos, state) -> new ProcessingMachineBlockEntity(self.value.get(), pos, state, slots, kind, inputTankX, outputTankX), block.get())
                .build(null));
        return self.value;
    }

    private static final class Holder<T extends BlockEntityType<?>> {
        DeferredHolder<BlockEntityType<?>, T> value;
    }

    private JSBlockEntities() {}
}
