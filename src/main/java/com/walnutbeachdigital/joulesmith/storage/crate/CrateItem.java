package com.walnutbeachdigital.joulesmith.storage.crate;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

/** A Crate as an item: shows how full it is and its first few stacks. */
public class CrateItem extends BlockItem {
    public CrateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int slots = getBlock() instanceof CrateBlock crate ? crate.rows() * 9 : 54;
        List<ItemStack> held = contents.nonEmptyStream().toList();
        tooltip.add(Component.translatable("tooltip.joulesmith.crate.slots", held.size(), slots).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < Math.min(5, held.size()); i++) {
            ItemStack s = held.get(i);
            tooltip.add(Component.literal(" - ").append(s.getHoverName()).append(" x" + s.getCount()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (held.size() > 5) {
            tooltip.add(Component.translatable("tooltip.joulesmith.crate.more", held.size() - 5).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
