package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A powered mining tool (Power Drill, Chainsaw): each block mined uses {@link #COST} FE, more with a Speed
 * module. When empty it digs no faster than a bare hand and drops nothing that needs a tool.
 */
public abstract class PoweredToolItem extends Item implements PoweredItem {
    public static final int COST = 200;
    private static final float SPEED_MODULE = 1.75f;

    private final int capacity;

    protected PoweredToolItem(int capacity, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
    }

    @Override public int capacity() { return capacity; }
    @Override public int transferRate() { return 2000; }

    @Override
    public boolean accepts(Module module) {
        return module.kind == Module.Kind.TOOL || module.kind == Module.Kind.TOOL_OR_WEAPON;
    }

    /** The FE one block costs. */
    public static int cost(ItemStack stack) {
        int cost = com.walnutbeachdigital.joulesmith.JSConfig.get(com.walnutbeachdigital.joulesmith.JSConfig.TOOL_COST);
        return PoweredItem.hasModule(stack, Module.SPEED) ? cost * 3 / 2 : cost;
    }

    protected static boolean charged(ItemStack stack) {
        return PoweredItem.energy(stack) >= cost(stack);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!charged(stack)) return 1.0f;
        float speed = super.getDestroySpeed(stack, state);
        return speed > 1.0f && PoweredItem.hasModule(stack, Module.SPEED) ? speed * SPEED_MODULE : speed;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return charged(stack) && super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0f) {
            PoweredItem.use(stack, cost(stack), miner);
        }
        return true;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return PoweredItem.barWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return BAR_COLOR; }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();   // charging doesn't bob the item
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PoweredItem.appendTooltip(stack, tooltip);
    }
}
