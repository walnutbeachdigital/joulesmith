package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A portable battery. Carried anywhere in your inventory it tops up your other powered gear, up to
 * 100, 250 or 500 FE per tick by tier; use it to switch that off and on. Also part of every powered tool's recipe.
 */
public class EnergyCellItem extends Item implements PoweredItem {
    private final int capacity, rate;

    public EnergyCellItem(int capacity, int rate, Properties properties) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.rate = rate;
    }

    @Override public int capacity() { return capacity; }
    @Override public int transferRate() { return 2000; }

    private static boolean off(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)
                && stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getBoolean("Off");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean off = !off(stack);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
                    tag -> tag.putBoolean("Off", off));
            player.displayClientMessage(Component.translatable(off ? "message.joulesmith.energy_cell.off" : "message.joulesmith.energy_cell.on"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || off(stack) || level.getGameTime() % 4 != 0) return;
        int budget = Math.min(rate * 4, PoweredItem.energy(stack));
        if (budget <= 0) return;
        int given = PoweredItem.chargeInventory(player, budget, false, stack);
        if (given > 0) PoweredItem.setEnergy(stack, PoweredItem.energy(stack) - given);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return PoweredItem.barWidth(stack); }
    @Override public int getBarColor(ItemStack stack) { return BAR_COLOR; }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(off(stack) ? "tooltip.joulesmith.energy_cell.off" : "tooltip.joulesmith.energy_cell")
                .withStyle(ChatFormatting.GRAY));
        PoweredItem.appendTooltip(stack, tooltip);
    }
}
