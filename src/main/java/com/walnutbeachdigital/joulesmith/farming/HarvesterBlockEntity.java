package com.walnutbeachdigital.joulesmith.farming;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.SlotDef;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Harvests ripe crops in front of it and replants them straight away (keeping one seed back), picks
 * melons, pumpkins and sweet berries, and cuts sugar cane, cactus and bamboo above their bottom block.
 * Everything goes into its nine output slots; when they are full it waits.
 */
public class HarvesterBlockEntity extends AreaMachineBlockEntity {
    public static final int ENERGY_PER_HARVEST = 60, MAX_STALK = 16;

    public static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) slots.add(SlotDef.byproduct(62 + col * 18, 17 + row * 18));
        }
        return slots;
    }

    public HarvesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, layout());
    }

    @Override
    protected int workAt(ServerLevel level, BlockPos spot, int dx, int dz) {
        if (!hasEnergy(ENERGY_PER_HARVEST)) return 0;
        BlockState state = level.getBlockState(spot);
        Block block = state.getBlock();
        if (block instanceof CropBlock crop && crop.isMaxAge(state)) {
            return replant(level, spot, state, crop.getStateForAge(0));
        }
        if (block instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3) {
            return replant(level, spot, state, state.setValue(NetherWartBlock.AGE, 0));
        }
        if (block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) >= 3) {
            return replant(level, spot, state, state.setValue(SweetBerryBushBlock.AGE, 1));
        }
        if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            return take(level, spot, state, Blocks.AIR.defaultBlockState());
        }
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock) {
            // leave the bottom block so it grows again; cut from the top down
            BlockPos top = spot;
            for (int i = 0; i < MAX_STALK && level.getBlockState(top.above()).is(block); i++) top = top.above();
            if (top.equals(spot)) return 0;
            return take(level, top, level.getBlockState(top), Blocks.AIR.defaultBlockState());
        }
        return 0;
    }

    /** Takes a ripe plant's drops, keeping one of its seeds back, and resets it to grow again. */
    private int replant(ServerLevel level, BlockPos spot, BlockState ripe, BlockState young) {
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(ripe, level, spot, null));
        ItemStack seed = new ItemStack(ripe.getBlock().asItem());
        for (ItemStack drop : drops) {
            if (!seed.isEmpty() && ItemStack.isSameItem(drop, seed)) {
                drop.shrink(1);
                break;
            }
        }
        drops.removeIf(ItemStack::isEmpty);
        if (!fits(drops)) return 0;
        store(drops);
        level.setBlock(spot, young, Block.UPDATE_ALL);
        level.levelEvent(2001, spot, Block.getId(ripe));          // break particles and sound
        return spend(ENERGY_PER_HARVEST);
    }

    private int take(ServerLevel level, BlockPos pos, BlockState state, BlockState replacement) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null);
        if (!fits(drops)) return 0;
        store(drops);
        level.setBlock(pos, replacement, Block.UPDATE_ALL);
        level.levelEvent(2001, pos, Block.getId(state));
        return spend(ENERGY_PER_HARVEST);
    }
}
