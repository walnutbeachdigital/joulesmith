package com.walnutbeachdigital.joulesmith.item.filter;

import java.util.List;

import com.walnutbeachdigital.joulesmith.registry.JSDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A Filter Card. Use it in the air to choose its items; right-click an item duct's side to fit it there. */
public class FilterCardItem extends Item {
    public FilterCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new FilterMenu(id, inventory, hand), stack.getHoverName()),
                    buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FilterContents filter = stack.getOrDefault(JSDataComponents.FILTER.get(), FilterContents.EMPTY);
        tooltip.add(Component.translatable(filter.blacklist() ? "tooltip.joulesmith.filter.blacklist" : "tooltip.joulesmith.filter.whitelist")
                .withStyle(ChatFormatting.GRAY));
        for (ItemStack item : filter.items()) {
            if (!item.isEmpty()) tooltip.add(Component.literal(" - ").append(item.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.joulesmith.filter.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
