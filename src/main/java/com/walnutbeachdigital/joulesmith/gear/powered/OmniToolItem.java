package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Pickaxe, shovel, axe, hoe and shears in one, mining like netherite and faster than the Power Drill, with
 * ten times its charge. Range modules dig 3x3 or 5x5 as on the Drill. Right-click strips logs, scrapes
 * copper and makes paths; sneak and right-click to till.
 */
public class OmniToolItem extends PowerDrillItem {
    private static final List<ItemAbility> ACTIONS = List.of(ItemAbilities.AXE_STRIP, ItemAbilities.AXE_SCRAPE,
            ItemAbilities.AXE_WAX_OFF, ItemAbilities.SHOVEL_FLATTEN);

    public OmniToolItem(Properties properties) {
        super(4_000_000, new Tool(List.of(
                Tool.Rule.deniesDrops(BlockTags.INCORRECT_FOR_NETHERITE_TOOL),
                Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0f),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, 14.0f),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, 14.0f),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, 14.0f),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, 14.0f),
                Tool.Rule.overrideSpeed(BlockTags.LEAVES, 15.0f),
                Tool.Rule.overrideSpeed(BlockTags.WOOL, 5.0f)), 1.0f, 0), properties);
    }

    @Override
    public int transferRate() {
        return 10_000;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return ACTIONS.contains(ability) || ability == ItemAbilities.HOE_TILL
                || ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(ability);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!charged(stack)) return InteractionResult.PASS;
        var level = context.getLevel();
        var pos = context.getClickedPos();
        List<ItemAbility> order = context.isSecondaryUseActive() ? List.of(ItemAbilities.HOE_TILL) : ACTIONS;
        for (ItemAbility ability : order) {
            if (ability == ItemAbilities.SHOVEL_FLATTEN && context.getClickedFace() == net.minecraft.core.Direction.DOWN) continue;
            BlockState changed = level.getBlockState(pos).getToolModifiedState(context, ability, false);
            if (changed == null) continue;
            if (!level.isClientSide) {
                level.setBlock(pos, changed, 11);
                level.playSound(null, pos, ability == ItemAbilities.SHOVEL_FLATTEN ? SoundEvents.SHOVEL_FLATTEN
                        : ability == ItemAbilities.HOE_TILL ? SoundEvents.HOE_TILL : SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1f, 1f);
                if (context.getPlayer() != null) PoweredItem.use(stack, cost(stack), context.getPlayer());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.omni_tool").withStyle(ChatFormatting.GRAY));
        PoweredItem.appendTooltip(stack, tooltip);
    }
}
