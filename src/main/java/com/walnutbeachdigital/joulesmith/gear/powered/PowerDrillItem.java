package com.walnutbeachdigital.joulesmith.gear.powered;

import java.util.List;

import com.walnutbeachdigital.joulesmith.gear.AreaToolItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pickaxe and shovel in one, mining like diamond and a little faster. With a Range module it digs the
 * 3x3 square facing you (5x5 with two); sneak to dig one block.
 */
public class PowerDrillItem extends PoweredToolItem {
    private static final ThreadLocal<Boolean> BREAKING = ThreadLocal.withInitial(() -> false);

    public PowerDrillItem(Properties properties) {
        super(400_000, properties.component(net.minecraft.core.component.DataComponents.TOOL, new Tool(List.of(
                Tool.Rule.deniesDrops(BlockTags.INCORRECT_FOR_DIAMOND_TOOL),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, 10.0f),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, 10.0f)), 1.0f, 0)));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        super.mineBlock(stack, level, state, pos, miner);
        if (BREAKING.get() || level.isClientSide || !(miner instanceof ServerPlayer player) || player.isShiftKeyDown()
                || !PoweredItem.hasModule(stack, Module.RANGE) || !isCorrectToolForDrops(stack, state)) {
            return true;
        }
        Direction face = AreaToolItem.facing(player);
        int r = (int) PoweredItem.modules(stack).stream().filter(m -> m == Module.RANGE).count();   // 1: 3x3, 2: 5x5
        float hardness = state.getDestroySpeed(level, pos);
        BREAKING.set(true);
        try {
            for (int a = -r; a <= r; a++) {
                for (int b = -r; b <= r; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos other = switch (face.getAxis()) {
                        case X -> pos.offset(0, a, b);
                        case Y -> pos.offset(a, 0, b);
                        case Z -> pos.offset(a, b, 0);
                    };
                    BlockState otherState = level.getBlockState(other);
                    float otherHardness = otherState.getDestroySpeed(level, other);
                    if (otherState.isAir() || otherHardness < 0 || otherHardness > hardness + 1.5f) continue;
                    if (!isCorrectToolForDrops(stack, otherState)) continue;
                    player.gameMode.destroyBlock(other);
                }
            }
        } finally {
            BREAKING.set(false);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.joulesmith.power_drill").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
