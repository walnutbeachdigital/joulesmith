package com.walnutbeachdigital.joulesmith.farming;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Plants saplings in a grid in front of it (every other block each way, so trees have room) and, when a
 * tree has grown, fells the whole thing, logs and leaves, a few blocks a tick, top down. Leaves drop what
 * they would when they decay (saplings, sticks, apples), so the farm keeps itself in saplings.
 */
public class TreeFarmBlockEntity extends AreaMachineBlockEntity {
    public static final int ENERGY_PER_SAPLING = 40, ENERGY_PER_BLOCK = 30, BLOCKS_PER_TICK = 4, MAX_TREE = 512, REACH = 7;

    public static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        for (int row = 0; row < 3; row++) slots.add(SlotDef.input(44, 17 + row * 18));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) slots.add(SlotDef.byproduct(98 + col * 18, 17 + row * 18));
        }
        return slots;
    }

    private final Deque<BlockPos> felling = new ArrayDeque<>();

    public TreeFarmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, layout());
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof SaplingBlock;
    }

    @Override
    protected int continueJob(ServerLevel level) {
        int used = 0;
        for (int n = 0; n < BLOCKS_PER_TICK && !felling.isEmpty(); n++) {
            BlockPos pos = felling.peekFirst();
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                felling.pollFirst();
                continue;
            }
            if (!hasEnergy(ENERGY_PER_BLOCK)) break;
            List<ItemStack> drops = Block.getDrops(state, level, pos, null);
            if (!fits(drops)) break;                               // outputs full: wait
            felling.pollFirst();
            store(drops);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (state.is(BlockTags.LOGS)) level.levelEvent(2001, pos, Block.getId(state));
            used += spend(ENERGY_PER_BLOCK);
        }
        return used;
    }

    @Override
    protected int workAt(ServerLevel level, BlockPos spot, int dx, int dz) {
        if (!felling.isEmpty()) return 0;
        BlockState state = level.getBlockState(spot);
        if (state.is(BlockTags.LOGS)) {
            findTree(level, spot);
            return felling.isEmpty() ? 0 : 1;
        }
        int r = radius();
        boolean gridSpot = (dx + r) % 2 == 0 && (dz + r) % 2 == 0;
        if (gridSpot && state.isAir() && hasEnergy(ENERGY_PER_SAPLING)) return plant(level, spot);
        return 0;
    }

    private int plant(ServerLevel level, BlockPos spot) {
        for (int i = 0; i < slotDefs().size(); i++) {
            if (slotDefs().get(i).role() != SlotRole.INPUT) continue;
            ItemStack stack = items.getStackInSlot(i);
            if (!(stack.getItem() instanceof BlockItem item)) continue;
            BlockState sapling = item.getBlock().defaultBlockState();
            if (!sapling.canSurvive(level, spot)) continue;
            level.setBlock(spot, sapling, Block.UPDATE_ALL);
            level.playSound(null, spot, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.6f, 1f);
            items.extractItem(i, 1, false);
            return spend(ENERGY_PER_SAPLING);
        }
        return 0;
    }

    /** Every log and leaf joined to the trunk at {@code base}, nearby, felled from the top down. */
    private void findTree(ServerLevel level, BlockPos base) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> found = new ArrayList<>();
        queue.add(base);
        seen.add(base);
        while (!queue.isEmpty() && found.size() < MAX_TREE) {
            BlockPos pos = queue.poll();
            BlockState state = level.getBlockState(pos);
            boolean log = state.is(BlockTags.LOGS);
            if (!log && !state.is(BlockTags.LEAVES)) continue;
            if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) continue;   // player-placed
            found.add(pos);
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos next = pos.offset(x, y, z);
                        if (Math.abs(next.getX() - base.getX()) > REACH || Math.abs(next.getZ() - base.getZ()) > REACH) continue;
                        if (next.getY() < base.getY() || !level.isLoaded(next) || !seen.add(next)) continue;
                        // leaves only lead on to more leaves or logs; logs reach in every direction
                        if (log || level.getBlockState(next).is(BlockTags.LEAVES)) queue.add(next);
                    }
                }
            }
        }
        found.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        felling.addAll(found);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Felling", new LongArrayTag(felling.stream().mapToLong(BlockPos::asLong).toArray()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        felling.clear();
        for (long l : tag.getLongArray("Felling")) felling.add(BlockPos.of(l));
    }
}
