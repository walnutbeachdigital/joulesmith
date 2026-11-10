package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hits like a diamond sword plus up to {@link #MAX_BONUS} more the fuller its charge. Each hit uses
 * {@link #COST} FE; when empty it hits like a fist.
 */
public class EnergySwordItem extends Item implements PoweredItem {
    public static final int COST = 400;
    private static final float MAX_BONUS = 5.0f;

    public EnergySwordItem(Properties properties) {
        super(properties.stacksTo(1).attributes(SwordItem.createAttributes(Tiers.DIAMOND, 3, -2.4f)));
    }

    @Override public int capacity() { return 200_000; }
    @Override public int transferRate() { return 2000; }

    @Override
    public boolean accepts(Module module) {
        return module.kind == Module.Kind.WEAPON || module.kind == Module.Kind.TOOL_OR_WEAPON;
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        ItemStack stack = source.getWeaponItem();
        if (stack == null || !(stack.getItem() instanceof EnergySwordItem)) return 0;
        if (!PoweredItem.has(stack, COST, source.getEntity() instanceof LivingEntity l ? l : null)) {
            return 1.0f - damage;                                        // empty: a bare-handed hit
        }
        return MAX_BONUS * PoweredItem.energy(stack) / capacity();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        PoweredItem.use(stack, COST, attacker);
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
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
        tooltip.add(Component.translatable("tooltip.joulesmith.energy_sword").withStyle(ChatFormatting.GRAY));
        PoweredItem.appendTooltip(stack, tooltip);
    }
}
