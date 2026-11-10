package com.walnutbeachdigital.joulesmith.automation;

import java.util.ArrayList;
import java.util.List;

import com.walnutbeachdigital.joulesmith.machine.MachineBlockEntity;
import com.walnutbeachdigital.joulesmith.machine.SlotDef;
import com.walnutbeachdigital.joulesmith.machine.SlotRole;
import com.walnutbeachdigital.joulesmith.registry.JSBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Places blocks from its nine slots into the space in front of it, a slot at a time, when that space is free. */
public class BlockPlacerBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 10_000, MAX_INPUT = 200, ENERGY_PER_BLOCK = 50, INTERVAL = 8;

    private int cooldown;

    static List<SlotDef> grid(int x0, SlotRole role) {
        List<SlotDef> slots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = x0 + col * 18, y = 17 + row * 18;
                slots.add(role == SlotRole.INPUT ? SlotDef.input(x, y) : SlotDef.byproduct(x, y));
            }
        }
        return slots;
    }

    public BlockPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(JSBlockEntities.BLOCK_PLACER.get(), pos, state, grid(62, SlotRole.INPUT), CAPACITY, MAX_INPUT, 0, true);
    }

    @Override
    protected boolean isValidInput(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    @Override
    public ProgressStyle progressStyle() {
        return ProgressStyle.NONE;
    }

    @Override
    protected boolean work(Level level, BlockPos pos, boolean allowed) {
        if (!allowed || ++cooldown < Math.max(1, INTERVAL / (1 + upgrades(com.walnutbeachdigital.joulesmith.item.UpgradeCardItem.Type.SPEED)))) return false;
        cooldown = 0;
        Direction facing = facing();
        BlockPos target = pos.relative(facing);
        if (!level.getBlockState(target).canBeReplaced() || energy.getEnergyStored() < ENERGY_PER_BLOCK) return false;
        for (int i = 0; i < slotDefs().size(); i++) {
            if (slotDefs().get(i).role() != SlotRole.INPUT) continue;
            ItemStack stack = items.getStackInSlot(i);
            if (!(stack.getItem() instanceof BlockItem item)) continue;
            ItemStack placing = stack.copy();
            if (item.place(new DirectionalPlaceContext(level, target, facing, placing, facing.getOpposite())).consumesAction()) {
                items.setStackInSlot(i, placing);           // place() used one up
                energy.consume(ENERGY_PER_BLOCK);
                return true;
            }
        }
        return false;
    }
}
