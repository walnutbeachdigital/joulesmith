package com.walnutbeachdigital.joulesmith.registry;

import com.walnutbeachdigital.joulesmith.Joulesmith;
import com.walnutbeachdigital.joulesmith.machine.creative.CreativePowerCellBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.furnace.ElectricFurnaceBlockEntity;

import net.minecraft.core.registries.Registries;
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

    private JSBlockEntities() {}
}
