package com.walnutbeachdigital.joulesmith.item;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A machine upgrade card. Each machine has three upgrade slots; cards of the same kind stack their effect. */
public class UpgradeCardItem extends Item {
    public enum Type {
        /** Each card: runs 50% faster, using 25% more energy per run. */
        SPEED,
        /** Each card: 20% less energy per run (down to 40%). */
        EFFICIENCY,
        /** Each card: +10% chance on byproducts and a 10% chance of one extra main output. */
        BONUS,
        /** Pushes finished items into blocks touching the machine's output faces. */
        EJECT,
        /** Farm machines: each card widens the area by two blocks each way (5x5, 9x9, 13x13). */
        RANGE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Type type;

    public UpgradeCardItem(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public Type type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.upgrade." + type.id()).withStyle(ChatFormatting.GRAY));
    }
}
