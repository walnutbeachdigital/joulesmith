package com.walnutbeachdigital.joulesmith.farming;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Fertilizer from the Fertilizer Mixer: on a plant it works like three bone meal at once. Hydroponic Beds
 * also take it to grow twice as fast.
 */
public class FertilizerItem extends Item {
    public static final int STRENGTH = 3;

    public FertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean grew = false;
        for (int i = 0; i < STRENGTH; i++) {
            ItemStack dose = new ItemStack(net.minecraft.world.item.Items.BONE_MEAL);   // bone meal's own growth, once per dose
            if (!BoneMealItem.growCrop(dose, level, pos)) break;
            grew = true;
        }
        if (!grew) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.levelEvent(1505, pos, 15);                // bone meal particles
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.fertilizer").withStyle(ChatFormatting.GRAY));
    }
}
