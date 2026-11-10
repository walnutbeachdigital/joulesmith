package com.walnutbeachdigital.joulesmith.automation;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mines out a square in front of it, 16x16 at first (32x32 and 64x64 with Range Upgrades), one layer at a
 * time from just below itself down to bedrock. It skips fluids, unbreakable blocks and anything with an
 * inventory, and mines as a diamond pickaxe would; a pickaxe in its tool slot lends its enchantments
 * (Fortune, Silk Touch) without wearing down. When its outputs are full it waits.
 */
public class QuarryBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 100_000, MAX_INPUT = 2_000, BASE_ENERGY = 100, ENERGY_PER_HARDNESS = 40, INTERVAL = 4;

    private int layer = Integer.MIN_VALUE;        // the y being mined; starts just below the quarry
    private int index;                            // position within the layer
    private int cooldown;
    private boolean finished;

    static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        slots.add(SlotDef.input(35, 35));                           // optional enchanted pickaxe
        slots.addAll(BlockPlacerBlockEntity.grid(98, SlotRole.OUTPUT));
        return slots;
    }

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.QUARRY.get(), pos, state, layout(), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.getMaxStackSize() == 1;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    public int size() {
        return 16 << Math.min(2, upgrades(UpgradeCardItem.Type.RANGE));
    }

    /** A diamond pickaxe carrying the enchantments of the pickaxe in the tool slot. */
    private ItemStack tool() {
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack given = items.getStackInSlot(0);
        if (!given.isEmpty()) tool.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, given.getEnchantments());
        return tool;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (!allowed || finished || !(level instanceof ServerLevel server)) return false;
        int interval = Math.max(1, (int) (INTERVAL / (1 + 0.5 * upgrades(UpgradeCardItem.Type.SPEED))));
        if (++cooldown < interval) return true;
        cooldown = 0;
        if (layer == Integer.MIN_VALUE) layer = pos.getY() - 1;
        int size = size();
        Direction facing = facing(), side = facing.getClockWise();
        // look ahead for the next block worth mining (skipping air quickly), at most a row's worth this tick
        for (int tries = 0; tries < size; tries++) {
            if (layer < level.getMinBuildHeight()) {
                finished = true;
                setChanged();
                return false;
            }
            int forward = index / size + 1, across = index % size - size / 2;
            BlockPos target = new BlockPos(pos.getX(), layer, pos.getZ()).relative(facing, forward).relative(side, across);
            if (!level.isLoaded(target)) return false;                        // wait for the chunk
            BlockState state = level.getBlockState(target);
            boolean skip = state.isAir() || state.getBlock() instanceof LiquidBlock || state.getDestroySpeed(level, target) < 0
                    || level.getBlockEntity(target) != null || state.is(Blocks.BEDROCK);
            if (skip) {
                advance(size);
                continue;
            }
            ItemStack tool = tool();
            List<ItemStack> drops = Block.getDrops(state, server, target, null, null, tool);
            if (!OutputHelper.fits(items, slotDefs(), drops)) return false;    // outputs full
            float hardness = state.getDestroySpeed(level, target);
            int cost = (int) Math.ceil((BASE_ENERGY + hardness * ENERGY_PER_HARDNESS)
                    * Math.max(0.4, 1 - 0.2 * upgrades(UpgradeCardItem.Type.EFFICIENCY)));
            if (energy.getEnergyStored() < cost) return false;
            energy.consume(cost);
            OutputHelper.store(items, slotDefs(), drops);
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.levelEvent(2001, target, Block.getId(state));
            advance(size);
            return true;
        }
        return true;
    }

    private void advance(int size) {
        if (++index >= size * size) {
            index = 0;
            layer--;
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Layer", layer);
        tag.putInt("Index", index);
        tag.putBoolean("Finished", finished);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Layer")) layer = tag.getInt("Layer");
        index = tag.getInt("Index");
        finished = tag.getBoolean("Finished");
    }
}
