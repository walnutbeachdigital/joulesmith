package com.walnutbeachdigital.joulesmith.guide;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The Engineer's Handbook: right-click to open it. */
public class HandbookItem extends Item {
    public HandbookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) com.walnutbeachdigital.joulesmith.client.HandbookOpener.open();   // screens only exist on the client
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
