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

    private final int tier;

    public ExoArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        this(3, material, type, properties);
    }

    /** {@code tier}: 3 (the first suit), 4 (Elite) or 5 (Ultimate). */
    public ExoArmorItem(int tier, Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties.stacksTo(1));
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override public int capacity() { return tier >= 5 ? 8_000_000 : tier == 4 ? 2_000_000 : 400_000; }
    @Override public int transferRate() { return tier >= 5 ? 20_000 : tier == 4 ? 8_000 : 2_000; }
    @Override public int moduleSlots() { return tier >= 5 ? 6 : tier == 4 ? 4 : 2; }

    /** How much more of the damage this piece soaks up than the first suit's. */
    public float absorbScale() {
        return tier >= 5 ? 1.2f : tier == 4 ? 1.1f : 1.0f;
    }

    @Override
    public boolean accepts(Module module) {
        return switch (module.kind) {
            case HELMET -> getType() == Type.HELMET;
            case CHESTPLATE -> getType() == Type.CHESTPLATE;
            case LEGGINGS -> getType() == Type.LEGGINGS;
            case BOOTS -> getType() == Type.BOOTS;
            case ARMOR -> true;
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
        tooltip.add(Component.translatable("tooltip.joulesmith.exo_suit." + tier).withStyle(ChatFormatting.GRAY));
        PoweredItem.appendTooltip(stack, tooltip);
    }
}
