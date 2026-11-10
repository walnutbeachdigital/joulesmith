package com.walnutbeachdigital.joulesmith.registry;

import java.util.EnumMap;
import java.util.Map;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.material.Metal;

import com.walnutbeachdigital.joulesmith.machine.MachineBlock;
import com.walnutbeachdigital.joulesmith.machine.creative.CreativePowerCellBlock;
import com.walnutbeachdigital.joulesmith.machine.furnace.ElectricFurnaceBlock;

import net.minecraft.world.level.block.Block;
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

    public static final DeferredBlock<CreativePowerCellBlock> CREATIVE_POWER_CELL = BLOCKS.register("creative_power_cell",
            () -> new CreativePowerCellBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3_600_000.0f).noLootTable().sound(SoundType.METAL).lightLevel(state -> 7)));

    private JSBlocks() {}
}
