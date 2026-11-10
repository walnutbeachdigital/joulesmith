package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A piece of the Exo-Suit: light armor on its own, but while charged each piece soaks up
 * {@link #ABSORB} of the damage you take, using {@link #FE_PER_POINT} FE per half-heart it stops.
 * Each piece takes the modules for its slot (see {@link Module.Kind}).
 */
public class ExoArmorItem extends ArmorItem implements PoweredItem {
    public static final float ABSORB = 0.2f;
    public static final int FE_PER_POINT = 400;

    public ExoArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1));
    }

    @Override public int capacity() { return 400_000; }
    @Override public int transferRate() { return 2000; }

    @Override
    public boolean accepts(Module module) {
        return switch (module.kind) {
            case HELMET -> getType() == Type.HELMET;
            case CHESTPLATE -> getType() == Type.CHESTPLATE;
            case LEGGINGS -> getType() == Type.LEGGINGS;
            case BOOTS -> getType() == Type.BOOTS;
            default -> false;
        };
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
        tooltip.add(Component.translatable("tooltip.joulesmith.exo_suit").withStyle(ChatFormatting.GRAY));
        PoweredItem.appendTooltip(stack, tooltip);
    }
}
