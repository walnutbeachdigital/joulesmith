package com.walnutbeachdigital.joulesmith.storage.bin;

import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** A Bulk Bin as an item: shows what it holds and how many. */
public class BinItem extends BlockItem {
    public BinItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BinContents contents = stack.getOrDefault(JSDataComponents.BIN.get(), BinContents.EMPTY);
        int capacity = getBlock() instanceof BinBlock bin ? bin.capacity() : 0;
        if (contents.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.joulesmith.bin.empty", String.format("%,d", capacity)).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.joulesmith.bin.holds", contents.item().getHoverName(),
                    String.format("%,d", contents.count()), String.format("%,d", capacity)).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.joulesmith.bin.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
