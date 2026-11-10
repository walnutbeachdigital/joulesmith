package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A module, fitted to powered gear at the Modification Table. */
public class ModuleItem extends Item {
    public final Module module;

    public ModuleItem(Module module, Properties properties) {
        super(properties);
        this.module = module;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.module." + module.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.joulesmith.module.fits." + module.kind.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
