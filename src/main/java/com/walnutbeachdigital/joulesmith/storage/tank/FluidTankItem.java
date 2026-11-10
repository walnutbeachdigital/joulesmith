package com.walnutbeachdigital.joulesmith.storage.tank;

import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** The Fluid Tank as an item: shows the fluid it carries, and places back down with it. */
public class FluidTankItem extends BlockItem {
    public FluidTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SimpleFluidContent fluid = stack.getOrDefault(JSDataComponents.FLUID.get(), SimpleFluidContent.EMPTY);
        tooltip.add(FluidTankBlock.describe(fluid.copy(), FluidTankBlockEntity.CAPACITY).copy().withStyle(ChatFormatting.GRAY));
    }
}
