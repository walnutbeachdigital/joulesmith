package com.walnutbeachdigital.joulesmith.farming;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plants seeds, saplings and other plants from its nine slots onto empty ground in front of it, tilling
 * dirt and grass into farmland first when a crop needs it. Slots are tried in order, so the first slot's
 * plant fills the field until it runs out.
 */
public class PlanterBlockEntity extends AreaMachineBlockEntity {
    public static final int ENERGY_PER_PLANT = 40;

    public static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) slots.add(SlotDef.input(62 + col * 18, 17 + row * 18));
        }
        return slots;
    }

    public PlanterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, layout());
    }

    /** Plants: anything that grows as a small plant (crops, saplings, stems, flowers), sugar cane and cactus. */
    @Override
    protected boolean isValidInput(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return false;
        Block block = item.getBlock();
        return block instanceof BushBlock || block instanceof SugarCaneBlock || block instanceof CactusBlock;
    }

    @Override
    protected int workAt(ServerLevel level, BlockPos spot, int dx, int dz) {
        if (!level.getBlockState(spot).isAir() || !hasEnergy(ENERGY_PER_PLANT)) return 0;
        BlockPos soilPos = spot.below();
        BlockState soil = level.getBlockState(soilPos);
        for (int i = 0; i < slotDefs().size(); i++) {
            if (slotDefs().get(i).role() != SlotRole.INPUT) continue;
            ItemStack stack = items.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) continue;
            BlockState plant = item.getBlock().defaultBlockState();
            boolean tilled = false;
            if (!plant.canSurvive(level, spot) && (soil.is(BlockTags.DIRT) || soil.is(Blocks.GRASS_BLOCK))) {
                level.setBlock(soilPos, Blocks.FARMLAND.defaultBlockState(), Block.UPDATE_ALL);   // try it on farmland
                tilled = true;
            }
            if (plant.canSurvive(level, spot)) {
                level.setBlock(spot, plant, Block.UPDATE_ALL);
                if (tilled) level.playSound(null, soilPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 0.6f, 1f);
                level.playSound(null, spot, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.6f, 1f);
                items.extractItem(i, 1, false);
                return spend(ENERGY_PER_PLANT);
            }
            if (tilled) level.setBlock(soilPos, soil, Block.UPDATE_ALL);              // didn't help: put it back
        }
        return 0;
    }
}
