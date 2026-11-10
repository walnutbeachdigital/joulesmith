package com.walnutbeachdigital.joulesmith.automation;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.item.UpgradeCardItem;
import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Breaks the block in front of it and keeps the drops. A tool in its tool slot is used as if a player held
 * it (Fortune and Silk Touch work, and it wears down); without one it only breaks what a bare hand could
 * harvest. Harder blocks take longer.
 */
public class BlockBreakerBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 10_000, MAX_INPUT = 200, BASE_ENERGY = 40, ENERGY_PER_HARDNESS = 20;

    static List<SlotDef> layout() {
        List<SlotDef> slots = new ArrayList<>();
        slots.add(SlotDef.input(35, 35));                           // tool
        slots.addAll(BlockPlacerBlockEntity.grid(98, SlotRole.OUTPUT));
        return slots;
    }

    public BlockBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.BLOCK_BREAKER.get(), pos, state, layout(), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.isDamageableItem() || stack.getMaxStackSize() == 1;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    /** The drops of breaking {@code target} with the tool, or null if it can't or shouldn't be broken. */
    static List<ItemStack> harvest(ServerLevel level, BlockPos target, BlockState state, ItemStack tool) {
        if (state.isAir() || state.getDestroySpeed(level, target) < 0 || state.getBlock() instanceof LiquidBlock) return null;
        if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) return null;
        return Block.getDrops(state, level, target, level.getBlockEntity(target), null, tool);
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (!allowed || !(level instanceof ServerLevel server)) {
            progress = 0;
            return false;
        }
        BlockPos target = pos.relative(facing());
        BlockState state = level.getBlockState(target);
        ItemStack tool = items.getStackInSlot(0);
        List<ItemStack> drops = harvest(server, target, state, tool);
        if (drops == null || !fits(drops)) {
            progress = 0;
            return false;
        }
        float hardness = state.getDestroySpeed(level, target);
        float speed = Math.max(1f, tool.getDestroySpeed(state)) * (1 + 0.5f * upgrades(UpgradeCardItem.Type.SPEED));
        maxProgress = Math.max(4, (int) Math.ceil(hardness * 30 / speed));
        int energyCost = BASE_ENERGY + (int) (hardness * ENERGY_PER_HARDNESS);
        if (energy.getEnergyStored() < energyCost / Math.max(1, maxProgress)) return false;
        energy.consume(energyCost / Math.max(1, maxProgress));
        if (++progress < maxProgress) return true;
        progress = 0;
        store(drops);
        level.destroyBlock(target, false);
        if (!tool.isEmpty() && tool.isDamageableItem()) {
            tool.hurtAndBreak(1, server, (ServerPlayer) null, item -> {});
        }
        return true;
    }

    /** Whether the drops fit in the output slots. */
    boolean fits(List<ItemStack> drops) {
        return OutputHelper.fits(items, slotDefs(), drops);
    }

    void store(List<ItemStack> drops) {
        OutputHelper.store(items, slotDefs(), drops);
    }
}
