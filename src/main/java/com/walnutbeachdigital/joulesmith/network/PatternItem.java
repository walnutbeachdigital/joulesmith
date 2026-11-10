package com.walnutbeachdigital.joulesmith.network;

import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** An encoded Pattern: a crafting recipe for an Auto-Crafting CPU. Made from a Blank Pattern at a Crafting Terminal. */
public class PatternItem extends Item {
    public PatternItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static PatternContents pattern(ItemStack stack) {
        return stack.get(JSDataComponents.PATTERN.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PatternContents p = pattern(stack);
        if (p == null) return;
        tooltip.add(Component.translatable("tooltip.joulesmith.pattern.makes", p.output().getCount(), p.output().getHoverName())
                .withStyle(ChatFormatting.AQUA));
        for (ItemStack s : p.grid()) {
            if (!s.isEmpty()) tooltip.add(Component.literal("  ").append(s.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }
}
